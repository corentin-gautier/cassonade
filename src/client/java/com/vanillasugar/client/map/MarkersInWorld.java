package com.vanillasugar.client.map;

import org.joml.Matrix4f;
import org.joml.Vector4f;

import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;

import com.vanillasugar.client.ClientSettings;
import com.vanillasugar.client.ClientSettings.MarkerDisplay;
import com.vanillasugar.compass.Marker;
import com.vanillasugar.compass.PlayerMarkers;
import com.vanillasugar.gear.GearSlot;
import com.vanillasugar.gear.PlayerGear;

/**
 * Shows the markers in the world while a compass is worn: each one's banner, twice the map size, with
 * one discreet line above for its name and distance unless the player chose icons only. Drawn on the
 * HUD where the marker is on screen, so they are seen through terrain and from afar.
 */
public final class MarkersInWorld {
	private static final float BANNER_SCALE = 2;
	private static final int TEXT_COLOR = 0xFFFFFFFF;
	private static final int DISTANCE_COLOR = 0xFFA0A0A0;
	private static final int TEXT_BACKGROUND = 0x40000000;
	/** Markers right next to the player are not drawn, they would only hide what is there. */
	private static final double MIN_DISTANCE = 3;

	private MarkersInWorld() {
	}

	public static void extractRenderState(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
		Minecraft minecraft = Minecraft.getInstance();
		LocalPlayer player = minecraft.player;
		MarkerDisplay display = ClientSettings.markerDisplay();
		if (player == null || display == MarkerDisplay.NONE || !PlayerGear.isWearing(player, GearSlot.COMPASS)
				|| minecraft.getDebugOverlay().showDebugScreen()) {
			return;
		}

		Camera camera = minecraft.gameRenderer.mainCamera();
		Vec3 eye = camera.position();
		Matrix4f viewProjection = camera.getViewRotationProjectionMatrix(new Matrix4f());
		Font font = minecraft.font;
		for (Marker marker : PlayerMarkers.of(player).markers()) {
			if (marker.pos().dimension() != player.level().dimension()) {
				continue;
			}
			// A little above the ground the marker was placed on
			BlockPos pos = marker.pos().pos();
			Vec3 target = new Vec3(pos.getX() + 0.5, pos.getY() + 1.5, pos.getZ() + 0.5);
			double distance = player.position().distanceTo(target);
			if (distance < MIN_DISTANCE) {
				continue;
			}

			Vector4f clip = viewProjection.transform(new Vector4f(
					(float) (target.x - eye.x), (float) (target.y - eye.y), (float) (target.z - eye.z), 1));
			if (clip.w <= 0) {
				continue;
			}
			float x = (clip.x / clip.w + 1) / 2 * graphics.guiWidth();
			float y = (1 - clip.y / clip.w) / 2 * graphics.guiHeight();
			if (x < 0 || x > graphics.guiWidth() || y < 0 || y > graphics.guiHeight()) {
				continue;
			}

			MapDecorations.banner(graphics, marker.color(), x, y, BANNER_SCALE);
			if (display == MarkerDisplay.ICON_AND_TEXT) {
				label(graphics, font, marker, distance, x, y - 4 * BANNER_SCALE - 2 - font.lineHeight);
			}
		}
	}

	/** "Name · 40 m" centered on x, the distance in gray, on a faint background like name tags. */
	private static void label(GuiGraphicsExtractor graphics, Font font, Marker marker, double distance, float x, float y) {
		Component text = Component.translatable("gui.vanillasugar.marker.distance", Math.round(distance)).withColor(DISTANCE_COLOR);
		if (!marker.name().isEmpty()) {
			text = Component.literal(marker.name() + " · ").withColor(TEXT_COLOR).append(text);
		}
		int width = font.width(text);
		int left = Math.round(x) - width / 2;
		int top = Math.round(y);
		graphics.fill(left - 2, top - 1, left + width + 2, top + font.lineHeight, TEXT_BACKGROUND);
		graphics.text(font, text, left, top, TEXT_COLOR, false);
	}
}
