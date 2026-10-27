package com.cassonade.client.map;

import java.util.ArrayList;
import java.util.List;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import com.cassonade.ServerSupport;
import com.cassonade.client.spyglass.GlancePanel;
import com.cassonade.ping.PingPayload;

/**
 * Shift and a middle click points out the spot or the mob looked at, as far as the eye reaches: every player
 * in the dimension sees it marked by a red point for a few seconds, with the name of who pointed, a mob's
 * following it. One ping per player at a time, the newest replacing the last. Needs the mod on the server: on
 * one without it, the middle click picks the block as usual.
 */
public final class Pings {
	private static final int SHOWN = 120;
	private static final float PITCH = 1.6F;
	private static final float VOLUME = 0.4F;

	private static final List<Ping> pings = new ArrayList<>();

	public record Ping(String player, Vec3 pos, int entity, int age) {
		Ping older() {
			return new Ping(player, pos, entity, age + 1);
		}

		/** Where it is: on the mob pointed out while the client knows it, else where it was pointed. */
		public Vec3 position(Minecraft minecraft, float partialTick) {
			Entity pointed = entity == PingPayload.NO_ENTITY ? null : minecraft.level.getEntity(entity);
			return pointed != null ? pointed.getPosition(partialTick).add(0, pointed.getBbHeight(), 0) : pos;
		}
	}

	private Pings() {
	}

	/** Shift held with the pick block key: points out what is looked at. Returns whether the click was taken. */
	public static boolean point(Minecraft minecraft) {
		if (!minecraft.hasShiftDown() || !ServerSupport.onClient() || minecraft.player == null || minecraft.player.isSpectator()) {
			return false;
		}
		HitResult looked = GlancePanel.farPick(minecraft, PingPayload.RANGE);
		if (looked instanceof EntityHitResult hit) {
			ClientPlayNetworking.send(new PingPayload.Request(hit.getEntity().position(), hit.getEntity().getId()));
		} else if (looked.getType() != HitResult.Type.MISS) {
			ClientPlayNetworking.send(new PingPayload.Request(looked.getLocation(), PingPayload.NO_ENTITY));
		}
		// Even pointing at the sky: Shift and a middle click never picks a block
		return true;
	}

	public static void receive(Minecraft minecraft, PingPayload payload) {
		pings.removeIf(ping -> ping.player().equals(payload.player()));
		pings.add(new Ping(payload.player(), payload.pos(), payload.entity(), 0));
		minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.NOTE_BLOCK_BELL.value(), PITCH, VOLUME));
	}

	public static void tick(Minecraft minecraft) {
		if (minecraft.player == null) {
			pings.clear();
			return;
		}
		pings.replaceAll(Ping::older);
		pings.removeIf(ping -> ping.age() > SHOWN);
	}

	public static List<Ping> shown() {
		return pings;
	}
}
