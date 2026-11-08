package com.cassonade.client.ambience;

import org.jspecify.annotations.Nullable;

import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

import com.cassonade.Cassonade;

/** Now and then by day, a robin perched on the top of a tree near the player, where they are heard singing (PerchedBirds). */
public final class Robins {
	/** Within the top of its leaf block, so a hop never takes it over the edge. */
	private static final float ROAM = 0.3F;
	private static final PerchedBirds.Perches TREE_TOPS = new PerchedBirds.Perches() {
		@Override
		public PerchedBirds.@Nullable Perch find(ClientLevel level, int x, int z) {
			BlockPos leaves = treeTop(level, x, z);
			return leaves == null ? null : perchOn(leaves);
		}

		@Override
		public boolean holds(ClientLevel level, PerchedBirds.Perch perch) {
			return level.getBlockState(perch.block()).is(BlockTags.LEAVES);
		}
	};
	private static final PerchedBirds BIRDS = new PerchedBirds(new PerchedBirds.Kind(Cassonade.id("textures/entity/robin.png"), 0.8F, 3, 1, 9, 22, 6,
			TREE_TOPS, Robins::singingHere, AirSounds.MEADOW, 0.3F, 48, 160, 600, 50, 1.3F));

	private Robins() {
	}

	/** Where robins sing: the meadow's and the wood's birdsong by day. */
	private static boolean singingHere(Minecraft minecraft) {
		Identifier songs = AirSounds.birdsAt(minecraft.level, minecraft.player.blockPosition());
		return songs == AirSounds.MEADOW || songs == AirSounds.FOREST_DAY;
	}

	/** The column's highest block, if it is leaves: open sky above, where a robin sings. */
	private static @Nullable BlockPos treeTop(ClientLevel level, int x, int z) {
		BlockPos top = new BlockPos(x, level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z) - 1, z);
		return level.getBlockState(top).is(BlockTags.LEAVES) ? top : null;
	}

	private static PerchedBirds.Perch perchOn(BlockPos leaves) {
		return new PerchedBirds.Perch(Vec3.atBottomCenterOf(leaves.above()), ROAM, leaves);
	}

	public static int count() {
		return BIRDS.count();
	}

	/** How many have flown off and are still in the air; for tests. */
	public static long flying() {
		return BIRDS.flying();
	}

	/** How many are coming in to land; for tests. */
	public static long landing() {
		return BIRDS.landing();
	}

	/** For tests: one coming in to land on this leaf block, from beyond it; false with every way in blocked. */
	public static boolean land(Minecraft minecraft, BlockPos leaves) {
		return BIRDS.land(minecraft, perchOn(leaves));
	}

	/** Where the first one is; for tests. */
	public static @Nullable Vec3 first() {
		return BIRDS.first();
	}

	/** For tests: stops robins coming on their own and sends the others away at once, or lets them come again. */
	public static void setSpawning(boolean spawning) {
		BIRDS.setSpawning(spawning);
	}

	/** For tests: one on this leaf block, facing the player. */
	public static void perch(Minecraft minecraft, BlockPos leaves) {
		BIRDS.perch(minecraft, perchOn(leaves));
	}

	/** One landing on the top of a tree before the player's eyes, to watch it in runClient; false with none in sight. */
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
