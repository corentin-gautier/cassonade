package com.cassonade.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.cassonade.client.spyglass.GlancePanel;

import net.minecraft.client.renderer.LevelEventHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.item.JukeboxSong;

/**
 * Remembers the song each jukebox plays, which the client only learns from the event that starts it, so that
 * looking at the jukebox shows its name (GlancePanel). Works on a vanilla server, which sends the same event.
 */
@Mixin(LevelEventHandler.class)
public abstract class LevelEventHandlerMixin {
	@Inject(method = "playJukeboxSong", at = @At("TAIL"))
	private void cassonade$rememberSong(Holder<JukeboxSong> song, BlockPos pos, CallbackInfo info) {
		GlancePanel.songStarted(pos, song.value().description());
	}

	@Inject(method = "stopJukeboxSong", at = @At("HEAD"))
	private void cassonade$forgetSong(BlockPos pos, CallbackInfo info) {
		GlancePanel.songStopped(pos);
	}
}
