package com.cassonade.client.storage;

import java.util.Locale;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.Consumer;

import org.jspecify.annotations.Nullable;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.gui.screens.inventory.ShulkerBoxScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.ShulkerBoxMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BundleContents;
import net.minecraft.world.item.component.ItemContainerContents;
import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;

import com.cassonade.client.BackpackPanel;
import com.cassonade.Cassonade;
import com.cassonade.client.PaperEditBox;
import com.cassonade.client.mixin.AbstractContainerScreenAccessor;
import com.cassonade.gear.BackpackContainer;
import com.cassonade.gear.GearLayout;

/** Search in storage screens without taking space from their titles until the search is opened. */
public final class ContainerSearch {
	private static final int BUTTON_SIZE = 11;
	private static final int GAP = 2;
	private static final int RIGHT_MARGIN = 7;
	private static final int SEARCH_WIDTH = 112;
	/** The size of its sprite (tools/search_field_texture.py). */
	private static final int SEARCH_HEIGHT = 22;
	/** Space between the field and the window below it, or the backpack's strap. */
	private static final int SEARCH_GAP = 3;
	private static final WidgetSprites SEARCH_BUTTON_SPRITES = new WidgetSprites(
			Cassonade.id("container/storage_search"), Cassonade.id("container/storage_search_highlighted"));
	private static final Map<AbstractContainerScreen<?>, SearchState> SEARCHES = new WeakHashMap<>();

	private ContainerSearch() {
	}

	public static void addToScreen(AbstractContainerScreen<?> screen, Consumer<AbstractWidget> add) {
		if (!supports(screen)) {
			return;
		}

		AbstractContainerScreenAccessor accessor = (AbstractContainerScreenAccessor) screen;
		SearchState state = SEARCHES.computeIfAbsent(screen, unused -> new SearchState());
		int top = accessor.cassonade$getTopPos();
		int right;
		int searchX;
		int searchY;
		if (screen instanceof InventoryScreen) {
			int panelLeft = accessor.cassonade$getLeftPos() + GearLayout.PANEL_X;
			right = panelLeft + GearLayout.PANEL_WIDTH - RIGHT_MARGIN;
			searchX = panelLeft + (GearLayout.PANEL_WIDTH - SEARCH_WIDTH) / 2;
			searchY = top + GearLayout.STRAP_Y - SEARCH_HEIGHT - SEARCH_GAP;
		} else {
			right = accessor.cassonade$getLeftPos() + accessor.cassonade$getImageWidth() - RIGHT_MARGIN;
			searchX = accessor.cassonade$getLeftPos() + (accessor.cassonade$getImageWidth() - SEARCH_WIDTH) / 2;
			searchY = top - SEARCH_HEIGHT - SEARCH_GAP;
		}

		EditBox search = new SearchEditBox(searchX, searchY);
		search.setBordered(true);
		search.setMaxLength(64);
		search.setValue(state.query);
		search.setResponder(query -> state.query = query);
		((PaperEditBox) search).cassonade$writeOnStorage(Component.translatable("gui.cassonade.storage.search_hint"));
		search.visible = state.open;
		state.search = search;
		add.accept(search);
		if (state.open) {
			screen.setFocused(search);
			search.setFocused(true);
		}

		ImageButton button = new SearchButton(right - BUTTON_SIZE - 2 * (BUTTON_SIZE + GAP), top + 4,
				() -> state.setOpen(screen, true));
		button.setTooltip(Tooltip.create(Component.translatable("gui.cassonade.storage.search")));
		add.accept(button);
	}

	public static boolean handleMouseClicked(AbstractContainerScreen<?> screen, MouseButtonEvent event, boolean doubleClick) {
		SearchState state = SEARCHES.get(screen);
		if (state == null || !state.open || state.search == null) {
			return false;
		}
		if (state.search.isMouseOver(event.x(), event.y())) {
			screen.setFocused(state.search);
			state.search.setFocused(true);
			state.search.mouseClicked(event, doubleClick);
			return true;
		}
		state.setOpen(screen, false);
		return false;
	}

	private static void blur(EditBox search) {
		for (SearchState state : SEARCHES.values()) {
			if (state.search == search && state.open) {
				state.open = false;
				search.visible = false;
				search.setValue("");
				return;
			}
		}
	}

	public static boolean handleKey(AbstractContainerScreen<?> screen, KeyEvent event) {
		if (screen instanceof CreativeModeInventoryScreen) {
			return false;
		}
		SearchState state = SEARCHES.get(screen);
		if (state == null) {
			return false;
		}
		if (event.key() == InputConstants.KEY_ESCAPE && state.open) {
			state.setOpen(screen, false);
			return true;
		}
		if (state.search != null && screen.getFocused() == state.search) {
			if (event.key() != InputConstants.KEY_F || !event.hasControlDownWithQuirk()) {
				state.search.keyPressed(event);
			}
			return true;
		}
		if (event.key() == InputConstants.KEY_F && event.hasControlDownWithQuirk()) {
			state.setOpen(screen, true);
			return true;
		}
		return false;
	}

	public static boolean dims(AbstractContainerScreen<?> screen, Slot slot) {
		SearchState state = SEARCHES.get(screen);
		return state != null && !state.query.isBlank() && isStorageSlot(screen.getMenu(), slot)
				&& !matches(slot.getItem(), state.query);
	}

	private static boolean supports(AbstractContainerScreen<?> screen) {
		return screen instanceof ContainerScreen || screen instanceof ShulkerBoxScreen
				|| screen instanceof InventoryScreen && BackpackPanel.isOpen();
	}

	private static boolean isStorageSlot(AbstractContainerMenu menu, Slot slot) {
		if (menu instanceof ChestMenu || menu instanceof ShulkerBoxMenu) {
			return !(slot.container instanceof Inventory);
		}
		return menu instanceof InventoryMenu && slot.container instanceof BackpackContainer;
	}

	private static boolean matches(ItemStack stack, String query) {
		if (stack.isEmpty()) {
			return false;
		}
		String needle = query.toLowerCase(Locale.ROOT);
		if (contains(stack.getHoverName().getString(), needle)
				|| contains(BuiltInRegistries.ITEM.getKey(stack.getItem()).toString(), needle)) {
			return true;
		}
		if (matchesEnchantments(stack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY), needle)
				|| matchesEnchantments(stack.getOrDefault(DataComponents.STORED_ENCHANTMENTS, ItemEnchantments.EMPTY), needle)) {
			return true;
		}

		ItemContainerContents container = stack.get(DataComponents.CONTAINER);
		if (container != null && container.nonEmptyItemCopyStream().anyMatch(child -> matches(child, needle))) {
			return true;
		}
		BundleContents bundle = stack.get(DataComponents.BUNDLE_CONTENTS);
		return bundle != null && bundle.itemCopies().anyMatch(child -> matches(child, needle));
	}

	private static boolean matchesEnchantments(ItemEnchantments enchantments, String query) {
		for (Holder<Enchantment> enchantment : enchantments.keySet()) {
			if (contains(enchantment.value().description().getString(), query)
					|| enchantment.unwrapKey().map(key -> contains(key.identifier().toString(), query)).orElse(false)) {
				return true;
			}
		}
		return false;
	}

	private static boolean contains(String haystack, String needle) {
		return haystack.toLowerCase(Locale.ROOT).contains(needle);
	}

	private static final class SearchState {
		private String query = "";
		private boolean open;
		private @Nullable EditBox search;

		private void setOpen(AbstractContainerScreen<?> screen, boolean open) {
			this.open = open;
			if (search != null) {
				search.visible = open;
				if (open) {
					search.setFocused(true);
					screen.setFocused(search);
				} else {
					search.setValue("");
					search.setFocused(false);
					screen.setFocused(null);
				}
			}
		}
	}

	private static final class SearchButton extends ImageButton {
		private SearchButton(int x, int y, Runnable onPress) {
			super(x, y, BUTTON_SIZE, BUTTON_SIZE, SEARCH_BUTTON_SPRITES, button -> onPress.run());
		}

		@Override
		public boolean shouldTakeFocusAfterInteraction() {
			return false;
		}
	}

	private static final class SearchEditBox extends EditBox {
		private SearchEditBox(int x, int y) {
			super(Minecraft.getInstance().font, x, y, SEARCH_WIDTH, SEARCH_HEIGHT, Component.empty());
		}

		@Override
		public void setFocused(boolean focused) {
			super.setFocused(focused);
			if (!focused) {
				ContainerSearch.blur(this);
			}
		}
	}
}
