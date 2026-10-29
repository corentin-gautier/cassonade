package com.cassonade.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.client.gui.screens.inventory.AbstractRecipeBookScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemStack;

import com.cassonade.Cassonade;
import com.cassonade.client.BackpackPanel;
import com.cassonade.client.InventoryTab;
import com.cassonade.client.CassonadeClient;
import com.cassonade.client.map.MarkerDisplayButton;
import com.cassonade.client.map.MinimapRotationButton;
import com.cassonade.client.map.Minimap;
import com.cassonade.client.map.WorldMapScreen;
import com.cassonade.client.storage.StorageButtons;
import com.cassonade.client.storage.ContainerSearch;
import com.cassonade.gear.GearLayout;
import com.cassonade.item.CassonadeItems;

/**
 * Puts tabs along the top of the inventory toggling the recipe book (in place of vanilla's button),
 * the backpack and the world map, a strap on the open backpack that closes it (the open recipe book
 * has its ribbon, see {@link AbstractRecipeBookScreenMixin}), the storage buttons (see {@link StorageButtons}) and, next to the minimap, the marker display and minimap rotation buttons. The
 * backpack panel and the recipe book do not fit on screen together, so opening one closes the other;
 * widgets are rebuilt so the inventory re-centers.
 */
@Mixin(InventoryScreen.class)
public abstract class InventoryScreenMixin extends AbstractRecipeBookScreen<InventoryMenu> {
	@Unique
	private static final WidgetSprites STRAP_SPRITES = new WidgetSprites(
			Cassonade.id("container/backpack_strap"), Cassonade.id("container/backpack_strap_highlighted"));

	@Shadow
	private boolean buttonClicked;

	/** What the widgets were laid out for: backpack worn, backpack open, map worn, minimap shown. */
	@Unique
	private int cassonade$layout = -1;

	private InventoryScreenMixin(InventoryMenu menu, RecipeBookComponent<?> recipeBook, Inventory inventory, Component title) {
		super(menu, recipeBook, inventory, title);
	}

	/** The crafting grid moved right of the gear slots (see {@link GearLayout}): its title follows. */
	@Inject(method = "<init>", at = @At("TAIL"))
	private void cassonade$moveTitle(CallbackInfo ci) {
		titleLabelX = GearLayout.CRAFTING_TITLE_X;
		titleLabelY = GearLayout.CRAFTING_TITLE_Y;
	}

	@Inject(method = "init", at = @At("TAIL"))
	private void cassonade$addButtons(CallbackInfo ci) {
		if (minecraft.player.hasInfiniteMaterials()) {
			return;
		}
		// The recipe book can be reopened from a crafting table while the backpack was left open:
		// the backpack key asks for the backpack, otherwise the book stays
		boolean backpackWanted = BackpackPanel.takeOpenRequest();
		if (cassonade$recipeBook().isVisible() && BackpackPanel.isOpen()) {
			if (backpackWanted) {
				cassonade$recipeBook().toggleVisibility();
			} else {
				BackpackPanel.setOpen(false);
			}
			rebuildWidgets();
			return;
		}

		cassonade$layout = cassonade$currentLayout();
		// Tabs along the top of the inventory toggle the recipe book, the backpack and the world map.
		// The recipe book's tab does what vanilla's button, kept hidden, does; the open book also closes
		// with its ribbon (AbstractRecipeBookScreenMixin)
		int tab = 0;
		for (var widget : children()) {
			if (widget instanceof ImageButton button && cassonade$isRecipeButton(button)) {
				button.visible = false;
				Button.OnPress toggle = pressed -> button.onPress(new KeyEvent(InputConstants.KEY_RETURN, 0, 0));
				boolean open = cassonade$recipeBook().isVisible();
				// The knowledge book with a darker outline, like the backpack's, see tools/gui_style.py
				addRenderableWidget(new InventoryTab(leftPos + GearLayout.tabX(tab++), topPos + GearLayout.TAB_Y, open,
						Cassonade.id("container/inventory_tab_recipe_book"), 18, 16, Component.translatable("gui.cassonade.recipe_book"), toggle));
				break;
			}
		}
		if (BackpackPanel.isWorn()) {
			boolean open = BackpackPanel.isOpen();
			cassonade$addTab(tab++, open, new ItemStack(CassonadeItems.BACKPACK),
					open ? "gui.cassonade.backpack.close" : "gui.cassonade.backpack.open", button -> cassonade$setBackpackOpen(!open));
			if (open) {
				ImageButton strap = addRenderableWidget(new ImageButton(leftPos + GearLayout.STRAP_X, topPos + GearLayout.STRAP_Y,
						GearLayout.STRAP_WIDTH, GearLayout.STRAP_HEIGHT, STRAP_SPRITES, button -> cassonade$setBackpackOpen(false)));
				strap.setTooltip(Tooltip.create(Component.translatable("gui.cassonade.backpack.close")));
			}
		}
		if (BackpackPanel.isMapWorn()) {
			// The map item reaches the tab's edges: a smaller icon of it, outlined, see tools/gui_style.py
			addRenderableWidget(new InventoryTab(leftPos + GearLayout.tabX(tab), topPos + GearLayout.TAB_Y, false, Cassonade.id("container/inventory_tab_map"),
					14, 14, Component.translatable("gui.cassonade.world_map.open"), button -> {
						// Closed first, so an item held on the cursor goes back to the inventory
						onClose();
						minecraft.gui.setScreen(new WorldMapScreen());
					}));
		}
		StorageButtons.addToInventory(leftPos, topPos, this::addRenderableWidget);
		if (BackpackPanel.isOpen()) {
			StorageButtons.addToBackpack(leftPos, topPos, this::addRenderableWidget);
			ContainerSearch.addToScreen(this, this::addRenderableWidget);
		}
		if (Minimap.isShown(minecraft)) {
			addRenderableWidget(new MarkerDisplayButton(width));
			addRenderableWidget(new MinimapRotationButton(width));
		}
	}


	@Unique
	private void cassonade$addTab(int index, boolean selected, ItemStack icon, String tooltip, Button.OnPress onPress) {
		addRenderableWidget(new InventoryTab(leftPos + GearLayout.tabX(index), topPos + GearLayout.TAB_Y, selected, icon,
				Component.translatable(tooltip), onPress));
	}

	@Unique
	private static boolean cassonade$isRecipeButton(ImageButton button) {
		return ((ImageButtonAccessor) button).cassonade$getSprites() == RecipeBookComponent.RECIPE_BUTTON_SPRITES;
	}

	@Unique
	private int cassonade$currentLayout() {
		return (BackpackPanel.isWorn() ? 1 : 0) | (BackpackPanel.isOpen() ? 2 : 0) | (BackpackPanel.isMapWorn() ? 4 : 0)
				| (Minimap.isShown(minecraft) ? 8 : 0);
	}

	@Unique
	private void cassonade$setBackpackOpen(boolean open) {
		if (open && cassonade$recipeBook().isVisible()) {
			cassonade$recipeBook().toggleVisibility();
		}
		BackpackPanel.setOpen(open);
		buttonClicked = true;
		rebuildWidgets();
	}

	/** The backpack key opens and closes the panel, unless the recipe book is open to type in. */
	@Override
	public boolean keyPressed(KeyEvent event) {
		if (CassonadeClient.OPEN_BACKPACK.matches(event) && BackpackPanel.isWorn() && !cassonade$recipeBook().isVisible()) {
			if (BackpackPanel.isOpen()) {
				onClose();
			} else {
				cassonade$setBackpackOpen(true);
			}
			return true;
		}
		return super.keyPressed(event);
	}

	/** The backpack opens with the inventory on demand only: closing the inventory closes it. */
	@Override
	public void removed() {
		super.removed();
		if (BackpackPanel.isOpen()) {
			BackpackPanel.setOpen(false);
		}
	}

	/** The recipe book moves the inventory: rebuild so the tabs follow, closing the backpack it replaces. */
	@Inject(method = "onRecipeBookButtonClick", at = @At("TAIL"))
	private void cassonade$followRecipeBook(CallbackInfo ci) {
		if (cassonade$recipeBook().isVisible() && BackpackPanel.isOpen()) {
			BackpackPanel.setOpen(false);
		}
		rebuildWidgets();
	}

	/** Equipping or removing gear while the inventory is open shows or hides the tabs and panel. */
	@Inject(method = "containerTick", at = @At("TAIL"))
	private void cassonade$followGear(CallbackInfo ci) {
		if (cassonade$layout != -1 && cassonade$layout != cassonade$currentLayout()) {
			rebuildWidgets();
		}
	}

	@Inject(method = "extractBackground", at = @At("TAIL"))
	private void cassonade$extractGear(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a, CallbackInfo ci) {
		BackpackPanel.extractBackground(graphics, leftPos, topPos);
	}

	@Unique
	private RecipeBookComponent<?> cassonade$recipeBook() {
		return ((AbstractRecipeBookScreenAccessor) this).cassonade$getRecipeBookComponent();
	}
}
