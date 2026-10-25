package com.cassonade.client.quest;

import java.io.IOException;
import java.io.Reader;
import java.util.List;

import org.jspecify.annotations.Nullable;

import com.google.gson.JsonParser;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.world.item.Item;

import com.cassonade.Cassonade;
import com.cassonade.client.LabeledKeys;
import com.cassonade.client.CassonadeClient;

/**
 * The chapters of the quest journal, read from assets/cassonade/quests/chapters.json: each one an
 * ordered list of advancements, the order the journal suggests them in. Their texts are in the
 * language files: the chapter's title and introduction, and for each quest a hint saying where to go
 * and what to do, or the advancement's own description when it has none, and a title for the few
 * whose advancement's title does not fit a quest.
 */
public final class QuestBook implements ResourceManagerReloadListener {
	public static final Identifier ID = Cassonade.id("quests");
	private static final Identifier FILE = Cassonade.id("quests/chapters.json");

	public record Chapter(String id, Item icon, List<Identifier> quests) {
		static final Codec<Chapter> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				Codec.STRING.fieldOf("id").forGetter(Chapter::id),
				BuiltInRegistries.ITEM.byNameCodec().fieldOf("icon").forGetter(Chapter::icon),
				Identifier.CODEC.listOf().fieldOf("quests").forGetter(Chapter::quests)
		).apply(instance, Chapter::new));

		public Component title() {
			return Component.translatable("quest.cassonade.chapter." + id);
		}

		public Component description() {
			return Component.translatable("quest.cassonade.chapter." + id + ".description");
		}
	}

	private static final Codec<List<Chapter>> CODEC = Chapter.CODEC.listOf().fieldOf("chapters").codec();

	private static List<Chapter> chapters = List.of();

	public static List<Chapter> chapters() {
		return chapters;
	}

	/**
	 * Where to go and what to do for the advancement, or null when the journal has no hint for it. A
	 * hint can name the keys of the mod's screens: %1$s the backpack's, %2$s the world map's.
	 */
	public static @Nullable Component hint(Identifier advancement) {
		String key = key("hint", advancement);
		return Language.getInstance().has(key) ? Component.translatable(key, LabeledKeys.printedName(CassonadeClient.OPEN_BACKPACK),
				LabeledKeys.printedName(CassonadeClient.OPEN_WORLD_MAP)) : null;
	}

	/** quest.cassonade.hint.story.mine_stone, the namespace after the kind when it is not minecraft. */
	private static String key(String kind, Identifier advancement) {
		String path = advancement.getPath().replace('/', '.');
		return advancement.getNamespace().equals(Identifier.DEFAULT_NAMESPACE)
				? "quest.cassonade." + kind + "." + path
				: "quest.cassonade." + kind + "." + advancement.getNamespace() + "." + path;
	}

	/**
	 * The quest's own title when the journal gives one, as for the roots of the advancement tree, whose
	 * title is their tab's name; else the advancement's title, or vanilla's for one not sent.
	 */
	public static Component title(Identifier advancement, @Nullable Component title) {
		String key = key("title", advancement);
		if (Language.getInstance().has(key)) {
			return Component.translatable(key);
		}
		return title != null ? title : Component.translatable("advancements." + advancement.getPath().replace('/', '.') + ".title");
	}

	@Override
	public void onResourceManagerReload(ResourceManager resourceManager) {
		try (Reader reader = resourceManager.openAsReader(FILE)) {
			chapters = CODEC.parse(JsonOps.INSTANCE, JsonParser.parseReader(reader)).getOrThrow();
		} catch (IOException | RuntimeException e) {
			Cassonade.LOGGER.warn("Could not read the quest journal's chapters from {}", FILE, e);
			chapters = List.of();
		}
	}
}
