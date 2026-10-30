package com.cassonade.test;

import java.lang.reflect.Method;
import java.util.List;
import java.util.function.ToIntFunction;

import com.mojang.blaze3d.platform.InputConstants;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.InputQuirks;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;

import com.cassonade.client.mixin.AbstractContainerScreenAccessor;
import com.cassonade.client.storage.ContainerSearch;
import com.cassonade.client.quest.QuestLog;
import com.cassonade.client.quest.QuestOptInScreen;
import com.cassonade.client.BackpackPanel;
import com.cassonade.gear.GearLayout;
import com.cassonade.gear.GearSlot;
import com.cassonade.gear.PlayerGear;
import com.cassonade.item.CassonadeItems;

/**
 * Searching storage: the backpack's search field, opened by its button, matching as one types, closed by a
 * click outside or Escape; a chest's, opened by Ctrl+F or its button; the creative inventory's search tab.
 */
public class SearchClientGameTest implements FabricClientGameTest {
	@Override
	public void runTest(ClientGameTestContext context) {
		if (TestSelection.skips(this)) {
			return;
		}
		try (TestSingleplayerContext world = context.worldBuilder().create()) {
			world.getConnection().waitForChunksRender();
			context.waitForScreen(QuestOptInScreen.class);
			context.runOnClient(client -> QuestLog.setEnabled(false));
			context.setScreen(() -> null);
			world.getServer().runCommand("gamemode survival @a");

			// A brick and clay put into the backpack by a shift click dragged right after its tab opens it
			context.setScreen(() -> null);
			world.getServer().runCommand("clear @a");
			world.getServer().runCommand("item replace entity @a inventory.0 with brick 5");
			world.getServer().runCommand("item replace entity @a inventory.1 with clay_ball 5");
			world.getServer().runOnServer(server -> {
				ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
				player.setAttached(PlayerGear.ATTACHMENT, PlayerGear.of(player).with(GearSlot.BACKPACK, new ItemStack(CassonadeItems.BACKPACK)));
			});
			context.waitTicks(2);
			context.runOnClient(client -> BackpackPanel.setOpen(false));
			context.setScreen(() -> new InventoryScreen(Minecraft.getInstance().player));
			context.waitForScreen(InventoryScreen.class);
			click(context, client -> leftPos(client) + GearLayout.tabX(1) + GearLayout.TAB_WIDTH / 2,
					client -> topPos(client) + GearLayout.TAB_Y + 8);
			check(context.computeOnClient(client -> BackpackPanel.isOpen()), "the backpack's tab opens it");
			shiftSweep(context, InventoryMenu.INV_SLOT_START, InventoryMenu.INV_SLOT_START + 1);
			world.getConnection().waitForServerboundPackets();
			context.waitTicks(2);
			String packed = context.computeOnClient(client -> client.player.getInventory().countItem(Items.BRICK) + " / "
					+ client.player.getInventory().countItem(Items.CLAY_BALL));
			check(packed.equals("0 / 0"), "the first shift click dragged after the backpack's tab moves both stacks (" + packed + ")");
			check(context.computeOnClient(client -> client.gui.screen().children().stream()
					.anyMatch(widget -> widget instanceof ImageButton button && button.getX() == backpackSearchButtonX(client))),
					"the open backpack has a search button");
			check(context.computeOnClient(client -> client.gui.screen().children().stream()
					.filter(EditBox.class::isInstance).map(EditBox.class::cast).anyMatch(search ->
							search.getX() == backpackSearchFieldLeft(client) && search.getY() == backpackSearchFieldTop(client))),
					"the backpack search field is centered above the panel and clear of its strap");
			click(context, client -> backpackSearchButtonX(client) + 5, client -> topPos(client) + 9);
			check(context.computeOnClient(client -> client.gui.screen().getFocused() instanceof EditBox),
					"the backpack search button opens and focuses the search field");
			click(context, SearchClientGameTest::backpackSearchFieldCenterX, SearchClientGameTest::backpackSearchFieldCenterY);
			check(context.computeOnClient(client -> client.gui.screen() instanceof InventoryScreen && BackpackPanel.isOpen()
					&& client.gui.screen().getFocused() instanceof EditBox),
					"clicking the backpack search field does not activate a widget behind it");
			context.runOnClient(client -> "brick".codePoints()
					.forEach(codepoint -> client.gui.screen().charTyped(new CharacterEvent(codepoint))));
			check(context.computeOnClient(client -> {
				AbstractContainerScreen<?> screen = (AbstractContainerScreen<?>) client.gui.screen();
				boolean foundBrick = false;
				boolean foundClayBall = false;
				boolean brickDimmed = true;
				boolean clayBallDimmed = false;
				for (int i = GearLayout.BACKPACK_START; i < GearLayout.BACKPACK_END; i++) {
					Slot slot = screen.getMenu().getSlot(i);
					if (slot.getItem().is(Items.BRICK)) {
						foundBrick = true;
						brickDimmed = ContainerSearch.dims(screen, slot);
					} else if (slot.getItem().is(Items.CLAY_BALL)) {
						foundClayBall = true;
						clayBallDimmed = ContainerSearch.dims(screen, slot);
					}
				}
				return foundBrick && foundClayBall && !brickDimmed && clayBallDimmed;
			}), "search matches backpack contents and dims the non-matching stack");
			Screenshots.take(context, "cassonade-backpack-search");
			click(context, SearchClientGameTest::emptyInventorySlotCenterX, SearchClientGameTest::emptyInventorySlotCenterY);
			check(context.computeOnClient(client -> {
				AbstractContainerScreen<?> screen = (AbstractContainerScreen<?>) client.gui.screen();
				for (int i = GearLayout.BACKPACK_START; i < GearLayout.BACKPACK_END; i++) {
					if (ContainerSearch.dims(screen, screen.getMenu().getSlot(i))) {
						return false;
					}
				}
				return !(screen.getFocused() instanceof EditBox);
			}), "clicking outside the search field closes it and restores every backpack item");
			click(context, client -> backpackSearchButtonX(client) + 5, client -> topPos(client) + 9);
			context.runOnClient(client -> client.gui.screen().keyPressed(new KeyEvent(InputConstants.KEY_ESCAPE, 0, 0)));
			check(context.computeOnClient(client -> {
				AbstractContainerScreen<?> screen = (AbstractContainerScreen<?>) client.gui.screen();
				return screen.getFocused() == null && !ContainerSearch.dims(screen, screen.getMenu().getSlot(GearLayout.BACKPACK_START));
			}), "Escape closes and clears backpack search");

			// Search opens from Ctrl+F or its title button, and dims only storage slots that do not match
			world.getServer().runCommand("execute at @a run setblock ~2 ~ ~ chest{Items:[{Slot:0b,id:\"minecraft:diamond\",count:1},"
					+ "{Slot:1b,id:\"minecraft:stone\",count:1}]}");
			world.getServer().runOnServer(server -> {
				ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
				player.openMenu((MenuProvider) player.level().getBlockEntity(player.blockPosition().east(2)));
			});
			context.waitForScreen(ContainerScreen.class);
			check(context.computeOnClient(client -> client.gui.screen().children().stream()
					.anyMatch(widget -> widget instanceof ImageButton button && button.getX() == searchButtonX(client))),
					"the chest has a search button");
			context.runOnClient(client -> client.gui.screen().keyPressed(
					new KeyEvent(InputConstants.KEY_F, InputConstants.KEY_F, InputQuirks.EDIT_SHORTCUT_KEY_MODIFIER)));
			check(context.computeOnClient(client -> client.gui.screen().getFocused() instanceof EditBox), "Ctrl+F opens and focuses the search field");
			check(context.computeOnClient(client -> client.gui.screen().keyPressed(
					new KeyEvent(InputConstants.KEY_E, InputConstants.KEY_E, 0))
					&& client.gui.screen() instanceof ContainerScreen
					&& client.gui.screen().getFocused() instanceof EditBox),
					"the inventory key does not close the screen while the search field has focus");
			context.runOnClient(client -> "diamond".codePoints().forEach(codepoint -> client.gui.screen().charTyped(new CharacterEvent(codepoint))));
			check(context.computeOnClient(client -> {
				AbstractContainerScreen<?> screen = (AbstractContainerScreen<?>) client.gui.screen();
				return !ContainerSearch.dims(screen, screen.getMenu().getSlot(0)) && ContainerSearch.dims(screen, screen.getMenu().getSlot(1));
			}), "search keeps matching items bright and dims other stacks");
			Screenshots.take(context, "cassonade-container-search");
			context.runOnClient(client -> client.gui.screen().keyPressed(new KeyEvent(InputConstants.KEY_ESCAPE, 0, 0)));
			check(context.computeOnClient(client -> {
				AbstractContainerScreen<?> screen = (AbstractContainerScreen<?>) client.gui.screen();
				return !ContainerSearch.dims(screen, screen.getMenu().getSlot(0)) && !ContainerSearch.dims(screen, screen.getMenu().getSlot(1));
			}), "closing search clears the query and restores every storage item");
			click(context,
					client -> client.gui.screen().children().stream()
							.filter(widget -> widget instanceof ImageButton button && button.getX() == searchButtonX(client))
							.map(widget -> ((ImageButton) widget).getX() + 5).findFirst().orElseThrow(),
					client -> client.gui.screen().children().stream()
							.filter(widget -> widget instanceof ImageButton button && button.getX() == searchButtonX(client))
							.map(widget -> ((ImageButton) widget).getY() + 5).findFirst().orElseThrow());
			check(context.computeOnClient(client -> client.gui.screen().getFocused() instanceof EditBox), "the search button opens the field too");
			context.setScreen(() -> null);

			// Creative search indexes item tooltips asynchronously, then filters its live tab on each typed character
			world.getServer().runCommand("gamemode creative @a");
			context.waitTicks(5);
			context.runOnClient(client -> client.gui.setScreen(new CreativeModeInventoryScreen(client.player,
					client.player.connection.enabledFeatures(), false)));
			context.waitForScreen(CreativeModeInventoryScreen.class);
			List<ItemStack> indexed = context.computeOnClient(client -> client.getConnection().searchTrees().creativeNameSearch().search("diamond"));
			check(indexed.stream().anyMatch(stack -> stack.is(Items.DIAMOND)), "the creative item search index returns diamonds");
			context.runOnClient(client -> {
				try {
					Method selectTab = CreativeModeInventoryScreen.class.getDeclaredMethod("selectTab", CreativeModeTab.class);
					selectTab.setAccessible(true);
					selectTab.invoke(client.gui.screen(), BuiltInRegistries.CREATIVE_MODE_TAB.getValueOrThrow(CreativeModeTabs.SEARCH));
				} catch (ReflectiveOperationException e) {
					throw new AssertionError(e);
				}
			});
			context.runOnClient(client -> "diamond".codePoints()
					.forEach(codepoint -> client.gui.screen().charTyped(new CharacterEvent(codepoint))));
			check(context.computeOnClient(client -> ((CreativeModeInventoryScreen) client.gui.screen()).getMenu().items.stream()
					.anyMatch(stack -> stack.is(Items.DIAMOND))), "typing in the creative search tab filters its items");
			context.setScreen(() -> null);
			world.getServer().runCommand("gamemode survival @a");
		}
	}

	private static int leftPos(Minecraft client) {
		return ((AbstractContainerScreenAccessor) client.gui.screen()).cassonade$getLeftPos();
	}

	private static int topPos(Minecraft client) {
		return ((AbstractContainerScreenAccessor) client.gui.screen()).cassonade$getTopPos();
	}

	private static int searchButtonX(Minecraft client) {
		AbstractContainerScreenAccessor accessor = (AbstractContainerScreenAccessor) client.gui.screen();
		return accessor.cassonade$getLeftPos() + accessor.cassonade$getImageWidth() - 7 - 11 - 2 * (11 + 2);
	}

	private static int backpackSearchButtonX(Minecraft client) {
		return leftPos(client) + GearLayout.PANEL_X + GearLayout.PANEL_WIDTH - 7 - 11 - 2 * (11 + 2);
	}

	private static int backpackSearchFieldCenterX(Minecraft client) {
		return backpackSearchFieldLeft(client) + 112 / 2;
	}

	private static int backpackSearchFieldCenterY(Minecraft client) {
		return backpackSearchFieldTop(client) + 22 / 2;
	}

	private static int backpackSearchFieldLeft(Minecraft client) {
		return leftPos(client) + GearLayout.PANEL_X + (GearLayout.PANEL_WIDTH - 112) / 2;
	}

	private static int backpackSearchFieldTop(Minecraft client) {
		return topPos(client) - 8 - 22 - 3;
	}

	private static int emptyInventorySlotCenterX(Minecraft client) {
		Slot slot = ((AbstractContainerScreen<?>) client.gui.screen()).getMenu().getSlot(InventoryMenu.INV_SLOT_START);
		return leftPos(client) + slot.x + 8;
	}

	private static int emptyInventorySlotCenterY(Minecraft client) {
		Slot slot = ((AbstractContainerScreen<?>) client.gui.screen()).getMenu().getSlot(InventoryMenu.INV_SLOT_START);
		return topPos(client) + slot.y + 8;
	}

	private static void click(ClientGameTestContext context, ToIntFunction<Minecraft> x, ToIntFunction<Minecraft> y) {
		context.runOnClient(client -> {
			MouseButtonInfo left = new MouseButtonInfo(InputConstants.MOUSE_BUTTON_LEFT, 0);
			client.gui.screen().mouseClicked(new MouseButtonEvent(x.applyAsInt(client), y.applyAsInt(client), left), false);
			client.gui.screen().mouseReleased(new MouseButtonEvent(x.applyAsInt(client), y.applyAsInt(client), left));
		});
		context.waitTicks(2);
	}

	private static void shiftSweep(ClientGameTestContext context, int from, int to) {
		context.getInput().holdShift();
		context.runOnClient(client -> {
			MouseButtonEvent click = slotEvent(client, from);
			MouseButtonEvent drag = slotEvent(client, to);
			client.gui.screen().mouseClicked(click, false);
			client.gui.screen().mouseDragged(drag, drag.x() - click.x(), drag.y() - click.y());
			client.gui.screen().mouseReleased(drag);
		});
		context.getInput().releaseShift();
	}

	private static MouseButtonEvent slotEvent(Minecraft client, int slotIndex) {
		AbstractContainerScreen<?> screen = (AbstractContainerScreen<?>) client.gui.screen();
		Slot slot = screen.getMenu().getSlot(slotIndex);
		return new MouseButtonEvent(leftPos(client) + slot.x + 8, topPos(client) + slot.y + 8,
				new MouseButtonInfo(InputConstants.MOUSE_BUTTON_LEFT, InputConstants.MOD_SHIFT));
	}

	private static void check(boolean condition, String what) {
		if (!condition) {
			throw new AssertionError("Expected: " + what);
		}
	}
}
