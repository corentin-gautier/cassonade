package com.cassonade.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.component.DataComponents;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.ItemStack;

/**
 * Soft sounds for what vanilla does silently in a container window: taking a stack, half of one,
 * putting it down, swapping, moving it across with shift or a hotbar key, throwing it out, spreading
 * it over slots (a tick for each slot the drag takes in, then the stack put down), sorting and putting away, locking a slot; and changing the hotbar's selected slot. They are the
 * bundle's, which already sound like an inventory; the bundle's own clicks keep theirs.
 */
public final class InventorySounds {
	private enum Action {
		TAKE(SoundEvents.BUNDLE_REMOVE_ONE, 1.1F, 0.5F),
		TAKE_HALF(SoundEvents.BUNDLE_REMOVE_ONE, 1.4F, 0.5F),
		PLACE(SoundEvents.BUNDLE_INSERT, 1.1F, 0.5F),
		SWAP(SoundEvents.BUNDLE_REMOVE_ONE, 0.9F, 0.5F),
		MOVE(SoundEvents.BUNDLE_INSERT, 1.3F, 0.4F),
		SPREAD(SoundEvents.BUNDLE_INSERT, 1.5F, 0.3F),
		THROW(SoundEvents.BUNDLE_DROP_CONTENTS, 1.4F, 0.35F),
		STORE(SoundEvents.BUNDLE_DROP_CONTENTS, 1.0F, 0.5F),
		SELECT(SoundEvents.BUNDLE_INSERT, 1.8F, 0.2F),
		LOCK(SoundEvents.CHAIN_PLACE, 1.6F, 0.35F),
		UNLOCK(SoundEvents.CHAIN_PLACE, 1.3F, 0.35F);

		final SoundEvent sound;
		final float pitch;
		final float volume;

		Action(SoundEvent sound, float pitch, float volume) {
			this.sound = sound;
			this.pitch = pitch;
			this.volume = volume;
		}
	}

	private static final float PITCH_SPREAD = 0.1F;
	private static final RandomSource RANDOM = RandomSource.create();

	/** The hotbar slot selected at the last tick, -1 out of a world. */
	private static int selected = -1;

	private InventorySounds() {
	}

	/** A soft tick when the hotbar's selected slot changes, by the wheel or the number keys. */
	public static void tick(Minecraft minecraft) {
		if (minecraft.player == null) {
			selected = -1;
			return;
		}
		int now = minecraft.player.getInventory().getSelectedSlot();
		if (selected != -1 && now != selected) {
			play(Action.SELECT);
		}
		selected = now;
	}

	/** After a click in a container window, from the carried stack and the slot's before and after it. */
	public static void clicked(ContainerInput input, int button, ItemStack carriedBefore, ItemStack slotBefore, ItemStack carriedAfter,
			ItemStack slotAfter) {
		Action action = action(input, button, carriedBefore, slotBefore, carriedAfter, slotAfter);
		if (action != null) {
			play(action);
		}
	}

	/** One more slot taken in by a drag that spreads the carried stack. */
	public static void spread() {
		play(Action.SPREAD);
	}

	/** A container or the inventory sorted, or the inventory put away in the container. */
	public static void stored() {
		play(Action.STORE);
	}

	/** The inventory's rows rotated into the hotbar (HotbarRows). */
	public static void rotated() {
		play(Action.SWAP);
	}

	/** A slot locked, or unlocked: a light clink of metal. */
	public static void locked(boolean locked) {
		play(locked ? Action.LOCK : Action.UNLOCK);
	}

	private static Action action(ContainerInput input, int button, ItemStack carriedBefore, ItemStack slotBefore, ItemStack carriedAfter,
			ItemStack slotAfter) {
		boolean slotChanged = !ItemStack.matches(slotBefore, slotAfter);
		boolean carriedChanged = !ItemStack.matches(carriedBefore, carriedAfter);
		switch (input) {
			case PICKUP -> {
				// A bundle clicked with or on a stack sounds by itself
				if (isBundle(carriedBefore) || isBundle(slotBefore) && !carriedBefore.isEmpty()) {
					return null;
				}
				if (!carriedChanged) {
					return null;
				}
				if (carriedBefore.isEmpty()) {
					return button == 1 ? Action.TAKE_HALF : Action.TAKE;
				}
				if (carriedAfter.isEmpty() || ItemStack.isSameItemSameComponents(carriedBefore, carriedAfter)) {
					return carriedAfter.getCount() > carriedBefore.getCount() ? Action.TAKE : Action.PLACE;
				}
				return Action.SWAP;
			}
			case PICKUP_ALL -> {
				return carriedChanged ? Action.TAKE : null;
			}
			case QUICK_MOVE, SWAP -> {
				return slotChanged ? Action.MOVE : null;
			}
			case THROW -> {
				return slotChanged || carriedChanged ? Action.THROW : null;
			}
			case QUICK_CRAFT -> {
				// Once, when the stack is spread over the slots dragged across
				return AbstractContainerMenu.getQuickcraftHeader(button) == AbstractContainerMenu.QUICKCRAFT_HEADER_END && carriedChanged
						? Action.PLACE : null;
			}
			default -> {
				return null;
			}
		}
	}

	private static boolean isBundle(ItemStack stack) {
		return stack.has(DataComponents.BUNDLE_CONTENTS);
	}

	private static void play(Action action) {
		float pitch = action.pitch + (RANDOM.nextFloat() - 0.5F) * PITCH_SPREAD;
		Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(action.sound, pitch, action.volume));
	}
}
