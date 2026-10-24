package com.vanillasugar.test;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestDedicatedServerConnection;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestDedicatedServerContext;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.zombie.Husk;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.item.crafting.display.RecipeDisplayId;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import com.vanillasugar.client.BackpackPanel;
import com.vanillasugar.client.ClockHud;
import com.vanillasugar.client.HopperFilterScreen;
import com.vanillasugar.client.combat.CombatHud;
import com.vanillasugar.client.map.Minimap;
import com.vanillasugar.client.quest.QuestLog;
import com.vanillasugar.client.quest.QuestOptInScreen;
import com.vanillasugar.clock.WeatherForecast;
import com.vanillasugar.compass.PlayerMarkers;
import com.vanillasugar.crafting.CraftAllPayload;
import com.vanillasugar.gear.PlayerGear;
import com.vanillasugar.hopper.HopperFilterMenu;
import com.vanillasugar.item.VanillaSugarItems;
import com.vanillasugar.storage.StoragePayload;

/**
 * The mod on both sides, with a dedicated server rather than the integrated one: everything that goes
 * through the network, and the gear kept by the server when the player comes back.
 */
public class DedicatedServerClientGameTest implements FabricClientGameTest {
	@Override
	public void runTest(ClientGameTestContext context) {
		context.getInput().resizeWindow(1920, 1080);
		try (TestDedicatedServerContext server = context.worldBuilder().createServer()) {
			try (TestDedicatedServerConnection connection = server.connect()) {
				connection.waitForChunksRender();
				declineJournal(context);
				server.runCommand("gamemode survival @a");
				server.runCommand("gamerule advance_weather true");
				server.runCommand("give @a vanillasugar:backpack");
				server.runCommand("give @a compass");
				server.runCommand("give @a clock");
				server.runCommand("give @a diamond 64");
				connection.waitForClientboundPackets();
				context.waitTicks(5);

				// Shift clicks equip the gear: the server takes it and sends it back
				context.setScreen(() -> new InventoryScreen(Minecraft.getInstance().player));
				context.waitForScreen(InventoryScreen.class);
				for (int hotbar = 0; hotbar < 3; hotbar++) {
					quickMove(context, InventoryMenu.USE_ROW_SLOT_START + hotbar);
				}
				connection.waitForServerboundPackets();
				connection.waitForClientboundPackets();
				context.waitTicks(2);
				server.runOnServer(s -> {
					PlayerGear gear = PlayerGear.of(player(s));
					check(gear.backpack().is(VanillaSugarItems.BACKPACK) && gear.compass().is(Items.COMPASS) && gear.clock().is(Items.CLOCK),
							"the dedicated server equips the backpack, compass and clock");
				});
				check(context.computeOnClient(client -> PlayerGear.of(client.player).compass().is(Items.COMPASS) && Minimap.isShown(client)),
						"the client gets the gear back and shows the minimap");

				// The backpack opened from the client: the server sends the diamonds into it
				context.runOnClient(client -> BackpackPanel.setOpen(true));
				connection.waitForServerboundPackets();
				context.waitTicks(2);
				quickMove(context, InventoryMenu.USE_ROW_SLOT_START + 3);
				connection.waitForServerboundPackets();
				connection.waitForClientboundPackets();
				server.runOnServer(s -> check(PlayerGear.of(player(s)).backpack().getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY)
						.nonEmptyItemCopyStream().anyMatch(stack -> stack.is(Items.DIAMOND) && stack.getCount() == 64), "diamonds in the backpack"));
				context.setScreen(() -> null);

				// What only the server knows, sent to the player: the weather forecast for the clock, the
				// markers, the mobs hunting them
				context.waitTicks(30);
				connection.waitForClientboundPackets();
				check(context.computeOnClient(client -> ClockHud.isShown() && !WeatherForecast.UNKNOWN.equals(get(ClockHud.class, "forecast"))),
						"the clock gets the weather forecast from the server");
				server.runOnServer(s -> PlayerMarkers.place(player(s), GlobalPos.of(Level.OVERWORLD, player(s).blockPosition().east(40)),
						"Camp", DyeColor.RED));
				connection.waitForClientboundPackets();
				context.waitTicks(2);
				check(context.computeOnClient(client -> PlayerMarkers.of(client.player).markers().stream()
						.anyMatch(marker -> marker.name().equals("Camp"))), "the client gets the marker placed on the server");
				server.runCommand("execute as @a at @s run summon husk ~3 ~ ~6 {NoAI:1b}");
				context.waitTicks(5);
				int husk = server.computeOnServer(s -> {
					ServerPlayer player = player(s);
					Husk found = player.level().getEntitiesOfClass(Husk.class, player.getBoundingBox().inflate(16)).getFirst();
					found.setTarget(player);
					return found.getId();
				});
				context.waitTicks(30);
				connection.waitForClientboundPackets();
				check(context.computeOnClient(client -> CombatHud.hasSpotted(husk)), "the client learns which mob hunts the player");
				server.runCommand("kill @e[type=husk]");

				// A chest sorted by the server on the client's request
				server.runCommand("execute as @a at @s run setblock ~2 ~ ~ chest{Items:[{Slot:3b,id:\"minecraft:dirt\",count:5},"
						+ "{Slot:10b,id:\"minecraft:stone\",count:7},{Slot:20b,id:\"minecraft:dirt\",count:9}]}");
				context.waitTicks(2);
				server.runOnServer(s -> player(s).openMenu((MenuProvider) player(s).level().getBlockEntity(player(s).blockPosition().east(2))));
				context.waitForScreen(ContainerScreen.class);
				context.runOnClient(client -> ClientPlayNetworking.send(new StoragePayload(StoragePayload.Action.SORT_CONTAINER)));
				connection.waitForServerboundPackets();
				server.runOnServer(s -> {
					Container chest = (Container) player(s).level().getBlockEntity(player(s).blockPosition().east(2));
					check(chest.getItem(0).is(Items.STONE) && chest.getItem(1).is(Items.DIRT) && chest.getItem(1).getCount() == 14,
							"the dedicated server sorts the chest");
				});
				context.runOnClient(client -> client.player.closeContainer());
				server.runCommand("execute as @a at @s run setblock ~2 ~ ~ air");

				// Crafting as many planks as the logs allow
				server.runCommand("recipe give @a minecraft:oak_planks");
				server.runCommand("give @a oak_log 70");
				connection.waitForClientboundPackets();
				RecipeDisplayId planks = server.computeOnServer(s -> {
					List<RecipeDisplayId> ids = new ArrayList<>();
					s.getRecipeManager().listDisplaysForRecipe(ResourceKey.create(Registries.RECIPE, Identifier.withDefaultNamespace("oak_planks")),
							entry -> ids.add(entry.id()));
					return ids.getFirst();
				});
				context.runOnClient(client -> ClientPlayNetworking.send(new CraftAllPayload(client.player.inventoryMenu.containerId, planks)));
				connection.waitForServerboundPackets();
				server.runOnServer(s -> check(player(s).getInventory().countItem(Items.OAK_LOG) == 0
						&& player(s).getInventory().countItem(Items.OAK_PLANKS) > 70, "the dedicated server crafts all the planks"));

				// A right click on ripe wheat, from the client's own key
				server.runCommand("execute as @a at @s run setblock ~ ~-1 ~2 farmland");
				server.runCommand("execute as @a at @s run setblock ~ ~ ~2 wheat[age=7]");
				connection.waitForClientboundPackets();
				context.waitTicks(5);
				// On the block itself: the generated world's grass could be in the way of the crosshair
				context.runOnClient(client -> {
					BlockPos pos = client.player.blockPosition().south(2);
					client.gameMode.useItemOn(client.player, InteractionHand.MAIN_HAND,
							new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false));
				});
				connection.waitForServerboundPackets();
				context.waitTicks(2);
				server.runOnServer(s -> {
					ServerPlayer player = player(s);
					BlockState wheat = player.level().getBlockState(player.blockPosition().south(2));
					check(wheat.is(Blocks.WHEAT) && wheat.getValue(CropBlock.AGE) == 0, "the dedicated server replants the wheat");
					player.level().getEntitiesOfClass(ItemEntity.class, player.getBoundingBox().inflate(4)).forEach(ItemEntity::discard);
				});

				// The hopper's filter screen, opened by the server
				server.runCommand("execute as @a at @s run setblock ~-3 ~ ~ hopper");
				context.waitTicks(2);
				server.runOnServer(s -> {
					ServerPlayer player = player(s);
					player.openMenu((MenuProvider) player.level().getBlockEntity(player.blockPosition().west(3)));
					player.containerMenu.getSlot(HopperFilterMenu.FILTER_SLOT).set(new ItemStack(Items.STONE));
				});
				context.waitForScreen(HopperFilterScreen.class);
				context.runOnClient(client -> client.player.closeContainer());
				context.waitTicks(2);
			}

			// Back on the server: it kept the gear, the backpack's contents and the markers
			try (TestDedicatedServerConnection connection = server.connect()) {
				connection.waitForChunksRender();
				declineJournal(context);
				connection.waitForClientboundPackets();
				context.waitTicks(5);
				check(context.computeOnClient(client -> {
					PlayerGear gear = PlayerGear.of(client.player);
					return gear.compass().is(Items.COMPASS) && gear.backpack().getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY)
							.nonEmptyItemCopyStream().anyMatch(stack -> stack.is(Items.DIAMOND));
				}), "the gear and the backpack's contents are back after reconnecting");
				check(context.computeOnClient(client -> !PlayerMarkers.of(client.player).markers().isEmpty()), "the markers are back after reconnecting");
				context.takeScreenshot("vanillasugar-dedicated-server-back");
			}
		}
	}

	/** The journal is offered once per world, as soon as the player is in it. */
	private static void declineJournal(ClientGameTestContext context) {
		context.waitTicks(10);
		if (context.computeOnClient(client -> client.gui.screen() instanceof QuestOptInScreen)) {
			context.runOnClient(client -> QuestLog.setEnabled(false));
			context.setScreen(() -> null);
		}
	}

	private static ServerPlayer player(MinecraftServer server) {
		return server.getPlayerList().getPlayers().getFirst();
	}

	private static void quickMove(ClientGameTestContext context, int slot) {
		context.runOnClient(client -> client.gameMode.handleContainerInput(
				client.player.inventoryMenu.containerId, slot, 0, ContainerInput.QUICK_MOVE, client.player));
	}

	@SuppressWarnings("unchecked")
	private static <T> T get(Class<?> owner, String name) {
		try {
			Field field = owner.getDeclaredField(name);
			field.setAccessible(true);
			return (T) field.get(null);
		} catch (ReflectiveOperationException e) {
			throw new AssertionError(e);
		}
	}

	private static void check(boolean condition, String what) {
		if (!condition) {
			throw new AssertionError("Expected: " + what);
		}
	}
}
