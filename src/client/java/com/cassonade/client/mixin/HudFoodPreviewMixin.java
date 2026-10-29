package com.cassonade.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;

/**
 * With food in hand that the player could eat, the drumsticks it would restore blink on the hunger bar,
 * past those already full: what the food tooltip says, where the hunger is. Client only.
 */
@Mixin(Hud.class)
public abstract class HudFoodPreviewMixin {
	@Unique
	private static final Identifier FULL = Identifier.withDefaultNamespace("hud/food_full");
	@Unique
	private static final Identifier HALF = Identifier.withDefaultNamespace("hud/food_half");
	@Unique
	private static final Identifier FULL_HUNGER = Identifier.withDefaultNamespace("hud/food_full_hunger");
	@Unique
	private static final Identifier HALF_HUNGER = Identifier.withDefaultNamespace("hud/food_half_hunger");
	/** One fade out and back in, in milliseconds. */
	@Unique
	private static final float BLINK_PERIOD = 1200;

	@Inject(method = "extractFood", at = @At("TAIL"))
	private void cassonade$previewFood(GuiGraphicsExtractor graphics, Player player, int yLineBase, int xRight, CallbackInfo ci) {
		FoodProperties food = cassonade$heldFood(player);
		if (food == null || !player.canEat(food.canAlwaysEat())) {
			return;
		}
		int now = player.getFoodData().getFoodLevel();
		int after = Math.min(now + food.nutrition(), 20);
		boolean hunger = player.hasEffect(MobEffects.HUNGER);
		float phase = Util.getMillis() % (long) BLINK_PERIOD / BLINK_PERIOD;
		float alpha = 0.25F + 0.75F * (0.5F + 0.5F * Mth.cos(phase * Mth.TWO_PI));
		// Drumstick i holds points 2i + 1 and 2i + 2, drawn from the right
		for (int i = now / 2; i * 2 < after; i++) {
			Identifier sprite = i * 2 + 2 <= after ? (hunger ? FULL_HUNGER : FULL) : (hunger ? HALF_HUNGER : HALF);
			graphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, xRight - i * 8 - 9, yLineBase, 9, 9, alpha);
		}
	}

	/** The food the use key would eat: the main hand's, else the offhand's. */
	@Unique
	private static FoodProperties cassonade$heldFood(Player player) {
		FoodProperties main = player.getItemInHand(InteractionHand.MAIN_HAND).get(DataComponents.FOOD);
		return main != null ? main : player.getItemInHand(InteractionHand.OFF_HAND).get(DataComponents.FOOD);
	}
}
