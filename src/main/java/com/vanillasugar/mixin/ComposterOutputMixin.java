package com.vanillasugar.mixin;

import org.spongepowered.asm.mixin.Mixin;

import com.vanillasugar.ComposterOutput;

/** Marks the composter's output for HopperBlockEntityMixin, the class being private. */
@Mixin(targets = "net.minecraft.world.level.block.ComposterBlock$OutputContainer")
public abstract class ComposterOutputMixin implements ComposterOutput {
}
