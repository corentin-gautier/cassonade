package com.vanillasugar.client;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import net.fabricmc.loader.api.FabricLoader;

import com.vanillasugar.VanillaSugar;

/** The player's display choices, kept in config/vanillasugar.properties. */
public final class ClientSettings {
	/** How markers are shown in the world while a compass is worn. */
	public enum MarkerDisplay {
		ICON_AND_TEXT, ICON, NONE;

		public MarkerDisplay next() {
			return values()[(ordinal() + 1) % values().length];
		}
	}

	/** How much of the quest followed is shown under the minimap. */
	public enum QuestTrackerDisplay {
		FULL, COMPACT, HIDDEN;

		public QuestTrackerDisplay next() {
			return values()[(ordinal() + 1) % values().length];
		}
	}

	private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("vanillasugar.properties");

	private static MarkerDisplay markerDisplay = MarkerDisplay.ICON_AND_TEXT;
	private static int minimapZoom = -1;
	private static boolean minimapRotates;
	private static QuestTrackerDisplay questTrackerDisplay = QuestTrackerDisplay.FULL;

	private ClientSettings() {
	}

	public static MarkerDisplay markerDisplay() {
		return markerDisplay;
	}

	public static void setMarkerDisplay(MarkerDisplay display) {
		markerDisplay = display;
		save();
	}

	/** The minimap zoom level, or -1 for the default. */
	public static int minimapZoom() {
		return minimapZoom;
	}

	public static void setMinimapZoom(int zoom) {
		minimapZoom = zoom;
		save();
	}

	/** Whether the minimap turns with the player, its view direction up, instead of keeping north up. */
	public static boolean minimapRotates() {
		return minimapRotates;
	}

	public static void setMinimapRotates(boolean rotates) {
		minimapRotates = rotates;
		save();
	}

	public static QuestTrackerDisplay questTrackerDisplay() {
		return questTrackerDisplay;
	}

	public static void setQuestTrackerDisplay(QuestTrackerDisplay display) {
		questTrackerDisplay = display;
		save();
	}

	public static void load() {
		if (!Files.exists(FILE)) {
			return;
		}
		Properties properties = new Properties();
		try (Reader reader = Files.newBufferedReader(FILE)) {
			properties.load(reader);
			markerDisplay = MarkerDisplay.valueOf(properties.getProperty("markerDisplay", markerDisplay.name()));
			minimapZoom = Integer.parseInt(properties.getProperty("minimapZoom", String.valueOf(minimapZoom)));
			minimapRotates = Boolean.parseBoolean(properties.getProperty("minimapRotates", String.valueOf(minimapRotates)));
			questTrackerDisplay = QuestTrackerDisplay.valueOf(properties.getProperty("questTracker", questTrackerDisplay.name()));
		} catch (IOException | IllegalArgumentException e) {
			VanillaSugar.LOGGER.warn("Could not read {}", FILE, e);
		}
	}

	private static void save() {
		Properties properties = new Properties();
		properties.setProperty("markerDisplay", markerDisplay.name());
		properties.setProperty("minimapZoom", String.valueOf(minimapZoom));
		properties.setProperty("minimapRotates", String.valueOf(minimapRotates));
		properties.setProperty("questTracker", questTrackerDisplay.name());
		try (Writer writer = Files.newBufferedWriter(FILE)) {
			properties.store(writer, "Vanilla Sugar");
		} catch (IOException e) {
			VanillaSugar.LOGGER.warn("Could not save {}", FILE, e);
		}
	}
}
