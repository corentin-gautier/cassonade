package com.cassonade.client.mixin;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;

@Mixin(AbstractContainerMenu.class)
public interface AbstractContainerMenuAccessor {
	/** Null for the player's inventory, where getType() throws. */
	@Accessor("menuType")
	@Nullable MenuType<?> cassonade$getMenuType();
}
