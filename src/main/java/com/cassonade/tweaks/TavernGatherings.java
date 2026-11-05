package com.cassonade.tweaks;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.WeakHashMap;

import org.apache.commons.lang3.mutable.MutableLong;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.StructureTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.behavior.EntityTracker;
import net.minecraft.world.entity.ai.behavior.OneShot;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.NearestVisibleLivingEntities;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.schedule.Activity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * In the evening, when vanilla villagers gather at their village's bell (the MEET activity), they gather in its tavern
 * instead while its fire burns, and the tavern fills with sips and murmurs. The bell stays their meeting
 * point, its point of interest and memory untouched: the behaviors of the meet package that read it are wrapped
 * (VillagerGoalPackagesMixin), so a tavern with its fire out, a church of a village made before the mod, or a bell
 * outside any village leaves them at the bell as in vanilla.
 */
public final class TavernGatherings {
	/** Ticks a bell's tavern is trusted before being looked at again: its fire may have been put out or lit since. */
	private static final int RECHECK_TICKS = 200;
	/** Ticks a tavern stays lively after the last villager asked for it, so its sounds stop once the gathering ends. */
	private static final int LIVELY_TICKS = 40;
	/** Ticks a villager who could not reach the tavern meets at the bell before trying again. */
	private static final int RETRY_TICKS = 600;
	/** Spots of floor a tavern needs at least, so a church that happens to hold a lit campfire is not taken for one. */
	private static final int MIN_FLOOR = 8;

	/** A tavern's hall: its piece's box, its lit fire, and the floor around it, where the villagers stand. */
	public record Hall(BoundingBox box, BlockPos fire, List<BlockPos> floor) {
		/** Whether the villager is inside, on the floor or a seat, under the roof. */
		public boolean holds(ServerLevel level, LivingEntity body) {
			BlockPos pos = body.blockPosition();
			return this.box.isInside(pos) && Math.abs(pos.getY() - this.fire.getY()) <= 1 && !level.canSeeSky(BlockPos.containing(body.getEyePosition()));
		}

		BlockPos spot(RandomSource random) {
			return this.floor.get(random.nextInt(this.floor.size()));
		}
	}

	private static final class Bell {
		private Hall hall;
		private long checkAt;
		private long askedAt;
	}

	private static final Map<ServerLevel, Map<BlockPos, Bell>> BELLS = new WeakHashMap<>();

	private TavernGatherings() {
	}

	public static void init() {
		ServerTickEvents.END_LEVEL_TICK.register(TavernGatherings::liven);
	}

	/** The tavern where the villager gathers instead of its bell, or null to stay at the bell. */
	private static Hall hall(ServerLevel level, LivingEntity body) {
		Optional<GlobalPos> point = body.getBrain().getMemory(MemoryModuleType.MEETING_POINT);
		if (point.isEmpty() || point.get().dimension() != level.dimension()) {
			return null;
		}
		Bell bell = BELLS.computeIfAbsent(level, key -> new HashMap<>()).computeIfAbsent(point.get().pos(), key -> new Bell());
		long now = level.getGameTime();
		if (now >= bell.checkAt) {
			bell.hall = find(level, point.get().pos());
			bell.checkAt = now + RECHECK_TICKS;
		}
		bell.askedAt = now;
		return bell.hall;
	}

	/** The tavern of the bell's village nearest the bell, with its fire lit and all of it loaded. */
	public static Hall find(ServerLevel level, BlockPos bell) {
		StructureStart village = level.structureManager().getStructureWithPieceAt(bell, StructureTags.VILLAGE);
		if (!village.isValid()) {
			return null;
		}
		Hall nearest = null;
		for (StructurePiece piece : village.getPieces()) {
			BoundingBox box = piece.getBoundingBox();
			if (!Taverns.is(piece) || !loaded(level, box)) {
				continue;
			}
			Hall hall = hall(level, box);
			if (hall != null && (nearest == null || hall.fire.distSqr(bell) < nearest.fire.distSqr(bell))) {
				nearest = hall;
			}
		}
		return nearest;
	}

	private static boolean loaded(ServerLevel level, BoundingBox box) {
		for (int x = SectionPos.blockToSectionCoord(box.minX()); x <= SectionPos.blockToSectionCoord(box.maxX()); x++) {
			for (int z = SectionPos.blockToSectionCoord(box.minZ()); z <= SectionPos.blockToSectionCoord(box.maxZ()); z++) {
				if (!level.hasChunk(x, z)) {
					return false;
				}
			}
		}
		return true;
	}

	/** The hall of a tavern's piece: its lit campfire, and the floor at its level under the roof. */
	private static Hall hall(ServerLevel level, BoundingBox box) {
		BlockPos fire = null;
		for (BlockPos pos : BlockPos.betweenClosed(box.minX(), box.minY(), box.minZ(), box.maxX(), box.maxY(), box.maxZ())) {
			BlockState state = level.getBlockState(pos);
			if (state.is(Blocks.CAMPFIRE) && state.getValue(BlockStateProperties.LIT)) {
				fire = pos.immutable();
				break;
			}
		}
		if (fire == null) {
			return null;
		}
		List<BlockPos> floor = new ArrayList<>();
		for (BlockPos pos : BlockPos.betweenClosed(box.minX(), fire.getY(), box.minZ(), box.maxX(), fire.getY(), box.maxZ())) {
			BlockPos below = pos.below();
			if (level.getBlockState(pos).getCollisionShape(level, pos).isEmpty() && level.getFluidState(pos).isEmpty()
					&& level.getBlockState(pos.above()).getCollisionShape(level, pos.above()).isEmpty()
					&& level.getBlockState(below).isFaceSturdy(level, below, Direction.UP) && !level.canSeeSky(pos)) {
				floor.add(pos.immutable());
			}
		}
		return floor.size() >= MIN_FLOOR ? new Hall(box, fire, floor) : null;
	}

	/**
	 * Replaces the walk to the bell: to a spot of the tavern's floor, in steps when it is far, as vanilla walks to the bell.
	 * A villager who could not reach the tavern goes to the bell for a while, as vanilla would; one whose last walk
	 * elsewhere failed still heads for the tavern, the game keeping that failure until a path is found.
	 */
	public static <E extends LivingEntity> OneShot<E> walk(OneShot<E> toBell, float speedModifier) {
		// One behavior per villager's brain: the time it last set off for the tavern, and when it may try again
		MutableLong setOffAt = new MutableLong(Long.MAX_VALUE);
		MutableLong retryAt = new MutableLong(Long.MIN_VALUE);
		return new Wrapped<>(toBell) {
			@Override
			public boolean trigger(ServerLevel level, E body, long timestamp) {
				Brain<?> brain = body.getBrain();
				Hall hall = hall(level, body);
				if (hall == null || !(body instanceof Villager villager)) {
					return toBell.trigger(level, body, timestamp);
				}
				Optional<Long> cantReachSince = brain.getMemory(MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE);
				if (cantReachSince.isPresent() && cantReachSince.get() >= setOffAt.longValue()) {
					// The tavern, not the bell, was out of reach: vanilla would give up the bell over it
					brain.eraseMemory(MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE);
					setOffAt.setValue(Long.MAX_VALUE);
					retryAt.setValue(timestamp + RETRY_TICKS);
				}
				if (timestamp < retryAt.longValue()) {
					return toBell.trigger(level, body, timestamp);
				}
				if (brain.hasMemoryValue(MemoryModuleType.WALK_TARGET)) {
					return false;
				}
				if (hall.holds(level, body)) {
					return true;
				}
				BlockPos spot = hall.spot(level.getRandom());
				if (spot.distManhattan(body.blockPosition()) > 100) {
					Vec3 towards = DefaultRandomPos.getPosTowards(villager, 15, 7, Vec3.atBottomCenterOf(spot), Math.PI / 2);
					if (towards == null) {
						return false;
					}
					brain.setMemory(MemoryModuleType.WALK_TARGET, new WalkTarget(towards, speedModifier, 6));
				} else {
					brain.setMemory(MemoryModuleType.WALK_TARGET, new WalkTarget(spot, speedModifier, 1));
				}
				setOffAt.setValue(timestamp);
				return true;
			}
		};
	}

	/** Replaces the stroll around the bell: from spot to spot of the tavern's floor, at vanilla's pace. */
	public static <E extends LivingEntity> OneShot<E> stroll(OneShot<E> aroundBell) {
		MutableLong nextOkStartTime = new MutableLong(0L);
		return new Wrapped<>(aroundBell) {
			@Override
			public boolean trigger(ServerLevel level, E body, long timestamp) {
				Hall hall = hall(level, body);
				if (hall == null) {
					return aroundBell.trigger(level, body, timestamp);
				}
				if (!hall.holds(level, body)) {
					return false;
				}
				if (timestamp <= nextOkStartTime.longValue()) {
					return true;
				}
				body.getBrain().setMemory(MemoryModuleType.WALK_TARGET, new WalkTarget(hall.spot(level.getRandom()), 0.4F, 0));
				nextOkStartTime.setValue(timestamp + 180L);
				return true;
			}
		};
	}

	/** Replaces the chat at the bell: the same, with a villager inside the tavern. */
	public static <E extends LivingEntity> OneShot<E> socialize(OneShot<E> atBell) {
		return new Wrapped<>(atBell) {
			@Override
			public boolean trigger(ServerLevel level, E body, long timestamp) {
				Hall hall = hall(level, body);
				if (hall == null) {
					return atBell.trigger(level, body, timestamp);
				}
				Brain<?> brain = body.getBrain();
				Optional<NearestVisibleLivingEntities> visible = brain.getMemory(MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES);
				if (visible.isEmpty() || brain.hasMemoryValue(MemoryModuleType.INTERACTION_TARGET) || level.getRandom().nextInt(100) != 0
						|| !hall.holds(level, body)) {
					return false;
				}
				Optional<LivingEntity> other = visible.get().findClosest(mob -> mob.is(EntityTypes.VILLAGER) && mob.distanceToSqr(body) <= 32.0);
				if (other.isEmpty()) {
					return false;
				}
				brain.setMemory(MemoryModuleType.INTERACTION_TARGET, other.get());
				brain.setMemory(MemoryModuleType.LOOK_TARGET, new EntityTracker(other.get(), true));
				brain.setMemory(MemoryModuleType.WALK_TARGET, new WalkTarget(new EntityTracker(other.get(), false), 0.3F, 1));
				return true;
			}
		};
	}

	/** A behavior standing in for a vanilla one, which it falls back to, needing the same memories. */
	private abstract static class Wrapped<E extends LivingEntity> extends OneShot<E> {
		private final OneShot<E> vanilla;

		Wrapped(OneShot<E> vanilla) {
			this.vanilla = vanilla;
		}

		@Override
		public Set<MemoryModuleType<?>> getRequiredMemories() {
			return this.vanilla.getRequiredMemories();
		}
	}

	/** Once a second, the sounds of the taverns where villagers are gathered, two or more: sips and murmurs. */
	private static void liven(ServerLevel level) {
		Map<BlockPos, Bell> bells = BELLS.get(level);
		if (bells == null || level.getGameTime() % 20 != 0) {
			return;
		}
		long now = level.getGameTime();
		List<Hall> lively = new ArrayList<>();
		for (Iterator<Bell> it = bells.values().iterator(); it.hasNext();) {
			Bell bell = it.next();
			if (now - bell.askedAt > RECHECK_TICKS * 5) {
				// Its village unloaded or its villagers gone: looked up again when asked
				it.remove();
			} else if (bell.hall != null && now - bell.askedAt <= LIVELY_TICKS && !lively.contains(bell.hall)) {
				lively.add(bell.hall);
			}
		}
		RandomSource random = level.getRandom();
		for (Hall hall : lively) {
			List<Villager> inside = level.getEntitiesOfClass(Villager.class, AABB.of(hall.box),
					villager -> !villager.isSleeping() && villager.getBrain().isActive(Activity.MEET) && hall.holds(level, villager));
			if (inside.size() < 2) {
				continue;
			}
			float crowd = inside.size();
			if (random.nextFloat() < Math.min(0.2F, crowd * 0.035F)) {
				Villager villager = inside.get(random.nextInt(inside.size()));
				level.playSound(null, villager.getX(), villager.getEyeY(), villager.getZ(), SoundEvents.GENERIC_DRINK, SoundSource.NEUTRAL, 0.4F,
						0.9F + random.nextFloat() * 0.2F);
			}
			if (random.nextFloat() < Math.min(0.6F, crowd * 0.1F)) {
				Villager villager = inside.get(random.nextInt(inside.size()));
				level.playSound(null, villager.getX(), villager.getEyeY(), villager.getZ(), SoundEvents.VILLAGER_AMBIENT, SoundSource.NEUTRAL, 0.35F,
						0.85F + random.nextFloat() * 0.3F);
			}
		}
	}
}
