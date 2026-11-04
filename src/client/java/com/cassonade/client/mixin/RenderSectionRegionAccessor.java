package com.cassonade.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.client.renderer.chunk.RenderSectionRegion;
import net.minecraft.client.renderer.chunk.SectionCopy;

/** The sections a mesh is built from, for ColoredLight to look for colored emitters in their palettes. */
@Mixin(RenderSectionRegion.class)
public interface RenderSectionRegionAccessor {
	@Accessor("sections")
	SectionCopy[] cassonade$sections();
}
