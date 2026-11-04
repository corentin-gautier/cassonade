package com.cassonade.combat;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

/**
 * Poison lasts half as long and hurts half as often, whatever gives it (potion, cave spider, pufferfish,
 * stew, command): a single potion no longer takes a player down to half a heart. The duration is halved
 * as the effect is added (LivingEntityMixin) and in tooltips (PotionContentsMixin), the pace in
 * PoisonMobEffectMixin.
 */
public final class Poison {
	public static final int DAMAGE_INTERVAL = 50;

	private Poison() {
	}

	public static MobEffectInstance shorten(MobEffectInstance effect) {
		if (!effect.is(MobEffects.POISON) || effect.isInfiniteDuration()) {
			return effect;
		}
		return new MobEffectInstance(effect.getEffect(), effect.getDuration() / 2, effect.getAmplifier(), effect.isAmbient(), effect.isVisible(),
				effect.showIcon());
	}
}
