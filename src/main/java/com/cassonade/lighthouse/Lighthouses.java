package com.cassonade.lighthouse;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.WeakHashMap;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;
import net.minecraft.world.level.block.BarrelBlock;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureCheckResult;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacement;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

import com.cassonade.Cassonade;

/**
 * Lighthouses on the coasts (a jigsaw structure of one piece, its template edited with /dev edit), whose lamp a player
 * sees from far out at sea at night, much farther than the terrain is drawn. The client cannot know where they are:
 * every few seconds, the server works out the lighthouses within {@link #VIEW} of each player from the structure set's
 * grid, as /locate does, and sends the lamps when they change. A spot of the grid is read once per level and kept: its
 * structure start alone, made if need be, never the whole chunk, and at most one a tick, so a fast flight never
 * stalls the server. On a server without the mod, nothing is sent and the lamps only shine as blocks.
 */
public final class Lighthouses {
	public static final ResourceKey<Structure> STRUCTURE = ResourceKey.create(Registries.STRUCTURE, Cassonade.id("lighthouse"));
	/** How far a lamp is seen: about as far as a light carries over a calm sea on a dark night, in the game's scale. */
	public static final int VIEW = 768;
	private static final Identifier TEMPLATE = Cassonade.id("lighthouse/lighthouse");
	private static final int INTERVAL = 40;
	private static final Map<ServerLevel, Map<Long, Optional<BlockPos>>> KNOWN = new WeakHashMap<>();
	private static final Map<ServerPlayer, List<BlockPos>> SENT = new WeakHashMap<>();

	private Lighthouses() {
	}

	public static void init() {
		PayloadTypeRegistry.clientboundPlay().register(LighthousesPayload.TYPE, LighthousesPayload.STREAM_CODEC);
		ServerTickEvents.END_SERVER_TICK.register(Lighthouses::tick);
	}

	private static void tick(MinecraftServer server) {
		boolean resolved = false;
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			// Players take turns, a few ticks apart
			if ((server.getTickCount() + player.getId()) % INTERVAL != 0 || !ServerPlayNetworking.canSend(player, LighthousesPayload.TYPE)) {
				continue;
			}
			List<BlockPos> lamps = new ArrayList<>();
			boolean complete = true;
			for (Spot spot : spots(player)) {
				Optional<BlockPos> lamp = KNOWN.computeIfAbsent(player.level(), level -> new HashMap<>()).get(spot.chunk.pack());
				if (lamp == null) {
					if (resolved) {
						complete = false;
						continue;
					}
					lamp = resolve(player.level(), spot);
					KNOWN.get(player.level()).put(spot.chunk.pack(), lamp);
					resolved = true;
				}
				lamp.filter(pos -> pos.closerThan(player.blockPosition(), VIEW)).ifPresent(lamps::add);
			}
			// Sent again once every spot is read, as the list may have grown
			if (!lamps.equals(SENT.get(player)) || !complete) {
				SENT.put(player, complete ? lamps : null);
				ServerPlayNetworking.send(player, new LighthousesPayload(lamps));
			}
		}
	}

	private record Spot(Holder<Structure> structure, StructurePlacement placement, ChunkPos chunk) {
	}

	/** The grid's spots within sight of the player, where a lighthouse may stand: none in a dimension without them. */
	private static List<Spot> spots(ServerPlayer player) {
		ServerLevel level = player.level();
		Optional<Holder.Reference<Structure>> structure = level.registryAccess().lookupOrThrow(Registries.STRUCTURE).get(STRUCTURE);
		if (structure.isEmpty()) {
			return List.of();
		}
		ChunkGeneratorStructureState state = level.getChunkSource().getGeneratorState();
		List<Spot> spots = new ArrayList<>();
		for (StructurePlacement placement : state.getPlacementsForStructure(structure.get())) {
			if (!(placement instanceof RandomSpreadStructurePlacement grid)) {
				continue;
			}
			int spacing = grid.spacing();
			int reach = (VIEW >> 4) + 1;
			ChunkPos here = player.chunkPosition();
			for (int x = Math.floorDiv(here.x() - reach, spacing); x <= Math.floorDiv(here.x() + reach, spacing); x++) {
				for (int z = Math.floorDiv(here.z() - reach, spacing); z <= Math.floorDiv(here.z() + reach, spacing); z++) {
					ChunkPos chunk = grid.getPotentialStructureChunk(state.getLevelSeed(), x * spacing, z * spacing);
					if (Math.abs(chunk.x() - here.x()) <= reach && Math.abs(chunk.z() - here.z()) <= reach) {
						spots.add(new Spot(structure.get(), grid, chunk));
					}
				}
			}
		}
		return spots;
	}

	/** The lamp of the lighthouse the spot has, if it has one: its structure start, made now when the chunk has none yet. */
	private static Optional<BlockPos> resolve(ServerLevel level, Spot spot) {
		StructureCheckResult check = level.structureManager().checkStructurePresence(spot.chunk, spot.structure.value(), spot.placement, false);
		if (check == StructureCheckResult.START_NOT_PRESENT) {
			return Optional.empty();
		}
		ChunkAccess chunk = level.getChunk(spot.chunk.x(), spot.chunk.z(), ChunkStatus.STRUCTURE_STARTS);
		StructureStart start = level.structureManager().getStartForStructure(spot.structure.value(), chunk);
		if (start == null || !start.isValid()) {
			return Optional.empty();
		}
		return lamp(level, start);
	}

	/**
	 * Where the lighthouse's lamp is: the highest block of its template that gives light, whatever it is and wherever
	 * /dev edit moved it, turned and placed as the piece was.
	 */
	public static Optional<BlockPos> lamp(ServerLevel level, StructureStart start) {
		if (!(start.getPieces().getFirst() instanceof PoolElementStructurePiece piece)) {
			return Optional.empty();
		}
		StructureTemplate template = level.getStructureTemplateManager().getOrCreate(TEMPLATE);
		StructurePlaceSettings settings = new StructurePlaceSettings().setRotation(piece.getRotation());
		// The template's blocks by kind, each kind read once: the light is the block's state, not its kind
		return BuiltInRegistries.BLOCK.stream()
				.flatMap(block -> template.filterBlocks(piece.getPosition(), settings, block).stream())
				.filter(info -> info.state().getLightEmission() > 0)
				.map(StructureTemplate.StructureBlockInfo::pos)
				.max(Comparator.comparingInt(BlockPos::getY));
	}

	/** Whether the start is a lighthouse's. */
	public static boolean is(WorldGenLevel level, Structure structure) {
		return level.registryAccess().lookupOrThrow(Registries.STRUCTURE).getResourceKey(structure).filter(STRUCTURE::equals).isPresent();
	}

	/**
	 * Once the part of the lighthouse in a chunk stands: the keeper, a villager on the floor by the bed's foot, made
	 * here rather than kept in the template, as a structure block saves none, so /dev edit would lose them; and the
	 * lookout's barrel, with its loot, taken off the workstations, so the keeper takes the cottage's one rather than
	 * climb the tower for it (placing them registered them for after this step: this is queued after it).
	 */
	public static void furnish(WorldGenLevel level, BoundingBox area) {
		List<BlockPos> lookouts = new ArrayList<>();
		for (BlockPos pos : BlockPos.betweenClosed(area.minX(), area.minY(), area.minZ(), area.maxX(), area.maxY(), area.maxZ())) {
			BlockState state = level.getBlockState(pos);
			if (state.getBlock() instanceof BedBlock && state.getValue(BedBlock.PART) == BedPart.HEAD) {
				keeper(level, pos.relative(state.getValue(BedBlock.FACING).getOpposite()));
			} else if (state.getBlock() instanceof BarrelBlock && level.getBlockEntity(pos) instanceof BarrelBlockEntity barrel && barrel.getLootTable() != null) {
				lookouts.add(pos.immutable());
			}
		}
		if (!lookouts.isEmpty()) {
			ServerLevel server = level.getLevel();
			server.getServer().execute(() -> lookouts.forEach(pos -> {
				if (server.getPoiManager().existsAtPosition(PoiTypes.FISHERMAN, pos)) {
					server.getPoiManager().remove(pos);
				}
			}));
		}
	}

	private static void keeper(WorldGenLevel level, BlockPos foot) {
		Villager keeper = EntityTypes.VILLAGER.create(level.getLevel(), EntitySpawnReason.STRUCTURE);
		if (keeper == null) {
			return;
		}
		keeper.snapTo(foot.getX() + 0.5, foot.getY() + 0.6, foot.getZ() + 0.5, 0, 0);
		keeper.finalizeSpawn(level, level.getCurrentDifficultyAt(foot), EntitySpawnReason.STRUCTURE, null);
		keeper.setPersistenceRequired();
		level.addFreshEntityWithPassengers(keeper);
	}
}
