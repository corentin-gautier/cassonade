package com.cassonade.client;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.jspecify.annotations.Nullable;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.LerpingBossEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent.BossBarColor;
import net.minecraft.world.BossEvent.BossBarOverlay;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

import com.cassonade.Cassonade;
import com.cassonade.client.combat.CombatHud;
import com.cassonade.client.map.Minimap;
import com.cassonade.client.mixin.BossHealthOverlayAccessor;

/**
 * The boss bars, drawn in place of vanilla's (BossHealthOverlayMixin): wider and thicker, in a frame of dark
 * iron held between two skulls. The bar fills up when the boss shows up, the health just lost stays a moment
 * as a pale trail before it drains, and the damage dealt adds up next to the name while the blows follow.
 * <p>
 * The server only sends how full the bar is: the damage is told in health points when a boss with the bar's
 * name is around to give its maximum health, and is left out for the bars that stand for no mob (raids, commands).
 */
public final class BossBars {
	private static final Identifier FRAME = Cassonade.id("hud/boss_bar/frame");
	private static final Identifier TRAIL = Cassonade.id("hud/boss_bar/trail");
	private static final Identifier SKULL = Cassonade.id("hud/boss_bar/skull");
	/** By BossBarColor, in its order. */
	private static final Identifier[] HEALTH = new Identifier[BossBarColor.values().length];

	static {
		for (BossBarColor color : BossBarColor.values()) {
			HEALTH[color.ordinal()] = Cassonade.id("hud/boss_bar/" + color.getName());
		}
	}

	/** Sizes of tools/boss_bar_textures.py. */
	private static final int INSIDE_WIDTH = 238;
	private static final int INSIDE_HEIGHT = 7;
	private static final int BORDER = 3;
	private static final int FRAME_WIDTH = INSIDE_WIDTH + 2 * BORDER;
	private static final int FRAME_HEIGHT = INSIDE_HEIGHT + 2 * BORDER;
	private static final int SKULL_SIZE = 13;
	/** How far the skulls stick out past the frame's ends. */
	private static final int SKULL_OUT = SKULL_SIZE - 3;
	/** Room kept from the panels in the top corners, which the bar narrows to stay clear of. */
	private static final int CLEARANCE = 4;
	/** The name's line, then the bar under it, then room before the next. */
	private static final int TOP = 4;
	private static final int NAME_GAP = 2;
	private static final int STEP = 30;
	private static final int NOTCH_COLOR = 0xB0160608;
	private static final int DAMAGE_COLOR = 0xFFFF8A70;
	private static final int FLOURISH_COLOR = 0xFF76626A;
	private static final int FLOURISH_SHADOW = 0xFF160608;
	private static final int FLOURISH_GAP = 4;
	private static final int FLOURISH_LENGTH = 24;

	/** Ticks the bar takes to fill when the boss shows up, and its name to fade in. */
	private static final float ENTRANCE = 30;
	private static final float NAME_FADE = 12;
	/** The health lost stays a moment before it drains, so the blow can be read. */
	private static final int TRAIL_DELAY = 15;
	private static final float TRAIL_DRAIN = 0.012F;
	/** The damage keeps adding up while the blows come within this many ticks, then fades out. */
	private static final int COMBO = 40;
	private static final int COMBO_FADE = 10;

	private static Map<UUID, Bar> bars = new HashMap<>();

	private static final class Bar {
		int age;
		float progress;
		float trail;
		int lastHurt = -COMBO - COMBO_FADE;
		float damage;
	}

	private BossBars() {
	}

	/** Whether a boss bar is on screen. */
	public static boolean isShown(Minecraft minecraft) {
		return !events(minecraft).isEmpty();
	}

	/** Whether the mob has a boss bar, named after it as bosses name theirs. */
	public static boolean hasBar(Minecraft minecraft, Entity entity) {
		String name = entity.getDisplayName().getString();
		return events(minecraft).values().stream().anyMatch(event -> event.getName().getString().equals(name));
	}

	private static Map<UUID, LerpingBossEvent> events(Minecraft minecraft) {
		return ((BossHealthOverlayAccessor) minecraft.gui.hud.getBossOverlay()).cassonade$events();
	}

	public static void tick(Minecraft minecraft) {
		if (minecraft.level == null) {
			bars.clear();
			return;
		}
		Map<UUID, Bar> seen = new HashMap<>();
		events(minecraft).forEach((id, event) -> {
			float progress = event.getProgress();
			Bar bar = bars.get(id);
			if (bar == null) {
				bar = new Bar();
				bar.progress = bar.trail = progress;
			}
			bar.age++;
			if (progress < bar.progress) {
				if (bar.age - bar.lastHurt > COMBO) {
					bar.damage = 0;
				}
				bar.lastHurt = bar.age;
				LivingEntity boss = boss(minecraft, event);
				bar.damage = boss == null ? 0 : bar.damage + (bar.progress - progress) * boss.getMaxHealth();
			}
			bar.progress = progress;
			if (progress >= bar.trail) {
				bar.trail = progress;
			} else if (bar.age - bar.lastHurt > TRAIL_DELAY) {
				bar.trail = Math.max(progress, bar.trail - TRAIL_DRAIN);
			}
			seen.put(id, bar);
		});
		bars = seen;
	}

	/** The nearest mob named like the bar, as bosses name theirs. */
	private static @Nullable LivingEntity boss(Minecraft minecraft, LerpingBossEvent event) {
		String name = event.getName().getString();
		LivingEntity nearest = null;
		for (Entity entity : minecraft.level.entitiesForRendering()) {
			if (entity instanceof LivingEntity living && living.getDisplayName().getString().equals(name)
					&& (nearest == null || living.distanceToSqr(minecraft.player) < nearest.distanceToSqr(minecraft.player))) {
				nearest = living;
			}
		}
		return nearest;
	}

	/** Stacked from the top of the screen down to a third of it, as in vanilla. */
	public static void extract(GuiGraphicsExtractor graphics, Collection<LerpingBossEvent> events) {
		Minecraft minecraft = Minecraft.getInstance();
		float partialTick = minecraft.getDeltaTracker().getGameTimeDeltaPartialTick(false);
		int y = TOP;
		for (LerpingBossEvent event : events) {
			Bar bar = bars.get(event.getId());
			drawBar(graphics, minecraft.font, event, bar, y, bar == null ? 0 : bar.age + partialTick);
			y += STEP;
			if (y >= graphics.guiHeight() / 3) {
				break;
			}
		}
	}

	private static void drawBar(GuiGraphicsExtractor graphics, Font font, LerpingBossEvent event, @Nullable Bar bar, int y, float age) {
		int center = graphics.guiWidth() / 2;
		int frameWidth = frameWidth(graphics, Minecraft.getInstance());
		int insideWidth = frameWidth - 2 * BORDER;
		int frameLeft = center - frameWidth / 2;
		int frameTop = y + font.lineHeight + NAME_GAP;

		// The name between two iron flourishes
		Component name = event.getName();
		int alpha = Mth.floor(255 * Mth.clamp(age / NAME_FADE, 0, 1));
		if (alpha > 8) {
			int width = font.width(name);
			int lineY = y + font.lineHeight / 2 - 1;
			int reach = Math.round(FLOURISH_LENGTH * ease(age / ENTRANCE));
			for (int side : new int[] {-1, 1}) {
				int from = center + side * (width / 2 + FLOURISH_GAP);
				int to = from + side * reach;
				graphics.fill(Math.min(from, to), lineY, Math.max(from, to), lineY + 1, ARGB.multiplyAlpha(FLOURISH_COLOR, alpha / 255F));
				graphics.fill(Math.min(from, to), lineY + 1, Math.max(from, to), lineY + 2, ARGB.multiplyAlpha(FLOURISH_SHADOW, alpha / 255F));
			}
			graphics.text(font, name, center - width / 2, y, ARGB.color(alpha, 0xFFFFFF));
		}

		graphics.blitSprite(RenderPipelines.GUI_TEXTURED, FRAME, frameLeft, frameTop, frameWidth, FRAME_HEIGHT);
		int insideLeft = frameLeft + BORDER;
		int insideTop = frameTop + BORDER;
		float filled = ease(age / ENTRANCE);
		int health = Mth.ceil(insideWidth * Mth.clamp(event.getProgress(), 0, 1) * filled);
		int trail = bar == null ? health : Mth.ceil(insideWidth * Mth.clamp(bar.trail, 0, 1) * filled);
		if (trail > health) {
			graphics.blitSprite(RenderPipelines.GUI_TEXTURED, TRAIL, INSIDE_WIDTH, INSIDE_HEIGHT, 0, 0, insideLeft, insideTop, trail, INSIDE_HEIGHT);
		}
		if (health > 0) {
			graphics.blitSprite(RenderPipelines.GUI_TEXTURED, HEALTH[event.getColor().ordinal()], INSIDE_WIDTH, INSIDE_HEIGHT, 0, 0,
					insideLeft, insideTop, health, INSIDE_HEIGHT);
		}
		int notches = notches(event.getOverlay());
		for (int i = 1; i < notches; i++) {
			int x = insideLeft + i * insideWidth / notches;
			graphics.fill(x, insideTop, x + 1, insideTop + INSIDE_HEIGHT, NOTCH_COLOR);
		}

		int skullTop = frameTop + (FRAME_HEIGHT - SKULL_SIZE) / 2;
		graphics.blitSprite(RenderPipelines.GUI_TEXTURED, SKULL, frameLeft - SKULL_OUT, skullTop, SKULL_SIZE, SKULL_SIZE);
		graphics.blitSprite(RenderPipelines.GUI_TEXTURED, SKULL, frameLeft + frameWidth - 3, skullTop, SKULL_SIZE, SKULL_SIZE);

		// Over the bar's right end, fading once the blows stop
		if (bar != null && bar.damage > 0) {
			float since = age - bar.lastHurt;
			float fade = 1 - Mth.clamp((since - COMBO) / COMBO_FADE, 0, 1);
			if (fade > 0.03F) {
				String damage = "-" + Math.max(1, Math.round(bar.damage));
				graphics.text(font, damage, frameLeft + frameWidth - font.width(damage), y, ARGB.multiplyAlpha(DAMAGE_COLOR, fade));
			}
		}
	}

	/**
	 * Centered, as wide as it can be up to its full width without its skulls reaching the clock's panel and the
	 * eye in the top left corner or the minimap in the top right one.
	 */
	private static int frameWidth(GuiGraphicsExtractor graphics, Minecraft minecraft) {
		int center = graphics.guiWidth() / 2;
		int left = ClockHud.right();
		if (CombatHud.isHuntedShown(minecraft)) {
			left = Math.max(left, CombatHud.huntedRight());
		}
		int right = Minimap.isShown(minecraft) ? Minimap.frameLeft(graphics.guiWidth()) : graphics.guiWidth();
		int half = Math.min(center - left, right - center) - CLEARANCE - SKULL_OUT;
		return Mth.clamp(2 * half, 2 * BORDER + INSIDE_WIDTH / 4, FRAME_WIDTH);
	}

	private static int notches(BossBarOverlay overlay) {
		return switch (overlay) {
			case PROGRESS -> 0;
			case NOTCHED_6 -> 6;
			case NOTCHED_10 -> 10;
			case NOTCHED_12 -> 12;
			case NOTCHED_20 -> 20;
		};
	}

	/** Quick at first, settling at the end. */
	private static float ease(float t) {
		float clamped = Mth.clamp(t, 0, 1);
		return 1 - (1 - clamped) * (1 - clamped) * (1 - clamped);
	}
}
