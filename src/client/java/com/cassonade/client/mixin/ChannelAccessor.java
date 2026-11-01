package com.cassonade.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import com.mojang.blaze3d.audio.Channel;

@Mixin(Channel.class)
public interface ChannelAccessor {
	@Accessor("source")
	int cassonade$getSource();
}
