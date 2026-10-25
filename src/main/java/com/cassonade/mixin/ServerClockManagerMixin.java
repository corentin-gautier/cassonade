package com.cassonade.mixin;

import java.util.Map;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.core.Holder;
import net.minecraft.network.protocol.game.ClientboundSetTimePacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.clock.ServerClockManager;
import net.minecraft.world.clock.WorldClock;
import net.minecraft.world.clock.WorldClocks;

import com.cassonade.time.LongDay;

@Mixin(ServerClockManager.class)
public abstract class ServerClockManagerMixin {
	@Shadow
	private MinecraftServer server;

	@Shadow
	public abstract ServerClockManager.ServerClockInstance getInstance(Holder<WorldClock> definition);

	@Shadow
	private long getGameTime() {
		throw new AssertionError();
	}

	/**
	 * Slows the overworld clock at sunrise and restores it at sunset, see {@link LongDay}. Clients predict the time
	 * from the rate they were last sent, so each change is sent to them, as /time rate does.
	 */
	@Inject(method = "tick", at = @At("HEAD"))
	private void cassonade$paceDay(CallbackInfo ci) {
		server.registryAccess().get(WorldClocks.OVERWORLD).ifPresent(reference -> {
			Holder<WorldClock> clock = reference;
			ServerClockManager.ServerClockInstance instance = getInstance(clock);
			LongDay day = (LongDay) instance;
			boolean slowed = LongDay.isDaytime(instance.totalTicks());
			if (day.cassonade$isSlowed() != slowed) {
				day.cassonade$setSlowed(slowed);
				server.getPlayerList().broadcastAll(new ClientboundSetTimePacket(getGameTime(), Map.of(clock, instance.packNetworkState(server))));
			}
		});
	}
}
