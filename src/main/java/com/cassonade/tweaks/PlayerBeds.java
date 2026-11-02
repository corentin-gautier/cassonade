package com.cassonade.tweaks;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/** A bed is a player's once they respawn there. Only players online are known: an absent player's bed is freed on their return, when its villager next tries it. */
public final class PlayerBeds {
	private PlayerBeds() {
	}

	public static boolean isPlayerBed(ServerLevel level, BlockPos pos) {
		for (ServerPlayer player : level.getServer().getPlayerList().getPlayers()) {
			ServerPlayer.RespawnConfig respawn = player.getRespawnConfig();
			if (respawn != null && respawn.respawnData().dimension() == level.dimension() && respawn.respawnData().pos().equals(pos)) {
				return true;
			}
		}
		return false;
	}
}
