package com.cassonade.test;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SweetBerryBushBlock;
import net.minecraft.world.level.storage.LevelData;

import com.cassonade.tweaks.PlayerBeds;

public class BedAndThornsClientGameTest implements FabricClientGameTest {
	@Override
	public void runTest(ClientGameTestContext context) {
		if (TestSelection.skips(this)) {
			return;
		}
		try (TestSingleplayerContext world = context.worldBuilder().create()) {
			world.getServer().runCommand("gamemode survival @a");
			world.getServer().runOnServer(server -> {
				ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
				ServerLevel level = player.level();
				BlockPos bed = player.blockPosition().east(3);
				check(!PlayerBeds.isPlayerBed(level, bed), "a bed nobody respawns in is free");
				player.setRespawnPosition(new ServerPlayer.RespawnConfig(LevelData.RespawnData.of(level.dimension(), bed, 0, 0), false), false);
				check(PlayerBeds.isPlayerBed(level, bed), "the player's respawn bed is theirs");

				Villager villager = EntityTypes.VILLAGER.create(level, EntitySpawnReason.COMMAND);
				villager.snapTo(bed, 0, 0);
				level.addFreshEntity(villager);
				villager.getBrain().setMemory(MemoryModuleType.HOME, GlobalPos.of(level.dimension(), bed));
				new net.minecraft.world.entity.ai.behavior.SleepInBed().tryStart(level, villager, level.getGameTime());
				check(villager.getBrain().getMemory(MemoryModuleType.HOME).isEmpty(), "the villager gives the bed up");
				villager.discard();

				BlockPos bush = player.blockPosition();
				level.setBlockAndUpdate(bush, Blocks.SWEET_BERRY_BUSH.defaultBlockState().setValue(SweetBerryBushBlock.AGE, 3));
				Villager walker = EntityTypes.VILLAGER.create(level, EntitySpawnReason.COMMAND);
				walker.snapTo(bush, 0, 0);
				level.addFreshEntity(walker);
				float full = walker.getHealth();
				walker.setItemSlot(EquipmentSlot.LEGS, Items.LEATHER_LEGGINGS.getDefaultInstance());
				walker.setItemSlot(EquipmentSlot.FEET, Items.LEATHER_BOOTS.getDefaultInstance());
				walk(walker, level, bush);
				check(walker.getHealth() == full, "leggings and boots stop the thorns");
				walker.setItemSlot(EquipmentSlot.LEGS, net.minecraft.world.item.ItemStack.EMPTY);
				walk(walker, level, bush);
				check(walker.getHealth() < full, "boots alone don't");
				walker.discard();
			});
		}
	}

	private static void walk(Villager walker, ServerLevel level, BlockPos bush) {
		walker.setOldPosAndRot();
		walker.setPos(walker.getX() + 0.1, walker.getY(), walker.getZ());
		level.getBlockState(bush).entityInside(level, bush, walker, net.minecraft.world.entity.InsideBlockEffectApplier.NOOP, true);
	}

	private static void check(boolean condition, String what) {
		if (!condition) {
			throw new AssertionError("Expected: " + what);
		}
	}
}
