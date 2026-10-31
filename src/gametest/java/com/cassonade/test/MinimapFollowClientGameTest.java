package com.cassonade.test;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import com.cassonade.client.map.Minimap;
import com.cassonade.client.quest.QuestLog;
import com.cassonade.client.quest.QuestOptInScreen;
import com.cassonade.gear.GearSlot;
import com.cassonade.gear.PlayerGear;

/**
 * The minimap moves its picture to follow the player and reads little of the terrain: what it shows must stay
 * what a full reading gives, in every direction, and take in a block changed within two seconds. Not to the block:
 * the world goes on, and leaves or water change while it is compared.
 */
public class MinimapFollowClientGameTest implements FabricClientGameTest {
	/** A little over REFRESH_TICKS. */
	private static final int REFRESH = 45;
	private static final float TOLERATED = 0.01F;

	@Override
	public void runTest(ClientGameTestContext context) {
		if (TestSelection.skips(this)) {
			return;
		}
		context.getInput().resizeWindow(1920, 1080);
		try (TestSingleplayerContext world = context.worldBuilder().setUseConsistentSettings(false).adjustSettings(state -> {
			state.setSeed("vanillasugar");
			state.setWorldType(state.getNormalPresetList().getFirst());
			state.setAllowCommands(true);
		}).create()) {
			context.waitForScreen(QuestOptInScreen.class);
			context.runOnClient(client -> QuestLog.setEnabled(false));
			context.setScreen(() -> null);
			world.getServer().runCommand("gamemode spectator @a");
			world.getServer().runCommand("gamemode survival @a");
			world.getServer().runOnServer(server -> {
				ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
				player.getAbilities().mayfly = true;
				player.getAbilities().flying = true;
				player.onUpdateAbilities();
				player.setAttached(PlayerGear.ATTACHMENT, PlayerGear.of(player).with(GearSlot.COMPASS, new ItemStack(Items.COMPASS)));
			});
			world.getServer().runCommand("execute as @a at @s positioned over motion_blocking run tp @s ~ ~10 ~");
			context.waitTicks(60);

			// One way then the other along each axis, then diagonally, at different steps; looked at right away, as
			// the rows read again would otherwise correct a picture moved wrong
			String[] steps = {"~3 ~ ~", "~-5 ~ ~", "~ ~ ~4", "~ ~ ~-7", "~6 ~ ~6", "~-2 ~ ~9", "~-9 ~ ~-2"};
			for (String step : steps) {
				for (int i = 0; i < 3; i++) {
					world.getServer().runCommand("execute as @a at @s run tp @s " + step);
					context.waitTicks(4);
				}
				float difference = context.computeOnClient(Minimap::differenceFromTerrain);
				check(difference <= TOLERATED, "after moving " + step + ", the minimap shows the terrain, " + difference + " differs");
			}

			// A change under the player's feet, without moving: not at once, but soon
			world.getServer().runCommand("execute as @a at @s positioned over world_surface run fill ~-6 ~ ~-6 ~6 ~ ~6 gold_block");
			context.waitTicks(2);
			check(context.computeOnClient(Minimap::differenceFromTerrain) > TOLERATED, "the blocks changed are not all shown at once");
			context.waitTicks(REFRESH);
			check(context.computeOnClient(Minimap::differenceFromTerrain) <= TOLERATED, "blocks changed show on the minimap within two seconds");
		}
	}

	private static void check(boolean condition, String what) {
		if (!condition) {
			throw new AssertionError(what);
		}
	}
}
