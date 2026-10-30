package com.cassonade.client.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;

import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.StonecutterScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.StonecutterMenu;

import com.cassonade.client.ContainerPanels;

/**
 * The title at the same height as the other windows, clear of the list of recipes: the panel's content comes down into
 * the room left below it, in the textures of the stonecutter and the woodcutter. Everything vanilla places, drawn or
 * clicked, starts from topPos: only the background keeps the real one.
 */
@Mixin(StonecutterScreen.class)
abstract class StonecutterScreenMixin extends AbstractContainerScreen<StonecutterMenu> {
	private StonecutterScreenMixin(StonecutterMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title);
	}

	@Inject(method = "<init>", at = @At("RETURN"))
	private void cassonade$lowerTitle(StonecutterMenu menu, Inventory inventory, Component title, CallbackInfo ci) {
		titleLabelY = 6;
	}

	@ModifyExpressionValue(method = {"extractBackground", "extractTooltip", "mouseClicked", "mouseDragged"},
			at = @At(value = "FIELD", opcode = Opcodes.GETFIELD, target = "Lnet/minecraft/client/gui/screens/inventory/StonecutterScreen;topPos:I"))
	private int cassonade$lowerContent(int top) {
		return top + ContainerPanels.STONECUTTER_ROOM;
	}

	@ModifyArg(method = "extractBackground", index = 3, at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;blit(Lcom/mojang/renderpearl/api/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIFFIIII)V"))
	private int cassonade$backgroundStays(int y) {
		return y - ContainerPanels.STONECUTTER_ROOM;
	}
}
