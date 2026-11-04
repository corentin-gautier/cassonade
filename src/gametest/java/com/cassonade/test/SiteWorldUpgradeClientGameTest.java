package com.cassonade.test;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.mojang.serialization.MapCodec;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.gametest.v1.world.TestWorldSave;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.StructureTags;
import net.minecraft.util.RandomSource;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.pools.SinglePoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlockIgnoreProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.JigsawReplacementProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorList;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

import com.cassonade.tweaks.StoneBricks;
import com.cassonade.tweaks.Taverns;

/**
 * Once, by hand (-PsiteScreenshots -Ptests=SiteWorldUpgrade): brings the shots' world, made before taverns and stone
 * bricks, up to what a village is built as now, without losing what was built in it by hand. Each piece of the
 * village is laid over the vanilla template it was built from (read from the game's jar, since the mod's tavern
 * replaces the church under the same name): a block still the template's was never touched and is upgraded, the church
 * to the tavern and cobblestone to stone bricks as the generation does; any other block was placed by hand and stays.
 * Works on a copy, then puts it in place of .test-worlds/site-world.
 */
public class SiteWorldUpgradeClientGameTest implements FabricClientGameTest {
	private static final BlockPos SCENE = new BlockPos(4965, 112, 5453);

	@Override
	public void runTest(ClientGameTestContext context) {
		if (TestSelection.skips(this) || System.getProperty("cassonade.siteWorld") == null) {
			return;
		}
		Path cache = Path.of(System.getProperty("cassonade.siteWorld"));
		context.getInput().resizeWindow(1920, 1080);
		TestWorldSave save;
		try (TestSingleplayerContext placeholder = context.worldBuilder().adjustSettings(state -> state.setName("Plains Village")).create()) {
			save = placeholder.getWorldSave();
		}
		try {
			SiteScreenshotsClientGameTest.replace(cache, save.getSaveDirectory());
		} catch (IOException e) {
			throw new AssertionError(e);
		}
		BoundingBox church;
		try (TestSingleplayerContext world = save.open()) {
			world.getConnection().waitForChunksRender();
			church = world.getServer().computeOnServer(server -> findChurch(server.overworld()));
			look(context, world, church, "site-world-church-before");
			String report = world.getServer().computeOnServer(SiteWorldUpgradeClientGameTest::upgrade);
			System.out.println(report);
			look(context, world, church, "site-world-church-after");
		}
		try {
			SiteScreenshotsClientGameTest.replace(save.getSaveDirectory(), cache);
		} catch (IOException e) {
			throw new AssertionError(e);
		}
	}

	/** A look at the church from outside, the player put back where it stood after, as the shots expect it. */
	private static void look(ClientGameTestContext context, TestSingleplayerContext world, BoundingBox church, String name) {
		double[] stood = context.computeOnClient(client -> new double[] {client.player.getX(), client.player.getY(), client.player.getZ(), client.player.getYRot(),
				client.player.getXRot()});
		// A spectator for the look, so the player neither falls nor is hurt in the world kept
		String mode = world.getServer().computeOnServer(server -> server.getPlayerList().getPlayers().getFirst().gameMode().getSerializedName());
		world.getServer().runCommand("gamemode spectator @a");
		var center = church.getCenter();
		world.getServer().runCommand("tp @a " + (center.getX() + 14) + " " + (church.maxY() + 4) + " " + (center.getZ() + 14) + " facing " + center.getX() + " "
				+ church.minY() + " " + center.getZ());
		world.getConnection().waitForChunksRender();
		context.waitTicks(40);
		context.runOnClient(client -> client.gui.hud.getChat().clearMessages(false));
		Screenshots.take(context, name);
		world.getServer().runCommand("tp @a " + stood[0] + " " + stood[1] + " " + stood[2] + " " + stood[3] + " " + stood[4]);
		world.getServer().runCommand("gamemode " + mode + " @a");
		context.waitTicks(5);
	}

	private static List<StructureStart> villages(ServerLevel level) {
		List<StructureStart> found = new ArrayList<>();
		var structures = level.registryAccess().lookupOrThrow(Registries.STRUCTURE);
		int cx = SCENE.getX() >> 4;
		int cz = SCENE.getZ() >> 4;
		for (int x = cx - 16; x <= cx + 16; x++) {
			for (int z = cz - 16; z <= cz + 16; z++) {
				for (var entry : level.getChunk(x, z).getAllStarts().entrySet()) {
					if (entry.getValue().isValid() && structures.wrapAsHolder(entry.getKey()).is(StructureTags.VILLAGE)) {
						found.add(entry.getValue());
					}
				}
			}
		}
		if (found.isEmpty()) {
			throw new AssertionError("Expected a village around " + SCENE);
		}
		return found;
	}

	private static BoundingBox findChurch(ServerLevel level) {
		for (StructureStart village : villages(level)) {
			for (StructurePiece piece : village.getPieces()) {
				if (Taverns.is(piece)) {
					return piece.getBoundingBox();
				}
			}
		}
		throw new AssertionError("Expected a church in the village");
	}

	private static String upgrade(MinecraftServer server) {
		ServerLevel level = server.overworld();
		HolderGetter<Block> blocks = level.holderLookup(Registries.BLOCK);
		StringBuilder report = new StringBuilder("Site world upgrade:");
		int bricks = 0;
		int keptCobblestone = 0;
		for (StructureStart village : villages(level)) {
			for (StructurePiece piece : village.getPieces()) {
				if (!(piece instanceof PoolElementStructurePiece pool) || !(pool.getElement() instanceof SinglePoolElement element)
						|| element.getProjection() != StructureTemplatePool.Projection.RIGID) {
					continue;
				}
				Identifier id = element.getTemplateLocation();
				StructurePlaceSettings settings = new StructurePlaceSettings().setRotation(pool.getRotation());
				Map<BlockPos, BlockState> built = blocks(vanilla(server, blocks, id), pool.getPosition(), settings);
				if (Taverns.is(piece)) {
					report.append(tavern(level, pool, element, id, settings, built));
					continue;
				}
				for (var block : built.entrySet()) {
					BlockState now = level.getBlockState(block.getKey());
					if (!cobblestone(now)) {
						continue;
					}
					if (family(now) == family(block.getValue())) {
						BlockState stone = StoneBricks.of(now, block.getKey());
						if (stone != null) {
							level.setBlock(block.getKey(), stone, Block.UPDATE_CLIENTS);
							bricks++;
						}
					} else {
						keptCobblestone++;
					}
				}
			}
		}
		report.append("\n  ").append(bricks).append(" cobblestone blocks of the houses turned to stone bricks, ").append(keptCobblestone)
				.append(" placed by hand kept");
		return report.toString();
	}

	/** The church taken down but for what was built in it by hand, the tavern put up around it. */
	private static String tavern(ServerLevel level, PoolElementStructurePiece piece, SinglePoolElement element, Identifier id, StructurePlaceSettings settings,
			Map<BlockPos, BlockState> church) {
		Set<BlockPos> byHand = new HashSet<>();
		StringBuilder kept = new StringBuilder();
		for (var block : church.entrySet()) {
			BlockState now = level.getBlockState(block.getKey());
			// A jigsaw became whatever its final state was; anything else not the template's was placed by hand
			if (!block.getValue().is(Blocks.JIGSAW) && family(now) != family(block.getValue())) {
				byHand.add(block.getKey().immutable());
				kept.append("\n    ").append(block.getKey().toShortString()).append(": ").append(BuiltInRegistries.BLOCK.getKey(now.getBlock()))
						.append(" (church: ").append(BuiltInRegistries.BLOCK.getKey(block.getValue().getBlock())).append(")");
			}
		}
		for (BlockPos pos : church.keySet()) {
			if (!byHand.contains(pos)) {
				level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
			}
		}
		StructureTemplate tavern = level.getServer().getStructureTemplateManager().getOrCreate(id);
		StructurePlaceSettings place = new StructurePlaceSettings().setRotation(piece.getRotation()).setKnownShape(true).setIgnoreEntities(false).setFinalizeEntities(true);
		place.addProcessor(BlockIgnoreProcessor.STRUCTURE_BLOCK);
		place.addProcessor(JigsawReplacementProcessor.INSTANCE);
		processors(element).forEach(place::addProcessor);
		place.addProcessor(new Keep(byHand, church.keySet()));
		tavern.placeInWorld(level, piece.getPosition(), piece.getPosition(), place, RandomSource.create(piece.getPosition().asLong()), Block.UPDATE_CLIENTS);
		for (BlockPos pos : blocks(tavern, piece.getPosition(), settings).keySet()) {
			BlockState stone = byHand.contains(pos) ? null : StoneBricks.of(level.getBlockState(pos), pos);
			if (stone != null) {
				level.setBlock(pos, stone, Block.UPDATE_CLIENTS);
			}
		}
		Taverns.keepKegs(level, piece.getBoundingBox());
		return "\n  church " + id + " at " + piece.getPosition().toShortString() + " became the tavern, " + byHand.size() + " blocks placed by hand kept:" + kept;
	}

	/** Leaves out of the tavern what was placed by hand, in the church or around it. */
	private record Keep(Set<BlockPos> byHand, Set<BlockPos> church) implements StructureProcessor {
		@Override
		public StructureTemplate.StructureBlockInfo processBlock(LevelReader level, BlockPos target, BlockPos reference, BlockPos relative,
				StructureTemplate.StructureBlockInfo info, StructurePlaceSettings settings) {
			if (this.byHand.contains(info.pos()) || (!this.church.contains(info.pos()) && !level.getBlockState(info.pos()).isAir())) {
				return null;
			}
			return info;
		}

		@Override
		public MapCodec<? extends StructureProcessor> codec() {
			return MapCodec.unit(this);
		}
	}

	@SuppressWarnings("unchecked")
	private static List<StructureProcessor> processors(SinglePoolElement element) {
		try {
			Field field = SinglePoolElement.class.getDeclaredField("processors");
			field.setAccessible(true);
			return ((Holder<StructureProcessorList>) field.get(element)).value().list();
		} catch (ReflectiveOperationException e) {
			throw new AssertionError(e);
		}
	}

	/** The vanilla template, from the game's jar: the mod's resources replace the churches under the same names. */
	private static StructureTemplate vanilla(MinecraftServer server, HolderGetter<Block> blocks, Identifier id) {
		try {
			Path jar = Path.of(MinecraftServer.class.getProtectionDomain().getCodeSource().getLocation().toURI());
			try (FileSystem files = FileSystems.newFileSystem(jar); InputStream input = Files.newInputStream(files.getPath("data", id.getNamespace(), "structure",
					id.getPath() + ".nbt"))) {
				CompoundTag tag = NbtIo.readCompressed(input, NbtAccounter.unlimitedHeap());
				StructureTemplate template = new StructureTemplate();
				template.load(blocks, DataFixTypes.STRUCTURE.updateToCurrentVersion(server.getFixerUpper(), tag, NbtUtils.getDataVersion(tag, 500)));
				return template;
			}
		} catch (Exception e) {
			throw new AssertionError("Could not read " + id + " from the game's jar", e);
		}
	}

	/** Every block of the template where it was placed, structure voids left out. */
	private static Map<BlockPos, BlockState> blocks(StructureTemplate template, BlockPos origin, StructurePlaceSettings settings) {
		Map<BlockPos, BlockState> placed = new HashMap<>();
		for (Block block : BuiltInRegistries.BLOCK) {
			if (block == Blocks.STRUCTURE_VOID) {
				continue;
			}
			for (StructureTemplate.StructureBlockInfo info : template.filterBlocks(origin, settings, block)) {
				placed.put(info.pos().immutable(), info.state());
			}
		}
		return placed;
	}

	private static boolean cobblestone(BlockState state) {
		return StoneBricks.of(state, BlockPos.ZERO) != null || StoneBricks.of(state, BlockPos.ZERO.east()) != null;
	}

	/** The block, mossy or not: the village's processors turn some of the template's cobblestone mossy. */
	private static Block family(BlockState state) {
		Block block = state.getBlock();
		if (block == Blocks.MOSSY_COBBLESTONE) {
			return Blocks.COBBLESTONE;
		}
		if (block == Blocks.MOSSY_COBBLESTONE_STAIRS) {
			return Blocks.COBBLESTONE_STAIRS;
		}
		if (block == Blocks.MOSSY_COBBLESTONE_SLAB) {
			return Blocks.COBBLESTONE_SLAB;
		}
		if (block == Blocks.MOSSY_COBBLESTONE_WALL) {
			return Blocks.COBBLESTONE_WALL;
		}
		return block;
	}
}
