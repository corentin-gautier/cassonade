package com.cassonade.client.map;

import java.util.Arrays;

import org.jspecify.annotations.Nullable;

import com.mojang.blaze3d.platform.NativeImage;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.Vec3;

import com.cassonade.Cassonade;
import com.cassonade.client.ClientSettings;
import com.cassonade.client.LabeledKeys;
import com.cassonade.client.combat.CombatHud;
import com.cassonade.client.CassonadeClient;
import com.cassonade.compass.Coordinates;
import com.cassonade.compass.Marker;
import com.cassonade.compass.PlayerMarkers;
import com.cassonade.gear.GearSlot;
import com.cassonade.gear.PlayerGear;
import com.cassonade.item.CassonadeItems;
import com.cassonade.waystone.WaystoneListPayload;

/**
 * Minimap shown in the top right corner while a compass is worn, until put away with its key: the terrain around the player in
 * a frame, north up or turning with the player (see {@link MinimapRotationButton}), with the
 * player's markers and the waystones they know, with their names, and with a sculk compass the mobs hunting the player. Below it, the
 * biome in a smaller frame, then the coordinates, in small text.
 */
public final class Minimap {
	/** Blocks covered by the map at each zoom level, one texel each. */
	private static final int[] RANGES = {32, 48, 64, 96, 128, 192, 256};
	private static final int DEFAULT_ZOOM = 3;
	private static final int MAP_SIZE = 108;
	private static final int BORDER = 4;
	private static final int FRAME_SIZE = MAP_SIZE + 2 * BORDER;
	/** In how many ticks the whole map is read again, for the blocks changed: it only serves to find one's way. */
	private static final int REFRESH_TICKS = 40;
	private static final int MARGIN = 4;
	/** How wide a line along the edge of the picture may wait for its chunk, drawn in the color next to it. */
	private static final int EDGE = 4;
	public static final int FRAME_TOP = MARGIN;
	private static final int BIOME_PADDING = 3;
	private static final int COORDINATES_GAP = 5;
	/** A turning map samples this much wider than its range, so its corners stay filled at any angle. */
	private static final float ROTATION_MARGIN = 1.45f;
	private static final int NORTH_COLOR = 0xFFE03030;
	/** The rotation key in the bottom right corner of the map, clear of the toasts, faint so the map stays readable. */
	private static final int KEY_COLOR = 0xA0FFFFFF;
	/** The player's arrow, half again the vanilla map's so it stands out from the other players. */
	private static final float PLAYER_SCALE = 1.5F;
	/** The markers' banners, smaller than the map's so they crowd it less. */
	private static final float BANNER_SCALE = 0.75F;
	private static final float OTHER_PLAYER_SCALE = 1;
	/** The mobs hunting the player: the minimap's red north, outlined in the frame's dark brown to stand out on any terrain. */
	private static final int HUNTER_COLOR = NORTH_COLOR;
	private static final int HUNTER_LIGHT = 0xFFFF7A6A;
	private static final int HUNTER_OUTLINE = 0xFF1A0B0C;
	/** How far under the surface, without sky light, a cave's floor is drawn instead of the surface; and from how much light or how close to it, the surface again. */
	private static final int CAVE_DEPTH = 8;
	private static final int CAVE_EXIT_LIGHT = 3;
	private static final int CAVE_EXIT_DEPTH = 4;
	/** How far above the head a cave's map reaches: at least this, up to the cave's ceiling, read in steps, at most this. */
	private static final int CUT_LOWEST = 4;
	private static final int CUT_STEP = 4;
	private static final int CUT_HIGHEST = 16;
	/** A jump's height in whole blocks, under which the cut stays where the player took off. */
	private static final int JUMP_HEIGHT = 2;

	private static final Identifier TEXTURE = Cassonade.id("minimap");
	private static final Identifier FRAME = FramedPanel.FRAME;

	private static int zoom = -1;
	private static DynamicTexture texture;
	private static byte[] colors = new byte[0];
	/** Where the picture is moved to, then swapped with {@link #colors}. */
	private static byte[] moved = new byte[0];
	private static byte[] row = new byte[0];
	private static int refreshedRow;
	private static Level sampledLevel;
	private static TerrainColors.Cut sampledCut = TerrainColors.SURFACE;
	/** The text under the map, kept while the player stays on a block: a new translated name is parsed again, each frame. */
	private static BlockPos labelsAt;
	private static @Nullable Component biomeName;
	private static Component coordinates;
	private static BlockPos sampledAt;
	private static int sampledRange;
	/** Blocks sampled on each side: the range, or more when the map turns. */
	private static int sampledSize;
	private static int ticksSinceUpdate;
	/** Whether the last sample had columns without color, such as chunks still arriving after a teleport. */
	private static boolean incomplete;
	private static boolean inCave;
	private static int groundY = Integer.MIN_VALUE;

	private Minimap() {
	}

	/** Everything the minimap takes from the top of the screen, so the status effects can go below it. */
	public static int height(Minecraft minecraft) {
		return FRAME_TOP + FRAME_SIZE + 2 + biomePanelHeight(minecraft) + COORDINATES_GAP + SmallText.lineHeight(minecraft.font) + 2;
	}

	private static int biomePanelHeight(Minecraft minecraft) {
		return 2 * (FramedPanel.BORDER + BIOME_PADDING) + SmallText.lineHeight(minecraft.font);
	}

	/** The width of the minimap's frame, which the panels below it share. */
	public static int frameWidth() {
		return FRAME_SIZE;
	}

	public static boolean isShown(Minecraft minecraft) {
		return isWorn(minecraft) && !ClientSettings.minimapHidden() && !minecraft.getDebugOverlay().showDebugScreen();
	}

	private static boolean isWorn(Minecraft minecraft) {
		return minecraft.player != null && PlayerGear.isWearing(minecraft.player, GearSlot.COMPASS);
	}

	/** Puts the minimap away or brings it back; the direction bar and the markers in the world stay. */
	public static void toggleHidden(Minecraft minecraft) {
		if (isWorn(minecraft)) {
			ClientSettings.setMinimapHidden(!ClientSettings.minimapHidden());
			minecraft.gui.hud.setOverlayMessage(Component.translatable(ClientSettings.minimapHidden() ? "gui.cassonade.minimap.hidden" : "gui.cassonade.minimap.shown"), false);
		}
	}

	public static void zoomIn() {
		setZoom(zoom() - 1);
	}

	public static void zoomOut() {
		setZoom(zoom() + 1);
	}

	/** Remembered between games in the client settings. */
	private static int zoom() {
		if (zoom < 0) {
			zoom = ClientSettings.minimapZoom() < 0 ? DEFAULT_ZOOM : Mth.clamp(ClientSettings.minimapZoom(), 0, RANGES.length - 1);
		}
		return zoom;
	}

	private static void setZoom(int level) {
		zoom = Mth.clamp(level, 0, RANGES.length - 1);
		ClientSettings.setMinimapZoom(zoom);
		sampledAt = null;
	}

	/**
	 * Follows the player by moving the picture and reading only the terrain coming into it: wider ranges
	 * do so less often, every 2 ticks up to 128 blocks, every 4 at 256. A few rows are read again each tick
	 * for the blocks changed, the whole map in {@link #REFRESH_TICKS}. Read in full when anything else
	 * changes, and while chunks are missing.
	 */
	public static void tick(Minecraft minecraft) {
		// The direction bar shows the waystones even with the minimap put away
		if (isWorn(minecraft)) {
			WaystoneMarkers.shownTick();
		} else {
			WaystoneMarkers.hiddenTick();
		}
		if (!isShown(minecraft)) {
			sampledAt = null;
			return;
		}
		int range = RANGES[zoom()];
		int size = ClientSettings.minimapRotates() ? Mth.ceil(range * ROTATION_MARGIN / 2) * 2 : range;
		ticksSinceUpdate++;
		BlockPos pos = minecraft.player.blockPosition();
		TerrainColors.Cut cut = cut(minecraft.level, pos.atY(groundY(minecraft.player)));
		boolean sameArea = sampledAt != null && sampledSize == size && sampledLevel == minecraft.level;
		boolean due = ticksSinceUpdate >= Math.max(2, size / 64);
		if (sameArea && !incomplete && cut.equals(sampledCut)) {
			boolean changed = refreshRows(minecraft.level, size, cut);
			int dx = pos.getX() - sampledAt.getX();
			int dz = pos.getZ() - sampledAt.getZ();
			if ((dx != 0 || dz != 0) && due && Math.abs(dx) < size && Math.abs(dz) < size) {
				follow(minecraft.level, pos, dx, dz, size, cut);
				changed = true;
			}
			if ((dx == 0 && dz == 0) || !due || Math.abs(dx) < size && Math.abs(dz) < size) {
				if (changed) {
					upload(size);
				}
				return;
			}
		} else if (sameArea && !due) {
			return;
		}

		if (texture == null || sampledSize != size) {
			texture = new DynamicTexture(() -> "Cassonade minimap", size, size, true);
			// Replaces and closes the texture of the previous zoom level
			minecraft.getTextureManager().register(TEXTURE, texture);
			colors = new byte[size * size];
			moved = new byte[size * size];
			row = new byte[size];
		}
		TerrainColors.sample(minecraft.level, pos.getX() - size / 2, pos.getZ() - size / 2, size, cut, colors);
		upload(size);
		sampledAt = pos;
		sampledRange = range;
		sampledSize = size;
		sampledLevel = minecraft.level;
		sampledCut = cut;
		ticksSinceUpdate = 0;
	}

	/**
	 * The height the cut is read from: the player's, but kept through a jump, which would otherwise shift the cut and read the
	 * whole map again twice. Landing higher or lower, falling, swimming, climbing, flying or riding move it at once.
	 */
	private static int groundY(LocalPlayer player) {
		int y = player.getBlockY();
		boolean jumping = !player.onGround() && !player.getAbilities().flying && !player.isInWater() && !player.onClimbable()
				&& !player.isPassenger() && y >= groundY && y <= groundY + JUMP_HEIGHT;
		if (!jumping) {
			groundY = y;
		}
		return groundY;
	}

	/**
	 * What the map shows: the surface, or under the Nether's roof the floor at the player's height. In a cave of the Overworld,
	 * everything open from the player's feet up to a few blocks over the head, or up to the cave's ceiling when it is higher,
	 * so that a low passage shows the ways just above it and a tall cave its whole floor, the slopes rising from where the
	 * player stands included. The ceiling is read in steps, as each change of cut reads the whole map again.
	 */
	private static TerrainColors.Cut cut(Level level, BlockPos pos) {
		if (level.dimensionType().hasCeiling()) {
			return TerrainColors.Cut.at(pos.getY() + 1);
		}
		if (!underground(level, pos)) {
			return TerrainColors.SURFACE;
		}
		BlockPos.MutableBlockPos above = pos.above(2).mutable();
		int room = 0;
		while (room < CUT_HIGHEST && level.getBlockState(above).getCollisionShape(level, above).isEmpty()) {
			room++;
			above.move(0, 1, 0);
		}
		int over = Math.max(CUT_LOWEST, Mth.roundToward(room, CUT_STEP));
		return new TerrainColors.Cut(pos.getY() + 1 + over, pos.getY());
	}

	/**
	 * Under the Nether's roof, or deep in a cave of the Overworld: the map then shows the floor the player walks on, not the
	 * ground far above. Once in, a little sky light, at a cave's mouth, doesn't bring the surface back, so the map does not
	 * flicker between the two on its edge.
	 */
	private static boolean underground(Level level, BlockPos pos) {
		if (level.dimensionType().hasCeiling()) {
			return true;
		}
		int depth = level.getHeight(Heightmap.Types.WORLD_SURFACE, pos.getX(), pos.getZ()) - pos.getY();
		int skyLight = level.getBrightness(LightLayer.SKY, pos);
		inCave = level.dimensionType().hasSkyLight() && (inCave ? skyLight <= CAVE_EXIT_LIGHT && depth >= CAVE_EXIT_DEPTH
				: skyLight == 0 && depth >= CAVE_DEPTH);
		return inCave;
	}

	/** The packed map color the picture holds for a column, or 0 outside it, for the tests. */
	public static byte colorAt(int x, int z) {
		int column = x - (sampledAt.getX() - sampledSize / 2);
		int row = z - (sampledAt.getZ() - sampledSize / 2);
		return column < 0 || row < 0 || column >= sampledSize || row >= sampledSize ? 0 : colors[column + row * sampledSize];
	}

	/** The slice of the world the picture shows, for the tests. */
	public static TerrainColors.Cut cut() {
		return sampledCut;
	}

	/** Whether the map shows a cave's floor in the Overworld, for the tests. */
	public static boolean inCave() {
		return inCave;
	}

	/** Reads the next rows of the map again, and tells whether they changed. */
	private static boolean refreshRows(Level level, int size, TerrainColors.Cut cut) {
		boolean changed = false;
		for (int i = 0; i < Math.max(1, size / REFRESH_TICKS); i++) {
			refreshedRow = (refreshedRow + 1) % size;
			TerrainColors.sample(level, sampledAt.getX() - size / 2, sampledAt.getZ() - size / 2 + refreshedRow, size, 1, cut, row, 0, size);
			if (!Arrays.equals(row, 0, size, colors, refreshedRow * size, (refreshedRow + 1) * size)) {
				System.arraycopy(row, 0, colors, refreshedRow * size, size);
				changed = true;
			}
		}
		return changed;
	}

	/** Moves the picture with the player, then reads the rows and columns that came into it. */
	private static void follow(Level level, BlockPos pos, int dx, int dz, int size, TerrainColors.Cut cut) {
		for (int z = Math.max(0, -dz); z < size - Math.max(0, dz); z++) {
			System.arraycopy(colors, Math.max(0, dx) + (z + dz) * size, moved, Math.max(0, -dx) + z * size, size - Math.abs(dx));
		}
		byte[] previous = colors;
		colors = moved;
		moved = previous;
		int minX = pos.getX() - size / 2;
		int minZ = pos.getZ() - size / 2;
		if (dz != 0) {
			int firstRow = dz > 0 ? size - dz : 0;
			TerrainColors.sample(level, minX, minZ + firstRow, size, Math.abs(dz), cut, colors, firstRow * size, size);
		}
		if (dx != 0) {
			int firstColumn = dx > 0 ? size - dx : 0;
			TerrainColors.sample(level, minX + firstColumn, minZ, Math.abs(dx), size, cut, colors, firstColumn, size);
		}
		sampledAt = pos;
		ticksSinceUpdate = 0;
	}

	private static void upload(int size) {
		NativeImage pixels = texture.getPixels();
		incomplete = false;
		for (int z = 0; z < size; z++) {
			for (int x = 0; x < size; x++) {
				byte color = colors[x + z * size];
				incomplete |= color == 0;
				// Terrain coming in before its chunk would leave a dark line along the edge until it arrives
				if (color == 0) {
					color = colors[Mth.clamp(x, EDGE, size - 1 - EDGE) + Mth.clamp(z, EDGE, size - 1 - EDGE) * size];
				}
				pixels.setPixel(x, z, MapColor.getColorFromPackedId(color));
			}
		}
		texture.upload();
	}

	/** The share of the picture that differs from the terrain read in full, which moving it and refreshing its rows should keep near 0. */
	public static float differenceFromTerrain(Minecraft minecraft) {
		byte[] terrain = new byte[sampledSize * sampledSize];
		TerrainColors.sample(minecraft.level, sampledAt.getX() - sampledSize / 2, sampledAt.getZ() - sampledSize / 2, sampledSize, sampledCut, terrain);
		int different = 0;
		for (int i = 0; i < terrain.length; i++) {
			different += terrain[i] != colors[i] ? 1 : 0;
		}
		return (float) different / terrain.length;
	}

	public static void extractRenderState(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
		Minecraft minecraft = Minecraft.getInstance();
		if (!isShown(minecraft) || sampledAt == null) {
			return;
		}
		LocalPlayer player = minecraft.player;

		int frameX = frameLeft(graphics.guiWidth());
		int frameY = FRAME_TOP;
		int mapX = frameX + BORDER;
		int mapY = frameY + BORDER;
		int centerX = mapX + MAP_SIZE / 2;
		float yRot = player.getYRot(deltaTracker.getGameTimeDeltaPartialTick(false));
		// Turned so the player's view points up: at yaw 180 the player looks north
		float angle = ClientSettings.minimapRotates() ? (180 - yRot) * Mth.DEG_TO_RAD : 0;
		float sin = Mth.sin(angle);
		float cos = Mth.cos(angle);
		float centerY = mapY + MAP_SIZE / 2f;

		graphics.fill(mapX, mapY, mapX + MAP_SIZE, mapY + MAP_SIZE, FramedPanel.BACKGROUND_COLOR);
		graphics.enableScissor(mapX, mapY, mapX + MAP_SIZE, mapY + MAP_SIZE);
		graphics.pose().pushMatrix();
		graphics.pose().translate(mapX + MAP_SIZE / 2f, centerY);
		graphics.pose().rotate(angle);
		float drawnSize = (float) MAP_SIZE * sampledSize / sampledRange;
		graphics.pose().scale(drawnSize / sampledSize, drawnSize / sampledSize);
		graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, -sampledSize / 2, -sampledSize / 2, 0, 0, sampledSize, sampledSize, sampledSize, sampledSize);
		graphics.pose().popMatrix();

		// Names under their icon, cut at the edge of the map, none on top of another
		LabelSpots labels = new LabelSpots();
		for (WaystoneListPayload.Destination waystone : WaystoneMarkers.here(player.level())) {
			BlockPos pos = waystone.pos().pos();
			float[] at = onMap(mapX, mapY, pos.getX() - sampledAt.getX(), pos.getZ() - sampledAt.getZ(), sin, cos);
			float half = MapDecorations.waystone(graphics, at[0], at[1], BANNER_SCALE);
			if (!waystone.name().isEmpty()) {
				name(graphics, labels, minecraft.font, Component.literal(waystone.name()), at[0], at[1] + half + 1);
			}
		}
		for (Marker marker : PlayerMarkers.of(player).markers()) {
			if (marker.pos().dimension() == player.level().dimension()) {
				BlockPos pos = marker.pos().pos();
				float[] at = onMap(mapX, mapY, pos.getX() - sampledAt.getX(), pos.getZ() - sampledAt.getZ(), sin, cos);
				float half = MapDecorations.banner(graphics, marker.color(), at[0], at[1], BANNER_SCALE);
				if (!marker.name().isEmpty()) {
					name(graphics, labels, minecraft.font, Component.literal(marker.name()), at[0], at[1] + half + 1);
				}
			}
		}
		for (LodestoneMarkers.Lodestone lodestone : LodestoneMarkers.here(player)) {
			float[] at = onMap(mapX, mapY, lodestone.pos().getX() - sampledAt.getX(), lodestone.pos().getZ() - sampledAt.getZ(), sin, cos);
			MapDecorations.lodestone(graphics, at[0], at[1], BANNER_SCALE);
		}
		DeathMarker.here(player).ifPresent(pos -> {
			float[] at = onMap(mapX, mapY, pos.getX() - sampledAt.getX(), pos.getZ() - sampledAt.getZ(), sin, cos);
			MapDecorations.death(graphics, at[0], at[1], BANNER_SCALE);
		});
		float partialTick = deltaTracker.getGameTimeDeltaPartialTick(false);
		// The other players the sculk compass senses, named, on the edge of the map when out of range, as far as
		// the client knows of them; not the invisible ones
		if (PlayerGear.isWearing(player, CassonadeItems.SCULK_COMPASS)) {
			for (Player other : player.level().players()) {
				if (other != player && !other.isSpectator() && !other.isInvisibleTo(player)) {
					Vec3 at = other.getPosition(partialTick);
					float[] dot = onMap(mapX, mapY, (float) (at.x - 0.5 - sampledAt.getX()), (float) (at.z - 0.5 - sampledAt.getZ()), sin, cos);
					MapDecorations.otherPlayer(graphics, dot[0], dot[1], other.getYRot(partialTick) + (ClientSettings.minimapRotates() ? 180 - yRot : 0), OTHER_PLAYER_SCALE);
					name(graphics, labels, minecraft.font, other.getName(), dot[0], dot[1] + 4 * OTHER_PLAYER_SCALE + 1);
				}
			}
		}
		// The mobs hunting the player, which the sculk compass senses: on the edge of the map when out of range,
		// so it still tells where they come from. Those too far for the client to know are left out.
		for (int id : CombatHud.hunters()) {
			Entity mob = player.level().getEntity(id);
			if (mob != null) {
				Vec3 at = mob.getPosition(partialTick);
				// Block centers line up with the markers, which sit on whole blocks
				float[] dot = onMap(mapX, mapY, (float) (at.x - 0.5 - sampledAt.getX()), (float) (at.z - 0.5 - sampledAt.getZ()), sin, cos);
				hunter(graphics, Math.round(dot[0]), Math.round(dot[1]));
			}
		}
		if (ClientSettings.minimapRotates()) {
			// North on the edge of the map
			float distance = MAP_SIZE / 2f - 6;
			SmallText.centered(graphics, minecraft.font, Component.literal("N"), centerX + sin * distance,
					centerY - cos * distance - SmallText.lineHeight(minecraft.font) / 2f, NORTH_COLOR);
		}
		graphics.disableScissor();
		graphics.blitSprite(RenderPipelines.GUI_TEXTURED, FRAME, frameX, frameY, FRAME_SIZE, FRAME_SIZE);
		MapDecorations.player(graphics, centerX, centerY, ClientSettings.minimapRotates() ? 180 : yRot, PLAYER_SCALE);
		if (!CassonadeClient.MINIMAP_ROTATION.isUnbound()) {
			Component key = LabeledKeys.printedName(CassonadeClient.MINIMAP_ROTATION);
			SmallText.left(graphics, minecraft.font, key, mapX + MAP_SIZE - 3 - SmallText.width(minecraft.font, key),
					mapY + MAP_SIZE - 3 - SmallText.lineHeight(minecraft.font), KEY_COLOR);
		}
		// The key that puts it away, in the other corner
		if (!CassonadeClient.HIDE_MINIMAP.isUnbound()) {
			SmallText.left(graphics, minecraft.font, LabeledKeys.printedName(CassonadeClient.HIDE_MINIMAP), mapX + 3,
					mapY + MAP_SIZE - 3 - SmallText.lineHeight(minecraft.font), KEY_COLOR);
		}

		// The biome in its own frame, centered under the map, the coordinates below it
		BlockPos pos = player.blockPosition();
		int panelY = frameY + FRAME_SIZE + 2;
		int panelHeight = biomePanelHeight(minecraft);
		if (!pos.equals(labelsAt)) {
			labelsAt = pos;
			biomeName = player.level().getBiome(pos).unwrapKey().map(biome -> (Component) Component.translatable(biome.identifier().toLanguageKey("biome"))).orElse(null);
			coordinates = Component.literal(Coordinates.of(pos));
		}
		if (biomeName != null) {
			int panelWidth = Math.min(FRAME_SIZE, SmallText.width(minecraft.font, biomeName) + 2 * (FramedPanel.BORDER + BIOME_PADDING));
			FramedPanel.draw(graphics, centerX - panelWidth / 2, panelY, panelWidth, panelHeight);
			SmallText.centered(graphics, minecraft.font, biomeName, centerX, panelY + FramedPanel.BORDER + BIOME_PADDING, 0xFFFFFFFF);
		}
		SmallText.centered(graphics, minecraft.font, coordinates, centerX, panelY + panelHeight + COORDINATES_GAP, 0xFFFFFFFF);
	}

	/** A name centered on x, at y or further down if another is already there. */
	private static void name(GuiGraphicsExtractor graphics, LabelSpots labels, Font font, Component name, float x, float y) {
		int height = SmallText.lineHeight(font);
		float top = labels.place(x, y, SmallText.width(font, name), height, height);
		if (!Float.isNaN(top)) {
			SmallText.centered(graphics, font, name, x, top, 0xFFFFFFFF);
		}
	}

	/** A round red dot five pixels wide, lit on its top left like the game's pixel art. */
	private static void hunter(GuiGraphicsExtractor graphics, int x, int y) {
		graphics.fill(x - 2, y - 1, x + 3, y + 2, HUNTER_OUTLINE);
		graphics.fill(x - 1, y - 2, x + 2, y + 3, HUNTER_OUTLINE);
		graphics.fill(x - 1, y - 1, x + 2, y + 2, HUNTER_COLOR);
		graphics.fill(x - 1, y - 1, x, y, HUNTER_LIGHT);
	}

	public static int frameLeft(int guiWidth) {
		return guiWidth - MARGIN - FRAME_SIZE;
	}

	/**
	 * Screen position of an offset in blocks from the map center, turned with the map, kept on the map
	 * when out of range.
	 */
	private static float[] onMap(int mapX, int mapY, float dx, float dz, float sin, float cos) {
		float scale = (float) MAP_SIZE / sampledRange;
		float limit = MAP_SIZE / 2f - 4;
		float x = Mth.clamp((dx * cos - dz * sin) * scale, -limit, limit);
		float y = Mth.clamp((dx * sin + dz * cos) * scale, -limit, limit);
		return new float[] {mapX + MAP_SIZE / 2f + x, mapY + MAP_SIZE / 2f + y};
	}
}
