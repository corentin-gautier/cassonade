package com.cassonade.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.client.gui.components.toasts.AdvancementToast;

@Mixin(AdvancementToast.class)
public interface AdvancementToastAccessor {
	@Accessor
	AdvancementHolder getAdvancement();
}
