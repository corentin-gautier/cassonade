package com.cassonade.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import net.minecraft.core.GlobalPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.behavior.OneShot;
import net.minecraft.world.entity.ai.behavior.VillagerGoalPackages;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.npc.villager.Villager;

import com.cassonade.tweaks.TavernGatherings;

/** In the evening, villagers gather in their village's tavern while its fire burns, rather than at the bell. */
@Mixin(VillagerGoalPackages.class)
abstract class VillagerGoalPackagesMixin {
	@WrapOperation(method = "getMeetPackage", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/world/entity/ai/behavior/StrollAroundPoi;create(Lnet/minecraft/world/entity/ai/memory/MemoryModuleType;FI)Lnet/minecraft/world/entity/ai/behavior/OneShot;"))
	private static OneShot<PathfinderMob> cassonade$strollInTavern(MemoryModuleType<GlobalPos> memory, float speedModifier, int maxDistance,
			Operation<OneShot<PathfinderMob>> original) {
		return TavernGatherings.stroll(original.call(memory, speedModifier, maxDistance));
	}

	@WrapOperation(method = "getMeetPackage", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/world/entity/ai/behavior/SocializeAtBell;create()Lnet/minecraft/world/entity/ai/behavior/OneShot;"))
	private static OneShot<LivingEntity> cassonade$socializeInTavern(Operation<OneShot<LivingEntity>> original) {
		return TavernGatherings.socialize(original.call());
	}

	@WrapOperation(method = "getMeetPackage", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/world/entity/ai/behavior/SetWalkTargetFromBlockMemory;create(Lnet/minecraft/world/entity/ai/memory/MemoryModuleType;FIII)Lnet/minecraft/world/entity/ai/behavior/OneShot;"))
	private static OneShot<Villager> cassonade$walkToTavern(MemoryModuleType<GlobalPos> memory, float speedModifier, int closeEnough, int tooFar,
			int tooLongUnreachable, Operation<OneShot<Villager>> original) {
		return TavernGatherings.walk(original.call(memory, speedModifier, closeEnough, tooFar, tooLongUnreachable), speedModifier);
	}
}
