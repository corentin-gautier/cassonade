package com.cassonade.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.screens.multiplayer.ServerSelectionList;

@Mixin(ServerSelectionList.class)
public interface ServerSelectionListAccessor {
	@Accessor
	JoinMultiplayerScreen getScreen();
}
