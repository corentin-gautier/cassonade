package com.vanillasugar.client.mixin;

import java.util.Set;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;

import com.vanillasugar.VanillaSugar;

/**
 * The windows that cannot make anything cross out their arrow with one red cross, drawn over the
 * window's own arrow: vanilla draws a grey arrow and background with it, one sprite per window, all
 * the same size and over the arrow.
 */
@Mixin(GuiGraphicsExtractor.class)
public abstract class CrossedOutMixin {
	@Unique
	private static final Identifier CROSSED_OUT = VanillaSugar.id("container/crossed_out");
	@Unique
	private static final Set<Identifier> CROSSED_ARROWS = Set.of(
			Identifier.withDefaultNamespace("container/anvil/error"),
			Identifier.withDefaultNamespace("container/smithing/error"),
			Identifier.withDefaultNamespace("container/grindstone/error"),
			Identifier.withDefaultNamespace("container/cartography_table/error"),
			Identifier.withDefaultNamespace("container/villager/out_of_stock"));

	@ModifyVariable(method = "blitSprite(Lcom/mojang/renderpearl/api/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIII)V",
			at = @At("HEAD"), argsOnly = true)
	private Identifier vanillasugar$crossedOut(Identifier sprite) {
		return CROSSED_ARROWS.contains(sprite) ? CROSSED_OUT : sprite;
	}
}
