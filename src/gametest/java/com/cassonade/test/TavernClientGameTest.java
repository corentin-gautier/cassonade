package com.cassonade.test;

import java.util.List;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

/**
 * Each village's church template now holds a tavern: a brewing stand for the cleric, kegs with the temple's loot, a
 * lit fireplace and lanterns, and a screenshot of a few from outside and inside, then of the tavern keeper.
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
				world.getServer().runCommand("forceload add 0 " + i * SPACING + " 20 " + (i * SPACING + 12));
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
			shoot(context, world, "tavern-plains", -12, ground + 7, 0 * SPACING + 12, 0 * SPACING + 4);
			shoot(context, world, "tavern-desert", -12, ground + 7, 3 * SPACING + 14, 3 * SPACING + 5);
			shoot(context, world, "tavern-taiga", -12, ground + 7, 7 * SPACING + 14, 7 * SPACING + 5);
			world.getServer().runCommand("tp @a 6.5 " + (ground + 1) + " " + (SPACING + 5.5) + " facing 15 " + (ground + 1.5) + " " + (SPACING + 4));
			world.getConnection().waitForChunksRender();
			context.waitTicks(20);
			clearChat(context);
			Screenshots.take(context, "tavern-inside");
			// The tavern keeper, the cleric renamed and dressed for the tavern, behind its counter
			world.getServer().runCommand("summon villager 10.5 " + (ground + 1) + " " + (SPACING + 2.5)
					+ " {NoAI:1b,Rotation:[0f,0f],VillagerData:{profession:\"minecraft:cleric\",level:1,type:\"minecraft:plains\"}}");
			world.getServer().runCommand("tp @a 10.5 " + (ground + 0.2) + " " + (SPACING + 5.5) + " facing 10.5 " + (ground + 1.6) + " " + (SPACING + 2.5));
			context.waitTicks(20);
			String name = context.computeOnClient(client -> Component.translatable("entity.minecraft.villager.cleric").getString());
			if (!name.equals("Tavern Keeper")) {
				throw new AssertionError("Expected the cleric to be called Tavern Keeper, found " + name);
			}
			clearChat(context);
			Screenshots.take(context, "tavern-keeper");
		}
	}

	private static void shoot(ClientGameTestContext context, TestSingleplayerContext world, String name, int x, int y, int z, int lookZ) {
		world.getServer().runCommand("tp @a " + x + " " + y + " " + z + " facing 8 " + (y - 3) + " " + lookZ);
		world.getConnection().waitForChunksRender();
		context.waitTicks(20);
		clearChat(context);
		Screenshots.take(context, name);
	}

	private static void clearChat(ClientGameTestContext context) {
		context.runOnClient(client -> client.gui.hud.getChat().clearMessages(false));
	}
}
