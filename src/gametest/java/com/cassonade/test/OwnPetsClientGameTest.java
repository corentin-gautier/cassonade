package com.cassonade.test;

import static com.cassonade.test.PlannedWorld.check;
import static com.cassonade.test.PlannedWorld.player;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.equine.Horse;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class OwnPetsClientGameTest implements FabricClientGameTest {
	@Override
	public void runTest(ClientGameTestContext context) {
		if (TestSelection.skips(this)) {
			return;
		}
		try (TestSingleplayerContext world = PlannedWorld.open(context)) {
			world.getServer().runOnServer(server -> {
				ServerPlayer player = player(server);
				ServerLevel level = server.overworld();
				player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_SWORD));
				Wolf wolf = spawn(level, EntityTypes.WOLF, 3.5);
				wolf.tame(player);
				Horse horse = spawn(level, EntityTypes.HORSE, 3.5);
				horse.tameWithName(player);
				float wolfHealth = wolf.getHealth();
				float horseHealth = horse.getHealth();

				player.attack(wolf);
				check(wolf.getHealth() == wolfHealth, "nothing behind: the dog unhurt");
				player.attack(horse);
				check(horse.getHealth() == horseHealth, "nothing behind: the horse unhurt");

				Mob pig = spawn(level, EntityTypes.PIG, 5);
				float pigHealth = pig.getHealth();
				player.lookAt(EntityAnchorArgument.Anchor.EYES, pig.position());
				player.attack(wolf);
				check(wolf.getHealth() == wolfHealth && pig.getHealth() < pigHealth, "the hit lands on the pig behind the dog");

				player.setShiftKeyDown(true);
				player.attack(wolf);
				check(wolf.getHealth() < wolfHealth, "sneaking still hits the dog");
				player.setShiftKeyDown(false);

				Wolf stray = spawn(level, EntityTypes.WOLF, 3.5);
				float strayHealth = stray.getHealth();
				player.attack(stray);
				check(stray.getHealth() < strayHealth, "someone else's or no one's dog is hit as usual");
			});
		}
	}

	private static <T extends Mob> T spawn(ServerLevel level, EntityType<T> type, double x) {
		T mob = type.create(level, EntitySpawnReason.COMMAND);
		mob.setPos(x, 101, 2.5);
		mob.setNoAi(true);
		level.addFreshEntity(mob);
		return mob;
	}
}
