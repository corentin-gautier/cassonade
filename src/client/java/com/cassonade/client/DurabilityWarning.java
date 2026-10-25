package com.cassonade.client;

import java.util.EnumMap;
import java.util.Map;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

/**
 * Warns before an enchanted tool, weapon or armor piece breaks: when it runs low (5 % of its
 * durability, at least 10 uses), then once more right before it breaks. A soft crack, a message above
 * the hotbar, and the item blinking red in the hotbar when it is there. Only as it wears: an item
 * already worn when taken in hand stays quiet.
 */
public final class DurabilityWarning {
	private static final int LAST_USES = 3;
	/** How long the item blinks, in ticks, and how fast. */
	private static final int BLINK_TICKS = 60;
	private static final float BLINK_SPEED = 0.6F;
	/** The theme's MOB_HEALTH red (tools/gui_theme.py). */
	private static final int BLINK_COLOR = 0xCD3336;
	private static final int TEXT_COLOR = 0xFFF26456;

	/** What was in each equipment slot at the last tick, to tell wear from an item taken in hand. */
	private static final Map<EquipmentSlot, ItemStack> LAST = new EnumMap<>(EquipmentSlot.class);
	/** The hotbar slot selected at the last tick: another one in hand is another item, however alike. */
	private static int lastSelected = -1;
	/** The warned item and the ticks left of its blinking. */
	private static ItemStack blinking = ItemStack.EMPTY;
	private static int blinkTicks;

	private DurabilityWarning() {
	}

	public static void tick(Minecraft minecraft) {
		LocalPlayer player = minecraft.player;
		if (player == null) {
			LAST.clear();
			lastSelected = -1;
			return;
		}
		if (blinkTicks > 0) {
			blinkTicks--;
		}
		int selected = player.getInventory().getSelectedSlot();
		boolean sameHand = selected == lastSelected;
		lastSelected = selected;
		for (EquipmentSlot slot : EquipmentSlot.values()) {
			ItemStack now = player.getItemBySlot(slot);
			ItemStack before = LAST.put(slot, now.copy());
			if (before != null && (sameHand || slot != EquipmentSlot.MAINHAND) && worn(before, now)) {
				int threshold = threshold(now);
				int left = now.getMaxDamage() - now.getDamageValue();
				int leftBefore = before.getMaxDamage() - before.getDamageValue();
				if (crossed(leftBefore, left, threshold) || crossed(leftBefore, left, LAST_USES)) {
					warn(minecraft, now);
				}
			}
		}
	}

	/** The same enchanted item, a little more damaged. */
	private static boolean worn(ItemStack before, ItemStack now) {
		return now.isDamageableItem() && now.isEnchanted() && before.is(now.getItem()) && now.getDamageValue() > before.getDamageValue();
	}

	private static int threshold(ItemStack stack) {
		return Math.max(10, stack.getMaxDamage() / 20);
	}

	private static boolean crossed(int before, int now, int threshold) {
		return before > threshold && now <= threshold;
	}

	private static void warn(Minecraft minecraft, ItemStack stack) {
		minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.ITEM_BREAK.value(), 1.8F, 0.3F));
		minecraft.gui.hud.setOverlayMessage(Component.translatable("gui.cassonade.durability.low", stack.getHoverName())
				.withColor(TEXT_COLOR), false);
		blinking = stack;
		blinkTicks = BLINK_TICKS;
	}

	/** The warned item blinking red in its hotbar or offhand slot, drawn over the hotbar. */
	public static void extractRenderState(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
		Minecraft minecraft = Minecraft.getInstance();
		LocalPlayer player = minecraft.player;
		if (blinkTicks <= 0 || player == null) {
			return;
		}
		float time = blinkTicks - deltaTracker.getGameTimeDeltaPartialTick(false);
		int alpha = Math.round((0.5F - 0.5F * Mth.cos(time * BLINK_SPEED)) * 0x90);
		int color = alpha << 24 | BLINK_COLOR;
		// The hotbar's slots, as vanilla lays them out
		int center = graphics.guiWidth() / 2;
		int y = graphics.guiHeight() - 19;
		Inventory inventory = player.getInventory();
		for (int i = 0; i < Inventory.SELECTION_SIZE; i++) {
			if (ItemStack.isSameItem(inventory.getItem(i), blinking)) {
				int x = center - 90 + i * 20 + 2;
				graphics.fill(x, y, x + 16, y + 16, color);
				return;
			}
		}
		if (ItemStack.isSameItem(player.getOffhandItem(), blinking)) {
			int x = player.getMainArm() == HumanoidArm.RIGHT ? center - 91 - 26 : center + 91 + 10;
			graphics.fill(x, y, x + 16, y + 16, color);
		}
	}
}
