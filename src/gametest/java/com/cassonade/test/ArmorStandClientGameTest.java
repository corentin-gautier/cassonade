package com.cassonade.test;

import java.util.List;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.Rotations;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import com.cassonade.client.CassonadeClient;
import com.cassonade.client.quest.QuestLog;
import com.cassonade.client.quest.QuestOptInScreen;

/**
 * Armor stands: placed with arms, which shears take off and two sticks put back; Alt and an empty hand swaps
 * the whole armor with the stand's, a cursed piece kept on; sneaking, an empty hand goes through the poses.
 */
public class ArmorStandClientGameTest implements FabricClientGameTest {
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
			// An armor stand is placed with arms; shears take them off and drop what the hands held, two sticks put them back
			world.getServer().runOnServer(server -> {
				ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
				ServerLevel level = player.level();
				BlockPos ground = player.blockPosition().south(3).below();
				level.setBlockAndUpdate(ground, Blocks.STONE.defaultBlockState());
				level.setBlockAndUpdate(ground.above(), Blocks.AIR.defaultBlockState());
				level.setBlockAndUpdate(ground.above(2), Blocks.AIR.defaultBlockState());
				player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.ARMOR_STAND));
				new ItemStack(Items.ARMOR_STAND).useOn(new UseOnContext(player, InteractionHand.MAIN_HAND,
						new BlockHitResult(Vec3.atCenterOf(ground).add(0, 0.5, 0), Direction.UP, ground, false)));
				ArmorStand placed = level.getEntitiesOfClass(ArmorStand.class, new AABB(ground.above())).stream().findFirst().orElseThrow();
				check(placed.showArms(), "a placed armor stand has arms");
				placed.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.TORCH));
				player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.SHEARS));
				placed.interact(player, InteractionHand.MAIN_HAND, new Vec3(0, 1, 0));
				check(!placed.showArms() && placed.getMainHandItem().isEmpty(), "shears take the arms off, the torch dropped");
				check(!level.getEntitiesOfClass(ItemEntity.class, placed.getBoundingBox().inflate(1), item -> item.getItem().is(Items.TORCH)).isEmpty(),
						"the torch on the ground");
				check(player.getMainHandItem().getDamageValue() == 1, "the shears worn by one use");
				player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.STICK));
				placed.interact(player, InteractionHand.MAIN_HAND, new Vec3(0, 1, 0));
				check(!placed.showArms() && player.getMainHandItem().getCount() == 1, "a single stick is not enough for two arms");
				player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.STICK, 3));
				placed.interact(player, InteractionHand.MAIN_HAND, new Vec3(0, 1, 0));
				check(placed.showArms() && player.getMainHandItem().getCount() == 1, "two sticks put the arms back");
				level.getEntitiesOfClass(ItemEntity.class, placed.getBoundingBox().inflate(1)).forEach(Entity::discard);
				placed.discard();
				player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
			});

			// Sneaking, an empty hand goes through the thirteen poses and back to the default
			world.getServer().runOnServer(server -> {
				ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
				ServerLevel level = player.level();
				ArmorStand stand = new ArmorStand(level, player.getX() + 1, player.getY(), player.getZ());
				level.addFreshEntity(stand);
				Rotations head = stand.getHeadPose();
				Rotations arm = stand.getRightArmPose();
				player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
				player.setShiftKeyDown(true);
				stand.interact(player, InteractionHand.MAIN_HAND, new Vec3(0, 1, 0));
				check(!stand.getRightArmPose().equals(arm), "the next pose");
				for (int i = 1; i < 13; i++) {
					stand.interact(player, InteractionHand.MAIN_HAND, new Vec3(0, 1, 0));
				}
				player.setShiftKeyDown(false);
				check(stand.getHeadPose().equals(head) && stand.getRightArmPose().equals(arm), "back to the default after thirteen clicks");
			});

			// Alt and an empty hand swaps the whole armor with an armor stand's, a cursed piece kept on
			int standId = world.getServer().computeOnServer(server -> {
				ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
				ServerLevel level = player.level();
				ArmorStand stand = level.getEntitiesOfClass(ArmorStand.class, player.getBoundingBox().inflate(3)).getFirst();
				stand.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
				stand.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.IRON_CHESTPLATE));
				player.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.DIAMOND_CHESTPLATE));
				ItemStack cursed = new ItemStack(Items.DIAMOND_BOOTS);
				cursed.enchant(level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.BINDING_CURSE), 1);
				player.setItemSlot(EquipmentSlot.FEET, cursed);
				player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
				return stand.getId();
			});
			context.waitTicks(2);
			context.getInput().holdKey(CassonadeClient.LOCK_SLOT);
			context.waitTicks(1);
			context.runOnClient(client -> {
				InteractionResult result = UseEntityCallback.EVENT.invoker().interact(client.player, client.level, InteractionHand.MAIN_HAND, client.level.getEntity(standId), null);
				check(result == InteractionResult.FAIL, "the click kept from the server");
			});
			context.getInput().releaseKey(CassonadeClient.LOCK_SLOT);
			context.waitTicks(2);
			world.getServer().runOnServer(server -> {
				ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
				ArmorStand stand = (ArmorStand) player.level().getEntity(standId);
				check(player.getItemBySlot(EquipmentSlot.HEAD).is(Items.IRON_HELMET) && player.getItemBySlot(EquipmentSlot.CHEST).is(Items.IRON_CHESTPLATE),
						"the stand's armor put on");
				check(stand.getItemBySlot(EquipmentSlot.CHEST).is(Items.DIAMOND_CHESTPLATE) && stand.getItemBySlot(EquipmentSlot.HEAD).isEmpty(),
						"the player's armor on the stand");
				check(player.getItemBySlot(EquipmentSlot.FEET).is(Items.DIAMOND_BOOTS) && stand.getItemBySlot(EquipmentSlot.FEET).isEmpty(),
						"the cursed boots kept on");
				stand.discard();
				for (EquipmentSlot slot : List.of(EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.FEET)) {
					player.setItemSlot(slot, ItemStack.EMPTY);
				}
			});
		}
	}

	private static void check(boolean condition, String what) {
		if (!condition) {
			throw new AssertionError("Expected: " + what);
		}
	}
}
