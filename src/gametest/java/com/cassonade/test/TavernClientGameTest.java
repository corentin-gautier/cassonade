package com.cassonade.test;

import java.util.List;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

/**
 * Each village's church template now holds a tavern: a brewing stand for the cleric, kegs with the temple's loot, a
 * lit fireplace and lanterns, and a screenshot of a few from outside and inside.
 */
public class TavernClientGameTest implements FabricClientGameTest {
	private static final List<String> TEMPLATES = List.of("plains/houses/plains_temple_3", "plains/houses/plains_temple_4", "desert/houses/desert_temple_1",
			"desert/houses/desert_temple_2", "savanna/houses/savanna_temple_1", "savanna/houses/savanna_temple_2", "snowy/houses/snowy_temple_1",
			"taiga/houses/taiga_temple_1", "taiga/zombie/houses/taiga_temple_1");
	/** Blocks between two taverns, along z. */
	private static final int SPACING = 16;

	@Override
	public void runTest(ClientGameTestContext context) {
		if (TestSelection.skips(this)) {
			return;
		}
		context.getInput().resizeWindow(1920, 1080);
		try (TestSingleplayerContext world = context.worldBuilder().create()) {
			world.getServer().runCommand("gamemode spectator @a");
			world.getServer().runCommand("gamerule advance_time false");
			world.getServer().runCommand("time set 1000");
			int ground = world.getServer().computeOnServer(server -> server.overworld().getHeight(Heightmap.Types.WORLD_SURFACE, 0, 0));
			for (int i = 0; i < TEMPLATES.size(); i++) {
				world.getServer().runCommand("forceload add 0 " + i * SPACING + " 16 " + (i * SPACING + 12));
				world.getServer().runCommand("place template minecraft:village/" + TEMPLATES.get(i) + " 0 " + ground + " " + i * SPACING);
			}
			world.getServer().runOnServer(server -> {
				ServerLevel level = server.overworld();
				for (int i = 0; i < TEMPLATES.size(); i++) {
					String name = TEMPLATES.get(i);
					StructureTemplate template = server.getStructureTemplateManager().get(Identifier.withDefaultNamespace("village/" + name)).orElseThrow();
					Vec3i size = template.getSize();
					int brewing = 0;
					int campfires = 0;
					int lanterns = 0;
					int kegs = 0;
					for (BlockPos pos : BlockPos.betweenClosed(0, ground, i * SPACING, size.getX() - 1, ground + size.getY() - 1, i * SPACING + size.getZ() - 1)) {
						if (level.getBlockState(pos).is(Blocks.BREWING_STAND)) {
							brewing++;
						} else if (level.getBlockState(pos).is(Blocks.CAMPFIRE)) {
							campfires++;
						} else if (level.getBlockState(pos).is(Blocks.LANTERN)) {
							lanterns++;
						} else if (level.getBlockEntity(pos) instanceof BarrelBlockEntity barrel && barrel.getLootTable() != null) {
							kegs++;
						}
					}
					System.out.println("Tavern " + name + " " + size + ": " + brewing + " brewing stand, " + campfires + " campfire, " + lanterns + " lanterns, " + kegs + " kegs with loot");
					if (brewing != 1 || campfires != 1 || lanterns < 3 || kegs != 1) {
						throw new AssertionError("Expected a tavern in " + name + ", found " + brewing + " brewing stands, " + campfires + " campfires, " + lanterns
								+ " lanterns and " + kegs + " kegs with loot");
					}
				}
			});
			// Outside, from the street side, then inside from the door
			context.runOnClient(client -> client.gui.hud.getChat().clearMessages(false));
			shoot(context, world, "tavern-plains", -9, ground + 5, 0 * SPACING + 12, 0 * SPACING + 4);
			shoot(context, world, "tavern-desert", -9, ground + 5, 3 * SPACING + 14, 3 * SPACING + 5);
			shoot(context, world, "tavern-taiga", -9, ground + 5, 7 * SPACING + 14, 7 * SPACING + 5);
			world.getServer().runCommand("tp @a 2.5 " + (ground + 1) + " " + (SPACING + 5.5) + " facing 11 " + (ground + 1.5) + " " + (SPACING + 4));
			world.getConnection().waitForChunksRender();
			context.waitTicks(20);
			Screenshots.take(context, "tavern-inside");
		}
	}

	private static void shoot(ClientGameTestContext context, TestSingleplayerContext world, String name, int x, int y, int z, int lookZ) {
		world.getServer().runCommand("tp @a " + x + " " + y + " " + z + " facing 6 " + (y - 3) + " " + lookZ);
		world.getConnection().waitForChunksRender();
		context.waitTicks(20);
		Screenshots.take(context, name);
	}
}
