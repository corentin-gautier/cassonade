package com.cassonade.client.map;

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

import com.cassonade.client.ClientSettings;
import com.cassonade.client.ClientSettings.MarkerDisplay;
import com.cassonade.compass.Marker;
import com.cassonade.compass.PlayerMarkers;
import com.cassonade.gear.GearSlot;
import com.cassonade.gear.PlayerGear;

/**
 * Shows the markers in the world while a compass is worn: each one's banner, with one
 * small line above for its name and distance unless the player chose icons only. Both shrink with the
 * distance, down to a size still easy to read, so that many markers far away stay discreet. Drawn on the
 * HUD where the marker is on screen, so they are seen through terrain and from afar. The red X where the
 * player last died too (DeathMarker).
 */
public final class MarkersInWorld {
	/** Closer than this, markers are full size; farther than FAR, smallest. */
	private static final double NEAR = 16;
	private static final double FAR = 256;
	/** The banners' and the X's scale, times the map's 8 pixels: full size, then smallest. */
	private static final float ICON_NEAR = 1;
	private static final float ICON_FAR = 0.5F;
	private static final int TEXT_COLOR = 0xFFFFFFFF;
	private static final int DISTANCE_COLOR = 0xFFA0A0A0;
	private static final int TEXT_BACKGROUND = 0x60000000;
	/** Markers right next to the player are not drawn, they would only hide what is there. */
	private static final double MIN_DISTANCE = 3;
	private static final Component DEATH_NAME = Component.translatable("gui.cassonade.marker.death");

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
		int guiScale = minecraft.getWindow().getGuiScale();
		for (Marker marker : PlayerMarkers.of(player).markers()) {
			if (marker.pos().dimension() == player.level().dimension()) {
				draw(graphics, font, player, marker.pos().pos(), marker.name(), display, viewProjection, eye, guiScale,
						(x, y, size) -> MapDecorations.banner(graphics, marker.color(), x, y, iconScale(size)));
			}
		}
		DeathMarker.here(player).ifPresent(pos -> draw(graphics, font, player, pos, DEATH_NAME.getString(), display, viewProjection, eye, guiScale,
				(x, y, size) -> MapDecorations.death(graphics, x, y, iconScale(size))));
	}

	private static float iconScale(float size) {
		return ICON_FAR + (ICON_NEAR - ICON_FAR) * size;
	}

	/** The marker's icon where its place is on screen, a little above the ground, and its label above it. */
	private static void draw(GuiGraphicsExtractor graphics, Font font, LocalPlayer player, BlockPos pos, String name, MarkerDisplay display,
			Matrix4f viewProjection, Vec3 eye, int guiScale, Icon icon) {
		Vec3 target = new Vec3(pos.getX() + 0.5, pos.getY() + 1.5, pos.getZ() + 0.5);
		double distance = player.position().distanceTo(target);
		if (distance < MIN_DISTANCE) {
			return;
		}

		Vector4f clip = viewProjection.transform(new Vector4f(
				(float) (target.x - eye.x), (float) (target.y - eye.y), (float) (target.z - eye.z), 1));
		if (clip.w <= 0) {
			return;
		}
		float x = (clip.x / clip.w + 1) / 2 * graphics.guiWidth();
		float y = (1 - clip.y / clip.w) / 2 * graphics.guiHeight();
		if (x < 0 || x > graphics.guiWidth() || y < 0 || y > graphics.guiHeight()) {
			return;
		}

		// 1 near, 0 far, the size in between
		float size = (float) (1 - Math.clamp((distance - NEAR) / (FAR - NEAR), 0, 1));
		float halfHeight = icon.draw(x, y, size);
		if (display == MarkerDisplay.ICON_AND_TEXT) {
			label(graphics, font, name, distance, x, y - halfHeight - 1, textScale(guiScale, size));
		}
	}

	private interface Icon {
		/** Draws the icon centered on (x, y), and returns its half height. */
		float draw(float x, float y, float size);
	}

	/**
	 * The label's scale: about half the usual size near, a third far, in whole screen pixels per font pixel
	 * so it stays sharp, and at least one: the size of text at GUI scale 1.
	 */
	private static float textScale(int guiScale, float size) {
		int near = Math.max(1, (guiScale + 1) / 2);
		int far = Math.max(1, guiScale / 3);
		return Math.round(far + (near - far) * size) / (float) guiScale;
	}

	/** "Name · 40 m" centered on x, its bottom at y, the distance in gray, on a faint background like name tags. */
	private static void label(GuiGraphicsExtractor graphics, Font font, String name, double distance, float x, float bottom, float scale) {
		Component text = Component.translatable("gui.cassonade.marker.distance", Math.round(distance)).withColor(DISTANCE_COLOR);
		if (!name.isEmpty()) {
			text = Component.literal(name + " · ").withColor(TEXT_COLOR).append(text);
		}
		int width = font.width(text);
		graphics.pose().pushMatrix();
		graphics.pose().translate(x, bottom);
		graphics.pose().scale(scale, scale);
		int left = -width / 2;
		int top = -font.lineHeight;
		graphics.fill(left - 2, top - 1, left + width + 2, top + font.lineHeight, TEXT_BACKGROUND);
		graphics.text(font, text, left, top, TEXT_COLOR, false);
		graphics.pose().popMatrix();
	}
}
