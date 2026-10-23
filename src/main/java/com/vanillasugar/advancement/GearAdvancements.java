package com.vanillasugar.advancement;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import com.vanillasugar.VanillaSugar;
import com.vanillasugar.compass.PlayerMarkers;
import com.vanillasugar.gear.GearSlot;
import com.vanillasugar.gear.PlayerGear;

/**
 * The mod's advancements (data/vanillasugar/advancement), which the quest journal guides to: wearing
 * a backpack, a compass for the minimap, a map for the world map, and placing a marker on it. Their
 * one criterion cannot be met by a trigger: once a second, each player is looked over and given
 * those they have earned, also when they were wearing the gear before the advancements existed.
 */
public final class GearAdvancements {
	private static final int INTERVAL = 20;

	private GearAdvancements() {
	}

	public static void init() {
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			if (server.getTickCount() % INTERVAL == 0) {
				for (ServerPlayer player : server.getPlayerList().getPlayers()) {
					check(server, player);
				}
			}
		});
	}

	private static void check(MinecraftServer server, ServerPlayer player) {
		if (PlayerGear.isWearing(player, GearSlot.BACKPACK)) {
			award(server, player, "backpack");
		}
		if (PlayerGear.isWearing(player, GearSlot.COMPASS)) {
			award(server, player, "minimap");
		}
		if (PlayerGear.isWearing(player, GearSlot.MAP)) {
			award(server, player, "world_map");
		}
		if (!PlayerMarkers.of(player).markers().isEmpty()) {
			award(server, player, "map_marker");
		}
	}

	private static void award(MinecraftServer server, ServerPlayer player, String name) {
		AdvancementHolder advancement = server.getAdvancements().get(VanillaSugar.id(name));
		if (advancement == null) {
			return;
		}
		AdvancementProgress progress = player.getAdvancements().getOrStartProgress(advancement);
		if (!progress.isDone()) {
			for (String criterion : progress.getRemainingCriteria()) {
				player.getAdvancements().award(advancement, criterion);
			}
		}
	}
}
