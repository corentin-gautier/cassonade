package com.vanillasugar.test;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

import net.minecraft.client.sounds.SoundEventListener;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import com.vanillasugar.client.quest.QuestLog;
import com.vanillasugar.client.quest.QuestOptInScreen;
import com.vanillasugar.gear.GearSlot;
import com.vanillasugar.gear.PlayerGear;
import com.vanillasugar.item.VanillaSugarItems;

/**
 * The clock's panel at night, a barometer worn: weather and forecast, the time over the day, the moon's
 * phase and the bed, a chime as the bed shows up.
 */
public class ClockClientGameTest implements FabricClientGameTest {
	@Override
	public void runTest(ClientGameTestContext context) {
		if (TestSelection.skips(this)) {
			return;
		}
		context.getInput().resizeWindow(1920, 1080);
		try (TestSingleplayerContext world = context.worldBuilder().create()) {
			world.getConnection().waitForChunksRender();
			context.waitForScreen(QuestOptInScreen.class);
			context.runOnClient(client -> QuestLog.setEnabled(false));
			context.setScreen(() -> null);
			world.getServer().runCommand("gamemode survival @a");
			world.getServer().runCommand("gamerule advance_weather true");
			world.getServer().runCommand("time set 6000");
			world.getServer().runCommand("weather rain 3000");
			world.getServer().runOnServer(server -> {
				ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
				player.setAttached(PlayerGear.ATTACHMENT, PlayerGear.of(player).with(GearSlot.CLOCK, new ItemStack(VanillaSugarItems.BAROMETER)));
			});
			context.waitTicks(30);
			context.takeScreenshot("vanillasugar-clock-day");

			// Night falls: the bed shows up with a chime
			List<String> sounds = new CopyOnWriteArrayList<>();
			SoundEventListener listener = (sound, event, range) -> sounds.add(sound.getIdentifier().getPath());
			context.runOnClient(client -> client.getSoundManager().addListener(listener));
			world.getServer().runCommand("time set 16000");
			context.waitTicks(30);
			context.runOnClient(client -> client.getSoundManager().removeListener(listener));
			check(sounds.contains("block.note_block.chime"), "the clock chimes when one can go to bed (" + sounds + ")");
			context.takeScreenshot("vanillasugar-clock-night");
		}
	}

	private static void check(boolean condition, String what) {
		if (!condition) {
			throw new AssertionError("Expected: " + what);
		}
	}
}
