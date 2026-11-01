package com.cassonade.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import net.minecraft.core.Holder;
import net.minecraft.world.entity.decoration.painting.Painting;
import net.minecraft.world.entity.decoration.painting.PaintingVariant;

@Mixin(Painting.class)
public interface PaintingAccessor {
	@Invoker("setVariant")
	void cassonade$setVariant(Holder<PaintingVariant> variant);
}
