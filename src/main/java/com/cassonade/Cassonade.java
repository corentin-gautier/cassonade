package com.cassonade;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;

import net.minecraft.resources.Identifier;

import com.cassonade.clock.WeatherForecast;
import com.cassonade.advancement.GearAdvancements;
import com.cassonade.combat.HuntersPayload;
import com.cassonade.combat.Netherite;
import com.cassonade.compass.MapMarkerPayload;
import com.cassonade.compass.PlayerMarkers;
import com.cassonade.crafting.CraftAllPayload;
import com.cassonade.dev.DevCommands;
import com.cassonade.enchanting.EnchantmentSeedPayload;
import com.cassonade.farming.RightClickHarvest;
import com.cassonade.furnace.FuelTimes;
import com.cassonade.furnace.FurnaceExperience;
import com.cassonade.gear.BackpackTogglePayload;
import com.cassonade.gear.ElytraGear;
import com.cassonade.gear.OrganizedBackpack;
import com.cassonade.gear.PlayerGear;
import com.cassonade.hopper.HopperFilter;
import com.cassonade.item.CassonadeItems;
import com.cassonade.mushroom.FairyRing;
import com.cassonade.mushroom.MyceliumPlants;
import com.cassonade.ping.PingPayload;
import com.cassonade.tweaks.ChoosePaintingPayload;
import com.cassonade.tweaks.NoCreepers;
import com.cassonade.tweaks.SnowShovel;
import com.cassonade.tweaks.SwapArmorPayload;
import com.cassonade.storage.ShulkerFilling;
import com.cassonade.trader.WorkstationPayload;
import com.cassonade.treasure.PirateBooty;
import com.cassonade.waystone.Waystones;
import com.cassonade.spyglass.GlancePayload;
import com.cassonade.spyglass.GlanceRequestPayload;
import com.cassonade.storage.LockedSlots;
import com.cassonade.storage.StoragePayload;
import com.cassonade.trader.TraderTimePayload;
import com.cassonade.woodcutter.Woodcutter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Cassonade implements ModInitializer {
	public static final String MOD_ID = "cassonade";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		CassonadeItems.init();
		HopperFilter.init();
		PlayerGear.init();
		OrganizedBackpack.init();
		ElytraGear.init();
		BackpackTogglePayload.init();
		PlayerMarkers.init();
		MapMarkerPayload.init();
		WeatherForecast.init();
		FuelTimes.init();
		Woodcutter.init();
		FurnaceExperience.init();
		StoragePayload.init();
		LockedSlots.init();
		CraftAllPayload.init();
		EnchantmentSeedPayload.init();
		HuntersPayload.init();
		GlancePayload.init();
		GlanceRequestPayload.init();
		TraderTimePayload.init();
		PingPayload.init();
		SwapArmorPayload.init();
		ChoosePaintingPayload.init();
		RightClickHarvest.init();
		SnowShovel.init();
		NoCreepers.init();
		Netherite.init();
		FairyRing.init();
		MyceliumPlants.init();
		PirateBooty.init();
		WorkstationPayload.init();
		ShulkerFilling.init();
		Waystones.init();
		GearAdvancements.init();
		// Commands to try the features in runClient, never in a player's game
		if (FabricLoader.getInstance().isDevelopmentEnvironment()) {
			DevCommands.init();
		}
		LOGGER.info("Cassonade loaded");
	}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}
}
