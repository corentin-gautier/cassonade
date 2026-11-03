package com.cassonade.test;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.item.Items;

import com.cassonade.client.BossBars;
import com.cassonade.client.combat.CombatHud;
import com.cassonade.gear.GearSlot;
import com.cassonade.gear.PlayerGear;
import com.cassonade.item.CassonadeItems;

/**
 * A wither's bar filling up as it shows up, then its trail and the damage dealt after two blows, with a
 * notched bar from a command under it that tells no damage, at the usual size and at the smallest GUI.
 */
public class BossBarClientGameTest implements FabricClientGameTest {
	@Override
	public void runTest(ClientGameTestContext context) {
		if (TestSelection.skips(this)) {
			return;
		}
		context.getInput().resizeWindow(1920, 1080);
		try (TestSingleplayerContext world = context.worldBuilder().create()) {
			world.getConnection().waitForChunksRender();
			world.getServer().runCommand("difficulty normal");
			world.getServer().runCommand("gamemode creative @a");
			world.getServer().runCommand("time set 6000");
			world.getServer().runCommand("gamerule mob_griefing false");
			check(!context.computeOnClient(BossBars::isShown), "no boss bar before the boss");
			// The sculk compass shows the health bars of hurt mobs, but not over a boss
			world.getServer().runOnServer(server -> {
				ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
				player.setAttached(PlayerGear.ATTACHMENT, PlayerGear.of(player).with(GearSlot.COMPASS, new ItemStack(CassonadeItems.SCULK_COMPASS)));
			});
			world.getServer().runCommand("execute as @a at @s run summon wither ^ ^ ^10 {Invul:0}");
			context.waitTicks(10);
			check(context.computeOnClient(BossBars::isShown), "the wither's bar shown");
			Screenshots.take(context, "cassonade-boss-bar-entrance");
			context.waitTicks(40);
			Screenshots.take(context, "cassonade-boss-bar-full");

			world.getServer().runCommand("damage @e[type=wither,limit=1] 40 minecraft:generic");
			context.waitTicks(5);
			world.getServer().runCommand("damage @e[type=wither,limit=1] 25 minecraft:generic");
			context.waitTicks(5);
			check(context.computeOnClient(client -> client.level.getEntitiesOfClass(WitherBoss.class, client.player.getBoundingBox().inflate(32))
					.stream().noneMatch(wither -> CombatHud.isShown(wither.getId()))), "no mob health bar over the wither, hurt with a sculk compass worn");
			world.getServer().runCommand("bossbar add cassonade:test \"Raid\"");
			world.getServer().runCommand("bossbar set cassonade:test players @a");
			world.getServer().runCommand("bossbar set cassonade:test color yellow");
			world.getServer().runCommand("bossbar set cassonade:test style notched_10");
			world.getServer().runCommand("bossbar set cassonade:test value 60");
			context.waitTicks(3);
			Screenshots.take(context, "cassonade-boss-bar-hit");
			context.getInput().resizeWindow(1280, 720);
			context.waitTicks(2);
			Screenshots.take(context, "cassonade-boss-bar-small");
			// Narrower between the clock's panel and the minimap
			world.getServer().runOnServer(server -> {
				ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
				player.setAttached(PlayerGear.ATTACHMENT, PlayerGear.of(player).with(GearSlot.COMPASS, new ItemStack(Items.COMPASS))
						.with(GearSlot.CLOCK, new ItemStack(Items.CLOCK)));
			});
			context.waitTicks(5);
			Screenshots.take(context, "cassonade-boss-bar-small-hud");
			context.getInput().resizeWindow(1920, 1080);
			context.waitTicks(2);
			world.getServer().runCommand("bossbar remove cassonade:test");
			world.getServer().runCommand("kill @e[type=wither]");
			// The bar stays through the wither's dying
			context.waitTicks(40);
			check(!context.computeOnClient(BossBars::isShown), "no boss bar once the boss is gone");
		}
	}

	private static void check(boolean condition, String what) {
		if (!condition) {
			throw new AssertionError("Expected: " + what);
		}
	}
}
