package com.cassonade.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;

/** What was cooked since the experience was last taken, by recipe: reading it must not hand the orbs out. */
@Mixin(AbstractFurnaceBlockEntity.class)
public interface AbstractFurnaceBlockEntityAccessor {
	@Accessor("recipesUsed")
	Reference2IntOpenHashMap<ResourceKey<Recipe<?>>> cassonade$getRecipesUsed();
}
