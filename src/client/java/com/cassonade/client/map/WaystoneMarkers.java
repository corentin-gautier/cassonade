package com.cassonade.client.map;

import java.util.List;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

import net.minecraft.world.level.Level;

import com.cassonade.waystone.WaystoneListPayload;
import com.cassonade.waystone.WaystoneMapRequestPayload;

/**
 * The waystones the player knows, for the minimap and the world map. The server keeps them: they are asked for when a map
 * shows, then every few seconds with the minimap up, for those discovered, renamed or broken since.
 */
public final class WaystoneMarkers {
	private static final int REQUEST_TICKS = 100;
	private static List<WaystoneListPayload.Destination> known = List.of();
	private static int untilRequest;

	private WaystoneMarkers() {
	}

	/** A server without the mod has none to show. */
	static void request() {
		if (ClientPlayNetworking.canSend(WaystoneMapRequestPayload.TYPE)) {
			ClientPlayNetworking.send(new WaystoneMapRequestPayload());
		}
	}

	static void shownTick() {
		if (--untilRequest <= 0) {
			request();
			untilRequest = REQUEST_TICKS;
		}
	}

	/** So the minimap asks right away when it comes back. */
	static void hiddenTick() {
		untilRequest = 0;
	}

	public static void receive(List<WaystoneListPayload.Destination> waystones) {
		known = waystones;
	}

	public static void clear() {
		known = List.of();
	}

	/** Those of this dimension. */
	public static List<WaystoneListPayload.Destination> here(Level level) {
		return known.stream().filter(waystone -> waystone.pos().dimension() == level.dimension()).toList();
	}
}
