package com.cassonade.combat;

import net.fabricmc.fabric.api.item.v1.DefaultItemComponentEvents;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import com.cassonade.ServerSupport;
import com.cassonade.settings.Setting;
import com.cassonade.settings.Settings;

/**
 * Netherite set further apart from diamond. The sword hits for 10 instead of 8, so most monsters (20
 * health) fall in two hits. The pickaxe breaks every deepslate block as fast as stone: with Efficiency V
 * and Haste II, the combination that already breaks stone at once, deepslate goes at once too.
 */
public final class Netherite {
	/** Every deepslate block, ores and building blocks included; reinforced deepslate cannot be broken. */
	public static final TagKey<Block> DEEPSLATE = TagKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath("cassonade", "deepslate"));
	private static final float SWORD_DAMAGE = 9.0F;
	private static final float SWORD_SPEED = -2.4F;
	private static final float STONE_HARDNESS = 1.5F;

	private Netherite() {
	}

	public static void init() {
		// The items' default components are set once, as the game starts
		if (!Settings.local(Setting.NETHERITE_SWORD)) {
			return;
		}
		DefaultItemComponentEvents.MODIFY.register(context -> context.modify(Items.NETHERITE_SWORD,
				builder -> builder.set(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.builder()
						.add(Attributes.ATTACK_DAMAGE, new AttributeModifier(Item.BASE_ATTACK_DAMAGE_ID, SWORD_DAMAGE, AttributeModifier.Operation.ADD_VALUE),
								EquipmentSlotGroup.MAINHAND)
						.add(Attributes.ATTACK_SPEED, new AttributeModifier(Item.BASE_ATTACK_SPEED_ID, SWORD_SPEED, AttributeModifier.Operation.ADD_VALUE),
								EquipmentSlotGroup.MAINHAND)
						.build())));
	}

	/** The player's mining speed on this block, scaled so a netherite pickaxe treats deepslate as stone. */
	public static float destroySpeed(Player player, BlockState state, float speed) {
		if (!state.is(DEEPSLATE) || !player.getMainHandItem().is(Items.NETHERITE_PICKAXE) || !ServerSupport.has(player)) {
			return speed;
		}
		float hardness = state.getDestroySpeed(EmptyBlockGetter.INSTANCE, BlockPos.ZERO);
		return hardness > STONE_HARDNESS ? speed * hardness / STONE_HARDNESS : speed;
	}
}
