package com.cassonade.client.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.blaze3d.vertex.VertexSorting;

import org.spongepowered.asm.mixin.Mixin;

import net.minecraft.client.renderer.SectionBufferBuilderPack;
import net.minecraft.client.renderer.chunk.RenderSectionRegion;
import net.minecraft.client.renderer.chunk.SectionCompiler;
import net.minecraft.core.SectionPos;

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
}
