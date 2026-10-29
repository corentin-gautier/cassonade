package com.cassonade.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import net.minecraft.client.gui.screens.inventory.StonecutterScreen;
import net.minecraft.resources.Identifier;

import com.cassonade.Cassonade;
import com.cassonade.woodcutter.Woodcutter;

@Mixin(StonecutterScreen.class)
abstract class WoodcutterScreenMixin {
	private static final Identifier WOODCUTTER_BACKGROUND = Cassonade.id("textures/gui/container/woodcutter.png");
	private static final String STONECUTTER_SPRITE_PREFIX = "container/stonecutter/";

	@ModifyArg(method = "extractBackground", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;blit(Lcom/mojang/renderpearl/api/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIFFIIII)V"),
			index = 1)
	private Identifier cassonade$woodcutterBackground(Identifier background) {
		AbstractContainerScreenAccessor screen = (AbstractContainerScreenAccessor) (Object) this;
		return screen.cassonade$getMenu().getType() == Woodcutter.MENU ? WOODCUTTER_BACKGROUND : background;
	}

	@ModifyArg(method = "extractBackground", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;blitSprite(Lcom/mojang/renderpearl/api/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIII)V"),
			index = 1)
	private Identifier cassonade$woodcutterScroller(Identifier sprite) {
		return this.cassonade$woodcutterSprite(sprite);
	}

	@ModifyArg(method = "extractButtons", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;blitSprite(Lcom/mojang/renderpearl/api/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIII)V"),
			index = 1)
	private Identifier cassonade$woodcutterRecipes(Identifier sprite) {
		return this.cassonade$woodcutterSprite(sprite);
	}

	private Identifier cassonade$woodcutterSprite(Identifier sprite) {
		AbstractContainerScreenAccessor screen = (AbstractContainerScreenAccessor) (Object) this;
		String path = sprite.getPath();
		if (screen.cassonade$getMenu().getType() != Woodcutter.MENU || !path.startsWith(STONECUTTER_SPRITE_PREFIX)) {
			return sprite;
		}
		return Cassonade.id("container/woodcutter/" + path.substring(STONECUTTER_SPRITE_PREFIX.length()));
	}
}
