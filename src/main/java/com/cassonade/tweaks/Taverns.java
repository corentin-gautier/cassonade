package com.cassonade.tweaks;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.PiecesContainer;

/**
 * The villages' taverns, saved over the churches' templates (edited in the game with /dev edit). Their kegs are barrels, each
 * a fisherman's workstation for the game: they are taken off the villagers' workstations once generated, so a tavern
 * does not turn the village's jobless into a dozen fishermen. A barrel a player places there later is one again.
 * In a zombie village, whose processors already take the doors, cobweb the walls and dirty the windows, the tavern
 * is left cold as well: the fire out, the candles out, the lanterns fallen from their chains.
 */
public final class Taverns {
	private Taverns() {
	}

	/** Whether the piece is a tavern: its template is one of the churches'. */
	public static boolean is(StructurePiece piece) {
		return piece instanceof PoolElementStructurePiece house && house.getElement().toString().contains("_temple_");
	}

	/** Whether the village is a zombie one, built from the pools of its biome's zombie houses and streets. */
	public static boolean abandoned(PiecesContainer pieces) {
		return pieces.pieces().stream().anyMatch(piece -> piece instanceof PoolElementStructurePiece house && house.getElement().toString().contains("/zombie/"));
	}

	/**
	 * Takes the kegs in the area off the workstations. Placing them registered them, for after the generation step
	 * (ServerLevel.updatePOIOnBlockStateChange): this is queued after it, then saved with the rest of the points.
	 */
	public static void keepKegs(WorldGenLevel level, BoundingBox area) {
		List<BlockPos> kegs = new ArrayList<>();
		for (BlockPos pos : BlockPos.betweenClosed(area.minX(), area.minY(), area.minZ(), area.maxX(), area.maxY(), area.maxZ())) {
			if (level.getBlockState(pos).is(Blocks.BARREL)) {
				kegs.add(pos.immutable());
			}
		}
		if (kegs.isEmpty()) {
			return;
		}
		ServerLevel server = level.getLevel();
		server.getServer().execute(() -> {
			for (BlockPos keg : kegs) {
				if (server.getPoiManager().existsAtPosition(PoiTypes.FISHERMAN, keg)) {
					server.getPoiManager().remove(keg);
				}
			}
		});
	}

	/** Puts out what burns in the part of an abandoned tavern within the area. */
	public static void abandon(WorldGenLevel level, BoundingBox area) {
		for (BlockPos pos : BlockPos.betweenClosed(area.minX(), area.minY(), area.minZ(), area.maxX(), area.maxY(), area.maxZ())) {
			BlockState state = level.getBlockState(pos);
			if (state.is(Blocks.LANTERN)) {
				level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
			} else if ((state.is(Blocks.CAMPFIRE) || state.is(Blocks.CANDLE)) && state.getValue(BlockStateProperties.LIT)) {
				level.setBlock(pos, state.setValue(BlockStateProperties.LIT, false), Block.UPDATE_CLIENTS);
			}
		}
	}
}
