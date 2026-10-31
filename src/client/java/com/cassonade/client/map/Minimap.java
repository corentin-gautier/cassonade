package com.cassonade.client.map;

import java.util.Arrays;

import org.jspecify.annotations.Nullable;

import com.mojang.blaze3d.platform.NativeImage;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
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

/**
 * Minimap shown in the top right corner while a compass is worn: the terrain around the player in
 * a frame, north up or turning with the player (see {@link MinimapRotationButton}), with the
 * player's markers and their names, and with a sculk compass the mobs hunting the player. Below it, the
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
	private static int sampledCeiling;
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
		return minecraft.player != null && PlayerGear.isWearing(minecraft.player, GearSlot.COMPASS)
				&& !minecraft.getDebugOverlay().showDebugScreen();
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
		if (!isShown(minecraft)) {
			sampledAt = null;
			return;
		}
		int range = RANGES[zoom()];
		int size = ClientSettings.minimapRotates() ? Mth.ceil(range * ROTATION_MARGIN / 2) * 2 : range;
		ticksSinceUpdate++;
		BlockPos pos = minecraft.player.blockPosition();
		int ceiling = minecraft.level.dimensionType().hasCeiling() ? pos.getY() + 1 : TerrainColors.NO_CEILING;
		boolean sameArea = sampledAt != null && sampledSize == size && sampledLevel == minecraft.level;
		boolean due = ticksSinceUpdate >= Math.max(2, size / 64);
		if (sameArea && !incomplete && ceiling == sampledCeiling) {
			boolean changed = refreshRows(minecraft.level, size, ceiling);
			int dx = pos.getX() - sampledAt.getX();
			int dz = pos.getZ() - sampledAt.getZ();
			if ((dx != 0 || dz != 0) && due && Math.abs(dx) < size && Math.abs(dz) < size) {
				follow(minecraft.level, pos, dx, dz, size, ceiling);
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
		TerrainColors.sample(minecraft.level, pos.getX() - size / 2, pos.getZ() - size / 2, size, ceiling, colors);
		upload(size);
		sampledAt = pos;
		sampledRange = range;
		sampledSize = size;
		sampledLevel = minecraft.level;
		sampledCeiling = ceiling;
		ticksSinceUpdate = 0;
	}

	/** Reads the next rows of the map again, and tells whether they changed. */
	private static boolean refreshRows(Level level, int size, int ceiling) {
		boolean changed = false;
		for (int i = 0; i < Math.max(1, size / REFRESH_TICKS); i++) {
			refreshedRow = (refreshedRow + 1) % size;
			TerrainColors.sample(level, sampledAt.getX() - size / 2, sampledAt.getZ() - size / 2 + refreshedRow, size, 1, ceiling, row, 0, size);
			if (!Arrays.equals(row, 0, size, colors, refreshedRow * size, (refreshedRow + 1) * size)) {
				System.arraycopy(row, 0, colors, refreshedRow * size, size);
				changed = true;
			}
		}
		return changed;
	}

	/** Moves the picture with the player, then reads the rows and columns that came into it. */
	private static void follow(Level level, BlockPos pos, int dx, int dz, int size, int ceiling) {
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
			TerrainColors.sample(level, minX, minZ + firstRow, size, Math.abs(dz), ceiling, colors, firstRow * size, size);
		}
		if (dx != 0) {
			int firstColumn = dx > 0 ? size - dx : 0;
			TerrainColors.sample(level, minX + firstColumn, minZ, Math.abs(dx), size, ceiling, colors, firstColumn, size);
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
				pixels.setPixel(x, z, MapColor.getColorFromPackedId(color));
			}
		}
		texture.upload();
	}

	/** The share of the picture that differs from the terrain read in full, which moving it and refreshing its rows should keep near 0. */
	public static float differenceFromTerrain(Minecraft minecraft) {
		byte[] terrain = new byte[sampledSize * sampledSize];
		TerrainColors.sample(minecraft.level, sampledAt.getX() - sampledSize / 2, sampledAt.getZ() - sampledSize / 2, sampledSize, sampledCeiling, terrain);
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

		// Names under their banner, cut at the edge of the map
		for (Marker marker : PlayerMarkers.of(player).markers()) {
			if (marker.pos().dimension() == player.level().dimension()) {
				BlockPos pos = marker.pos().pos();
				float[] at = onMap(mapX, mapY, pos.getX() - sampledAt.getX(), pos.getZ() - sampledAt.getZ(), sin, cos);
				float half = MapDecorations.banner(graphics, marker.color(), at[0], at[1], BANNER_SCALE);
				if (!marker.name().isEmpty()) {
					SmallText.centered(graphics, minecraft.font, Component.literal(marker.name()), at[0], at[1] + half + 1, 0xFFFFFFFF);
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
					SmallText.centered(graphics, minecraft.font, other.getName(), dot[0], dot[1] + 4 * OTHER_PLAYER_SCALE + 1, 0xFFFFFFFF);
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
