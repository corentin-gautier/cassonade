package com.vanillasugar.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.advancements.AdvancementsScreen;

import com.vanillasugar.client.quest.AdvancementsListScreen;

/** The advancements key and the pause menu's button open the list of advancements instead of the tree. */
@Mixin(Gui.class)
public abstract class GuiMixin {
	@ModifyVariable(method = "setScreen", at = @At("HEAD"), argsOnly = true)
	private Screen vanillasugar$listAdvancements(Screen screen) {
		if (screen instanceof AdvancementsScreen tree) {
			return new AdvancementsListScreen(((AdvancementsScreenAccessor) tree).getLastScreen());
		}
		return screen;
	}
}
