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
import com.vanillasugar.compass.Marker;
import com.vanillasugar.compass.PlayerMarkers;
import com.vanillasugar.gear.GearSlot;
import com.vanillasugar.gear.PlayerGear;

/**
 * Minimap shown in the top right corner while a compass is worn: the terrain around the player in
 * a metal frame, north up, with the player's markers and their names, its coordinates and the biome
 * below in small text.
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
	private static final int BACKGROUND_COLOR = 0x80000000;

	private static final Identifier TEXTURE = VanillaSugar.id("minimap");
	// Nine-slice metal frame, see tools/gui_textures.py
	private static final Identifier FRAME = VanillaSugar.id("hud/minimap_frame");

	private static int zoom = -1;
	private static DynamicTexture texture;
	private static byte[] colors = new byte[0];
	private static BlockPos sampledAt;
	private static int sampledRange;
	private static int ticksSinceUpdate;
	/** Whether the last sample had columns without color, such as chunks still arriving after a teleport. */
	private static boolean incomplete;

	private Minimap() {
	}

	/** Everything the minimap takes from the top of the screen, so the status effects can go below it. */
	public static int height(Minecraft minecraft) {
		return FRAME_TOP + FRAME_SIZE + 2 + 2 * (SmallText.lineHeight(minecraft.font) + 1);
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
		ticksSinceUpdate++;
		BlockPos pos = minecraft.player.blockPosition();
		if (sampledAt != null && (pos.equals(sampledAt) && !incomplete && ticksSinceUpdate < 20 || ticksSinceUpdate < Math.max(2, range / 64))) {
			return;
		}

		if (texture == null || sampledRange != range) {
			texture = new DynamicTexture(() -> "Vanilla Sugar minimap", range, range, true);
			// Replaces and closes the texture of the previous zoom level
			minecraft.getTextureManager().register(TEXTURE, texture);
			colors = new byte[range * range];
		}
		int ceiling = minecraft.level.dimensionType().hasCeiling() ? pos.getY() + 1 : TerrainColors.NO_CEILING;
		TerrainColors.sample(minecraft.level, pos.getX() - range / 2, pos.getZ() - range / 2, range, ceiling, colors);
		NativeImage pixels = texture.getPixels();
		incomplete = false;
		for (int z = 0; z < range; z++) {
			for (int x = 0; x < range; x++) {
				byte color = colors[x + z * range];
				incomplete |= color == 0;
				pixels.setPixel(x, z, MapColor.getColorFromPackedId(color));
			}
		}
		texture.upload();
		sampledAt = pos;
		sampledRange = range;
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
		graphics.fill(mapX, mapY, mapX + MAP_SIZE, mapY + MAP_SIZE, BACKGROUND_COLOR);
		graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, mapX, mapY, 0, 0, MAP_SIZE, MAP_SIZE, sampledRange, sampledRange, sampledRange, sampledRange);
		graphics.blitSprite(RenderPipelines.GUI_TEXTURED, FRAME, frameX, frameY, FRAME_SIZE, FRAME_SIZE);

		// Names under their banner, cut at the edge of the map
		graphics.enableScissor(mapX, mapY, mapX + MAP_SIZE, mapY + MAP_SIZE);
		for (Marker marker : PlayerMarkers.of(player).markers()) {
			if (marker.pos().dimension() == player.level().dimension()) {
				BlockPos pos = marker.pos().pos();
				float x = onMap(mapX, pos.getX() - sampledAt.getX());
				float y = onMap(mapY, pos.getZ() - sampledAt.getZ());
				MapDecorations.banner(graphics, marker.color(), x, y);
				if (!marker.name().isEmpty()) {
					SmallText.centered(graphics, minecraft.font, Component.literal(marker.name()), x, y + 5, 0xFFFFFFFF);
				}
			}
		}
		graphics.disableScissor();
		MapDecorations.player(graphics, onMap(mapX, 0), onMap(mapY, 0), player.getYRot(deltaTracker.getGameTimeDeltaPartialTick(false)));

		BlockPos pos = player.blockPosition();
		int lineHeight = SmallText.lineHeight(minecraft.font) + 1;
		int textY = frameY + FRAME_SIZE + 2;
		SmallText.centered(graphics, minecraft.font, Component.literal(pos.getX() + ", " + pos.getY() + ", " + pos.getZ()), centerX, textY, 0xFFFFFFFF);
		player.level().getBiome(pos).unwrapKey().ifPresent(biome -> SmallText.centered(graphics, minecraft.font,
				Component.translatable(biome.identifier().toLanguageKey("biome")), centerX, textY + lineHeight, 0xFFFFFFFF));
	}

	public static int frameLeft(int guiWidth) {
		return guiWidth - MARGIN - FRAME_SIZE;
	}

	/** Screen position of an offset in blocks from the map center, kept on the map when out of range. */
	private static float onMap(int mapStart, int offset) {
		float limit = MAP_SIZE / 2f - 4;
		return mapStart + MAP_SIZE / 2f + Mth.clamp(offset * (float) MAP_SIZE / sampledRange, -limit, limit);
	}
}
