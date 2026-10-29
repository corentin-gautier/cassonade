package com.cassonade.test;

import java.util.List;
import java.util.function.Consumer;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.NoteBlock;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.entity.BeehiveBlockEntity;

import com.cassonade.Cassonade;
import com.cassonade.client.quest.QuestLog;
import com.cassonade.client.quest.QuestOptInScreen;
import com.cassonade.client.spyglass.GlancePanel;
import com.cassonade.client.spyglass.ScopeZoom;

/**
 * The naturalist's spyglass: it zooms like a vanilla spyglass, and its panel reads what the player sees through
 * it, a horse, a villager, an animal, a crop, near or far; a vanilla spyglass reads nothing. Through either, the
 * scroll wheel zooms a step further in or out.
 */
public class SpyglassClientGameTest implements FabricClientGameTest {
	@Override
	public void runTest(ClientGameTestContext context) {
		if (TestSelection.skips(this)) {
			return;
		}
		context.getInput().resizeWindow(1920, 1080);
		try (TestSingleplayerContext world = context.worldBuilder().create()) {
			world.getConnection().waitForChunksRender();
			context.waitForScreen(QuestOptInScreen.class);
			context.runOnClient(client -> QuestLog.setEnabled(false));
			context.setScreen(() -> null);
			world.getServer().runCommand("gamemode survival @a");
			world.getServer().runCommand("time set noon");

			// The recipe: a spyglass, a book and a sculk sensor
			world.getServer().runOnServer(server -> check(server.getRecipeManager()
					.byKey(ResourceKey.create(Registries.RECIPE, Cassonade.id("naturalist_spyglass"))).isPresent(), "the naturalist's spyglass recipe"));
			BlockPos feet = world.getServer().computeOnServer(server -> server.getPlayerList().getPlayers().getFirst().blockPosition());

			// A vanilla spyglass zooms and reads nothing, even a horse
			spawn(world, feet.east(3), level -> EntityTypes.HORSE.create(level, EntitySpawnReason.COMMAND), horse -> {
				var living = (net.minecraft.world.entity.LivingEntity) horse;
				living.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.3);
				living.getAttribute(Attributes.JUMP_STRENGTH).setBaseValue(0.7);
				living.getAttribute(Attributes.MAX_HEALTH).setBaseValue(24);
			});
			world.getServer().runCommand("item replace entity @a weapon.mainhand with spyglass");
			check(lookThrough(context, world, feet, 20).isEmpty(), "a vanilla spyglass reads nothing");

			// The wheel zooms a step further in or out than the game, and leaves the spyglass in hand
			look(world, feet, -60);
			context.waitTicks(3);
			context.getInput().holdKey(options -> options.keyUse);
			context.waitTicks(3);
			int slot = context.computeOnClient(client -> client.player.getInventory().getSelectedSlot());
			// The camera eases into the zoom over a few ticks
			context.waitTicks(10);
			float gameFov = context.computeOnClient(client -> client.gameRenderer.mainCamera().getFov());
			context.getInput().scroll(1);
			context.waitTicks(10);
			float closerFov = context.computeOnClient(client -> client.gameRenderer.mainCamera().getFov());
			check(closerFov < gameFov * 0.6F && context.computeOnClient(client -> client.player.isScoping()
					&& client.player.getInventory().getSelectedSlot() == slot), "the wheel zooms in, the spyglass still in hand (" + gameFov + "° then " + closerFov + "°)");
			for (int i = 0; i < 3; i++) {
				context.getInput().scroll(-1);
				context.waitTicks(1);
			}
			context.waitTicks(10);
			float widerFov = context.computeOnClient(client -> client.gameRenderer.mainCamera().getFov());
			check(context.computeOnClient(client -> ScopeZoom.fieldOfView()) == 0.2F && widerFov > gameFov * 1.6F,
					"the wheel zooms out a step past the game's, no further (" + gameFov + "° then " + widerFov + "°)");
			context.getInput().releaseKey(options -> options.keyUse);
			context.waitTicks(3);
			check(context.computeOnClient(client -> ScopeZoom.fieldOfView()) == 0.1F, "the game's zoom again, the spyglass put down");

			// The naturalist's spyglass zooms the same, and reads what it sees, here east of the player
			world.getServer().runCommand("item replace entity @a weapon.mainhand with cassonade:naturalist_spyglass");
			List<String> horse = lookThrough(context, world, feet, 20);
			check(horse.contains("Speed 12.6 blocks/s") && horse.contains("Health 12.0 hearts"), "a horse's speed and health (" + horse + ")");

			spawn(world, feet.east(2), level -> EntityTypes.VILLAGER.create(level, EntitySpawnReason.COMMAND), villager -> {
				var trader = (net.minecraft.world.entity.npc.villager.Villager) villager;
				trader.setVillagerData(trader.getVillagerData().withProfession(villager.level().registryAccess(), VillagerProfession.LIBRARIAN).withLevel(2));
			});
			List<String> villager = lookThrough(context, world, feet, 20);
			check(villager.contains("Apprentice") && villager.contains("Trades not seen yet"), "a villager's level and its trades, never made (" + villager + ")");

			spawn(world, feet.east(2), level -> EntityTypes.COW.create(level, EntitySpawnReason.COMMAND),
					cow -> ((net.minecraft.world.entity.AgeableMob) cow).setAge(1200));
			List<String> cow = lookThrough(context, world, feet, 20);
			check(cow.stream().anyMatch(line -> line.startsWith("Ready to breed in ")), "a cow resting after breeding (" + cow + ")");

			// Tall grass in front of the cow hides nothing
			world.getServer().runOnServer(server -> DoublePlantBlock.placeAt(server.overworld(), Blocks.TALL_GRASS.defaultBlockState(), feet.east(), Block.UPDATE_ALL));
			List<String> behindGrass = lookThrough(context, world, feet, 20);
			check(behindGrass.contains("Cow"), "the cow read through the tall grass in front of it (" + behindGrass + ")");
			check(context.computeOnClient(client -> client.hitResult instanceof net.minecraft.world.phys.BlockHitResult aimed
					&& client.level.getBlockState(aimed.getBlockPos()).is(Blocks.TALL_GRASS)), "the grass in the way of the look, as the crosshair shows");
			world.getServer().runOnServer(server -> {
				server.overworld().setBlockAndUpdate(feet.east().above(), Blocks.AIR.defaultBlockState());
				server.overworld().setBlockAndUpdate(feet.east(), Blocks.AIR.defaultBlockState());
			});

			// A cow twelve blocks away
			spawn(world, feet.east(12), level -> EntityTypes.COW.create(level, EntitySpawnReason.COMMAND), far -> {
			});
			List<String> far = lookThrough(context, world, feet, 4);
			check(far.contains("Cow") && far.contains("Ready to breed"), "a cow read from afar (" + far + ")");

			// Past the old 64 blocks, the cow a hundred blocks away, on flat ground: the view and entity distances as in a
			// game, the tests' being too short for the client to know a mob that far
			context.runOnClient(client -> {
				client.options.renderDistance().set(12);
				// The game's own entity distance, which the tests turn down
				client.options.entityDistanceScaling().set(1.0);
				// The server tracks mobs as far as the view distance the client asked for
				client.options.broadcastOptions();
			});
			world.getServer().runOnServer(server -> server.getPlayerList().setViewDistance(12));
			world.getConnection().waitForChunksRender();
			spawn(world, feet.east(100), level -> EntityTypes.COW.create(level, EntitySpawnReason.COMMAND), farther -> {
			});
			context.waitTicks(20);
			List<String> farther = lookThrough(context, world, feet, 0.5F);
			int onClient = context.computeOnClient(client -> client.level.getEntitiesOfClass(net.minecraft.world.entity.animal.cow.Cow.class,
					client.player.getBoundingBox().inflate(128)).size());
			check(farther.contains("Cow") && farther.contains("Ready to breed"), "a cow read a hundred blocks away (" + farther + ", cows known to the client: " + onClient + ")");

			// Wheat three steps grown, of seven, looked at from above
			clear(world);
			world.getServer().runOnServer(server -> {
				ServerLevel level = server.overworld();
				level.setBlockAndUpdate(feet.east().below(), Blocks.FARMLAND.defaultBlockState());
				level.setBlockAndUpdate(feet.east(), Blocks.WHEAT.defaultBlockState().setValue(CropBlock.AGE, 3));
			});
			List<String> wheat = lookThrough(context, world, feet, 50);
			check(wheat.contains("Growth: 42%"), "the wheat's growth (" + wheat + ")");
			check(context.computeOnClient(client -> GlancePanel.shown()).isEmpty(), "nothing read once the spyglass is put down");

			// A beehive's honey and the number of bees inside, synchronized as they come and go
			clear(world);
			BlockPos hivePos = feet.east();
			world.getServer().runOnServer(server -> server.overworld().setBlockAndUpdate(hivePos, Blocks.BEEHIVE.defaultBlockState()));
			context.waitTicks(3);
			world.getServer().runOnServer(server -> {
				BeehiveBlockEntity hive = (BeehiveBlockEntity) server.overworld().getBlockEntity(hivePos);
				hive.storeBee(BeehiveBlockEntity.Occupant.create(100));
				hive.storeBee(BeehiveBlockEntity.Occupant.create(100));
			});
			List<String> beehive = lookThrough(context, world, feet, 50);
			check(beehive.equals(List.of("Beehive", "Honey: 0 / 5", "Bees inside: 2 / 3")), "the beehive's honey and bees (" + beehive + ")");

			// A note block's note, read with no spyglass, just by looking at it
			clear(world);
			world.getServer().runOnServer(server -> server.overworld().setBlockAndUpdate(feet.east(),
					Blocks.NOTE_BLOCK.defaultBlockState().setValue(NoteBlock.NOTE, 6)));
			look(world, feet, 50);
			context.waitTicks(5);
			List<String> note = context.computeOnClient(client -> GlancePanel.shown());
			check(note.equals(List.of("Note Block", "C4, tuned 6 of 24")), "the note block's note (" + note + ")");
			context.takeScreenshot("cassonade-note-block");

			// A chest's name, sent by the server and read the same way, from either half of a double chest
			clear(world);
			// Facing north, a left half's other half is east of it: the name is on the half beyond the one looked at
			BlockPos near = feet.east();
			world.getServer().runCommand("setblock " + near.getX() + " " + near.getY() + " " + near.getZ() + " minecraft:chest[type=left,facing=north]");
			world.getServer().runCommand("setblock " + (near.getX() + 1) + " " + near.getY() + " " + near.getZ()
					+ " minecraft:chest[type=right,facing=north]{CustomName:\"Ores\"}");
			look(world, feet, 50);
			context.waitTicks(5);
			List<String> chest = context.computeOnClient(client -> GlancePanel.shown());
			check(chest.equals(List.of("Ores")), "the chest's name (" + chest + ")");
			String at = near.getX() + " " + near.getY() + " " + near.getZ();
			world.getServer().runCommand("setblock " + (near.getX() + 1) + " " + near.getY() + " " + near.getZ() + " minecraft:air");

			// With a honeycomb in hand, whether copper is waxed; nothing without it
			world.getServer().runCommand("setblock " + at + " minecraft:exposed_copper");
			context.waitTicks(5);
			check(context.computeOnClient(client -> GlancePanel.shown()).isEmpty(), "nothing read on copper with an empty hand");
			world.getServer().runCommand("item replace entity @a weapon.mainhand with honeycomb");
			context.waitTicks(5);
			List<String> copper = context.computeOnClient(client -> GlancePanel.shown());
			check(copper.equals(List.of("Exposed Copper", "Not waxed")), "the copper is not waxed (" + copper + ")");
			world.getServer().runCommand("setblock " + at + " minecraft:waxed_exposed_copper");
			context.waitTicks(5);
			List<String> waxed = context.computeOnClient(client -> GlancePanel.shown());
			check(waxed.equals(List.of("Waxed Exposed Copper", "Waxed")), "the copper is waxed (" + waxed + ")");

			// With redstone dust in hand, a wire's power, here next to a block of redstone
			world.getServer().runCommand("setblock " + at + " minecraft:redstone_wire");
			world.getServer().runCommand("setblock " + (near.getX() + 1) + " " + near.getY() + " " + near.getZ() + " minecraft:redstone_block");
			world.getServer().runCommand("item replace entity @a weapon.mainhand with redstone");
			context.waitTicks(5);
			List<String> wire = context.computeOnClient(client -> GlancePanel.shown());
			check(wire.equals(List.of("Redstone Wire", "Power: 15 of 15")), "the wire's power (" + wire + ")");

			// Any other block: whether it is powered, here a stone block beside a block of redstone, then without it
			world.getServer().runCommand("setblock " + at + " minecraft:stone");
			context.waitTicks(5);
			List<String> powered = context.computeOnClient(client -> GlancePanel.shown());
			check(powered.equals(List.of("Stone", "Powered: 15 of 15")), "the stone is powered (" + powered + ")");
			world.getServer().runCommand("setblock " + (near.getX() + 1) + " " + near.getY() + " " + near.getZ() + " minecraft:air");
			context.waitTicks(5);
			List<String> unpowered = context.computeOnClient(client -> GlancePanel.shown());
			check(unpowered.equals(List.of("Stone", "Not powered")), "the stone is not powered (" + unpowered + ")");
			world.getServer().runCommand("item replace entity @a weapon.mainhand with air");
		}
	}

	/**
	 * Raises the spyglass at the sky (used on a mob, it would ride or trade with it), turns to look down east by the
	 * angle, and gives the panel's lines once the server answered what only it knows.
	 */
	private static List<String> lookThrough(ClientGameTestContext context, TestSingleplayerContext world, BlockPos feet, float down) {
		look(world, feet, -60);
		// The item given a moment ago reaches the hand first: a click on an empty hand would be spent
		context.waitTicks(3);
		context.getInput().holdKey(options -> options.keyUse);
		context.waitTicks(3);
		check(context.computeOnClient(client -> client.player.isScoping()), "the spyglass zooms");
		look(world, feet, down);
		context.waitTicks(8);
		List<String> lines = context.computeOnClient(client -> GlancePanel.shown());
		if (down == 20 && lines.contains("Horse")) {
			context.takeScreenshot("cassonade-spyglass-horse");
		}
		context.getInput().releaseKey(options -> options.keyUse);
		context.waitTicks(3);
		return lines;
	}

	/** Every mob gone at once: a killed one stays a second, dying, still looked at. */
	private static void clear(TestSingleplayerContext world) {
		world.getServer().runOnServer(server -> {
			List<Entity> mobs = new java.util.ArrayList<>();
			server.overworld().getAllEntities().forEach(entity -> {
				if (!(entity instanceof ServerPlayer)) {
					mobs.add(entity);
				}
			});
			mobs.forEach(Entity::discard);
		});
	}

	/** On the middle of the block, looking east and down by the angle. */
	private static void look(TestSingleplayerContext world, BlockPos feet, float down) {
		world.getServer().runCommand("tp @a " + (feet.getX() + 0.5) + " " + feet.getY() + " " + (feet.getZ() + 0.5) + " -90 " + down);
	}

	private static void spawn(TestSingleplayerContext world, BlockPos pos, java.util.function.Function<ServerLevel, Entity> create, Consumer<Entity> setUp) {
		clear(world);
		world.getServer().runOnServer(server -> {
			ServerLevel level = server.overworld();
			Entity entity = create.apply(level);
			entity.snapTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 90, 0);
			((net.minecraft.world.entity.Mob) entity).setNoAi(true);
			setUp.accept(entity);
			if (entity instanceof net.minecraft.world.entity.LivingEntity living) {
				living.setHealth(living.getMaxHealth());
			}
			level.addFreshEntity(entity);
		});
	}

	private static void check(boolean condition, String what) {
		if (!condition) {
			throw new AssertionError("Expected: " + what);
		}
	}
}
