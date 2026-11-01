package com.cassonade.test;

import static com.cassonade.test.PlannedWorld.check;
import static com.cassonade.test.PlannedWorld.player;

import java.util.List;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import com.cassonade.client.waystone.WaystoneScreen;
import com.cassonade.waystone.WaystoneCrystalBlockEntity;
import com.cassonade.waystone.WaystoneData;
import com.cassonade.waystone.WaystoneListPayload;
import com.cassonade.waystone.WaystoneTravelPayload;
import com.cassonade.waystone.Waystones;

public class WaystoneClientGameTest implements FabricClientGameTest {
	@Override
	public void runTest(ClientGameTestContext context) {
		if (TestSelection.skips(this)) {
			return;
		}
		try (TestSingleplayerContext world = PlannedWorld.open(context)) {
			// One item for the two halves, whichever is broken; none without a pickaxe, none in Creative
			checkDrops(world, 0, Items.IRON_PICKAXE, 1, "the lower half broken");
			checkDrops(world, 1, Items.IRON_PICKAXE, 1, "the upper half broken");
			checkDrops(world, 0, Items.AIR, 0, "the lower half broken by hand");
			checkDrops(world, 1, Items.AIR, 0, "the upper half broken by hand");
			world.getServer().runCommand("gamemode creative @a");
			checkDrops(world, 1, Items.AIR, 0, "the upper half broken in Creative");
			world.getServer().runCommand("gamemode survival @a");

			// Alone, it can only be named
			world.getServer().runCommand("setblock 4 101 2 cassonade:waystone");
			world.getServer().runCommand("setblock 4 102 2 cassonade:waystone[half=upper]");
			context.waitTicks(5);
			aim(context, world);
			use(context);
			context.waitForScreen(WaystoneScreen.class);
			context.runOnClient(client -> check(client.gui.screen().children().stream().noneMatch(Button.class::isInstance), "no destination with one waystone"));
			context.runOnClient(client -> {
				EditBox box = client.gui.screen().children().stream().filter(EditBox.class::isInstance).map(EditBox.class::cast).findFirst().orElseThrow();
				box.setValue("  Home ");
			});
			ScreenInput.pressOnScreen(context, InputConstants.KEY_RETURN);
			context.waitTicks(5);
			world.getServer().runOnServer(server -> check(WaystoneData.of(server).at(GlobalPos.of(Level.OVERWORLD, new BlockPos(4, 101, 2))).orElseThrow().name().equals("Home"), "named from its screen"));

			world.getServer().runCommand("forceload add 10 10");
			world.getServer().runCommand("setblock 10 101 10 cassonade:waystone");
			world.getServer().runCommand("setblock 10 102 10 cassonade:waystone[half=upper]");
			world.getServer().runCommand("execute in minecraft:the_nether run forceload add 0 0");
			world.getServer().runCommand("execute in minecraft:the_nether run fill -1 99 -1 1 99 1 stone");
			world.getServer().runCommand("execute in minecraft:the_nether run fill -1 100 -1 1 102 1 air");
			world.getServer().runCommand("execute in minecraft:the_nether run setblock 0 100 0 cassonade:waystone");
			world.getServer().runCommand("execute in minecraft:the_nether run setblock 0 101 0 cassonade:waystone[half=upper]");
			world.getServer().runOnServer(server -> {
				WaystoneData data = WaystoneData.of(server);
				check(data.entries().size() == 3, "three waystones written down: " + data.entries());
				data.put(GlobalPos.of(Level.OVERWORLD, new BlockPos(10, 101, 10)), "Mine");
			});

			// To the named one, first in the list
			use(context);
			context.waitForScreen(WaystoneScreen.class);
			Screenshots.take(context, "cassonade-waystone");
			press(context, 0);
			context.waitTicks(10);
			world.getServer().runOnServer(server -> {
				BlockPos at = player(server).blockPosition();
				check(at.getY() == 101 && at.distManhattan(new BlockPos(10, 101, 10)) <= 2, "at the foot of the other waystone, at " + at);
			});
			world.getServer().runCommand("tp @a 8.5 101 10.5 -90 -18");
			context.waitTicks(10);
			Screenshots.take(context, "cassonade-waystone-block");
			world.getServer().runCommand("tp @a 10.5 101 8.5 0 -18");
			context.waitTicks(23);
			Screenshots.take(context, "cassonade-waystone-block-turned");
			world.getServer().runCommand("item replace entity @a weapon.mainhand with cassonade:waystone");
			context.waitTicks(5);
			context.runOnClient(client -> check(client.level.getBlockEntity(new BlockPos(10, 102, 10)) instanceof WaystoneCrystalBlockEntity
					&& client.level.getBlockEntity(new BlockPos(10, 101, 10)) == null, "the crystal on the top half only"));
			world.getServer().runCommand("item replace entity @a weapon.mainhand with air");
			world.getServer().runCommand("item replace entity @a inventory.0 with cassonade:waystone");
			context.runOnClient(client -> client.gui.setScreen(new InventoryScreen(client.player)));
			context.waitTicks(5);
			Screenshots.take(context, "cassonade-waystone-item");
			context.setScreen(() -> null);

			// Blocked: no trip, the player stays
			world.getServer().runCommand("fill 3 101 1 5 102 3 stone replace air");
			world.getServer().runCommand("tp @a 8.5 101 10.5 90 0");
			context.waitTicks(10);
			context.runOnClient(client -> ClientPlayNetworking.send(new WaystoneTravelPayload(new BlockPos(10, 101, 10), GlobalPos.of(Level.OVERWORLD, new BlockPos(4, 101, 2)))));
			context.waitTicks(10);
			world.getServer().runOnServer(server -> check(player(server).getX() > 8, "walled in: no trip"));

			// Too far from any waystone: no trip
			world.getServer().runCommand("fill 3 101 1 5 102 3 air replace stone");
			context.runOnClient(client -> ClientPlayNetworking.send(new WaystoneTravelPayload(new BlockPos(100, 64, 100), GlobalPos.of(Level.OVERWORLD, new BlockPos(4, 101, 2)))));
			context.waitTicks(10);
			world.getServer().runOnServer(server -> check(player(server).getX() > 8, "a request from nowhere: no trip"));

			// Across dimensions
			aim(context, world);
			use(context);
			context.waitForScreen(WaystoneScreen.class);
			press(context, 1);
			context.waitTicks(20);
			context.runOnClient(client -> check(client.level.dimension() == Level.NETHER, "in the Nether"));

			// Each dimension's row, for the eye
			context.runOnClient(client -> WaystoneScreen.open(client, new WaystoneListPayload(BlockPos.ZERO, "", List.of(
					new WaystoneListPayload.Destination(GlobalPos.of(Level.OVERWORLD, new BlockPos(10, 64, 10)), "Mine"),
					new WaystoneListPayload.Destination(GlobalPos.of(Level.NETHER, new BlockPos(0, 100, 0)), ""),
					new WaystoneListPayload.Destination(GlobalPos.of(Level.END, new BlockPos(100, 50, 0)), "")))));
			context.waitTicks(5);
			ScreenInput.quiet(context);
			Screenshots.take(context, "cassonade-waystone-dimensions");
			context.setScreen(() -> null);

			// Gone from the list when broken
			world.getServer().runCommand("execute in minecraft:overworld run setblock 10 101 10 air");
			world.getServer().runOnServer(server -> check(WaystoneData.of(server).entries().size() == 2, "the broken one struck off"));
			world.getServer().runOnServer(server -> server.saveEverything(true, true, true));
			context.waitTicks(5);
		}
	}

	private static void checkDrops(TestSingleplayerContext world, int half, Item tool, int expected, String what) {
		world.getServer().runCommand("setblock 12 101 12 cassonade:waystone");
		world.getServer().runCommand("setblock 12 102 12 cassonade:waystone[half=upper]");
		world.getServer().runOnServer(server -> {
			ServerPlayer player = player(server);
			player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(tool));
			player.gameMode.destroyBlock(new BlockPos(12, 101 + half, 12));
			AABB around = new AABB(10, 99, 10, 15, 105, 15);
			int drops = server.overworld().getEntitiesOfClass(ItemEntity.class, around).stream().filter(item -> item.getItem().is(Waystones.ITEM))
					.mapToInt(item -> item.getItem().getCount()).sum();
			check(drops == expected, what + ": " + drops + " dropped, expected " + expected);
			check(server.overworld().getBlockState(new BlockPos(12, 101, 12)).isAir() && server.overworld().getBlockState(new BlockPos(12, 102, 12)).isAir(), what + ": both halves gone");
			player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
		});
		world.getServer().runCommand("kill @e[type=item]");
	}

	private static void aim(ClientGameTestContext context, TestSingleplayerContext world) {
		world.getServer().runCommand("execute in minecraft:overworld run tp @a 2.5 101 2.5 -90 15");
		context.waitTicks(10);
	}

	private static void use(ClientGameTestContext context) {
		context.getInput().pressKey(options -> options.keyUse);
		context.waitTicks(5);
	}

	private static void press(ClientGameTestContext context, int row) {
		context.runOnClient(client -> client.gui.screen().children().stream().filter(Button.class::isInstance).map(Button.class::cast)
				.skip(row).findFirst().orElseThrow().onPress(null));
	}
}
