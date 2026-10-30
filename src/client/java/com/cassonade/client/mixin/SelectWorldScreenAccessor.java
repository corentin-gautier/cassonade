package com.cassonade.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;

@Mixin(SelectWorldScreen.class)
public interface SelectWorldScreenAccessor {
	@Accessor
	Screen getLastScreen();
}
