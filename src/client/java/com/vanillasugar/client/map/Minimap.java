package com.vanillasugar.client.map;

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
import net.minecraft.world.level.material.MapColor;

import com.vanillasugar.VanillaSugar;
import com.vanillasugar.client.ClientSettings;
import com.vanillasugar.compass.Coordinates;
import com.vanillasugar.compass.Marker;
import com.vanillasugar.compass.PlayerMarkers;
import com.vanillasugar.gear.GearSlot;
import com.vanillasugar.gear.PlayerGear;

/**
 * Minimap shown in the top right corner while a compass is worn: the terrain around the player in
 * a frame, north up or turning with the player (see {@link MinimapRotationButton}), with the
 * player's markers and their names. Below it, the biome in a smaller frame, then the coordinates, in
 * small text.
 */
public final class Minimap {
	/** Blocks covered by the map at each zoom level, one texel each. */
	private static final int[] RANGES = {32, 48, 64, 96, 128, 192, 256};
	private static final int DEFAULT_ZOOM = 3;
	private static final int MAP_SIZE = 108;
	private static final int BORDER = 4;
	private static final int FRAME_SIZE = MAP_SIZE + 2 * BORDER;
	private static final int MARGIN = 4;
	public static final int FRAME_TOP = MARGIN;
	private static final int BIOME_PADDING = 3;
	private static final int COORDINATES_GAP = 5;
	/** A turning map samples this much wider than its range, so its corners stay filled at any angle. */
	private static final float ROTATION_MARGIN = 1.45f;
	private static final int NORTH_COLOR = 0xFFE03030;
	/** The player's arrow, twice the size of the markers' banners. */
	private static final float PLAYER_SCALE = 2;

	private static final Identifier TEXTURE = VanillaSugar.id("minimap");
	private static final Identifier FRAME = FramedPanel.FRAME;

	private static int zoom = -1;
	private static DynamicTexture texture;
	private static byte[] colors = new byte[0];
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
	 * Re-samples the terrain when the player moved, or while chunks are missing. Wider ranges cost more,
	 * so they are refreshed less often: every 2 ticks up to 128 blocks, every 4 at 256.
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
		if (sampledAt != null && sampledSize == size
				&& (pos.equals(sampledAt) && !incomplete && ticksSinceUpdate < 20 || ticksSinceUpdate < Math.max(2, size / 64))) {
			return;
		}

		if (texture == null || sampledSize != size) {
			texture = new DynamicTexture(() -> "Vanilla Sugar minimap", size, size, true);
			// Replaces and closes the texture of the previous zoom level
			minecraft.getTextureManager().register(TEXTURE, texture);
			colors = new byte[size * size];
		}
		int ceiling = minecraft.level.dimensionType().hasCeiling() ? pos.getY() + 1 : TerrainColors.NO_CEILING;
		TerrainColors.sample(minecraft.level, pos.getX() - size / 2, pos.getZ() - size / 2, size, ceiling, colors);
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
		sampledAt = pos;
		sampledRange = range;
		sampledSize = size;
		ticksSinceUpdate = 0;
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
				MapDecorations.banner(graphics, marker.color(), at[0], at[1]);
				if (!marker.name().isEmpty()) {
					SmallText.centered(graphics, minecraft.font, Component.literal(marker.name()), at[0], at[1] + 5, 0xFFFFFFFF);
				}
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

		// The biome in its own frame, centered under the map, the coordinates below it
		BlockPos pos = player.blockPosition();
		int panelY = frameY + FRAME_SIZE + 2;
		int panelHeight = biomePanelHeight(minecraft);
		player.level().getBiome(pos).unwrapKey().ifPresent(biome -> {
			Component name = Component.translatable(biome.identifier().toLanguageKey("biome"));
			int panelWidth = Math.min(FRAME_SIZE, SmallText.width(minecraft.font, name) + 2 * (FramedPanel.BORDER + BIOME_PADDING));
			FramedPanel.draw(graphics, centerX - panelWidth / 2, panelY, panelWidth, panelHeight);
			SmallText.centered(graphics, minecraft.font, name, centerX, panelY + FramedPanel.BORDER + BIOME_PADDING, 0xFFFFFFFF);
		});
		SmallText.centered(graphics, minecraft.font, Component.literal(Coordinates.of(pos)), centerX, panelY + panelHeight + COORDINATES_GAP, 0xFFFFFFFF);
	}

	public static int frameLeft(int guiWidth) {
		return guiWidth - MARGIN - FRAME_SIZE;
	}

	/**
	 * Screen position of an offset in blocks from the map center, turned with the map, kept on the map
	 * when out of range.
	 */
	private static float[] onMap(int mapX, int mapY, int dx, int dz, float sin, float cos) {
		float scale = (float) MAP_SIZE / sampledRange;
		float limit = MAP_SIZE / 2f - 4;
		float x = Mth.clamp((dx * cos - dz * sin) * scale, -limit, limit);
		float y = Mth.clamp((dx * sin + dz * cos) * scale, -limit, limit);
		return new float[] {mapX + MAP_SIZE / 2f + x, mapY + MAP_SIZE / 2f + y};
	}
}
