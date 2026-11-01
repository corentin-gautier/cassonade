package com.cassonade.test;

import static com.cassonade.test.PlannedWorld.check;
import static com.cassonade.test.PlannedWorld.player;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

import net.minecraft.client.gui.screens.inventory.MerchantScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.phys.Vec3;

import com.cassonade.client.map.Pings;
import com.cassonade.client.trader.WorkstationButton;

public class WorkstationButtonClientGameTest implements FabricClientGameTest {
	private static final BlockPos LECTERN = new BlockPos(8, 101, 8);

	@Override
	public void runTest(ClientGameTestContext context) {
		if (TestSelection.skips(this)) {
			return;
		}
		try (TestSingleplayerContext world = PlannedWorld.open(context)) {
			world.getServer().runCommand("setblock 8 101 8 lectern");
			world.getServer().runOnServer(server -> {
				ServerLevel level = server.overworld();
				Villager villager = EntityTypes.VILLAGER.create(level, EntitySpawnReason.COMMAND);
				villager.setPos(4.5, 101, 2.5);
				villager.setNoAi(true);
				villager.setVillagerData(villager.getVillagerData().withProfession(
						server.registryAccess().lookupOrThrow(Registries.VILLAGER_PROFESSION).getOrThrow(VillagerProfession.LIBRARIAN)));
				level.addFreshEntity(villager);
				villager.getBrain().setMemory(MemoryModuleType.JOB_SITE, GlobalPos.of(level.dimension(), LECTERN));
				villager.mobInteract(player(server), InteractionHand.MAIN_HAND);
			});
			context.waitForScreen(MerchantScreen.class);
			context.waitTicks(10);
			Screenshots.take(context, "cassonade-workstation-button");
			context.runOnClient(client -> {
				WorkstationButton button = client.gui.screen().children().stream().filter(WorkstationButton.class::isInstance)
						.map(WorkstationButton.class::cast).findFirst().orElseThrow();
				check(button.visible, "the button shown");
				button.onPress(null);
			});
			context.waitTicks(5);
			context.runOnClient(client -> {
				check(client.gui.screen() == null, "the screen closed");
				Vec3 toBlock = Vec3.atCenterOf(LECTERN).subtract(client.player.getEyePosition()).normalize();
				check(client.player.getViewVector(1).dot(toBlock) > 0.99, "the view turned to the block");
				check(Pings.shown().size() == 1 && Pings.shown().getFirst().pos().y == 102, "the block pointed out");
			});
			Screenshots.take(context, "cassonade-workstation-ping");
		}
	}
}
