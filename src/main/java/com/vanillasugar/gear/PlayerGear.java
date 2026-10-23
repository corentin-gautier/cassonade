package com.vanillasugar.gear;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentSyncPredicate;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import com.vanillasugar.VanillaSugar;

/** What a player wears in the gear slots. Stored on the player and synced to its own client for the HUD. */
public record PlayerGear(ItemStack backpack, ItemStack compass, ItemStack map, ItemStack elytra, ItemStack clock) {
	public static final PlayerGear EMPTY = new PlayerGear(ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY);

	public static final Codec<PlayerGear> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			ItemStack.OPTIONAL_CODEC.optionalFieldOf("backpack", ItemStack.EMPTY).forGetter(PlayerGear::backpack),
			ItemStack.OPTIONAL_CODEC.optionalFieldOf("compass", ItemStack.EMPTY).forGetter(PlayerGear::compass),
			ItemStack.OPTIONAL_CODEC.optionalFieldOf("map", ItemStack.EMPTY).forGetter(PlayerGear::map),
			ItemStack.OPTIONAL_CODEC.optionalFieldOf("elytra", ItemStack.EMPTY).forGetter(PlayerGear::elytra),
			ItemStack.OPTIONAL_CODEC.optionalFieldOf("clock", ItemStack.EMPTY).forGetter(PlayerGear::clock)
	).apply(instance, PlayerGear::new));

	public static final StreamCodec<RegistryFriendlyByteBuf, PlayerGear> STREAM_CODEC = StreamCodec.composite(
			ItemStack.OPTIONAL_STREAM_CODEC, PlayerGear::backpack,
			ItemStack.OPTIONAL_STREAM_CODEC, PlayerGear::compass,
			ItemStack.OPTIONAL_STREAM_CODEC, PlayerGear::map,
			ItemStack.OPTIONAL_STREAM_CODEC, PlayerGear::elytra,
			ItemStack.OPTIONAL_STREAM_CODEC, PlayerGear::clock,
			PlayerGear::new);

	// Kept through death so keepInventory works; without it the gear is dropped first (see PlayerMixin)
	public static final AttachmentType<PlayerGear> ATTACHMENT = AttachmentRegistry.create(VanillaSugar.id("gear"), builder -> builder
			.persistent(CODEC)
			.copyOnDeath()
			.syncWith(STREAM_CODEC, AttachmentSyncPredicate.targetOnly()));

	public static void init() {
	}

	public static PlayerGear of(Player player) {
		return player.getAttachedOrElse(ATTACHMENT, EMPTY);
	}

	public static boolean isWearing(Player player, GearSlot slot) {
		return !of(player).get(slot).isEmpty();
	}

	public ItemStack get(GearSlot slot) {
		return switch (slot) {
			case BACKPACK -> backpack;
			case COMPASS -> compass;
			case MAP -> map;
			case ELYTRA -> elytra;
			case CLOCK -> clock;
		};
	}

	public PlayerGear with(GearSlot slot, ItemStack stack) {
		return switch (slot) {
			case BACKPACK -> new PlayerGear(stack, compass, map, elytra, clock);
			case COMPASS -> new PlayerGear(backpack, stack, map, elytra, clock);
			case MAP -> new PlayerGear(backpack, compass, stack, elytra, clock);
			case ELYTRA -> new PlayerGear(backpack, compass, map, stack, clock);
			case CLOCK -> new PlayerGear(backpack, compass, map, elytra, stack);
		};
	}
}
