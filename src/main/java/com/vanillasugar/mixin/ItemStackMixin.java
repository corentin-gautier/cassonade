package com.vanillasugar.mixin;

import java.util.Optional;
import java.util.function.Consumer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.component.TooltipProvider;

import com.vanillasugar.storage.ContainerPreview;

/** Shulker boxes and backpacks show their contents as a grid of slots, like bundles, rather than a list of names. */
@Mixin(ItemStack.class)
public abstract class ItemStackMixin {
	@Inject(method = "getTooltipImage", at = @At("RETURN"), cancellable = true)
	private void vanillasugar$previewContainer(CallbackInfoReturnable<Optional<TooltipComponent>> cir) {
		if (cir.getReturnValue().isEmpty()) {
			ContainerPreview.of((ItemStack) (Object) this).ifPresent(preview -> cir.setReturnValue(Optional.of(preview)));
		}
	}

	@Inject(method = "addToTooltip(Lnet/minecraft/core/component/DataComponentType;Lnet/minecraft/world/item/component/TooltipProvider$Getter;"
			+ "Lnet/minecraft/world/item/Item$TooltipContext;Lnet/minecraft/world/item/component/TooltipDisplay;Ljava/util/function/Consumer;"
			+ "Lnet/minecraft/world/item/TooltipFlag;)V", at = @At("HEAD"), cancellable = true)
	private <T> void vanillasugar$skipContainerList(DataComponentType<T> type, TooltipProvider.Getter<T> getter, Item.TooltipContext context,
			TooltipDisplay display, Consumer<Component> consumer, TooltipFlag flag, CallbackInfo ci) {
		if (type == DataComponents.CONTAINER && ContainerPreview.of((ItemStack) (Object) this).isPresent()) {
			ci.cancel();
		}
	}
}
