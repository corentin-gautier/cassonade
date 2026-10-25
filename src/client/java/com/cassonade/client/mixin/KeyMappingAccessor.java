package com.cassonade.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.client.KeyMapping;

@Mixin(KeyMapping.class)
public interface KeyMappingAccessor {
	@Mutable
	@Accessor("defaultKey")
	void cassonade$setDefaultKey(InputConstants.Key key);

	@Accessor("key")
	InputConstants.Key cassonade$getKey();
}
