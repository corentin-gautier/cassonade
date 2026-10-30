package com.cassonade.gear;

import net.fabricmc.fabric.api.loot.v3.LootTableEvents;

import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceCondition;

import com.cassonade.item.CassonadeItems;

/**
 * The backpack with its upgrade, a template found only in the late game's chests: unlike the plain one, whose
 * contents wait for it to be opened, it refills the hand (HandRefill), lends its items to the recipe book
 * (InventoryMixin, ServerPlaceRecipeMixin) and tops up its own stacks with what is picked up (ItemEntityMixin).
 */
public final class OrganizedBackpack {
	private OrganizedBackpack() {
	}

	public static void init() {
		LootTableEvents.MODIFY.register((key, table, source, registries) -> {
			if (source.isBuiltin()) {
				addTemplate(key, table, BuiltInLootTables.ANCIENT_CITY, 0.08F);
				addTemplate(key, table, BuiltInLootTables.BASTION_TREASURE, 0.1F);
				addTemplate(key, table, BuiltInLootTables.END_CITY_TREASURE, 0.05F);
			}
		});
	}

	private static void addTemplate(ResourceKey<LootTable> key, LootTable.Builder table, ResourceKey<LootTable> chest, float chance) {
		if (key.equals(chest)) {
			table.withPool(LootPool.lootPool()
					.when(LootItemRandomChanceCondition.randomChance(chance))
					.add(LootItem.lootTableItem(CassonadeItems.BACKPACK_UPGRADE_TEMPLATE)));
		}
	}

	/** The contents of the organized backpack worn, null with the plain one or none. */
	public static BackpackContainer of(Player player) {
		return PlayerGear.of(player).backpack().is(CassonadeItems.ORGANIZED_BACKPACK)
				? ((BackpackMenu) player.inventoryMenu).cassonade$backpack() : null;
	}

	/** Adds to the stacks already there, never to an empty slot, and shrinks the stack by what went in. */
	public static void topUp(BackpackContainer backpack, ItemStack stack) {
		for (int i = 0; i < backpack.getContainerSize() && !stack.isEmpty(); i++) {
			ItemStack present = backpack.getItem(i);
			if (!present.isEmpty() && present.getCount() < present.getMaxStackSize() && ItemStack.isSameItemSameComponents(present, stack)) {
				int moved = Math.min(stack.getCount(), present.getMaxStackSize() - present.getCount());
				present.grow(moved);
				stack.shrink(moved);
				backpack.setChanged();
			}
		}
	}

	/** The stack most like the one used up, taken out whole, as the inventory's: the same components first. */
	public static ItemStack takeForHand(BackpackContainer backpack, ItemStack used) {
		int found = -1;
		for (int i = 0; i < backpack.getContainerSize(); i++) {
			ItemStack stack = backpack.getItem(i);
			if (stack.isEmpty() || !stack.is(used.getItem())) {
				continue;
			}
			if (ItemStack.isSameItemSameComponents(stack, used)) {
				found = i;
				break;
			}
			if (found < 0) {
				found = i;
			}
		}
		return found >= 0 ? backpack.removeItemNoUpdate(found) : ItemStack.EMPTY;
	}

	/** Up to count of an ingredient, under the rules of Inventory.findSlotMatchingCraftingIngredient. */
	public static ItemStack takeIngredient(BackpackContainer backpack, Holder<Item> item, ItemStack existing, int count) {
		for (int i = 0; i < backpack.getContainerSize(); i++) {
			ItemStack stack = backpack.getItem(i);
			if (!stack.isEmpty() && stack.is(item) && isPlain(stack)
					&& (existing.isEmpty() || ItemStack.isSameItemSameComponents(existing, stack))) {
				return backpack.removeItem(i, count);
			}
		}
		return ItemStack.EMPTY;
	}

	/** What the recipe book may use, as Inventory.isUsableForCrafting: nothing worn, enchanted or renamed. */
	private static boolean isPlain(ItemStack stack) {
		return !stack.isDamaged() && !stack.isEnchanted() && !stack.has(DataComponents.CUSTOM_NAME);
	}
}
