package com.cassonade.test;

import com.cassonade.client.ambience.MorningMist;

/** The sunrise of the first morning that has mist. Not the world's very first, so the hours before it belong to that morning. */
final class MistyMorning {
	private static final int TICKS_PER_DAY = 24000;

	private MistyMorning() {
	}

	static long sunrise() {
		long morning = 1;
		while (!MorningMist.comes(morning)) {
			morning++;
		}
		return morning * TICKS_PER_DAY;
	}
}
