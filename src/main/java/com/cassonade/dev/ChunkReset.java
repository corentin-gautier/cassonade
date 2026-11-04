package com.cassonade.dev;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelResource;

import com.cassonade.Cassonade;

/**
 * {@code /dev regen}: the world's terrain is made again the next time it opens, to see a change to world generation in
 * a world already played. Chunks cannot be made again while the server runs, so the command only leaves a mark in the
 * save; the next start, before any level loads, deletes every dimension's chunks, entities and points of interest, and
 * the world map's exploration, which would show the old terrain. Players, their inventories and the mod's saved data
 * stay; each player is stood on the new ground as they join.
 */
final class ChunkReset {
	/** Folders of each dimension that hold what the world generation made. */
	private static final List<String> GENERATED = List.of("region", "entities", "poi");

	/** Players still to stand on the new ground, until the server stops. */
	private static final Set<UUID> TO_PLACE = new HashSet<>();
	private static boolean reset;

	private ChunkReset() {
	}

	static void init() {
		ServerLifecycleEvents.SERVER_STARTING.register(ChunkReset::resetIfAsked);
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
			reset = false;
			TO_PLACE.clear();
		});
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			ServerPlayer player = handler.getPlayer();
			if (reset && TO_PLACE.add(player.getUUID())) {
				place(player);
			}
		});
	}

	/** Leaves the mark read at the next start, or takes it back if already left: true when the terrain will be made again. */
	static boolean toggle(MinecraftServer server) {
		try {
			Path mark = mark(server);
			if (Files.deleteIfExists(mark)) {
				return false;
			}
			Files.createDirectories(mark.getParent());
			Files.writeString(mark, "");
			return true;
		} catch (IOException exception) {
			throw new UncheckedIOException(exception);
		}
	}

	private static Path mark(MinecraftServer server) {
		return server.getWorldPath(LevelResource.ROOT).resolve("cassonade/dev-regen");
	}

	private static void resetIfAsked(MinecraftServer server) {
		Path mark = mark(server);
		if (!Files.exists(mark)) {
			return;
		}
		Path root = server.getWorldPath(LevelResource.ROOT);
		try (Stream<Path> folders = Files.walk(root.resolve("dimensions"))) {
			for (Path folder : folders.filter(path -> Files.isDirectory(path) && GENERATED.contains(path.getFileName().toString())).toList()) {
				delete(folder);
			}
			delete(root.resolve("cassonade/worldmap"));
			Files.delete(mark);
		} catch (IOException exception) {
			throw new UncheckedIOException(exception);
		}
		reset = true;
		Cassonade.LOGGER.info("Deleted the generated chunks of {}, to be made again", root.normalize().getFileName());
	}

	private static void delete(Path folder) throws IOException {
		if (!Files.exists(folder)) {
			return;
		}
		try (Stream<Path> paths = Files.walk(folder)) {
			for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
				Files.delete(path);
			}
		}
	}

	/** Where the player stood, the new ground could be above their head or far below their feet. */
	private static void place(ServerPlayer player) {
		ServerLevel level = player.level();
		BlockPos pos = player.blockPosition();
		level.getChunk(pos);
		BlockPos floor = DevCommands.floorNear(level, pos, pos.getY(), feet -> level.dimensionType().hasCeiling() || level.canSeeSky(feet));
		if (floor != null) {
			player.teleportTo(level, floor.getX() + 0.5, floor.getY(), floor.getZ() + 0.5, Set.of(), player.getYRot(), player.getXRot(), true);
		}
		player.sendSystemMessage(Component.literal("Terrain made again from scratch"));
	}
}
