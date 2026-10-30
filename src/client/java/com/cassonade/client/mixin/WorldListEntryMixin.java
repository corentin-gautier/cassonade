package com.cassonade.client.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.FaviconTexture;
import net.minecraft.client.gui.screens.worldselection.WorldSelectionList;

import com.cassonade.client.WorldSelectionScreen;

/** The rows of the mod's world selection are drawn by it, with its colors and the details it shows, the icon further in. */
@Mixin(WorldSelectionList.WorldListEntry.class)
public abstract class WorldListEntryMixin {
	@Shadow
	@Final
	private WorldSelectionList list;

	@Shadow
	@Final
	private FaviconTexture icon;

	/**
	 * Vanilla's icon test, from the row's content corner (2 pixels in): moved to where the mod draws the icon,
	 * so a click on it joins the world.
	 */
	public boolean mouseOverIcon(int relX, int relY, int size) {
		int shift = WorldSelectionScreen.styles(list) ? WorldSelectionScreen.ROW_ICON_INSET - 2 : 0;
		return relX >= shift && relX < shift + size && relY >= shift && relY < shift + size;
	}

	@Inject(method = "extractContent", at = @At("HEAD"), cancellable = true)
	private void cassonade$extractRow(GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean hovered, float a, CallbackInfo ci) {
		if (WorldSelectionScreen.styles(list)) {
			WorldSelectionList.WorldListEntry self = (WorldSelectionList.WorldListEntry) (Object) this;
			WorldSelectionScreen.extractRow(graphics, self, icon.textureLocation(), list.getSelected() == self, hovered, mouseX, mouseY,
					list.getY(), list.getBottom());
			ci.cancel();
		}
	}
}
