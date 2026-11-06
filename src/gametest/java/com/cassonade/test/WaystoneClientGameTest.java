package com.cassonade.test;

import static com.cassonade.test.PlannedWorld.check;
import static com.cassonade.test.PlannedWorld.player;

import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import com.cassonade.client.map.WaystoneMarkers;
import com.cassonade.client.map.WorldMapScreen;
import com.cassonade.client.waystone.WaystoneScreen;
import com.cassonade.gear.GearSlot;
import com.cassonade.gear.PlayerGear;
import com.cassonade.item.CassonadeItems;
import com.cassonade.waystone.WaystoneCrystalBlockEntity;
import com.cassonade.waystone.WaystoneData;
import com.cassonade.waystone.WaystoneListPayload;
import com.cassonade.waystone.WaystoneRenamePayload;
import com.cassonade.waystone.WaystoneTravelPayload;
import com.cassonade.waystone.Waystones;

public class WaystoneClientGameTest implements FabricClientGameTest {
	@Override
	public void runTest(ClientGameTestContext context) {
		if (TestSelection.skips(this)) {
			return;
		}
		try (TestSingleplayerContext world = PlannedWorld.open(context)) {
			// One item for the two halves, whichever is broken; none without a pickaxe, none in Creative
			checkDrops(world, 0, Items.IRON_PICKAXE, 1, "the lower half broken");
			checkDrops(world, 1, Items.IRON_PICKAXE, 1, "the upper half broken");
			checkDrops(world, 0, Items.AIR, 0, "the lower half broken by hand");
			checkDrops(world, 1, Items.AIR, 0, "the upper half broken by hand");
			world.getServer().runCommand("gamemode creative @a");
			checkDrops(world, 1, Items.AIR, 0, "the upper half broken in Creative");
			world.getServer().runCommand("gamemode survival @a");

			// Alone, it can only be named
			world.getServer().runCommand("setblock 4 101 2 cassonade:waystone");
			world.getServer().runCommand("setblock 4 102 2 cassonade:waystone[half=upper]");
			context.waitTicks(5);
			aim(context, world);
			use(context);
			context.waitForScreen(WaystoneScreen.class);
			context.runOnClient(client -> check(client.gui.screen().children().stream().noneMatch(Button.class::isInstance), "no destination with one waystone"));
			context.runOnClient(client -> {
				EditBox box = client.gui.screen().children().stream().filter(EditBox.class::isInstance).map(EditBox.class::cast).findFirst().orElseThrow();
				box.setValue("  Home ");
			});
			ScreenInput.pressOnScreen(context, InputConstants.KEY_RETURN);
			context.waitTicks(5);
			world.getServer().runOnServer(server -> check(WaystoneData.of(server).at(GlobalPos.of(Level.OVERWORLD, new BlockPos(4, 101, 2))).orElseThrow().name().equals("Home"), "named from its screen"));

			world.getServer().runCommand("forceload add 10 10");
			world.getServer().runCommand("setblock 10 101 10 cassonade:waystone");
			world.getServer().runCommand("setblock 10 102 10 cassonade:waystone[half=upper]");
			world.getServer().runCommand("execute in minecraft:the_nether run forceload add 0 0");
			world.getServer().runCommand("execute in minecraft:the_nether run fill -1 99 -1 1 99 1 stone");
			world.getServer().runCommand("execute in minecraft:the_nether run fill -1 100 -1 1 102 1 air");
			world.getServer().runCommand("execute in minecraft:the_nether run setblock 0 100 0 cassonade:waystone");
			world.getServer().runCommand("execute in minecraft:the_nether run setblock 0 101 0 cassonade:waystone[half=upper]");
			world.getServer().runOnServer(server -> {
				WaystoneData data = WaystoneData.of(server);
				check(data.entries().size() == 3, "three waystones written down: " + data.entries());
				data.put(GlobalPos.of(Level.OVERWORLD, new BlockPos(10, 101, 10)), "Mine");
				data.discover(data.at(GlobalPos.of(Level.NETHER, new BlockPos(0, 100, 0))).orElseThrow(), player(server).getUUID());
			});

			// On the world map, once one is worn: this dimension's only
			context.runOnClient(client -> client.gui.setScreen(new WorldMapScreen()));
			context.waitTicks(5);
			context.runOnClient(client -> check(((WorldMapScreen) client.gui.screen()).waystonesHere().isEmpty(), "no waystones without a world map worn"));
			context.setScreen(() -> null);

			// On the minimap too, with only a compass worn, among the markers
			world.getServer().runOnServer(server -> player(server).setAttached(PlayerGear.ATTACHMENT, PlayerGear.of(player(server)).with(GearSlot.COMPASS, new ItemStack(Items.COMPASS))));
			// The minimap asks every 5 seconds, and the first answer can come before the last waystone is discovered
			context.waitFor(client -> WaystoneMarkers.here(client.level).size() == 2, 20 * 11);
			context.waitTicks(5);
			world.getServer().runCommand("execute as @p at @s run tp @s ~ ~ ~ facing 10.5 104 10.5");
			context.waitTicks(5);
			Screenshots.take(context, "cassonade-waystone-minimap");
			aim(context, world);
			world.getServer().runOnServer(server -> player(server).setAttached(PlayerGear.ATTACHMENT, PlayerGear.of(player(server)).with(GearSlot.COMPASS, ItemStack.EMPTY)));
			world.getServer().runOnServer(server -> player(server).setAttached(PlayerGear.ATTACHMENT, PlayerGear.of(player(server)).with(GearSlot.MAP, new ItemStack(CassonadeItems.WORLD_MAP))));
			context.waitTicks(5);
			context.waitTicks(25);
			checkOnMap(context, 2, "the overworld's two waystones, both near");
			// Another's, far away: unknown until the player has been near it
			world.getServer().runCommand("forceload add 300 300");
			world.getServer().runCommand("setblock 300 101 300 cassonade:waystone");
			world.getServer().runCommand("setblock 300 102 300 cassonade:waystone[half=upper]");
			context.waitTicks(25);
			checkOnMap(context, 2, "a far waystone not discovered yet");
			use(context);
			context.waitForScreen(WaystoneScreen.class);
			context.runOnClient(client -> check(client.gui.screen().children().stream().filter(Button.class::isInstance).count() == 2, "a far waystone not discovered yet: not in the list"));
			context.setScreen(() -> null);
			context.runOnClient(client -> ClientPlayNetworking.send(new WaystoneTravelPayload(new BlockPos(4, 101, 2), GlobalPos.of(Level.OVERWORLD, new BlockPos(300, 101, 300)))));
			context.waitTicks(10);
			world.getServer().runOnServer(server -> check(player(server).getX() < 100, "no trip to a waystone not discovered"));
			// The player's own: known at once, however far
			world.getServer().runCommand("setblock 302 101 300 cassonade:waystone");
			world.getServer().runOnServer(server -> {
				BlockPos own = new BlockPos(302, 101, 300);
				Waystones.BLOCK.setPlacedBy(server.overworld(), own, server.overworld().getBlockState(own), player(server), ItemStack.EMPTY);
				check(WaystoneData.of(server).at(GlobalPos.of(Level.OVERWORLD, own)).orElseThrow().discoveredBy().contains(player(server).getUUID()), "known to whoever placed it");
			});
			world.getServer().runCommand("setblock 302 101 300 air");
			world.getServer().runCommand("tp @a 290 101 300");
			context.waitTicks(25);
			aim(context, world);
			checkOnMap(context, 3, "a waystone discovered");
			world.getServer().runCommand("setblock 300 101 300 air");
			world.getServer().runCommand("forceload remove 300 300");
			context.runOnClient(client -> client.gui.setScreen(new WorldMapScreen()));
			context.waitTicks(5);
			ScreenInput.quiet(context);
			Screenshots.take(context, "cassonade-waystone-map");
			context.setScreen(() -> null);
			world.getServer().runOnServer(server -> player(server).setAttached(PlayerGear.ATTACHMENT, PlayerGear.EMPTY));

			// Another's cannot be renamed, its owner's can
			GlobalPos home = GlobalPos.of(Level.OVERWORLD, new BlockPos(4, 101, 2));
			world.getServer().runOnServer(server -> WaystoneData.of(server).claim(home, UUID.randomUUID()));
			context.runOnClient(client -> ClientPlayNetworking.send(new WaystoneRenamePayload(home.pos(), "Stolen")));
			context.waitTicks(5);
			world.getServer().runOnServer(server -> check(WaystoneData.of(server).at(home).orElseThrow().name().equals("Home"), "another's waystone not renamed"));
			use(context);
			context.waitForScreen(WaystoneScreen.class);
			context.runOnClient(client -> check(!client.gui.screen().children().stream().filter(EditBox.class::isInstance).map(EditBox.class::cast).findFirst().orElseThrow().canConsumeInput(), "another's name box locked"));
			context.setScreen(() -> null);
			world.getServer().runOnServer(server -> WaystoneData.of(server).claim(home, player(server).getUUID()));
			context.runOnClient(client -> ClientPlayNetworking.send(new WaystoneRenamePayload(home.pos(), "Home")));

			// Sorted by the arrows of the row under the mouse: Mine down, then back to the top with Shift, kept by the server
			use(context);
			context.waitForScreen(WaystoneScreen.class);
			hoverArrow(context, 0, false);
			context.waitTicks(2);
			Screenshots.take(context, "cassonade-waystone-order");
			clickArrow(context, 0, false, false);
			checkRows(context, "a waystone moved down", false);
			context.setScreen(() -> null);
			use(context);
			context.waitForScreen(WaystoneScreen.class);
			checkRows(context, "the order kept on opening again", false);
			clickArrow(context, 1, true, true);
			checkRows(context, "a waystone sent to the top", true);
			context.setScreen(() -> null);
			world.getServer().runOnServer(server -> check(WaystoneData.of(server).known(player(server).getUUID()).stream()
					.filter(entry -> !entry.pos().equals(home)).findFirst().orElseThrow().name().equals("Mine"), "the order written on the server"));
			// The player's own first, the others' under a heading: Nether's sent to the top stays below Mine
			world.getServer().runOnServer(server -> WaystoneData.of(server).claim(GlobalPos.of(Level.OVERWORLD, new BlockPos(10, 101, 10)), player(server).getUUID()));
			world.getServer().runOnServer(server -> {
				ServerPlayer player = player(server);
				WaystoneData.of(server).move(player.getUUID(), home, GlobalPos.of(Level.NETHER, new BlockPos(0, 100, 0)), true, true);
			});
			use(context);
			context.waitForScreen(WaystoneScreen.class);
			checkRows(context, "the player's own waystone above the others'", true);
			Screenshots.take(context, "cassonade-waystone-groups");
			context.setScreen(() -> null);

			// To the named one, first in the list
			use(context);
			context.waitForScreen(WaystoneScreen.class);
			Screenshots.take(context, "cassonade-waystone");
			press(context, 0);
			context.waitTicks(10);
			world.getServer().runOnServer(server -> {
				BlockPos at = player(server).blockPosition();
				check(at.getY() == 101 && at.distManhattan(new BlockPos(10, 101, 10)) <= 2, "at the foot of the other waystone, at " + at);
			});
			world.getServer().runCommand("tp @a 8.5 101 10.5 -90 -18");
			context.waitTicks(10);
			Screenshots.take(context, "cassonade-waystone-block");
			world.getServer().runCommand("tp @a 10.5 101 8.5 0 -18");
			context.waitTicks(23);
			Screenshots.take(context, "cassonade-waystone-block-turned");
			world.getServer().runCommand("item replace entity @a weapon.mainhand with cassonade:waystone");
			context.waitTicks(5);
			context.runOnClient(client -> check(client.level.getBlockEntity(new BlockPos(10, 102, 10)) instanceof WaystoneCrystalBlockEntity
					&& client.level.getBlockEntity(new BlockPos(10, 101, 10)) == null, "the crystal on the top half only"));
			world.getServer().runCommand("item replace entity @a weapon.mainhand with air");
			world.getServer().runCommand("item replace entity @a inventory.0 with cassonade:waystone");
			context.runOnClient(client -> client.gui.setScreen(new InventoryScreen(client.player)));
			context.waitTicks(5);
			Screenshots.take(context, "cassonade-waystone-item");
			context.setScreen(() -> null);

			// Blocked: no trip, the player stays
			world.getServer().runCommand("fill 3 101 1 5 102 3 stone replace air");
			world.getServer().runCommand("tp @a 8.5 101 10.5 90 0");
			context.waitTicks(10);
			context.runOnClient(client -> ClientPlayNetworking.send(new WaystoneTravelPayload(new BlockPos(10, 101, 10), GlobalPos.of(Level.OVERWORLD, new BlockPos(4, 101, 2)))));
			context.waitTicks(10);
			world.getServer().runOnServer(server -> check(player(server).getX() > 8, "walled in: no trip"));

			// Too far from any waystone: no trip
			world.getServer().runCommand("fill 3 101 1 5 102 3 air replace stone");
			context.runOnClient(client -> ClientPlayNetworking.send(new WaystoneTravelPayload(new BlockPos(100, 64, 100), GlobalPos.of(Level.OVERWORLD, new BlockPos(4, 101, 2)))));
			context.waitTicks(10);
			world.getServer().runOnServer(server -> check(player(server).getX() > 8, "a request from nowhere: no trip"));

			// Across dimensions
			aim(context, world);
			use(context);
			context.waitForScreen(WaystoneScreen.class);
			press(context, 1);
			context.waitTicks(20);
			context.runOnClient(client -> check(client.level.dimension() == Level.NETHER, "in the Nether"));

			// Each dimension's row, for the eye
			context.runOnClient(client -> WaystoneScreen.open(client, new WaystoneListPayload(BlockPos.ZERO, "", true, List.of(
					new WaystoneListPayload.Destination(GlobalPos.of(Level.OVERWORLD, new BlockPos(10, 64, 10)), "Mine"),
					new WaystoneListPayload.Destination(GlobalPos.of(Level.NETHER, new BlockPos(0, 100, 0)), ""),
					new WaystoneListPayload.Destination(GlobalPos.of(Level.END, new BlockPos(100, 50, 0)), "")))));
			context.waitTicks(5);
			ScreenInput.quiet(context);
			Screenshots.take(context, "cassonade-waystone-dimensions");
			context.setScreen(() -> null);

			// A list longer than the panel shows its scrollbar, here scrolled down three rows
			context.runOnClient(client -> WaystoneScreen.open(client, new WaystoneListPayload(BlockPos.ZERO, "", true,
					IntStream.range(0, 12).mapToObj(i -> new WaystoneListPayload.Destination(
							GlobalPos.of(Level.OVERWORLD, new BlockPos(i * 100, 64, 0)), "Waystone " + (char) ('A' + i))).toList())));
			context.waitTicks(5);
			context.runOnClient(client -> client.gui.screen().mouseScrolled(0, 0, 0, -3));
			context.runOnClient(client -> client.gui.screen().mouseScrolled(0, 0, 0, -1));
			context.runOnClient(client -> client.gui.screen().mouseScrolled(0, 0, 0, -1));
			context.waitTicks(5);
			ScreenInput.quiet(context);
			Screenshots.take(context, "cassonade-waystone-scroll");
			context.setScreen(() -> null);

			// Gone from the list when broken
			world.getServer().runCommand("execute in minecraft:overworld run setblock 10 101 10 air");
			world.getServer().runOnServer(server -> check(WaystoneData.of(server).entries().size() == 2, "the broken one struck off"));
			world.getServer().runOnServer(server -> server.saveEverything(true, true, true));
			context.waitTicks(5);
		}
	}

	private static void checkDrops(TestSingleplayerContext world, int half, Item tool, int expected, String what) {
		world.getServer().runCommand("setblock 12 101 12 cassonade:waystone");
		world.getServer().runCommand("setblock 12 102 12 cassonade:waystone[half=upper]");
		world.getServer().runOnServer(server -> {
			ServerPlayer player = player(server);
			player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(tool));
			player.gameMode.destroyBlock(new BlockPos(12, 101 + half, 12));
			AABB around = new AABB(10, 99, 10, 15, 105, 15);
			int drops = server.overworld().getEntitiesOfClass(ItemEntity.class, around).stream().filter(item -> item.getItem().is(Waystones.ITEM))
					.mapToInt(item -> item.getItem().getCount()).sum();
			check(drops == expected, what + ": " + drops + " dropped, expected " + expected);
			check(server.overworld().getBlockState(new BlockPos(12, 101, 12)).isAir() && server.overworld().getBlockState(new BlockPos(12, 102, 12)).isAir(), what + ": both halves gone");
			player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
		});
		world.getServer().runCommand("kill @e[type=item]");
	}

	private static Button row(Minecraft client, int row) {
		return client.gui.screen().children().stream().filter(Button.class::isInstance).map(Button.class::cast).skip(row).findFirst().orElseThrow();
	}

	/** Where a row's arrow is, in GUI pixels: in the column on its left, up in the upper half, down in the lower. */
	private static double[] arrow(Minecraft client, int row, boolean up) {
		Button button = row(client, row);
		return new double[] {button.getX() - 9, up ? button.getY() + 4 : button.getBottom() - 4};
	}

	private static void hoverArrow(ClientGameTestContext context, int row, boolean up) {
		double[] at = context.computeOnClient(client -> {
			double[] gui = arrow(client, row, up);
			return new double[] {gui[0] * client.getWindow().getGuiScale(), gui[1] * client.getWindow().getGuiScale()};
		});
		context.getInput().setCursorPos(at[0], at[1]);
	}

	private static void clickArrow(ClientGameTestContext context, int row, boolean up, boolean shift) {
		context.runOnClient(client -> {
			double[] at = arrow(client, row, up);
			MouseButtonInfo left = new MouseButtonInfo(InputConstants.MOUSE_BUTTON_LEFT, shift ? InputConstants.MOD_SHIFT : 0);
			client.gui.screen().mouseClicked(new MouseButtonEvent(at[0], at[1], left), false);
			client.gui.screen().mouseReleased(new MouseButtonEvent(at[0], at[1], left));
		});
		context.waitTicks(5);
	}

	/** The two waystones listed at home, the named one first or second, and the screen still open: an arrow is no trip. */
	private static void checkRows(ClientGameTestContext context, String what, boolean mineFirst) {
		context.runOnClient(client -> {
			check(client.gui.screen() instanceof WaystoneScreen, what + ": the screen still open");
			check(row(client, 0).getMessage().getString().equals("Mine") == mineFirst && row(client, 1).getMessage().getString().equals("Mine") != mineFirst,
					what + ": " + row(client, 0).getMessage().getString() + ", " + row(client, 1).getMessage().getString());
		});
	}

	private static void checkOnMap(ClientGameTestContext context, int expected, String what) {
		context.runOnClient(client -> client.gui.setScreen(new WorldMapScreen()));
		context.waitTicks(5);
		context.runOnClient(client -> check(((WorldMapScreen) client.gui.screen()).waystonesHere().size() == expected, what));
		context.setScreen(() -> null);
	}

	private static void aim(ClientGameTestContext context, TestSingleplayerContext world) {
		world.getServer().runCommand("execute in minecraft:overworld run tp @a 2.5 101 2.5 -90 15");
		context.waitTicks(10);
	}

	private static void use(ClientGameTestContext context) {
		context.getInput().pressKey(options -> options.keyUse);
		context.waitTicks(5);
	}

	private static void press(ClientGameTestContext context, int row) {
		context.runOnClient(client -> client.gui.screen().children().stream().filter(Button.class::isInstance).map(Button.class::cast)
				.skip(row).findFirst().orElseThrow().onPress(null));
	}
}
