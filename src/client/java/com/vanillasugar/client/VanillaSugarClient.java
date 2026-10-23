package com.vanillasugar.client;

import com.mojang.blaze3d.platform.InputConstants;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ChatScreen;

import com.vanillasugar.VanillaSugar;
import com.vanillasugar.client.map.MarkerDisplayButton;
import com.vanillasugar.client.map.MarkersInWorld;
import com.vanillasugar.client.map.Minimap;
import com.vanillasugar.client.map.WorldMap;
import com.vanillasugar.client.map.WorldMapScreen;
import com.vanillasugar.clock.WeatherForecast;
import com.vanillasugar.gear.GearSlot;
import com.vanillasugar.gear.PlayerGear;

public class VanillaSugarClient implements ClientModInitializer {
	public static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(VanillaSugar.id("main"));
	public static final KeyMapping OPEN_WORLD_MAP = KeyMappingHelper.registerKeyMapping(
			new KeyMapping("key.vanillasugar.world_map", InputConstants.KEY_M, CATEGORY));
	public static final KeyMapping OPEN_BACKPACK = KeyMappingHelper.registerKeyMapping(
			new KeyMapping("key.vanillasugar.backpack", InputConstants.KEY_B, CATEGORY));
	public static final KeyMapping MARKER_DISPLAY = KeyMappingHelper.registerKeyMapping(
			new KeyMapping("key.vanillasugar.marker_display", InputConstants.KEY_N, CATEGORY));
	public static final KeyMapping ZOOM_IN = KeyMappingHelper.registerKeyMapping(
			new KeyMapping("key.vanillasugar.zoom_in", InputConstants.KEY_EQUALS, CATEGORY));
	public static final KeyMapping ZOOM_OUT = KeyMappingHelper.registerKeyMapping(
			new KeyMapping("key.vanillasugar.zoom_out", InputConstants.KEY_MINUS, CATEGORY));

	@Override
	public void onInitializeClient() {
		ClientSettings.load();
		HudElementRegistry.attachElementBefore(VanillaHudElements.MOB_EFFECTS, VanillaSugar.id("minimap"), Minimap::extractRenderState);
		HudElementRegistry.attachElementBefore(VanillaHudElements.MOB_EFFECTS, VanillaSugar.id("clock"), ClockHud::extractRenderState);
		HudElementRegistry.attachElementBefore(VanillaHudElements.CROSSHAIR, VanillaSugar.id("markers"), MarkersInWorld::extractRenderState);
		ClientPlayNetworking.registerGlobalReceiver(WeatherForecast.TYPE, (forecast, context) -> ClockHud.receive(context.client(), forecast));
		// Status effects are listed in the top right corner too: move them below the minimap
		HudElementRegistry.replaceElement(VanillaHudElements.MOB_EFFECTS, effects -> (graphics, deltaTracker) -> {
			boolean moved = Minimap.isShown(Minecraft.getInstance());
			if (moved) {
				graphics.pose().pushMatrix();
				graphics.pose().translate(0, Minimap.height(Minecraft.getInstance()));
			}
			effects.extractRenderState(graphics, deltaTracker);
			if (moved) {
				graphics.pose().popMatrix();
			}
		});

		// The chat frees the cursor too: the marker display button can be clicked there
		ScreenEvents.AFTER_INIT.register((minecraft, screen, width, height) -> {
			if (screen instanceof ChatScreen && Minimap.isShown(minecraft)) {
				Screens.getWidgets(screen).add(new MarkerDisplayButton(width));
			}
		});

		ClientPlayConnectionEvents.JOIN.register((listener, sender, minecraft) -> WorldMap.open(minecraft));
		ClientPlayConnectionEvents.DISCONNECT.register((listener, minecraft) -> WorldMap.close());

		ClientTickEvents.END_CLIENT_TICK.register(minecraft -> {
			LabeledKeys.tick(minecraft);
			while (ZOOM_IN.consumeClick()) {
				Minimap.zoomIn();
			}
			while (ZOOM_OUT.consumeClick()) {
				Minimap.zoomOut();
			}
			while (OPEN_BACKPACK.consumeClick()) {
				if (PlayerGear.isWearing(minecraft.player, GearSlot.BACKPACK) && !minecraft.player.hasInfiniteMaterials()) {
					BackpackPanel.openInventory(minecraft);
				}
			}
			while (MARKER_DISPLAY.consumeClick()) {
				MarkerDisplayButton.cycle(minecraft);
			}
			while (OPEN_WORLD_MAP.consumeClick()) {
				if (PlayerGear.isWearing(minecraft.player, GearSlot.MAP)) {
					minecraft.gui.setScreen(new WorldMapScreen());
				}
			}
			Minimap.tick(minecraft);
			ClockHud.tick(minecraft);
			WorldMap.tick(minecraft);
		});
	}
}
