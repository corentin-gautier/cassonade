package com.cassonade.test;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class AnvilClientGameTest implements FabricClientGameTest {
	@Override
	public void runTest(ClientGameTestContext context) {
		if (TestSelection.skips(this)) {
			return;
		}
		try (TestSingleplayerContext world = context.worldBuilder().create()) {
			world.getServer().runOnServer(server -> {
				ServerPlayer player = server.getPlayerList().getPlayers().getFirst();

				AnvilMenu rename = new AnvilMenu(100, player.getInventory());
				ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
				sword.set(DataComponents.REPAIR_COST, 50);
				rename.getSlot(0).set(sword);
				rename.setItemName("Field Sword");
				rename.createResult();
				check(rename.getCost() == 0, "renaming alone costs no levels");
				check(rename.getSlot(2).getItem().getHoverName().getString().equals("Field Sword"),
						"free renaming still produces the renamed item");
				check(rename.getSlot(2).mayPickup(player), "a free rename can be taken");

				AnvilMenu box = new AnvilMenu(102, player.getInventory());
				box.getSlot(0).set(new ItemStack(Items.SHULKER_BOX));
				box.setItemName("Tools");
				box.createResult();
				check(box.getSlot(2).getItem().getHoverName().getString().equals("Tools") && box.getSlot(2).mayPickup(player),
						"a renamed Shulker box can be taken");

				AnvilMenu empty = new AnvilMenu(103, player.getInventory());
				empty.createResult();
				check(!empty.getSlot(2).mayPickup(player), "an empty anvil has nothing to take");

				AnvilMenu repair = new AnvilMenu(101, player.getInventory());
				ItemStack damaged = new ItemStack(Items.DIAMOND_SWORD);
				damaged.setDamageValue(100);
				repair.getSlot(0).set(damaged);
				repair.getSlot(1).set(new ItemStack(Items.DIAMOND));
				repair.setItemName("Repaired Sword");
				repair.createResult();
				check(repair.getCost() > 0, "repairing and renaming still costs levels");
			});
		}
	}

	private static void check(boolean condition, String what) {
		if (!condition) {
			throw new AssertionError("Expected: " + what);
		}
	}
}
