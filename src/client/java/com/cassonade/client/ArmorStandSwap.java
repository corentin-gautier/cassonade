package com.cassonade.client;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.component.SwingAnimation;

import com.cassonade.ServerSupport;
import com.cassonade.tweaks.SwapArmorPayload;

/** Alt and a right click with an empty hand asks the server to swap the armor; on a server without the mod, the click stays vanilla. */
public final class ArmorStandSwap {
	private ArmorStandSwap() {
	}

	public static void init() {
		UseEntityCallback.EVENT.register((player, level, hand, entity, hit) -> {
			// The slot lock's key, Alt by default: a second mapping on the same key would conflict with it
			if (!level.isClientSide() || hand != InteractionHand.MAIN_HAND || !(entity instanceof ArmorStand stand) || stand.isMarker()
					|| !CassonadeClient.LOCK_SLOT.isDown() || !player.getMainHandItem().isEmpty() || !ServerSupport.onClient()) {
				return InteractionResult.PASS;
			}
			ClientPlayNetworking.send(new SwapArmorPayload(stand.getId()));
			player.swing(hand, SwingAnimation.DEFAULT, false);
			// Not a success: that would also send the click, which would take a piece off the stand
			return InteractionResult.FAIL;
		});
	}
}
