package com.cassonade.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import net.minecraft.core.cauldron.CauldronInteraction;
import net.minecraft.world.item.Item;

/** Lets the mod register what a cauldron does with an item, as vanilla does for banners and shulker boxes. */
@Mixin(CauldronInteraction.Dispatcher.class)
public interface CauldronDispatcherAccessor {
	@Invoker("put")
	void cassonade$put(Item item, CauldronInteraction interaction);
}
