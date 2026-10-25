package com.cassonade.item;

import net.minecraft.world.item.Item;

/**
 * Worn in the backpack gear slot. Its contents live in the vanilla {@code container} component,
 * so a dropped or stored backpack keeps everything inside, like a shulker box.
 */
public class BackpackItem extends Item {
	public BackpackItem(Properties properties) {
		super(properties);
	}

	@Override
	public boolean canFitInsideContainerItems() {
		return false;
	}
}
