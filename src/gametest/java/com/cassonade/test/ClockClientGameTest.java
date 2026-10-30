package com.cassonade.test;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.lang.reflect.Field;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

import net.minecraft.client.sounds.SoundEventListener;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.world.inventory.InventoryMenu;

import com.cassonade.client.ClockHud;
import com.cassonade.client.quest.QuestLog;
import com.cassonade.client.quest.QuestOptInScreen;
import com.cassonade.gear.GearSlot;
import com.cassonade.gear.PlayerGear;
import com.cassonade.item.CassonadeItems;
import com.cassonade.clock.WeatherForecast;

/**
 * A clock in the inventory shows nothing; worn, the time and weather, and a barometer the forecast too. The clock's panel at night, a barometer worn: weather and forecast, the time over the day, the moon's
 * phase blinking with the bed as night falls, with a chime, the panel keeping its width.
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
			// A clock in the inventory does nothing; worn in its gear slot, it shows the time, weather and day
			// in the top left corner, and the barometer made from it the next weather too
			world.getServer().runCommand("item replace entity @a hotbar.8 with clock");
			context.waitTicks(2);
			check(!context.computeOnClient(client -> ClockHud.isShown()), "a clock in the inventory shows no panel");
			context.setScreen(() -> new InventoryScreen(Minecraft.getInstance().player));
			context.waitForScreen(InventoryScreen.class);
			ScreenInput.quickMove(context, InventoryMenu.USE_ROW_SLOT_START + 8);
			world.getConnection().waitForServerboundPackets();
			world.getConnection().waitForClientboundPackets();
			context.waitTicks(2);
			Screenshots.take(context, "cassonade-4-clock-slot");
			world.getServer().runOnServer(server -> check(PlayerGear.isWearing(server.getPlayerList().getPlayers().getFirst(), GearSlot.CLOCK),
					"shift click equips the clock"));
			context.setScreen(() -> null);
			world.getServer().runCommand("gamerule advance_weather true");
			world.getServer().runCommand("time set 6000");
			world.getServer().runCommand("weather rain 3000");
			context.waitTicks(40);
			Screenshots.take(context, "cassonade-4-clock-rain");
			check(context.computeOnClient(client -> WeatherForecast.UNKNOWN.equals(forecast())), "a clock gets no weather forecast");
			world.getServer().runOnServer(server -> {
				ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
				player.setAttached(PlayerGear.ATTACHMENT, PlayerGear.of(player).with(GearSlot.CLOCK, new ItemStack(CassonadeItems.BAROMETER)));
			});
			context.waitTicks(30);
			check(context.computeOnClient(client -> !WeatherForecast.UNKNOWN.equals(forecast())), "a barometer gets the weather forecast");
			Screenshots.take(context, "cassonade-4-barometer-rain");
			world.getServer().runCommand("time set 16000");
			world.getServer().runCommand("weather clear 1000");
			context.waitTicks(40);
			Screenshots.take(context, "cassonade-4-barometer-night");
			world.getServer().runOnServer(server -> {
				ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
				player.setAttached(PlayerGear.ATTACHMENT, PlayerGear.of(player).with(GearSlot.CLOCK, ItemStack.EMPTY));
			});
			world.getServer().runCommand("time set 6000");

			world.getServer().runCommand("gamerule advance_weather true");
			world.getServer().runCommand("time set 6000");
			world.getServer().runCommand("weather rain 3000");
			world.getServer().runOnServer(server -> {
				ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
				player.setAttached(PlayerGear.ATTACHMENT, PlayerGear.of(player).with(GearSlot.CLOCK, new ItemStack(CassonadeItems.BAROMETER)));
			});
			context.waitTicks(30);
			Screenshots.take(context, "cassonade-clock-day");

			// Night falls: a chime, and the moon blinks with the bed in its place
			List<String> sounds = new CopyOnWriteArrayList<>();
			SoundEventListener listener = (sound, event, range) -> sounds.add(sound.getIdentifier().getPath());
			context.runOnClient(client -> client.getSoundManager().addListener(listener));
			world.getServer().runCommand("time set 16000");
			context.waitTicks(2);
			check(context.computeOnClient(client -> ClockHud.showsBed()), "the bed in the moon's place as night falls");
			Screenshots.take(context, "cassonade-clock-night-bed");
			int width = context.computeOnClient(client -> ClockHud.right());
			context.waitTicks(10);
			check(!context.computeOnClient(client -> ClockHud.showsBed()), "the moon back between two blinks");
			context.runOnClient(client -> client.getSoundManager().removeListener(listener));
			check(sounds.contains("block.note_block.chime"), "the clock chimes when one can go to bed (" + sounds + ")");
			context.waitTicks(100);
			check(!context.computeOnClient(client -> ClockHud.showsBed()) && context.computeOnClient(client -> ClockHud.right()) == width,
					"the moon alone after a few seconds, the panel as wide as with the bed");
			Screenshots.take(context, "cassonade-clock-night");
		}
	}

	/** The forecast the clock's panel last received. */
	private static WeatherForecast forecast() {
		try {
			Field field = ClockHud.class.getDeclaredField("forecast");
			field.setAccessible(true);
			return (WeatherForecast) field.get(null);
		} catch (ReflectiveOperationException e) {
			throw new AssertionError(e);
		}
	}

	private static void check(boolean condition, String what) {
		if (!condition) {
			throw new AssertionError("Expected: " + what);
		}
	}
}
