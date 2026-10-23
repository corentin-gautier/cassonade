package com.vanillasugar.crafting;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractCraftingMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.display.RecipeDisplayId;

import com.vanillasugar.VanillaSugar;

/**
 * Sent by a control click on a recipe of the recipe book: crafts it as many times as the inventory allows,
 * each round filling the grid as a shift click does then taking the result as a shift click on it does.
 */
public record CraftAllPayload(int containerId, RecipeDisplayId recipe) implements CustomPacketPayload {
	public static final Type<CraftAllPayload> TYPE = new Type<>(VanillaSugar.id("craft_all"));
	public static final StreamCodec<RegistryFriendlyByteBuf, CraftAllPayload> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT, CraftAllPayload::containerId,
			RecipeDisplayId.STREAM_CODEC.cast(), CraftAllPayload::recipe,
			CraftAllPayload::new);
	/** A full grid of 64 crafts each round: enough for a whole inventory of ingredients, with room to spare. */
	private static final int MAX_ROUNDS = 64;

	public static void init() {
		PayloadTypeRegistry.serverboundPlay().register(TYPE, STREAM_CODEC);
		ServerPlayNetworking.registerGlobalReceiver(TYPE, (payload, context) -> payload.apply(context.player()));
	}

	@Override
	public Type<CraftAllPayload> type() {
		return TYPE;
	}

	/** The same checks as the game's recipe placement, then the rounds. */
	private void apply(ServerPlayer player) {
		player.resetLastActionTime();
		if (player.isSpectator() || !(player.containerMenu instanceof AbstractCraftingMenu menu) || menu.containerId != containerId
				|| !menu.stillValid(player)) {
			return;
		}
		RecipeManager.ServerDisplayInfo displayInfo = player.level().getServer().getRecipeManager().getRecipeFromDisplay(recipe);
		if (displayInfo == null) {
			return;
		}
		RecipeHolder<?> holder = displayInfo.parent();
		if (!player.getRecipeBook().contains(holder.id()) || holder.value().placementInfo().isImpossibleToPlace()) {
			return;
		}
		Slot result = menu.getResultSlot();
		for (int round = 0; round < MAX_ROUNDS; round++) {
			menu.handlePlacement(true, player.isCreative(), holder, player.level(), player.getInventory());
			ItemStack crafted = result.getItem().copy();
			if (crafted.isEmpty()) {
				break;
			}
			menu.clicked(result.index, 0, ContainerInput.QUICK_MOVE, player);
			// The inventory is full: what is left stays in the grid
			if (ItemStack.isSameItemSameComponents(result.getItem(), crafted)) {
				break;
			}
		}
	}
}
