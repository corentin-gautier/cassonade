package com.cassonade;

import net.fabricmc.api.ModInitializer;

import net.minecraft.resources.Identifier;

import com.cassonade.clock.WeatherForecast;
import com.cassonade.advancement.GearAdvancements;
import com.cassonade.combat.HuntersPayload;
import com.cassonade.compass.MapMarkerPayload;
import com.cassonade.compass.PlayerMarkers;
import com.cassonade.crafting.CraftAllPayload;
import com.cassonade.enchanting.EnchantmentSeedPayload;
import com.cassonade.farming.RightClickHarvest;
import com.cassonade.furnace.FuelTimes;
import com.cassonade.gear.BackpackTogglePayload;
import com.cassonade.gear.ElytraGear;
import com.cassonade.gear.PlayerGear;
import com.cassonade.hopper.HopperFilter;
import com.cassonade.item.CassonadeItems;
import com.cassonade.spyglass.GlancePayload;
import com.cassonade.spyglass.GlanceRequestPayload;
import com.cassonade.storage.LockedSlots;
import com.cassonade.storage.StoragePayload;

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
		ElytraGear.init();
		BackpackTogglePayload.init();
		PlayerMarkers.init();
		MapMarkerPayload.init();
		WeatherForecast.init();
		FuelTimes.init();
		StoragePayload.init();
		LockedSlots.init();
		CraftAllPayload.init();
		EnchantmentSeedPayload.init();
		HuntersPayload.init();
		GlancePayload.init();
		GlanceRequestPayload.init();
		RightClickHarvest.init();
		GearAdvancements.init();
		LOGGER.info("Cassonade loaded");
	}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}
}
