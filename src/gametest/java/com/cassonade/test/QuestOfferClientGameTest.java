package com.cassonade.test;

import com.mojang.blaze3d.platform.InputConstants;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

import net.fabricmc.fabric.api.client.screen.v1.Screens;

import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.ChatScreen;

import com.cassonade.client.CassonadeClient;
import com.cassonade.client.quest.QuestJournalScreen;
import com.cassonade.client.quest.QuestLog;
import com.cassonade.client.quest.QuestOffer;

/** The quest journal's offer beside the game: answered with its keys or its buttons, gone by itself if ignored. */
public class QuestOfferClientGameTest implements FabricClientGameTest {
	@Override
	public void runTest(ClientGameTestContext context) {
		if (TestSelection.skips(this)) {
			return;
		}
		context.getInput().resizeWindow(1920, 1080);
		JournalOffer.shown = true;
		try {
			try (TestSingleplayerContext world = context.worldBuilder().create()) {
				world.getConnection().waitForChunksRender();
				awaitOffer(context);
				check(context.computeOnClient(client -> client.gui.screen() == null), "the offer leaves the game free");
				context.waitTicks(10);
				Screenshots.take(context, "cassonade-quest-offer");

				// Ignored: it goes away without an answer, and comes back on the next join
				context.waitFor(client -> !QuestOffer.isShown(), 20 * 30);
				check(context.computeOnClient(client -> !QuestLog.hasChosen()), "ignoring the offer is no answer");
				context.waitTicks(20);
				context.runOnClient(client -> QuestOffer.reset());
				awaitOffer(context);

				context.getInput().pressKey(CassonadeClient.OPEN_JOURNAL);
				context.waitTicks(2);
				check(context.computeOnClient(client -> QuestLog.isEnabled() && client.gui.screen() == null),
						"the journal's key follows the journal, without opening it");
			}

			try (TestSingleplayerContext world = context.worldBuilder().create()) {
				world.getConnection().waitForChunksRender();
				awaitOffer(context);
				context.getInput().pressKey(CassonadeClient.QUEST_TRACKER);
				context.waitTicks(2);
				check(context.computeOnClient(client -> QuestLog.hasChosen() && !QuestLog.isEnabled()), "the tracker's key declines");
			}

			try (TestSingleplayerContext world = context.worldBuilder().create()) {
				world.getConnection().waitForChunksRender();
				awaitOffer(context);
				context.setScreen(() -> new ChatScreen("", false));
				context.waitForScreen(ChatScreen.class);
				// The time left does not run while the cursor is free
				context.waitTicks(10);
				double[] follow = context.computeOnClient(client -> {
					int guiWidth = client.getWindow().getGuiScaledWidth();
					double scale = client.getWindow().getGuiScale();
					for (int y = 0; y < client.getWindow().getGuiScaledHeight(); y++) {
						if (Boolean.TRUE.equals(QuestOffer.choiceAt(client, guiWidth, guiWidth - 20, y))) {
							return new double[] {(guiWidth - 20) * scale, (y + 5) * scale};
						}
					}
					throw new AssertionError("no button to follow the journal");
				});
				context.getInput().setCursorPos(follow[0], follow[1]);
				context.waitTicks(2);
				Screenshots.take(context, "cassonade-quest-offer-chat");
				context.getInput().pressMouse(InputConstants.MOUSE_BUTTON_LEFT);
				context.waitTicks(2);
				check(context.computeOnClient(client -> QuestLog.isEnabled()), "the button can be clicked from the chat");

				// The journal's button in the corner works at once, without reopening the chat
				double[] journal = context.computeOnClient(client -> {
					double scale = client.getWindow().getGuiScale();
					AbstractWidget button = Screens.getWidgets(client.gui.screen()).getLast();
					return new double[] {(button.getX() + button.getWidth() / 2.0) * scale, (button.getY() + button.getHeight() / 2.0) * scale};
				});
				context.getInput().setCursorPos(journal[0], journal[1]);
				context.waitTicks(2);
				context.getInput().pressMouse(InputConstants.MOUSE_BUTTON_LEFT);
				context.waitForScreen(QuestJournalScreen.class);
				context.setScreen(() -> null);
			}
		} finally {
			JournalOffer.shown = false;
		}
	}

	private static void awaitOffer(ClientGameTestContext context) {
		context.waitFor(client -> QuestOffer.isShown());
	}

	private static void check(boolean condition, String what) {
		if (!condition) {
			throw new AssertionError(what);
		}
	}
}
