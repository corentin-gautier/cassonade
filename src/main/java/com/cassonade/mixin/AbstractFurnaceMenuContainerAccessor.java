package com.cassonade.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.world.Container;
import net.minecraft.world.inventory.AbstractFurnaceMenu;

/** The furnace behind an open window: the block entity on the server, a plain container on the client. */
@Mixin(AbstractFurnaceMenu.class)
public interface AbstractFurnaceMenuContainerAccessor {
	@Accessor("container")
	Container cassonade$getContainer();
}
