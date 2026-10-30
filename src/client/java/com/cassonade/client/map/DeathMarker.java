package com.cassonade.client.map;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;

import org.jspecify.annotations.Nullable;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import com.cassonade.Cassonade;

/**
 * Where the player last died, shown like a marker of its own (the treasure map's red X) on the minimap,
 * the world map and in the world, without counting toward the markers. The game already gives the client
 * that place, for the recovery compass. Coming back to it, or a right click on it on the world map, puts it
 * away until the next death; that is kept next to the explored map (WorldMap), per world or server.
 */
public final class DeathMarker {
	/** Closer than this, the player is back where it died, once it went farther than {@link #AWAY}. */
	private static final double REACHED = 4;
	/** Respawning next to where it died, as at a bed nearby, is not coming back yet. */
	private static final double AWAY = 12;
	private static final String FILE = "dismissed_death.json";

	private static @Nullable Path file;
	/** The death put away, which the marker no longer shows. */
	private static @Nullable GlobalPos dismissed;
	/** The death the player has gone away from since. */
	private static @Nullable GlobalPos leftBehind;

	private DeathMarker() {
	}

	public static void open(Path folder) {
		file = folder.resolve(FILE);
		dismissed = null;
		if (Files.exists(file)) {
			try {
				dismissed = GlobalPos.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(Files.readString(file))).result().orElse(null);
			} catch (IOException | RuntimeException e) {
				Cassonade.LOGGER.warn("Could not read {}", file, e);
			}
		}
	}

	public static void close() {
		file = null;
		dismissed = null;
		leftBehind = null;
	}

	/** Makes the marker visible again and forgets the previous death's return trip. */
	public static void onDeath() {
		leftBehind = null;
		if (dismissed == null) {
			return;
		}
		dismissed = null;
		if (file != null) {
			try {
				Files.deleteIfExists(file);
			} catch (IOException e) {
				Cassonade.LOGGER.warn("Could not clear {}", file, e);
			}
		}
	}

	/** Where the player last died, unless it was put away. */
	public static Optional<GlobalPos> of(Player player) {
		return player.getLastDeathLocation().filter(pos -> !pos.equals(dismissed));
	}

	/** Where the player last died, in the dimension it is in now. */
	public static Optional<BlockPos> here(Player player) {
		return of(player).filter(pos -> pos.dimension() == player.level().dimension()).map(GlobalPos::pos);
	}

	public static void tick(Minecraft minecraft) {
		LocalPlayer player = minecraft.player;
		if (player == null) {
			return;
		}
		here(player).ifPresent(pos -> {
			double distance = player.position().distanceTo(Vec3.atBottomCenterOf(pos));
			GlobalPos death = player.getLastDeathLocation().orElseThrow();
			if (distance > AWAY) {
				leftBehind = death;
			} else if (distance < REACHED && death.equals(leftBehind)) {
				dismiss(player);
			}
		});
	}

	/** Puts the last death away, until the next one. */
	public static void dismiss(Player player) {
		player.getLastDeathLocation().ifPresent(pos -> {
			dismissed = pos;
			if (file != null) {
				try {
					Files.createDirectories(file.getParent());
					Files.writeString(file, GlobalPos.CODEC.encodeStart(JsonOps.INSTANCE, pos).getOrThrow().toString());
				} catch (IOException e) {
					Cassonade.LOGGER.warn("Could not write {}", file, e);
				}
			}
		});
	}
}
