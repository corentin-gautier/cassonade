package com.cassonade.waystone;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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

		/** Placed by the player: listed before the others', and sorted apart from them. */
		public boolean ownedBy(UUID player) {
			return owner.isPresent() && owner.get().equals(player);
		}

		static final Codec<Entry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				GlobalPos.CODEC.fieldOf("pos").forGetter(Entry::pos),
				Codec.STRING.optionalFieldOf("name", "").forGetter(Entry::name),
				UUIDUtil.CODEC_SET.optionalFieldOf("discovered_by", Set.of()).forGetter(Entry::discoveredBy),
				UUIDUtil.CODEC.optionalFieldOf("owner").forGetter(Entry::owner)).apply(instance, Entry::new));
	}

	private static final Codec<WaystoneData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Entry.CODEC.listOf().fieldOf("waystones").forGetter(data -> data.entries),
			Codec.unboundedMap(UUIDUtil.STRING_CODEC, GlobalPos.CODEC.listOf()).optionalFieldOf("orders", Map.of()).forGetter(data -> data.orders))
			.apply(instance, WaystoneData::new));
	private static final SavedDataType<WaystoneData> TYPE = new SavedDataType<>(Cassonade.id("waystones"), WaystoneData::new, CODEC, null);
	/** Until a player sorts them: the named ones first, by name, then by dimension and place. */
	private static final Comparator<Entry> DEFAULT_ORDER = Comparator.comparing((Entry entry) -> entry.name().isEmpty())
			.thenComparing(Entry::name)
			.thenComparing(entry -> entry.pos().dimension().identifier().toString())
			.thenComparing(entry -> entry.pos().pos());

	private final List<Entry> entries;
	/** The order each player gave their list, which those discovered since follow in the default order. */
	private final Map<UUID, List<GlobalPos>> orders;

	private WaystoneData() {
		this(List.of(), Map.of());
	}

	private WaystoneData(List<Entry> entries, Map<UUID, List<GlobalPos>> orders) {
		this.entries = new ArrayList<>(entries);
		this.orders = new HashMap<>(orders);
	}

	public static WaystoneData of(MinecraftServer server) {
		return server.overworld().getDataStorage().computeIfAbsent(TYPE);
	}

	public List<Entry> entries() {
		return entries;
	}

	/** The waystones the player knows, their own first, each group in the order they gave it. */
	public List<Entry> known(UUID player) {
		List<GlobalPos> order = orders.getOrDefault(player, List.of());
		return entries.stream().filter(entry -> entry.discoveredBy().contains(player))
				.sorted(Comparator.comparing((Entry entry) -> !entry.ownedBy(player)).thenComparingInt((Entry entry) -> {
					int index = order.indexOf(entry.pos());
					return index < 0 ? Integer.MAX_VALUE : index;
				}).thenComparing(DEFAULT_ORDER))
				.toList();
	}

	/**
	 * Moves a waystone of the player's list one row up or down, or to the top or bottom of its group (theirs or the
	 * others'), as the screen of the one at {@code here} shows it: without that one, which keeps its own place.
	 */
	public void move(UUID player, GlobalPos here, GlobalPos which, boolean up, boolean toEnd) {
		List<Entry> all = known(player);
		int herePlace = all.stream().map(Entry::pos).toList().indexOf(here);
		Optional<Entry> moved = all.stream().filter(entry -> entry.pos().equals(which) && !entry.pos().equals(here)).findFirst();
		if (moved.isEmpty()) {
			return;
		}
		boolean own = moved.get().ownedBy(player);
		List<GlobalPos> group = new ArrayList<>(all.stream().filter(entry -> entry.ownedBy(player) == own && !entry.pos().equals(here))
				.map(Entry::pos).toList());
		if (!moveIn(group, which, up, toEnd)) {
			return;
		}
		List<GlobalPos> order = new ArrayList<>();
		for (boolean ownFirst : new boolean[] {true, false}) {
			if (ownFirst == own) {
				order.addAll(group);
			} else {
				all.stream().filter(entry -> entry.ownedBy(player) == ownFirst && !entry.pos().equals(here)).map(Entry::pos).forEach(order::add);
			}
		}
		if (herePlace >= 0) {
			order.add(herePlace, here);
		}
		orders.put(player, List.copyOf(order));
		setDirty();
	}

	/** Moves the item in the list as {@link #move} does; false when it is already at that end. */
	public static <T> boolean moveIn(List<T> list, T item, boolean up, boolean toEnd) {
		int from = list.indexOf(item);
		int to = toEnd ? (up ? 0 : list.size() - 1) : from + (up ? -1 : 1);
		if (from < 0 || to < 0 || to >= list.size() || to == from) {
			return false;
		}
		list.remove(from);
		list.add(to, item);
		return true;
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

	/** Moves the entry, with its name, discoveries and owner, to the waystone now at {@code to}, replacing what its placing listed there. */
	public void restore(Entry entry, GlobalPos to) {
		entries.removeIf(other -> other.pos().equals(entry.pos()) || other.pos().equals(to));
		entries.add(new Entry(to, entry.name(), entry.discoveredBy(), entry.owner()));
		setDirty();
	}

	public void remove(GlobalPos pos) {
		if (entries.removeIf(entry -> entry.pos().equals(pos))) {
			setDirty();
		}
	}
}
