package com.cassonade.settings;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import com.cassonade.Cassonade;

/** The server's settings, sent on joining and on each change, by id so a different version of the mod reads the ones it knows. */
public record SettingsPayload(Map<Setting, Boolean> settings) implements CustomPacketPayload {
	public static final Type<SettingsPayload> TYPE = new Type<>(Cassonade.id("settings"));
	public static final StreamCodec<RegistryFriendlyByteBuf, SettingsPayload> STREAM_CODEC = ByteBufCodecs.<RegistryFriendlyByteBuf, String, Boolean, Map<String, Boolean>>map(
			HashMap::new, ByteBufCodecs.STRING_UTF8, ByteBufCodecs.BOOL)
			.map(SettingsPayload::fromIds, SettingsPayload::ids);

	static void init() {
		PayloadTypeRegistry.clientboundPlay().register(TYPE, STREAM_CODEC);
	}

	private static SettingsPayload fromIds(Map<String, Boolean> ids) {
		Map<Setting, Boolean> settings = new EnumMap<>(Setting.class);
		ids.forEach((id, on) -> {
			Setting setting = Setting.byId(id);
			if (setting != null) {
				settings.put(setting, on);
			}
		});
		return new SettingsPayload(settings);
	}

	private Map<String, Boolean> ids() {
		Map<String, Boolean> ids = new HashMap<>();
		settings.forEach((setting, on) -> ids.put(setting.id(), on));
		return ids;
	}

	@Override
	public Type<SettingsPayload> type() {
		return TYPE;
	}
}
