package com.cassonade.test;

import static com.cassonade.test.PlannedWorld.check;

import java.util.List;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;

/** With Shift held, a piece of gear tells how it compares with the one worn or held, and not with itself. */
public class GearComparisonClientGameTest implements FabricClientGameTest {
	@Override
	public void runTest(ClientGameTestContext context) {
		if (TestSelection.skips(this)) {
			return;
		}
		try (TestSingleplayerContext world = PlannedWorld.open(context)) {
			world.getServer().runCommand("item replace entity @a armor.chest with minecraft:iron_chestplate");
			world.getServer().runCommand("item replace entity @a weapon.mainhand with minecraft:stone_sword");
			context.waitTicks(5);
			String armor = Component.translatable("attribute.name.armor").getString();
			String damage = Component.translatable("attribute.name.attack_damage").getString();

			check(context.computeOnClient(client -> tooltip(client, new ItemStack(Items.DIAMOND_CHESTPLATE))).stream()
					.noneMatch(line -> line.equals("+2 " + armor)), "no comparison without Shift");
			context.getInput().holdShift();
			List<String> diamond = context.computeOnClient(client -> tooltip(client, new ItemStack(Items.DIAMOND_CHESTPLATE)));
			check(diamond.stream().anyMatch(line -> line.equals("+2 " + armor)), "a diamond chestplate has 2 armor more than iron: " + diamond);
			List<String> leather = context.computeOnClient(client -> tooltip(client, new ItemStack(Items.LEATHER_CHESTPLATE)));
			check(leather.stream().anyMatch(line -> line.equals("-3 " + armor)), "a leather chestplate has 3 armor less than iron: " + leather);
			List<String> sword = context.computeOnClient(client -> tooltip(client, new ItemStack(Items.IRON_SWORD)));
			check(sword.stream().anyMatch(line -> line.equals("+1 " + damage)), "an iron sword deals 1 more than a stone one: " + sword);
			List<String> worn = context.computeOnClient(client -> tooltip(client, client.player.getMainHandItem()));
			check(worn.stream().noneMatch(line -> line.startsWith("+") || line.startsWith("-")), "the sword held is not compared with itself: " + worn);
			List<String> elytra = context.computeOnClient(client -> tooltip(client, new ItemStack(Items.ELYTRA)));
			check(elytra.stream().noneMatch(line -> line.contains(armor)), "elytra are not compared with the chestplate: " + elytra);
			context.getInput().releaseShift();
		}
	}

	private static List<String> tooltip(Minecraft client, ItemStack stack) {
		return stack.getTooltipLines(Item.TooltipContext.of(client.level), client.player, TooltipFlag.NORMAL).stream().map(Component::getString).toList();
	}
}
