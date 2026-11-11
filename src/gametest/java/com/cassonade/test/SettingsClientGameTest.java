package com.cassonade.test;

import java.util.List;
import java.util.Locale;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.locale.Language;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.HopperMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ComposterBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BuiltinStructureSets;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.phys.AABB;

import com.cassonade.client.settings.SettingsCategoryScreen;
import com.cassonade.client.settings.SettingsScreen;
import com.cassonade.settings.Setting;
import com.cassonade.settings.Settings;
import com.cassonade.tweaks.Towns;

/** The settings screen, and features turned off going back to vanilla, data from the mod's pack included. */
public class SettingsClientGameTest implements FabricClientGameTest {
	@Override
	public void runTest(ClientGameTestContext context) {
		if (TestSelection.skips(this)) {
			return;
		}
		context.getInput().resizeWindow(1920, 1080);
		// Every setting is named and explained in both languages the mod ships
		context.runOnClient(client -> {
			for (Setting setting : Setting.values()) {
				check(Language.getInstance().has(setting.key()) && Language.getInstance().has(setting.key() + ".tooltip"), setting.key() + " in en_us");
			}
		});
		context.setScreen(() -> new SettingsScreen(null));
		Screenshots.take(context, "settings");
		for (Setting.Category category : Setting.Category.values()) {
			context.setScreen(() -> new SettingsCategoryScreen(null, category));
			Screenshots.take(context, "settings-" + category.name().toLowerCase(Locale.ROOT).replace('_', '-'));
		}
		context.setScreen(() -> null);

		try {
			for (Setting setting : Setting.values()) {
				Settings.set(setting, false);
			}
			try (TestSingleplayerContext world = context.worldBuilder().create()) {
				world.getServer().runOnServer(server -> {
					ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
					ServerLevel level = server.overworld();

					ShapedRecipe sticks = (ShapedRecipe) server.getRecipeManager()
							.byKey(ResourceKey.create(Registries.RECIPE, Identifier.withDefaultNamespace("stick"))).orElseThrow().value();
					ItemStack planks = new ItemStack(Items.OAK_PLANKS);
					int count = sticks.assemble(CraftingInput.of(1, 2, List.of(planks, planks.copy()))).getCount();
					check(count == 4, "vanilla's 4 sticks (" + count + ")");

					check(server.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.PROTECTION).value().getMaxLevel() == 4,
							"Protection IV");

					BlockPos pos = player.blockPosition().above(5);
					level.setBlockAndUpdate(pos, Blocks.ENDER_CHEST.defaultBlockState());
					List<ItemStack> drops = Block.getDrops(level.getBlockState(pos), level, pos, level.getBlockEntity(pos), player, new ItemStack(Items.WOODEN_PICKAXE));
					check(drops.stream().allMatch(drop -> drop.is(Items.OBSIDIAN)) && !drops.isEmpty(), "obsidian from an ender chest (" + drops + ")");
					level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());

					BlockPos hopper = player.blockPosition().west(3);
					level.setBlockAndUpdate(hopper, Blocks.HOPPER.defaultBlockState());
					player.openMenu((MenuProvider) level.getBlockEntity(hopper));
					check(player.containerMenu instanceof HopperMenu, "the vanilla hopper window (" + player.containerMenu + ")");
					player.closeContainer();

					BlockPos composter = player.blockPosition().west(5);
					BlockState ready = Blocks.COMPOSTER.defaultBlockState().setValue(ComposterBlock.LEVEL, ComposterBlock.READY);
					level.setBlockAndUpdate(composter, ready);
					ComposterBlock.extractProduce(player, ready, level, composter);
					List<ItemEntity> boneMeal = level.getEntitiesOfClass(ItemEntity.class, new AABB(composter).inflate(2));
					int total = boneMeal.stream().filter(drop -> drop.getItem().is(Items.BONE_MEAL)).mapToInt(drop -> drop.getItem().getCount()).sum();
					check(total == 1, "a single bone meal (" + total + ")");
					boneMeal.forEach(ItemEntity::discard);

					// The other data overlays: recipes, tags and structures
					check(server.getRecipeManager().byKey(ResourceKey.create(Registries.RECIPE, Identifier.fromNamespaceAndPath("cassonade", "uncraft/oak_stairs"))).isEmpty(),
							"no uncrafting");
					check(!Blocks.CRYING_OBSIDIAN.defaultBlockState().is(BlockTags.NETHER_PORTAL_FRAME), "obsidian alone frames a portal");
					check(server.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.MENDING).is(EnchantmentTags.BOW_EXCLUSIVE),
							"Mending and Infinity apart");
					var sets = server.registryAccess().lookupOrThrow(Registries.STRUCTURE_SET);
					for (String set : List.of("lighthouse", "lone_dwellings", "pirate_booty")) {
						check(sets.get(ResourceKey.create(Registries.STRUCTURE_SET, Identifier.fromNamespaceAndPath("cassonade", set))).isEmpty(), "no " + set);
					}
					StructureSet villages = sets.getValueOrThrow(BuiltinStructureSets.VILLAGES);
					check(villages.structures().stream().noneMatch(entry -> entry.structure().is(Towns.TAG)), "no towns among the villages");
				});
			}
		} finally {
			for (Setting setting : Setting.values()) {
				Settings.set(setting, true);
			}
		}
	}

	private static void check(boolean condition, String what) {
		if (!condition) {
			throw new AssertionError("Expected: " + what);
		}
	}
}
