package com.cassonade.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

import com.cassonade.client.SwingingDoors;

/** A door, trapdoor or fence gate opening or closing starts to swing (SwingingDoors), before its section is marked to be built again. */
@Mixin(ClientLevel.class)
abstract class ClientLevelMixin {
	@Inject(method = "sendBlockUpdated", at = @At("HEAD"))
	private void cassonade$swing(BlockPos pos, BlockState old, BlockState current, int updateFlags, CallbackInfo ci) {
		SwingingDoors.changed((ClientLevel) (Object) this, pos, old, current);
	}
}
