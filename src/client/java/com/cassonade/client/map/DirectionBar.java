package com.cassonade.client.map;

import java.util.Comparator;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

import com.cassonade.client.mixin.BossHealthOverlayAccessor;

/**
 * Vanilla's locator bar at the top of the screen, for the markers and waystones, so that the world itself stays clear:
 * their banner or icon, what is in front in the middle, what is out of its range held at the end on the side to turn
 * to; those in range with an arrow when much higher or lower. Only the one aimed at is named, under the bar, unless it is close enough to be named in the world.
 */
final class DirectionBar {
	private static final Identifier BACKGROUND = Identifier.withDefaultNamespace("hud/locator_bar_background");
	private static final Identifier ARROW_UP = Identifier.withDefaultNamespace("hud/locator_bar_arrow_up");
	private static final Identifier ARROW_DOWN = Identifier.withDefaultNamespace("hud/locator_bar_arrow_down");
	private static final int WIDTH = 182;
	private static final int HEIGHT = 5;
	/** Vanilla's room for a dot, which the icons take. */
	private static final int DOT_SIZE = 9;
	private static final int ARROW_WIDTH = 7;
	private static final int ARROW_HEIGHT = 5;
	/** Room above the bar for the dots and the arrows. */
	private static final int TOP = 7;
	/** Boss bars' own spacing (BossHealthOverlay), which they stop at a third of the screen. */
	private static final int BOSS_STEP = 19;
	/** The angle from the middle of the bar to its ends, as in vanilla. */
	private static final float RANGE = 60;
	/** How close to a marker's direction the player must look for it to be named. */
	private static final float AIM = 6;
	/** How many blocks higher or lower a marker gets an arrow. */
	private static final double ARROW_HEIGHT_DIFFERENCE = 4;

	private DirectionBar() {
	}

	/** The degrees to turn from a yaw to face this offset in blocks, positive to the right. */
	static float turn(double dx, double dz, float yaw) {
		return Mth.wrapDegrees((float) Math.toDegrees(Math.atan2(-dx, dz)) - yaw);
	}

	static void extract(GuiGraphicsExtractor graphics, Minecraft minecraft, LocalPlayer player, float yaw, List<MarkersInWorld.Place> places, int guiScale) {
		int left = (graphics.guiWidth() - WIDTH) / 2;
		int top = top(graphics, minecraft);
		int centerX = Mth.ceil((graphics.guiWidth() - DOT_SIZE) / 2F);
		graphics.blitSprite(RenderPipelines.GUI_TEXTURED, BACKGROUND, left, top, WIDTH, HEIGHT);

		// The nearest on top
		Vec3 at = player.position();
		MarkersInWorld.Place aimed = null;
		float aimedTurn = AIM;
		List<MarkersInWorld.Place> farFirst = places.stream()
				.sorted(Comparator.comparingDouble((MarkersInWorld.Place place) -> at.distanceTo(place.target())).reversed()).toList();
		for (MarkersInWorld.Place place : farFirst) {
			float turn = turn(place.target().x - at.x, place.target().z - at.z, yaw);
			int x = centerX + Mth.floor(Mth.clamp(turn / RANGE, -1, 1) * (WIDTH - DOT_SIZE) / 2);
			place.icon().draw(x + DOT_SIZE / 2f, top + HEIGHT / 2f, MarkersInWorld.size(at.distanceTo(place.target())));
			double height = place.target().y - player.getEyeY();
			// Held at an end, it only tells which way to turn
			if (Math.abs(turn) <= RANGE && Math.abs(height) > ARROW_HEIGHT_DIFFERENCE) {
				graphics.blitSprite(RenderPipelines.GUI_TEXTURED, height > 0 ? ARROW_UP : ARROW_DOWN, x + 1, top + (height > 0 ? -6 : 6), ARROW_WIDTH, ARROW_HEIGHT);
			}
			// One close by already has its name in the world
			if (Math.abs(turn) < aimedTurn && !MarkersInWorld.isNear(place, player)) {
				aimedTurn = Math.abs(turn);
				aimed = place;
			}
		}
		if (aimed != null) {
			float scale = MarkersInWorld.textScale(guiScale, 1);
			MarkersInWorld.label(graphics, minecraft.font, MarkersInWorld.text(aimed.name(), at.distanceTo(aimed.target())), graphics.guiWidth() / 2f,
					top + HEIGHT + ARROW_HEIGHT + 3 + minecraft.font.lineHeight * scale, scale);
		}
	}

	/** At the top of the screen, or under the boss bars. */
	private static int top(GuiGraphicsExtractor graphics, Minecraft minecraft) {
		int bosses = ((BossHealthOverlayAccessor) minecraft.gui.hud.getBossOverlay()).cassonade$events().size();
		return bosses == 0 ? TOP : Math.min(bosses * BOSS_STEP, graphics.guiHeight() / 3 + BOSS_STEP) + TOP;
	}
}
