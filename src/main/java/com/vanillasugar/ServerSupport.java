package com.vanillasugar;

import net.minecraft.world.entity.player.Player;

/**
 * Whether the server has the mod. A server always does; a client learns it on joining (VanillaSugarClient).
 * Without it, what only the mod's server can keep (gear, backpack, sorting) stays out of the way, so the
 * client does not show what the server never did.
 */
public final class ServerSupport {
	private static volatile boolean onClient = true;

	private ServerSupport() {
	}

	public static boolean has(Player player) {
		return !player.level().isClientSide() || onClient;
	}

	/** On the client: whether the server it plays on has the mod. */
	public static boolean onClient() {
		return onClient;
	}

	public static void setOnClient(boolean present) {
		onClient = present;
	}
}
