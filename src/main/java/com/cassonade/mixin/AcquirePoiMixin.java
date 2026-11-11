package com.cassonade.mixin;

import java.util.function.BiPredicate;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.behavior.AcquirePoi;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;

import com.cassonade.settings.Setting;
import com.cassonade.settings.Settings;
import com.cassonade.tweaks.PlayerBeds;

/** Villagers looking for a home pass over the beds players respawn in. */
@Mixin(AcquirePoi.class)
abstract class AcquirePoiMixin {
	@ModifyVariable(method = "create(Ljava/util/function/Predicate;Lnet/minecraft/world/entity/ai/memory/MemoryModuleType;Lnet/minecraft/world/entity/ai/memory/MemoryModuleType;ZLjava/util/Optional;Ljava/util/function/BiPredicate;)Lnet/minecraft/world/entity/ai/behavior/BehaviorControl;",
			at = @At("HEAD"), argsOnly = true)
	private static BiPredicate<ServerLevel, BlockPos> cassonade$skipPlayerBeds(BiPredicate<ServerLevel, BlockPos> validPoi,
			@Local(argsOnly = true, ordinal = 1) MemoryModuleType<GlobalPos> memoryToAcquire) {
		return memoryToAcquire == MemoryModuleType.HOME
				? validPoi.and((level, pos) -> !Settings.on(Setting.PLAYER_BEDS) || !PlayerBeds.isPlayerBed(level, pos))
				: validPoi;
	}
}
