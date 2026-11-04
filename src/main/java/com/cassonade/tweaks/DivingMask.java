package com.cassonade.tweaks;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.FluidState;

import com.cassonade.item.CassonadeItems;

/**
 * The diving mask, worn in place of a helmet and enchanted like one: under water, its glass keeps the view sharp and
 * twice as long (UnderwaterSight), and its snorkel lets a player whose head is up to 2.5 blocks under the surface
 * breathe, as long as the open air is right above it. Diving deeper, or taking it off, the last breath drawn lasts a few
 * seconds: Water Breathing for that long.
 */
public final class DivingMask {
	/** How far above the eyes the snorkel's end reaches. */
	private static final double SNORKEL = 2.5;
	/** The last breath through the snorkel, in ticks. */
	private static final int LAST_BREATH = 100;
	/** The players breathing through their snorkel at the last tick. */
	private static final Set<UUID> SNORKELING = new HashSet<>();

	private DivingMask() {
	}

	public static void init() {
		ServerTickEvents.END_SERVER_TICK.register(DivingMask::tick);
	}

	private static void tick(MinecraftServer server) {
		Set<UUID> online = new HashSet<>();
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			online.add(player.getUUID());
			if (breathes(player)) {
				SNORKELING.add(player.getUUID());
			} else if (SNORKELING.remove(player.getUUID()) && player.isUnderWater()) {
				// No particles: it is only the air taken in, but the icon tells how long it lasts
				player.addEffect(new MobEffectInstance(MobEffects.WATER_BREATHING, LAST_BREATH, 0, false, false, true));
			}
		}
		SNORKELING.retainAll(online);
	}

	public static boolean worn(Entity entity) {
		return entity instanceof LivingEntity living && living.getItemBySlot(EquipmentSlot.HEAD).is(CassonadeItems.DIVING_MASK);
	}

	/** Whether the player breathes through the snorkel: its end is out of the water, in the open air. */
	public static boolean breathes(LivingEntity entity) {
		if (!(entity instanceof Player) || !worn(entity)) {
			return false;
		}
		Level level = entity.level();
		double top = entity.getEyeY() + SNORKEL;
		BlockPos pos = BlockPos.containing(entity.getX(), top, entity.getZ());
		FluidState water = level.getFluidState(pos);
		if (water.is(FluidTags.WATER) && top < pos.getY() + water.getHeight(level, pos)) {
			return false;
		}
		// Not up against a ceiling or a boat's hull
		return level.getBlockState(pos).getCollisionShape(level, pos).isEmpty();
	}
}
