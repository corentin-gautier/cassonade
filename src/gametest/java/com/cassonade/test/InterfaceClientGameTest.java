package com.cassonade.test;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import com.mojang.blaze3d.platform.InputConstants;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

import net.minecraft.client.Minecraft;
import net.minecraft.client.sounds.SoundEventListener;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.InventoryMenu;

import com.cassonade.client.ClientSettings;
import com.cassonade.client.ClientSettings.MarkerDisplay;
import com.cassonade.client.ClientSettings.QuestTrackerDisplay;
import com.cassonade.client.CassonadeClient;
import com.cassonade.client.map.MarkerDisplayButton;
import com.cassonade.client.map.Minimap;
import com.cassonade.client.map.MinimapRotationButton;
import com.cassonade.client.quest.AchievementBanner;
import com.cassonade.client.quest.QuestLog;
import com.cassonade.client.quest.QuestOptInScreen;

/**
 * The HUD and the buttons around it: the keys printed on them, and the keys and buttons switching how
 * the minimap, the markers and the quest followed are shown.
 */
public class InterfaceClientGameTest implements FabricClientGameTest {
	@Override
	public void runTest(ClientGameTestContext context) {
		if (TestSelection.skips(this)) {
			return;
		}
		context.getInput().resizeWindow(1920, 1080);
		try (TestSingleplayerContext world = context.worldBuilder().create()) {
			world.getConnection().waitForChunksRender();
			// The journal on, for the quest followed under the minimap
			context.waitForScreen(QuestOptInScreen.class);
			context.runOnClient(client -> QuestLog.setEnabled(true));
			context.setScreen(() -> null);
			world.getServer().runCommand("gamemode survival @a");
			world.getServer().runCommand("give @a compass");
			context.waitTicks(5);

			// The compass, shift-clicked from the hotbar into its slot, shows the minimap
			context.setScreen(() -> new InventoryScreen(Minecraft.getInstance().player));
			context.waitForScreen(InventoryScreen.class);
			context.runOnClient(client -> client.gameMode.handleContainerInput(
					client.player.inventoryMenu.containerId, InventoryMenu.USE_ROW_SLOT_START, 0, ContainerInput.QUICK_MOVE, client.player));
			world.getConnection().waitForServerboundPackets();
			world.getConnection().waitForClientboundPackets();
			context.waitTicks(2);
			check(context.computeOnClient(Minimap::isShown), "the compass worn shows the minimap");

			// Beside the minimap, the rotation and marker display buttons, each with its key
			context.runOnClient(client -> {
				ClientSettings.setMinimapRotates(false);
				ClientSettings.setMarkerDisplay(MarkerDisplay.ICON_AND_TEXT);
			});
			quiet(context);
			context.takeScreenshot("cassonade-interface-minimap-buttons");
			clickWidget(context, MinimapRotationButton.class);
			check(context.computeOnClient(client -> ClientSettings.minimapRotates()), "the rotation button turns the minimap with the player");
			clickWidget(context, MarkerDisplayButton.class);
			check(context.computeOnClient(client -> ClientSettings.markerDisplay() == MarkerDisplay.ICON), "the marker display button moves to icons only");
			context.takeScreenshot("cassonade-interface-minimap-buttons-switched");
			clickWidget(context, MinimapRotationButton.class);
			check(context.computeOnClient(client -> !ClientSettings.minimapRotates()), "the rotation button brings north back up");
			context.runOnClient(client -> ClientSettings.setMarkerDisplay(MarkerDisplay.ICON_AND_TEXT));

			// In game, the keys printed on the HUD: R on the minimap, K on the quest followed
			context.setScreen(() -> null);
			quiet(context);
			context.takeScreenshot("cassonade-interface-hud-keys");
			context.getInput().pressKey(CassonadeClient.MINIMAP_ROTATION);
			context.waitTicks(2);
			check(context.computeOnClient(client -> ClientSettings.minimapRotates()), "the rotation key turns the minimap with the player");
			context.takeScreenshot("cassonade-interface-minimap-rotating");
			context.getInput().pressKey(CassonadeClient.MINIMAP_ROTATION);
			context.waitTicks(2);
			check(context.computeOnClient(client -> !ClientSettings.minimapRotates()), "the rotation key brings north back up");

			context.getInput().pressKey(CassonadeClient.MARKER_DISPLAY);
			context.waitTicks(2);
			check(context.computeOnClient(client -> ClientSettings.markerDisplay() == MarkerDisplay.ICON), "the marker display key moves to icons only");
			context.runOnClient(client -> ClientSettings.setMarkerDisplay(MarkerDisplay.ICON_AND_TEXT));

			context.runOnClient(client -> ClientSettings.setQuestTrackerDisplay(QuestTrackerDisplay.FULL));
			context.getInput().pressKey(CassonadeClient.QUEST_TRACKER);
			context.waitTicks(2);
			check(context.computeOnClient(client -> ClientSettings.questTrackerDisplay() == QuestTrackerDisplay.COMPACT),
					"the quest tracker key shrinks the quest followed");
			context.runOnClient(client -> ClientSettings.setQuestTrackerDisplay(QuestTrackerDisplay.FULL));

			// The chat, in game and open: its messages on the wood's dark brown, framed while it is open
			for (String message : new String[] {"Bonjour !", "Who has some iron to spare?", "Meet me at the village, by the well"}) {
				world.getServer().runCommand("say " + message);
			}
			context.waitTicks(5);
			quiet(context);
			context.takeScreenshot("cassonade-interface-chat-in-game");
			context.setScreen(() -> new ChatScreen("/gamemode adventures", false));
			context.waitForScreen(ChatScreen.class);
			context.waitTicks(5);
			context.takeScreenshot("cassonade-interface-chat-open");
			// The suggestions for what is typed, below the field like the command's usage
			context.setScreen(() -> new ChatScreen("/", false));
			context.waitForScreen(ChatScreen.class);
			context.getInput().typeChars("t");
			context.waitTicks(5);
			context.takeScreenshot("cassonade-interface-chat-suggestions");
			// The arrow keys stay in the field, for the history, over the buttons beside the minimap
			context.getInput().pressKey(InputConstants.KEY_UP);
			context.getInput().pressKey(InputConstants.KEY_DOWN);
			check(context.computeOnClient(client -> client.gui.screen().getFocused() instanceof EditBox), "the arrow keys keep the chat's field focused");
			context.setScreen(() -> null);

			// An enchanted pickaxe in hand wearing down past 5 % of its durability: a crack, a message and the
			// item blinking in the hotbar. Taken in hand already worn, it stays quiet
			List<String> sounds = new CopyOnWriteArrayList<>();
			SoundEventListener listener = (sound, event, range) -> sounds.add(sound.getIdentifier().getPath());
			context.runOnClient(client -> {
				client.player.getInventory().setSelectedSlot(0);
				client.getSoundManager().addListener(listener);
			});
			world.getServer().runCommand("item replace entity @a hotbar.0 with diamond_pickaxe[damage=1400,enchantments={efficiency:3}]");
			context.waitTicks(3);
			check(sounds.isEmpty(), "a worn pickaxe taken in hand makes no sound (" + sounds + ")");
			world.getServer().runCommand("item replace entity @a hotbar.0 with diamond_pickaxe[damage=1500,enchantments={efficiency:3}]");
			context.waitTicks(3);
			context.takeScreenshot("cassonade-interface-durability-warning");
			context.runOnClient(client -> client.getSoundManager().removeListener(listener));
			check(sounds.contains("entity.item.break"), "a pickaxe wearing past 5 % cracks (" + sounds + ")");
			world.getServer().runCommand("item replace entity @a hotbar.0 with air");

			// A tutorial toast, as when joining a server: its title gold and its text light on the dark frame
			context.runOnClient(client -> client.gui.addSocialInteractionsToast());
			context.waitTicks(20);
			context.takeScreenshot("cassonade-interface-tutorial-toast");
		}
	}

	/** Clicks the middle of the screen's widget of that class. */
	private static void clickWidget(ClientGameTestContext context, Class<? extends AbstractWidget> type) {
		context.runOnClient(client -> {
			AbstractWidget widget = client.gui.screen().children().stream().filter(type::isInstance).map(type::cast).findFirst()
					.orElseThrow(() -> new AssertionError("Expected: a " + type.getSimpleName() + " on the screen"));
			MouseButtonEvent event = new MouseButtonEvent(widget.getX() + widget.getWidth() / 2.0, widget.getY() + widget.getHeight() / 2.0,
					new MouseButtonInfo(InputConstants.MOUSE_BUTTON_LEFT, 0));
			client.gui.screen().mouseClicked(event, false);
			client.gui.screen().mouseReleased(event);
		});
		context.waitTicks(2);
	}

	/** Clears the advancements' banners and the recipes' toasts, which would cover the minimap. */
	private static void quiet(ClientGameTestContext context) {
		context.runOnClient(client -> {
			AchievementBanner.clear();
			client.gui.toastManager().clear();
		});
		context.waitTicks(1);
	}

	private static void check(boolean condition, String what) {
		if (!condition) {
			throw new AssertionError("Expected: " + what);
		}
	}
}
