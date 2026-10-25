package com.cassonade.test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import com.mojang.blaze3d.platform.InputConstants;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import com.cassonade.client.BackpackPanel;
import com.cassonade.client.ClockHud;
import com.cassonade.client.map.Minimap;
import com.cassonade.client.mixin.AbstractRecipeBookScreenAccessor;
import com.cassonade.client.quest.QuestLog;
import com.cassonade.client.quest.QuestOptInScreen;
import com.cassonade.gear.GearSlot;
import com.cassonade.gear.PlayerGear;

/**
 * The mod on the client only, on Minecraft's own server (the jar Loom downloaded, see build.gradle),
 * run aside and driven from its console: the client must stay connected and usable, its features
 * falling back on vanilla's behavior.
 */
public class VanillaServerClientGameTest implements FabricClientGameTest {
	private static final int TICKS_TO_START = 20 * 180;

	@Override
	public void runTest(ClientGameTestContext context) {
		if (TestSelection.skips(this)) {
			return;
		}
		String jar = System.getProperty("cassonade.vanillaServerJar");
		if (jar == null || !Files.isRegularFile(Path.of(jar))) {
			throw new AssertionError("Expected: the vanilla server jar, from the cassonade.vanillaServerJar property (" + jar + ")");
		}
		context.getInput().resizeWindow(1920, 1080);
		List<String> log = new CopyOnWriteArrayList<>();
		Process server = null;
		try {
			int port = freePort();
			server = start(Path.of(jar), port, log);
			Process running = server;
			context.waitFor(client -> log.stream().anyMatch(line -> line.contains("Done (")) || !running.isAlive(), TICKS_TO_START);
			check(server.isAlive(), "the vanilla server starts (" + tail(log) + ")");
			Writer console = new OutputStreamWriter(server.getOutputStream(), StandardCharsets.UTF_8);

			context.runOnClient(client -> ConnectScreen.startConnecting(client.gui.screen(), client, ServerAddress.parseString("localhost:" + port),
					new ServerData("vanilla", "localhost:" + port, ServerData.Type.OTHER), false, null));
			context.waitFor(client -> client.level != null && client.player != null, 20 * 60);
			context.waitTicks(40);
			if (context.computeOnClient(client -> client.gui.screen() instanceof QuestOptInScreen)) {
				context.runOnClient(client -> QuestLog.setEnabled(false));
				context.setScreen(() -> null);
			}
			String name = context.computeOnClient(client -> client.player.getGameProfile().name());
			command(console, "gamemode survival " + name);
			command(console, "give " + name + " compass");
			command(console, "give " + name + " clock");
			command(console, "give " + name + " oak_planks 64");
			context.waitTicks(20);
			check(context.computeOnClient(client -> client.player.getInventory().countItem(Items.COMPASS) == 1), "the vanilla server gives the compass");

			// No gear on a server that has no slot for it: a shift click on the compass moves it as vanilla does, the minimap stays hidden
			context.setScreen(() -> new InventoryScreen(Minecraft.getInstance().player));
			context.waitForScreen(InventoryScreen.class);
			context.takeScreenshot("cassonade-vanilla-server-inventory");
			quickMove(context, InventoryMenu.USE_ROW_SLOT_START);
			context.waitTicks(20);
			check(connected(context), "still connected after a shift click on the compass");
			check(context.computeOnClient(client -> !PlayerGear.isWearing(client.player, GearSlot.COMPASS) && !Minimap.isShown(client)
					&& client.player.getInventory().countItem(Items.COMPASS) == 1), "the compass stays in the inventory, no minimap");
			check(!context.computeOnClient(client -> BackpackPanel.isWorn()), "no backpack tab without a backpack");

			// The recipe book opens and closes with its ribbon, all on the client
			context.runOnClient(client -> ((AbstractRecipeBookScreenAccessor) client.gui.screen()).cassonade$getRecipeBookComponent().toggleVisibility());
			context.waitTicks(2);
			context.takeScreenshot("cassonade-vanilla-server-recipe-book");
			context.setScreen(() -> null);

			// A chest: sorting asks a server that cannot answer, nothing must break
			BlockPos chest = context.computeOnClient(client -> client.player.blockPosition().east(2));
			command(console, "setblock " + chest.getX() + " " + chest.getY() + " " + chest.getZ()
					+ " chest{Items:[{Slot:3b,id:\"minecraft:dirt\",count:5},{Slot:10b,id:\"minecraft:stone\",count:7}]}");
			context.waitTicks(20);
			useOn(context, chest);
			context.waitForScreen(ContainerScreen.class);
			context.waitTicks(2);
			context.takeScreenshot("cassonade-vanilla-server-chest");
			context.runOnClient(client -> client.gui.screen().children().stream()
					.filter(widget -> widget instanceof ImageButton).map(ImageButton.class::cast)
					.forEach(button -> button.onPress(new KeyEvent(InputConstants.KEY_RETURN, 0, 0))));
			context.waitTicks(20);
			check(connected(context), "still connected after pressing the chest's buttons");
			context.runOnClient(client -> client.player.closeContainer());

			// Ripe wheat right-clicked: vanilla does nothing, the client must not pretend otherwise
			BlockPos wheat = context.computeOnClient(client -> client.player.blockPosition().south(2));
			command(console, "setblock " + wheat.getX() + " " + (wheat.getY() - 1) + " " + wheat.getZ() + " farmland");
			command(console, "setblock " + wheat.getX() + " " + wheat.getY() + " " + wheat.getZ() + " wheat[age=7]");
			context.waitTicks(20);
			useOn(context, wheat);
			context.waitTicks(20);
			check(connected(context), "still connected after right-clicking ripe wheat");

			// A clock in the hand shows nothing, and a while in the world keeps the connection
			check(!context.computeOnClient(client -> ClockHud.isShown()), "no clock panel without the clock slot");
			context.waitTicks(100);
			check(connected(context), "still connected after five seconds");
			// From the player's arrival on: the server logs an error of its own while starting
			int joined = log.indexOf(log.stream().filter(line -> line.contains("joined the game")).findFirst().orElseThrow());
			List<String> played = log.subList(joined, log.size());
			check(played.stream().noneMatch(line -> line.contains("lost connection") || line.contains("Exception") || line.contains("/ERROR]")),
					"no error on the vanilla server (" + tail(played) + ")");

			context.runOnClient(client -> {
				client.level.disconnect(Component.literal("Disconnecting"));
				client.disconnectWithSavingScreen();
			});
			context.waitFor(client -> client.level == null);
			context.waitTicks(2);
			context.setScreen(TitleScreen::new);
			command(console, "stop");
			check(server.waitFor(60, TimeUnit.SECONDS), "the vanilla server stops");
		} catch (IOException | InterruptedException e) {
			throw new AssertionError(e);
		} finally {
			if (server != null && server.isAlive()) {
				server.destroyForcibly();
			}
		}
	}

	/** A fresh world each time, in offline mode so the test player can join. */
	private static Process start(Path jar, int port, List<String> log) throws IOException {
		Path dir = Path.of("vanilla-server").toAbsolutePath();
		if (Files.exists(dir)) {
			try (Stream<Path> files = Files.walk(dir)) {
				files.sorted(Comparator.reverseOrder()).forEach(path -> path.toFile().delete());
			}
		}
		Files.createDirectories(dir);
		Files.writeString(dir.resolve("eula.txt"), "eula=true\n");
		Files.writeString(dir.resolve("server.properties"), String.join("\n",
				"online-mode=false", "white-list=false", "server-port=" + port, "level-type=minecraft\\:flat", "generate-structures=false",
				"spawn-protection=0", "view-distance=6", "simulation-distance=6", "enforce-secure-profile=false", "spawn-monsters=false", ""));
		String java = Path.of(System.getProperty("java.home"), "bin", "java").toString();
		Process process = new ProcessBuilder(java, "-Xmx1G", "-jar", jar.toAbsolutePath().toString(), "nogui")
				.directory(dir.toFile()).redirectErrorStream(true).start();
		Thread reader = new Thread(() -> {
			try (BufferedReader lines = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
				for (String line; (line = lines.readLine()) != null; ) {
					log.add(line);
				}
			} catch (IOException ignored) {
				// The server stopped
			}
		}, "Vanilla server log");
		reader.setDaemon(true);
		reader.start();
		return process;
	}

	private static int freePort() throws IOException {
		try (ServerSocket socket = new ServerSocket(0)) {
			return socket.getLocalPort();
		}
	}

	private static void command(Writer console, String command) throws IOException {
		console.write(command + "\n");
		console.flush();
	}

	/** A right click on the top of a block, as the use key would. */
	private static void useOn(ClientGameTestContext context, BlockPos pos) {
		context.runOnClient(client -> client.gameMode.useItemOn(client.player, InteractionHand.MAIN_HAND,
				new BlockHitResult(Vec3.atCenterOf(pos).add(0, 0.5, 0), Direction.UP, pos, false)));
	}

	private static boolean connected(ClientGameTestContext context) {
		return context.computeOnClient(client -> client.level != null && client.getConnection() != null && client.getConnection().getConnection().isConnected());
	}

	private static void quickMove(ClientGameTestContext context, int slot) {
		context.runOnClient(client -> client.gameMode.handleContainerInput(
				client.player.inventoryMenu.containerId, slot, 0, ContainerInput.QUICK_MOVE, client.player));
	}

	private static String tail(List<String> log) {
		return String.join("\n", log.subList(Math.max(0, log.size() - 15), log.size()));
	}

	private static void check(boolean condition, String what) {
		if (!condition) {
			throw new AssertionError("Expected: " + what);
		}
	}
}
