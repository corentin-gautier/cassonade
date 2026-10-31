package com.cassonade.test;

import java.util.List;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.screens.multiplayer.ServerSelectionList;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.ServerList;
import net.minecraft.client.server.LanServer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.status.ServerStatus;

import com.cassonade.client.ServerSelectionScreen;
import com.cassonade.client.WorldSelectionScreen;

/** The server selection screen, with a server in each state: nothing answers on these addresses, so the first is made to look online. */
public class ServerSelectionClientGameTest implements FabricClientGameTest {
	@Override
	public void runTest(ClientGameTestContext context) {
		if (TestSelection.skips(this)) {
			return;
		}
		context.getInput().resizeWindow(1920, 1080);
		setServers(context, List.of("Survie entre amis", "Le serveur de Camille", "Ancien serveur"));

		// Vanilla's screen, as the title screen opens it, gives way to the mod's, the first server selected
		context.setScreen(() -> new JoinMultiplayerScreen(new TitleScreen()));
		context.waitForScreen(ServerSelectionScreen.class);
		context.waitTicks(40);
		check(context.computeOnClient(client -> client.gui.screen() instanceof ServerSelectionScreen screen
				&& "Survie entre amis".equals(screen.selectedName())), "the first server selected as the screen opens");
		context.runOnClient(client -> {
			ServerSelectionList list = list(client.gui.screen());
			ServerData online = ((ServerSelectionList.OnlineServerEntry) list.children().getFirst()).getServerData();
			online.setState(ServerData.State.SUCCESSFUL);
			online.motd = Component.literal("Une survie tranquille, sans pression");
			online.players = new ServerStatus.Players(20, 3, List.of());
			online.ping = 42;
			online.playerList = List.of(Component.literal("Camille"), Component.literal("Noé"), Component.literal("Sacha"));
			list.updateNetworkServers(List.of(new LanServer("Monde de Camille", "192.168.1.12:53412")));
		});
		context.waitTicks(2);
		Screenshots.take(context, "cassonade-server-selection");

		// Hovering the second row's icon shows its actions
		hoverRow(context, 1);
		Screenshots.take(context, "cassonade-server-selection-hovered");

		// A click on the upper left of its icon moves it up, in the saved list too; the widgets stay usable
		context.getInput().pressMouse(0);
		context.waitTicks(2);
		check(context.computeOnClient(client -> "Le serveur de Camille".equals(((JoinMultiplayerScreen) client.gui.screen()).getServers().get(0).name)),
				"a click on the arrow to move a server up the list");
		context.getInput().setCursorPos(0, 0);

		// A world on the local network, at the end of the list
		context.runOnClient(client -> {
			ServerSelectionList list = list(client.gui.screen());
			list.setSelected(list.children().getLast());
			list.setScrollAmount(list.maxScrollAmount());
		});
		context.waitTicks(2);
		Screenshots.take(context, "cassonade-server-selection-lan");

		// The smallest GUI (scale 3 in 1280×720: 426×240), in French, whose words are longer
		setLanguage(context, "fr_fr");
		context.getInput().resizeWindow(1280, 720);
		context.waitTicks(5);
		Screenshots.take(context, "cassonade-server-selection-small-fr");
		setLanguage(context, "en_us");
		context.getInput().resizeWindow(1920, 1080);

		// Alone in the list, a server has nothing to move: the arrow to join it takes the middle of its icon
		setServers(context, List.of("Survie entre amis"));
		context.setScreen(() -> new JoinMultiplayerScreen(new TitleScreen()));
		context.waitForScreen(ServerSelectionScreen.class);
		context.waitTicks(5);
		hoverRow(context, 0);
		Screenshots.take(context, "cassonade-server-selection-alone");
		context.getInput().setCursorPos(0, 0);

		// Back on the title screen through the mod's own back button, without a server left behind for the other tests
		context.clickScreenButton("gui.back");
		context.waitForScreen(TitleScreen.class);
		setServers(context, List.of());
	}

	private static ServerSelectionList list(Screen screen) {
		return screen.children().stream().filter(ServerSelectionList.class::isInstance).map(ServerSelectionList.class::cast).findFirst().orElseThrow();
	}

	/** Puts the cursor on the upper left of a row's icon. */
	private static void hoverRow(ClientGameTestContext context, int row) {
		double[] cursor = context.computeOnClient(client -> {
			ServerSelectionList.Entry entry = list(client.gui.screen()).children().get(row);
			return new double[] {(entry.getX() + WorldSelectionScreen.ROW_ICON_INSET + 7) * client.getWindow().getGuiScale(),
					(entry.getY() + WorldSelectionScreen.ROW_ICON_INSET + 7) * client.getWindow().getGuiScale()};
		});
		context.getInput().setCursorPos(cursor[0], cursor[1]);
		context.waitTicks(2);
	}

	/** Addresses that nothing answers on: one refuses the connection, another cannot be resolved. */
	private static void setServers(ClientGameTestContext context, List<String> names) {
		context.runOnClient(client -> {
			ServerList servers = new ServerList(client);
			servers.load();
			while (servers.size() > 0) {
				servers.remove(servers.get(0));
			}
			for (int i = 0; i < names.size(); i++) {
				servers.add(new ServerData(names.get(i), i == 2 ? "serveur.invalid" : "127.0.0.1:" + (i + 1), ServerData.Type.OTHER), false);
			}
			servers.save();
		});
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
}
