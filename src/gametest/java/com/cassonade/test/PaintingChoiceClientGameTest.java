package com.cassonade.test;

import java.util.HashSet;
import java.util.Set;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

import net.minecraft.core.registries.Registries;
import net.minecraft.tags.PaintingVariantTags;
import net.minecraft.world.entity.decoration.painting.Painting;
import net.minecraft.world.entity.decoration.painting.PaintingVariant;
import net.minecraft.world.phys.EntityHitResult;

import com.cassonade.client.quest.QuestLog;
import com.cassonade.client.quest.QuestOptInScreen;

public class PaintingChoiceClientGameTest implements FabricClientGameTest {
	@Override
	public void runTest(ClientGameTestContext context) {
		if (TestSelection.skips(this)) {
			return;
		}
		try (TestSingleplayerContext world = context.worldBuilder().create()) {
			world.getConnection().waitForChunksRender();
			context.waitForScreen(QuestOptInScreen.class);
			context.runOnClient(client -> QuestLog.setEnabled(false));
			context.setScreen(() -> null);
			world.getServer().runCommand("time set noon");
			world.getServer().runCommand("gamemode survival @a");
			world.getServer().runCommand("fill 0 100 0 15 100 15 stone");
			world.getServer().runCommand("fill 0 101 0 15 112 15 air");
			world.getServer().runCommand("fill 5 101 0 5 112 15 stone");

			// Facing the wall to the east, at eye level: the free wall takes vanilla's largest paintings
			aim(context, world, 4.5);
			hold(context, world, "painting 2");
			place(context);
			String placed = variant(context);
			check(placed != null, "a painting placed");
			int area = area(context);
			int slot = slot(context);
			// Without sneaking, the wheel stays the hotbar's
			scroll(context, -1);
			check(placed.equals(variant(context)) && slot(context) != slot, "no change to the painting without sneaking");
			scroll(context, 1);
			check(slot(context) == slot, "back on the painting's slot");
			context.getInput().holdKey(options -> options.keyShift);
			context.waitTicks(5);
			scroll(context, -1);
			String next = variant(context);
			check(!placed.equals(next) && slot(context) == slot, "the wheel changes the painting, not the hotbar slot");
			check(area(context) <= area, "the next painting no larger than the first");
			Screenshots.take(context, "cassonade-painting-choice");
			scroll(context, 1);
			check(placed.equals(variant(context)), "the wheel the other way goes back");

			// A hole of one block: all of the small ones go by, then the first one again
			world.getServer().runCommand("kill @e[type=painting]");
			world.getServer().runCommand("fill 4 101 0 4 112 15 stone");
			world.getServer().runCommand("setblock 4 102 4 air");
			context.waitTicks(5);
			hold(context, world, "painting 2");
			place(context);
			String first = variant(context);
			Set<String> seen = new HashSet<>(Set.of(first));
			int small = context.computeOnClient(client -> {
				int[] count = {0};
				client.level.registryAccess().lookupOrThrow(Registries.PAINTING_VARIANT).getTagOrEmpty(PaintingVariantTags.PLACEABLE)
						.forEach(variant -> count[0] += variant.value().area() == 1 ? 1 : 0);
				return count[0];
			});
			for (int i = 1; i < small; i++) {
				scroll(context, -1);
				seen.add(variant(context));
				check(area(context) == 1, "only what fits a single block");
			}
			check(seen.size() == small, "every small painting in turn: " + seen);
			scroll(context, -1);
			check(first.equals(variant(context)), "the list goes round");

			// The last painting of a stack: the hand is empty, the wheel still answers
			world.getServer().runCommand("kill @e[type=painting]");
			world.getServer().runCommand("fill 4 101 0 4 112 15 air");
			context.waitTicks(5);
			hold(context, world, "painting 1");
			place(context);
			check(context.computeOnClient(client -> client.player.getMainHandItem().isEmpty()), "the last painting placed");
			placed = variant(context);
			slot = slot(context);
			scroll(context, -1);
			check(!placed.equals(variant(context)) && slot(context) == slot, "the wheel on the painting just placed, with an empty hand");

			// Another painting: neither an empty hand nor another item touches it
			world.getServer().runCommand("summon painting 4 102 12 {facing:1b,variant:\"minecraft:kebab\"}");
			aim(context, world, 12.5);
			context.waitTicks(5);
			String other = variant(context);
			check(other != null, "another painting aimed at");
			slot = slot(context);
			scroll(context, -1);
			check(other.equals(variant(context)) && slot(context) != slot, "an empty hand scrolls the hotbar in front of another painting");
			hold(context, world, "stone");
			slot = slot(context);
			scroll(context, -1);
			check(other.equals(variant(context)) && slot(context) != slot, "another item scrolls the hotbar in front of a painting");

			// A painting in hand reaches any painting, one from a command included
			hold(context, world, "painting");
			scroll(context, -1);
			check(!other.equals(variant(context)), "a painting in hand changes a painting already on the wall");
			context.getInput().releaseKey(options -> options.keyShift);
		}
	}

	private static void aim(ClientGameTestContext context, TestSingleplayerContext world, double z) {
		world.getServer().runCommand("tp @a 2.5 101 " + z + " -90 0");
		context.waitTicks(5);
	}

	private static void hold(ClientGameTestContext context, TestSingleplayerContext world, String item) {
		world.getServer().runCommand("item replace entity @a weapon.mainhand with " + item);
		context.waitTicks(5);
	}

	private static void place(ClientGameTestContext context) {
		context.getInput().pressKey(options -> options.keyUse);
		context.waitTicks(5);
	}

	private static void scroll(ClientGameTestContext context, double wheel) {
		context.getInput().scroll(wheel);
		context.waitTicks(5);
	}

	private static int slot(ClientGameTestContext context) {
		return context.computeOnClient(client -> client.player.getInventory().getSelectedSlot());
	}

	private static String variant(ClientGameTestContext context) {
		return context.computeOnClient(client -> client.hitResult instanceof EntityHitResult hit && hit.getEntity() instanceof Painting painting
				? painting.getVariant().getRegisteredName() : null);
	}

	private static int area(ClientGameTestContext context) {
		return context.computeOnClient(client -> {
			PaintingVariant variant = ((Painting) ((EntityHitResult) client.hitResult).getEntity()).getVariant().value();
			return variant.area();
		});
	}

	private static void check(boolean condition, String what) {
		if (!condition) {
			throw new AssertionError("Expected: " + what);
		}
	}
}
