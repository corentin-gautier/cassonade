package com.cassonade.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.client.gui.screens.FaviconTexture;
import net.minecraft.client.gui.screens.worldselection.WorldSelectionList;

@Mixin(WorldSelectionList.WorldListEntry.class)
public interface WorldListEntryAccessor {
	@Accessor
	FaviconTexture getIcon();
}
