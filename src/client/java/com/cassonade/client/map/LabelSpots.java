package com.cassonade.client.map;

import java.util.ArrayList;
import java.util.List;

/**
 * Where the labels drawn so far sit, so the next ones don't land on them: a label that would moves away from its
 * icon by whole lines, and is left out when even that doesn't free it. Those placed first keep their spot.
 */
public final class LabelSpots {
	/** Beyond this many lines, a label would no longer read as its icon's. */
	private static final int MAX_LINES_AWAY = 3;
	private final List<float[]> taken = new ArrayList<>();

	/**
	 * The top of a label of this size centered on x and wished at {@code top}, moved by {@code step} per line
	 * if needed, and now taken; NaN if it has no room.
	 */
	public float place(float x, float top, float width, float height, float step) {
		float left = x - width / 2;
		for (int line = 0; line <= MAX_LINES_AWAY; line++) {
			float at = top + line * step;
			if (isFree(left, at, width, height)) {
				taken.add(new float[] {left, at, left + width, at + height});
				return at;
			}
		}
		return Float.NaN;
	}

	private boolean isFree(float left, float top, float width, float height) {
		for (float[] box : taken) {
			if (left < box[2] && left + width > box[0] && top < box[3] && top + height > box[1]) {
				return false;
			}
		}
		return true;
	}
}
