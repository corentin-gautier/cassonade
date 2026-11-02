package com.cassonade.client.map;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

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

import com.cassonade.client.CassonadeClient;
import com.cassonade.client.ClientSettings;
import com.cassonade.client.ClientSettings.MarkerDisplay;
import com.cassonade.compass.Marker;
import com.cassonade.compass.PlayerMarkers;
import com.cassonade.gear.GearSlot;
import com.cassonade.gear.PlayerGear;
import com.cassonade.waystone.WaystoneListPayload;

/**
 * Shows where the markers and the waystones known are while a compass is worn: on the DirectionBar and, once near,
 * in the world; or all in the world if the player chose so, each one's icon with one small line above for its name
 * and distance. Both shrink with the distance, down to a size still easy to read. Drawn on the HUD where the marker
 * is on screen, so they are seen through terrain and from afar. The red X where the player last died always shows
 * in the world (DeathMarker) and so, compass worn or not, does what players point out (Pings).
 */
public final class MarkersInWorld {
	/** Closer than this, markers are full size; farther than FAR, smallest. */
	private static final double NEAR = 16;
	private static final double FAR = 256;
	/** The banners' and the X's scale, times the map's 8 pixels: full size, then smallest. */
	private static final float ICON_NEAR = 1.25F;
	private static final float ICON_FAR = 0.75F;
	private static final int TEXT_COLOR = 0xFFFFFFFF;
	private static final int DISTANCE_COLOR = 0xFFA0A0A0;
	private static final int TEXT_BACKGROUND = 0x60000000;
	private static final int TEXT_MARGIN = 2;
	/** Markers right next to the player are not drawn, they would only hide what is there. */
	private static final double MIN_DISTANCE = 3;
	/** Over the waystone's turning crystal, two blocks and a half up. */
	private static final double WAYSTONE_TOP = 3;
	/** With the direction bar, how far along the ground a marker also shows in the world. */
	private static final double BAR_NEAR = 32;
	private static final Component DEATH_NAME = Component.translatable("gui.cassonade.marker.death");

	private MarkersInWorld() {
	}

	public static void extractRenderState(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
		Minecraft minecraft = Minecraft.getInstance();
		LocalPlayer player = minecraft.player;
		if (player == null || minecraft.getDebugOverlay().showDebugScreen()) {
			return;
		}

		Camera camera = minecraft.gameRenderer.mainCamera();
		Vec3 eye = camera.position();
		Matrix4f viewProjection = camera.getViewRotationProjectionMatrix(new Matrix4f());
		Font font = minecraft.font;
		int guiScale = minecraft.getWindow().getGuiScale();
		List<Label> labels = new ArrayList<>();
		// What other players point out, compass worn or not
		float partialTick = deltaTracker.getGameTimeDeltaPartialTick(false);
		for (Pings.Ping ping : Pings.shown()) {
			draw(graphics, font, player, ping.position(minecraft, partialTick), ping.player(), true, viewProjection, eye,
					guiScale, labels, (x, y, size) -> MapDecorations.ping(graphics, x, y, iconScale(size)));
		}

		// The key held shows them all in the world for a moment, whatever the choice
		MarkerDisplay display = CassonadeClient.MARKERS_IN_WORLD.isDown() ? MarkerDisplay.WORLD : ClientSettings.markerDisplay();
		if (display != MarkerDisplay.NONE && PlayerGear.isWearing(player, GearSlot.COMPASS)) {
			List<Place> places = new ArrayList<>();
			for (WaystoneListPayload.Destination waystone : WaystoneMarkers.here(player.level())) {
				BlockPos pos = waystone.pos().pos();
				places.add(new Place(new Vec3(pos.getX() + 0.5, pos.getY() + WAYSTONE_TOP, pos.getZ() + 0.5), waystone.name(),
						(x, y, size) -> MapDecorations.waystone(graphics, x, y, iconScale(size))));
			}
			for (Marker marker : PlayerMarkers.of(player).markers()) {
				if (marker.pos().dimension() == player.level().dimension()) {
					places.add(new Place(above(marker.pos().pos()), marker.name(),
							(x, y, size) -> MapDecorations.banner(graphics, marker.color(), x, y, iconScale(size))));
				}
			}
			for (Place place : places) {
				// With the bar, only those close by show in the world, to tell how high they are: going by the
				// distance along the ground, one right under the player's feet in a cave is close by
				if (display == MarkerDisplay.WORLD || isNear(place, player)) {
					draw(graphics, font, player, place.target(), place.name(), true, viewProjection, eye, guiScale, labels, place.icon());
				}
			}
			if (display == MarkerDisplay.BAR && !places.isEmpty()) {
				DirectionBar.extract(graphics, minecraft, player, player.getYRot(partialTick), places, guiScale);
			}
			DeathMarker.here(player).ifPresent(pos -> draw(graphics, font, player, above(pos), DEATH_NAME.getString(), true, viewProjection, eye,
					guiScale, labels, (x, y, size) -> MapDecorations.death(graphics, x, y, iconScale(size))));
		}

		// The nearest keep their place, and those behind them move up rather than be written over
		labels.sort(Comparator.comparingDouble(Label::distance));
		LabelSpots spots = new LabelSpots();
		for (Label label : labels) {
			float width = (font.width(label.text()) + 2 * TEXT_MARGIN) * label.scale();
			float height = (font.lineHeight + 1) * label.scale();
			float top = spots.place(label.x(), label.bottom() - height, width, height, -height);
			if (!Float.isNaN(top)) {
				label(graphics, font, label.text(), label.x(), top + height, label.scale());
			}
		}
	}

	/** A marked block's place, a little above the ground. */
	private static Vec3 above(BlockPos pos) {
		return new Vec3(pos.getX() + 0.5, pos.getY() + 1.5, pos.getZ() + 0.5);
	}

	private static float iconScale(float size) {
		return ICON_FAR + (ICON_NEAR - ICON_FAR) * size;
	}

	/** The marker's icon where its place is on screen, and its label above it. */
	private static void draw(GuiGraphicsExtractor graphics, Font font, LocalPlayer player, Vec3 target, String name, boolean labeled,
			Matrix4f viewProjection, Vec3 eye, int guiScale, List<Label> labels, Icon icon) {
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

		float size = size(distance);
		float halfHeight = icon.draw(x, y, size);
		if (labeled) {
			labels.add(new Label(text(name, distance), distance, x, y - halfHeight - 1, textScale(guiScale, size)));
		}
	}

	/** Close enough along the ground to show in the world along with the direction bar. */
	static boolean isNear(Place place, LocalPlayer player) {
		return Math.hypot(place.target().x - player.getX(), place.target().z - player.getZ()) <= BAR_NEAR;
	}

	/** 1 near, 0 far, the size in between. */
	static float size(double distance) {
		return (float) (1 - Math.clamp((distance - NEAR) / (FAR - NEAR), 0, 1));
	}

	/** A marker or a waystone, where it shows above the ground. */
	record Place(Vec3 target, String name, Icon icon) {
	}

	interface Icon {
		/** Draws the icon centered on (x, y), and returns its half height. */
		float draw(float x, float y, float size);
	}

	/**
	 * The label's scale: about two thirds the usual size near, half far, in whole screen pixels per font pixel
	 * so it stays sharp, and at least one: the size of text at GUI scale 1.
	 */
	static float textScale(int guiScale, float size) {
		int near = Math.round(guiScale * 2 / 3F);
		int far = (guiScale + 1) / 2;
		return Math.round(far + (near - far) * size) / (float) guiScale;
	}

	/** "Name · 40 m", the distance in gray. */
	static Component text(String name, double distance) {
		Component text = Component.translatable("gui.cassonade.marker.distance", Math.round(distance)).withColor(DISTANCE_COLOR);
		return name.isEmpty() ? text : Component.literal(name + " · ").withColor(TEXT_COLOR).append(text);
	}

	/** The text centered on x, its bottom at y, on a faint background like name tags. */
	static void label(GuiGraphicsExtractor graphics, Font font, Component text, float x, float bottom, float scale) {
		int width = font.width(text);
		graphics.pose().pushMatrix();
		graphics.pose().translate(x, bottom);
		graphics.pose().scale(scale, scale);
		int left = -width / 2;
		int top = -font.lineHeight;
		graphics.fill(left - TEXT_MARGIN, top - 1, left + width + TEXT_MARGIN, top + font.lineHeight, TEXT_BACKGROUND);
		graphics.text(font, text, left, top, TEXT_COLOR, false);
		graphics.pose().popMatrix();
	}

	/** A label waiting for all of them to be known, to be sorted out. */
	private record Label(Component text, double distance, float x, float bottom, float scale) {
	}
}
