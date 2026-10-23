package com.vanillasugar.client.mixin;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.advancements.AdvancementsScreen;

@Mixin(AdvancementsScreen.class)
public interface AdvancementsScreenAccessor {
	@Accessor
	@Nullable Screen getLastScreen();
}
