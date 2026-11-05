package com.cassonade.test.mixin;

import java.util.Map;
import java.util.Optional;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.client.renderer.PostChain;
import net.minecraft.resources.Identifier;

/** Each post effect loaded so far, empty if it failed, for the tests. */
@Mixin(targets = "net.minecraft.client.renderer.ShaderManager$PostChainCache")
public interface PostChainCacheAccessor {
	@Accessor("postChains")
	Map<Identifier, Optional<PostChain>> cassonade$getPostChains();
}
