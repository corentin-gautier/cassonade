package com.cassonade.client.light;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
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
 * fading with the distance, moving in fractions of a block and level rather than from block to block, and
 * lighting up and going out gradually. Torches, campfires and lava go out under water. Client only: the light is
 * added where the game reads it to draw blocks and entities, the world's own light does not change, so
 * mobs still spawn as before.
 */
public final class DynamicLights {
	/** How far from the camera entities carry light. */
	private static final double RANGE = 64;
	/** At most this many lights, the nearest ones. */
	private static final int MAX_SOURCES = 32;
	/** How far a light moves before the blocks are lit again: redrawing them every tick would cost more than it shows. */
	private static final double MOVE_STEP = 0.25;
	/** Levels gained and lost per tick. Lighting up faster than going out keeps a dropped torch from dimming the room. */
	private static final float FADE_IN = 2;
	private static final float FADE_OUT = 1;
	/** The distance around a light that it lights fully, so that on average it reaches as far as a placed block. */
	private static final float LEVEL_PLATEAU = 0.5F;

	/** The lights now, read by the threads building chunk meshes. */
	private static volatile Source[] sources = new Source[0];
	/** The entities' lights, by entity id, kept while they fade out. Client thread only. */
	private static final Map<Integer, Carried> carried = new HashMap<>();
	private static ClientLevel carriedLevel;

	private DynamicLights() {
	}

	/** Moves and fades the lights, and has the sections they lit or now light drawn again when one changed. */
	public static void tick(Minecraft minecraft) {
		ClientLevel level = minecraft.level;
		if (level != carriedLevel) {
			carried.clear();
			sources = new Source[0];
			carriedLevel = level;
		}
		if (level == null) {
			return;
		}
		aim(level, minecraft.gameRenderer.mainCamera().position());
		boolean changed = false;
		for (Iterator<Carried> it = carried.values().iterator(); it.hasNext();) {
			Carried light = it.next();
			light.fade();
			if (light.level == 0) {
				if (light.shown != null) {
					setDirty(level, light.shown);
					changed = true;
				}
				it.remove();
			} else if (light.outdated()) {
				if (light.shown != null) {
					setDirty(level, light.shown);
				}
				light.shown = new Source(light.entity.getX(), light.entity.getEyeY(), light.entity.getZ(), light.level);
				setDirty(level, light.shown);
				changed = true;
			}
		}
		if (changed) {
			sources = carried.values().stream().map(light -> light.shown).toArray(Source[]::new);
		}
	}

	/** Gives each light the level its entity carries now, 0 for those that no longer carry any. */
	private static void aim(ClientLevel level, Vec3 camera) {
		for (Carried light : carried.values()) {
			light.target = 0;
		}
		List<Entity> found = new ArrayList<>();
		for (Entity entity : level.entitiesForRendering()) {
			if (entity.position().distanceToSqr(camera) < RANGE * RANGE && emission(entity) > 0) {
				found.add(entity);
			}
		}
		if (found.size() > MAX_SOURCES) {
			found.sort((a, b) -> Double.compare(a.position().distanceToSqr(camera), b.position().distanceToSqr(camera)));
			found = found.subList(0, MAX_SOURCES);
		}
		for (Entity entity : found) {
			carried.computeIfAbsent(entity.getId(), id -> new Carried(entity)).target = emission(entity);
		}
	}

	private static void setDirty(ClientLevel level, Source source) {
		int reach = Mth.ceil(source.light() + LEVEL_PLATEAU);
		level.setSectionRangeDirty(SectionPos.blockToSectionCoord(Mth.floor(source.x()) - reach), SectionPos.blockToSectionCoord(Mth.floor(source.y()) - reach),
				SectionPos.blockToSectionCoord(Mth.floor(source.z()) - reach), SectionPos.blockToSectionCoord(Mth.floor(source.x()) + reach),
				SectionPos.blockToSectionCoord(Mth.floor(source.y()) + reach), SectionPos.blockToSectionCoord(Mth.floor(source.z()) + reach));
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

	/** The block light carried to a block's center, 0 when none. */
	public static float light(BlockPos pos) {
		return light(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
	}

	/** The block light carried to a point, 0 when none. */
	public static float light(Vec3 pos) {
		return light(pos.x(), pos.y(), pos.z());
	}

	private static float light(double x, double y, double z) {
		float best = 0;
		for (Source source : sources) {
			double dx = x - source.x();
			double dy = y - source.y();
			double dz = z - source.z();
			float light = Math.min(source.light(), source.light() + LEVEL_PLATEAU - (float) Math.sqrt(dx * dx + dy * dy + dz * dz));
			if (light > best) {
				best = light;
			}
		}
		return best;
	}

	/** Packed light coordinates with the carried light at a block, where it is brighter than the block's own. */
	public static int withCarried(int lightCoords, BlockPos pos) {
		return sources.length == 0 ? lightCoords : withCarried(lightCoords, light(pos));
	}

	/** Packed light coordinates with the carried light at a point, where it is brighter than the block's own. */
	public static int withCarried(int lightCoords, Vec3 pos) {
		return sources.length == 0 ? lightCoords : withCarried(lightCoords, light(pos));
	}

	/** Writes fractions of a level: the packed format holds 16 steps per level, which the lightmap's filtering blends. */
	private static int withCarried(int lightCoords, float light) {
		int block = (int) (light * 16);
		return block > LightCoordsUtil.smoothBlock(lightCoords) ? LightCoordsUtil.smoothPack(block, LightCoordsUtil.smoothSky(lightCoords)) : lightCoords;
	}

	/** A light as the blocks see it, immutable since the threads building chunk meshes read it. */
	private record Source(double x, double y, double z, float light) {
	}

	/** An entity's light on its way to the level the entity carries now. */
	private static final class Carried {
		final Entity entity;
		float level;
		int target;
		Source shown;

		Carried(Entity entity) {
			this.entity = entity;
		}

		void fade() {
			level = level < target ? Math.min(level + FADE_IN, target) : Math.max(level - FADE_OUT, target);
		}

		boolean outdated() {
			if (shown == null || shown.light() != level) {
				return true;
			}
			double dx = entity.getX() - shown.x();
			double dy = entity.getEyeY() - shown.y();
			double dz = entity.getZ() - shown.z();
			return dx * dx + dy * dy + dz * dz >= MOVE_STEP * MOVE_STEP;
		}
	}
}
