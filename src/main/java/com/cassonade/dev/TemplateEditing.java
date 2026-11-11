package com.cassonade.dev;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.StructureBlockEntity;
import net.minecraft.world.level.block.state.properties.StructureMode;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

/**
 * The mod's structure templates (the taverns), edited in the game rather than written by a script: {@code /dev edit
 * <template>} builds one before the player with a structure block set to save it, and its Save button writes the
 * template into the project's {@code src/main/resources} (passed by Gradle as {@code cassonade.resources}) instead of
 * the world's {@code generated} folder, which would hide the project's copy in that world for good.
 * <p>
 * On saving, a container that had a loot table in the project's template, at the same place, and has lost it gets it
 * back, its items dropped: looking into a keg in creative rolls its loot, which a template must not keep. One placed anew has none, unless picked with its data
 * (Ctrl and the pick block button on another keg). A template whose jigsaw was broken is not saved, as the piece could
 * no longer join its street. A zombie village's copy ({@code <biome>/zombie/houses/…}, the same building) is written
 * along, so the two never drift apart.
 */
public final class TemplateEditing {
	private static final String JIGSAW = "minecraft:jigsaw";
	private static final String LOOT_TABLE = "LootTable";
	private static final SimpleCommandExceptionType NO_RESOURCES = new SimpleCommandExceptionType(
			Component.literal("Only from ./gradlew runClient, which says where the project's resources are"));
	private static final DynamicCommandExceptionType NOT_EDITABLE = new DynamicCommandExceptionType(
			name -> Component.literal(name + " is not one of the mod's templates"));

	private TemplateEditing() {
	}

	/** Read on each use rather than once, so a test can point it at a copy of the templates. */
	private static String resources() {
		return System.getProperty("cassonade.resources");
	}

	/** The templates the mod has, but the zombie copies, which follow their village's. */
	public static List<Identifier> editable() {
		if (resources() == null) {
			return List.of();
		}
		List<Identifier> ids = new ArrayList<>();
		for (Path data : dataRoots()) {
			try (Stream<Path> files = Files.walk(data)) {
				files.filter(file -> file.toString().endsWith(".nbt")).forEach(file -> {
					Path relative = data.relativize(file);
					// data/<namespace>/structure/<path>.nbt
					if (relative.getNameCount() > 2 && relative.getName(1).toString().equals("structure")) {
						String path = relative.subpath(2, relative.getNameCount()).toString().replace('\\', '/');
						Identifier id = Identifier.fromNamespaceAndPath(relative.getName(0).toString(), path.substring(0, path.length() - 4));
						if (!id.getPath().contains("/zombie/")) {
							ids.add(id);
						}
					}
				});
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		}
		ids.sort(null);
		return ids;
	}

	/** The data folder, then those of the settings' overlays (setting_<id>/data), which hold the taverns and lamp posts. */
	private static List<Path> dataRoots() {
		List<Path> roots = new ArrayList<>();
		roots.add(Path.of(resources(), "data"));
		try (Stream<Path> overlays = Files.list(Path.of(resources()))) {
			overlays.filter(dir -> dir.getFileName().toString().startsWith("setting_")).map(dir -> dir.resolve("data"))
					.filter(Files::isDirectory).sorted().forEach(roots::add);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
		return roots;
	}

	/** Where the project keeps the template, or null when it is not one of the mod's. */
	private static Path file(Identifier id) {
		if (resources() == null) {
			return null;
		}
		for (Path data : dataRoots()) {
			Path file = data.resolve(id.getNamespace()).resolve("structure").resolve(id.getPath() + ".nbt");
			if (Files.isRegularFile(file)) {
				return file;
			}
		}
		return null;
	}

	/** The same building in a zombie village, when the project has one. */
	private static Identifier zombieCopy(Identifier id) {
		String path = id.getPath();
		int houses = path.indexOf("/houses/");
		if (houses < 0 || path.contains("/zombie/")) {
			return null;
		}
		Identifier copy = id.withPath(path.substring(0, houses) + "/zombie" + path.substring(houses));
		return file(copy) != null ? copy : null;
	}

	/** Builds the template a few blocks before the player, its structure block on the corner nearest them. */
	public static int edit(CommandSourceStack source, Identifier id) throws CommandSyntaxException {
		if (resources() == null) {
			throw NO_RESOURCES.create();
		}
		Path file = file(id);
		if (file == null || id.getPath().contains("/zombie/")) {
			throw NOT_EDITABLE.create(id.toString());
		}
		ServerPlayer player = source.getPlayerOrException();
		StructureTemplate template = read(source, file);
		Vec3i size = template.getSize();
		// Its middle a few blocks ahead, on the player's level: the template's bottom layer is its foundation
		Direction facing = player.getDirection();
		BlockPos center = player.blockPosition().relative(facing, 6 + Math.max(size.getX(), size.getZ()) / 2);
		BlockPos origin = center.offset(-size.getX() / 2, 0, -size.getZ() / 2);
		BlockPos corner = origin.offset(facing == Direction.WEST ? size.getX() : -1, 0, facing == Direction.NORTH ? size.getZ() : -1);
		build(source.getLevel(), id, template, origin, corner);
		source.sendSuccess(() -> Component.literal("Built " + id + ": change it, then press Save in its structure block to write it to "
				+ Path.of(resources()).relativize(file).toString()), false);
		return 1;
	}

	/**
	 * Builds every template in a row going east from the player, their entrances (west, x = 0) on the player's side,
	 * each one's structure block at its north-west corner.
	 */
	public static int editAll(CommandSourceStack source) throws CommandSyntaxException {
		if (resources() == null) {
			throw NO_RESOURCES.create();
		}
		ServerPlayer player = source.getPlayerOrException();
		BlockPos origin = player.blockPosition().offset(6, 0, 0);
		List<Identifier> ids = editable();
		for (Identifier id : ids) {
			StructureTemplate template = read(source, file(id));
			build(source.getLevel(), id, template, origin, origin.offset(-1, 0, -1));
			origin = origin.offset(0, 0, template.getSize().getZ() + 6);
		}
		source.sendSuccess(() -> Component.literal("Built " + ids.size() + " templates: change them, then press Save in each one's structure block"), false);
		return ids.size();
	}

	/** The template as the project has it, rather than the game's resources (copied at launch, so stale after a save). */
	private static StructureTemplate read(CommandSourceStack source, Path file) {
		StructureTemplate template = new StructureTemplate();
		try {
			CompoundTag tag = NbtIo.readCompressed(file, NbtAccounter.unlimitedHeap());
			template.load(BuiltInRegistries.BLOCK, DataFixTypes.STRUCTURE.updateToCurrentVersion(
					source.getServer().getFixerUpper(), tag, NbtUtils.getDataVersion(tag, 500)));
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
		return template;
	}

	/**
	 * Builds the template at origin, its cells left to the terrain filled with structure void so they stay so once
	 * saved, and a structure block at corner, outside it, set to save it whole.
	 */
	private static void build(ServerLevel level, Identifier id, StructureTemplate template, BlockPos origin, BlockPos corner) {
		Vec3i size = template.getSize();
		BlockPos.betweenClosed(origin, origin.offset(size.getX() - 1, size.getY() - 1, size.getZ() - 1))
				.forEach(pos -> level.setBlock(pos, Blocks.STRUCTURE_VOID.defaultBlockState(), 2));
		template.placeInWorld(level, origin, origin, new StructurePlaceSettings(), RandomSource.create(), 2);
		level.setBlock(corner, Blocks.STRUCTURE_BLOCK.defaultBlockState(), 3);
		if (level.getBlockEntity(corner) instanceof StructureBlockEntity block) {
			block.setMode(StructureMode.SAVE);
			block.setStructureName(id);
			block.setStructurePos(origin.subtract(corner));
			block.setStructureSize(size);
			block.setShowBoundingBox(true);
			block.setChanged();
			level.sendBlockUpdated(corner, block.getBlockState(), block.getBlockState(), 3);
		}
	}

	/**
	 * Saves the template the structure block just filled from the world into the project, or returns null when it is
	 * not one of the mod's so the game saves it as usual. False when it may not be saved.
	 */
	public static Boolean save(ServerLevel level, Identifier id, StructureTemplate template) {
		Path file = file(id);
		if (file == null) {
			return null;
		}
		CompoundTag tag = template.save(new CompoundTag());
		ListTag palette = tag.getListOrEmpty(StructureTemplate.PALETTE_TAG);
		boolean jigsaw = false;
		for (int i = 0; i < palette.size(); i++) {
			jigsaw |= palette.getCompoundOrEmpty(i).getStringOr("id", "").equals(JIGSAW);
		}
		CompoundTag before;
		try {
			before = NbtIo.readCompressed(file, NbtAccounter.unlimitedHeap());
		} catch (IOException e) {
			tell(level, "Not saved: " + e.getMessage());
			return false;
		}
		if (!jigsaw && hasJigsaw(before)) {
			tell(level, "Not saved: " + id + " lost its jigsaw, it could no longer join its street");
			return false;
		}

		// Each container's loot table in the project's template, by its place
		Map<BlockPos, String> loot = new HashMap<>();
		for (Tag entry : before.getListOrEmpty(StructureTemplate.BLOCKS_TAG)) {
			CompoundTag block = (CompoundTag) entry;
			block.getCompound(StructureTemplate.BLOCK_TAG_NBT).flatMap(nbt -> nbt.getString(LOOT_TABLE))
					.ifPresent(table -> loot.put(position(block), table));
		}
		int kegs = 0;
		int restored = 0;
		for (Tag entry : tag.getListOrEmpty(StructureTemplate.BLOCKS_TAG)) {
			CompoundTag block = (CompoundTag) entry;
			CompoundTag nbt = block.getCompound(StructureTemplate.BLOCK_TAG_NBT).orElse(null);
			if (nbt == null) {
				continue;
			}
			String lost = loot.get(position(block));
			if (nbt.getStringOr(LOOT_TABLE, "").isEmpty() && lost != null) {
				nbt.remove("Items");
				nbt.putString(LOOT_TABLE, lost);
				restored++;
			}
			kegs += nbt.contains(LOOT_TABLE) ? 1 : 0;
		}
		// The world's copy changes too, so villages made in this session have their loot
		template.load(BuiltInRegistries.BLOCK, tag);

		List<Path> written = new ArrayList<>(List.of(file));
		Identifier copy = zombieCopy(id);
		if (copy != null) {
			written.add(file(copy));
			level.getStructureTemplateManager().getOrCreate(copy).load(BuiltInRegistries.BLOCK, tag);
		}
		try {
			for (Path path : written) {
				NbtIo.writeCompressed(tag, path);
			}
		} catch (IOException e) {
			tell(level, "Not saved: " + e.getMessage());
			return false;
		}
		Path root = Path.of(resources());
		String kegsSaid = ", " + kegs + " containers with loot" + (restored == 0 ? "" : " (" + restored + " given back the loot they had)");
		tell(level, "Written to " + String.join(" and ", written.stream().map(path -> root.relativize(path).toString()).toList()) + kegsSaid);
		return true;
	}

	private static BlockPos position(CompoundTag block) {
		int[] pos = block.getListOrEmpty(StructureTemplate.BLOCK_TAG_POS).stream().mapToInt(tag -> tag.asInt().orElse(0)).toArray();
		return new BlockPos(pos[0], pos[1], pos[2]);
	}

	private static boolean hasJigsaw(CompoundTag tag) {
		for (Tag entry : tag.getListOrEmpty(StructureTemplate.PALETTE_TAG)) {
			CompoundTag state = (CompoundTag) entry;
			// "Name" in templates older than the block state's "id"
			if (state.getStringOr("id", state.getStringOr("Name", "")).equals(JIGSAW)) {
				return true;
			}
		}
		return false;
	}

	private static void tell(ServerLevel level, String message) {
		level.getServer().getPlayerList().broadcastSystemMessage(Component.literal(message), false);
	}
}
