package com.cassonade.test;

import static com.cassonade.test.PlannedWorld.check;
import static com.cassonade.test.PlannedWorld.player;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;

public class NameTagSilenceClientGameTest implements FabricClientGameTest {
	@Override
	public void runTest(ClientGameTestContext context) {
		if (TestSelection.skips(this)) {
			return;
		}
		try (TestSingleplayerContext world = PlannedWorld.open(context)) {
			world.getServer().runOnServer(server -> {
				ServerPlayer player = player(server);
				ServerLevel level = server.overworld();
				Mob villager = spawn(level, EntityTypes.VILLAGER);
				ItemStack tags = new ItemStack(Items.NAME_TAG, 3);
				player.setItemInHand(InteractionHand.MAIN_HAND, tags);

				player.interactOn(villager, InteractionHand.MAIN_HAND, villager.position());
				check(villager.isSilent() && tags.getCount() == 2, "a villager silenced, the tag used up");
				check(villager.isPersistenceRequired(), "it stays like a named mob");
				check(player.containerMenu == player.inventoryMenu, "no trade screen");

				player.interactOn(villager, InteractionHand.MAIN_HAND, villager.position());
				check(!villager.isSilent() && tags.getCount() == 2, "its voice back, the tag kept");

				ItemStack named = new ItemStack(Items.NAME_TAG);
				named.set(DataComponents.CUSTOM_NAME, Component.literal("Bob"));
				player.setItemInHand(InteractionHand.MAIN_HAND, named);
				Mob pig = spawn(level, EntityTypes.PIG);
				player.interactOn(pig, InteractionHand.MAIN_HAND, pig.position());
				check(!pig.isSilent() && "Bob".equals(pig.getCustomName().getString()), "a named tag only names");
			});
			context.waitTicks(5);
			context.runOnClient(client -> {
				String blank = new ItemStack(Items.NAME_TAG).getTooltipLines(Item.TooltipContext.of(client.level), client.player, TooltipFlag.NORMAL).toString();
				check(blank.contains("name_tag.hint") || blank.contains("silences"), "the tip on a blank tag: " + blank);
				ItemStack named = new ItemStack(Items.NAME_TAG);
				named.set(DataComponents.CUSTOM_NAME, Component.literal("Bob"));
				String tagged = named.getTooltipLines(Item.TooltipContext.of(client.level), client.player, TooltipFlag.NORMAL).toString();
				check(!tagged.contains("name_tag.hint") && !tagged.contains("silences"), "no tip on a named one");
			});
		}
	}

	private static <T extends Mob> T spawn(ServerLevel level, EntityType<T> type) {
		T mob = type.create(level, EntitySpawnReason.COMMAND);
		mob.setPos(4.5, 101, 2.5);
		level.addFreshEntity(mob);
		return mob;
	}
}
