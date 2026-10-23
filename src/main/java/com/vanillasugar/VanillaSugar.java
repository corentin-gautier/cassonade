package com.vanillasugar;

import net.fabricmc.api.ModInitializer;

import net.minecraft.resources.Identifier;

import com.vanillasugar.clock.WeatherForecast;
import com.vanillasugar.advancement.GearAdvancements;
import com.vanillasugar.compass.MapMarkerPayload;
import com.vanillasugar.compass.PlayerMarkers;
import com.vanillasugar.enchanting.EnchantmentSeedPayload;
import com.vanillasugar.farming.RightClickHarvest;
import com.vanillasugar.gear.BackpackTogglePayload;
import com.vanillasugar.gear.ElytraGear;
import com.vanillasugar.gear.PlayerGear;
import com.vanillasugar.hopper.HopperFilter;
import com.vanillasugar.item.VanillaSugarItems;
import com.vanillasugar.storage.StoragePayload;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VanillaSugar implements ModInitializer {
	public static final String MOD_ID = "vanillasugar";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		VanillaSugarItems.init();
		HopperFilter.init();
		PlayerGear.init();
		ElytraGear.init();
		BackpackTogglePayload.init();
		PlayerMarkers.init();
		MapMarkerPayload.init();
		WeatherForecast.init();
		StoragePayload.init();
		EnchantmentSeedPayload.init();
		RightClickHarvest.init();
		GearAdvancements.init();
		LOGGER.info("Vanilla Sugar loaded");
	}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}
}
