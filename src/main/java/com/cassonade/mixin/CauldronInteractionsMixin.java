package com.cassonade.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.core.BlockPos;
import net.minecraft.core.cauldron.CauldronInteractions;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ColorCollection;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.state.BlockState;

import com.cassonade.ServerSupport;

/**
 * A water cauldron washes dyed wool, carpets, beds, terracotta and glass back to white or plain, as vanilla
 * already does for leather armor, banners and shulker boxes. A whole stack is washed for one level of water,
 * since doing it one block at a time would be the annoyance. Without the mod on the server, vanilla.
 */
@Mixin(CauldronInteractions.class)
public abstract class CauldronInteractionsMixin {
	@Inject(method = "bootStrap", at = @At("TAIL"))
	private static void cassonade$washDyedBlocks(CallbackInfo ci) {
		CauldronDispatcherAccessor water = (CauldronDispatcherAccessor) CauldronInteractions.WATER;
		wash(water, Items.WOOL, Items.WOOL.pick(DyeColor.WHITE));
		wash(water, Items.CARPET, Items.CARPET.pick(DyeColor.WHITE));
		wash(water, Items.BED, Items.BED.pick(DyeColor.WHITE));
		wash(water, Items.STAINED_GLASS, Items.GLASS);
		wash(water, Items.STAINED_GLASS_PANE, Items.GLASS_PANE);
		wash(water, Items.DYED_TERRACOTTA, Items.TERRACOTTA);
	}

	private static void wash(CauldronDispatcherAccessor water, ColorCollection<Item> dyed, Item clean) {
		dyed.forEach(item -> {
			if (item != clean) {
				water.cassonade$put(item, (state, level, pos, player, hand, stack) -> washStack(state, level, pos, player, hand, stack, clean));
			}
		});
	}

	private static InteractionResult washStack(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
			ItemStack stack, Item clean) {
		if (!ServerSupport.has(player)) {
			return InteractionResult.TRY_WITH_EMPTY_HAND;
		}
		if (!level.isClientSide()) {
			player.setItemInHand(hand, stack.transmuteCopy(clean, stack.getCount()));
			player.awardStat(Stats.CLEAN_ARMOR);
			LayeredCauldronBlock.lowerFillLevel(state, level, pos);
		}
		return InteractionResult.SUCCESS;
	}
}
