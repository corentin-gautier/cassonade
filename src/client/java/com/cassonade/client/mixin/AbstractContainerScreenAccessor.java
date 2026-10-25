package com.cassonade.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;

@Mixin(AbstractContainerScreen.class)
public interface AbstractContainerScreenAccessor {
	@Accessor("leftPos")
	int cassonade$getLeftPos();

	@Accessor("topPos")
	int cassonade$getTopPos();

	@Accessor("imageWidth")
	int cassonade$getImageWidth();

	@Accessor("imageHeight")
	int cassonade$getImageHeight();

	@Accessor("inventoryLabelY")
	int cassonade$getInventoryLabelY();
}
