package com.cassonade.tweaks;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.PaintingVariantTags;
import net.minecraft.world.entity.decoration.painting.Painting;
import net.minecraft.world.entity.decoration.painting.PaintingVariant;
import net.minecraft.world.item.Items;

import com.cassonade.Cassonade;
import com.cassonade.mixin.PaintingAccessor;
import com.cassonade.settings.Setting;
import com.cassonade.settings.Settings;

/** The scroll wheel on a painting: the server hangs the next variant that fits where this one is, step being 1 or -1. */
public record ChoosePaintingPayload(int painting, int step) implements CustomPacketPayload {
	public static final Type<ChoosePaintingPayload> TYPE = new Type<>(Cassonade.id("choose_painting"));
	public static final StreamCodec<RegistryFriendlyByteBuf, ChoosePaintingPayload> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT, ChoosePaintingPayload::painting,
			ByteBufCodecs.VAR_INT, ChoosePaintingPayload::step,
			ChoosePaintingPayload::new);
	private static final double REACH_MARGIN = 2;

	public static void init() {
		PayloadTypeRegistry.serverboundPlay().register(TYPE, STREAM_CODEC);
		ServerPlayNetworking.registerGlobalReceiver(TYPE, (payload, context) -> {
			if (Settings.on(Setting.PAINTING_CHOICE)) {
				payload.choose(context.player());
			}
		});
	}

	private void choose(ServerPlayer player) {
		// Empty-handed is for the last painting of a stack, which the client only allows on the one just placed
		boolean holding = player.getMainHandItem().isEmpty() || player.getMainHandItem().is(Items.PAINTING) || player.getOffhandItem().is(Items.PAINTING);
		if (player.level().getEntity(painting) instanceof Painting target && holding && !player.isSpectator() && player.mayBuild()
				&& player.level().mayInteract(player, target.getPos()) && player.isWithinEntityInteractionRange(target, REACH_MARGIN)) {
			next(target, Integer.signum(step));
		}
	}

	private static void next(Painting painting, int step) {
		if (step == 0) {
			return;
		}
		List<Holder<PaintingVariant>> variants = new ArrayList<>();
		painting.registryAccess().lookupOrThrow(Registries.PAINTING_VARIANT).getTagOrEmpty(PaintingVariantTags.PLACEABLE).forEach(variants::add);
		// Largest first, as vanilla picks among the largest: sizes stay together on the way through
		variants.sort(Comparator.comparingInt((Holder<PaintingVariant> variant) -> -variant.value().area()));
		Holder<PaintingVariant> current = painting.getVariant();
		// A painting from a command may not be in the tag: the first step then lands on an end of the list
		int start = variants.contains(current) ? variants.indexOf(current) : step > 0 ? -1 : 0;
		PaintingAccessor accessor = (PaintingAccessor) painting;
		for (int i = 1; i <= variants.size(); i++) {
			Holder<PaintingVariant> candidate = variants.get(Math.floorMod(start + step * i, variants.size()));
			if (candidate.equals(current)) {
				break;
			}
			accessor.cassonade$setVariant(candidate);
			if (painting.survives()) {
				painting.playPlacementSound();
				return;
			}
		}
		accessor.cassonade$setVariant(current);
	}

	@Override
	public Type<ChoosePaintingPayload> type() {
		return TYPE;
	}
}
