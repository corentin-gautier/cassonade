package com.cassonade.mixin;

import org.spongepowered.asm.mixin.Mixin;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.level.Level;

import com.cassonade.settings.Setting;
import com.cassonade.settings.Settings;

/**
 * Baby zombies, and the husks, drowned, zombie villagers and zombified piglins born small, are easier to aim at: the
 * box the crosshair looks for is a tenth of a block larger on every side. Their real hitbox stays the game's, under a
 * block high, so they still slip through the same gaps.
 */
@Mixin(Zombie.class)
public abstract class ZombieMixin extends Monster {
	protected ZombieMixin(EntityType<? extends Monster> type, Level level) {
		super(type, level);
	}

	@Override
	public float getPickRadius() {
		return isBaby() && Settings.on(Setting.BABY_ZOMBIE_AIM) ? 0.1F : super.getPickRadius();
	}
}
