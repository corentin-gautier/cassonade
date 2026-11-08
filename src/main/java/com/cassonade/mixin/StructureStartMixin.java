package com.cassonade.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.core.registries.Registries;
import net.minecraft.tags.StructureTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.pieces.PiecesContainer;

import com.cassonade.lighthouse.Lighthouses;
import com.cassonade.tweaks.HouseFoundations;
import com.cassonade.tweaks.LampPosts;
import com.cassonade.tweaks.Markets;
import com.cassonade.tweaks.MinersCamps;
import com.cassonade.tweaks.StoneBricks;
import com.cassonade.tweaks.Taverns;

/**
 * Builds villages of stone bricks rather than cobblestone, once their pieces in this chunk stand, mossy cobblestone of
 * zombie villages included; keeps the taverns' kegs and the markets' barrels from being workstations, and leaves a zombie
 * village's tavern cold and its lamp posts dark; takes away a lamp post a street ran under, and pours the houses'
 * foundations down to the ground. Brings a lighthouse its keeper, and a mineshaft its miners' camps.
 */
@Mixin(StructureStart.class)
abstract class StructureStartMixin {
	@Shadow
	@Final
	private Structure structure;

	@Shadow
	@Final
	private PiecesContainer pieceContainer;

	@Inject(method = "placeInChunk", at = @At("TAIL"))
	private void cassonade$stoneBricks(WorldGenLevel level, StructureManager structureManager, ChunkGenerator generator, RandomSource random, BoundingBox chunkBB, ChunkPos chunkPos, CallbackInfo ci) {
		if (Lighthouses.is(level, this.structure)) {
			BoundingBox box = this.pieceContainer.calculateBoundingBox();
			if (box.intersects(chunkBB)) {
				Lighthouses.furnish(level, new BoundingBox(
						Math.max(box.minX(), chunkBB.minX()), box.minY(), Math.max(box.minZ(), chunkBB.minZ()),
						Math.min(box.maxX(), chunkBB.maxX()), box.maxY(), Math.min(box.maxZ(), chunkBB.maxZ())));
			}
			return;
		}
		if (level.registryAccess().lookupOrThrow(Registries.STRUCTURE).wrapAsHolder(this.structure).is(StructureTags.MINESHAFT)) {
			MinersCamps.place(level, this.pieceContainer, ((StructureStart) (Object) this).getChunkPos(), chunkBB);
			return;
		}
		if (!level.registryAccess().lookupOrThrow(Registries.STRUCTURE).wrapAsHolder(this.structure).is(StructureTags.VILLAGE)) {
			return;
		}
		boolean abandoned = Taverns.abandoned(this.pieceContainer);
		for (StructurePiece piece : this.pieceContainer.pieces()) {
			BoundingBox box = piece.getBoundingBox();
			if (box.intersects(chunkBB)) {
				BoundingBox area = new BoundingBox(
						Math.max(box.minX(), chunkBB.minX()), Math.max(box.minY(), chunkBB.minY()), Math.max(box.minZ(), chunkBB.minZ()),
						Math.min(box.maxX(), chunkBB.maxX()), Math.min(box.maxY(), chunkBB.maxY()), Math.min(box.maxZ(), chunkBB.maxZ()));
				StoneBricks.replace(level, area);
				if (HouseFoundations.is(piece)) {
					HouseFoundations.pour(level, box, area);
				}
				if (Taverns.is(piece) || Markets.is(piece)) {
					Taverns.keepKegs(level, area);
				}
				if (abandoned && (Taverns.is(piece) || LampPosts.is(piece))) {
					Taverns.abandon(level, area);
				}
				if (LampPosts.is(piece)) {
					LampPosts.clearStreet(level, box, area);
				}
			}
		}
	}
}
