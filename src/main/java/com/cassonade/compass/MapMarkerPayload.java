package com.cassonade.compass;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.SectionPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.levelgen.Heightmap;

import com.cassonade.Cassonade;
import com.cassonade.gear.GearSlot;
import com.cassonade.gear.PlayerGear;

/**
 * Sent by a right click on the world map, in the player's dimension: removes the marker at that
 * position, or places one there with the name typed and color picked. Needs a worn map.
 */
public record MapMarkerPayload(int x, int z, String name, DyeColor color, boolean remove) implements CustomPacketPayload {
	public static final Type<MapMarkerPayload> TYPE = new Type<>(Cassonade.id("map_marker"));
	public static final StreamCodec<RegistryFriendlyByteBuf, MapMarkerPayload> STREAM_CODEC = StreamCodec.composite(
			ByteBufCodecs.VAR_INT, MapMarkerPayload::x,
			ByteBufCodecs.VAR_INT, MapMarkerPayload::z,
			ByteBufCodecs.stringUtf8(Marker.MAX_NAME_LENGTH), MapMarkerPayload::name,
			DyeColor.STREAM_CODEC, MapMarkerPayload::color,
			ByteBufCodecs.BOOL, MapMarkerPayload::remove,
			MapMarkerPayload::new);

	public static void init() {
		PayloadTypeRegistry.serverboundPlay().register(TYPE, STREAM_CODEC);
		ServerPlayNetworking.registerGlobalReceiver(TYPE, (payload, context) -> payload.apply(context.player()));
	}

	private void apply(ServerPlayer player) {
		if (!PlayerGear.isWearing(player, GearSlot.MAP)) {
			return;
		}

		ServerLevel level = player.level();
		if (remove) {
			PlayerMarkers.of(player).markers().stream()
					.filter(marker -> marker.pos().dimension() == level.dimension() && marker.pos().pos().getX() == x && marker.pos().pos().getZ() == z)
					.findFirst()
					.ifPresent(marker -> PlayerMarkers.remove(player, marker));
			return;
		}
		// On the ground when the chunk is loaded, at the player's height otherwise
		int y = level.hasChunk(SectionPos.blockToSectionCoord(x), SectionPos.blockToSectionCoord(z))
				? level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z) : player.getBlockY();
		PlayerMarkers.place(player, GlobalPos.of(level.dimension(), new BlockPos(x, y, z)), name.strip(), color);
	}

	@Override
	public Type<MapMarkerPayload> type() {
		return TYPE;
	}
}
