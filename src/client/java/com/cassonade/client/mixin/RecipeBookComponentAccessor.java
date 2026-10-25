package com.cassonade.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;

@Mixin(RecipeBookComponent.class)
public interface RecipeBookComponentAccessor {
	@Invoker("getXOrigin")
	int cassonade$getXOrigin();

	@Invoker("getYOrigin")
	int cassonade$getYOrigin();
}
