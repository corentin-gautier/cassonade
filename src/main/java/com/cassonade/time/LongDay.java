package com.cassonade.time;

/**
 * Daytime lasts twice as long: from sunrise to sunset the overworld clock runs at half its rate, then sunset,
 * night and dawn pass as in vanilla. Only the rate the clock runs and is sent at changes, never the rate it
 * saves, so /time rate keeps its meaning and a world left in the day keeps the vanilla pace without the mod.
 * Implemented on the server clock instances (ServerClockInstanceMixin, ServerClockManagerMixin).
 */
public interface LongDay {
	float DAY_RATE = 0.5F;
	/** Day ticks 0 to 12000: from the end of dawn to the start of sunset, when no monster spawns in the open. */
	int DAY_END = 12000;
	int TICKS_PER_DAY = 24000;
	/** Game ticks a whole day and night take at the vanilla rate: 36000 instead of 24000. */
	int CYCLE_TICKS = TICKS_PER_DAY - DAY_END + (int) (DAY_END / DAY_RATE);

	static boolean isDaytime(long totalTicks) {
		return Math.floorMod(totalTicks, TICKS_PER_DAY) < DAY_END;
	}

	/** The overworld clock time {@code gameTicks} from {@code time}, the clock running at {@code rate} outside the day. */
	static long after(long time, long gameTicks, float rate) {
		if (rate <= 0) {
			return time;
		}
		double clock = time;
		double left = gameTicks;
		// One phase at a time: a weather timer spans a few days at most
		while (true) {
			long tickOfDay = Math.floorMod((long) clock, TICKS_PER_DAY);
			boolean day = tickOfDay < DAY_END;
			long phaseLeft = (day ? DAY_END : TICKS_PER_DAY) - tickOfDay;
			double pace = day ? rate * DAY_RATE : rate;
			if (phaseLeft / pace >= left) {
				return (long) (clock + left * pace);
			}
			clock += phaseLeft;
			left -= phaseLeft / pace;
		}
	}

	boolean cassonade$isSlowed();

	void cassonade$setSlowed(boolean slowed);
}
