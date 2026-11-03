package com.cassonade.tweaks;

import net.fabricmc.fabric.api.gamerule.v1.GameRuleBuilder;

import net.minecraft.world.level.gamerules.GameRule;
import net.minecraft.world.level.gamerules.GameRuleCategory;

import com.cassonade.Cassonade;

/**
 * A game rule like vanilla's spawn_phantoms, for players who want the world without creepers:
 * {@code /gamerule cassonade:spawn_creepers false} stops their natural spawns (see SpawnPlacementsMixin).
 * Creepers already in the world and those from spawn eggs or commands are left alone.
 */
public final class NoCreepers {
	public static final GameRule<Boolean> SPAWN_CREEPERS = GameRuleBuilder.forBoolean(true)
			.category(GameRuleCategory.SPAWNING)
			.buildAndRegister(Cassonade.id("spawn_creepers"));

	private NoCreepers() {
	}

	/** Loads the class, which registers the rule. */
	public static void init() {
	}
}
