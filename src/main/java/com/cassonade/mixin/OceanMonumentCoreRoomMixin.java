package com.cassonade.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;

import com.cassonade.settings.Setting;
import com.cassonade.settings.Settings;
import com.cassonade.treasure.MonumentTreasure;

/** Sets barrels of treasure in the corners of the monument's treasure room, around vanilla's gold blocks. */
@Mixin(targets = "net.minecraft.world.level.levelgen.structure.structures.OceanMonumentPieces$OceanMonumentCoreRoom")
abstract class OceanMonumentCoreRoomMixin extends StructurePiece {
	private OceanMonumentCoreRoomMixin(StructurePieceType type, int genDepth, BoundingBox boundingBox) {
		super(type, genDepth, boundingBox);
	}

	@Inject(method = "postProcess", at = @At("TAIL"))
	private void cassonade$treasure(WorldGenLevel level, StructureManager structureManager, ChunkGenerator generator, RandomSource random, BoundingBox chunkBB, ChunkPos chunkPos, BlockPos referencePos, CallbackInfo ci) {
		if (!Settings.on(Setting.MONUMENT_TREASURE)) {
			return;
		}
		// The room's floor corners, just outside the small steps vanilla lays in each one
		for (int x = 1; x <= 14; x += 13) {
			for (int z = 1; z <= 14; z += 13) {
				BlockPos pos = this.getWorldPos(x, 1, z).immutable();
				// Decided by position, as the room is built chunk by chunk with a random of each chunk's own
				if (chunkBB.isInside(pos) && MonumentTreasure.holdsBarrel(pos)) {
					this.placeBlock(level, Blocks.BARREL.defaultBlockState(), x, 1, z, chunkBB);
					if (level.getBlockEntity(pos) instanceof RandomizableContainerBlockEntity barrel) {
						barrel.setLootTable(MonumentTreasure.LOOT, RandomSource.create(pos.asLong()).nextLong());
					}
				}
			}
		}
	}
}
