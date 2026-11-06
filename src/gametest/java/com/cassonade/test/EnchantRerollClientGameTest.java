package com.cassonade.test;

import static com.cassonade.test.PlannedWorld.check;
import static com.cassonade.test.PlannedWorld.player;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.EnchantmentScreen;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import com.cassonade.enchanting.EnchantmentReroll;

/** The enchanting table's reroll: new offers for 1 level, then 2, back to 1 once an item is enchanted, nothing without the levels. */
public class EnchantRerollClientGameTest implements FabricClientGameTest {
	@Override
	public void runTest(ClientGameTestContext context) {
		if (TestSelection.skips(this)) {
			return;
		}
		try (TestSingleplayerContext world = context.worldBuilder().create()) {
			world.getServer().runCommand("gamerule spawn_mobs false");
			world.getServer().runCommand("time set noon");
			world.getServer().runCommand("fill -4 99 -4 4 99 4 stone");
			world.getServer().runCommand("fill -4 100 -4 4 104 4 air");
			world.getServer().runCommand("setblock 0 100 2 enchanting_table");
			world.getServer().runCommand("tp @a 0.5 100 0.5 0 30");
			world.getServer().runCommand("experience set @a 20 levels");
			context.waitTicks(10);
			context.getInput().pressKey(options -> options.keyUse);
			context.waitForScreen(EnchantmentScreen.class);
			world.getServer().runOnServer(server -> {
				player(server).containerMenu.getSlot(0).set(new ItemStack(Items.IRON_SWORD));
				player(server).containerMenu.getSlot(1).set(new ItemStack(Items.LAPIS_LAZULI, 3));
			});
			context.waitTicks(5);

			int seed = seed(world);
			reroll(context);
			check(seed(world) != seed, "new offers after a reroll");
			check(levels(world) == 19 && rerolls(world) == 1, "the first reroll for 1 level: " + levels(world) + " levels left");
			reroll(context);
			check(levels(world) == 17 && rerolls(world) == 2, "the second for 2: " + levels(world) + " levels left");

			// Its price in the tooltip, the button under the slots
			double[] at = context.computeOnClient(client -> {
				Button button = rerollButton(client);
				double scale = client.getWindow().getGuiScale();
				return new double[] {(button.getX() + button.getWidth() / 2.0) * scale, (button.getY() + button.getHeight() / 2.0) * scale};
			});
			context.getInput().setCursorPos(at[0], at[1]);
			context.waitTicks(10);
			ScreenInput.quiet(context);
			Screenshots.take(context, "cassonade-enchanting-reroll");

			// Enchanting for real brings it back to 1
			context.runOnClient(client -> client.gameMode.handleInventoryButtonClick(client.player.containerMenu.containerId, 0));
			context.waitTicks(5);
			check(world.getServer().computeOnServer(server -> player(server).containerMenu.getSlot(0).getItem().isEnchanted()), "the sword enchanted");
			check(rerolls(world) == 0, "rerolls counted again from 1 after enchanting");

			// Without the levels, nothing
			world.getServer().runCommand("experience set @a 0 levels");
			context.waitTicks(2);
			world.getServer().runOnServer(server -> player(server).containerMenu.getSlot(0).set(new ItemStack(Items.IRON_SWORD)));
			context.waitTicks(5);
			context.runOnClient(client -> check(!rerollButton(client).active, "the button off without levels"));
			int before = seed(world);
			context.runOnClient(client -> client.gameMode.handleInventoryButtonClick(client.player.containerMenu.containerId, EnchantmentReroll.BUTTON));
			context.waitTicks(5);
			check(seed(world) == before, "no reroll without levels");
		}
	}

	private static Button rerollButton(Minecraft client) {
		return client.gui.screen().children().stream().filter(Button.class::isInstance).map(Button.class::cast)
				.filter(button -> button.getMessage().getContents() instanceof TranslatableContents contents
						&& contents.getKey().equals("gui.cassonade.enchanting.reroll"))
				.findFirst().orElseThrow();
	}

	private static void reroll(ClientGameTestContext context) {
		context.runOnClient(client -> rerollButton(client).onPress(null));
		context.waitTicks(5);
	}

	private static int seed(TestSingleplayerContext world) {
		return world.getServer().computeOnServer(server -> player(server).getEnchantmentSeed());
	}

	private static int levels(TestSingleplayerContext world) {
		return world.getServer().computeOnServer(server -> player(server).experienceLevel);
	}

	private static int rerolls(TestSingleplayerContext world) {
		return world.getServer().computeOnServer(server -> player(server).getAttachedOrElse(EnchantmentReroll.REROLLS, 0));
	}
}
