package com.cassonade.test;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import com.mojang.blaze3d.platform.InputConstants;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.client.sounds.SoundEventListener;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MapItem;

import com.cassonade.client.map.MapTooltip;
import com.cassonade.client.mixin.AbstractContainerScreenAccessor;
import com.cassonade.client.quest.QuestLog;
import com.cassonade.client.quest.QuestOptInScreen;
import com.cassonade.storage.LockedSlots;
import com.cassonade.storage.MapPreview;

/**
 * Containers and the inventory: a chest sorted with a middle click and filled with its deposit button, stacks
 * moved by a shift click dragged, with a sound, a bundle emptied and filled by dragging, a locked slot left
 * alone; the contents of a Shulker box, a bundle, food and a filled map in their tooltips.
 */
public class StorageClientGameTest implements FabricClientGameTest {
	@Override
	public void runTest(ClientGameTestContext context) {
		if (TestSelection.skips(this)) {
			return;
		}
		context.getInput().resizeWindow(1920, 1080);
		try (TestSingleplayerContext world = context.worldBuilder().create()) {
			world.getConnection().waitForChunksRender();
			context.waitForScreen(QuestOptInScreen.class);
			context.runOnClient(client -> QuestLog.setEnabled(false));
			context.setScreen(() -> null);
			world.getServer().runCommand("gamemode survival @a");
			context.setScreen(() -> null);
			world.getServer().runCommand("execute as @a at @s run setblock ~2 ~ ~ chest{Items:[{Slot:3b,id:\"minecraft:dirt\",count:5},"
					+ "{Slot:10b,id:\"minecraft:stone\",count:7},{Slot:20b,id:\"minecraft:dirt\",count:9}]}");
			world.getServer().runCommand("item replace entity @a inventory.5 with dirt 20");
			context.waitTicks(2);
			world.getServer().runOnServer(server -> {
				ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
				player.openMenu((MenuProvider) player.level().getBlockEntity(player.blockPosition().east(2)));
			});
			context.waitForScreen(ContainerScreen.class);
			context.waitTicks(2);
			Screenshots.take(context, "cassonade-13-chest-storage-buttons");
			ScreenInput.middleClick(context, client -> (client.getWindow().getGuiScaledWidth() - 176) / 2 + 8 + 4 * 18 + 8,
					client -> chestTop(client) + 18 + 18 + 8);
			world.getConnection().waitForServerboundPackets();
			world.getServer().runOnServer(server -> {
				Container chest = chestNextTo(server);
				check(chest.getItem(0).is(Items.STONE) && chest.getItem(0).getCount() == 7 && chest.getItem(1).is(Items.DIRT)
						&& chest.getItem(1).getCount() == 14 && chest.getItem(2).isEmpty(), "middle click sorts the chest, stacks merged");
			});
			ScreenInput.click(context, client -> (client.getWindow().getGuiScaledWidth() - 176) / 2 + 176 - 7 - 2 * 11 - 2 + 5,
					client -> chestTop(client) + 4 + 5);
			world.getConnection().waitForServerboundPackets();
			world.getServer().runOnServer(server -> {
				check(chestNextTo(server).countItem(Items.DIRT) == 34, "the deposit button puts the inventory's dirt in the chest");
				check(server.getPlayerList().getPlayers().getFirst().getInventory().countItem(Items.DIRT) == 0, "no dirt left in the inventory");
			});
			// Moving stacks in a window makes a sound, which vanilla does not
			List<String> sounds = new CopyOnWriteArrayList<>();
			SoundEventListener listener = (sound, event, range) -> sounds.add(sound.getIdentifier().getPath());
			context.runOnClient(client -> client.getSoundManager().addListener(listener));
			// A shift click on a stack, then the next one hovered with shift and the button still held: both move to the chest
			world.getServer().runCommand("item replace entity @a inventory.6 with cobblestone 10");
			world.getServer().runCommand("item replace entity @a inventory.7 with sand 10");
			context.waitTicks(2);
			ScreenInput.shiftSweep(context, 27 + 6, 27 + 7, true);
			world.getServer().runOnServer(server -> {
				Container chest = chestNextTo(server);
				check(chest.countItem(Items.COBBLESTONE) == 10 && chest.countItem(Items.SAND) == 10,
						"shift click then hovering with shift held moves both stacks (cobblestone " + chest.countItem(Items.COBBLESTONE)
								+ ", sand " + chest.countItem(Items.SAND) + ")");
			});
			context.runOnClient(client -> client.getSoundManager().removeListener(listener));
			check(sounds.contains("item.bundle.insert"), "moving stacks makes a sound (" + sounds + ")");
			// The button released at once, the sweep stops with it
			world.getServer().runCommand("item replace entity @a inventory.6 with gravel 10");
			world.getServer().runCommand("item replace entity @a inventory.7 with clay 10");
			context.waitTicks(2);
			ScreenInput.shiftSweep(context, 27 + 6, 27 + 7, false);
			world.getServer().runOnServer(server -> {
				Container chest = chestNextTo(server);
				check(chest.countItem(Items.GRAVEL) == 10 && chest.countItem(Items.CLAY) == 0,
						"releasing the button ends the sweep (gravel " + chest.countItem(Items.GRAVEL) + ", clay " + chest.countItem(Items.CLAY) + ")");
			});
			// A bundle carried, a right click dragged over empty slots takes a stack out in each
			world.getServer().runCommand("item replace entity @a inventory.7 with air");
			world.getServer().runCommand("item replace entity @a inventory.8 with bundle[bundle_contents=[{id:\"minecraft:apple\",count:5},"
					+ "{id:\"minecraft:bread\",count:3}]]");
			context.waitTicks(2);
			ScreenInput.click(context, client -> inventoryRowSlotX(client, 8), client -> inventoryRowSlotY(client));
			context.runOnClient(client -> {
				MouseButtonInfo right = new MouseButtonInfo(InputConstants.MOUSE_BUTTON_RIGHT, 0);
				MouseButtonEvent first = new MouseButtonEvent(inventoryRowSlotX(client, 6), inventoryRowSlotY(client), right);
				MouseButtonEvent second = new MouseButtonEvent(inventoryRowSlotX(client, 7), inventoryRowSlotY(client), right);
				client.gui.screen().mouseClicked(first, false);
				client.gui.screen().mouseDragged(second, 18, 0);
				client.gui.screen().mouseReleased(second);
			});
			world.getConnection().waitForServerboundPackets();
			world.getServer().runOnServer(server -> {
				ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
				ItemStack bundle = player.containerMenu.getCarried();
				check(player.getInventory().getItem(15).is(Items.APPLE) && player.getInventory().getItem(16).is(Items.BREAD)
						&& bundle.is(Items.BUNDLE) && bundle.get(DataComponents.BUNDLE_CONTENTS).isEmpty(),
						"a right click dragged empties the bundle in the slots (" + player.getInventory().getItem(15) + ", "
								+ player.getInventory().getItem(16) + ")");
			});
			// And a left click dragged over stacks puts them back in it
			context.runOnClient(client -> {
				MouseButtonInfo left = new MouseButtonInfo(InputConstants.MOUSE_BUTTON_LEFT, 0);
				MouseButtonEvent first = new MouseButtonEvent(inventoryRowSlotX(client, 6), inventoryRowSlotY(client), left);
				MouseButtonEvent second = new MouseButtonEvent(inventoryRowSlotX(client, 7), inventoryRowSlotY(client), left);
				client.gui.screen().mouseClicked(first, false);
				client.gui.screen().mouseDragged(second, 18, 0);
				client.gui.screen().mouseReleased(second);
			});
			world.getConnection().waitForServerboundPackets();
			world.getServer().runOnServer(server -> {
				ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
				ItemStack bundle = player.containerMenu.getCarried();
				check(player.getInventory().getItem(15).isEmpty() && player.getInventory().getItem(16).isEmpty()
						&& bundle.get(DataComponents.BUNDLE_CONTENTS).size() == 2, "a left click dragged fills the bundle with the stacks");
			});
			// Put down in the slot left empty
			ScreenInput.click(context, client -> inventoryRowSlotX(client, 8), client -> inventoryRowSlotY(client));

			// Locked slots: a click on an inventory slot, the lock key held, locks it, a padlock on it. Its stack
			// then stays put through a shift click, a deposit and a sort; a click again unlocks it
			world.getServer().runCommand("item replace entity @a inventory.4 with dirt 4");
			world.getServer().runCommand("item replace entity @a inventory.2 with oak_planks 3");
			world.getServer().runCommand("item replace entity @a inventory.20 with oak_planks 5");
			context.waitTicks(2);
			double[] lockCenter = context.computeOnClient(client -> {
				MouseButtonEvent center = ScreenInput.slotEvent(client, 27 + 4, 0);
				return new double[] {center.x() * client.getWindow().getGuiScale(), center.y() * client.getWindow().getGuiScale()};
			});
			context.getInput().setCursorPos(lockCenter[0], lockCenter[1]);
			context.waitTicks(2);
			ScreenInput.lockClick(context, 27 + 4);
			world.getConnection().waitForServerboundPackets();
			world.getConnection().waitForClientboundPackets();
			context.waitTicks(2);
			check(context.computeOnClient(client -> LockedSlots.of(client.player).isLocked(13)), "a click with the lock key held locks the slot");
			Screenshots.take(context, "cassonade-13-locked-slot");
			ScreenInput.shiftSweep(context, 27 + 4, 27 + 4, false);
			ScreenInput.click(context, client -> (client.getWindow().getGuiScaledWidth() - 176) / 2 + 176 - 7 - 2 * 11 - 2 + 5,
					client -> chestTop(client) + 4 + 5);
			ScreenInput.middleClick(context, client -> (int) ScreenInput.slotEvent(client, 27 + 4, 0).x(), client -> (int) ScreenInput.slotEvent(client, 27 + 4, 0).y());
			world.getConnection().waitForServerboundPackets();
			world.getServer().runOnServer(server -> {
				Inventory inventory = server.getPlayerList().getPlayers().getFirst().getInventory();
				check(inventory.getItem(13).is(Items.DIRT) && inventory.getItem(13).getCount() == 4,
						"a locked stack stays through a shift click, a deposit and a sort (" + inventory.getItem(13) + ")");
				boolean merged = false;
				for (int slot = Inventory.SELECTION_SIZE; slot < Inventory.INVENTORY_SIZE; slot++) {
					merged |= inventory.getItem(slot).is(Items.OAK_PLANKS) && inventory.getItem(slot).getCount() == 8;
				}
				check(merged, "the sort goes on around the locked slot, the planks merged");
			});
			ScreenInput.lockClick(context, 27 + 4);
			world.getConnection().waitForServerboundPackets();
			world.getConnection().waitForClientboundPackets();
			context.waitTicks(2);
			check(context.computeOnClient(client -> !LockedSlots.of(client.player).isLocked(13)), "a click with the lock key held again unlocks the slot");
			world.getServer().runCommand("clear @a oak_planks");
			world.getServer().runCommand("clear @a dirt");
			context.waitTicks(2);
			context.setScreen(() -> new InventoryScreen(Minecraft.getInstance().player));
			context.waitForScreen(InventoryScreen.class);

			// A shulker box's contents shown in its tooltip like a bundle's
			world.getServer().runCommand("item replace entity @a inventory.0 with shulker_box[container=[{slot:0,item:{id:\"minecraft:diamond\",count:5}},"
					+ "{slot:1,item:{id:\"minecraft:apple\",count:3}},{slot:4,item:{id:\"minecraft:oak_log\",count:64}}]]");
			context.waitTicks(2);
			double[] slotCenter = context.computeOnClient(client -> {
				double scale = client.getWindow().getGuiScale();
				return new double[] {((client.getWindow().getGuiScaledWidth() - 176) / 2 + 8 + 8) * scale,
						((client.getWindow().getGuiScaledHeight() - 166) / 2 + 84 + 8) * scale};
			});
			context.getInput().setCursorPos(slotCenter[0], slotCenter[1]);
			context.waitTicks(3);
			Screenshots.take(context, "cassonade-14-shulker-preview");
			world.getServer().runCommand("item replace entity @a inventory.1 with bundle[bundle_contents=[{id:\"minecraft:arrow\",count:16},"
					+ "{id:\"minecraft:bread\",count:5},{id:\"minecraft:torch\",count:12},{id:\"minecraft:string\",count:4},{id:\"minecraft:flint\",count:2}]]");
			context.waitTicks(2);
			double[] bundleCenter = context.computeOnClient(client -> {
				double scale = client.getWindow().getGuiScale();
				return new double[] {((client.getWindow().getGuiScaledWidth() - 176) / 2 + 8 + 18 + 8) * scale,
						((client.getWindow().getGuiScaledHeight() - 166) / 2 + 84 + 8) * scale};
			});
			context.getInput().setCursorPos(bundleCenter[0], bundleCenter[1]);
			context.waitTicks(3);
			Screenshots.take(context, "cassonade-14-bundle-preview");
			// Food: the hunger it restores as drumsticks, its saturation in gold below
			world.getServer().runCommand("item replace entity @a inventory.2 with cooked_beef 3");
			context.waitTicks(2);
			double[] foodCenter = context.computeOnClient(client -> {
				double scale = client.getWindow().getGuiScale();
				return new double[] {((client.getWindow().getGuiScaledWidth() - 176) / 2 + 8 + 2 * 18 + 8) * scale,
						((client.getWindow().getGuiScaledHeight() - 166) / 2 + 84 + 8) * scale};
			});
			context.getInput().setCursorPos(foodCenter[0], foodCenter[1]);
			context.waitTicks(3);
			Screenshots.take(context, "cassonade-14-food-preview");
			world.getServer().runCommand("item replace entity @a inventory.2 with air");

			// A filled map in the inventory shows itself in its tooltip once the server sent its contents; held a moment to draw it
			world.getServer().runOnServer(server -> {
				ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
				player.getInventory().setSelectedSlot(0);
				player.getInventory().setItem(0, MapItem.create(player.level(), player.getBlockX(), player.getBlockZ(), (byte) 0, true, false));
			});
			context.waitTicks(40);
			check(context.computeOnClient(client -> {
				ItemStack map = client.player.getInventory().getItem(0);
				return map.getTooltipImage().orElse(null) instanceof MapPreview preview && new MapTooltip(preview).getHeight(client.font) > 0;
			}), "a filled map's tooltip shows the map");
			context.setScreen(() -> new InventoryScreen(Minecraft.getInstance().player));
			context.waitForScreen(InventoryScreen.class);
			double[] mapCenter = context.computeOnClient(client -> {
				AbstractContainerScreen<?> screen = (AbstractContainerScreen<?>) client.gui.screen();
				Slot slot = screen.getMenu().getSlot(InventoryMenu.USE_ROW_SLOT_START);
				AbstractContainerScreenAccessor accessor = (AbstractContainerScreenAccessor) screen;
				double scale = client.getWindow().getGuiScale();
				return new double[] {(accessor.cassonade$getLeftPos() + slot.x + 8) * scale, (accessor.cassonade$getTopPos() + slot.y + 8) * scale};
			});
			context.getInput().setCursorPos(mapCenter[0], mapCenter[1]);
			context.waitTicks(3);
			Screenshots.take(context, "cassonade-14-map-preview");
			context.setScreen(() -> null);
			world.getServer().runCommand("clear @a filled_map");
		}
	}

	/** The center of a slot of the chest screen's first inventory row, in GUI pixels. */
	private static int inventoryRowSlotX(Minecraft client, int column) {
		return (client.getWindow().getGuiScaledWidth() - 176) / 2 + 8 + column * 18 + 8;
	}

	private static int inventoryRowSlotY(Minecraft client) {
		return chestTop(client) + ((AbstractContainerScreen<?>) client.gui.screen()).getMenu().getSlot(27).y + 8;
	}

	/** The top of the chest screen open, in GUI pixels: the room around its inventory makes it taller than vanilla's. */
	private static int chestTop(Minecraft client) {
		return ((AbstractContainerScreenAccessor) client.gui.screen()).cassonade$getTopPos();
	}

	/** The chest placed two blocks east of the player. */
	private static Container chestNextTo(MinecraftServer server) {
		ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
		return (Container) player.level().getBlockEntity(player.blockPosition().east(2));
	}

	private static void check(boolean condition, String what) {
		if (!condition) {
			throw new AssertionError("Expected: " + what);
		}
	}
}
