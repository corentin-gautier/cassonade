package com.vanillasugar.client.combat;

import java.util.ArrayList;
import java.util.List;

import org.jspecify.annotations.Nullable;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.phys.Vec3;

/**
 * Where a hit came from: a red arc around the crosshair, on its side (on top for a hit from the front,
 * below for one from behind), fading in a second and a half. It follows the player's turning, so it
 * keeps pointing at where the mob, arrow or explosion was. Hits without a place (falling, hunger, fire)
 * show nothing. Always on, client only: the game tells every client where its hits came from.
 */
public final class DamageDirection {
	private static final int LIFE_TICKS = 30;
	/** Hits kept at once, the oldest dropped first. */
	private static final int MAX_HITS = 4;
	/** Half the arc's span, in radians. */
	private static final float SPAN = 0.26F;
	/** The arc's distance from the crosshair, as a share of the screen's height. */
	private static final float RADIUS = 0.3F;
	/** Thickest in its middle, in GUI pixels, thinning to one at its ends. */
	private static final float THICKNESS = 3;
	/** The theme's MOB_HEALTH red (tools/gui_theme.py). */
	private static final int COLOR = 0xCD3336;
	private static final int ALPHA = 0xD0;

	/** Where each hit came from, and the ticks left of its mark. */
	private static final List<Hit> HITS = new ArrayList<>();

	private DamageDirection() {
	}

	/** Called as the local player takes damage, from the server's damage event. */
	public static void hurt(LocalPlayer player, DamageSource source) {
		Vec3 from = source.getSourcePosition();
		if (from == null || from.subtract(player.position()).horizontalDistanceSqr() < 0.01) {
			return;
		}
		if (HITS.size() == MAX_HITS) {
			HITS.removeFirst();
		}
		HITS.add(new Hit(from, LIFE_TICKS));
	}

	public static void tick(Minecraft minecraft) {
		if (minecraft.player == null) {
			HITS.clear();
			return;
		}
		HITS.replaceAll(hit -> new Hit(hit.from(), hit.ticks() - 1));
		HITS.removeIf(hit -> hit.ticks() <= 0);
	}

	/** The angle of the last hit's mark, clockwise from the top of the screen, or null when none shows. */
	public static @Nullable Float lastAngle() {
		LocalPlayer player = Minecraft.getInstance().player;
		return HITS.isEmpty() || player == null ? null : angle(player, HITS.getLast().from(), 1);
	}

	/** Clockwise from straight ahead, as the player looks now. */
	private static float angle(LocalPlayer player, Vec3 from, float partialTick) {
		Vec3 to = from.subtract(player.getPosition(partialTick));
		float yaw = player.getViewYRot(partialTick) * Mth.DEG_TO_RAD;
		double ahead = -to.x * Mth.sin(yaw) + to.z * Mth.cos(yaw);
		double right = -to.x * Mth.cos(yaw) - to.z * Mth.sin(yaw);
		return (float) Mth.atan2(right, ahead);
	}

	public static void extractRenderState(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
		LocalPlayer player = Minecraft.getInstance().player;
		if (HITS.isEmpty() || player == null) {
			return;
		}
		float partialTick = deltaTracker.getGameTimeDeltaPartialTick(false);
		for (Hit hit : HITS) {
			float life = Mth.clamp((hit.ticks() - partialTick) / LIFE_TICKS, 0, 1);
			int alpha = Math.round(ALPHA * life * (2 - life));
			if (alpha > 0) {
				arc(graphics, angle(player, hit.from(), partialTick), alpha << 24 | COLOR);
			}
		}
	}

	/**
	 * The arc, pixel by pixel on the GUI's grid like the rest of the HUD: each pixel of its bounding box
	 * near enough the circle, within the span, thinner toward the ends.
	 */
	private static void arc(GuiGraphicsExtractor graphics, float angle, int color) {
		float centerX = graphics.guiWidth() / 2F;
		float centerY = graphics.guiHeight() / 2F;
		float radius = Math.max(24, graphics.guiHeight() * RADIUS);
		float middleX = centerX + radius * Mth.sin(angle);
		float middleY = centerY - radius * Mth.cos(angle);
		int reach = Mth.ceil(radius * Mth.sin(SPAN) + THICKNESS);
		for (int y = Mth.floor(middleY) - reach; y <= Mth.floor(middleY) + reach; y++) {
			for (int x = Mth.floor(middleX) - reach; x <= Mth.floor(middleX) + reach; x++) {
				float dx = x + 0.5F - centerX;
				float dy = y + 0.5F - centerY;
				float off = Mth.wrapDegrees(((float) Mth.atan2(dx, -dy) - angle) * Mth.RAD_TO_DEG) * Mth.DEG_TO_RAD;
				float along = Math.abs(off) / SPAN;
				float thickness = 1 + (THICKNESS - 1) * (1 - along * along);
				if (along <= 1 && Math.abs(Mth.sqrt(dx * dx + dy * dy) - radius) <= thickness / 2) {
					graphics.fill(x, y, x + 1, y + 1, color);
				}
			}
		}
	}

	private record Hit(Vec3 from, int ticks) {
	}
}
