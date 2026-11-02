package com.cassonade.client.ambience;

import java.util.function.Consumer;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;

import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * {@code /ambience <effect>}, to watch what is rare in runClient, whatever the hour or the weather. Registered only in the
 * development environment: players never get it, so its one message stays out of the language files.
 */
final class AmbienceCommands {
	private AmbienceCommands() {
	}

	static void register() {
		ClientCommandRegistrationCallback.EVENT.register((dispatcher, buildContext) -> {
			LiteralArgumentBuilder<FabricClientCommandSource> root = ClientCommands.literal("ambience");
			root.then(effect("murmuration", Murmuration::release));
			root.then(effect("geese", minecraft -> Birds.release(minecraft, 50)));
			root.then(effect("gulls", minecraft -> Birds.releaseGulls(minecraft, 50)));
			root.then(effect("crows", minecraft -> Birds.releaseCrows(minecraft, 50)));
			// Where the player is looking, these two being over in a moment
			root.then(effect("shootingstar", minecraft -> ShootingStars.release(bearing(minecraft), Math.max(elevation(minecraft), 15))));
			root.then(effect("lightning", minecraft -> HeatLightning.flash(bearing(minecraft))));
			root.then(effect("frogs", AirSounds::croak));
			root.then(effect("crickets", minecraft -> AirSounds.play(minecraft, AirSounds.CRICKETS)));
			root.then(effect("grasshoppers", minecraft -> AirSounds.play(minecraft, AirSounds.GRASSHOPPERS)));
			root.then(effect("waves", minecraft -> Waves.toggleNear()));
			root.then(effect("wavesfar", minecraft -> Waves.toggleFar()));
			root.then(effect("foam", minecraft -> Foam.surge(minecraft, minecraft.level)));
			root.then(effect("breath", Breath::puffAround));
			root.then(ClientCommands.literal("fish").executes(command -> {
				Minecraft minecraft = command.getSource().getClient();
				if (!JumpingFish.release(minecraft, minecraft.level)) {
					command.getSource().sendError(Component.literal("No water two blocks deep, 6 to 22 blocks away"));
					return 0;
				}
				return 1;
			}));
			dispatcher.register(root);
		});
	}

	private static LiteralArgumentBuilder<FabricClientCommandSource> effect(String name, Consumer<Minecraft> effect) {
		return ClientCommands.literal(name).executes(command -> {
			effect.accept(command.getSource().getClient());
			return 1;
		});
	}

	/** 0 to the north, as the effects take it. */
	private static float bearing(Minecraft minecraft) {
		Vec3 look = minecraft.player.getLookAngle();
		return (float) Mth.atan2(look.x, -look.z);
	}

	private static float elevation(Minecraft minecraft) {
		return -minecraft.player.getXRot();
	}
}
