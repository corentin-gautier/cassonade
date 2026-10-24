package com.vanillasugar.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;

@Mixin(AbstractContainerScreen.class)
public interface AbstractContainerScreenAccessor {
	@Accessor("leftPos")
	int vanillasugar$getLeftPos();

	@Accessor("topPos")
	int vanillasugar$getTopPos();

	@Accessor("imageWidth")
	int vanillasugar$getImageWidth();

	@Accessor("imageHeight")
	int vanillasugar$getImageHeight();

	@Accessor("inventoryLabelY")
	int vanillasugar$getInventoryLabelY();
}
