package com.cassonade.client;

import net.minecraft.network.chat.Component;

/** Text field styles applied by EditBoxMixin. */
public interface PaperEditBox {
	void cassonade$writeOnPaper(Component hint);

	void cassonade$writeOnStorage(Component hint);
}
