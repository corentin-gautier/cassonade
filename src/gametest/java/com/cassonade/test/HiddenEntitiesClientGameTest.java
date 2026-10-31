package com.cassonade.test;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.entity.decoration.ArmorStand;

import com.cassonade.client.HiddenEntities;
import com.cassonade.client.quest.QuestLog;
import com.cassonade.client.quest.QuestOptInScreen;

/**
 * Only what no player can see goes undrawn: a cow sealed in rock is hidden, the same cow is not once glowing, named,
 * leashed or in the open.
 */
public class HiddenEntitiesClientGameTest implements FabricClientGameTest {
	@Override
	public void runTest(ClientGameTestContext context) {
		if (TestSelection.skips(this)) {
			return;
		}
		context.getInput().resizeWindow(1920, 1080);
		try (TestSingleplayerContext world = context.worldBuilder().create()) {
			context.waitForScreen(QuestOptInScreen.class);
			context.runOnClient(client -> QuestLog.setEnabled(false));
			context.setScreen(() -> null);
			world.getConnection().waitForChunksRender();

			// A closed room around the player, a second one ahead with the cow: nothing connects the two
			world.getServer().runCommand("gamemode survival @a");
			world.getServer().runCommand("gamerule max_block_modifications 1000000");
			world.getServer().runCommand("tp @a 8 40 8 0 0");
			world.getServer().runCommand("fill -24 -40 -24 40 90 72 stone");
			world.getServer().runCommand("fill 4 38 4 12 44 12 air");
			world.getServer().runCommand("fill 4 38 44 12 44 52 air");
			world.getServer().runCommand("summon cow 8 40 48 {NoAI:1b}");
			world.getServer().runCommand("summon armor_stand 10 40 48");
			context.waitTicks(20);
			world.getConnection().waitForChunksRender();
			context.waitTicks(20);
			check(context.computeOnClient(HiddenEntitiesClientGameTest::cowHidden), "a cow sealed away is not drawn");

			world.getServer().runCommand("effect give @e[type=cow] glowing 100 0 true");
			context.waitTicks(10);
			check(!context.computeOnClient(HiddenEntitiesClientGameTest::cowHidden), "a glowing cow shows through the walls");
			world.getServer().runCommand("effect clear @e[type=cow]");
			context.waitTicks(10);

			world.getServer().runCommand("data merge entity @e[type=cow,limit=1] {CustomName:\"Marguerite\"}");
			context.waitTicks(10);
			check(!context.computeOnClient(HiddenEntitiesClientGameTest::cowHidden), "a named cow keeps its name tag");
			world.getServer().runCommand("data remove entity @e[type=cow,limit=1] CustomName");
			context.waitTicks(10);

			// A leash can reach sections that are seen; held nearby here, since it breaks past 12 blocks
			leash(world, true);
			context.waitTicks(10);
			check(!context.computeOnClient(HiddenEntitiesClientGameTest::cowHidden), "a leashed cow keeps its leash");
			leash(world, false);
			context.waitTicks(10);
			check(context.computeOnClient(HiddenEntitiesClientGameTest::cowHidden), "with nothing left to show, hidden again");

			// The wall opened, the sections between the two rooms can be seen through
			world.getServer().runCommand("fill 4 38 12 12 44 44 air");
			context.waitTicks(20);
			world.getConnection().waitForChunksRender();
			context.waitTicks(20);
			check(!context.computeOnClient(HiddenEntitiesClientGameTest::cowHidden), "a cow in the open is drawn");
			context.runOnClient(client -> client.gui.hud.toggle());
			context.waitTicks(2);
			Screenshots.take(context, "cassonade-hidden-entities-cow-in-the-open");
			context.runOnClient(client -> client.gui.hud.toggle());
		}
	}

	private static void leash(TestSingleplayerContext world, boolean leashed) {
		world.getServer().runOnServer(server -> {
			Entity holder = null;
			Cow cow = null;
			for (Entity entity : server.overworld().getAllEntities()) {
				if (entity instanceof Cow found) {
					cow = found;
				} else if (entity instanceof ArmorStand) {
					holder = entity;
				}
			}
			if (leashed) {
				cow.setLeashedTo(holder, true);
			} else {
				cow.dropLeash();
			}
		});
	}

	private static boolean cowHidden(Minecraft client) {
		for (Entity entity : client.level.entitiesForRendering()) {
			if (entity.is(EntityTypes.COW)) {
				return HiddenEntities.isHidden(entity, entity.getBoundingBox());
			}
		}
		throw new AssertionError("the cow is known to the client");
	}

	private static void check(boolean condition, String what) {
		if (!condition) {
			throw new AssertionError(what);
		}
	}
}
