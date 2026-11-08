package com.cassonade.client.spyglass;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.animal.equine.Llama;

/**
 * A horse's speed, jump and health, and a llama's strength, each as a bar from the worst a horse can be born with to the
 * best, red to green: read through the naturalist's spyglass (GlancePanel) and at a hitching post (StableScreen).
 */
public final class HorseStats {
	/** Blocks per second for one point of movement speed, on flat ground. */
	private static final double SPEED_TO_BLOCKS = 42.16;
	/** The worst and the best a horse can be born with: its speed attribute, its jump strength, its hearts; a llama's strength. */
	private static final double WORST_SPEED = 0.1125 * SPEED_TO_BLOCKS;
	private static final double BEST_SPEED = 0.3375 * SPEED_TO_BLOCKS;
	private static final double WORST_JUMP = jumpHeight(0.4);
	private static final double BEST_JUMP = jumpHeight(1.0);
	private static final double WORST_HEALTH = 7.5;
	private static final double BEST_HEALTH = 15;
	private static final int WORST_STRENGTH = 1;
	private static final int BEST_STRENGTH = 5;
	public static final int BAR_WIDTH = 40;
	public static final int BAR_HEIGHT = 5;
	/** The bar's frame and empty part, as the slots' (tools/gui_theme.py OUTLINE, FILL). */
	private static final int BAR_OUTLINE = 0xFF160608;
	private static final int BAR_EMPTY = 0xFF2E1D1C;
	/** From the worst horse to the best: the theme's MOB_HEALTH, GOLD and SELECT. */
	private static final int WORST_COLOR = 0xFFCD3336;
	private static final int MIDDLE_COLOR = 0xFFDEA62B;
	private static final int BEST_COLOR = 0xFF7CA72E;

	/** {@code fill}: from 0 for the worst a horse is born with to 1 for the best. */
	public record Stat(Component name, double fill, Component value) {
	}

	private HorseStats() {
	}

	public static List<Stat> of(AbstractHorse horse) {
		List<Stat> stats = new ArrayList<>();
		double speed = horse.getAttributeValue(Attributes.MOVEMENT_SPEED) * SPEED_TO_BLOCKS;
		stats.add(stat("speed", speed, WORST_SPEED, BEST_SPEED, Component.translatable("gui.cassonade.glance.speed_value", decimal(speed))));
		if (horse.getAttribute(Attributes.JUMP_STRENGTH) != null) {
			double jump = jumpHeight(horse.getAttributeValue(Attributes.JUMP_STRENGTH));
			stats.add(stat("jump", jump, WORST_JUMP, BEST_JUMP, Component.translatable("gui.cassonade.glance.jump_value", decimal(jump))));
		}
		double hearts = horse.getMaxHealth() / 2;
		stats.add(stat("health", hearts, WORST_HEALTH, BEST_HEALTH, Component.translatable("gui.cassonade.glance.health_value", decimal(hearts))));
		if (horse instanceof Llama llama) {
			stats.add(stat("strength", llama.getStrength(), WORST_STRENGTH, BEST_STRENGTH, Component.literal(String.valueOf(llama.getStrength()))));
		}
		return stats;
	}

	/** A stat's bar, filled as far as it goes from the worst to the best, red through gold to green. */
	public static void bar(GuiGraphicsExtractor graphics, int x, int y, double fill) {
		graphics.fill(x, y, x + BAR_WIDTH, y + BAR_HEIGHT, BAR_OUTLINE);
		graphics.fill(x + 1, y + 1, x + BAR_WIDTH - 1, y + BAR_HEIGHT - 1, BAR_EMPTY);
		float amount = (float) Mth.clamp(fill, 0, 1);
		int color = amount < 0.5F ? ARGB.srgbLerp(amount * 2, WORST_COLOR, MIDDLE_COLOR) : ARGB.srgbLerp(amount * 2 - 1, MIDDLE_COLOR, BEST_COLOR);
		int filled = Math.max(1, Mth.floor(amount * (BAR_WIDTH - 2)));
		graphics.fill(x + 1, y + 1, x + 1 + filled, y + BAR_HEIGHT - 1, color);
	}

	private static Stat stat(String name, double value, double worst, double best, Component shown) {
		return new Stat(Component.translatable("gui.cassonade.glance." + name), (value - worst) / (best - worst), shown);
	}

	/** How high a horse jumps, in blocks, for its jump strength (fitted to the game's physics). */
	private static double jumpHeight(double strength) {
		return -0.1817584952 * strength * strength * strength + 3.689713992 * strength * strength + 2.128599134 * strength - 0.343930367;
	}

	private static String decimal(double value) {
		return String.format(Locale.ROOT, "%.1f", value);
	}
}
