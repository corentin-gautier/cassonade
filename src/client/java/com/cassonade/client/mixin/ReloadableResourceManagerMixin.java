package com.cassonade.client.mixin;

import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.resources.ReloadableResourceManager;

import com.cassonade.client.dev.DevRendering;

/** In runClient, while the game's own rendering is shown (DevRendering), the mod's shaders are left out of the packs. */
@Mixin(ReloadableResourceManager.class)
abstract class ReloadableResourceManagerMixin {
	@ModifyArg(method = "createReload", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/server/packs/resources/MultiPackResourceManager;<init>(Lnet/minecraft/server/packs/PackType;Ljava/util/List;)V"), index = 1)
	private List<PackResources> cassonade$vanillaRendering(List<PackResources> packs) {
		DevRendering.reloading();
		return DevRendering.hidesShaders() ? packs.stream().map(DevRendering::filter).toList() : packs;
	}
}
