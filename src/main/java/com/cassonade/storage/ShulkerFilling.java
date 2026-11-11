package com.cassonade.storage;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;

import com.cassonade.Cassonade;
import com.cassonade.ServerSupport;
import com.cassonade.settings.Setting;
import com.cassonade.settings.Settings;

/**
 * A Shulker box in the inventory clicks like a bundle, stack on it or it on a stack, and a right click with nothing to
 * put in takes out the stack picked with the wheel, else the last. Unlike a bundle's, its clicks can't throw items out in
 * the world: using it places it. Both sides run this, the client predicting; on a server without the mod, clicks stay
 * vanilla. The stack picked is kept per player rather than on the item, whose component would have to be registered:
 * it only lasts while the box is hovered.
 */
public final class ShulkerFilling {
	public static final int SLOTS = 27;
	// The bundle's bar (BundleItem)
	private static final int BAR_COLOR = ARGB.colorFromFloat(1.0F, 0.44F, 0.53F, 1.0F);
	private static final int FULL_BAR_COLOR = ARGB.colorFromFloat(1.0F, 1.0F, 0.33F, 0.33F);
	private static final int BAR_WIDTH = 13;
	/** Kept apart per side: in singleplayer both run in one game, and players compare equal by their entity id. */
	private static final Map<UUID, Integer> selectedOnServer = new ConcurrentHashMap<>();
	private static volatile int selectedOnClient = -1;

	/** The wheel over a box: which of its stacks comes out next, -1 for none. */
	public record Select(int index) implements CustomPacketPayload {
		public static final Type<Select> TYPE = new Type<>(Cassonade.id("shulker_select"));
		public static final StreamCodec<RegistryFriendlyByteBuf, Select> STREAM_CODEC = ByteBufCodecs.VAR_INT.map(Select::new, Select::index).cast();

		@Override
		public Type<Select> type() {
			return TYPE;
		}
	}

	private ShulkerFilling() {
	}

	public static void init() {
		PayloadTypeRegistry.serverboundPlay().register(Select.TYPE, Select.STREAM_CODEC);
		ServerPlayNetworking.registerGlobalReceiver(Select.TYPE, (payload, context) -> select(context.player(), payload.index()));
	}

	public static void select(Player player, int index) {
		if (player.level().isClientSide()) {
			selectedOnClient = index;
		} else {
			selectedOnServer.put(player.getUUID(), index);
		}
	}

	public static int selected(Player player) {
		return player.level().isClientSide() ? selectedOnClient : selectedOnServer.getOrDefault(player.getUUID(), -1);
	}

	public static boolean isBox(ItemStack stack) {
		return stack.is(ItemTags.SHULKER_BOXES);
	}

	/** Whether a box takes these clicks. An unopened loot box stays closed: filling it would throw away its loot table. */
	public static boolean clicksLikeBundle(ItemStack stack, Player player) {
		return isBox(stack) && stack.getCount() == 1 && !stack.has(DataComponents.CONTAINER_LOOT) && ServerSupport.has(player)
				&& Settings.on(Setting.SHULKER_CLICKS);
	}

	public static int stacksIn(ItemStack box) {
		return (int) box.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY).nonEmptyItemCopyStream().count();
	}

	/** The box on the cursor, clicking a slot. Whether the click was for the box. */
	public static boolean clickSlot(ItemStack box, Slot slot, ClickAction action, Player player) {
		if (!clicksLikeBundle(box, player)) {
			return false;
		}
		ItemStack stack = slot.getItem();
		if (!stack.isEmpty()) {
			boolean canTake = !LockedSlots.isLocked(slot) && slot.allowModification(player);
			int moved = canTake ? insert(box, stack, stack.getCount()) : 0;
			if (moved > 0) {
				slot.safeTake(moved, moved, player);
			}
			sound(player, moved > 0 ? SoundEvents.BUNDLE_INSERT : SoundEvents.BUNDLE_INSERT_FAIL);
			return true;
		}
		if (action == ClickAction.SECONDARY) {
			ItemStack removed = removeOne(box, player);
			if (!removed.isEmpty()) {
				ItemStack left = slot.safeInsert(removed);
				if (!left.isEmpty()) {
					insert(box, left, left.getCount());
				} else {
					sound(player, SoundEvents.BUNDLE_REMOVE_ONE);
				}
			}
			return true;
		}
		return false;
	}

	/** A click on the box in its slot, with this on the cursor. Whether the click was for the box. */
	public static boolean clickBox(ItemStack box, ItemStack carried, Slot slot, ClickAction action, Player player, SlotAccess cursor) {
		if (!clicksLikeBundle(box, player)) {
			return false;
		}
		if (action == ClickAction.PRIMARY && !carried.isEmpty()) {
			int moved = slot.allowModification(player) ? insert(box, carried, carried.getCount()) : 0;
			carried.shrink(moved);
			sound(player, moved > 0 ? SoundEvents.BUNDLE_INSERT : SoundEvents.BUNDLE_INSERT_FAIL);
			return true;
		}
		if (action == ClickAction.SECONDARY && carried.isEmpty()) {
			if (slot.allowModification(player)) {
				ItemStack removed = removeOne(box, player);
				if (!removed.isEmpty()) {
					cursor.set(removed);
					sound(player, SoundEvents.BUNDLE_REMOVE_ONE);
				}
			}
			return true;
		}
		return false;
	}

	/** Puts up to this many in, what the box already holds first, then its free slots. Returns how many went in. */
	private static int insert(ItemStack box, ItemStack stack, int count) {
		if (!stack.getItem().canFitInsideContainerItems()) {
			return 0;
		}
		NonNullList<ItemStack> contents = read(box);
		int left = count;
		for (int i = 0; i < SLOTS && left > 0; i++) {
			ItemStack held = contents.get(i);
			if (ItemStack.isSameItemSameComponents(held, stack) && held.getCount() < held.getMaxStackSize()) {
				int added = Math.min(left, held.getMaxStackSize() - held.getCount());
				held.grow(added);
				left -= added;
			}
		}
		for (int i = 0; i < SLOTS && left > 0; i++) {
			if (contents.get(i).isEmpty()) {
				int added = Math.min(left, stack.getMaxStackSize());
				contents.set(i, stack.copyWithCount(added));
				left -= added;
			}
		}
		if (left < count) {
			box.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(contents));
		}
		return count - left;
	}

	/** The stack picked with the wheel, counted among the stacks as the tooltip shows them, else the last one in. */
	private static ItemStack removeOne(ItemStack box, Player player) {
		NonNullList<ItemStack> contents = read(box);
		List<Integer> filled = java.util.stream.IntStream.range(0, SLOTS).filter(i -> !contents.get(i).isEmpty()).boxed().toList();
		if (filled.isEmpty()) {
			return ItemStack.EMPTY;
		}
		int picked = selected(player);
		int slot = filled.get(picked >= 0 && picked < filled.size() ? picked : filled.size() - 1);
		ItemStack removed = contents.get(slot);
		contents.set(slot, ItemStack.EMPTY);
		box.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(contents));
		select(player, -1);
		return removed;
	}

	private static NonNullList<ItemStack> read(ItemStack box) {
		NonNullList<ItemStack> contents = NonNullList.withSize(SLOTS, ItemStack.EMPTY);
		box.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY).copyInto(contents);
		return contents;
	}

	private static void sound(Player player, SoundEvent sound) {
		player.playSound(sound, 0.8F, 0.8F + player.level().getRandom().nextFloat() * 0.4F);
	}

	public static boolean isBarVisible(ItemStack box) {
		return isBox(box) && Settings.on(Setting.SHULKER_CLICKS) && stacksIn(box) > 0;
	}

	public static int barWidth(ItemStack box) {
		return Math.min(1 + Mth.floor((BAR_WIDTH - 1) * (float) stacksIn(box) / SLOTS), BAR_WIDTH);
	}

	public static int barColor(ItemStack box) {
		return stacksIn(box) >= SLOTS ? FULL_BAR_COLOR : BAR_COLOR;
	}
}
