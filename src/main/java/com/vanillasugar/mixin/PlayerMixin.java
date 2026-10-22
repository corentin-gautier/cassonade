package com.vanillasugar.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.gamerules.GameRules;

import com.vanillasugar.gear.GearSlot;
import com.vanillasugar.gear.PlayerGear;

@Mixin(Player.class)
public abstract class PlayerMixin {
	/** Gear drops on death like the rest of the inventory, unless keepInventory is on. */
	@Inject(method = "dropEquipment", at = @At("TAIL"))
	private void vanillasugar$dropGear(ServerLevel level, CallbackInfo ci) {
		if (level.getGameRules().get(GameRules.KEEP_INVENTORY)) {
			return;
		}

		Player player = (Player) (Object) this;
		PlayerGear gear = PlayerGear.of(player);
		for (GearSlot slot : GearSlot.values()) {
			ItemStack stack = gear.get(slot);
			if (!stack.isEmpty()) {
				ItemEntity drop = player.createItemStackToDrop(stack, true, false);
				if (drop != null) {
					level.addFreshEntity(drop);
				}
			}
		}
		player.setAttached(PlayerGear.ATTACHMENT, PlayerGear.EMPTY);
	}
}
