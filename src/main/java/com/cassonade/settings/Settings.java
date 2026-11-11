package com.cassonade.settings;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.resource.conditions.v1.ResourceCondition;
import net.fabricmc.fabric.api.resource.conditions.v1.ResourceConditionType;
import net.fabricmc.fabric.api.resource.conditions.v1.ResourceConditions;
import net.fabricmc.loader.api.FabricLoader;

import net.minecraft.resources.RegistryOps;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import org.jspecify.annotations.Nullable;

import com.cassonade.Cassonade;

/**
 * Which features are on, kept in config/cassonade-settings.properties: every one is on until turned off. A server
 * setting is read from the server's file, the integrated server's in singleplayer, and sent to its players
 * ({@link SettingsPayload}) for what the client shows of it; a client one from the player's own.
 */
public final class Settings {
	private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("cassonade-settings.properties");

	private static final Map<Setting, Boolean> local = new EnumMap<>(Setting.class);
	/** The server settings of the server the client plays on, when that is not its own integrated one. */
	private static volatile Map<Setting, Boolean> remote = Map.of();
	private static @Nullable MinecraftServer server;

	/** {@code {"condition": "cassonade:setting", "setting": "fair_recipes"}}: the mod's data overlays (pack.mcmeta). */
	public static final ResourceConditionType<SettingCondition> CONDITION = ResourceConditionType.create(Cassonade.id("setting"), SettingCondition.CODEC);

	private Settings() {
	}

	public static void init() {
		load();
		ResourceConditions.register(CONDITION);
		SettingsPayload.init();
		ServerLifecycleEvents.SERVER_STARTED.register(started -> server = started);
		ServerLifecycleEvents.SERVER_STOPPED.register(stopped -> server = null);
		ServerPlayConnectionEvents.JOIN.register((listener, sender, joined) -> send(listener.player));
	}

	/** Whether the feature is on, as the server playing it decides for a server setting. */
	public static boolean on(Setting setting) {
		Boolean fromServer = remote.get(setting);
		return fromServer != null ? fromServer : local(setting);
	}

	/** Whether this game's own file has the feature on. */
	public static boolean local(Setting setting) {
		synchronized (local) {
			return local.getOrDefault(setting, true);
		}
	}

	/** Turns a feature on or off in this game's file, and tells the players of the integrated server. */
	public static void set(Setting setting, boolean on) {
		synchronized (local) {
			local.put(setting, on);
		}
		save();
		MinecraftServer running = server;
		if (setting.side() == Setting.Side.SERVER && running != null) {
			running.execute(() -> running.getPlayerList().getPlayers().forEach(Settings::send));
		}
	}

	/** On the client: the settings of a server other than its own, or none once it leaves. */
	public static void setRemote(Map<Setting, Boolean> settings) {
		remote = Map.copyOf(settings);
	}

	/** On the client: whether a server other than its own decides the server settings. */
	public static boolean fromRemote() {
		return !remote.isEmpty();
	}

	private static void send(ServerPlayer player) {
		if (!ServerPlayNetworking.canSend(player, SettingsPayload.TYPE)) {
			return;
		}
		Map<Setting, Boolean> settings = new EnumMap<>(Setting.class);
		for (Setting setting : Setting.values()) {
			if (setting.side() == Setting.Side.SERVER) {
				settings.put(setting, local(setting));
			}
		}
		ServerPlayNetworking.send(player, new SettingsPayload(settings));
	}

	private static void load() {
		if (!Files.exists(FILE)) {
			return;
		}
		Properties properties = new Properties();
		try (Reader reader = Files.newBufferedReader(FILE)) {
			properties.load(reader);
		} catch (IOException e) {
			Cassonade.LOGGER.warn("Could not read {}", FILE, e);
			return;
		}
		synchronized (local) {
			for (String id : properties.stringPropertyNames()) {
				Setting setting = Setting.byId(id);
				if (setting != null) {
					local.put(setting, Boolean.parseBoolean(properties.getProperty(id).trim()));
				}
			}
		}
	}

	/** Every setting is written, on or off, so a server's admin finds them all in the file. */
	private static void save() {
		Map<String, String> sorted = new TreeMap<>();
		for (Setting setting : Setting.values()) {
			sorted.put(setting.id(), String.valueOf(local(setting)));
		}
		StringBuilder text = new StringBuilder("# Cassonade: set a feature to false to turn it off.\n");
		text.append("# Settings marked as data take effect the next time the world loads.\n");
		sorted.forEach((id, value) -> text.append(id).append('=').append(value).append('\n'));
		try (Writer writer = Files.newBufferedWriter(FILE)) {
			writer.write(text.toString());
		} catch (IOException e) {
			Cassonade.LOGGER.warn("Could not save {}", FILE, e);
		}
	}

	/** True while the setting is on in the game's own file: the server's data comes from its own. */
	public record SettingCondition(String setting) implements ResourceCondition {
		static final MapCodec<SettingCondition> CODEC = Codec.STRING.fieldOf("setting").xmap(SettingCondition::new, SettingCondition::setting);

		@Override
		public ResourceConditionType<?> getType() {
			return CONDITION;
		}

		@Override
		public boolean test(RegistryOps.@Nullable RegistryInfoLookup registryInfo) {
			Setting found = Setting.byId(setting);
			if (found == null) {
				Cassonade.LOGGER.warn("Unknown setting in a resource condition: {}", setting);
				return true;
			}
			return local(found);
		}
	}
}
