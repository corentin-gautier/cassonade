package com.vanillasugar.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.client.gui.screens.inventory.DispenserScreen;
import net.minecraft.resources.Identifier;

import com.vanillasugar.client.ContainerThemes;

/** The chest and dispenser windows are drawn with the texture of the block they were opened from. */
@Mixin({ ContainerScreen.class, DispenserScreen.class })
public abstract class SharedContainerScreenMixin {
	@ModifyArg(method = "extractBackground", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;blit(Lcom/mojang/renderpearl/api/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIFFIIII)V"),
			index = 1)
	private Identifier vanillasugar$themeTexture(Identifier texture) {
		return ContainerThemes.texture(((Screen) (Object) this).getTitle(), texture);
	}
}
