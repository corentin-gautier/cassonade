package com.cassonade.client.settings;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.OptionsSubScreen;
import net.minecraft.network.chat.Component;

import com.cassonade.settings.Setting;

/**
 * The mod's settings, opened from Mod Menu: one button per category, each opening a page of on/off switches,
 * laid out like the game's own options screens.
 */
public class SettingsScreen extends OptionsSubScreen {
	public SettingsScreen(Screen lastScreen) {
		super(lastScreen, net.minecraft.client.Minecraft.getInstance().options, Component.translatable("setting.cassonade.title"));
	}

	@Override
	protected void addOptions() {
		List<AbstractWidget> buttons = new ArrayList<>();
		for (Setting.Category category : Setting.Category.values()) {
			if (SettingsCategoryScreen.settings(category).isEmpty()) {
				continue;
			}
			buttons.add(Button.builder(Component.translatable("setting.cassonade.open", Component.translatable(category.key())),
					button -> minecraft.gui.setScreen(new SettingsCategoryScreen(this, category))).build());
		}
		list.addSmall(buttons);
	}
}
