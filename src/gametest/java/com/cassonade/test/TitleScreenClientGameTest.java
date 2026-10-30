package com.cassonade.test;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;

import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.PlainTextButton;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.options.OptionsScreen;

/** The title screen, at the usual size and at the smallest GUI, where the logo, the buttons and the footer must all fit. */
public class TitleScreenClientGameTest implements FabricClientGameTest {
	@Override
	public void runTest(ClientGameTestContext context) {
		if (TestSelection.skips(this)) {
			return;
		}
		context.getInput().resizeWindow(1920, 1080);
		context.setScreen(TitleScreen::new);
		context.waitTicks(5);
		checkButtonsFit(context);
		Screenshots.take(context, "cassonade-title-screen");

		// The buttons answer where they were moved to, not where vanilla put them
		context.clickScreenButton("menu.options");
		context.waitForScreen(OptionsScreen.class);
		context.setScreen(TitleScreen::new);
		context.waitTicks(5);

		// The smallest GUI (scale 3 in 1280×720: 426×240), in French, whose words are longer
		setLanguage(context, "fr_fr");
		context.getInput().resizeWindow(1280, 720);
		context.waitTicks(5);
		checkButtonsFit(context);
		Screenshots.take(context, "cassonade-title-screen-small-fr");
		setLanguage(context, "en_us");
		context.getInput().resizeWindow(1920, 1080);
	}

	/** Moving the buttons must leave them on screen and above the version and the credits. */
	private static void checkButtonsFit(ClientGameTestContext context) {
		boolean fit = context.computeOnClient(client -> client.gui.screen().children().stream()
				.filter(AbstractWidget.class::isInstance).map(AbstractWidget.class::cast)
				.filter(widget -> !(widget instanceof PlainTextButton))
				.allMatch(widget -> widget.getY() > 0 && widget.getY() + widget.getHeight() <= client.gui.screen().height - 10));
		if (!fit) {
			throw new AssertionError("Expected: the title screen's buttons between the logo and the footer");
		}
	}

	private static void setLanguage(ClientGameTestContext context, String code) {
		context.runOnClient(client -> {
			client.getLanguageManager().setSelected(code);
			client.options.languageCode = code;
			client.reloadResourcePacks();
		});
		context.waitFor(client -> client.gui.overlay() == null);
		context.waitTicks(5);
	}
}
