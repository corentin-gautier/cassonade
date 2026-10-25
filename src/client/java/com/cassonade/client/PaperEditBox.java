package com.cassonade.client;

import net.minecraft.network.chat.Component;

/** A text field written on paper (EditBoxMixin): a pale field, ink without shadow, a faded hint. */
public interface PaperEditBox {
	void cassonade$writeOnPaper(Component hint);
}
