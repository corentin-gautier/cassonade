package com.cassonade.client;

import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;

public final class HudLayout {
	public static final int BOTTOM_MARGIN = 4;

	private HudLayout() {
	}

	public static HudElement shiftUp(HudElement element) {
		return (graphics, deltaTracker) -> {
			graphics.pose().pushMatrix();
			graphics.pose().translate(0, -BOTTOM_MARGIN);
			try {
				element.extractRenderState(graphics, deltaTracker);
			} finally {
				graphics.pose().popMatrix();
			}
		};
	}
}
