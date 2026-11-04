package com.cassonade.test;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;

/** Poison lasts half as long, as its potion's tooltip says, and hurts every 2.5 seconds. */
public class PoisonClientGameTest implements FabricClientGameTest {
	@Override
	public void runTest(ClientGameTestContext context) {
		if (TestSelection.skips(this)) {
			return;
		}
		try (TestSingleplayerContext world = context.worldBuilder().create()) {
			world.getServer().runCommand("gamemode survival @a");
			// Full hunger would heal faster than the poison hurts
			world.getServer().runCommand("gamerule natural_health_regeneration false");
			world.getServer().runOnServer(server -> {
				ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
				player.addEffect(new MobEffectInstance(MobEffects.POISON, 900));
				int duration = player.getEffect(MobEffects.POISON).getDuration();
				check(duration == 450, "a 45-second poison lasts 22.5 seconds, not " + duration + " ticks");
			});
			float before = health(world);
			// A hit every 50 ticks: 2 in 100 ticks, 3 if one falls on each end, where vanilla would give 4 or 5
			context.waitTicks(100);
			float lost = before - health(world);
			check(lost >= 2 && lost <= 3, "2 or 3 health lost in 5 seconds, not " + lost);
			String tooltip = context.computeOnClient(client -> String.join("|", PotionContents.createItemStack(Items.POTION, Potions.POISON)
					.getTooltipLines(Item.TooltipContext.of(client.level), client.player, TooltipFlag.NORMAL).stream().map(Component::getString).toList()));
			check(tooltip.contains("00:22"), "the potion's tooltip gives 00:22: " + tooltip);
		}
	}

	private static float health(TestSingleplayerContext world) {
		return world.getServer().computeOnServer(server -> server.getPlayerList().getPlayers().getFirst().getHealth());
	}

	private static void check(boolean condition, String what) {
		if (!condition) {
			throw new AssertionError("Expected: " + what);
		}
	}
}
