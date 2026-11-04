package com.cassonade.dev;

import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.datafixers.util.Pair;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;

import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.ResourceOrTagArgument;
import net.minecraft.commands.arguments.ResourceOrTagKeyArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Prediction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;

import com.cassonade.gear.GearSlot;
import com.cassonade.gear.PlayerGear;
import com.cassonade.item.CassonadeItems;
import com.cassonade.waystone.Waystones;
import com.cassonade.woodcutter.Woodcutter;

/**
 * {@code /dev}, to try the mod's features in runClient without crafting or travelling first. Registered only in the
 * development environment: players never get it, so its messages stay out of the language files.
 * <ul>
 * <li>{@code kit}: fills every gear slot with its best item and gives what is held to be used;</li>
 * <li>{@code unequip}: empties the gear slots, to see the game without them;</li>
 * <li>{@code goto biome|structure <id>}: finds the nearest, as {@code /locate} does, and stands the player on a floor
 * there, in a cave for a cave biome, in the heart of a structure, buried or not.</li>
 * </ul>
 */
public final class DevCommands {
	private static final DynamicCommandExceptionType NOT_FOUND = new DynamicCommandExceptionType(
			name -> Component.literal("No " + name + " nearby"));
	private static final DynamicCommandExceptionType NO_FLOOR = new DynamicCommandExceptionType(
			name -> Component.literal("Found " + name + ", but nowhere to stand there"));

	/** The best item of each slot, so every feature shows at once. Made on each call: items cannot be made as the mod loads. */
	private static List<Pair<GearSlot, ItemStack>> gear() {
		return List.of(
				Pair.of(GearSlot.COMPASS, new ItemStack(CassonadeItems.SCULK_COMPASS)),
				Pair.of(GearSlot.MAP, new ItemStack(CassonadeItems.WORLD_MAP)),
				Pair.of(GearSlot.CLOCK, new ItemStack(CassonadeItems.BAROMETER)),
				Pair.of(GearSlot.ELYTRA, new ItemStack(Items.ELYTRA)),
				Pair.of(GearSlot.BACKPACK, new ItemStack(CassonadeItems.ORGANIZED_BACKPACK)));
	}

	/** What works from the hand or once placed. */
	private static List<ItemStack> held() {
		return List.of(
				new ItemStack(CassonadeItems.NATURALIST_SPYGLASS),
				new ItemStack(Items.SPYGLASS),
				new ItemStack(CassonadeItems.SCULK_TORCH),
				new ItemStack(Items.FIREWORK_ROCKET, 64),
				new ItemStack(Waystones.ITEM, 2),
				new ItemStack(Woodcutter.ITEM),
				new ItemStack(Items.HOPPER, 4),
				new ItemStack(CassonadeItems.BACKPACK),
				new ItemStack(CassonadeItems.BACKPACK_UPGRADE_TEMPLATE));
	}

	private DevCommands() {
	}

	public static void init() {
		CommandRegistrationCallback.EVENT.register((dispatcher, context, selection) -> register(dispatcher, context));
	}

	private static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext context) {
		dispatcher.register(Commands.literal("dev")
				.requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
				.then(Commands.literal("kit").executes(command -> kit(command.getSource().getPlayerOrException())))
				.then(Commands.literal("unequip").executes(command -> unequip(command.getSource().getPlayerOrException())))
				.then(Commands.literal("goto")
						.then(Commands.literal("biome")
								.then(Commands.argument("biome", ResourceOrTagArgument.resourceOrTag(context, Registries.BIOME))
										.executes(command -> gotoBiome(command.getSource(),
												ResourceOrTagArgument.getResourceOrTag(command, "biome", Registries.BIOME)))))
						.then(Commands.literal("structure")
								.then(Commands.argument("structure", ResourceOrTagKeyArgument.resourceOrTagKey(Registries.STRUCTURE))
										.executes(command -> gotoStructure(command.getSource(),
												ResourceOrTagKeyArgument.getResourceOrTagKey(command, "structure", Registries.STRUCTURE, NOT_FOUND)))))));
	}

	private static int kit(ServerPlayer player) {
		PlayerGear gear = PlayerGear.of(player);
		for (Pair<GearSlot, ItemStack> entry : gear()) {
			// What was worn goes back to the inventory rather than being lost
			giveBack(player, gear.get(entry.getFirst()));
			gear = gear.with(entry.getFirst(), entry.getSecond());
		}
		player.setAttached(PlayerGear.ATTACHMENT, gear);
		for (ItemStack stack : held()) {
			giveBack(player, stack);
		}
		player.sendSystemMessage(Component.literal("Every gear slot filled, tools in the inventory"));
		return 1;
	}

	private static int unequip(ServerPlayer player) {
		PlayerGear gear = PlayerGear.of(player);
		for (GearSlot slot : GearSlot.values()) {
			giveBack(player, gear.get(slot));
		}
		player.setAttached(PlayerGear.ATTACHMENT, PlayerGear.EMPTY);
		player.sendSystemMessage(Component.literal("Gear slots emptied"));
		return 1;
	}

	private static void giveBack(ServerPlayer player, ItemStack stack) {
		if (!stack.isEmpty()) {
			player.getInventory().placeItemBackInInventory(stack, Prediction.SERVER_ONLY);
		}
	}

	private static int gotoBiome(CommandSourceStack source, ResourceOrTagArgument.Result<Biome> biome) throws CommandSyntaxException {
		ServerPlayer player = source.getPlayerOrException();
		ServerLevel level = source.getLevel();
		// The same search as /locate biome
		Pair<BlockPos, Holder<Biome>> found = level.findClosestBiome3d(biome, player.blockPosition(), 6400, 32, 64);
		if (found == null) {
			throw NOT_FOUND.create(biome.asPrintable());
		}
		BlockPos pos = found.getFirst();
		level.getChunk(pos);
		// On the surface when the biome reaches it, otherwise (a cave biome) as near as can be to where it was found
		BlockPos surface = surface(level, pos);
		BlockPos target = surface;
		if (surface == null || !biome.test(level.getBiome(surface))) {
			// The biome was sampled in rough steps: a floor of its own nearby, rather than another cave's
			target = floorNear(level, pos, pos.getY(), feet -> biome.test(level.getBiome(feet)));
			if (target == null) {
				target = floorNear(level, pos, pos.getY(), feet -> true);
			}
		}
		return teleport(player, level, target, biome.asPrintable());
	}

	private static int gotoStructure(CommandSourceStack source, ResourceOrTagKeyArgument.Result<Structure> structure) throws CommandSyntaxException {
		ServerPlayer player = source.getPlayerOrException();
		ServerLevel level = source.getLevel();
		Registry<Structure> registry = level.registryAccess().lookupOrThrow(Registries.STRUCTURE);
		HolderSet<Structure> target = structure.unwrap()
				.map(key -> registry.get(key).<HolderSet<Structure>>map(HolderSet::direct), key -> registry.get(key).<HolderSet<Structure>>map(set -> set))
				.orElseThrow(() -> NOT_FOUND.create(structure.asPrintable()));
		// The same search as /locate structure
		Pair<BlockPos, Holder<Structure>> found = level.getChunkSource().getGenerator().findNearestMapStructure(level, target, player.blockPosition(), 100, false);
		if (found == null) {
			throw NOT_FOUND.create(structure.asPrintable());
		}
		BlockPos pos = found.getFirst();
		// The middle of its first piece, the heart of it (a village's meeting point, an ancient city's center): its
		// whole box can be centered on empty ground between the houses
		StructureStart start = level.getChunk(pos).getStartForStructure(found.getSecond().value());
		if (start != null && start.isValid() && !start.getPieces().isEmpty()) {
			pos = start.getPieces().getFirst().getBoundingBox().getCenter();
		}
		level.getChunk(pos);
		return teleport(player, level, floorNear(level, pos, pos.getY(), feet -> true), structure.asPrintable());
	}

	private static int teleport(ServerPlayer player, ServerLevel level, BlockPos target, String name) throws CommandSyntaxException {
		if (target == null) {
			throw NO_FLOOR.create(name);
		}
		player.teleportTo(level, target.getX() + 0.5, target.getY(), target.getZ() + 0.5, Set.of(), player.getYRot(), player.getXRot(), true);
		player.sendSystemMessage(Component.literal("Went to " + name + " at " + target.getX() + " " + target.getY() + " " + target.getZ()));
		return 1;
	}

	/** The open-air floor of that column, or null under a ceiling (the Nether), where the top is bedrock. */
	private static BlockPos surface(ServerLevel level, BlockPos pos) {
		if (level.dimensionType().hasCeiling()) {
			return null;
		}
		BlockPos top = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, pos);
		return standable(level, top) ? top : null;
	}

	/** The spot to stand on nearest to that height, in that column or, if it has none, in columns further out. */
	private static BlockPos floorNear(ServerLevel level, BlockPos pos, int y, Predicate<BlockPos> wanted) {
		for (int radius = 0; radius <= 32; radius += 4) {
			for (int dx = -radius; dx <= radius; dx += Math.max(radius, 1)) {
				for (int dz = -radius; dz <= radius; dz += Math.max(radius, 1)) {
					BlockPos column = pos.offset(dx, 0, dz);
					level.getChunk(column);
					for (int dy = 0; dy <= 96; dy++) {
						for (int sign : new int[] {-1, 1}) {
							BlockPos feet = new BlockPos(column.getX(), y + sign * dy, column.getZ());
							if (standable(level, feet) && wanted.test(feet)) {
								return feet;
							}
						}
					}
				}
			}
		}
		return null;
	}

	/** Room for the player, out of any fluid, over a block that holds them. */
	private static boolean standable(ServerLevel level, BlockPos feet) {
		if (feet.getY() <= level.getMinY() || feet.getY() + 1 >= level.getMaxY()) {
			return false;
		}
		BlockState below = level.getBlockState(feet.below());
		return !below.getCollisionShape(level, feet.below()).isEmpty() && below.getFluidState().isEmpty()
				&& free(level, feet) && free(level, feet.above());
	}

	private static boolean free(ServerLevel level, BlockPos pos) {
		BlockState state = level.getBlockState(pos);
		return state.getCollisionShape(level, pos).isEmpty() && state.getFluidState().isEmpty();
	}
}
