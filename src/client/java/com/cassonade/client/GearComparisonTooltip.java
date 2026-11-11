package com.cassonade.client;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.jspecify.annotations.Nullable;

import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.equipment.Equippable;

import com.cassonade.settings.Setting;
import com.cassonade.settings.Settings;

/**
 * With Shift held, a weapon's, tool's or armor's tooltip ends with how it compares with what the player wears or holds there:
 * the difference in each attribute, worded and colored as the game's own lines. The items' attributes only, their enchantments
 * aside. Client only: it reads what the item carries.
 */
public final class GearComparisonTooltip {
	/** Under this, a difference is only rounding. */
	private static final double NONE = 0.005;
	/** The theme's SELECT and MOB_HEALTH_LIGHT (tools/gui_theme.py), readable on the tooltip's dark ground. */
	private static final int BETTER = 0xFF7CA72E;
	private static final int WORSE = 0xFFF26456;

	private GearComparisonTooltip() {
	}

	public static void append(ItemStack stack, Item.TooltipContext context, TooltipFlag flag, List<Component> lines) {
		Minecraft minecraft = Minecraft.getInstance();
		Player player = minecraft.player;
		// Tooltips are also built off the render thread, for the search, where no key is held
		if (player == null || !RenderSystem.isOnRenderThread() || !minecraft.hasShiftDown() || !Settings.on(Setting.GEAR_COMPARISON)) {
			return;
		}
		EquipmentSlot slot = slotOf(stack);
		ItemStack worn = slot != null ? player.getItemBySlot(slot) : ItemStack.EMPTY;
		// The very stack worn, or an item with nothing to compare there, as elytra in the chest's place
		if (slot == null || worn == stack) {
			return;
		}
		Map<Holder<Attribute>, Double> difference = new LinkedHashMap<>();
		add(stack, slot, 1, difference);
		if (difference.isEmpty()) {
			return;
		}
		add(worn, slot, -1, difference);
		List<Component> compared = new ArrayList<>();
		difference.forEach((attribute, amount) -> {
			if (Math.abs(amount) >= NONE) {
				compared.add(line(attribute, amount));
			}
		});
		if (compared.isEmpty()) {
			return;
		}
		Component header = !worn.isEmpty() ? Component.translatable("gui.cassonade.compare", worn.getHoverName())
				: Component.translatable(slot == EquipmentSlot.MAINHAND ? "gui.cassonade.compare.empty_hand" : "gui.cassonade.compare.nothing_worn");
		lines.add(header.copy().withStyle(style -> style.withColor(DescriptionText.COLOR).withInsertion(DescriptionText.HEADING)));
		lines.addAll(compared);
	}

	/** Where the item goes: its armor slot, or the main hand for what deals damage. */
	private static @Nullable EquipmentSlot slotOf(ItemStack stack) {
		Equippable equippable = stack.get(DataComponents.EQUIPPABLE);
		if (equippable != null && equippable.slot().getType() == EquipmentSlot.Type.HUMANOID_ARMOR) {
			return equippable.slot();
		}
		boolean[] deals = {false};
		stack.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY).forEach(EquipmentSlot.MAINHAND,
				(attribute, modifier) -> deals[0] |= attribute.value() == Attributes.ATTACK_DAMAGE.value());
		return deals[0] ? EquipmentSlot.MAINHAND : null;
	}

	/** The item's flat bonuses in that slot, the way the game adds them up. */
	private static void add(ItemStack stack, EquipmentSlot slot, int sign, Map<Holder<Attribute>, Double> into) {
		stack.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY).forEach(slot, (attribute, modifier) -> {
			if (modifier.operation() == AttributeModifier.Operation.ADD_VALUE) {
				into.merge(attribute, sign * modifier.amount(), Double::sum);
			}
		});
	}

	/**
	 * As the game's "+2 Armor", in cream as the values above it (ItemStackAttributesMixin), the difference alone in green when
	 * better and red when worse, as the game judges each attribute. Knockback resistance in tenths, as the game shows it.
	 */
	private static Component line(Holder<Attribute> attribute, double amount) {
		double shown = attribute.value() == Attributes.KNOCKBACK_RESISTANCE.value() ? amount * 10 : amount;
		// The game's own verdict: blue for better, red for worse, gray when neither
		ChatFormatting verdict = attribute.value().getStyle(amount > 0);
		Component difference = Component.literal((shown > 0 ? "+" : "-") + ItemAttributeModifiers.ATTRIBUTE_MODIFIER_FORMAT.format(Math.abs(shown)))
				.withColor(verdict == ChatFormatting.BLUE ? BETTER : verdict == ChatFormatting.RED ? WORSE : DescriptionText.NAME_COLOR);
		return Component.translatable("gui.cassonade.compare.line", difference, Component.translatable(attribute.value().getDescriptionId()))
				.withColor(DescriptionText.NAME_COLOR);
	}
}
