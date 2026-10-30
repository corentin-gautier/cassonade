package com.cassonade.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import it.unimi.dsi.fastutil.ints.IntList;

import net.minecraft.world.inventory.AbstractContainerMenu;

/** The values the client is believed to hold, cleared to send one again after the client changed it on its own. */
@Mixin(AbstractContainerMenu.class)
public interface AbstractContainerMenuAccessor {
	@Accessor("remoteDataSlots")
	IntList cassonade$getRemoteDataSlots();
}
