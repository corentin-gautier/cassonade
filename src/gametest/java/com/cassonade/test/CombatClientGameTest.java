package com.cassonade.test;

import java.util.UUID;
import java.util.List;

import com.mojang.authlib.GameProfile;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

import net.minecraft.client.player.RemotePlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.zombie.Husk;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Ghast;
import net.minecraft.world.item.Items;

import com.cassonade.client.ClientSettings;
import com.cassonade.client.combat.CombatHud;
import com.cassonade.client.combat.DamageDirection;
import com.cassonade.client.quest.QuestLog;
import com.cassonade.client.quest.QuestOptInScreen;
import com.cassonade.gear.GearSlot;
import com.cassonade.gear.PlayerGear;
import com.cassonade.item.CassonadeItems;
import com.cassonade.client.quest.AchievementBanner;
import com.cassonade.client.map.Minimap;

/**
 * Where a hit came from: a zombie on the player's left marks the left of the crosshair, and a fall
 * marks nothing. With a sculk compass, a mob hunting the player shows on the minimap. An attack aimed at
 * tall grass hits the mob behind it. With a clock worn, a mob chasing the player shows its health, a "!" and an eye
 * by the clock; a plain compass senses nothing.
 */
public class CombatClientGameTest implements FabricClientGameTest {
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
			// Looking south, east is on the left
			world.getServer().runCommand("tp @a ~ ~ ~ 0 0");
			world.getServer().runCommand("execute at @a run summon zombie ~3 ~ ~ {NoAI:1b}");
			context.waitTicks(5);
			world.getServer().runOnServer(server -> {
				ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
				Zombie zombie = player.level().getEntitiesOfClass(Zombie.class, player.getBoundingBox().inflate(5)).getFirst();
				player.hurtServer(player.level(), player.damageSources().mobAttack(zombie), 1);
			});
			context.waitTicks(3);
			Float angle = context.computeOnClient(client -> DamageDirection.lastAngle());
			check(angle != null && Math.abs(angle + Mth.HALF_PI) < 0.2, "the zombie's hit marked on the left (" + angle + ")");
			Screenshots.take(context, "cassonade-damage-direction");

			context.waitTicks(40);
			check(context.computeOnClient(client -> DamageDirection.lastAngle()) == null, "the mark gone after a moment");
			world.getServer().runOnServer(server -> {
				ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
				player.hurtServer(player.level(), player.damageSources().fall(), 1);
			});
			context.waitTicks(3);
			check(context.computeOnClient(client -> DamageDirection.lastAngle()) == null, "no mark for a fall");

			// With a sculk compass, a husk hunting the player from the east shows on the minimap, north up
			context.runOnClient(client -> ClientSettings.setMinimapRotates(false));
			world.getServer().runOnServer(server -> {
				ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
				player.setAttached(PlayerGear.ATTACHMENT, PlayerGear.of(player).with(GearSlot.COMPASS, new ItemStack(CassonadeItems.SCULK_COMPASS)));
			});
			world.getServer().runCommand("execute at @a run summon husk ~12 ~ ~");
			context.waitTicks(5);
			world.getServer().runOnServer(server -> {
				ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
				player.level().getEntitiesOfClass(Husk.class, player.getBoundingBox().inflate(20)).getFirst().setTarget(player);
			});
			context.waitTicks(5);
			check(context.computeOnClient(client -> CombatHud.hunters().stream().anyMatch(id -> client.level.getEntity(id) instanceof Husk)),
					"the husk hunting the player known to the minimap");
			Screenshots.take(context, "cassonade-hunters-minimap");

			// Another player to the south west, sensed by the sculk compass too: a blue marker, named
			context.runOnClient(client -> {
				RemotePlayer other = new RemotePlayer(client.level, new GameProfile(UUID.randomUUID(), "Alex"));
				// An id the server will never hand out, the entity living on the client only
				other.setId(Integer.MAX_VALUE);
				other.snapTo(client.player.getX() - 10, client.player.getY(), client.player.getZ() + 14, 45, 0);
				client.level.addEntity(other);
			});
			context.waitTicks(2);
			Screenshots.take(context, "cassonade-players-minimap");
			context.runOnClient(client -> client.level.players().stream().filter(other -> other != client.player).toList()
					.forEach(other -> client.level.removeEntity(other.getId(), Entity.RemovalReason.DISCARDED)));

			// An attack aimed at tall grass reaches the husk behind it, but not through glass
			world.getServer().runCommand("kill @e[type=!player]");
			BlockPos feet = world.getServer().computeOnServer(server -> server.getPlayerList().getPlayers().getFirst().blockPosition());
			// Centered on its block, looking east: the husk's near side 2.7 blocks from the eyes, within the attack's reach of 3
			world.getServer().runCommand("tp @a " + (feet.getX() + 0.5) + " " + feet.getY() + " " + (feet.getZ() + 0.5) + " -90 0");
			world.getServer().runOnServer(server -> {
				ServerLevel level = server.overworld();
				DoublePlantBlock.placeAt(level, Blocks.TALL_GRASS.defaultBlockState(), feet.east(), Block.UPDATE_ALL);
				level.setBlockAndUpdate(feet.east(2).above(), Blocks.GLASS.defaultBlockState());
			});
			// A husk: it doesn't burn in the sun, which would hurt it without any attack
			world.getServer().runCommand("summon husk " + (feet.getX() + 3.5) + " " + feet.getY() + " " + (feet.getZ() + 0.5) + " {NoAI:1b}");
			context.waitTicks(5);
			attack(context);
			float health = huskHealth(world);
			check(health == 20, "the glass between the grass and the husk stops the attack (health " + health + ")");
			world.getServer().runOnServer(server -> {
				ServerLevel level = server.overworld();
				level.setBlockAndUpdate(feet.east(2).above(), Blocks.AIR.defaultBlockState());
				DoublePlantBlock.placeAt(level, Blocks.TALL_GRASS.defaultBlockState(), feet.east(), Block.UPDATE_ALL);
			});
			context.waitTicks(5);
			check(context.computeOnClient(client -> client.hitResult instanceof BlockHitResult aimed
					&& client.level.getBlockState(aimed.getBlockPos()).is(Blocks.TALL_GRASS)), "the crosshair on the tall grass");
			attack(context);
			health = huskHealth(world);
			check(health < 20, "the attack through the tall grass hurts the husk (health " + health + ")");
			check(world.getServer().computeOnServer(server -> server.overworld().getBlockState(feet.east()).is(Blocks.TALL_GRASS)),
					"the tall grass left standing");

			// The compass worn back, with a clock, facing south
			world.getServer().runCommand("kill @e[type=!player]");
			world.getServer().runCommand("tp @a ~ ~ ~ 0 0");
			world.getServer().runOnServer(server -> {
				ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
				player.setAttached(PlayerGear.ATTACHMENT, PlayerGear.of(player).with(GearSlot.COMPASS, new ItemStack(Items.COMPASS)));
			});
			// Combat, with a sculk compass only: a mob chasing the player shows its health and a "!"; one left
			// alone shows nothing. The clock worn again, for the eye right of its panel
			world.getServer().runOnServer(server -> {
				ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
				player.setAttached(PlayerGear.ATTACHMENT, PlayerGear.of(player).with(GearSlot.CLOCK, new ItemStack(Items.CLOCK)));
			});
			world.getServer().runCommand("execute as @a at @s run summon husk ~-2 ~ ~9 {NoAI:1b,Tags:[\"hunter\"]}");
			world.getServer().runCommand("execute as @a at @s run summon husk ~3 ~ ~10 {NoAI:1b,Tags:[\"idle\"]}");
			context.waitTicks(5);
			int[] husks = world.getServer().computeOnServer(server -> {
				ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
				List<Husk> found = player.level().getEntitiesOfClass(Husk.class, player.getBoundingBox().inflate(16));
				Husk hunter = found.stream().filter(husk -> husk.entityTags().contains("hunter")).findFirst().orElseThrow();
				Husk idle = found.stream().filter(husk -> husk.entityTags().contains("idle")).findFirst().orElseThrow();
				hunter.setTarget(player);
				return new int[] {hunter.getId(), idle.getId()};
			});
			// The banners of the advancements made so far would hide the mobs' heads
			context.runOnClient(client -> AchievementBanner.clear());
			context.waitTicks(13);
			Screenshots.take(context, "cassonade-5-combat-compass");
			check(context.computeOnClient(client -> !CombatHud.hasSpotted(husks[0]) && !CombatHud.isShown(husks[0]) && !CombatHud.isHuntedShown(client)),
					"a compass senses no mob, its health, \"!\" or eye");
			// The sculk compass worn, its advancement celebrated, the mob starts the chase again
			world.getServer().runOnServer(server -> {
				ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
				((Mob) server.overworld().getEntity(husks[0])).setTarget(null);
				player.setAttached(PlayerGear.ATTACHMENT, PlayerGear.of(player).with(GearSlot.COMPASS, new ItemStack(CassonadeItems.SCULK_COMPASS)));
			});
			// The advancement comes within a second
			context.waitTicks(30);
			context.runOnClient(client -> AchievementBanner.clear());
			world.getServer().runOnServer(server -> ((Mob) server.overworld().getEntity(husks[0]))
					.setTarget(server.getPlayerList().getPlayers().getFirst()));
			context.waitTicks(8);
			Screenshots.take(context, "cassonade-5-combat-spotted");
			context.waitTicks(10);
			check(context.computeOnClient(client -> CombatHud.isHuntedShown(client) && Minimap.isShown(client)),
					"a sculk compass shows the minimap and the eye");
			Screenshots.take(context, "cassonade-5-combat-hunted");
			check(context.computeOnClient(client -> CombatHud.hasSpotted(husks[0]) && CombatHud.isShown(husks[0])),
					"a mob chasing the player shows its health and that it spotted them");
			check(context.computeOnClient(client -> !CombatHud.isShown(husks[1]) && !CombatHud.hasSpotted(husks[1])),
					"a mob left alone shows nothing");
			// A second one: the eye next to the hotbar counts them
			world.getServer().runOnServer(server -> ((Mob) server.overworld().getEntity(husks[1]))
					.setTarget(server.getPlayerList().getPlayers().getFirst()));
			context.waitTicks(10);
			Screenshots.take(context, "cassonade-5-combat-hunted-twice");
			check(context.computeOnClient(client -> CombatHud.hasSpotted(husks[1])), "a second mob chasing the player is counted");
			world.getServer().runOnServer(server -> ((Mob) server.overworld().getEntity(husks[1])).setTarget(null));
			context.waitTicks(10);
			world.getServer().runCommand("damage @e[type=husk,tag=hunter,limit=1] 8");
			world.getServer().runCommand("damage @e[type=husk,tag=idle,limit=1] 5");
			context.waitTicks(2);
			Screenshots.take(context, "cassonade-5-combat-hurt");
			check(context.computeOnClient(client -> CombatHud.isShown(husks[1]) && !CombatHud.hasSpotted(husks[1])),
					"a mob just hurt shows its health");
			world.getServer().runOnServer(server -> ((Mob) server.overworld().getEntity(husks[0])).setTarget(null));
			context.waitTicks(30);
			Screenshots.take(context, "cassonade-5-combat-drained");
			check(context.computeOnClient(client -> !CombatHud.hasSpotted(husks[0])), "a mob that gave up the chase loses its \"!\"");
			world.getServer().runCommand("kill @e[type=husk]");
			// A ghast aims from farther than any other mob: one chasing the player 56 blocks away shows too
			world.getServer().runCommand("execute as @a at @s run summon ghast ~ ~6 ~56 {NoAI:1b}");
			context.waitTicks(5);
			int ghast = world.getServer().computeOnServer(server -> {
				ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
				Ghast found = player.level().getEntitiesOfClass(Ghast.class, player.getBoundingBox().inflate(64)).getFirst();
				found.setTarget(player);
				return found.getId();
			});
			context.waitTicks(5);
			check(context.computeOnClient(client -> CombatHud.hasSpotted(ghast) && CombatHud.isShown(ghast) && CombatHud.isHuntedShown(client)),
					"a ghast chasing the player from afar shows its health, \"!\" and eye");
			world.getServer().runCommand("kill @e[type=ghast]");
			world.getServer().runOnServer(server -> {
				ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
				player.setAttached(PlayerGear.ATTACHMENT, PlayerGear.of(player).with(GearSlot.CLOCK, ItemStack.EMPTY)
						.with(GearSlot.COMPASS, new ItemStack(Items.COMPASS)));
			});
		}
	}

	private static void attack(ClientGameTestContext context) {
		context.getInput().pressKey(options -> options.keyAttack);
		context.waitTicks(3);
	}

	private static float huskHealth(TestSingleplayerContext world) {
		return world.getServer().computeOnServer(server -> {
			ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
			return player.level().getEntitiesOfClass(Husk.class, player.getBoundingBox().inflate(5)).getFirst().getHealth();
		});
	}

	private static void check(boolean condition, String what) {
		if (!condition) {
			throw new AssertionError("Expected: " + what);
		}
	}
}
