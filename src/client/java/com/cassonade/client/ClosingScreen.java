package com.cassonade.client;

/** A screen that fades away once closed instead of vanishing: GuiMixin keeps drawing it, ScreenMixin animates it. */
public interface ClosingScreen {
	/** Starts the closing animation; false for the screens that do not animate. */
	boolean startClosing();

	boolean closingFinished();

	/** Whether the window is drawn in place, done opening: always, for the screens that do not animate. */
	boolean settled();
}
