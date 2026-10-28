package com.cassonade.test;

import java.util.List;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

import net.minecraft.core.BlockPos;
import net.minecraft.data.worldgen.features.TreeFeatures;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.entity.animal.sheep.Sheep;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.WallSignBlock;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import com.cassonade.tweaks.TreeClearance;
import com.cassonade.client.light.DynamicLights;
import com.cassonade.client.light.SpawnSpots;
import com.cassonade.client.quest.QuestLog;
import com.cassonade.client.quest.QuestOptInScreen;

/**
 * Small tweaks, one at a time: a ladder used on a ladder extends the column upward, or downward when
 * looking down, and stops where the wall ends; shears hide an item frame; a sign on a chest opens it; a torch
 * in hand lights around; farmland is only trampled by mobs; a sweep spares the player's animals;
 * sneaking, an empty hand swaps armor with an armor stand; trees grow taller so you can walk under
 * them; a sculk torch marks where monsters can spawn.
 */
public class TweaksClientGameTest implements FabricClientGameTest {
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
			world.getServer().runOnServer(server -> {
				ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
				ServerLevel level = player.level();
				// A wall five blocks high, ladders on its two middle blocks, facing west
				BlockPos base = player.blockPosition().east(3).above(10);
				for (int i = 0; i < 5; i++) {
					level.setBlockAndUpdate(base.above(i), Blocks.STONE.defaultBlockState());
					level.setBlockAndUpdate(base.above(i).west(), Blocks.AIR.defaultBlockState());
				}
				level.setBlockAndUpdate(base.above(5).west(), Blocks.AIR.defaultBlockState());
				level.setBlockAndUpdate(base.below().west(), Blocks.AIR.defaultBlockState());
				var ladder = Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING, Direction.WEST);
				level.setBlockAndUpdate(base.above(2).west(), ladder);
				level.setBlockAndUpdate(base.above(3).west(), ladder);
				player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.LADDER, 8));

				// Looking up: the column grows at its top
				player.setXRot(-30);
				use(player, base.above(2).west());
				check(level.getBlockState(base.above(4).west()).is(Blocks.LADDER), "a ladder added at the top of the column");
				check(player.getMainHandItem().getCount() == 7, "the ladder taken from the hand");

				// Looking down: at its bottom, until the wall ends
				player.setXRot(60);
				use(player, base.above(4).west());
				use(player, base.above(4).west());
				use(player, base.above(4).west());
				check(level.getBlockState(base.above(1).west()).is(Blocks.LADDER)
						&& level.getBlockState(base.west()).is(Blocks.LADDER), "ladders added at the bottom of the column");
				check(level.getBlockState(base.below().west()).isAir(), "no ladder past the wall's end");
				check(player.getMainHandItem().getCount() == 5, "only the ladders placed taken");

				// Shears hide a frame holding an item, show it again, and a frame emptied shows again
				BlockPos wall = base.south(2);
				level.setBlockAndUpdate(wall, Blocks.STONE.defaultBlockState());
				ItemFrame frame = new ItemFrame(level, wall.west(), Direction.WEST);
				level.addFreshEntity(frame);
				frame.setItem(new ItemStack(Items.DIAMOND));
				player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.SHEARS));
				frame.interact(player, InteractionHand.MAIN_HAND, frame.position());
				check(frame.isInvisible() && frame.getItem().is(Items.DIAMOND), "the frame hidden, its item kept");
				check(player.getMainHandItem().getDamageValue() == 1, "the shears worn by one use");
				frame.interact(player, InteractionHand.MAIN_HAND, frame.position());
				check(!frame.isInvisible() && frame.getRotation() == 0, "the frame shown again, its item not turned");
				frame.interact(player, InteractionHand.MAIN_HAND, frame.position());
				frame.hurtServer(level, player.damageSources().playerAttack(player), 1);
				check(frame.getItem().isEmpty() && !frame.isInvisible(), "a hidden frame shown again once emptied");
				frame.discard();

				// A player jumping on farmland leaves it tilled, a zombie still tramples it
				BlockPos field = base.south(4);
				level.setBlockAndUpdate(field, Blocks.FARMLAND.defaultBlockState());
				level.getBlockState(field).getBlock().fallOn(level, level.getBlockState(field), field, player, 10);
				check(level.getBlockState(field).is(Blocks.FARMLAND), "farmland kept under a player's fall");
				Zombie zombie = new Zombie(level);
				zombie.setPos(Vec3.atCenterOf(field.above()));
				level.getBlockState(field).getBlock().fallOn(level, level.getBlockState(field), field, zombie, 10);
				check(level.getBlockState(field).is(Blocks.DIRT), "farmland trampled by a zombie's fall");
				level.setBlockAndUpdate(field, Blocks.AIR.defaultBlockState());

				// An oak grows taller than vanilla's six logs, with three blocks of air under the leaves
				BlockPos dirt = base.south(8);
				Feature oak = level.registryAccess().lookupOrThrow(Registries.FEATURE).get(TreeFeatures.OAK).orElseThrow().value();
				boolean grew = false;
				boolean grewTall = false;
				for (int seed = 0; seed < 24 && !grewTall; seed++) {
					clearTreeSite(level, dirt);
					BlockPos origin = dirt.above();
					if (!oak.place(level, level.getChunkSource().getGenerator(), RandomSource.create(seed), origin)) {
						continue;
					}
					grew = true;
					int leaves = 0;
					int logs = 0;
					boolean lowLeaf = false;
					boolean tallTrunk = false;
					for (int dx = -6; dx <= 6; dx++) {
						for (int dz = -6; dz <= 6; dz++) {
							for (int dy = 0; dy <= 16; dy++) {
								var state = level.getBlockState(origin.offset(dx, dy, dz));
								if (state.is(BlockTags.LEAVES)) {
									leaves++;
									if (dy < TreeClearance.AIR_UNDER_LEAVES) {
										lowLeaf = true;
									}
								}
								if (state.is(BlockTags.LOGS)) {
									logs++;
									if (dy >= 6) {
										tallTrunk = true;
									}
								}
							}
						}
					}
					check(leaves > 0 && logs > 0, "the oak grew logs and leaves");
					check(!lowLeaf, "no leaves in the three blocks above the sapling");
					grewTall = tallTrunk;
				}
				check(grew, "an oak tree grew");
				check(grewTall, "a tall oak's trunk rose past vanilla's six logs");
				clearTreeSite(level, dirt);
			});

			// A sweep of the sword hurts the sheep beside the zombie hit, not the player's own wolf
			world.getServer().runCommand("item replace entity @a weapon.mainhand with diamond_sword");
			context.waitTicks(30);
			world.getServer().runOnServer(server -> {
				ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
				ServerLevel level = player.level();
				Vec3 spot = player.position().add(0, 0, -2);
				Zombie zombie = spawn(level, new Zombie(level), spot);
				Wolf wolf = spawn(level, EntityTypes.WOLF.create(level, EntitySpawnReason.COMMAND), spot.add(0.8, 0, 0));
				wolf.tame(player);
				Sheep sheep = spawn(level, EntityTypes.SHEEP.create(level, EntitySpawnReason.COMMAND), spot.add(-0.8, 0, 0));
				player.lookAt(EntityAnchorArgument.Anchor.EYES, zombie.getEyePosition());
				player.attack(zombie);
				check(sheep.getHealth() < sheep.getMaxHealth(), "the sweep hurts the sheep beside");
				check(wolf.getHealth() == wolf.getMaxHealth(), "the sweep spares the player's wolf");
				zombie.discard();
				wolf.discard();
				sheep.discard();
			});

			// Sneaking, an empty hand swaps the whole armor with an armor stand's, a cursed piece kept on
			world.getServer().runOnServer(server -> {
				ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
				ServerLevel level = player.level();
				ArmorStand stand = new ArmorStand(level, player.getX() + 1, player.getY(), player.getZ());
				level.addFreshEntity(stand);
				stand.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
				stand.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.IRON_CHESTPLATE));
				player.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.DIAMOND_CHESTPLATE));
				ItemStack cursed = new ItemStack(Items.DIAMOND_BOOTS);
				cursed.enchant(level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.BINDING_CURSE), 1);
				player.setItemSlot(EquipmentSlot.FEET, cursed);
				player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
				player.setShiftKeyDown(true);
				stand.interact(player, InteractionHand.MAIN_HAND, new Vec3(0, 1, 0));
				player.setShiftKeyDown(false);
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

			// Clicking a sign hung on a chest opens the chest, sneaking edits the sign
			world.getServer().runOnServer(server -> {
				ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
				ServerLevel level = player.level();
				BlockPos chest = player.blockPosition().north(3);
				BlockPos sign = chest.west();
				level.setBlockAndUpdate(chest, Blocks.CHEST.defaultBlockState());
				level.setBlockAndUpdate(sign, Blocks.OAK_WALL_SIGN.defaultBlockState().setValue(WallSignBlock.FACING, Direction.WEST));
				player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
				BlockHitResult onSign = new BlockHitResult(Vec3.atCenterOf(sign), Direction.WEST, sign, false);
				player.gameMode.useItemOn(player, level, ItemStack.EMPTY, InteractionHand.MAIN_HAND, onSign);
				check(player.containerMenu instanceof ChestMenu, "the sign on the chest opens the chest");
				player.closeContainer();
				player.setShiftKeyDown(true);
				player.gameMode.useItemOn(player, level, ItemStack.EMPTY, InteractionHand.MAIN_HAND, onSign);
				player.setShiftKeyDown(false);
				check(!(player.containerMenu instanceof ChestMenu) && level.getBlockEntity(sign) instanceof SignBlockEntity edited
						&& player.getUUID().equals(edited.getPlayerWhoMayEdit()), "sneaking edits the sign instead");
				level.setBlockAndUpdate(sign, Blocks.AIR.defaultBlockState());
				level.setBlockAndUpdate(chest, Blocks.AIR.defaultBlockState());
			});

			// A torch in hand lights the dark around the player, here a closed stone room deep down
			world.getServer().runCommand("execute as @a at @s run tp @s ~ -40 ~ 0 50");
			world.getServer().runCommand("execute as @a at @s run fill ~-8 ~-1 ~-8 ~8 ~5 ~8 stone hollow");
			world.getServer().runCommand("item replace entity @a weapon.mainhand with air");
			context.runOnClient(client -> client.gui.hud.toggle());
			world.getConnection().waitForChunksRender();
			context.waitTicks(20);
			context.takeScreenshot("cassonade-dynamic-light-off");
			world.getServer().runCommand("item replace entity @a weapon.mainhand with torch");
			context.waitTicks(20);
			world.getConnection().waitForChunksRender();
			int light = context.computeOnClient(client -> DynamicLights.light(BlockPos.containing(client.player.getEyePosition())));
			check(light == Blocks.TORCH.defaultBlockState().getLightEmission(), "the torch's light carried with the player (" + light + ")");
			context.takeScreenshot("cassonade-dynamic-light-on");

			// A sculk torch in hand marks the dark floor where monsters can spawn, but not a lit block
			world.getServer().runCommand("execute as @a at @s run setblock ~-6 ~ ~-6 glowstone");
			world.getServer().runCommand("item replace entity @a weapon.mainhand with cassonade:sculk_torch");
			world.getServer().runCommand("execute as @a at @s run tp @s ~ ~ ~ 0 45");
			context.waitTicks(25);
			world.getConnection().waitForChunksRender();
			int spots = context.computeOnClient(client -> SpawnSpots.count());
			check(spots > 50, "the dark room's floor marked (" + spots + " spots)");
			check(context.computeOnClient(client -> SpawnSpots.isMarked(client.player.blockPosition().offset(3, 0, 3))), "the floor in front marked");
			check(!context.computeOnClient(client -> SpawnSpots.isMarked(client.player.blockPosition().offset(-5, 0, -6))), "no mark next to the glowstone");
			context.runOnClient(client -> client.gui.hud.toggle());
			context.waitTicks(2);
			context.takeScreenshot("cassonade-sculk-torch");
		}
	}

	private static <T extends Mob> T spawn(ServerLevel level, T mob, Vec3 at) {
		mob.setPos(at);
		mob.setNoAi(true);
		level.addFreshEntity(mob);
		return mob;
	}

	private static void use(ServerPlayer player, BlockPos ladder) {
		player.gameMode.useItemOn(player, player.level(), player.getMainHandItem(), InteractionHand.MAIN_HAND,
				new BlockHitResult(Vec3.atCenterOf(ladder), Direction.WEST, ladder, false));
	}

	private static void clearTreeSite(ServerLevel level, BlockPos dirt) {
		for (int dx = -8; dx <= 8; dx++) {
			for (int dz = -8; dz <= 8; dz++) {
				for (int dy = -1; dy <= 18; dy++) {
					level.setBlockAndUpdate(dirt.offset(dx, dy, dz), Blocks.AIR.defaultBlockState());
				}
			}
		}
		level.setBlockAndUpdate(dirt, Blocks.DIRT.defaultBlockState());
	}

	private static void check(boolean condition, String what) {
		if (!condition) {
			throw new AssertionError("Expected: " + what);
		}
	}
}
