package com.cassonade.test;

import static com.cassonade.test.PlannedWorld.check;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

import net.minecraft.client.sounds.SoundEventListener;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.levelgen.Heightmap;

import com.cassonade.client.ambience.AirSounds;
import com.cassonade.client.ambience.CaveEcho;
import com.cassonade.client.ambience.Shelter;

/** How the place changes what is heard: the rain under a roof, a cave's echo, the thunder from afar, a wolf on a full moon. */
public class SoundPlacesClientGameTest implements FabricClientGameTest {
	@Override
	public void runTest(ClientGameTestContext context) {
		if (TestSelection.skips(this)) {
			return;
		}
		try (TestSingleplayerContext world = PlannedWorld.open(context)) {
			world.getServer().runCommand("gamemode creative @a");
			world.getServer().runCommand("gamerule advance_weather false");

			// A roof over the head and around it shelters from the rain; a single overhang would not
			context.waitFor(client -> Shelter.roofed() == 0, 20 * 5);
			world.getServer().runCommand("fill -2 104 -2 7 104 7 minecraft:stone");
			context.waitFor(client -> Shelter.roofed() == 1, 20 * 5);
			world.getServer().runCommand("fill -2 104 -2 7 104 7 minecraft:air");
			context.waitFor(client -> Shelter.roofed() == 0, 20 * 5);

			// A great hall of rock rings loud and long (each fill under the game's 32768 blocks), a narrow passage a little, the open air not at all
			PlannedWorld.load(context, world, 104, 104, 134, 134);
			world.getServer().runCommand("fill 104 150 104 134 172 134 minecraft:stone");
			// Carved once the rock is dark, as a real cave is: carved at once, the light left in the air takes long to go
			context.waitTicks(40);
			world.getServer().runCommand("fill 106 152 106 132 170 132 minecraft:air");
			world.getServer().runCommand("tp @a 120.5 152 120.5");
			context.waitFor(client -> client.level.getBrightness(LightLayer.SKY, client.player.blockPosition()) == 0, 20 * 30);
			for (int i = 0; i < 20 * 15 && !context.computeOnClient(client -> CaveEcho.echo() > 0.9F && CaveEcho.size() > 0.9F); i++) {
				context.waitTick();
			}
			check(context.computeOnClient(client -> CaveEcho.echo() > 0.9F && CaveEcho.size() > 0.9F), "a great hall rings: " + context.computeOnClient(client -> {
				BlockPos eye = BlockPos.containing(client.gameRenderer.mainCamera().position());
				return "echo " + CaveEcho.echo() + ", size " + CaveEcho.size() + ", eye " + eye + ", sky light " + client.level.getBrightness(LightLayer.SKY, eye)
						+ ", surface " + client.level.getHeight(Heightmap.Types.WORLD_SURFACE, eye.getX(), eye.getZ());
			}));
			world.getServer().runCommand("fill 106 152 106 132 170 132 minecraft:stone");
			world.getServer().runCommand("fill 107 152 120 131 153 120 minecraft:air");
			context.waitFor(client -> CaveEcho.size() == 0 && Math.abs(CaveEcho.echo() - 0.2F) < 0.01F, 20 * 15);
			world.getServer().runCommand("tp @a 2.5 101 2.5");
			context.waitFor(client -> CaveEcho.echo() == 0, 20 * 15);

			// A bolt 64 blocks away, within the test's small view distance, rumbles over a second and a half after it struck
			List<Identifier> heard = new CopyOnWriteArrayList<>();
			SoundEventListener listener = (sound, event, range) -> heard.add(sound.getIdentifier());
			context.runOnClient(client -> client.getSoundManager().addListener(listener));
			Identifier thunder = SoundEvents.LIGHTNING_BOLT_THUNDER.location();
			world.getServer().runCommand("summon minecraft:lightning_bolt 2.5 101 66.5");
			context.waitTicks(15);
			check(!heard.contains(thunder), "no rumble right after a far bolt");
			context.waitFor(client -> heard.contains(thunder), 20 * 5);

			// Wolves in a taiga on a full moon night, the first night of a world
			world.getServer().runCommand("time set 18000");
			biome(context, world, "taiga");
			check(context.computeOnClient(client -> AirSounds.wolvesAt(client.level, client.player.blockPosition())), "wolves in a taiga on a full moon");
			check(context.computeOnClient(client -> client.getSoundManager().getSoundEvent(AirSounds.WOLF).getWeight() > 0), "the wolf has its sounds");
			context.runOnClient(AirSounds::howl);
			context.waitFor(client -> heard.contains(AirSounds.WOLF), 20);
			biome(context, world, "plains");
			check(!context.computeOnClient(client -> AirSounds.wolvesAt(client.level, client.player.blockPosition())), "no wolves in the plains");
			world.getServer().runCommand("time set 6000");
			biome(context, world, "taiga");
			check(!context.computeOnClient(client -> AirSounds.wolvesAt(client.level, client.player.blockPosition())), "no wolves by day");
			context.runOnClient(client -> client.getSoundManager().removeListener(listener));
		}
	}

	private static void biome(ClientGameTestContext context, TestSingleplayerContext world, String biome) {
		world.getServer().runCommand("execute as @p at @s run fillbiome ~-8 ~-8 ~-8 ~8 ~8 ~8 minecraft:" + biome);
		context.waitFor(client -> client.level.getBiome(client.player.blockPosition()).is(Identifier.withDefaultNamespace(biome)), 20 * 5);
		context.waitTicks(3);
	}
}
