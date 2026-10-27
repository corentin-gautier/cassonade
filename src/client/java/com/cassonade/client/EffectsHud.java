package com.cassonade.client;

import java.util.ArrayList;
import java.util.List;

import com.google.common.collect.Ordering;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;

import com.cassonade.client.map.FramedPanel;
import com.cassonade.client.map.MarkerDisplayButton;
import com.cassonade.client.map.Minimap;
import com.cassonade.client.quest.QuestTracker;

/**
 * The status effects in the top right corner, each in the minimap's frame like the rest of the HUD:
 * left of the minimap and the quest followed when they are shown, so the HUD keeps to its corners.
 * Beneficial effects on the first row, harmful ones on the next, as in vanilla; effects from a beacon sit on a background tinted like its beam.
 * The time left is written small in the icon's corner, like a stack's count, rounded to the minute past one.
 */
public final class EffectsHud {
	private static final int ICON_SIZE = 18;
	/** The frame around the icon, which fills its inside. */
	private static final int SIZE = ICON_SIZE + 2 * FramedPanel.BORDER;
	private static final int GAP = 2;
	private static final int MARGIN = 4;
	/** The theme's FILL, toward the beacon's beam; the inventory's effects share it (tools/panel_textures.py). */
	private static final int AMBIENT_BACKGROUND = 0xFF1E3438;

	private EffectsHud() {
	}

	public static void extractRenderState(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
		Minecraft minecraft = Minecraft.getInstance();
		LocalPlayer player = minecraft.player;
		if (player == null || player.getActiveEffects().isEmpty()
				|| minecraft.gui.screen() != null && minecraft.gui.screen().showsActiveEffects()) {
			return;
		}

		List<MobEffectInstance> beneficial = new ArrayList<>();
		List<MobEffectInstance> harmful = new ArrayList<>();
		for (MobEffectInstance instance : Ordering.natural().reverse().sortedCopy(player.getActiveEffects())) {
			if (instance.showIcon()) {
				(instance.getEffect().value().isBeneficial() ? beneficial : harmful).add(instance);
			}
		}

		int top = MARGIN;
		if (minecraft.isDemo()) {
			top += 15;
		}
		// The minimap and the quest followed share one column on the right
		int right = graphics.guiWidth() - MARGIN;
		if (QuestTracker.bottom(minecraft) > 0) {
			right -= Minimap.frameWidth() + GAP;
		}
		// The chat shows the minimap's buttons on its left
		if (minecraft.gui.screen() instanceof ChatScreen && Minimap.isShown(minecraft)) {
			right -= MarkerDisplayButton.SIZE + 2;
		}
		for (List<MobEffectInstance> row : List.of(beneficial, harmful)) {
			if (row.isEmpty()) {
				continue;
			}
			for (int i = 0; i < row.size(); i++) {
				draw(graphics, minecraft.font, row.get(i), right - SIZE - i * (SIZE + GAP), top);
			}
			top += SIZE + GAP;
		}
	}

	private static void draw(GuiGraphicsExtractor graphics, Font font, MobEffectInstance instance, int x, int y) {
		graphics.fill(x + FramedPanel.BORDER, y + FramedPanel.BORDER, x + SIZE - FramedPanel.BORDER, y + SIZE - FramedPanel.BORDER,
				instance.isAmbient() ? AMBIENT_BACKGROUND : FramedPanel.BACKGROUND_COLOR);
		graphics.blitSprite(RenderPipelines.GUI_TEXTURED, FramedPanel.FRAME, x, y, SIZE, SIZE);
		graphics.blitSprite(RenderPipelines.GUI_TEXTURED, Hud.getMobEffectSprite(instance.getEffect()), x + FramedPanel.BORDER, y + FramedPanel.BORDER,
				ICON_SIZE, ICON_SIZE, ARGB.white(alpha(instance)));
		String left = timeLeft(instance);
		if (left != null) {
			int right = x + SIZE - FramedPanel.BORDER + 1;
			int bottom = y + SIZE - FramedPanel.BORDER + 1;
			graphics.text(font, left, right - font.width(left), bottom - font.lineHeight, 0xFFFFFFFF, true);
		}
	}

	/** "45s", "3m", "2h": short enough to sit in the icon. Nothing for a beacon's effect or an endless one. */
	public static String timeLeft(MobEffectInstance instance) {
		if (instance.isAmbient() || instance.isInfiniteDuration()) {
			return null;
		}
		int seconds = Mth.positiveCeilDiv(instance.getDuration(), 20);
		if (seconds < 60) {
			return seconds + "s";
		}
		int minutes = Mth.positiveCeilDiv(seconds, 60);
		return minutes < 60 ? minutes + "m" : Mth.positiveCeilDiv(minutes, 60) + "h";
	}

	/** The icon fades in and out over the effect's last ten seconds, as in vanilla. */
	private static float alpha(MobEffectInstance instance) {
		if (instance.isAmbient() || !instance.endsWithin(200)) {
			return 1;
		}
		int remaining = instance.getDuration();
		int usedSeconds = 10 - remaining / 20;
		float alpha = Mth.clamp(remaining / 10.0F / 5.0F * 0.5F, 0, 0.5F)
				+ Mth.cos(remaining * (float) Math.PI / 5.0F) * Mth.clamp(usedSeconds / 10.0F * 0.25F, 0, 0.25F);
		return Mth.clamp(alpha, 0, 1);
	}
}
