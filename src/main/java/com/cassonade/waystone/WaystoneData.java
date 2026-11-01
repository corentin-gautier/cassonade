package com.cassonade.waystone;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.GlobalPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import com.cassonade.Cassonade;

/** Every waystone of the world, in all dimensions, kept with the overworld's data: a far one has no loaded chunk to ask. */
public final class WaystoneData extends SavedData {
	/** {@code discoveredBy}: whoever placed it or has been near it, the only players to see it in the list and on their world map.
	 * {@code owner}: who placed it and alone may rename it, nobody for one placed by a command, which anyone may. */
	public record Entry(GlobalPos pos, String name, Set<UUID> discoveredBy, Optional<UUID> owner) {
		public boolean canRename(UUID player) {
			return owner.isEmpty() || owner.get().equals(player);
		}

		static final Codec<Entry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				GlobalPos.CODEC.fieldOf("pos").forGetter(Entry::pos),
				Codec.STRING.optionalFieldOf("name", "").forGetter(Entry::name),
				UUIDUtil.CODEC_SET.optionalFieldOf("discovered_by", Set.of()).forGetter(Entry::discoveredBy),
				UUIDUtil.CODEC.optionalFieldOf("owner").forGetter(Entry::owner)).apply(instance, Entry::new));
	}

	private static final Codec<WaystoneData> CODEC = Entry.CODEC.listOf().xmap(WaystoneData::new, data -> data.entries)
			.fieldOf("waystones").codec();
	private static final SavedDataType<WaystoneData> TYPE = new SavedDataType<>(Cassonade.id("waystones"), WaystoneData::new, CODEC, null);

	private final List<Entry> entries;

	private WaystoneData() {
		this(List.of());
	}

	private WaystoneData(List<Entry> entries) {
		this.entries = new ArrayList<>(entries);
	}

	public static WaystoneData of(MinecraftServer server) {
		return server.overworld().getDataStorage().computeIfAbsent(TYPE);
	}

	public List<Entry> entries() {
		return entries;
	}

	public Optional<Entry> at(GlobalPos pos) {
		return entries.stream().filter(entry -> entry.pos().equals(pos)).findFirst();
	}

	/** Adds the waystone, or names it if already there. */
	public void put(GlobalPos pos, String name) {
		Optional<Entry> old = at(pos);
		entries.removeIf(entry -> entry.pos().equals(pos));
		entries.add(new Entry(pos, name, old.map(Entry::discoveredBy).orElse(Set.of()), old.flatMap(Entry::owner)));
		setDirty();
	}

	public void discover(Entry entry, UUID player) {
		Set<UUID> discoveredBy = new HashSet<>(entry.discoveredBy());
		discoveredBy.add(player);
		entries.replaceAll(other -> other == entry ? new Entry(entry.pos(), entry.name(), discoveredBy, entry.owner()) : other);
		setDirty();
	}

	/** Makes the player its owner, who knows it from the start. */
	public void claim(GlobalPos pos, UUID player) {
		entries.replaceAll(entry -> {
			if (!entry.pos().equals(pos)) {
				return entry;
			}
			Set<UUID> discoveredBy = new HashSet<>(entry.discoveredBy());
			discoveredBy.add(player);
			return new Entry(pos, entry.name(), discoveredBy, Optional.of(player));
		});
		setDirty();
	}

	public void remove(GlobalPos pos) {
		if (entries.removeIf(entry -> entry.pos().equals(pos))) {
			setDirty();
		}
	}
}
