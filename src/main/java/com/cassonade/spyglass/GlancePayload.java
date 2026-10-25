package com.cassonade.spyglass;

import java.util.Optional;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.npc.villager.AbstractVillager;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;

import com.cassonade.Cassonade;
import com.cassonade.mixin.AbstractVillagerAccessor;

/**
 * What the server knows of a mob that its client does not, for the naturalist's spyglass: its age (below 0 a
 * baby growing up, above 0 an adult resting before it can breed again), whether it is in love, and for a
 * villager how many of its trades are sold out. A villager never traded with has no trades yet: they are
 * not made up just to be counted, and trades is -1.
 */
public record GlancePayload(int entity, int age, boolean inLove, int soldOut, int trades) implements CustomPacketPayload {
	public static final Type<GlancePayload> TYPE = new Type<>(Cassonade.id("glance"));
	public static final StreamCodec<RegistryFriendlyByteBuf, GlancePayload> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT, GlancePayload::entity,
			ByteBufCodecs.VAR_INT, GlancePayload::age,
			ByteBufCodecs.BOOL, GlancePayload::inLove,
			ByteBufCodecs.VAR_INT, GlancePayload::soldOut,
			ByteBufCodecs.VAR_INT, GlancePayload::trades,
			GlancePayload::new);

	public static void init() {
		PayloadTypeRegistry.clientboundPlay().register(TYPE, STREAM_CODEC);
	}

	static Optional<GlancePayload> of(Entity entity) {
		if (!(entity instanceof AgeableMob mob)) {
			return Optional.empty();
		}
		boolean inLove = mob instanceof Animal animal && animal.isInLove();
		int soldOut = 0;
		int trades = -1;
		if (mob instanceof AbstractVillager villager) {
			MerchantOffers offers = ((AbstractVillagerAccessor) villager).cassonade$getOffers();
			if (offers != null) {
				trades = offers.size();
				soldOut = (int) offers.stream().filter(MerchantOffer::isOutOfStock).count();
			}
		}
		return Optional.of(new GlancePayload(mob.getId(), mob.getAge(), inLove, soldOut, trades));
	}

	@Override
	public Type<GlancePayload> type() {
		return TYPE;
	}
}
