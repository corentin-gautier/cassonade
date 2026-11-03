package com.cassonade.test;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

import net.minecraft.client.sounds.SoundEventListener;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;

import com.cassonade.client.ambience.Birds;
import com.cassonade.client.ambience.Waves;

/** The sound of waves near where the sea meets low ground, clear at its edge and dull from afar; none inland, by a lake, against a cliff or under ice. */
public class WavesClientGameTest implements FabricClientGameTest {
	@Override
	public void runTest(ClientGameTestContext context) {
		if (TestSelection.skips(this)) {
			return;
		}
		context.getInput().resizeWindow(1920, 1080);
		try (TestSingleplayerContext world = context.worldBuilder().create()) {
			world.getConnection().waitForChunksRender();
			context.runOnClient(client -> Birds.setSpawning(false));
			world.getServer().runCommand("gamemode spectator @a");
			check(shore(context) == null, "no shore inland");
			world.getServer().runCommand("execute as @p at @s run fill ~16 ~-3 ~-16 ~32 ~-1 ~16 minecraft:water");
			world.getConnection().waitForClientboundPackets();
			context.waitTicks(10);
			check(shore(context) == null, "no shore by a lake");

			world.getServer().runCommand("execute as @p at @s run fillbiome ~8 -64 ~-16 ~32 -40 ~16 minecraft:ocean");
			context.waitFor(client -> Waves.shore(client.level, client.player.blockPosition()) != null, 20 * 5);
			double distance = context.computeOnClient(client -> Math.sqrt(Waves.shore(client.level, client.player.blockPosition()).distToCenterSqr(client.player.getX(), client.player.getBlockY(), client.player.getZ())));
			check(distance >= 12 && distance < 24, "the nearest shore is at the water's edge (" + distance + ")");

			// At the water's edge both play; farther off, only the roar; and they start with a real sound
			List<Identifier> heard = new CopyOnWriteArrayList<>();
			SoundEventListener listener = (sound, event, range) -> heard.add(sound.getIdentifier());
			context.runOnClient(client -> client.getSoundManager().addListener(listener));
			context.runOnClient(client -> Waves.scanNext());
			context.waitFor(client -> Waves.nearVolume() > 0 && Waves.farVolume() > 0, 20);
			context.waitTicks(2);
			check(heard.contains(Waves.NEAR) && heard.contains(Waves.FAR), "both loops play (" + heard + ")");
			context.runOnClient(client -> client.getSoundManager().removeListener(listener));
			world.getServer().runCommand("execute as @p at @s run tp @a ~-24 ~ ~");
			world.getConnection().waitForClientboundPackets();
			context.waitTicks(2);
			context.runOnClient(client -> Waves.scanNext());
			context.waitFor(client -> Waves.nearVolume() == 0, 20 * 10);
			check(context.computeOnClient(client -> Waves.farVolume()) > 0, "the roar carries farther than the waves");
			world.getServer().runCommand("execute as @p at @s run tp @a ~24 ~ ~");
			world.getConnection().waitForClientboundPackets();

			// Climbing, the waves fade gradually and are gone high in the sky
			context.runOnClient(client -> Waves.scanNext());
			context.waitTicks(20 * 15);
			float full = context.computeOnClient(client -> Waves.farVolume());
			world.getServer().runCommand("execute as @p at @s run tp @a ~ ~16 ~");
			world.getConnection().waitForClientboundPackets();
			context.runOnClient(client -> Waves.scanNext());
			context.waitTicks(20 * 15);
			float halfway = context.computeOnClient(client -> Waves.farVolume());
			check(halfway > 0 && halfway < full, "fainter, not gone, part of the way up (" + halfway + " of " + full + ")");
			world.getServer().runCommand("execute as @p at @s run tp @a ~ ~40 ~");
			world.getConnection().waitForClientboundPackets();
			context.runOnClient(client -> Waves.scanNext());
			context.waitFor(client -> Waves.nearVolume() == 0 && Waves.farVolume() == 0, 20 * 20);
			world.getServer().runCommand("execute as @p at @s run tp @a ~ ~-56 ~");
			world.getConnection().waitForClientboundPackets();

			// A cliff all along the water's edge, then the sea frozen over
			world.getServer().runCommand("execute as @p at @s run fill ~-4 ~-1 ~-32 ~15 ~8 ~32 minecraft:stone");
			world.getConnection().waitForClientboundPackets();
			context.waitTicks(10);
			check(shoreBeside(context) == null, "no surf against a cliff");
			world.getServer().runCommand("execute as @p at @s run fill ~-4 ~-1 ~-32 ~15 ~8 ~32 minecraft:air");
			world.getServer().runCommand("execute as @p at @s run fill ~16 ~-1 ~-16 ~32 ~-1 ~16 minecraft:ice");
			world.getConnection().waitForClientboundPackets();
			context.waitTicks(10);
			check(shore(context) == null, "no surf under ice");
			context.runOnClient(client -> Waves.scanNext());
			context.waitFor(client -> Waves.nearVolume() == 0 && Waves.farVolume() == 0, 20 * 10);
		}
	}

	private static BlockPos shore(ClientGameTestContext context) {
		return context.computeOnClient(client -> Waves.shore(client.level, client.player.blockPosition()));
	}

	/** Only the wall's side of the water: its other edges, on open ground, are still shores. */
	private static BlockPos shoreBeside(ClientGameTestContext context) {
		return context.computeOnClient(client -> {
			BlockPos shore = Waves.shore(client.level, client.player.blockPosition());
			return shore != null && shore.getX() < client.player.getBlockX() + 20 && Math.abs(shore.getZ() - client.player.getBlockZ()) < 8 ? shore : null;
		});
	}

	private static void check(boolean condition, String what) {
		if (!condition) {
			throw new AssertionError(what);
		}
	}
}
