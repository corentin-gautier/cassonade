package com.cassonade.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.AbstractContainerMenu;

@Mixin(AbstractContainerScreen.class)
public interface AbstractContainerScreenAccessor {
	@Accessor("menu")
	AbstractContainerMenu cassonade$getMenu();

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
