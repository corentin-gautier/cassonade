package com.cassonade.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import net.minecraft.world.clock.ServerClockManager;

import com.cassonade.time.LongDay;

@Mixin(ServerClockManager.ServerClockInstance.class)
public abstract class ServerClockInstanceMixin implements LongDay {
	@Unique
	private boolean cassonade$slowed;

	@Override
	public boolean cassonade$isSlowed() {
		return cassonade$slowed;
	}

	@Override
	public void cassonade$setSlowed(boolean slowed) {
		cassonade$slowed = slowed;
	}

	/** The clock advances, and tells the clients it advances, at half its rate in the day; the saved rate stays as set. */
	@ModifyExpressionValue(method = {"tick", "packNetworkState"}, at = @At(value = "FIELD", target = "Lnet/minecraft/world/clock/ServerClockManager$ServerClockInstance;rate:F"))
	private float cassonade$slowDay(float rate) {
		return cassonade$slowed ? rate * LongDay.DAY_RATE : rate;
	}
}
