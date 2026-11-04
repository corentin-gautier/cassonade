package com.cassonade.client.mixin;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.client.renderer.chunk.SectionCopy;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.PalettedContainer;

/** A copied section's blocks, null when it holds only air. */
@Mixin(SectionCopy.class)
public interface SectionCopyAccessor {
	@Accessor("section")
	@Nullable PalettedContainer<BlockState> cassonade$section();
}
