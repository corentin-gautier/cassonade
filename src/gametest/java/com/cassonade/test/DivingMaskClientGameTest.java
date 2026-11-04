package com.cassonade.test;

import java.util.List;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

import net.minecraft.client.CameraType;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;

import com.cassonade.Cassonade;
import com.cassonade.client.ambience.Birds;
import com.cassonade.item.CassonadeItems;

/**
 * The diving mask: the view under water sharp and longer, pillars 4 to 48 blocks away; breath kept with the head just
 * under the surface, lost without the mask or deeper; the helmet's enchantments; and the mask worn, from the front.
 */
public class DivingMaskClientGameTest implements FabricClientGameTest {
	private static final int SURFACE = 120;
	/** The shallow pool's floor: two blocks of water over it, the eyes of a player standing on it just under the surface. */
	private static final int SHALLOW = SURFACE + 10;
	private static final Identifier BLUR = Cassonade.id("underwater");

	@Override
	public void runTest(ClientGameTestContext context) {
		if (TestSelection.skips(this)) {
			return;
		}
		context.getInput().resizeWindow(1920, 1080);
		try (TestSingleplayerContext world = context.worldBuilder().create()) {
			context.runOnClient(client -> Birds.setSpawning(false));
			world.getServer().runCommand("gamerule advance_time false");
			world.getServer().runCommand("gamerule advance_weather false");
			world.getServer().runCommand("weather clear");
			world.getServer().runCommand("time set 6000");
			world.getServer().runCommand("forceload add -8 -8 60 8");

			// The helmet's enchantments, from the table and the anvil
			List<String> missing = world.getServer().computeOnServer(server -> {
				ItemStack mask = new ItemStack(CassonadeItems.DIVING_MASK);
				return List.of(Enchantments.RESPIRATION, Enchantments.AQUA_AFFINITY, Enchantments.PROTECTION, Enchantments.UNBREAKING, Enchantments.MENDING)
						.stream().filter(key -> !enchants(server.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(key).value(), mask, key))
						.map(key -> key.identifier().toString()).toList();
			});
			check(missing.isEmpty(), "the mask taking the helmet's enchantments, missing " + missing);

			// Sight: as the UnderwaterSight test, farther pillars, through the mask
			world.getServer().runCommand("gamemode spectator @a");
			world.getServer().runCommand("fill -4 " + (SURFACE - 12) + " -8 60 " + SURFACE + " 8 glass hollow");
			world.getServer().runCommand("fill -3 " + (SURFACE - 11) + " -7 59 " + SURFACE + " 7 water");
			for (int distance : new int[] { 4, 8, 16, 32, 48 }) {
				world.getServer().runCommand("fill " + distance + " " + (SURFACE - 11) + " -1 " + distance + " " + (SURFACE - 2) + " -1 yellow_wool");
				world.getServer().runCommand("fill " + distance + " " + (SURFACE - 11) + " 1 " + distance + " " + (SURFACE - 2) + " 1 red_wool");
			}
			world.getServer().runCommand("item replace entity @a armor.head with cassonade:diving_mask");
			world.getServer().runCommand("tp @a 0.5 " + (SURFACE - 6) + " 0.5 -90 0");
			world.getConnection().waitForChunksRender();
			context.waitTicks(80);
			check(context.computeOnClient(client -> !client.gameRenderer.getRequestedPostEffects().contains(BLUR)), "the view sharp through the mask");
			context.runOnClient(client -> client.gui.hud.getChat().clearMessages(false));
			Screenshots.take(context, "cassonade-diving-mask");
			world.getServer().runCommand("item replace entity @a armor.head with air");
			context.waitTicks(5);
			check(context.computeOnClient(client -> client.gameRenderer.getRequestedPostEffects().contains(BLUR)), "the view blurred again without the mask");

			// Breath: standing in two blocks of water, the snorkel reaches the air
			world.getServer().runCommand("fill 20 " + SHALLOW + " -2 24 " + (SHALLOW + 3) + " 2 glass hollow");
			world.getServer().runCommand("fill 21 " + (SHALLOW + 1) + " -1 23 " + (SHALLOW + 3) + " 1 air");
			world.getServer().runCommand("fill 21 " + (SHALLOW + 1) + " -1 23 " + (SHALLOW + 2) + " 1 water");
			world.getServer().runCommand("gamemode survival @a");
			world.getServer().runCommand("effect give @a resistance infinite 4 true");
			world.getServer().runCommand("item replace entity @a armor.head with cassonade:diving_mask");
			world.getServer().runCommand("tp @a 22.5 " + (SHALLOW + 1) + " 0.5 -90 30");
			context.waitTicks(60);
			check(eyeInWater(world), "the eyes under the surface in the shallow pool");
			int withMask = air(world);
			check(withMask == 300, "full breath through the snorkel, found " + withMask);

			world.getServer().runCommand("item replace entity @a armor.head with air");
			context.waitTicks(60);
			int without = air(world);
			check(without < 300, "breath running out without the mask, found " + without);

			// Deeper, the snorkel is under water too
			world.getServer().runOnServer(server -> server.getPlayerList().getPlayers().getFirst().setAirSupply(300));
			world.getServer().runCommand("item replace entity @a armor.head with cassonade:diving_mask");
			world.getServer().runCommand("tp @a 10.5 " + (SURFACE - 11) + " 4.5");
			context.waitTicks(60);
			int deep = air(world);
			check(deep < 300, "breath running out deeper down even with the mask, found " + deep);

			// The mask worn, from the front, on land
			world.getServer().runCommand("gamemode creative @a");
			// In the open air, away from the pools' glass
			world.getServer().runCommand("tp @a 40.5 " + (SHALLOW + 4) + " 0.5 90 10");
			context.runOnClient(client -> client.options.setCameraType(CameraType.THIRD_PERSON_FRONT));
			context.waitTicks(20);
			context.runOnClient(client -> client.gui.hud.getChat().clearMessages(false));
			Screenshots.take(context, "cassonade-diving-mask-worn");
			context.runOnClient(client -> client.options.setCameraType(CameraType.FIRST_PERSON));
		}
	}

	/** Any of them at the anvil; the ones only for helmets from the enchanting table too. */
	private static boolean enchants(Enchantment enchantment, ItemStack stack, ResourceKey<Enchantment> key) {
		boolean helmetOnly = key == Enchantments.RESPIRATION || key == Enchantments.AQUA_AFFINITY;
		return enchantment.canEnchant(stack) && (!helmetOnly || enchantment.isPrimaryItem(stack));
	}

	private static int air(TestSingleplayerContext world) {
		return world.getServer().computeOnServer(server -> server.getPlayerList().getPlayers().getFirst().getAirSupply());
	}

	private static boolean eyeInWater(TestSingleplayerContext world) {
		return world.getServer().computeOnServer(server -> {
			ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
			return player.isUnderWater();
		});
	}

	private static void check(boolean condition, String what) {
		if (!condition) {
			throw new AssertionError(what);
		}
	}
}
