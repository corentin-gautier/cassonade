package com.cassonade.client.mixin;

import java.util.Map;

import org.joml.Vector2f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;

import com.cassonade.client.OpenFade;

/**
 * Sprites the mod draws alike are kept once in the interface atlas: the others are read from the one kept.
 * Every toast has the same background, a disabled button looks like a slider, the furnace's recipe overlay
 * like the crafting table's.
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
			alias("recipe_book/furnace_filter_disabled_highlighted", "recipe_book/furnace_filter_disabled"));

	/** Its nine-slice pieces already carry the fade in their tint. */
	@Unique
	private boolean cassonade$blittingSprite;

	@Unique
	private static Map.Entry<Identifier, Identifier> alias(String sprite, String drawnAs) {
		return Map.entry(Identifier.withDefaultNamespace(sprite), Identifier.withDefaultNamespace(drawnAs));
	}

	/** Every form of blitSprite, as it is called: the sprite drawn alike in its stead. */
	@ModifyVariable(method = "blitSprite", at = @At("HEAD"), argsOnly = true)
	private Identifier cassonade$sameAs(Identifier sprite) {
		return SAME_AS.getOrDefault(sprite, sprite);
	}

	/** Panels, sprites and text fade in with a window opening; the three below are where they all end up. */
	@ModifyVariable(method = "innerFill", at = @At("HEAD"), argsOnly = true, ordinal = 4)
	private int cassonade$fadeFill(int color) {
		return OpenFade.apply(color);
	}

	@ModifyVariable(method = "innerFill", at = @At("HEAD"), argsOnly = true)
	private Integer cassonade$fadeGradient(Integer color) {
		return color == null ? null : OpenFade.apply(color);
	}

	@ModifyVariable(method = "innerBlit(Lcom/mojang/renderpearl/api/pipeline/RenderPipeline;Lcom/mojang/renderpearl/api/textures/GpuTextureView;Lcom/mojang/renderpearl/api/textures/GpuSampler;IIIIFFFFI)V",
			at = @At("HEAD"), argsOnly = true, ordinal = 4)
	private int cassonade$fadeBlit(int color) {
		return cassonade$blittingSprite ? color : OpenFade.apply(color);
	}

	@Inject(method = "blitSprite(Lcom/mojang/renderpearl/api/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIIII)V",
			at = @At("HEAD"))
	private void cassonade$startSpriteFade(CallbackInfo ci) {
		cassonade$blittingSprite = true;
	}

	@ModifyVariable(method = "blitSprite(Lcom/mojang/renderpearl/api/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIIII)V",
			at = @At("HEAD"), argsOnly = true, ordinal = 4)
	private int cassonade$fadeSprite(int color) {
		return OpenFade.apply(color);
	}

	@Inject(method = "blitSprite(Lcom/mojang/renderpearl/api/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIIII)V",
			at = @At("RETURN"))
	private void cassonade$endSpriteFade(CallbackInfo ci) {
		cassonade$blittingSprite = false;
	}

	@ModifyVariable(method = "createDefaultTextParameters", at = @At("HEAD"), argsOnly = true)
	private float cassonade$fadeText(float opacity) {
		return opacity * OpenFade.alpha();
	}

	@Shadow
	public abstract org.joml.Matrix3x2fStack pose();

	@ModifyVariable(method = "entity", at = @At("HEAD"), argsOnly = true, ordinal = 0)
	private int cassonade$entityX0(int value) {
		return cassonade$entityCorner(value, 0);
	}

	@ModifyVariable(method = "entity", at = @At("HEAD"), argsOnly = true, ordinal = 1)
	private int cassonade$entityY0(int value) {
		return cassonade$entityCorner(value, 1);
	}

	@ModifyVariable(method = "entity", at = @At("HEAD"), argsOnly = true, ordinal = 2)
	private int cassonade$entityX1(int value) {
		return cassonade$entityCorner(value, 0);
	}

	@ModifyVariable(method = "entity", at = @At("HEAD"), argsOnly = true, ordinal = 3)
	private int cassonade$entityY1(int value) {
		return cassonade$entityCorner(value, 1);
	}

	@ModifyVariable(method = "entity", at = @At("HEAD"), argsOnly = true)
	private float cassonade$entityScale(float scale) {
		return OpenFade.alpha() >= 1f ? scale : scale * pose().m00();
	}

	/** The game ignores the pose for entities (the smithing table's armor stand): they follow a window opening by hand. */
	@Unique
	private int cassonade$entityCorner(int value, int ordinal) {
		if (OpenFade.alpha() >= 1f) {
			return value;
		}
		Vector2f corner = pose().transformPosition(ordinal % 2 == 0 ? value : 0, ordinal % 2 == 0 ? 0 : value, new Vector2f());
		return Math.round(ordinal % 2 == 0 ? corner.x : corner.y);
	}
}
