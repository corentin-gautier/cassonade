package com.cassonade.test;

import java.util.List;
import java.util.stream.Stream;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.BundleContents;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ComposterBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import com.cassonade.client.HopperFilterScreen;
import com.cassonade.client.quest.QuestLog;
import com.cassonade.client.quest.QuestOptInScreen;
import com.cassonade.hopper.HopperFilter;
import com.cassonade.hopper.HopperFilterMenu;

/**
 * A hopper's filter takes only its item, a bundle's or a Shulker box's items, and is dropped with it;
 * a full composter gives its eight bone meal at once, to a hopper below or by hand.
 */
public class HopperClientGameTest implements FabricClientGameTest {
	@Override
	public void runTest(ClientGameTestContext context) {
		if (TestSelection.skips(this)) {
			return;
		}
		context.getInput().resizeWindow(1920, 1080);
		try (TestSingleplayerContext world = context.worldBuilder().create()) {
			world.getConnection().waitForChunksRender();
			context.waitForScreen(QuestOptInScreen.class);
			context.runOnClient(client -> QuestLog.setEnabled(false));
			context.setScreen(() -> null);
			world.getServer().runCommand("gamemode survival @a");
			// A hopper whose filter is set to stone, then put under a chest of stone and dirt, only takes the stone
			world.getServer().runCommand("execute as @a at @s run setblock ~-3 ~ ~ hopper");
			context.waitTicks(2);
			world.getServer().runOnServer(server -> {
				ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
				check(!player.level().getBlockState(player.blockPosition().west(3)).getValue(HopperFilter.FILTERED), "a placed hopper has no filter");
				player.openMenu((MenuProvider) player.level().getBlockEntity(player.blockPosition().west(3)));
				player.containerMenu.getSlot(HopperFilterMenu.FILTER_SLOT).set(new ItemStack(Items.STONE));
			});
			context.waitForScreen(HopperFilterScreen.class);
			world.getServer().runCommand("execute as @a at @s run setblock ~-3 ~1 ~ chest{Items:[{Slot:0b,id:\"minecraft:dirt\",count:3},"
					+ "{Slot:1b,id:\"minecraft:stone\",count:3},{Slot:2b,id:\"minecraft:dirt\",count:2}]}");
			context.waitTicks(40);
			Screenshots.take(context, "cassonade-15-hopper-filter");
			check(context.computeOnClient(client -> client.level.getBlockState(client.player.blockPosition().west(3)).getValue(HopperFilter.FILTERED)),
					"the client sees the hopper with its filter's look");
			world.getServer().runOnServer(server -> {
				ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
				Container hopper = (Container) player.level().getBlockEntity(player.blockPosition().west(3));
				Container chest = (Container) player.level().getBlockEntity(player.blockPosition().west(3).above());
				check(hopper.countItem(Items.STONE) == 3 && hopper.countItem(Items.DIRT) == 0 && chest.countItem(Items.DIRT) == 5,
						"the hopper takes the stone and leaves the dirt (stone " + hopper.countItem(Items.STONE) + ", dirt "
								+ hopper.countItem(Items.DIRT) + ")");
				check(!player.containerMenu.getSlot(0).mayPlace(new ItemStack(Items.DIRT)), "the hopper's slots refuse dirt");

				// A bundle or a shulker box with items in it lets each of them through; an empty one filters like any item
				ItemStack bundle = new ItemStack(Items.BUNDLE);
				bundle.set(DataComponents.BUNDLE_CONTENTS, BundleContents.EMPTY.copyWithContents(Stream.of(new ItemStack(Items.STONE),
						new ItemStack(Items.DIRT))));
				ItemStack box = new ItemStack(Items.SHULKER_BOX);
				box.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(List.of(ItemStack.EMPTY, new ItemStack(Items.SAND))));
				check(HopperFilter.accepts(bundle, new ItemStack(Items.DIRT)) && !HopperFilter.accepts(bundle, new ItemStack(Items.GRAVEL))
						&& !HopperFilter.accepts(bundle, new ItemStack(Items.BUNDLE)), "a bundle filter lets only its items through");
				check(HopperFilter.accepts(box, new ItemStack(Items.SAND)) && !HopperFilter.accepts(box, new ItemStack(Items.SHULKER_BOX)),
						"a shulker box filter lets only its items through");
				check(HopperFilter.accepts(new ItemStack(Items.BUNDLE), new ItemStack(Items.BUNDLE))
						&& HopperFilter.accepts(new ItemStack(Items.SHULKER_BOX), new ItemStack(Items.SHULKER_BOX))
						&& !HopperFilter.accepts(new ItemStack(Items.SHULKER_BOX), new ItemStack(Items.SAND)), "an empty bundle or shulker box filters itself");
			});
			context.runOnClient(client -> client.player.closeContainer());
			world.getServer().runCommand("execute as @a at @s run setblock ~-3 ~1 ~ air");
			world.getServer().runCommand("execute as @a at @s run setblock ~-3 ~ ~ air destroy");
			context.waitTicks(2);
			world.getServer().runOnServer(server -> {
				ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
				List<ItemEntity> drops = player.level().getEntitiesOfClass(ItemEntity.class, player.getBoundingBox().inflate(5));
				int stone = drops.stream().filter(drop -> drop.getItem().is(Items.STONE)).mapToInt(drop -> drop.getItem().getCount()).sum();
				check(stone == 4 && drops.stream().anyMatch(drop -> drop.getItem().is(Items.HOPPER)),
						"a broken hopper drops itself, its contents and its filter (stone " + stone + ")");
				drops.forEach(ItemEntity::discard);
			});

			// A composter filled to its last layer is ready at once, and the hopper below takes its eight bone meal together
			world.getServer().runCommand("execute as @a at @s run setblock ~-5 ~ ~ hopper");
			world.getServer().runCommand("execute as @a at @s run setblock ~-5 ~1 ~ composter[level=7]");
			context.waitTicks(12);
			world.getServer().runOnServer(server -> {
				ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
				Container hopper = (Container) player.level().getBlockEntity(player.blockPosition().west(5));
				int level = player.level().getBlockState(player.blockPosition().west(5).above()).getValue(ComposterBlock.LEVEL);
				check(hopper.countItem(Items.BONE_MEAL) == 8 && level == 0,
						"the hopper takes the composter's eight bone meal (" + hopper.countItem(Items.BONE_MEAL) + ", level " + level + ")");
			});
			// Emptied by hand, it gives the eight too
			world.getServer().runOnServer(server -> {
				ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
				BlockPos composter = player.blockPosition().west(5).above();
				BlockState ready = Blocks.COMPOSTER.defaultBlockState().setValue(ComposterBlock.LEVEL, ComposterBlock.READY);
				player.level().setBlockAndUpdate(composter, ready);
				ComposterBlock.extractProduce(player, ready, player.level(), composter);
				List<ItemEntity> drops = player.level().getEntitiesOfClass(ItemEntity.class, new AABB(composter).inflate(2));
				int boneMeal = drops.stream().filter(drop -> drop.getItem().is(Items.BONE_MEAL)).mapToInt(drop -> drop.getItem().getCount()).sum();
				check(boneMeal == 8, "an emptied composter gives eight bone meal (" + boneMeal + ")");
				drops.forEach(ItemEntity::discard);
			});
			world.getServer().runCommand("execute as @a at @s run setblock ~-5 ~1 ~ air");
			world.getServer().runCommand("execute as @a at @s run setblock ~-5 ~ ~ air");
		}
	}

	private static void check(boolean condition, String what) {
		if (!condition) {
			throw new AssertionError("Expected: " + what);
		}
	}
}
