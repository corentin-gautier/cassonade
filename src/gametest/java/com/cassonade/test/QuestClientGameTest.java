package com.cassonade.test;

import java.lang.reflect.Field;
import java.util.List;

import com.mojang.blaze3d.platform.InputConstants;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.advancements.AdvancementsScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.resources.Identifier;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.InventoryMenu;

import com.cassonade.Cassonade;
import com.cassonade.client.BackpackPanel;
import com.cassonade.client.ClientSettings;
import com.cassonade.client.CassonadeClient;
import com.cassonade.client.map.Minimap;
import com.cassonade.client.quest.AchievementBanner;
import com.cassonade.client.quest.AdvancementCatalog;
import com.cassonade.client.quest.AdvancementCatalog.Category;
import com.cassonade.client.quest.AdvancementCatalog.Entry;
import com.cassonade.client.quest.AdvancementsListScreen;
import com.cassonade.client.quest.QuestJournalScreen;
import com.cassonade.client.quest.QuestLog;
import com.cassonade.client.quest.QuestOptInScreen;
import com.cassonade.client.quest.QuestTracker;
import com.cassonade.gear.GearLayout;

/** Advancement banners, the quest journal and the advancements screen. */
public class QuestClientGameTest implements FabricClientGameTest {
	/** Long enough for a banner to have grown in, short enough for it not to fade yet. */
	private static final long BANNER_SHOWN_MS = 1200;
	/** Longer than the longest banner, a challenge's. */
	private static final long BANNER_GONE_MS = 7500;

	@Override
	public void runTest(ClientGameTestContext context) {
		if (TestSelection.skips(this)) {
			return;
		}
		context.getInput().resizeWindow(1920, 1080);
		try (TestSingleplayerContext world = context.worldBuilder().create()) {
			world.getConnection().waitForChunksRender();
			// The journal is offered once, as soon as the player is in the world
			context.waitForScreen(QuestOptInScreen.class);
			context.takeScreenshot("cassonade-quest-opt-in");
			context.runOnClient(client -> QuestLog.setEnabled(true));
			context.setScreen(() -> null);
			world.getServer().runCommand("gamemode survival @a");

			context.runOnClient(client -> QuestLog.journal(client));
			banner(context, world, "story/mine_stone", "task");
			check(context.computeOnClient(client -> {
				QuestLog.Quest quest = QuestLog.journal(client).find(Identifier.withDefaultNamespace("story/mine_stone"));
				return quest != null && quest.status() == QuestLog.Status.DONE;
			}), "the quest journal refreshes when advancement progress changes");
			banner(context, world, "adventure/totem_of_undying", "goal");
			banner(context, world, "nether/all_potions", "challenge");

			// Over a screen, as when the crafting table makes one
			context.setScreen(() -> new InventoryScreen(Minecraft.getInstance().player));
			context.waitForScreen(InventoryScreen.class);
			banner(context, world, "story/smelt_iron", "over-inventory");
			context.setScreen(() -> null);
			context.waitTicks(2);
			context.takeScreenshot("cassonade-quest-tracker");

			// The journal opens on the followed quest's chapter
			context.setScreen(QuestJournalScreen::new);
			context.waitForScreen(QuestJournalScreen.class);
			context.waitTicks(2);
			context.takeScreenshot("cassonade-quest-journal");
			// Scrolled to the bottom: the quests done, apart under their heading
			context.runOnClient(client -> set(client.gui.screen(), "questsScroll", 10000.0));
			context.waitTicks(2);
			context.takeScreenshot("cassonade-quest-journal-done");
			context.setScreen(() -> null);

			// Iron armor followed, after the journal's suggestion
			context.runOnClient(client -> QuestLog.follow(Identifier.withDefaultNamespace("story/obtain_armor")));
			context.waitTicks(2);
			context.takeScreenshot("cassonade-quest-tracker-followed");
			// A click on it, where the chat frees the cursor, opens the journal on its chapter
			context.setScreen(() -> new ChatScreen("", false));
			context.waitForScreen(ChatScreen.class);
			double[] tracker = context.computeOnClient(client -> {
				double scale = client.getWindow().getGuiScale();
				return new double[] {(client.getWindow().getGuiScaledWidth() - 4 - Minimap.frameWidth() / 2.0) * scale, 24 * scale};
			});
			context.getInput().setCursorPos(tracker[0], tracker[1]);
			context.waitTicks(2);
			context.takeScreenshot("cassonade-quest-tracker-hovered");
			context.getInput().pressMouse(InputConstants.MOUSE_BUTTON_LEFT);
			context.waitForScreen(QuestJournalScreen.class);
			context.waitTicks(2);
			context.takeScreenshot("cassonade-quest-journal-from-tracker");
			check(context.computeOnClient(client -> {
				QuestLog.Journal journal = get(client.gui.screen(), "journal");
				int selected = get(client.gui.screen(), "selected");
				return journal.chapters().get(selected).quests().stream()
						.anyMatch(quest -> quest.id().equals(Identifier.withDefaultNamespace("story/obtain_armor")));
			}), "a click on the quest followed opens the journal on its chapter");
			context.setScreen(() -> null);
			// With a compass, under the minimap; the status effects below it
			world.getServer().runCommand("give @a compass");
			world.getServer().runCommand("effect give @a speed 600");
			world.getConnection().waitForClientboundPackets();
			context.waitTicks(2);
			context.runOnClient(client -> client.gameMode.handleContainerInput(client.player.inventoryMenu.containerId,
					InventoryMenu.USE_ROW_SLOT_START, 0, ContainerInput.QUICK_MOVE, client.player));
			world.getConnection().waitForServerboundPackets();
			world.getConnection().waitForClientboundPackets();
			context.waitTicks(5);
			context.takeScreenshot("cassonade-quest-tracker-minimap");
			// The compass worn earns the mod's minimap advancement, within a second
			context.waitTicks(25);
			world.getConnection().waitForClientboundPackets();
			check(context.computeOnClient(client -> {
				AdvancementCatalog.Entry minimap = AdvancementCatalog.find(AdvancementCatalog.categories(client), Cassonade.id("minimap"));
				return minimap != null && minimap.done();
			}), "wearing a compass makes the minimap advancement");
			// Its key shrinks the quest to its title, then hides it, then shows it whole again
			context.runOnClient(client -> AchievementBanner.clear());
			context.getInput().pressKey(CassonadeClient.QUEST_TRACKER);
			context.waitTicks(2);
			check(context.computeOnClient(client -> ClientSettings.questTrackerDisplay() == ClientSettings.QuestTrackerDisplay.COMPACT),
					"the key shrinks the current quest");
			context.takeScreenshot("cassonade-quest-tracker-compact");
			context.getInput().pressKey(CassonadeClient.QUEST_TRACKER);
			context.waitTicks(2);
			check(context.computeOnClient(client -> QuestTracker.bottom(client) == Minimap.height(client)), "the key hides the current quest");
			context.getInput().pressKey(CassonadeClient.QUEST_TRACKER);
			context.waitTicks(2);
			check(context.computeOnClient(client -> ClientSettings.questTrackerDisplay() == ClientSettings.QuestTrackerDisplay.FULL),
					"the key shows the current quest whole again");
			context.setScreen(QuestJournalScreen::new);
			context.waitForScreen(QuestJournalScreen.class);
			context.runOnClient(client -> {
				List<QuestLog.ChapterState> chapters = QuestLog.journal(client).chapters();
				for (int i = 0; i < chapters.size(); i++) {
					if (chapters.get(i).chapter().id().equals("explorer_gear")) {
						set(client.gui.screen(), "selected", i);
					}
				}
			});
			context.waitTicks(2);
			context.takeScreenshot("cassonade-quest-journal-explorer-gear");
			context.setScreen(() -> null);
			context.waitTicks(5);
			check(context.computeOnClient(client -> QuestLog.journal(client).current().id().getPath()).equals("story/obtain_armor"),
					"the followed quest is the current one");
			context.setScreen(QuestJournalScreen::new);
			context.waitForScreen(QuestJournalScreen.class);
			context.waitTicks(2);
			context.takeScreenshot("cassonade-quest-journal-followed");
			// A smaller GUI scale, more room on the pages
			context.runOnClient(client -> {
				client.options.guiScale().set(3);
				client.resizeGui();
			});
			context.waitTicks(2);
			context.takeScreenshot("cassonade-quest-journal-scale-3");
			context.runOnClient(client -> {
				client.options.guiScale().set(0);
				client.resizeGui();
			});
			context.setScreen(() -> null);

			// The advancements key opens vanilla's tree, replaced by the list
			world.getServer().runCommand("advancement grant @a only minecraft:nether/explore_nether minecraft:crimson_forest");
			world.getServer().runCommand("advancement grant @a only minecraft:nether/explore_nether minecraft:soul_sand_valley");
			world.getConnection().waitForClientboundPackets();
			context.setScreen(() -> new AdvancementsScreen(Minecraft.getInstance().getConnection().getAdvancements()));
			context.waitForScreen(AdvancementsListScreen.class);
			context.waitTicks(2);
			context.takeScreenshot("cassonade-quest-advancements");

			// The Nether, to do only, Hot Tourist Destinations opened on its biomes
			Identifier explore = Identifier.withDefaultNamespace("nether/explore_nether");
			context.runOnClient(client -> {
				AdvancementsListScreen screen = (AdvancementsListScreen) client.gui.screen();
				List<Category> categories = AdvancementCatalog.categories(client);
				int nether = 0;
				while (!categories.get(nether).root().id().getPath().equals("nether/root")) {
					nether++;
				}
				List<Entry> todo = categories.get(nether).entries().stream().filter(entry -> !entry.done()).toList();
				int row = todo.indexOf(AdvancementCatalog.find(categories, explore));
				set(screen, "selected", nether);
				set(screen, "filter", filterTodo());
				set(screen, "expanded", explore);
				set(screen, "scroll", Math.max(0, row - 1) * 46.0);
			});
			context.waitTicks(2);
			context.takeScreenshot("cassonade-quest-advancements-details");
			context.setScreen(() -> null);

			context.setScreen(() -> new InventoryScreen(Minecraft.getInstance().player));
			context.waitForScreen(InventoryScreen.class);
			context.takeScreenshot("cassonade-quest-inventory-buttons");

			// The quest followed and the buttons stay in front of the recipe book and the backpack
			context.runOnClient(client -> client.gui.screen().mouseClicked(new MouseButtonEvent(
					(client.getWindow().getGuiScaledWidth() - 176) / 2 + GearLayout.tabX(0) + GearLayout.TAB_WIDTH / 2,
					(client.getWindow().getGuiScaledHeight() - 166) / 2 + GearLayout.TAB_Y + 8,
					new MouseButtonInfo(InputConstants.MOUSE_BUTTON_LEFT, 0)), false));
			context.waitTicks(2);
			context.takeScreenshot("cassonade-quest-inventory-recipe-book");
			// The book opened rebuilds the inventory's widgets: the buttons still click
			clickQuestButton(context);
			check(context.computeOnClient(client -> client.gui.screen() instanceof AdvancementsListScreen
					|| client.gui.screen() instanceof QuestJournalScreen), "the buttons click with the recipe book open");
			// The inventory key closes it, as it does every screen
			context.getInput().pressKey(options -> options.keyInventory);
			context.waitTicks(2);
			check(context.computeOnClient(client -> client.gui.screen() == null), "the inventory key closes the journal or the advancements");
			world.getServer().runCommand("give @a cassonade:backpack");
			world.getConnection().waitForClientboundPackets();
			context.waitTicks(2);
			context.runOnClient(client -> client.gameMode.handleContainerInput(client.player.inventoryMenu.containerId,
					InventoryMenu.USE_ROW_SLOT_START, 0, ContainerInput.QUICK_MOVE, client.player));
			world.getConnection().waitForServerboundPackets();
			world.getConnection().waitForClientboundPackets();
			context.waitTicks(2);
			context.runOnClient(BackpackPanel::openInventory);
			context.waitForScreen(InventoryScreen.class);
			context.waitTicks(2);
			context.takeScreenshot("cassonade-quest-inventory-backpack");
			clickQuestButton(context);
			check(context.computeOnClient(client -> client.gui.screen() instanceof AdvancementsListScreen
					|| client.gui.screen() instanceof QuestJournalScreen), "the buttons click with the backpack open");
			context.setScreen(() -> null);

			// J and L go from the journal to the advancements and back
			context.setScreen(QuestJournalScreen::new);
			context.waitForScreen(QuestJournalScreen.class);
			context.runOnClient(client -> client.gui.screen().keyPressed(new KeyEvent(InputConstants.KEY_L, 0, 0)));
			context.waitForScreen(AdvancementsListScreen.class);
			context.runOnClient(client -> client.gui.screen().keyPressed(new KeyEvent(InputConstants.KEY_J, 0, 0)));
			context.waitForScreen(QuestJournalScreen.class);
			context.runOnClient(client -> client.gui.screen().keyPressed(new KeyEvent(InputConstants.KEY_J, 0, 0)));
			context.waitTicks(2);
			check(context.computeOnClient(client -> client.gui.screen() == null), "J closes the journal");

			// The followed quest done stays a few seconds, struck through, before the next one slides in
			context.runOnClient(client -> AchievementBanner.clear());
			world.getServer().runCommand("advancement grant @a only minecraft:story/obtain_armor");
			world.getConnection().waitForClientboundPackets();
			context.waitTicks(20);
			context.runOnClient(client -> AchievementBanner.clear());
			context.takeScreenshot("cassonade-quest-tracker-done");
			context.waitTicks(70);
			check(!context.computeOnClient(client -> QuestLog.journal(client).current().id().getPath()).equals("story/obtain_armor"),
					"the next quest follows the one done");
			context.takeScreenshot("cassonade-quest-tracker-next");
		}
	}

	/** Clicks the first of the journal's and the advancements' buttons on the screen open. */
	private static void clickQuestButton(ClientGameTestContext context) {
		context.runOnClient(client -> {
			AbstractWidget button = client.gui.screen().children().stream()
					.filter(widget -> widget.getClass().getSimpleName().equals("QuestButton"))
					.map(AbstractWidget.class::cast).findFirst().orElseThrow(() -> new AssertionError("Expected: the quest buttons on the screen"));
			MouseButtonEvent event = new MouseButtonEvent(button.getX() + button.getWidth() / 2.0, button.getY() + button.getHeight() / 2.0,
					new MouseButtonInfo(InputConstants.MOUSE_BUTTON_LEFT, 0));
			client.gui.screen().mouseClicked(event, false);
			client.gui.screen().mouseReleased(event);
		});
		context.waitTicks(2);
	}

	private static void check(boolean condition, String what) {
		if (!condition) {
			throw new AssertionError("Expected: " + what);
		}
	}

	@SuppressWarnings("unchecked")
	private static <T> T get(Object target, String field) {
		try {
			Field declared = target.getClass().getDeclaredField(field);
			declared.setAccessible(true);
			return (T) declared.get(target);
		} catch (ReflectiveOperationException e) {
			throw new AssertionError(e);
		}
	}

	private static void set(Object target, String field, Object value) {
		try {
			Field declared = target.getClass().getDeclaredField(field);
			declared.setAccessible(true);
			declared.set(target, value);
		} catch (ReflectiveOperationException e) {
			throw new AssertionError(e);
		}
	}

	private static Object filterTodo() {
		try {
			Class<?> filter = Class.forName(AdvancementsListScreen.class.getName() + "$Filter");
			return filter.getEnumConstants()[1];
		} catch (ClassNotFoundException e) {
			throw new AssertionError(e);
		}
	}

	private static void banner(ClientGameTestContext context, TestSingleplayerContext world, String advancement, String name) {
		world.getServer().runCommand("advancement grant @a only minecraft:" + advancement);
		world.getConnection().waitForClientboundPackets();
		context.waitTicks(1);
		sleep(BANNER_SHOWN_MS);
		context.takeScreenshot("cassonade-quest-banner-" + name);
		sleep(BANNER_GONE_MS);
		context.waitTicks(1);
	}

	private static void sleep(long ms) {
		try {
			Thread.sleep(ms);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new AssertionError(e);
		}
	}
}
