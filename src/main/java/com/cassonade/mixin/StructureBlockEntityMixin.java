package com.cassonade.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.StructureBlockEntity;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;

import com.cassonade.dev.TemplateEditing;

/**
 * In runClient, a structure block saving one of the mod's templates writes it into the project rather than the
 * world (see {@link TemplateEditing}). Anywhere else, and for any other template, the game saves it as usual.
 */
@Mixin(StructureBlockEntity.class)
public abstract class StructureBlockEntityMixin {
	@WrapOperation(method = "saveStructure(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/resources/Identifier;Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/Vec3i;ZLjava/lang/String;ZLjava/util/List;)Z",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/levelgen/structure/templatesystem/StructureTemplateManager;save(Lnet/minecraft/resources/Identifier;)Z"))
	private static boolean cassonade$intoProject(StructureTemplateManager manager, Identifier id, Operation<Boolean> original,
			@Local(argsOnly = true) ServerLevel level, @Local StructureTemplate template) {
		Boolean saved = TemplateEditing.save(level, id, template);
		return saved != null ? saved : original.call(manager, id);
	}
}
