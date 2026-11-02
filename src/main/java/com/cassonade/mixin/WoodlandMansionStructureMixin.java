package com.cassonade.mixin;

import java.util.List;
import java.util.Set;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.PiecesContainer;
import net.minecraft.world.level.levelgen.structure.structures.WoodlandMansionStructure;

import com.cassonade.tweaks.MansionDecor;

/** Furnishes the mansion once all of it stands in this chunk: while a piece is placed, the floor above it doesn't exist yet. */
@Mixin(WoodlandMansionStructure.class)
abstract class WoodlandMansionStructureMixin {
	@Inject(method = "afterPlace", at = @At("TAIL"))
	private void cassonade$furnish(WorldGenLevel level, StructureManager structureManager, ChunkGenerator generator, RandomSource random, BoundingBox chunkBB, ChunkPos chunkPos, PiecesContainer pieces, CallbackInfo ci) {
		List<StructurePiece> rooms = pieces.pieces().stream().filter(piece -> cassonade$name(piece).matches("[12]x[12]_.*")).toList();
		Set<Integer> templateRooms = MansionDecor.templateRooms(rooms.size(), pieces.calculateBoundingBox());
		for (StructurePiece piece : pieces.pieces()) {
			BoundingBox box = piece.getBoundingBox();
			String name = cassonade$name(piece);
			// Rooms are the templates named after their size, like 1x2_a1; a corridor's piece stops at its carpet, so one block is added above
			boolean corridor = name.equals("corridor_floor") || name.startsWith("carpet_");
			int maxY = corridor ? box.maxY() + 1 : box.maxY();
			if (box.minX() <= chunkBB.maxX() && box.maxX() >= chunkBB.minX() && box.minZ() <= chunkBB.maxZ() && box.maxZ() >= chunkBB.minZ()) {
				BoundingBox area = new BoundingBox(
						Math.max(box.minX(), chunkBB.minX()), Math.max(box.minY(), chunkBB.minY()), Math.max(box.minZ(), chunkBB.minZ()),
						Math.min(box.maxX(), chunkBB.maxX()), Math.min(maxY, chunkBB.maxY()), Math.min(box.maxZ(), chunkBB.maxZ()));
				if (templateRooms.contains(rooms.indexOf(piece)) && chunkBB.isInside(new BlockPos(box.getCenter().getX(), chunkBB.minY(), box.getCenter().getZ()))) {
					MansionDecor.placeTemplateBarrel(level, area);
				}
				MansionDecor.furnish(level, area, rooms.contains(piece), corridor);
			}
		}
	}

	@Unique
	private static String cassonade$name(StructurePiece piece) {
		return ((TemplateStructurePieceAccessor) piece).cassonade$templateName();
	}
}
