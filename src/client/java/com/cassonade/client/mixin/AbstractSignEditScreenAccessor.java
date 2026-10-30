package com.cassonade.client.mixin;

import org.joml.Vector3fc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import net.minecraft.client.gui.screens.inventory.AbstractSignEditScreen;
import net.minecraft.world.level.block.entity.SignBlockEntity;

/** Where the sign is drawn, for the tests clicking on its lines. */
@Mixin(AbstractSignEditScreen.class)
public interface AbstractSignEditScreenAccessor {
	@Accessor("sign")
	SignBlockEntity cassonade$getSign();

	@Invoker("getSignTextScale")
	Vector3fc cassonade$getSignTextScale();

	@Invoker("getSignYOffset")
	float cassonade$getSignYOffset();
}
