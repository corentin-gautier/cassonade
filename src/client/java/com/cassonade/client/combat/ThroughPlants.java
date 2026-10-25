package com.cassonade.client.combat;

import java.util.Optional;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * An attack aimed at grass, a flower or a crop with a mob behind it hits the mob instead of breaking the
 * plant. Only the attack: the crosshair still picks the plant, so using an item on it (harvesting, bone
 * meal, placing a block) stays as in vanilla. The server only checks the attack's range, not what stands
 * in the way, so it works on any server.
 */
public final class ThroughPlants {
	private ThroughPlants() {
	}

	/** The mob behind the aimed block, when the block is one an attack can pass through. */
	public static Optional<EntityHitResult> behind(LocalPlayer player, BlockHitResult aimed) {
		Level level = player.level();
		if (aimed.getType() != HitResult.Type.BLOCK || !passable(level, aimed.getBlockPos())) {
			return Optional.empty();
		}
		double range = player.entityInteractionRange();
		Vec3 from = player.getEyePosition();
		Vec3 direction = player.getViewVector(1);
		Vec3 to = from.add(direction.scale(range));
		AABB area = player.getBoundingBox().expandTowards(direction.scale(range)).inflate(1);
		EntityHitResult hit = ProjectileUtil.getEntityHitResult(player, from, to, area, EntitySelector.CAN_BE_PICKED, range * range);
		if (hit == null) {
			return Optional.empty();
		}
		// Through plants only: a block with collision on the way, a wall or a fence, still stops the attack
		BlockHitResult wall = level.clip(new ClipContext(from, hit.getLocation(), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
		return wall.getType() == HitResult.Type.MISS ? Optional.of(hit) : Optional.empty();
	}

	/** Nothing to bump into and broken at a touch: grass, flowers, crops, saplings, but also torches and redstone dust. */
	private static boolean passable(Level level, BlockPos pos) {
		BlockState state = level.getBlockState(pos);
		return state.getCollisionShape(level, pos).isEmpty() && state.getDestroySpeed(level, pos) == 0;
	}
}
