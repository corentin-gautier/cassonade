package com.cassonade.client.settings;

import java.util.Arrays;
import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.OptionsSubScreen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

import com.cassonade.settings.Setting;
import com.cassonade.settings.Settings;

/**
 * One category's switches, one to a row as in the game's wider options. On a server other than the player's own, the
 * server's settings are shown as it set them, greyed out.
 */
public class SettingsCategoryScreen extends OptionsSubScreen {
	private final Setting.Category category;
	/** Whether a setting drawn from the resources changed: they are loaded again on leaving. */
	private boolean reload;

	public SettingsCategoryScreen(Screen lastScreen, Setting.Category category) {
		super(lastScreen, Minecraft.getInstance().options, Component.translatable(category.key()));
		this.category = category;
	}

	static List<Setting> settings(Setting.Category category) {
		return Arrays.stream(Setting.values()).filter(setting -> setting.category() == category).toList();
	}

	@Override
	protected void addOptions() {
		// One to a row, wide enough for the longest names in every language
		for (Setting setting : settings(category)) {
			list.addBig(button(setting));
		}
	}

	@Override
	public void onClose() {
		super.onClose();
		if (reload) {
			minecraft.reloadResourcePacks();
		}
	}

	private CycleButton<Boolean> button(Setting setting) {
		boolean fromServer = setting.side() == Setting.Side.SERVER && Settings.fromRemote();
		CycleButton<Boolean> button = CycleButton.onOffBuilder(Settings.on(setting))
				.create(Component.translatable(setting.key()), (cycle, on) -> {
					Settings.set(setting, on);
					reload |= setting.applies() == Setting.Applies.ON_RESOURCE_RELOAD;
				});
		button.setTooltip(Tooltip.create(tooltip(setting, fromServer)));
		button.active = !fromServer;
		return button;
	}

	/** What the feature does, then when a change takes effect or who decides it. */
	private static Component tooltip(Setting setting, boolean fromServer) {
		Component text = Component.translatable(setting.key() + ".tooltip");
		if (fromServer) {
			return withNote(text, "setting.cassonade.from_server");
		}
		return switch (setting.applies()) {
			case AT_ONCE -> text;
			case ON_WORLD_LOAD -> withNote(text, "setting.cassonade.on_world_load");
			case ON_RESTART -> withNote(text, "setting.cassonade.on_restart");
			case ON_RESOURCE_RELOAD -> text;
		};
	}

	private static Component withNote(Component text, String note) {
		return text.copy().append(CommonComponents.NEW_LINE).append(CommonComponents.NEW_LINE)
				.append(Component.translatable(note).withStyle(ChatFormatting.GRAY));
	}
}
