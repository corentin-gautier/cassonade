package com.cassonade.test;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

/** The netherite sword hits for 10; the netherite pickaxe, with Efficiency V and Haste II, breaks every deepslate block at once. */
public class NetheriteClientGameTest implements FabricClientGameTest {
	@Override
	public void runTest(ClientGameTestContext context) {
		if (TestSelection.skips(this)) {
			return;
		}
		try (TestSingleplayerContext world = context.worldBuilder().create()) {
			world.getServer().runCommand("gamemode survival @a");
			world.getServer().runCommand("item replace entity @a weapon.mainhand with netherite_sword");
			context.waitTicks(2);
			world.getServer().runOnServer(server -> {
				ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
				double damage = player.getAttributeValue(Attributes.ATTACK_DAMAGE);
				check(damage == 10, "a netherite sword hits for 10, not " + damage);

				ServerLevel level = player.level();
				player.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.NETHERITE_PICKAXE));
				check(progress(player, level, Blocks.DEEPSLATE) < 1, "deepslate takes more than a tick without enchantment");
				player.setItemSlot(EquipmentSlot.MAINHAND, efficient(level, Items.NETHERITE_PICKAXE));
				player.addEffect(new MobEffectInstance(MobEffects.HASTE, 600, 1));
			});
			// Efficiency's mining speed comes with the equipment change, on the next tick
			context.waitTicks(2);
			world.getServer().runOnServer(server -> {
				ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
				ServerLevel level = player.level();
				for (Block block : new Block[] { Blocks.STONE, Blocks.DEEPSLATE, Blocks.COBBLED_DEEPSLATE, Blocks.DEEPSLATE_DIAMOND_ORE, Blocks.DEEPSLATE_TILES }) {
					check(progress(player, level, block) >= 1, block + " breaks at once");
				}
				player.setItemSlot(EquipmentSlot.MAINHAND, efficient(level, Items.DIAMOND_PICKAXE));
			});
			context.waitTicks(2);
			world.getServer().runOnServer(server -> {
				ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
				check(progress(player, player.level(), Blocks.DEEPSLATE) < 1, "a diamond pickaxe does not break deepslate at once");
			});
		}
	}

	private static ItemStack efficient(ServerLevel level, Item item) {
		ItemStack stack = new ItemStack(item);
		stack.enchant(level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.EFFICIENCY), 5);
		return stack;
	}

	private static float progress(ServerPlayer player, ServerLevel level, Block block) {
		BlockPos pos = player.blockPosition().east(2);
		level.setBlockAndUpdate(pos, block.defaultBlockState());
		return level.getBlockState(pos).getDestroyProgress(player, level, pos);
	}

	private static void check(boolean condition, String what) {
		if (!condition) {
			throw new AssertionError("Expected: " + what);
		}
	}
}
