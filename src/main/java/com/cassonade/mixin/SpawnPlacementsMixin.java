package com.cassonade.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.level.ServerLevelAccessor;

import com.cassonade.settings.Setting;
import com.cassonade.settings.Settings;
import com.cassonade.time.FullMoon;
import com.cassonade.tweaks.NoCreepers;

@Mixin(SpawnPlacements.class)
public abstract class SpawnPlacementsMixin {
	/** With the spawn_creepers rule off, creepers no longer spawn in the dark or with new chunks. */
	@Inject(method = "checkSpawnRules", at = @At("HEAD"), cancellable = true)
	private static void cassonade$noCreepers(EntityType<?> type, ServerLevelAccessor level, EntitySpawnReason reason, BlockPos pos,
			RandomSource random, CallbackInfoReturnable<Boolean> cir) {
		if (type == EntityTypes.CREEPER && (reason == EntitySpawnReason.NATURAL || reason == EntitySpawnReason.CHUNK_GENERATION)
				&& !level.getLevel().getGameRules().get(NoCreepers.SPAWN_CREEPERS)) {
			cir.setReturnValue(false);
		}
	}

	/** Full moon nights are peaceful on the surface, see {@link FullMoon}. */
	@Inject(method = "checkSpawnRules", at = @At("HEAD"), cancellable = true)
	private static void cassonade$fullMoon(EntityType<?> type, ServerLevelAccessor level, EntitySpawnReason reason, BlockPos pos,
			RandomSource random, CallbackInfoReturnable<Boolean> cir) {
		if (reason == EntitySpawnReason.NATURAL && Settings.on(Setting.PEACEFUL_FULL_MOON) && FullMoon.peaceful(level.getLevel(), type, pos)) {
			cir.setReturnValue(false);
		}
	}
}
