package com.vanillasugar.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.gui.GuiGraphicsExtractor;
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
import net.minecraft.world.item.Items;

import com.vanillasugar.VanillaSugar;
import com.vanillasugar.client.BackpackPanel;
import com.vanillasugar.client.InventoryTab;
import com.vanillasugar.client.VanillaSugarClient;
import com.vanillasugar.client.map.MarkerDisplayButton;
import com.vanillasugar.client.map.Minimap;
import com.vanillasugar.client.map.WorldMapScreen;
import com.vanillasugar.gear.GearLayout;
import com.vanillasugar.item.VanillaSugarItems;

/**
 * Adds the backpack and world map tabs, the close button of the open backpack panel and, next to the
 * minimap, the marker display button. The backpack panel and the recipe book do not fit on screen
 * together, so opening one closes the other; widgets are rebuilt so the inventory re-centers.
 */
@Mixin(InventoryScreen.class)
public abstract class InventoryScreenMixin extends AbstractRecipeBookScreen<InventoryMenu> {
	@Unique
	private static final WidgetSprites CLOSE_SPRITES = new WidgetSprites(
			VanillaSugar.id("container/backpack_close"), VanillaSugar.id("container/backpack_close_highlighted"));

	@Shadow
	private boolean buttonClicked;

	/** What the widgets were laid out for: backpack worn, backpack open, map worn, minimap shown. */
	@Unique
	private int vanillasugar$layout = -1;

	private InventoryScreenMixin(InventoryMenu menu, RecipeBookComponent<?> recipeBook, Inventory inventory, Component title) {
		super(menu, recipeBook, inventory, title);
	}

	@Inject(method = "init", at = @At("TAIL"))
	private void vanillasugar$addButtons(CallbackInfo ci) {
		if (minecraft.player.hasInfiniteMaterials()) {
			return;
		}
		// The recipe book can be reopened from a crafting table while the backpack was left open:
		// the backpack key asks for the backpack, otherwise the book stays
		boolean backpackWanted = BackpackPanel.takeOpenRequest();
		if (vanillasugar$recipeBook().isVisible() && BackpackPanel.isOpen()) {
			if (backpackWanted) {
				vanillasugar$recipeBook().toggleVisibility();
			} else {
				BackpackPanel.setOpen(false);
			}
			rebuildWidgets();
			return;
		}

		vanillasugar$layout = vanillasugar$currentLayout();
		if (BackpackPanel.showsBackpackTab()) {
			addRenderableWidget(new InventoryTab(leftPos + GearLayout.BUTTON_X, topPos + GearLayout.BUTTON_Y,
					new ItemStack(VanillaSugarItems.BACKPACK), Component.translatable("gui.vanillasugar.backpack.open"),
					button -> vanillasugar$setBackpackOpen(true)));
		}
		if (BackpackPanel.isMapWorn()) {
			addRenderableWidget(new InventoryTab(leftPos + BackpackPanel.mapTabX(), topPos + BackpackPanel.mapTabY(),
					new ItemStack(Items.FILLED_MAP), Component.translatable("gui.vanillasugar.world_map.open"),
					button -> {
						// Closed first, so an item held on the cursor goes back to the inventory
						onClose();
						minecraft.gui.setScreen(new WorldMapScreen());
					}));
		}
		if (BackpackPanel.isOpen()) {
			ImageButton close = addRenderableWidget(new ImageButton(leftPos + GearLayout.CLOSE_X, topPos + GearLayout.CLOSE_Y,
					GearLayout.CLOSE_SIZE, GearLayout.CLOSE_SIZE, CLOSE_SPRITES, button -> vanillasugar$setBackpackOpen(false)));
			close.setTooltip(Tooltip.create(Component.translatable("gui.vanillasugar.backpack.close")));
		}
		if (Minimap.isShown(minecraft)) {
			addRenderableWidget(new MarkerDisplayButton(width));
		}
	}

	@Unique
	private int vanillasugar$currentLayout() {
		return (BackpackPanel.isWorn() ? 1 : 0) | (BackpackPanel.isOpen() ? 2 : 0) | (BackpackPanel.isMapWorn() ? 4 : 0)
				| (Minimap.isShown(minecraft) ? 8 : 0);
	}

	@Unique
	private void vanillasugar$setBackpackOpen(boolean open) {
		if (open && vanillasugar$recipeBook().isVisible()) {
			vanillasugar$recipeBook().toggleVisibility();
		}
		BackpackPanel.setOpen(open);
		buttonClicked = true;
		rebuildWidgets();
	}

	/** The backpack key opens and closes the panel, unless the recipe book is open to type in. */
	@Override
	public boolean keyPressed(KeyEvent event) {
		if (VanillaSugarClient.OPEN_BACKPACK.matches(event) && BackpackPanel.isWorn() && !vanillasugar$recipeBook().isVisible()) {
			if (BackpackPanel.isOpen()) {
				onClose();
			} else {
				vanillasugar$setBackpackOpen(true);
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
	private void vanillasugar$followRecipeBook(CallbackInfo ci) {
		if (vanillasugar$recipeBook().isVisible() && BackpackPanel.isOpen()) {
			BackpackPanel.setOpen(false);
		}
		rebuildWidgets();
	}

	/** Equipping or removing gear while the inventory is open shows or hides the tabs and panel. */
	@Inject(method = "containerTick", at = @At("TAIL"))
	private void vanillasugar$followGear(CallbackInfo ci) {
		if (vanillasugar$layout != -1 && vanillasugar$layout != vanillasugar$currentLayout()) {
			rebuildWidgets();
		}
	}

	@Inject(method = "extractBackground", at = @At("TAIL"))
	private void vanillasugar$extractGear(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a, CallbackInfo ci) {
		BackpackPanel.extractBackground(graphics, leftPos, topPos);
	}

	@Unique
	private RecipeBookComponent<?> vanillasugar$recipeBook() {
		return ((AbstractRecipeBookScreenAccessor) this).vanillasugar$getRecipeBookComponent();
	}
}
