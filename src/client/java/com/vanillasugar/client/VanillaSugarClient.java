package com.vanillasugar.client;

import com.mojang.blaze3d.platform.InputConstants;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.ClientTooltipComponentCallback;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.client.gui.screens.inventory.ShulkerBoxScreen;
import net.minecraft.server.packs.PackType;
import net.minecraft.world.inventory.tooltip.BundleTooltip;

import com.vanillasugar.VanillaSugar;
import com.vanillasugar.client.map.MarkerDisplayButton;
import com.vanillasugar.client.map.MarkersInWorld;
import com.vanillasugar.client.map.MinimapRotationButton;
import com.vanillasugar.client.map.Minimap;
import com.vanillasugar.client.map.WorldMap;
import com.vanillasugar.client.map.WorldMapScreen;
import com.vanillasugar.client.quest.AchievementBanner;
import com.vanillasugar.client.quest.QuestBook;
import com.vanillasugar.client.quest.QuestButtons;
import com.vanillasugar.client.quest.QuestJournalScreen;
import com.vanillasugar.client.quest.QuestLog;
import com.vanillasugar.client.quest.QuestOptInScreen;
import com.vanillasugar.client.quest.QuestTracker;
import com.vanillasugar.client.storage.SlotGridTooltip;
import com.vanillasugar.client.storage.StorageButtons;
import com.vanillasugar.clock.WeatherForecast;
import com.vanillasugar.enchanting.EnchantmentSeedPayload;
import com.vanillasugar.gear.GearSlot;
import com.vanillasugar.gear.PlayerGear;
import com.vanillasugar.hopper.HopperFilter;
import com.vanillasugar.storage.ContainerPreview;

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
	public static final KeyMapping OPEN_JOURNAL = KeyMappingHelper.registerKeyMapping(
			new KeyMapping("key.vanillasugar.journal", InputConstants.KEY_J, CATEGORY));
	public static final KeyMapping QUEST_TRACKER = KeyMappingHelper.registerKeyMapping(
			new KeyMapping("key.vanillasugar.quest_tracker", InputConstants.KEY_K, CATEGORY));

	@Override
	public void onInitializeClient() {
		ClientSettings.load();
		GearWingsLayer.init();
		MenuScreens.register(HopperFilter.MENU, HopperFilterScreen::new);
		HudElementRegistry.attachElementBefore(VanillaHudElements.MOB_EFFECTS, VanillaSugar.id("minimap"), Minimap::extractRenderState);
		HudElementRegistry.attachElementBefore(VanillaHudElements.MOB_EFFECTS, VanillaSugar.id("clock"), ClockHud::extractRenderState);
		HudElementRegistry.attachElementBefore(VanillaHudElements.CROSSHAIR, VanillaSugar.id("markers"), MarkersInWorld::extractRenderState);
		HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT, VanillaSugar.id("quest_buttons"), QuestButtons::extractRenderState);
		HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT, VanillaSugar.id("quest_tracker"), QuestTracker::extractRenderState);
		ResourceLoader.get(PackType.CLIENT_RESOURCES).registerReloadListener(QuestBook.ID, new QuestBook());
		ClientPlayNetworking.registerGlobalReceiver(WeatherForecast.TYPE, (forecast, context) -> ClockHud.receive(context.client(), forecast));
		ClientPlayNetworking.registerGlobalReceiver(EnchantmentSeedPayload.TYPE, (seed, context) -> EnchantmentPreview.receive(seed));
		// Status effects are listed in the top right corner too: move them below the minimap and the quest followed
		HudElementRegistry.replaceElement(VanillaHudElements.MOB_EFFECTS, effects -> (graphics, deltaTracker) -> {
			int offset = QuestTracker.bottom(Minecraft.getInstance());
			boolean moved = offset > 0;
			if (moved) {
				graphics.pose().pushMatrix();
				graphics.pose().translate(0, offset);
			}
			effects.extractRenderState(graphics, deltaTracker);
			if (moved) {
				graphics.pose().popMatrix();
			}
		});

		// Sort and deposit buttons in chests, barrels, ender chests and shulker boxes
		ScreenEvents.AFTER_INIT.register((minecraft, screen, width, height) -> {
			if (screen instanceof ContainerScreen || screen instanceof ShulkerBoxScreen) {
				StorageButtons.addToContainerScreen((AbstractContainerScreen<?>) screen, Screens.getWidgets(screen)::add);
			}
		});
		// Bundles, shulker boxes and backpacks show their contents in the same grid
		ClientTooltipComponentCallback.EVENT.register(data -> switch (data) {
			case ContainerPreview preview -> SlotGridTooltip.of(preview);
			case BundleTooltip bundle -> SlotGridTooltip.of(bundle);
			default -> null;
		});

		// The chat frees the cursor too: the buttons next to the minimap can be clicked there
		ScreenEvents.AFTER_INIT.register((minecraft, screen, width, height) -> {
			if (screen instanceof ChatScreen && Minimap.isShown(minecraft)) {
				Screens.getWidgets(screen).add(new MarkerDisplayButton(width));
				Screens.getWidgets(screen).add(new MinimapRotationButton(width));
			}
		});

		// The buttons in the bottom right corner can be clicked wherever the cursor is free, the quest followed seen
		ScreenEvents.AFTER_INIT.register((minecraft, screen, width, height) -> {
			if (QuestButtons.freesCursor(screen)) {
				QuestButtons.addTo(screen);
				// The quest followed and the buttons stay in front, over the recipe book and the backpack too
				ScreenEvents.afterExtract(screen).register((current, graphics, mouseX, mouseY, tickProgress) -> {
					graphics.nextStratum();
					QuestTracker.draw(graphics);
					QuestButtons.drawOver(graphics, mouseX, mouseY);
				});
			}
		});

		ClientPlayConnectionEvents.JOIN.register((listener, sender, minecraft) -> {
			WorldMap.open(minecraft);
			QuestLog.open(minecraft);
		});
		ClientPlayConnectionEvents.DISCONNECT.register((listener, minecraft) -> {
			WorldMap.close();
			QuestLog.close();
			AchievementBanner.clear();
			QuestTracker.reset();
		});

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
			while (OPEN_JOURNAL.consumeClick()) {
				minecraft.gui.setScreen(new QuestJournalScreen());
			}
			while (QUEST_TRACKER.consumeClick()) {
				QuestTracker.cycle(minecraft);
			}
			// The quest journal is offered once per world, once the player is in it
			QuestLog.tick(minecraft);
			if (QuestLog.needsChoice() && minecraft.player != null && minecraft.gui.screen() == null) {
				minecraft.gui.setScreen(new QuestOptInScreen());
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
