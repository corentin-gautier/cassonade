package com.cassonade.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import net.minecraft.client.renderer.block.FluidStateModelSet;
import net.minecraft.resources.Identifier;

import com.cassonade.settings.Setting;
import com.cassonade.settings.Settings;

/** Gives still water a surface drawn over several blocks (FluidRendererMixin); cauldrons keep the game's water_still. */
@Mixin(FluidStateModelSet.class)
abstract class FluidStateModelSetMixin {
	@ModifyArg(method = "<clinit>", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/resources/model/sprite/Material;<init>(Lnet/minecraft/resources/Identifier;)V", ordinal = 0))
	private static Identifier cassonade$waterSurface(Identifier sprite) {
		// Read once, as the class loads
		return Settings.local(Setting.SEA_SURFACE) ? Identifier.fromNamespaceAndPath("cassonade", "block/water_still") : sprite;
	}
}
