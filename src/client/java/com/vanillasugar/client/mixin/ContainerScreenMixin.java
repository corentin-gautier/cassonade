package com.vanillasugar.client.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.Identifier;

import com.vanillasugar.VanillaSugar;
import com.vanillasugar.client.ContainerPanels;

/**
 * The chest's window draws its inventory part apart, below its rows: that part grows with the room left
 * above the inventory (ContainerPanels). The ender chest's window, which shares the chest's texture,
 * gets its own, its rows on dark obsidian (tools/container_panels.py); it is told by its title, which
 * an ender chest cannot be renamed out of.
 */
@Mixin(ContainerScreen.class)
public abstract class ContainerScreenMixin {
	@Unique
	private static final Identifier ENDER_CHEST_BACKGROUND = VanillaSugar.id("textures/gui/container/ender_chest.png");

	@ModifyConstant(method = "extractBackground", constant = @Constant(intValue = 96))
	private int vanillasugar$tallerInventoryPart(int height) {
		return height + ContainerPanels.shift(((ContainerScreen) (Object) this).getMenu());
	}

	@ModifyExpressionValue(method = "extractBackground",
			at = @At(value = "FIELD", target = "Lnet/minecraft/client/gui/screens/inventory/ContainerScreen;CONTAINER_BACKGROUND:Lnet/minecraft/resources/Identifier;"))
	private Identifier vanillasugar$enderChestBackground(Identifier background) {
		return ((ContainerScreen) (Object) this).getTitle().getContents() instanceof TranslatableContents title
				&& title.getKey().equals("container.enderchest") ? ENDER_CHEST_BACKGROUND : background;
	}
}
