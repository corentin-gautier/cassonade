package com.cassonade.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.world.level.levelgen.structure.TemplateStructurePiece;

@Mixin(TemplateStructurePiece.class)
public interface TemplateStructurePieceAccessor {
	@Accessor("templateName")
	String cassonade$templateName();
}
