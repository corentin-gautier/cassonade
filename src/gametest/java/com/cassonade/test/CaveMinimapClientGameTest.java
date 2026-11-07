package com.cassonade.test;

import static com.cassonade.test.PlannedWorld.check;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.MapColor;

import com.cassonade.client.map.Minimap;
import com.cassonade.client.map.TerrainColors;
import com.cassonade.gear.GearSlot;
import com.cassonade.gear.PlayerGear;

/** Deep in a cave, the minimap shows the floor the player walks on, and the surface again once out. */
public class CaveMinimapClientGameTest implements FabricClientGameTest {
	@Override
	public void runTest(ClientGameTestContext context) {
		if (TestSelection.skips(this)) {
			return;
		}
		context.getInput().resizeWindow(1920, 1080);
		try (TestSingleplayerContext world = PlannedWorld.open(context)) {
			world.getServer().runOnServer(server -> {
				ServerPlayer player = PlannedWorld.player(server);
				player.setAttached(PlayerGear.ATTACHMENT, PlayerGear.of(player).with(GearSlot.COMPASS, new ItemStack(Items.COMPASS)));
			});
			context.waitTicks(10);
			check(!context.computeOnClient(client -> Minimap.inCave()), "the surface under the open sky");

			// Under a forest's thick canopy, far above and dark below, the minimap stays on the surface
			world.getServer().runCommand("gamemode creative @a");
			PlannedWorld.load(context, world, 80, 80, 120, 120);
			world.getServer().runCommand("fill 80 150 80 120 150 120 minecraft:grass_block");
			world.getServer().runCommand("fill 80 151 80 120 157 120 minecraft:air");
			world.getServer().runCommand("fill 80 158 80 120 162 120 minecraft:oak_leaves[persistent=true]");
			world.getServer().runCommand("tp @a 100.5 151 100.5");
			context.waitFor(client -> client.player.getBlockY() == 151, 20 * 5);
			context.waitTicks(40);
			check(!context.computeOnClient(client -> Minimap.inCave()), "the surface under a forest's canopy");

			// A sealed hall under 20 blocks of rock, its floor of gold
			PlannedWorld.load(context, world, 40, 40, 64, 64);
			world.getServer().runCommand("fill 40 150 40 64 180 64 minecraft:stone");
			world.getServer().runCommand("fill 42 152 42 62 156 62 minecraft:air");
			world.getServer().runCommand("fill 42 151 42 62 151 62 minecraft:gold_block");
			// A vein of diorite in the rock around, which must not show as white blobs
			world.getServer().runCommand("fill 40 152 40 64 156 41 minecraft:diorite");
			// A ledge of emerald above the player's head, which shows as floor, not as rock
			world.getServer().runCommand("fill 56 152 56 62 154 62 minecraft:emerald_block");
			world.getServer().runCommand("tp @a 52.5 152 52.5");
			context.waitFor(client -> Minimap.inCave(), 20 * 10);
			context.waitTicks(10);
			float difference = context.computeOnClient(Minimap::differenceFromTerrain);
			check(difference <= 0.01F, "the minimap shows the cave's floor, " + difference + " differs");
			byte emerald = MapColor.EMERALD.getPackedId(MapColor.Brightness.NORMAL);
			// Any brightness: the slope shades it
			check((context.computeOnClient(client -> Minimap.colorAt(60, 60)) & 0xFF) >> 2 == (emerald & 0xFF) >> 2, "the ledge above the head shows as floor");
			Screenshots.take(context, "cassonade-cave-minimap");

			// A jump leaves the cut where it was: in the air a block up, then back on the ground
			TerrainColors.Cut before = context.computeOnClient(client -> Minimap.cut());
			world.getServer().runCommand("tp @a 52.5 153.2 52.5");
			context.waitTicks(2);
			check(context.computeOnClient(client -> !client.player.onGround() && Minimap.cut().equals(before)), "the cut stays through a jump");
			context.waitFor(client -> client.player.onGround(), 20 * 3);
			context.waitTicks(2);
			check(context.computeOnClient(client -> Minimap.cut().equals(before)), "the cut is the same once landed");

			world.getServer().runCommand("tp @a 2.5 101 2.5");
			context.waitFor(client -> !Minimap.inCave(), 20 * 5);
		}
	}
}
