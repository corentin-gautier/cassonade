package com.cassonade.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.world.inventory.AbstractFurnaceMenu;
import net.minecraft.world.inventory.ContainerData;

/** The furnace's timers the server syncs: fuel left and its total, cooking time spent and its total. */
@Mixin(AbstractFurnaceMenu.class)
public interface AbstractFurnaceMenuAccessor {
	@Accessor("data")
	ContainerData cassonade$getData();
}
