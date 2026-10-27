package com.cassonade.client.light;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.BaseTorchBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.phys.Vec3;

import com.cassonade.item.CassonadeItems;

/**
 * Light carried around: a torch, lantern or glowstone in hand (the player's, another player's, a mob's),
 * dropped on the ground, or a burning entity, lights the blocks and entities around it as the block would,
 * fading with the distance. Torches, campfires and lava go out under water. Client only: the light is
 * added where the game reads it to draw blocks and entities, the world's own light does not change, so
 * mobs still spawn as before.
 */
public final class DynamicLights {
	/** How far from the camera entities carry light. */
	private static final double RANGE = 64;
	/** At most this many lights, the nearest ones. */
	private static final int MAX_SOURCES = 32;

	/** The lights now, read by the threads building chunk meshes. */
	private static volatile Source[] sources = new Source[0];

	private DynamicLights() {
	}

	/** Gathers the lights, and has the sections they lit or now light drawn again when one moved. */
	public static void tick(Minecraft minecraft) {
		ClientLevel level = minecraft.level;
		if (level == null) {
			sources = new Source[0];
			return;
		}
		Vec3 camera = minecraft.gameRenderer.mainCamera().position();
		List<Source> found = new ArrayList<>();
		for (Entity entity : level.entitiesForRendering()) {
			if (entity.position().distanceToSqr(camera) < RANGE * RANGE) {
				int light = emission(entity);
				if (light > 0) {
					found.add(new Source(BlockPos.containing(entity.getX(), entity.getEyeY(), entity.getZ()), light));
				}
			}
		}
		if (found.size() > MAX_SOURCES) {
			found.sort((a, b) -> Double.compare(a.pos().distToCenterSqr(camera), b.pos().distToCenterSqr(camera)));
			found = found.subList(0, MAX_SOURCES);
		}
		Set<Source> before = Set.of(sources);
		Set<Source> now = new HashSet<>(found);
		if (now.equals(before)) {
			return;
		}
		sources = now.toArray(Source[]::new);
		for (Source source : before) {
			if (!now.contains(source)) {
				setDirty(level, source);
			}
		}
		for (Source source : now) {
			if (!before.contains(source)) {
				setDirty(level, source);
			}
		}
	}

	private static void setDirty(ClientLevel level, Source source) {
		int reach = source.light() - 1;
		BlockPos pos = source.pos();
		level.setSectionRangeDirty(SectionPos.blockToSectionCoord(pos.getX() - reach), SectionPos.blockToSectionCoord(pos.getY() - reach),
				SectionPos.blockToSectionCoord(pos.getZ() - reach), SectionPos.blockToSectionCoord(pos.getX() + reach),
				SectionPos.blockToSectionCoord(pos.getY() + reach), SectionPos.blockToSectionCoord(pos.getZ() + reach));
	}

	/** The light an entity carries: burning, or the brightest of its hands, or the item it is. */
	private static int emission(Entity entity) {
		if (entity.isOnFire()) {
			return 15;
		}
		boolean underWater = entity.isUnderWater();
		if (entity instanceof ItemEntity item) {
			return emission(item.getItem(), underWater);
		}
		if (entity instanceof LivingEntity living && !entity.isSpectator()) {
			return Math.max(emission(living.getMainHandItem(), underWater), emission(living.getOffhandItem(), underWater));
		}
		return 0;
	}

	private static int emission(ItemStack stack, boolean underWater) {
		if (stack.is(Items.LAVA_BUCKET)) {
			return underWater ? 0 : 15;
		}
		// The sculk torch is still a torch
		if (stack.is(CassonadeItems.SCULK_TORCH)) {
			return underWater ? 0 : Blocks.TORCH.defaultBlockState().getLightEmission();
		}
		if (!(stack.getItem() instanceof BlockItem item)) {
			return 0;
		}
		Block block = item.getBlock();
		if (underWater && (block instanceof BaseTorchBlock || block instanceof CampfireBlock)) {
			return 0;
		}
		return block.defaultBlockState().getLightEmission();
	}

	/** The block light carried to a block, 0 when none. */
	public static int light(BlockPos pos) {
		int best = 0;
		for (Source source : sources) {
			BlockPos from = source.pos();
			int dx = pos.getX() - from.getX();
			int dy = pos.getY() - from.getY();
			int dz = pos.getZ() - from.getZ();
			int light = source.light() - (int) Math.sqrt(dx * dx + dy * dy + dz * dz);
			if (light > best) {
				best = light;
			}
		}
		return best;
	}

	/** Packed light coordinates with the carried light, where it is brighter than the block's own. */
	public static int withCarried(int lightCoords, BlockPos pos) {
		if (sources.length == 0) {
			return lightCoords;
		}
		int carried = light(pos);
		return carried > LightCoordsUtil.block(lightCoords) ? LightCoordsUtil.withBlock(lightCoords, carried) : lightCoords;
	}

	private record Source(BlockPos pos, int light) {
	}
}
