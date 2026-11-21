package com.cassonade.mixin;

import org.spongepowered.asm.mixin.Mixin;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Silverfish;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;

import com.cassonade.settings.Setting;
import com.cassonade.settings.Settings;

/**
 * Silverfishs are easier to aim at, like baby zombies: the box the crosshair looks for is a tenth of a block larger on
 * every side. Their real hitbox stays the game's, under half a block, so they still crawl through the same gaps.
 */
@Mixin(Silverfish.class)
public abstract class SilverfishMixin extends Monster {
	protected SilverfishMixin(EntityType<? extends Monster> type, Level level) {
		super(type, level);
	}

	@Override
	public float getPickRadius() {
		return Settings.on(Setting.SMALL_MOB_AIM) ? 0.1F : super.getPickRadius();
	}
}
