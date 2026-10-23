package com.vanillasugar.client.quest;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.jspecify.annotations.Nullable;

import net.minecraft.advancements.AdvancementNode;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.LevelResource;

import com.vanillasugar.VanillaSugar;
import com.vanillasugar.client.quest.AdvancementCatalog.Category;
import com.vanillasugar.client.quest.AdvancementCatalog.Entry;
import com.vanillasugar.client.quest.QuestBook.Chapter;

/**
 * Where the player is in the quest journal. Whether they follow it is asked once per world, and
 * kept with the quest they chose to follow: in the world's folder in single player, in the game's
 * folder for a server.
 *
 * <p>A quest is done once its advancement is made, and open once the advancement it follows in the
 * advancement tree is: the journal guides the player along the game's own progression. The quest
 * followed is the one the player chose, else the first open one in the journal's order; when it is
 * done, the next open one after it.
 */
public final class QuestLog {
	public enum Status {
		DONE, OPEN, LOCKED
	}

	public record Quest(Chapter chapter, Identifier id, @Nullable Entry entry, Status status, @Nullable Component after) {
		public Component title() {
			return QuestBook.title(id, entry != null ? entry.display().title() : null);
		}

		/** What to do: the journal's hint, else the advancement's description. */
		public Component hint() {
			Component hint = QuestBook.hint(id);
			if (hint != null) {
				return hint;
			}
			return entry != null ? entry.display().description() : Component.empty();
		}

		public ItemStack icon() {
			return entry != null ? entry.display().icon().create() : new ItemStack(chapter.icon());
		}
	}

	public record ChapterState(Chapter chapter, List<Quest> quests) {
		public int done() {
			return (int) quests.stream().filter(quest -> quest.status == Status.DONE).count();
		}

		/** Locked while none of its quests is open or done yet. */
		public boolean locked() {
			return quests.stream().allMatch(quest -> quest.status == Status.LOCKED);
		}
	}

	/** Every chapter with the state of its quests, and the quest to follow. */
	public record Journal(List<ChapterState> chapters, @Nullable Quest current) {
		public int done() {
			return chapters.stream().mapToInt(ChapterState::done).sum();
		}

		public int total() {
			return chapters.stream().mapToInt(chapter -> chapter.quests().size()).sum();
		}

		public @Nullable Quest find(Identifier id) {
			return chapters.stream().flatMap(chapter -> chapter.quests().stream()).filter(quest -> quest.id().equals(id)).findFirst().orElse(null);
		}
	}

	/** How long the player is in the world before the journal is offered, so they see it first. */
	private static final int OFFER_DELAY = 40;

	private static @Nullable Path file;
	private static int ticksInWorld;
	/** Null until the player has chosen. */
	private static @Nullable Boolean enabled;
	private static @Nullable Identifier followed;

	private QuestLog() {
	}

	public static void open(Minecraft minecraft) {
		if (minecraft.getSingleplayerServer() != null) {
			file = minecraft.getSingleplayerServer().getWorldPath(LevelResource.ROOT).resolve("vanillasugar/quests.properties");
		} else {
			ServerData server = minecraft.getCurrentServer();
			String name = server == null ? "unknown" : server.ip.replaceAll("[^A-Za-z0-9._-]", "_");
			file = minecraft.gameDirectory.toPath().resolve("vanillasugar/quests/servers").resolve(name + ".properties");
		}
		enabled = null;
		followed = null;
		ticksInWorld = 0;
		load();
	}

	public static void tick(Minecraft minecraft) {
		if (file != null && minecraft.player != null) {
			ticksInWorld++;
		}
	}

	public static void close() {
		file = null;
		enabled = null;
		followed = null;
	}

	/** Whether to ask the player now if they follow the journal in this world. */
	public static boolean needsChoice() {
		return file != null && enabled == null && ticksInWorld >= OFFER_DELAY;
	}

	public static boolean isEnabled() {
		return Boolean.TRUE.equals(enabled);
	}

	public static void setEnabled(boolean value) {
		enabled = value;
		save();
	}

	public static @Nullable Identifier followed() {
		return followed;
	}

	public static void follow(Identifier quest) {
		followed = quest;
		save();
	}

	public static Journal journal(Minecraft minecraft) {
		List<Category> categories = AdvancementCatalog.categories(minecraft);
		Map<Identifier, Entry> entries = new HashMap<>();
		for (Category category : categories) {
			for (Entry entry : category.entries()) {
				entries.put(entry.id(), entry);
			}
		}

		List<ChapterState> chapters = new ArrayList<>();
		List<Quest> all = new ArrayList<>();
		for (Chapter chapter : QuestBook.chapters()) {
			List<Quest> quests = new ArrayList<>();
			for (Identifier id : chapter.quests()) {
				Entry entry = entries.get(id);
				Quest quest = quest(chapter, id, entry, entries);
				quests.add(quest);
				all.add(quest);
			}
			chapters.add(new ChapterState(chapter, quests));
		}
		return new Journal(chapters, current(all));
	}

	private static Quest quest(Chapter chapter, Identifier id, @Nullable Entry entry, Map<Identifier, Entry> entries) {
		if (entry == null) {
			// Not sent by a server without the mod: far from what the player has made
			return new Quest(chapter, id, null, Status.LOCKED, null);
		}
		if (entry.done()) {
			return new Quest(chapter, id, entry, Status.DONE, null);
		}
		AdvancementNode parent = entry.node().parent();
		Entry after = parent != null ? entries.get(parent.holder().id()) : null;
		if (after == null || after.done()) {
			return new Quest(chapter, id, entry, Status.OPEN, null);
		}
		return new Quest(chapter, id, entry, Status.LOCKED, after.display().title());
	}

	/** The quest followed if it is open, else the next open one after it, else the first open one. */
	private static @Nullable Quest current(List<Quest> quests) {
		int start = 0;
		for (int i = 0; i < quests.size(); i++) {
			if (quests.get(i).id.equals(followed)) {
				start = i;
			}
		}
		for (int i = 0; i < quests.size(); i++) {
			Quest quest = quests.get((start + i) % quests.size());
			if (quest.status == Status.OPEN) {
				return quest;
			}
		}
		return null;
	}

	private static void load() {
		if (file == null || !Files.exists(file)) {
			return;
		}
		Properties properties = new Properties();
		try (Reader reader = Files.newBufferedReader(file)) {
			properties.load(reader);
			String value = properties.getProperty("enabled");
			enabled = value == null ? null : Boolean.parseBoolean(value);
			String quest = properties.getProperty("followed");
			followed = quest == null ? null : Identifier.tryParse(quest);
		} catch (IOException e) {
			VanillaSugar.LOGGER.warn("Could not read {}", file, e);
		}
	}

	private static void save() {
		if (file == null) {
			return;
		}
		Properties properties = new Properties();
		if (enabled != null) {
			properties.setProperty("enabled", enabled.toString());
		}
		if (followed != null) {
			properties.setProperty("followed", followed.toString());
		}
		try {
			Files.createDirectories(file.getParent());
			try (Writer writer = Files.newBufferedWriter(file)) {
				properties.store(writer, "Vanilla Sugar quest journal");
			}
		} catch (IOException e) {
			VanillaSugar.LOGGER.warn("Could not save {}", file, e);
		}
	}
}
