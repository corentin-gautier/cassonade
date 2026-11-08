package com.cassonade.client.ambience;

import org.jspecify.annotations.Nullable;

import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

import com.cassonade.Cassonade;

/**
 * Sparrows around what was built, a base or a village, by day: two or three at a time, now and then, on the tops of fences and walls and on roofs,
 * stairs and slabs open to the sky, chirping, flying off as anyone comes near (PerchedBirds). They never sit on what grows,
 * so a place starts to look lived in once something stands there.
 */
public final class Sparrows {
	/** On a fence's post, or a wall's: room to turn about, not to hop off. */
	private static final float ON_POST = 0.04F;
	private static final float ON_WALL = 0.1F;
	/** On the high half of a stair, or the whole top of a block. */
	private static final float ON_STAIR = 0.12F;
	private static final float ON_TOP = 0.3F;
	/** A low wall without a post comes up this far. */
	private static final double LOW_WALL = 0.875;
	private static final PerchedBirds.Perches BUILT = new PerchedBirds.Perches() {
		@Override
		public PerchedBirds.@Nullable Perch find(ClientLevel level, int x, int z) {
			BlockPos top = new BlockPos(x, level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z) - 1, z);
			return perchOn(level, top);
		}

		@Override
		public boolean holds(ClientLevel level, PerchedBirds.Perch perch) {
			PerchedBirds.Perch now = perchOn(level, perch.block());
			return now != null && now.at().equals(perch.at());
		}
	};
	private static final PerchedBirds BIRDS = new PerchedBirds(new PerchedBirds.Kind(Cassonade.id("textures/entity/sparrow.png"), 0.7F, 3, 4, 6, 20, 1.2F,
			BUILT, minecraft -> Ambience.daytime(minecraft.level) && !minecraft.level.isRaining(), Cassonade.id("sparrow"), 0.25F, 32, 60, 260, 20, 1.5F));

	private Sparrows() {
	}

	/** Where one stands on the block, if it is something built: the top of its post, its stair or its slab. */
	private static PerchedBirds.@Nullable Perch perchOn(ClientLevel level, BlockPos pos) {
		BlockState state = level.getBlockState(pos);
		Vec3 center = Vec3.atBottomCenterOf(pos);
		if (state.is(BlockTags.FENCES)) {
			return new PerchedBirds.Perch(center.add(0, 1, 0), ON_POST, pos);
		}
		if (state.is(BlockTags.WALLS)) {
			return new PerchedBirds.Perch(center.add(0, state.getValue(WallBlock.UP) ? 1 : LOW_WALL, 0), ON_WALL, pos);
		}
		if (state.is(BlockTags.STAIRS)) {
			if (state.getValue(StairBlock.HALF) == Half.TOP) {
				return new PerchedBirds.Perch(center.add(0, 1, 0), ON_TOP, pos);
			}
			// On its high half, the back the stair faces
			Direction back = state.getValue(StairBlock.FACING);
			return new PerchedBirds.Perch(center.add(back.getStepX() * 0.25, 1, back.getStepZ() * 0.25), ON_STAIR, pos);
		}
		if (state.is(BlockTags.SLABS)) {
			return new PerchedBirds.Perch(center.add(0, state.getValue(SlabBlock.TYPE) == SlabType.BOTTOM ? 0.5 : 1, 0), ON_TOP, pos);
		}
		return null;
	}

	public static int count() {
		return BIRDS.count();
	}

	/** How many have flown off and are still in the air; for tests. */
	public static long flying() {
		return BIRDS.flying();
	}

	/** Whether one is inside a block right now; for tests. */
	public static boolean anyInBlock() {
		return BIRDS.anyInBlock();
	}

	/** Where the first one is; for tests. */
	public static @Nullable Vec3 first() {
		return BIRDS.first();
	}

	/** For tests: stops sparrows coming on their own and sends the others away at once, or lets them come again. */
	public static void setSpawning(boolean spawning) {
		BIRDS.setSpawning(spawning);
	}

	/** For tests: one on this block, facing the player; false when it is nothing a sparrow perches on. */
	public static boolean perch(Minecraft minecraft, BlockPos block) {
		PerchedBirds.Perch perch = perchOn(minecraft.level, block);
		if (perch != null) {
			BIRDS.perch(minecraft, perch);
		}
		return perch != null;
	}

	/** One landing on something built before the player's eyes, to watch it in runClient; false with none in sight. */
	static boolean perchInView(Minecraft minecraft) {
		return BIRDS.perchInView(minecraft);
	}

	static void tick(Minecraft minecraft) {
		BIRDS.tick(minecraft);
	}

	static void draw(LevelRenderContext context) {
		BIRDS.draw(context);
	}
}
