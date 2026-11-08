package com.cassonade.stable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.UUIDUtil;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.component.TypedEntityData;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import com.cassonade.Cassonade;

/** Each player's registered horses, kept with the overworld's data: a horse left far away has no loaded chunk to ask. */
public final class StableData extends SavedData {
	/**
	 * {@code id}: the entry's own, which the horse carries (Stables.TAG) so a copy of it can be told from the real one.
	 * {@code entity}: the UUID of the horse in the world, a new one each time it is taken out. {@code boarded}: kept at the
	 * stable, out of the world. {@code horse}: the horse as saved when boarded or last seen, to take it out again or to
	 * bring it back from an unloaded chunk, and to show it in the screen.
	 */
	public record Entry(UUID id, UUID entity, String name, boolean boarded, TypedEntityData<EntityType<?>> horse) {
		static final Codec<Entry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				UUIDUtil.CODEC.fieldOf("id").forGetter(Entry::id),
				UUIDUtil.CODEC.fieldOf("entity").forGetter(Entry::entity),
				Codec.STRING.optionalFieldOf("name", "").forGetter(Entry::name),
				Codec.BOOL.optionalFieldOf("boarded", false).forGetter(Entry::boarded),
				TypedEntityData.codec(EntityType.CODEC).fieldOf("horse").forGetter(Entry::horse)).apply(instance, Entry::new));
	}

	private static final Codec<StableData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Codec.unboundedMap(UUIDUtil.STRING_CODEC, Entry.CODEC.listOf()).optionalFieldOf("horses", Map.of()).forGetter(data -> data.horses))
			.apply(instance, StableData::new));
	private static final SavedDataType<StableData> TYPE = new SavedDataType<>(Cassonade.id("stables"), StableData::new, CODEC, null);

	/** Each player's horses, in the order they were registered. */
	private final Map<UUID, List<Entry>> horses;

	private StableData() {
		this(Map.of());
	}

	private StableData(Map<UUID, List<Entry>> horses) {
		this.horses = new HashMap<>();
		horses.forEach((player, list) -> this.horses.put(player, new ArrayList<>(list)));
	}

	public static StableData of(MinecraftServer server) {
		return server.overworld().getDataStorage().computeIfAbsent(TYPE);
	}

	public List<Entry> of(UUID player) {
		return List.copyOf(horses.getOrDefault(player, List.of()));
	}

	public Optional<Entry> get(UUID player, UUID id) {
		return horses.getOrDefault(player, List.of()).stream().filter(entry -> entry.id().equals(id)).findFirst();
	}

	public void add(UUID player, Entry entry) {
		horses.computeIfAbsent(player, key -> new ArrayList<>()).add(entry);
		setDirty();
	}

	/** Replaces the entry of the same id, in its place. */
	public void put(UUID player, Entry entry) {
		List<Entry> list = horses.get(player);
		if (list != null) {
			list.replaceAll(other -> other.id().equals(entry.id()) ? entry : other);
			setDirty();
		}
	}

	public void remove(UUID player, UUID id) {
		List<Entry> list = horses.get(player);
		if (list != null && list.removeIf(entry -> entry.id().equals(id))) {
			if (list.isEmpty()) {
				horses.remove(player);
			}
			setDirty();
		}
	}
}
