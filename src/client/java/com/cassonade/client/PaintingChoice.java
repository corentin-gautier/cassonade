package com.cassonade.client;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.decoration.painting.Painting;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.EntityHitResult;

import com.cassonade.ServerSupport;
import com.cassonade.settings.Setting;
import com.cassonade.settings.Settings;
import com.cassonade.tweaks.ChoosePaintingPayload;

/**
 * Sneaking and aiming at a painting with a painting in hand, the scroll wheel asks the server for the next one that fits:
 * without sneaking, going through the hotbar in front of a painting would change it by accident. Placing the last of a stack
 * leaves the hand empty, so the painting just placed also answers to an empty hand. On a server without the mod, the wheel stays vanilla.
 */
public final class PaintingChoice {
	/** Where the last painting was placed: a painting's position is the block next to the face clicked. */
	private static BlockPos placedAt;

	private PaintingChoice() {
	}

	public static void init() {
		UseBlockCallback.EVENT.register((player, level, hand, hit) -> {
			if (level.isClientSide() && player.getItemInHand(hand).is(Items.PAINTING)) {
				placedAt = hit.getBlockPos().relative(hit.getDirection());
			}
			return InteractionResult.PASS;
		});
	}

	/** Whether the wheel went to a painting, so not to the hotbar. */
	public static boolean scroll(Minecraft minecraft, double wheel) {
		if (!(minecraft.hitResult instanceof EntityHitResult hit) || !(hit.getEntity() instanceof Painting painting) || !ServerSupport.onClient()
				|| !Settings.on(Setting.PAINTING_CHOICE)
				|| !minecraft.player.isShiftKeyDown()) {
			return false;
		}
		LocalPlayer player = minecraft.player;
		boolean holding = player.getMainHandItem().is(Items.PAINTING) || player.getOffhandItem().is(Items.PAINTING);
		if (!holding && !(player.getMainHandItem().isEmpty() && painting.getPos().equals(placedAt))) {
			return false;
		}
		// Up the wheel goes back, as in the hotbar
		ClientPlayNetworking.send(new ChoosePaintingPayload(painting.getId(), wheel > 0 ? -1 : 1));
		return true;
	}
}
