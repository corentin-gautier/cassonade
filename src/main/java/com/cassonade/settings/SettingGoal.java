package com.cassonade.settings;

import net.minecraft.world.entity.ai.goal.Goal;

/**
 * A mob's goal that only runs while a setting is on, or only while it is off for the game's own goal the mod's replaces:
 * goals are given once, when the mob is made, so a mob already in the world follows the setting when it changes.
 */
public class SettingGoal extends Goal {
	private final Setting setting;
	private final boolean whenOn;
	private final Goal goal;

	private SettingGoal(Setting setting, boolean whenOn, Goal goal) {
		this.setting = setting;
		this.whenOn = whenOn;
		this.goal = goal;
		setFlags(goal.getFlags());
	}

	/** The mod's goal, while the setting is on. */
	public static Goal whileOn(Setting setting, Goal goal) {
		return new SettingGoal(setting, true, goal);
	}

	/** The game's goal, while the setting is off. */
	public static Goal whileOff(Setting setting, Goal goal) {
		return new SettingGoal(setting, false, goal);
	}

	private boolean allowed() {
		return Settings.on(setting) == whenOn;
	}

	@Override
	public boolean canUse() {
		return allowed() && goal.canUse();
	}

	@Override
	public boolean canContinueToUse() {
		return allowed() && goal.canContinueToUse();
	}

	@Override
	public boolean isInterruptable() {
		return goal.isInterruptable();
	}

	@Override
	public void start() {
		goal.start();
	}

	@Override
	public void stop() {
		goal.stop();
	}

	@Override
	public boolean requiresUpdateEveryTick() {
		return goal.requiresUpdateEveryTick();
	}

	@Override
	public void tick() {
		goal.tick();
	}

	@Override
	public String toString() {
		return goal.toString();
	}
}
