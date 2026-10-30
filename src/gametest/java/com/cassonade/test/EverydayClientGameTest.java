package com.cassonade.test;

import java.util.ArrayList;
import java.util.List;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.vehicle.boat.AbstractBoat;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.display.RecipeDisplayId;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import com.cassonade.client.quest.QuestLog;
import com.cassonade.client.quest.QuestOptInScreen;
import com.cassonade.crafting.CraftAllPayload;

/**
 * Everyday details: ripe wheat harvested and replanted with a right click, a boat climbing from a path onto
 * grass, a villager on a lead, the hand refilled from the inventory, a recipe crafted until the ingredients run out.
 */
public class EverydayClientGameTest implements FabricClientGameTest {
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
			// A right click on ripe wheat harvests it and plants it again
			context.setScreen(() -> null);
			world.getServer().runCommand("execute as @a at @s run setblock ~ ~-1 ~2 farmland");
			world.getServer().runCommand("execute as @a at @s run setblock ~ ~ ~2 wheat[age=7]");
			world.getServer().runCommand("execute as @a at @s run tp @s ~ ~ ~ 0 35");
			context.waitTicks(3);
			context.getInput().pressKey(options -> options.keyUse);
			world.getConnection().waitForServerboundPackets();
			context.waitTicks(2);
			world.getServer().runOnServer(server -> {
				ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
				BlockState wheat = player.level().getBlockState(player.blockPosition().south(2));
				check(wheat.is(Blocks.WHEAT) && wheat.getValue(CropBlock.AGE) == 0, "right click replants ripe wheat");
				check(!player.level().getEntitiesOfClass(ItemEntity.class, player.getBoundingBox().inflate(4),
						item -> item.getItem().is(Items.WHEAT)).isEmpty(), "right click harvests the wheat");
			});
			world.getServer().runCommand("execute as @a at @s run setblock ~2 ~ ~ air");

			// A boat on a dirt path climbs onto the grass, a sixteenth of a block higher
			world.getServer().runCommand("execute as @a at @s run fill ~4 ~-1 ~-1 ~7 ~-1 ~1 dirt_path");
			world.getServer().runCommand("execute as @a at @s run summon oak_boat ~5.5 ~ ~0.5");
			context.waitTicks(10);
			world.getServer().runOnServer(server -> {
				ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
				AbstractBoat boat = player.level().getEntitiesOfClass(AbstractBoat.class, player.getBoundingBox().inflate(8)).getFirst();
				double startX = boat.getX();
				// Pushed east, and down by gravity as every tick on land
				for (int step = 0; step < 8; step++) {
					boat.move(MoverType.SELF, new Vec3(0.5, -0.04, 0));
				}
				check(boat.getX() > startX + 3.5 && boat.getY() == player.getY(), "a boat climbs from a dirt path onto grass (moved "
						+ (boat.getX() - startX) + ", at y " + boat.getY() + " for the grass at " + player.getY() + ")");
				boat.discard();
			});

			// A lead ties a villager
			world.getServer().runCommand("execute as @a at @s run summon villager ~ ~ ~-2 {NoAI:1b}");
			world.getServer().runCommand("item replace entity @a weapon.mainhand with lead");
			context.waitTicks(2);
			world.getServer().runOnServer(server -> {
				ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
				Villager villager = player.level().getEntitiesOfClass(Villager.class, player.getBoundingBox().inflate(8)).getFirst();
				villager.interact(player, InteractionHand.MAIN_HAND, villager.position());
				check(villager.getLeashHolder() == player, "a lead ties a villager");
				villager.discard();
			});
			world.getServer().runCommand("clear @a lead");

			// The last block placed from the hand is replaced from the inventory, and only from the inventory
			world.getServer().runCommand("execute as @a at @s run setblock ~2 ~-1 ~ stone");
			world.getServer().runCommand("execute as @a at @s run setblock ~2 ~ ~ air");
			world.getServer().runCommand("item replace entity @a hotbar.0 with cobblestone 1");
			world.getServer().runCommand("item replace entity @a inventory.10 with cobblestone 10");
			world.getServer().runOnServer(server -> {
				ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
				player.getInventory().setSelectedSlot(0);
				BlockPos ground = player.blockPosition().east(2).below();
				player.gameMode.useItemOn(player, player.level(), player.getMainHandItem(), InteractionHand.MAIN_HAND,
						new BlockHitResult(Vec3.atCenterOf(ground).add(0, 0.5, 0), Direction.UP, ground, false));
				check(player.level().getBlockState(ground.above()).is(Blocks.COBBLESTONE), "the last cobblestone of the hand placed");
				check(player.getMainHandItem().is(Items.COBBLESTONE) && player.getMainHandItem().getCount() == 10
						&& player.getInventory().getItem(Inventory.SELECTION_SIZE + 10).isEmpty(), "the hand refilled from the inventory");
			});
			world.getServer().runCommand("clear @a cobblestone");
			world.getServer().runCommand("execute as @a at @s run setblock ~2 ~ ~ air");

			// A control click on a recipe crafts it until the ingredients run out, over more than one full grid
			world.getServer().runCommand("clear @a oak_log");
			world.getServer().runCommand("clear @a oak_planks");
			world.getServer().runCommand("recipe give @a minecraft:oak_planks");
			world.getServer().runCommand("give @a oak_log 70");
			context.waitTicks(2);
			RecipeDisplayId planks = world.getServer().computeOnServer(server -> {
				List<RecipeDisplayId> ids = new ArrayList<>();
				server.getRecipeManager().listDisplaysForRecipe(ResourceKey.create(Registries.RECIPE, Identifier.withDefaultNamespace("oak_planks")),
						entry -> ids.add(entry.id()));
				return ids.getFirst();
			});
			context.runOnClient(client -> ClientPlayNetworking.send(new CraftAllPayload(client.player.inventoryMenu.containerId, planks)));
			world.getConnection().waitForServerboundPackets();
			world.getServer().runOnServer(server -> {
				Inventory inventory = server.getPlayerList().getPlayers().getFirst().getInventory();
				check(inventory.countItem(Items.OAK_LOG) == 0 && inventory.countItem(Items.OAK_PLANKS) > 70,
						"craft all turns every log into planks (logs " + inventory.countItem(Items.OAK_LOG) + ", planks "
								+ inventory.countItem(Items.OAK_PLANKS) + ")");
			});
			world.getServer().runCommand("clear @a oak_planks");
		}
	}

	private static void check(boolean condition, String what) {
		if (!condition) {
			throw new AssertionError("Expected: " + what);
		}
	}
}
