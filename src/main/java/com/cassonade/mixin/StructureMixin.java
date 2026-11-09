package com.cassonade.mixin;

import java.util.Optional;

import com.mojang.datafixers.util.Either;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePiecesBuilder;

import com.cassonade.tweaks.DryLand;

/**
 * Keeps the mod's buildings that stand on the ground off the water: once a lighthouse's or a lone dwelling's pieces
 * are laid out, it is built only if the ground under them is dry. /locate goes through here too, so it agrees.
 */
@Mixin(Structure.class)
abstract class StructureMixin {
	@Inject(method = "findValidGenerationPoint", at = @At("RETURN"), cancellable = true)
	private void cassonade$dryLand(Structure.GenerationContext context, CallbackInfoReturnable<Optional<Structure.GenerationStub>> cir) {
		Optional<Structure.GenerationStub> stub = cir.getReturnValue();
		if (stub.isEmpty() || !context.registryAccess().lookupOrThrow(Registries.STRUCTURE).wrapAsHolder((Structure) (Object) this).is(DryLand.TAG)) {
			return;
		}
		// The pieces, laid out once: the stub keeps them, so the start built from it does not lay them out again
		StructurePiecesBuilder builder = stub.get().getPiecesBuilder();
		if (builder.isEmpty() || !DryLand.under(context, builder.getBoundingBox())) {
			cir.setReturnValue(Optional.empty());
		} else {
			cir.setReturnValue(Optional.of(new Structure.GenerationStub(stub.get().position(), Either.right(builder))));
		}
	}
}
