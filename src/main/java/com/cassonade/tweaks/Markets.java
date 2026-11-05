package com.cassonade.tweaks;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.npc.wanderingtrader.WanderingTrader;
import net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

import com.cassonade.Cassonade;

/**
 * The towns' market squares, each town's meeting point (data/cassonade/structure/town/&lt;biome&gt;/market, edited in the
 * game with /dev edit): stalls around the bell, one left bare. A wandering trader the game sends to that bell settles
 * at the bare stall rather than roaming 16 blocks around it, its place marked in the template by a jigsaw named
 * {@link #TRADER_SPOT} that leads nowhere. Its barrels are kept from being workstations, as the taverns' are.
 */
public final class Markets {
	/** The jigsaw in the bare stall's floor, the trader standing on it. */
	public static final Identifier TRADER_SPOT = Cassonade.id("wandering_trader");
	/** How far from its spot the trader may stroll: the stall, and the step before its counter. */
	private static final int TRADER_RANGE = 2;

	private Markets() {
	}

	/** Whether the piece is a town's market square. */
	public static boolean is(StructurePiece piece) {
		return piece instanceof PoolElementStructurePiece square && square.getElement().toString().contains(":town/")
				&& square.getElement().toString().contains("/market");
	}

	/**
	 * Sends a trader the game sent to a bell to its stall when the bell is a market's, keeping it there. False when the
	 * bell is no market's, the vanilla home left as it is.
	 */
	public static boolean settle(ServerLevel level, WanderingTrader trader, BlockPos bell) {
		BlockPos spot = traderSpot(level, bell);
		if (spot == null) {
			return false;
		}
		trader.setWanderTarget(spot);
		trader.setHomeTo(spot, TRADER_RANGE);
		return true;
	}

	/** Where a trader stands in the market around the bell, or null when the bell is in none. */
	public static BlockPos traderSpot(ServerLevel level, BlockPos bell) {
		StructureStart town = level.structureManager().getStructureWithPieceAt(bell, Towns.TAG);
		if (!town.isValid()) {
			return null;
		}
		for (StructurePiece piece : town.getPieces()) {
			if (is(piece) && piece.getBoundingBox().isInside(bell)) {
				PoolElementStructurePiece square = (PoolElementStructurePiece) piece;
				for (StructureTemplate.JigsawBlockInfo jigsaw : square.getElement().getShuffledJigsawBlocks(level.getStructureTemplateManager(),
						square.getPosition(), square.getRotation(), RandomSource.create(0))) {
					if (TRADER_SPOT.equals(jigsaw.name())) {
						return jigsaw.pos().above();
					}
				}
			}
		}
		return null;
	}
}
