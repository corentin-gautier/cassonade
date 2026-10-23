package com.vanillasugar.client.mixin;

import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.world.item.ItemStack;

@Mixin(targets = "net.minecraft.client.gui.screens.recipebook.OverlayRecipeComponent$OverlayRecipeButton$Pos")
public interface OverlayRecipePosAccessor {
	@Accessor("x")
	int vanillasugar$getX();

	@Accessor("y")
	int vanillasugar$getY();

	@Accessor("ingredients")
	List<ItemStack> vanillasugar$getIngredients();
}
