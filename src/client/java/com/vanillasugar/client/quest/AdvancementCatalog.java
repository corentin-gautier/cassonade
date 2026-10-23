package com.vanillasugar.client.quest;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.jspecify.annotations.Nullable;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

import net.minecraft.advancements.AdvancementNode;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.advancements.DisplayInfo;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientAdvancements;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import com.vanillasugar.storage.StoragePayload;

/**
 * Every advancement the player can see, by category (the roots of the advancement tree), each one
 * with its progress.
 *
 * <p>A vanilla server only tells the client about the advancements near the ones already made. With
 * the mod, it sends them all (AdvancementVisibilityEvaluatorMixin), so the list shows everything
 * there is to do; on a server without it, only what it has sent.
 */
public final class AdvancementCatalog {
	/** The vanilla tabs first, in the game's order, then those of datapacks by name. */
	private static final List<String> ROOT_ORDER = List.of("story/root", "nether/root", "end/root", "adventure/root", "husbandry/root");

	public record Entry(AdvancementNode node, DisplayInfo display, @Nullable AdvancementProgress progress) {
		public Identifier id() {
			return node.holder().id();
		}

		public boolean done() {
			return progress != null && progress.isDone();
		}

		/** Hidden by its datapack until made: shown as a secret. */
		public boolean secret() {
			return display.hidden() && !done();
		}

		/** How many requirements it has, all to meet: the progress bar is shown above 1. */
		public int requirements() {
			return node.advancement().requirements().size();
		}

		public int requirementsMet() {
			if (progress == null) {
				return 0;
			}
			return Math.round(progress.getPercent() * requirements());
		}

		public Iterable<String> remainingCriteria() {
			return progress != null ? progress.getRemainingCriteria() : node.advancement().criteria().keySet();
		}

		public Iterable<String> completedCriteria() {
			return progress != null ? progress.getCompletedCriteria() : List.of();
		}
	}

	public record Category(Entry root, List<Entry> entries) {
		public int done() {
			return (int) entries.stream().filter(Entry::done).count();
		}
	}

	private AdvancementCatalog() {
	}

	/** Whether the list holds every advancement: the server has the mod, which sends them all. */
	public static boolean isComplete() {
		return ClientPlayNetworking.canSend(StoragePayload.TYPE);
	}

	public static List<Category> categories(Minecraft minecraft) {
		if (minecraft.getConnection() == null) {
			return List.of();
		}
		ClientAdvancements client = minecraft.getConnection().getAdvancements();
		Map<Identifier, AdvancementProgress> progress = new HashMap<>();
		client.progress().forEach((holder, value) -> progress.put(holder.id(), value));

		List<AdvancementNode> roots = new ArrayList<>();
		client.tree().roots().forEach(roots::add);
		roots.sort(Comparator.comparingInt(AdvancementCatalog::rootRank).thenComparing(root -> root.holder().id().toString()));

		List<Category> categories = new ArrayList<>();
		for (AdvancementNode root : roots) {
			List<Entry> entries = new ArrayList<>();
			collect(root, progress, entries);
			if (!entries.isEmpty() && entries.getFirst().node == root) {
				categories.add(new Category(entries.getFirst(), entries));
			}
		}
		return categories;
	}

	private static int rootRank(AdvancementNode root) {
		Identifier id = root.holder().id();
		int index = id.getNamespace().equals(Identifier.DEFAULT_NAMESPACE) ? ROOT_ORDER.indexOf(id.getPath()) : -1;
		return index >= 0 ? index : ROOT_ORDER.size();
	}

	/** The node, then its children depth first: each advancement comes after the one it follows. */
	private static void collect(AdvancementNode node, Map<Identifier, AdvancementProgress> progress, List<Entry> entries) {
		node.advancement().display().ifPresent(display -> entries.add(new Entry(node, display, progress.get(node.holder().id()))));
		for (AdvancementNode child : node.children()) {
			collect(child, progress, entries);
		}
	}

	public static @Nullable Entry find(List<Category> categories, Identifier id) {
		for (Category category : categories) {
			for (Entry entry : category.entries) {
				if (entry.id().equals(id)) {
					return entry;
				}
			}
		}
		return null;
	}

	/**
	 * A criterion's name for the player. The criteria of the advancements with many of them are named
	 * after what they ask for: a biome, a mob, an item. Others get their name tidied up.
	 */
	public static Component criterionName(String criterion) {
		Identifier id = Identifier.tryParse(criterion);
		if (id != null) {
			String path = id.getPath().replace('/', '.');
			for (String kind : List.of("biome", "entity", "item", "block", "effect")) {
				String key = kind + "." + id.getNamespace() + "." + path;
				if (Language.getInstance().has(key)) {
					return Component.translatable(key);
				}
			}
		}
		String name = (id != null ? id.getPath() : criterion).replace('_', ' ').replace('/', ' ');
		return Component.literal(name.isEmpty() ? criterion : name.substring(0, 1).toUpperCase(Locale.ROOT) + name.substring(1));
	}
}
