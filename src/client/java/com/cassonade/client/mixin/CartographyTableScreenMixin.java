package com.cassonade.client.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CartographyTableScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.CartographyTableMenu;
import net.minecraft.world.level.saveddata.maps.MapId;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;

import com.cassonade.client.ContainerPanels;

/**
 * Vanilla squeezes the title between the window's edge and its content: the title comes down to where the other
 * windows have it, and what vanilla draws at fixed places follows the room made under it (ContainerPanels.CARTOGRAPHY_ROOM).
 */
@Mixin(CartographyTableScreen.class)
abstract class CartographyTableScreenMixin extends AbstractContainerScreen<CartographyTableMenu> {
	private CartographyTableScreenMixin(CartographyTableMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title);
	}

	@Inject(method = "<init>", at = @At("RETURN"))
	private void cassonade$lowerTitle(CartographyTableMenu menu, Inventory inventory, Component title, CallbackInfo ci) {
		titleLabelY += 2;
	}

	@WrapOperation(method = "extractBackground", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/gui/screens/inventory/CartographyTableScreen;extractResultingMap(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/world/level/saveddata/maps/MapId;Lnet/minecraft/world/level/saveddata/maps/MapItemSavedData;ZZZZ)V"))
	private void cassonade$lowerMap(CartographyTableScreen screen, GuiGraphicsExtractor graphics, @Nullable MapId id, @Nullable MapItemSavedData data,
			boolean isDuplication, boolean isScaling, boolean isLocking, boolean locked, Operation<Void> original) {
		graphics.pose().pushMatrix();
		graphics.pose().translate(0, ContainerPanels.CARTOGRAPHY_ROOM);
		original.call(screen, graphics, id, data, isDuplication, isScaling, isLocking, locked);
		graphics.pose().popMatrix();
	}

	/** The arrow crossed out, when the map cannot be changed. */
	@ModifyArg(method = "extractBackground", index = 3, require = 2, at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;blitSprite(Lcom/mojang/renderpearl/api/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIII)V"))
	private int cassonade$lowerError(int y) {
		return y + ContainerPanels.CARTOGRAPHY_ROOM;
	}
}
