package com.cassonade.test;

import com.mojang.blaze3d.platform.InputConstants;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.gametest.v1.world.TestWorldSave;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.StructureTags;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece;

import com.cassonade.gear.GearSlot;
import com.cassonade.gear.PlayerGear;
import com.cassonade.item.CassonadeItems;

/**
 * {@code /dev}: the kit fills the gear slots, goto stands the player in a cave biome, in a village and in a tavern, and regen makes
 * the terrain again when the world is opened next.
 */
public class DevCommandsClientGameTest implements FabricClientGameTest {
	@Override
	public void runTest(ClientGameTestContext context) {
		if (TestSelection.skips(this)) {
			return;
		}
		context.getInput().resizeWindow(1920, 1080);
		TestWorldSave save;
		BlockPos witness;
		try (TestSingleplayerContext world = context.worldBuilder().setUseConsistentSettings(false).adjustSettings(state -> {
			state.setSeed("vanillasugar");
			state.setWorldType(state.getNormalPresetList().getFirst());
			state.setAllowCommands(true);
		}).create()) {
			world.getConnection().waitForChunksRender();
			world.getServer().runCommand("time set 6000");

			command(context, "dev kit");
			check(world.getServer().computeOnServer(server -> {
				PlayerGear gear = PlayerGear.of(player(server));
				return gear.get(GearSlot.COMPASS).is(CassonadeItems.SCULK_COMPASS) && gear.get(GearSlot.CLOCK).is(CassonadeItems.BAROMETER)
						&& gear.get(GearSlot.BACKPACK).is(CassonadeItems.ORGANIZED_BACKPACK) && !gear.get(GearSlot.ELYTRA).isEmpty()
						&& player(server).getInventory().contains(stack -> stack.is(CassonadeItems.NATURALIST_SPYGLASS));
			}), "the kit fills every gear slot and gives the tools");
			Screenshots.take(context, "dev-kit");

			command(context, "dev unequip");
			check(world.getServer().computeOnServer(server -> {
				PlayerGear gear = PlayerGear.of(player(server));
				return gear.get(GearSlot.COMPASS).isEmpty() && player(server).getInventory().contains(stack -> stack.is(CassonadeItems.SCULK_COMPASS));
			}), "unequip empties the slots into the inventory");

			// The keys run the commands without an argument
			context.getInput().pressKey(InputConstants.KEY_F6);
			context.waitTicks(20);
			check(world.getServer().computeOnServer(server -> PlayerGear.of(player(server)).get(GearSlot.COMPASS).is(CassonadeItems.SCULK_COMPASS)),
					"F6 runs /dev kit");

			command(context, "dev goto biome minecraft:lush_caves");
			String cave = world.getServer().computeOnServer(server -> {
				ServerPlayer player = player(server);
				BlockPos feet = player.blockPosition();
				boolean ok = player.level().getBiome(feet).is(Biomes.LUSH_CAVES) && !player.level().getBlockState(feet.below()).getCollisionShape(player.level(), feet.below()).isEmpty()
						&& !player.level().canSeeSky(feet);
				return ok ? null : player.position() + " in " + player.level().getBiome(feet).getRegisteredName() + " on " + player.level().getBlockState(feet.below())
						+ ", sky " + player.level().canSeeSky(feet);
			});
			check(cave == null, "goto biome stands the player in a cave biome, under ground: " + cave);
			world.getConnection().waitForChunksRender();
			Screenshots.take(context, "dev-goto-lush-caves");

			command(context, "dev goto structure #minecraft:village");
			check(world.getServer().computeOnServer(server -> {
				ServerPlayer player = player(server);
				return player.level().structureManager().getStructureWithPieceAt(player.blockPosition(), StructureTags.VILLAGE).isValid()
						|| player.level().structureManager().getStructureWithPieceAt(player.blockPosition().below(), StructureTags.VILLAGE).isValid();
			}), "goto structure stands the player in a village");
			world.getConnection().waitForChunksRender();
			Screenshots.take(context, "dev-goto-village");

			command(context, "dev goto tavern");
			check(world.getServer().computeOnServer(server -> {
				ServerPlayer player = player(server);
				return BlockPos.betweenClosedStream(player.blockPosition().offset(-12, -3, -12), player.blockPosition().offset(12, 3, 12))
						.anyMatch(pos -> player.level().getBlockState(pos).is(Blocks.BREWING_STAND));
			}), "goto tavern stands the player in a tavern, by its brewing stand");
			world.getConnection().waitForChunksRender();
			Screenshots.take(context, "dev-goto-tavern");

			// Of a given type of village: the tavern's template is that village's
			command(context, "dev goto tavern desert");
			check(world.getServer().computeOnServer(server -> {
				ServerPlayer player = player(server);
				return player.level().structureManager().getStructureWithPieceAt(player.blockPosition(), StructureTags.VILLAGE).getPieces().stream()
						.anyMatch(piece -> piece instanceof PoolElementStructurePiece house && house.getElement().toString().contains("village/desert/")
								&& house.getElement().toString().contains("_temple_") && house.getBoundingBox().inflatedBy(3).isInside(player.blockPosition()));
			}), "goto tavern desert stands the player in a desert village's tavern");

			// A block the world generation never makes, to see it gone once the terrain is made again
			witness = world.getServer().computeOnServer(server -> player(server).blockPosition().above(3));
			world.getServer().runCommand("setblock " + witness.getX() + " " + witness.getY() + " " + witness.getZ() + " minecraft:diamond_block");
			command(context, "dev regen");
			save = world.getWorldSave();
		}
		try (TestSingleplayerContext world = save.open()) {
			world.getConnection().waitForChunksRender();
			check(world.getServer().computeOnServer(server -> {
				ServerPlayer player = player(server);
				BlockPos feet = player.blockPosition();
				return !player.level().getBlockState(witness).is(Blocks.DIAMOND_BLOCK) && player.level().canSeeSky(feet)
						&& !player.level().getBlockState(feet.below()).getCollisionShape(player.level(), feet.below()).isEmpty()
						&& player.getInventory().contains(stack -> stack.is(CassonadeItems.NATURALIST_SPYGLASS));
			}), "regen makes the terrain again, keeps the player's things and stands them on the ground");
			Screenshots.take(context, "dev-regen");
		}
	}

	private static ServerPlayer player(MinecraftServer server) {
		return server.getPlayerList().getPlayers().getFirst();
	}

	private static void command(ClientGameTestContext context, String command) {
		context.runOnClient(client -> client.player.connection.sendCommand(command));
		context.waitTicks(20);
	}

	private static void check(boolean condition, String what) {
		if (!condition) {
			throw new AssertionError(what);
		}
	}
}
