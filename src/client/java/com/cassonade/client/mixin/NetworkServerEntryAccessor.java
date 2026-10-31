package com.cassonade.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.client.gui.screens.multiplayer.ServerSelectionList;
import net.minecraft.client.server.LanServer;

@Mixin(ServerSelectionList.NetworkServerEntry.class)
public interface NetworkServerEntryAccessor {
	@Accessor
	LanServer getServerData();
}
