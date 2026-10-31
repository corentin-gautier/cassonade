package com.cassonade.test;

import java.util.function.Supplier;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.screens.AlertScreen;
import net.minecraft.client.gui.screens.BackupConfirmScreen;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.client.gui.screens.DirectJoinServerScreen;
import net.minecraft.client.gui.screens.DisconnectedScreen;
import net.minecraft.client.gui.screens.GenericMessageScreen;
import net.minecraft.client.gui.screens.ManageServerScreen;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.options.OptionsScreen;
import net.minecraft.client.gui.screens.options.VideoSettingsScreen;
import net.minecraft.client.gui.screens.social.SocialInteractionsScreen;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.network.chat.Component;

import com.cassonade.client.quest.QuestLog;
import com.cassonade.client.quest.QuestOptInScreen;

/** The menus beyond the title and selection screens: the dialogs, the world creation, the pause and death screens, at the usual size and at the smallest GUI. */
public class MenuScreensClientGameTest implements FabricClientGameTest {
	@Override
	public void runTest(ClientGameTestContext context) {
		if (TestSelection.skips(this)) {
			return;
		}
		context.getInput().resizeWindow(1920, 1080);
		dialogs(context, "");
		setLanguage(context, "fr_fr");
		context.getInput().resizeWindow(1280, 720);
		context.waitTicks(5);
		dialogs(context, "-small-fr");
		setLanguage(context, "en_us");
		context.getInput().resizeWindow(1920, 1080);

		context.setScreen(TitleScreen::new);
		try (TestSingleplayerContext world = context.worldBuilder().create()) {
			context.waitForScreen(QuestOptInScreen.class);
			context.runOnClient(client -> QuestLog.setEnabled(false));
			context.setScreen(() -> null);
			world.getConnection().waitForChunksRender();
			take(context, "pause", () -> new PauseScreen(true));
			// The button answers where it was moved to
			context.clickScreenButton("menu.options");
			context.waitForScreen(OptionsScreen.class);
			take(context, "social", SocialInteractionsScreen::new);
			context.setScreen(() -> null);

			world.getServer().runCommand("kill @a");
			context.waitForScreen(DeathScreen.class);
			context.waitTicks(30);
			checkFit(context);
			Screenshots.take(context, "cassonade-menu-death");
			context.clickScreenButton("deathScreen.respawn");
			context.waitFor(client -> client.gui.screen() == null);
		}
	}

	private static void dialogs(ClientGameTestContext context, String suffix) {
		Screen title = context.computeOnClient(client -> new TitleScreen());
		ServerData server = new ServerData("Minecraft Server", "localhost", ServerData.Type.OTHER);
		take(context, "confirm" + suffix, () -> new ConfirmScreen(yes -> {}, Component.translatable("selectWorld.deleteQuestion"),
				Component.translatable("selectWorld.deleteWarning", "New World")));
		take(context, "alert" + suffix, () -> new AlertScreen(() -> {}, Component.translatable("selectWorld.backupQuestion.customized"),
				Component.translatable("selectWorld.backupWarning.customized")));
		take(context, "backup" + suffix, () -> new BackupConfirmScreen(() -> {}, (backup, erase) -> {}, Component.translatable("selectWorld.backupQuestion.customized"),
				Component.translatable("selectWorld.backupWarning.customized"), true));
		context.runOnClient(client -> client.gui.screen().children().stream().filter(Checkbox.class::isInstance).map(Checkbox.class::cast).findFirst().orElseThrow()
				.onClick(new MouseButtonEvent(0, 0, new MouseButtonInfo(0, 0)), false));
		context.waitTicks(5);
		Screenshots.take(context, "cassonade-menu-backup-checked" + suffix);
		take(context, "disconnected" + suffix, () -> new DisconnectedScreen(title, Component.translatable("connect.failed"),
				Component.literal("Connection refused: getsockopt")));
		take(context, "direct-join" + suffix, () -> new DirectJoinServerScreen(title, ok -> {}, server));
		take(context, "manage-server" + suffix, () -> new ManageServerScreen(title, Component.translatable("manageServer.add.title"), ok -> {}, server));
		take(context, "preparing" + suffix, () -> new GenericMessageScreen(Component.translatable("createWorld.preparing")));
		take(context, "message" + suffix, () -> new GenericMessageScreen(Component.translatable("recover_world.restoring")));
		context.runOnClient(client -> CreateWorldScreen.openFresh(client, () -> client.gui.setScreen(new TitleScreen())));
		context.waitForScreen(CreateWorldScreen.class);
		context.waitTicks(5);
		Screenshots.take(context, "cassonade-menu-create-world" + suffix);
		take(context, "options" + suffix, () -> new OptionsScreen(title, Minecraft.getInstance().options));
		take(context, "video" + suffix, () -> new VideoSettingsScreen(title, Minecraft.getInstance(), Minecraft.getInstance().options));
		context.setScreen(TitleScreen::new);
	}

	private static void take(ClientGameTestContext context, String name, Supplier<Screen> screen) {
		context.setScreen(screen::get);
		context.waitTicks(5);
		checkFit(context);
		Screenshots.take(context, "cassonade-menu-" + name);
	}

	private static void checkFit(ClientGameTestContext context) {
		boolean fit = context.computeOnClient(client -> client.gui.screen().children().stream()
				.filter(AbstractWidget.class::isInstance).map(AbstractWidget.class::cast)
				.allMatch(widget -> widget.getY() >= 0 && widget.getY() + widget.getHeight() <= client.gui.screen().height));
		if (!fit) {
			throw new AssertionError("Expected: every widget of the screen on screen");
		}
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
}
