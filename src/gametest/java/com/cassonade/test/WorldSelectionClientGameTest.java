package com.cassonade.test;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.world.Difficulty;

import com.cassonade.client.WorldSelectionScreen;
import com.cassonade.client.quest.QuestOptInScreen;

/** The world selection screen, with a few worlds of each kind to choose from. */
public class WorldSelectionClientGameTest implements FabricClientGameTest {
	@Override
	public void runTest(ClientGameTestContext context) {
		if (TestSelection.skips(this)) {
			return;
		}
		context.getInput().resizeWindow(1920, 1080);
		createWorld(context, "Royaume des Cerisiers", WorldCreationUiState.SelectedGameMode.SURVIVAL, Difficulty.NORMAL);
		createWorld(context, "Atelier créatif", WorldCreationUiState.SelectedGameMode.CREATIVE, Difficulty.PEACEFUL);
		createWorld(context, "Dernière chance", WorldCreationUiState.SelectedGameMode.HARDCORE, Difficulty.HARD);

		// Vanilla's screen, as the title screen opens it, gives way to the mod's, the most recent world selected
		context.setScreen(() -> new SelectWorldScreen(new TitleScreen()));
		context.waitForScreen(WorldSelectionScreen.class);
		context.waitTicks(20);
		check(context.computeOnClient(client -> client.gui.screen() instanceof WorldSelectionScreen screen
				&& "Dernière chance".equals(screen.selectedName())), "the most recent world selected as the screen opens");
		Screenshots.take(context, "cassonade-world-selection");

		// The smallest GUI (scale 3 in 1280×720: 426×240), in French, whose words are longer
		setLanguage(context, "fr_fr");
		context.getInput().resizeWindow(1280, 720);
		context.waitTicks(5);
		Screenshots.take(context, "cassonade-world-selection-small-fr");
		setLanguage(context, "en_us");
		context.getInput().resizeWindow(1920, 1080);
		context.setScreen(TitleScreen::new);
	}

	private static void setLanguage(ClientGameTestContext context, String code) {
		context.runOnClient(client -> {
			client.getLanguageManager().setSelected(code);
			client.options.languageCode = code;
			client.reloadResourcePacks();
		});
		context.waitFor(client -> client.gui.overlay() == null);
		context.waitTicks(5);
	}

	private static void check(boolean condition, String what) {
		if (!condition) {
			throw new AssertionError("Expected: " + what);
		}
	}

	/** A real world, not the flat test one, so its icon shows some landscape. */
	private static void createWorld(ClientGameTestContext context, String name, WorldCreationUiState.SelectedGameMode mode,
			Difficulty difficulty) {
		try (TestSingleplayerContext world = context.worldBuilder().setUseConsistentSettings(false).adjustSettings(settings -> {
			settings.setName(name);
			settings.setGameMode(mode);
			settings.setDifficulty(difficulty);
		}).create()) {
			world.getConnection().waitForChunksRender();
			context.waitForScreen(QuestOptInScreen.class);
			context.setScreen(() -> null);
			// The game takes the world's icon a moment after joining
			context.waitTicks(60);
		}
	}
}
