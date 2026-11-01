package com.cassonade.waystone;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.GlobalPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import com.cassonade.Cassonade;

/** Every waystone of the world, in all dimensions, kept with the overworld's data: a far one has no loaded chunk to ask. */
public final class WaystoneData extends SavedData {
	public record Entry(GlobalPos pos, String name) {
		static final Codec<Entry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				GlobalPos.CODEC.fieldOf("pos").forGetter(Entry::pos),
				Codec.STRING.optionalFieldOf("name", "").forGetter(Entry::name)).apply(instance, Entry::new));
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
		entries.removeIf(entry -> entry.pos().equals(pos));
		entries.add(new Entry(pos, name));
		setDirty();
	}

	public void remove(GlobalPos pos) {
		if (entries.removeIf(entry -> entry.pos().equals(pos))) {
			setDirty();
		}
	}
}
