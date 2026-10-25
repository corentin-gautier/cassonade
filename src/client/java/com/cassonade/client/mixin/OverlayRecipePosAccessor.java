package com.cassonade.client.mixin;

import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.world.item.ItemStack;

@Mixin(targets = "net.minecraft.client.gui.screens.recipebook.OverlayRecipeComponent$OverlayRecipeButton$Pos")
public interface OverlayRecipePosAccessor {
	@Accessor("x")
	int cassonade$getX();

	@Accessor("y")
	int cassonade$getY();

	@Accessor("ingredients")
	List<ItemStack> cassonade$getIngredients();
}
