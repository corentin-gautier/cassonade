package com.cassonade.client;

import java.util.List;

import com.mojang.blaze3d.platform.InputConstants;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.ClientTooltipComponentCallback;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.gui.screens.inventory.StonecutterScreen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.client.gui.screens.inventory.ShulkerBoxScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackType;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.inventory.tooltip.BundleTooltip;

import com.cassonade.ServerSupport;
import com.cassonade.Cassonade;
import com.cassonade.client.combat.CombatHud;
import com.cassonade.client.combat.DamageDirection;
import com.cassonade.client.light.DynamicLights;
import com.cassonade.client.light.SpawnSpots;
import com.cassonade.client.map.DeathMarker;
import com.cassonade.client.map.MarkerDisplayButton;
import com.cassonade.client.map.MapTooltip;
import com.cassonade.client.map.MarkersInWorld;
import com.cassonade.client.map.MinimapRotationButton;
import com.cassonade.client.map.Pings;
import com.cassonade.client.map.Minimap;
import com.cassonade.client.map.WorldMap;
import com.cassonade.client.map.WorldMapScreen;
import com.cassonade.client.quest.AchievementBanner;
import com.cassonade.client.quest.QuestBook;
import com.cassonade.client.quest.QuestButtons;
import com.cassonade.client.quest.QuestJournalScreen;
import com.cassonade.client.quest.QuestLog;
import com.cassonade.client.quest.QuestOptInScreen;
import com.cassonade.client.quest.QuestTracker;
import com.cassonade.client.storage.SlotGridTooltip;
import com.cassonade.client.storage.ContainerSearch;
import com.cassonade.client.spyglass.GlancePanel;
import com.cassonade.client.spyglass.ScopeZoom;
import com.cassonade.client.trader.TraderTimePanel;
import com.cassonade.client.storage.StorageButtons;
import com.cassonade.clock.WeatherForecast;
import com.cassonade.furnace.FuelTimes;
import com.cassonade.furnace.FurnaceExperience;
import com.cassonade.combat.HuntersPayload;
import com.cassonade.enchanting.EnchantmentSeedPayload;
import com.cassonade.gear.BackpackTogglePayload;
import com.cassonade.gear.GearSlot;
import com.cassonade.gear.PlayerGear;
import com.cassonade.hopper.HopperFilter;
import com.cassonade.ping.PingPayload;
import com.cassonade.spyglass.GlancePayload;
import com.cassonade.trader.TraderTimePayload;
import com.cassonade.storage.ContainerPreview;
import com.cassonade.storage.FoodPreview;
import com.cassonade.storage.MapPreview;
import com.cassonade.woodcutter.Woodcutter;

public class CassonadeClient implements ClientModInitializer {
	public static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(Cassonade.id("main"));
	public static final KeyMapping OPEN_WORLD_MAP = KeyMappingHelper.registerKeyMapping(
			new KeyMapping("key.cassonade.world_map", InputConstants.KEY_M, CATEGORY));
	public static final KeyMapping OPEN_BACKPACK = KeyMappingHelper.registerKeyMapping(
			new KeyMapping("key.cassonade.backpack", InputConstants.KEY_B, CATEGORY));
	public static final KeyMapping MARKER_DISPLAY = KeyMappingHelper.registerKeyMapping(
			new KeyMapping("key.cassonade.marker_display", InputConstants.KEY_N, CATEGORY));
	public static final KeyMapping MINIMAP_ROTATION = KeyMappingHelper.registerKeyMapping(
			new KeyMapping("key.cassonade.minimap_rotation", InputConstants.KEY_R, CATEGORY));
	public static final KeyMapping ZOOM_IN = KeyMappingHelper.registerKeyMapping(
			new KeyMapping("key.cassonade.zoom_in", InputConstants.KEY_EQUALS, CATEGORY));
	public static final KeyMapping ZOOM_OUT = KeyMappingHelper.registerKeyMapping(
			new KeyMapping("key.cassonade.zoom_out", InputConstants.KEY_MINUS, CATEGORY));
	public static final KeyMapping OPEN_JOURNAL = KeyMappingHelper.registerKeyMapping(
			new KeyMapping("key.cassonade.journal", InputConstants.KEY_J, CATEGORY));
	public static final KeyMapping QUEST_TRACKER = KeyMappingHelper.registerKeyMapping(
			new KeyMapping("key.cassonade.quest_tracker", InputConstants.KEY_K, CATEGORY));
	/** Held while clicking one of the inventory's slots in a container window, locks or unlocks it (LockedSlots). */
	public static final KeyMapping LOCK_SLOT = KeyMappingHelper.registerKeyMapping(
			new KeyMapping("key.cassonade.lock_slot", InputConstants.KEY_LALT, CATEGORY));
	public static final KeyMapping ROTATE_HOTBAR = KeyMappingHelper.registerKeyMapping(
			new KeyMapping("key.cassonade.rotate_hotbar", InputConstants.KEY_G, CATEGORY));

	@Override
	public void onInitializeClient() {
		ClientSettings.load();
		for (Identifier element : List.of(VanillaHudElements.HOTBAR, VanillaHudElements.SPECTATOR_MENU, VanillaHudElements.ARMOR_BAR,
				VanillaHudElements.HEALTH_BAR, VanillaHudElements.FOOD_BAR, VanillaHudElements.AIR_BAR, VanillaHudElements.MOUNT_HEALTH,
				VanillaHudElements.INFO_BAR, VanillaHudElements.EXPERIENCE_LEVEL, VanillaHudElements.HELD_ITEM_TOOLTIP,
				VanillaHudElements.OVERLAY_MESSAGE)) {
			HudElementRegistry.replaceElement(element, HudLayout::shiftUp);
		}
		GearWingsLayer.init();
		MenuScreens.register(HopperFilter.MENU, HopperFilterScreen::new);
		MenuScreens.register(Woodcutter.MENU, StonecutterScreen::new);
		// Last and on a stratum of their own: the vanilla overlays drawn after the effects (player list,
		// scoreboard, titles, sleep) each take a new one, and would cover them. With the subtitles, which the game
		// defers past the screens: over an open inventory too. The toasts come after them (GuiMixin)
		HudElementRegistry.attachElementAfter(VanillaHudElements.SUBTITLES, Cassonade.id("minimap"), (graphics, deltaTracker) -> {
			graphics.nextStratum();
			Minimap.extractRenderState(graphics, deltaTracker);
		});
		HudElementRegistry.attachElementAfter(Cassonade.id("minimap"), Cassonade.id("clock"), ClockHud::extractRenderState);
		// Right of the clock, which says where its panel stops
		HudElementRegistry.attachElementAfter(Cassonade.id("clock"), Cassonade.id("hunted"), CombatHud::extractHunted);
		HudElementRegistry.attachElementBefore(VanillaHudElements.CROSSHAIR, Cassonade.id("markers"), MarkersInWorld::extractRenderState);
		HudElementRegistry.attachElementBefore(VanillaHudElements.CROSSHAIR, Cassonade.id("combat"), CombatHud::extractRenderState);
		HudElementRegistry.attachElementBefore(VanillaHudElements.CROSSHAIR, Cassonade.id("damage_direction"), DamageDirection::extractRenderState);
		HudElementRegistry.attachElementAfter(VanillaHudElements.CROSSHAIR, Cassonade.id("glance"), GlancePanel::extractRenderState);
		HudElementRegistry.attachElementAfter(VanillaHudElements.HOTBAR, Cassonade.id("durability"),
				HudLayout.shiftUp(DurabilityWarning::extractRenderState));
		HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT, Cassonade.id("quest_buttons"), QuestButtons::extractRenderState);
		HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT, Cassonade.id("quest_tracker"), QuestTracker::extractRenderState);
		HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT, Cassonade.id("picked_up"), PickedUpItems::extractRenderState);
		ResourceLoader.get(PackType.CLIENT_RESOURCES).registerReloadListener(QuestBook.ID, new QuestBook());
		ClientPlayNetworking.registerGlobalReceiver(WeatherForecast.TYPE, (forecast, context) -> ClockHud.receive(context.client(), forecast));
		ClientPlayNetworking.registerGlobalReceiver(EnchantmentSeedPayload.TYPE, (seed, context) -> EnchantmentPreview.receive(seed));
		ClientPlayNetworking.registerGlobalReceiver(HuntersPayload.TYPE, (hunters, context) -> CombatHud.receive(hunters));
		ClientPlayNetworking.registerGlobalReceiver(GlancePayload.TYPE, (glance, context) -> GlancePanel.receive(glance));
		ClientPlayNetworking.registerGlobalReceiver(TraderTimePayload.TYPE, (time, context) -> TraderTimePanel.receive(context.client(), time));
		ClientPlayNetworking.registerGlobalReceiver(PingPayload.TYPE, (ping, context) -> Pings.receive(context.client(), ping));
		ClientPlayNetworking.registerGlobalReceiver(FuelTimes.TYPE, (times, context) -> FuelTooltip.receive(times));
		ClientPlayNetworking.registerGlobalReceiver(FurnaceExperience.TYPE, (experience, context) -> FurnaceExperienceTooltip.receive(experience));
		// Status effects are listed in the top right corner too: framed like the rest, below the minimap and the quest followed
		HudElementRegistry.replaceElement(VanillaHudElements.MOB_EFFECTS, effects -> EffectsHud::extractRenderState);

		// Sort and deposit buttons in chests, barrels, ender chests and shulker boxes
		ScreenEvents.AFTER_INIT.register((minecraft, screen, width, height) -> {
			if (screen instanceof ContainerScreen || screen instanceof ShulkerBoxScreen) {
				StorageButtons.addToContainerScreen((AbstractContainerScreen<?>) screen, Screens.getWidgets(screen)::add);
				ContainerSearch.addToScreen((AbstractContainerScreen<?>) screen, Screens.getWidgets(screen)::add);
			}
		});
		// Bundles, shulker boxes and backpacks show their contents in the same grid, filled maps their map, food its hunger
		ClientTooltipComponentCallback.EVENT.register(data -> switch (data) {
			case ContainerPreview preview -> SlotGridTooltip.of(preview);
			case MapPreview preview -> new MapTooltip(preview);
			case FoodPreview preview -> new FoodTooltip(preview);
			case BundleTooltip bundle -> SlotGridTooltip.of(bundle);
			default -> null;
		});
		// What the anvil adds to every use on an item already worked on there
		ItemTooltipCallback.EVENT.register(RepairCostTooltip::append);
		// In a furnace, how many items a fuel smelts
		ItemTooltipCallback.EVENT.register(FuelTooltip::append);
		ChatMentions.init();

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
					QuestTracker.draw(graphics, mouseX, mouseY);
					QuestButtons.drawOver(graphics, mouseX, mouseY);
				});
				// A click on the quest followed opens the journal on it
				ScreenMouseEvents.allowMouseClick(screen).register((current, event) -> {
					QuestLog.Quest quest = event.button() == InputConstants.MOUSE_BUTTON_LEFT
							? QuestTracker.questAt(minecraft, current.width, event.x(), event.y()) : null;
					if (quest == null) {
						return true;
					}
					// Clicked like the buttons beside it
					minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
					minecraft.gui.setScreen(new QuestJournalScreen(quest.id()));
					return false;
				});
			}
		});

		ClientPlayConnectionEvents.JOIN.register((listener, sender, minecraft) -> {
			// A server takes the mod's payloads only if it has the mod
			ServerSupport.setOnClient(ClientPlayNetworking.canSend(BackpackTogglePayload.TYPE));
			WorldMap.open(minecraft);
			QuestLog.open(minecraft);
		});
		ClientPlayConnectionEvents.DISCONNECT.register((listener, minecraft) -> {
			ServerSupport.setOnClient(true);
			WorldMap.close();
			QuestLog.close();
			AchievementBanner.clear();
			QuestTracker.reset();
			CombatHud.reset();
			FuelTooltip.clear();
			FurnaceExperienceTooltip.clear();
			TraderTimePanel.clear();
		});

		ClientTickEvents.END_CLIENT_TICK.register(minecraft -> {
			TraderTimePanel.tick(minecraft);
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
			while (MINIMAP_ROTATION.consumeClick()) {
				if (Minimap.isShown(minecraft)) {
					MinimapRotationButton.toggle(minecraft);
				}
			}
			while (MARKER_DISPLAY.consumeClick()) {
				MarkerDisplayButton.cycle(minecraft);
			}
			while (OPEN_JOURNAL.consumeClick()) {
				minecraft.gui.setScreen(new QuestJournalScreen());
			}
			while (ROTATE_HOTBAR.consumeClick()) {
				HotbarRows.rotate(minecraft);
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
			InventorySounds.tick(minecraft);
			DurabilityWarning.tick(minecraft);
			DamageDirection.tick(minecraft);
			DynamicLights.tick(minecraft);
			Minimap.tick(minecraft);
			ClockHud.tick(minecraft);
			SpawnSpots.tick(minecraft);
			WorldMap.tick(minecraft);
			DeathMarker.tick(minecraft);
			CombatHud.tick(minecraft);
			PickedUpItems.tick(minecraft);
			NewItems.tick(minecraft);
			Pings.tick(minecraft);
			ScopeZoom.tick(minecraft);
			GlancePanel.tick(minecraft);
		});
	}
}
