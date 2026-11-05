package com.cassonade.client.mixin;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.blaze3d.vertex.VertexSorting;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import net.minecraft.client.renderer.SectionBufferBuilderPack;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.BlockQuadOutput;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.chunk.RenderSectionRegion;
import net.minecraft.client.renderer.chunk.SectionCompiler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.block.state.BlockState;

import com.cassonade.client.SwingingDoors;
import com.cassonade.client.light.ColoredLight;

/** Each section's mesh is built with the colored light of its region, see {@link ColoredLight}. */
@Mixin(SectionCompiler.class)
public abstract class SectionCompilerMixin {
	@WrapMethod(method = "compile")
	private SectionCompiler.Results cassonade$coloredLight(SectionPos sectionPos, RenderSectionRegion region, VertexSorting vertexSorting,
			SectionBufferBuilderPack builders, Operation<SectionCompiler.Results> original) {
		ColoredLight.begin(sectionPos, region);
		try {
			return original.call(sectionPos, region, vertexSorting, builders);
		} finally {
			ColoredLight.end();
		}
	}

	/** A door swinging is drawn by {@link SwingingDoors}, not in the section's mesh. */
	@WrapWithCondition(method = "compile", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/renderer/block/ModelBlockRenderer;tesselateBlock(Lnet/minecraft/client/renderer/block/BlockQuadOutput;FFFLnet/minecraft/client/renderer/block/BlockAndTintGetter;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/client/renderer/block/dispatch/BlockStateModel;J)V"))
	private boolean cassonade$swinging(ModelBlockRenderer renderer, BlockQuadOutput output, float x, float y, float z, BlockAndTintGetter level, BlockPos pos,
			BlockState state, BlockStateModel model, long seed) {
		return !SwingingDoors.hidden(pos);
	}
}
