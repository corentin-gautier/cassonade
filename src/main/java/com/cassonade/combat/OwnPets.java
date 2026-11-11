package com.cassonade.combat;

import org.jspecify.annotations.Nullable;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import com.cassonade.settings.Setting;
import com.cassonade.settings.Settings;

/**
 * A melee hit passes through the player's own animal, a dog at their side or their horse, and lands on what stands
 * behind it, if anything. Sneaking still hits it, for the rare time it is meant. The server decides: a horse's owner
 * never reaches the client. On a server without the mod, melee stays vanilla.
 */
public final class OwnPets {
	private OwnPets() {
	}

	public static boolean isSpared(Player player, Entity target) {
		return !player.level().isClientSide() && !player.isShiftKeyDown() && Settings.on(Setting.SPARE_PETS) && isOwn(player, target);
	}

	/** What the hit lands on: the target itself, or for one of the player's own animals what is behind it, null if nothing is. */
	public static @Nullable Entity target(Player player, Entity target) {
		return isSpared(player, target) ? behind(player) : target;
	}

	private static boolean isOwn(Player player, Entity entity) {
		return entity instanceof OwnableEntity animal && animal.getOwner() == player;
	}

	private static @Nullable Entity behind(Player player) {
		double range = player.entityInteractionRange();
		Vec3 from = player.getEyePosition();
		Vec3 direction = player.getViewVector(1);
		Vec3 to = from.add(direction.scale(range));
		AABB area = player.getBoundingBox().expandTowards(direction.scale(range)).inflate(1);
		EntityHitResult hit = ProjectileUtil.getEntityHitResult(player, from, to, area,
				entity -> EntitySelector.CAN_BE_PICKED.test(entity) && !isOwn(player, entity), range * range);
		if (hit == null) {
			return null;
		}
		boolean walled = player.level().clip(new ClipContext(from, hit.getLocation(), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player))
				.getType() != HitResult.Type.MISS;
		return walled ? null : hit.getEntity();
	}
}
