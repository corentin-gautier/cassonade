package com.cassonade.client.quest;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

import org.jspecify.annotations.Nullable;

import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementType;
import net.minecraft.advancements.DisplayInfo;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.ARGB;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import net.minecraft.world.item.ItemStack;

import com.cassonade.Cassonade;

/**
 * Shown at the center of the screen when an advancement is made, instead of vanilla's toast in the
 * corner: the advancement's icon in a medallion, over an ornate frame with its kind, its title in
 * large letters and its description. The bigger the advancement, the bigger the banner: wood for a
 * task, gold and rays of light for a goal, purple and a fanfare for a challenge. Advancements made
 * together are shown one after the other.
 */
public final class AchievementBanner {
	/** Keep in sync with FRAME_BORDER in tools/quest_textures.py. */
	private static final int FRAME_BORDER = 10;
	private static final int PADDING = 4;
	private static final int MEDALLION_SIZE = 28;
	private static final int MIN_WIDTH = 160;
	private static final int DESCRIPTION_WIDTH = 220;
	private static final int MAX_DESCRIPTION_LINES = 3;
	private static final int APPEAR_MS = 350;
	private static final int FADE_MS = 600;
	private static final int TITLE_COLOR = 0xFFFFFFFF;
	private static final int DESCRIPTION_COLOR = 0xFFD2BBB5;
	private static final Identifier RAYS = Cassonade.id("hud/banner_rays");

	private enum Style {
		TASK("task", 1, 2, 4500, 0, 0xFFE8DABC),
		GOAL("goal", 2, 2, 5500, 0xFFF6DA8A, 0xFFF6DA8A),
		CHALLENGE("challenge", 2, 3, 7000, 0xFFC898EC, 0xFFE0C0FF);

		final Identifier frame;
		final Identifier medallion;
		/** How much bigger than its sprite the medallion is drawn, the icon with it. */
		final int medallionScale;
		final int titleScale;
		final int durationMs;
		/** Color of the rays behind the medallion, none when 0. */
		final int raysColor;
		final int kindColor;

		Style(String name, int medallionScale, int titleScale, int durationMs, int raysColor, int kindColor) {
			this.frame = Cassonade.id("hud/banner_" + name);
			this.medallion = Cassonade.id("hud/banner_medallion_" + name);
			this.medallionScale = medallionScale;
			this.titleScale = titleScale;
			this.durationMs = durationMs;
			this.raysColor = raysColor;
			this.kindColor = kindColor;
		}

		static Style of(AdvancementType type) {
			return switch (type) {
				case TASK -> TASK;
				case GOAL -> GOAL;
				case CHALLENGE -> CHALLENGE;
			};
		}
	}

	private record Banner(Style style, ItemStack icon, Component kind, Component title, Component description) {
	}

	private static final Deque<Banner> QUEUE = new ArrayDeque<>();
	private static @Nullable Banner current;
	private static long shownAt;

	private AchievementBanner() {
	}

	/** Queues the banner of an advancement just made. */
	public static void add(AdvancementHolder advancement) {
		advancement.value().display().ifPresent(display -> QUEUE.add(of(display)));
	}

	private static Banner of(DisplayInfo display) {
		return new Banner(Style.of(display.type()), display.icon().create(), display.type().getDisplayName(), display.title(),
				display.description());
	}

	/** Whether a banner is on the screen or waiting for its turn. */
	public static boolean isShowing() {
		return current != null || !QUEUE.isEmpty();
	}

	/** Drops the banners left, on leaving the world. */
	public static void clear() {
		QUEUE.clear();
		current = null;
	}

	/** Draws the banner over everything else, screens included, like vanilla's toasts. */
	public static void extractRenderState(GuiGraphicsExtractor graphics) {
		Minecraft minecraft = Minecraft.getInstance();
		long now = Util.getMillis();
		if (current != null && now - shownAt >= current.style.durationMs) {
			current = null;
		}
		if (current == null) {
			current = QUEUE.poll();
			if (current == null) {
				return;
			}
			shownAt = now;
			playSound(minecraft, current.style);
		}
		if (minecraft.gui.hud.isHidden()) {
			return;
		}
		graphics.nextStratum();
		draw(graphics, minecraft.font, current, now - shownAt);
	}

	private static void playSound(Minecraft minecraft, Style style) {
		SoundEvent sound = style == Style.TASK ? SoundEvents.PLAYER_LEVELUP : SoundEvents.UI_TOAST_CHALLENGE_COMPLETE;
		float pitch = style == Style.GOAL ? 1.25F : 1.0F;
		float volume = switch (style) {
			case TASK -> 0.6F;
			case GOAL -> 0.7F;
			case CHALLENGE -> 1.0F;
		};
		minecraft.getSoundManager().play(SimpleSoundInstance.forUI(sound, pitch, volume));
	}

	private static void draw(GuiGraphicsExtractor graphics, Font font, Banner banner, long age) {
		Style style = banner.style;
		// Grows in with a slight overshoot, then fades out while shrinking a little
		float appear = Mth.clamp(age / (float) APPEAR_MS, 0, 1);
		float fade = Mth.clamp((style.durationMs - age) / (float) FADE_MS, 0, 1);
		float alpha = Math.min(appear, fade);
		float scale = easeOutBack(appear) * (0.9F + 0.1F * fade);
		if (alpha <= 0.02F) {
			return;
		}

		int guiWidth = graphics.guiWidth();
		List<FormattedCharSequence> description = font.split(banner.description, DESCRIPTION_WIDTH);
		if (description.size() > MAX_DESCRIPTION_LINES) {
			description = description.subList(0, MAX_DESCRIPTION_LINES);
		}
		int descriptionWidth = 0;
		for (FormattedCharSequence line : description) {
			descriptionWidth = Math.max(descriptionWidth, font.width(line));
		}
		// The title as large as its style wants, as long as it fits on the screen
		int inside = 2 * (FRAME_BORDER + PADDING);
		int titleScale = style.titleScale;
		while (titleScale > 1 && font.width(banner.title) * titleScale + inside > guiWidth - 16) {
			titleScale--;
		}
		int contentWidth = Math.max(font.width(banner.kind), Math.max(font.width(banner.title) * titleScale, descriptionWidth));
		int width = Math.max(MIN_WIDTH, contentWidth + inside);

		int medallion = MEDALLION_SIZE * style.medallionScale;
		int frameTop = medallion / 2;
		int kindTop = frameTop + medallion / 2 + 2;
		int titleTop = kindTop + font.lineHeight + 3;
		int descriptionTop = titleTop + font.lineHeight * titleScale + 4;
		int height = descriptionTop + description.size() * font.lineHeight + FRAME_BORDER + PADDING - frameTop;

		int centerX = guiWidth / 2;
		int top = graphics.guiHeight() / 6;
		graphics.pose().pushMatrix();
		// Scales around the medallion's center, where the banner seems to come from
		graphics.pose().translate(centerX, top + frameTop);
		graphics.pose().scale(scale, scale);
		graphics.pose().translate(-width / 2f, -frameTop);

		int white = ARGB.white(alpha);
		if (style.raysColor != 0) {
			drawRays(graphics, width / 2f, frameTop, medallion * 4, ARGB.multiplyAlpha(style.raysColor, alpha), age);
		}
		graphics.blitSprite(RenderPipelines.GUI_TEXTURED, style.frame, 0, frameTop, width, height, white);
		graphics.pose().pushMatrix();
		graphics.pose().translate((width - medallion) / 2f, 0);
		graphics.pose().scale(style.medallionScale, style.medallionScale);
		graphics.blitSprite(RenderPipelines.GUI_TEXTURED, style.medallion, 0, 0, MEDALLION_SIZE, MEDALLION_SIZE, white);
		// Items have no transparency: the icon shrinks away with the banner instead
		if (fade > 0.25F) {
			graphics.fakeItem(banner.icon, (MEDALLION_SIZE - 16) / 2, (MEDALLION_SIZE - 16) / 2);
		}
		graphics.pose().popMatrix();

		centered(graphics, font, banner.kind, width / 2f, kindTop, 1, ARGB.multiplyAlpha(style.kindColor, alpha));
		centered(graphics, font, banner.title, width / 2f, titleTop, titleScale, ARGB.multiplyAlpha(TITLE_COLOR, alpha));
		int y = descriptionTop;
		for (FormattedCharSequence line : description) {
			graphics.text(font, line, (width - font.width(line)) / 2, y, ARGB.multiplyAlpha(DESCRIPTION_COLOR, alpha), true);
			y += font.lineHeight;
		}
		graphics.pose().popMatrix();
	}

	private static void drawRays(GuiGraphicsExtractor graphics, float x, float y, float size, int color, long age) {
		graphics.pose().pushMatrix();
		graphics.pose().translate(x, y);
		graphics.pose().rotate(age / 4000f * Mth.TWO_PI / 4);
		int half = Math.round(size / 2);
		graphics.blitSprite(RenderPipelines.GUI_TEXTURED, RAYS, -half, -half, 2 * half, 2 * half, color);
		graphics.pose().popMatrix();
	}

	private static void centered(GuiGraphicsExtractor graphics, Font font, Component text, float x, float y, int scale, int color) {
		graphics.pose().pushMatrix();
		graphics.pose().translate(x, y);
		graphics.pose().scale(scale, scale);
		graphics.text(font, text, -font.width(text) / 2, 0, color, true);
		graphics.pose().popMatrix();
	}

	/** 0 to 1, going a little past 1 before settling. */
	private static float easeOutBack(float t) {
		float overshoot = 1.7F;
		float u = t - 1;
		return 1 + (overshoot + 1) * u * u * u + overshoot * u * u;
	}
}
