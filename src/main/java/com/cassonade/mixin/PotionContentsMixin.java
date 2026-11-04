package com.cassonade.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.alchemy.PotionContents;

import com.cassonade.ServerSupport;
import com.cassonade.combat.Poison;

/** A potion's tooltip gives the poison's halved duration, see {@link Poison}; on a server without the mod, vanilla's. */
@Mixin(PotionContents.class)
public abstract class PotionContentsMixin {
	@WrapOperation(method = "addPotionTooltip(Ljava/lang/Iterable;Ljava/util/function/Consumer;FF)V", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/world/effect/MobEffectUtil;formatDuration(Lnet/minecraft/world/effect/MobEffectInstance;FF)Lnet/minecraft/network/chat/Component;"))
	private static Component cassonade$shortPoison(MobEffectInstance effect, float scale, float tickrate, Operation<Component> original) {
		return original.call(ServerSupport.onClient() ? Poison.shorten(effect) : effect, scale, tickrate);
	}
}
