package com.cassonade.client.light;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.function.Predicate;

import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;

import net.fabricmc.loader.api.FabricLoader;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.client.renderer.chunk.RenderSectionRegion;
import net.minecraft.client.renderer.chunk.SectionCopy;
import net.minecraft.util.ARGB;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.PalettedContainer;

import com.cassonade.client.mixin.RenderSectionRegionAccessor;
import com.cassonade.client.mixin.SectionCopyAccessor;
import com.cassonade.item.CassonadeItems;

/**
 * Light-emitting blocks tint the terrain they light with their own color: soul fire blue, redstone red,
 * copper torches green, amethyst purple. The world's light is unchanged; only chunk meshes are tinted, as
 * they are built (SectionCompilerMixin, BufferBuilderMixin), fluids included.
 *
 * The color follows the game's own block light: a lit block got its level from a neighbor one level
 * brighter, so its color is the average of those neighbors', up to the blocks that emit. The color goes
 * where the light goes, around corners and never through walls, without a second flood fill. Blocks
 * without a color of their own (torches, lanterns, fire) count as white, diluting the colored ones near
 * them. Light carried by entities (DynamicLights) takes its color from the item, the mod's sculk torch and
 * waystone included. A section with no colored light around is left alone.
 */
public final class ColoredLight {
	private static final int WHITE = 0xFFFFFFFF;
	/** No color for this vertex: its alpha stays whole. */
	private static final int UNTINTED = 255;
	/** Hues and saturations a vertex's alpha holds: 32 and 8, in its 8 bits. */
	private static final int HUES = 32;
	private static final int SATURATIONS = 8;
	private static final int NONE = -1;
	private static final int SCULK = 0xFF4ADCE6;
	/** Sodium builds chunk meshes itself, through none of this. */
	private static final boolean ENABLED = !FabricLoader.getInstance().isModLoaded("sodium");
	private static final ThreadLocal<Context> CONTEXT = new ThreadLocal<>();
	/** Built by the first worker thread to need it; another one racing it builds the same table. */
	private static volatile Map<Block, Integer> colors;

	private ColoredLight() {
	}

	/** Called as a section starts being meshed, on its worker thread. */
	public static void begin(SectionPos section, RenderSectionRegion region) {
		CONTEXT.set(ENABLED && (hasColoredEmitter(region) || DynamicLights.coloredNear(section)) ? new Context(section.origin(), region) : null);
	}

	public static void end() {
		CONTEXT.remove();
	}

	/**
	 * A vertex's color, carrying the colored light in front of it while a section is meshed; its position is
	 * relative to the section, its normal the face's. The color itself is left as it is: its alpha, always whole
	 * on terrain, takes the light's hue and saturation instead, and the terrain shader (terrain.vsh) tints the
	 * vertex with it as much as the blocks' light outshines the sky's at that moment, so the sun washes it out by
	 * day and it comes back at night, without building the mesh again.
	 */
	public static int tint(int color, float x, float y, float z, float nx, float ny, float nz) {
		Context context = CONTEXT.get();
		if (context == null || ARGB.alpha(color) != 255) {
			return color;
		}
		int code = context.vertexCode(x, y, z, Math.round(nx), Math.round(ny), Math.round(nz));
		return code == UNTINTED ? color : ARGB.color(code, color & 0xFFFFFF);
	}

	/** The light's color as the alpha holds it: its hue in the low 5 bits, its saturation in the high 3; never 255. */
	private static int encode(float r, float g, float b) {
		float max = Math.max(r, Math.max(g, b));
		float min = Math.min(r, Math.min(g, b));
		int saturation = Math.round((1 - min / max) * (SATURATIONS - 1));
		if (saturation == 0) {
			return UNTINTED;
		}
		float delta = max - min;
		float hue;
		if (max == r) {
			hue = ((g - b) / delta) / 6;
		} else if (max == g) {
			hue = ((b - r) / delta + 2) / 6;
		} else {
			hue = ((r - g) / delta + 4) / 6;
		}
		int index = Math.floorMod(Math.round(hue * HUES), HUES);
		// The last hue, nearly red, at full saturation would read as no color at all: red it is
		if (index == HUES - 1 && saturation == SATURATIONS - 1) {
			index = 0;
		}
		return index | saturation << 5;
	}

	/** The color a block lights with, white for those without one of their own. */
	public static int color(Block block) {
		return colors().getOrDefault(block, WHITE);
	}

	/** The color a held or dropped item lights with, as DynamicLights carries it. */
	public static int color(ItemStack stack) {
		if (stack.is(Items.LAVA_BUCKET)) {
			return color(Blocks.LAVA);
		}
		// The sculk torch burns with the sculk's own glow
		if (stack.is(CassonadeItems.SCULK_TORCH)) {
			return SCULK;
		}
		return stack.getItem() instanceof BlockItem item ? color(item.getBlock()) : WHITE;
	}

	private static boolean hasColoredEmitter(RenderSectionRegion region) {
		Map<Block, Integer> table = colors();
		Predicate<BlockState> colored = state -> state.getLightEmission() > 0 && table.containsKey(state.getBlock());
		for (SectionCopy copy : ((RenderSectionRegionAccessor) region).cassonade$sections()) {
			PalettedContainer<BlockState> states = ((SectionCopyAccessor) copy).cassonade$section();
			if (states != null && states.maybeHas(colored)) {
				return true;
			}
		}
		return false;
	}

	/** Each colored block, found by name so that every variant (wall torches, weathered copper) comes along. */
	private static Map<Block, Integer> colors() {
		if (colors == null) {
			Map<Block, Integer> table = new IdentityHashMap<>();
			for (Block block : BuiltInRegistries.BLOCK) {
				int color = colorOf(BuiltInRegistries.BLOCK.getKey(block).getPath());
				if (color != NONE) {
					table.put(block, color);
				}
			}
			colors = table;
		}
		return colors;
	}

	private static int colorOf(String name) {
		if (name.startsWith("soul_")) {
			return 0x5AC8FF;
		}
		if (name.startsWith("redstone_") || name.endsWith("_redstone_ore")) {
			return name.equals("redstone_lamp") ? NONE : 0xFF3A2A;
		}
		if (name.contains("copper_torch") || name.contains("copper_wall_torch") || name.endsWith("copper_lantern")) {
			return 0xA8FF90;
		}
		if (name.contains("copper_bulb")) {
			return 0xFFC080;
		}
		if (name.contains("amethyst")) {
			return 0xC070FF;
		}
		return switch (name) {
			// The waystone's crystal, pink like the End crystal it is drawn from
			case "waystone" -> 0xE07CFF;
			// The glowcaps of mushroom fields, as cyan as their caps
			case "glowcap" -> 0x46D6CA;
			case "crying_obsidian", "nether_portal", "respawn_anchor" -> 0x9A4DFF;
			case "sea_lantern", "conduit", "beacon" -> 0x9AE6FF;
			case "lava", "magma_block" -> 0xFF7A30;
			case "shroomlight" -> 0xFFA050;
			case "glowstone" -> 0xFFE09A;
			case "ochre_froglight" -> 0xFFD870;
			case "verdant_froglight" -> 0x9CFF80;
			case "pearlescent_froglight" -> 0xFF9AE6;
			case "glow_lichen" -> 0x9CFFC0;
			case "cave_vines", "cave_vines_plant" -> 0xFFCC66;
			case "sculk_sensor", "calibrated_sculk_sensor", "sculk_catalyst" -> SCULK;
			default -> NONE;
		};
	}

	/** One section being meshed: the region it reads and the color found for each lit block so far. */
	private static final class Context {
		private final BlockPos origin;
		private final RenderSectionRegion region;
		private final Long2IntOpenHashMap hues = new Long2IntOpenHashMap();
		/** The tint of each vertex so far, as neighboring faces share them, by position to a sixteenth of a block and normal. */
		private final Int2IntOpenHashMap vertices = new Int2IntOpenHashMap();
		/** The block light read so far, plus one so that 0 means not read yet. */
		private final Long2IntOpenHashMap lights = new Long2IntOpenHashMap();
		private final BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();

		Context(BlockPos origin, RenderSectionRegion region) {
			this.origin = origin;
			this.region = region;
		}

		/**
		 * The light's color at a vertex, encoded: the four blocks touching it on the face's outer side, as the
		 * game's smooth lighting blends them, each weighted by its light.
		 */
		int vertexCode(float x, float y, float z, int nx, int ny, int nz) {
			// Within the section and the blocks around it, a sixteenth of a block takes 9 bits per axis
			int key = (Math.round(x * 16) + 32 & 511) | (Math.round(y * 16) + 32 & 511) << 9 | (Math.round(z * 16) + 32 & 511) << 18
					| (nx + 1 + (ny + 1) * 3 + (nz + 1) * 9) << 27;
			// Plus one, so that 0 means not found yet
			int known = vertices.get(key);
			if (known != 0) {
				return known - 1;
			}
			int code = computeCode(x, y, z, nx, ny, nz);
			vertices.put(key, code + 1);
			return code;
		}

		private int computeCode(float x, float y, float z, int nx, int ny, int nz) {
			float px = origin.getX() + x;
			float py = origin.getY() + y;
			float pz = origin.getZ() + z;
			// A light's own faces keep their color: the block behind the face, or the one it is drawn in
			if (emits(px - nx * 0.5F, py - ny * 0.5F, pz - nz * 0.5F) || emits(px - nx * 0.01F, py - ny * 0.01F, pz - nz * 0.01F)) {
				return UNTINTED;
			}
			int bx = floor(px + nx * 0.5F);
			int by = floor(py + ny * 0.5F);
			int bz = floor(pz + nz * 0.5F);
			// Towards the vertex along the two axes of the face, from the middle of the block it sits in
			int sx = nx != 0 ? 0 : px - bx < 0.5F ? -1 : 1;
			int sy = ny != 0 ? 0 : py - by < 0.5F ? -1 : 1;
			int sz = nz != 0 ? 0 : pz - bz < 0.5F ? -1 : 1;
			float r = 0;
			float g = 0;
			float b = 0;
			int light = 0;
			for (int i = 0; i < 4; i++) {
				// The base block, then one step along each face axis, then both
				int ox = 0;
				int oy = 0;
				int oz = 0;
				int first = i & 1;
				int second = i >> 1;
				if (nx != 0) {
					oy = sy * first;
					oz = sz * second;
				} else if (ny != 0) {
					ox = sx * first;
					oz = sz * second;
				} else {
					ox = sx * first;
					oy = sy * second;
				}
				long key = BlockPos.asLong(bx + ox, by + oy, bz + oz);
				int level = blockLight(key);
				int hue;
				DynamicLights.Lit carried = DynamicLights.brightest(bx + ox + 0.5, by + oy + 0.5, bz + oz + 0.5);
				if (carried != null && carried.light() > level) {
					// Carried light outshines the world's here: its color, at its level
					level = Math.round(carried.light());
					hue = carried.color();
				} else if (level == 0) {
					continue;
				} else {
					hue = hue(key, level);
				}
				r += ARGB.red(hue) * level;
				g += ARGB.green(hue) * level;
				b += ARGB.blue(hue) * level;
				light += level;
			}
			// The hue alone: the game's light gives the level, the shader how much of it is the blocks'
			return light == 0 ? UNTINTED : encode(r, g, b);
		}

		private boolean emits(float x, float y, float z) {
			cursor.set(floor(x), floor(y), floor(z));
			return region.getBlockState(cursor).getLightEmission() > 0;
		}

		private static int floor(float value) {
			return (int) Math.floor(value);
		}

		private int blockLight(long key) {
			int known = lights.get(key);
			if (known != 0) {
				return known - 1;
			}
			cursor.set(BlockPos.getX(key), BlockPos.getY(key), BlockPos.getZ(key));
			int level = region.getBrightness(LightLayer.BLOCK, cursor);
			lights.put(key, level + 1);
			return level;
		}

		/**
		 * The color of the light at a block: its own if it emits that level, and that of each neighbor one
		 * level brighter, which is where the game's light came from. Each step up is one level brighter, so
		 * it stops within 15.
		 */
		private int hue(long key, int level) {
			// Every hue is opaque, so 0 means not found yet
			int known = hues.get(key);
			if (known != 0) {
				return known;
			}
			int x = BlockPos.getX(key);
			int y = BlockPos.getY(key);
			int z = BlockPos.getZ(key);
			int r = 0;
			int g = 0;
			int b = 0;
			int count = 0;
			cursor.set(x, y, z);
			BlockState state = region.getBlockState(cursor);
			if (state.getLightEmission() >= level) {
				int own = colors().getOrDefault(state.getBlock(), WHITE);
				r += ARGB.red(own);
				g += ARGB.green(own);
				b += ARGB.blue(own);
				count++;
			}
			if (level < 15) {
				for (Direction direction : Direction.values()) {
					long neighbor = BlockPos.asLong(x + direction.getStepX(), y + direction.getStepY(), z + direction.getStepZ());
					if (blockLight(neighbor) == level + 1) {
						int hue = hue(neighbor, level + 1);
						r += ARGB.red(hue);
						g += ARGB.green(hue);
						b += ARGB.blue(hue);
						count++;
					}
				}
			}
			int hue = count == 0 ? WHITE : ARGB.color(r / count, g / count, b / count);
			hues.put(key, hue);
			return hue;
		}
	}
}
