package com.cassonade.client.mixin;

import java.util.Map;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;

/**
 * Sprites the mod draws alike are kept once in the interface atlas: the others are read from the one kept.
 * Every toast has the same background, a disabled button looks like a slider, the furnace's recipe overlay
 * like the crafting table's, and a recipe slot doesn't change with how many recipes it holds.
 */
@Mixin(GuiGraphicsExtractor.class)
public abstract class GuiGraphicsExtractorMixin {
	@Unique
	private static final Map<Identifier, Identifier> SAME_AS = Map.ofEntries(
			alias("toast/now_playing", "toast/advancement"),
			alias("toast/recipe", "toast/advancement"),
			alias("toast/system", "toast/advancement"),
			alias("toast/tutorial", "toast/advancement"),
			alias("widget/button_disabled", "widget/slider"),
			alias("recipe_book/furnace_overlay", "recipe_book/crafting_overlay"),
			alias("recipe_book/furnace_overlay_disabled", "recipe_book/crafting_overlay_disabled"),
			alias("recipe_book/furnace_overlay_disabled_highlighted", "recipe_book/crafting_overlay_disabled_highlighted"),
			alias("recipe_book/furnace_overlay_highlighted", "recipe_book/crafting_overlay_highlighted"),
			alias("recipe_book/furnace_filter_disabled_highlighted", "recipe_book/furnace_filter_disabled"),
			alias("recipe_book/slot_many_craftable", "recipe_book/slot_craftable"),
			alias("recipe_book/slot_many_uncraftable", "recipe_book/slot_uncraftable"));

	@Unique
	private static Map.Entry<Identifier, Identifier> alias(String sprite, String drawnAs) {
		return Map.entry(Identifier.withDefaultNamespace(sprite), Identifier.withDefaultNamespace(drawnAs));
	}

	/** Every form of blitSprite, as it is called: the sprite drawn alike in its stead. */
	@ModifyVariable(method = "blitSprite", at = @At("HEAD"), argsOnly = true)
	private Identifier cassonade$sameAs(Identifier sprite) {
		return SAME_AS.getOrDefault(sprite, sprite);
	}
}
