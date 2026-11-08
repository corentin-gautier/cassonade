package com.cassonade.test;

import java.util.ArrayList;
import java.util.List;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.StructureTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;

import com.cassonade.tweaks.MinersCamps;

/**
 * Miners' camps: the nearest mineshafts of a fixed seed, generated whole, have one or two camps between them, each a
 * barrel with its loot beside a cold campfire, a straw bed across the corridor; a screenshot of one.
 */
public class MinersCampClientGameTest implements FabricClientGameTest {
	private static final int MINESHAFTS = 3;

	@Override
	public void runTest(ClientGameTestContext context) {
		if (TestSelection.skips(this)) {
			return;
		}
		context.getInput().resizeWindow(1920, 1080);
		try (TestSingleplayerContext world = context.worldBuilder().setUseConsistentSettings(false).adjustSettings(state -> {
			state.setSeed("vanillasugar");
			state.setWorldType(state.getNormalPresetList().getFirst());
			state.setAllowCommands(true);
		}).create()) {
			world.getConnection().waitForChunksRender();
			List<BlockPos> camps = world.getServer().computeOnServer(server -> camps(server.overworld()));
			if (camps.isEmpty()) {
				throw new AssertionError("Expected miners' camps in the nearest mineshafts");
			}
			BlockPos barrel = camps.getFirst();
			BlockPos eye = world.getServer().computeOnServer(server -> eye(server.overworld(), barrel));
			world.getServer().runCommand("gamemode spectator @a");
			world.getServer().runCommand("effect give @a minecraft:night_vision infinite 0 true");
			world.getServer().runCommand("tp @a " + (eye.getX() + 0.5) + " " + eye.getY() + " " + (eye.getZ() + 0.5) + " facing " + (barrel.getX() + 0.5) + " "
					+ (barrel.getY() - 0.5) + " " + (barrel.getZ() + 0.5));
			context.waitTicks(40);
			world.getConnection().waitForChunksRender();
			context.runOnClient(client -> client.gui.hud.getChat().clearMessages(false));
			Screenshots.take(context, "miners-camp");
		}
	}

	/** The camps' barrels in the nearest mineshafts, each checked: its campfire out beside it, a straw bed across. */
	private static List<BlockPos> camps(ServerLevel level) {
		List<BlockPos> barrels = new ArrayList<>();
		List<BoundingBox> seen = new ArrayList<>();
		BlockPos from = BlockPos.ZERO;
		for (int i = 0; i < MINESHAFTS; i++) {
			BlockPos found = level.findNearestMapStructure(StructureTags.MINESHAFT, from, 100, false);
			if (found == null) {
				break;
			}
			StructureStart start = start(level, found);
			BoundingBox box = start.getBoundingBox();
			int inThis = 0;
			for (int cx = box.minX() >> 4; cx <= box.maxX() >> 4; cx++) {
				for (int cz = box.minZ() >> 4; cz <= box.maxZ() >> 4; cz++) {
					LevelChunk chunk = level.getChunk(cx, cz);
					for (BlockEntity entity : chunk.getBlockEntities().values()) {
						if (entity instanceof BarrelBlockEntity camp && MinersCamps.LOOT.equals(camp.getLootTable())) {
							check(level, camp.getBlockPos());
							barrels.add(camp.getBlockPos());
							inThis++;
						}
					}
				}
			}
			System.out.println("Miners' camps: " + inThis + " in the mineshaft at " + box);
			if (inThis > 2) {
				throw new AssertionError("Expected two camps at most in a mineshaft, found " + inThis);
			}
			seen.add(box);
			// The next one, past this one
			from = new BlockPos(box.maxX() + 200, 0, box.maxZ() + 200);
		}
		List<String> items = new ArrayList<>();
		for (ItemStack stack : level.getServer().reloadableRegistries().getLootTable(MinersCamps.LOOT).getRandomItems(
				new LootParams.Builder(level).withParameter(LootContextParams.ORIGIN, Vec3.ZERO).create(LootContextParamSets.CHEST))) {
			items.add(stack.getCount() + " " + BuiltInRegistries.ITEM.getKey(stack.getItem()));
		}
		if (items.isEmpty()) {
			throw new AssertionError("Expected a camp's barrel to hold something");
		}
		System.out.println("Miners' camps: " + barrels.size() + " in " + seen.size() + " mineshafts, at " + barrels + ", a barrel rolled " + items);
		return barrels;
	}

	private static StructureStart start(ServerLevel level, BlockPos found) {
		LevelChunk chunk = level.getChunk(found.getX() >> 4, found.getZ() >> 4);
		for (Holder<Structure> structure : level.registryAccess().lookupOrThrow(Registries.STRUCTURE).getTagOrEmpty(StructureTags.MINESHAFT)) {
			StructureStart start = chunk.getStartForStructure(structure.value());
			if (start != null && start.isValid()) {
				return start;
			}
		}
		throw new AssertionError("Expected a mineshaft's start in the chunk of " + found);
	}

	/** The campfire, out, next to the barrel, and a straw bed across the corridor from one of them. */
	private static void check(ServerLevel level, BlockPos barrel) {
		for (Direction side : Direction.Plane.HORIZONTAL) {
			BlockPos fire = barrel.relative(side);
			if (level.getBlockState(fire).is(Blocks.CAMPFIRE)) {
				if (level.getBlockState(fire).getValue(CampfireBlock.LIT)) {
					throw new AssertionError("Expected the camp's campfire out at " + fire);
				}
				Direction across = side.getClockWise();
				if (level.getBlockState(fire.relative(across, 2)).is(Blocks.STRAW_BED) || level.getBlockState(fire.relative(across.getOpposite(), 2)).is(Blocks.STRAW_BED)) {
					return;
				}
				throw new AssertionError("Expected the camp's straw bed across the corridor from " + fire);
			}
		}
		throw new AssertionError("Expected the camp's campfire beside its barrel at " + barrel);
	}

	/** Down the corridor past the bed's head, in the middle of the corridor, where the camera sees all of the camp. */
	private static BlockPos eye(ServerLevel level, BlockPos barrel) {
		for (Direction side : Direction.Plane.HORIZONTAL) {
			BlockPos fire = barrel.relative(side);
			if (level.getBlockState(fire).is(Blocks.CAMPFIRE)) {
				Direction across = level.getBlockState(fire.relative(side.getClockWise(), 2)).is(Blocks.STRAW_BED) ? side.getClockWise() : side.getCounterClockWise();
				for (int out = 4; out >= 2; out--) {
					BlockPos spot = fire.relative(side, out).relative(across).above();
					if (level.getBlockState(spot).isAir()) {
						return spot;
					}
				}
			}
		}
		return barrel.above(2);
	}
}
