package com.cassonade.dev;

import java.util.List;
import java.util.Set;

import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.blocks.BlockStateParser;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * {@code /dev lights}: a closed stone hall going east from the player, every light of the game and the mod in a bay of
 * its own along both walls, to see the colored lights (ColoredLight) side by side. Plain lights first, as the warm
 * light the colored ones stand against. Blocks are set without updating their neighbors, so a lamp or a copper bulb
 * stays lit with no power and the glowcap stays on stone; a lit redstone ore still goes out at its next random tick.
 */
final class LightGallery {
	/** A light, the block set under it in place of the floor (or null), and its height: -1 sunk into the floor, 4 hung from the ceiling. */
	private record Light(String block, String floor, int height) {
		Light(String block) {
			this(block, null, 0);
		}
	}

	private static final List<Light> LIGHTS = List.of(
			new Light("torch"), new Light("lantern"),
			new Light("campfire"), new Light("fire", "netherrack", 0),
			new Light("jack_o_lantern"), new Light("candle[candles=4,lit=true]"),
			new Light("end_rod"), new Light("furnace[lit=true]"),
			new Light("redstone_lamp[lit=true]"), new Light("glowstone"),
			new Light("shroomlight"), new Light("ochre_froglight"),
			new Light("verdant_froglight"), new Light("pearlescent_froglight"),
			new Light("lava", null, -1), new Light("magma_block", null, -1),
			new Light("cave_vines[berries=true]", null, 4), new Light("glow_lichen[down=true]"),
			new Light("soul_torch"), new Light("soul_lantern"),
			new Light("soul_campfire"), new Light("soul_fire", "soul_soil", 0),
			new Light("redstone_torch"), new Light("redstone_ore[lit=true]"),
			new Light("copper_torch"), new Light("copper_lantern"),
			new Light("copper_bulb[lit=true]"), new Light("amethyst_cluster"),
			new Light("crying_obsidian"), new Light("respawn_anchor[charges=4]"),
			new Light("sea_lantern"), new Light("conduit[waterlogged=false]"),
			new Light("beacon"), new Light("sculk_catalyst"),
			new Light("sculk_sensor"), new Light("calibrated_sculk_sensor"),
			new Light("cassonade:glowcap", "mycelium", 0), new Light("cassonade:waystone[half=lower]"));
	/** Bays along each wall, the lights alternating from one wall to the other. */
	private static final int BAYS = (LIGHTS.size() + 1) / 2;

	private LightGallery() {
	}

	static int build(CommandSourceStack source) throws CommandSyntaxException {
		ServerPlayer player = source.getPlayerOrException();
		ServerLevel level = source.getLevel();
		HolderLookup<Block> blocks = level.holderLookup(Registries.BLOCK);
		BlockPos origin = player.blockPosition();
		int length = BAYS * 4 + 3;
		// A closed room, so no sky light gets in by day: floor at -1, ceiling at 5, the player at its west end
		BlockState stone = Blocks.STONE.defaultBlockState();
		BlockState air = Blocks.AIR.defaultBlockState();
		for (BlockPos pos : BlockPos.betweenClosed(origin.offset(-2, -1, -8), origin.offset(length, 5, 8))) {
			boolean shell = pos.getX() == origin.getX() - 2 || pos.getX() == origin.getX() + length
					|| pos.getY() == origin.getY() - 1 || pos.getY() == origin.getY() + 5
					|| Math.abs(pos.getZ() - origin.getZ()) == 8;
			// Walls between the bays, four blocks deep on each side of a corridor seven wide
			int x = pos.getX() - origin.getX();
			boolean bayWall = x >= 2 && (x - 2) % 4 == 0 && Math.abs(pos.getZ() - origin.getZ()) >= 4;
			level.setBlock(pos, shell || bayWall ? stone : air, 2);
		}
		for (int i = 0; i < LIGHTS.size(); i++) {
			Light light = LIGHTS.get(i);
			BlockPos pos = origin.offset(4 + i / 2 * 4, light.height(), i % 2 == 0 ? -6 : 6);
			if (light.floor() != null) {
				level.setBlock(pos.below(), parse(blocks, light.floor()), 2);
			}
			level.setBlock(pos, parse(blocks, light.block()), 2);
			if (light.block().startsWith("cassonade:waystone")) {
				level.setBlock(pos.above(), parse(blocks, "cassonade:waystone[half=upper]"), 2);
			}
		}
		player.teleportTo(level, origin.getX() + 0.5, origin.getY(), origin.getZ() + 0.5, Set.of(), -90, 0, true);
		source.sendSuccess(() -> Component.literal(LIGHTS.size() + " lights along a hall going east, plain ones first"), false);
		return LIGHTS.size();
	}

	private static BlockState parse(HolderLookup<Block> blocks, String state) throws CommandSyntaxException {
		return BlockStateParser.parseForBlock(blocks, state, false).blockState();
	}
}
