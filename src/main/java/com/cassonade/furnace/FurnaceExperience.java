package com.cassonade.furnace;

import java.util.Map;
import java.util.WeakHashMap;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractFurnaceMenu;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;

import com.cassonade.Cassonade;
import com.cassonade.mixin.AbstractFurnaceBlockEntityAccessor;
import com.cassonade.mixin.AbstractFurnaceMenuContainerAccessor;

/**
 * The experience a furnace, blast furnace or smoker holds, sent to the player who has it open: only the server
 * knows what was cooked in it. It is the sum of every cooked item's recipe experience, sent in tenths since
 * a recipe gives fractions (taking the result out turns the fraction into a chance of one more point).
 */
public record FurnaceExperience(int containerId, int tenths) implements CustomPacketPayload {
	public static final Type<FurnaceExperience> TYPE = new Type<>(Cassonade.id("furnace_experience"));
	public static final StreamCodec<RegistryFriendlyByteBuf, FurnaceExperience> STREAM_CODEC =
			StreamCodec.composite(ByteBufCodecs.VAR_INT, FurnaceExperience::containerId, ByteBufCodecs.VAR_INT, FurnaceExperience::tenths,
					FurnaceExperience::new);
	private static final int CHECK_INTERVAL = 10;
	private static final Map<ServerPlayer, int[]> SENT = new WeakHashMap<>();

	public static void init() {
		PayloadTypeRegistry.clientboundPlay().register(TYPE, STREAM_CODEC);
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			if (server.getTickCount() % CHECK_INTERVAL != 0) {
				return;
			}
			for (ServerPlayer player : server.getPlayerList().getPlayers()) {
				check(player);
			}
		});
	}

	private static void check(ServerPlayer player) {
		if (!(player.containerMenu instanceof AbstractFurnaceMenu menu)
				|| !(((AbstractFurnaceMenuContainerAccessor) menu).cassonade$getContainer() instanceof AbstractFurnaceBlockEntity furnace)
				|| !ServerPlayNetworking.canSend(player, TYPE)) {
			SENT.remove(player);
			return;
		}
		int tenths = tenths(player.level(), furnace);
		int[] last = SENT.get(player);
		// Sent again for each window opened, even when it holds the same: the client forgets it on closing
		if (last == null || last[0] != menu.containerId || last[1] != tenths) {
			SENT.put(player, new int[] {menu.containerId, tenths});
			ServerPlayNetworking.send(player, new FurnaceExperience(menu.containerId, tenths));
		}
	}

	static int tenths(ServerLevel level, AbstractFurnaceBlockEntity furnace) {
		float total = 0;
		for (var entry : ((AbstractFurnaceBlockEntityAccessor) furnace).cassonade$getRecipesUsed().reference2IntEntrySet()) {
			var recipe = level.recipeAccess().byKey(entry.getKey());
			if (recipe.isPresent() && recipe.get().value() instanceof AbstractCookingRecipe cooking) {
				total += entry.getIntValue() * cooking.experience();
			}
		}
		return Math.round(total * 10);
	}

	@Override
	public Type<FurnaceExperience> type() {
		return TYPE;
	}
}
