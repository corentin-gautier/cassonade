package com.cassonade.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.ClientInput;
import net.minecraft.client.player.KeyboardInput;

import net.minecraft.world.entity.vehicle.boat.AbstractBoat;

import com.cassonade.client.IsometricCamera;

/** In the isometric view the keys walk along the screen, in vehicles too. */
@Mixin(KeyboardInput.class)
abstract class KeyboardInputMixin extends ClientInput {
	@Inject(method = "tick", at = @At("TAIL"))
	private void cassonade$walkAlongScreen(CallbackInfo ci) {
		Minecraft minecraft = Minecraft.getInstance();
		if (IsometricCamera.isActive() && minecraft.player != null) {
			if (minecraft.player.getVehicle() instanceof AbstractBoat boat) {
				this.keyPresses = IsometricCamera.steer(this.moveVector, this.keyPresses, boat);
			} else {
				this.moveVector = IsometricCamera.walk(this.moveVector, minecraft.player);
			}
		}
	}
}
