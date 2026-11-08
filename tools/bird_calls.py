"""Synthesizes the sounds not taken from recordings (see tools/recorded_sounds.py): gulls on the coast, robins in the woods,
sparrows around what was built and swallows over it (sounds/birds/). The notes' pitches and timing were measured on spectrograms of real
ones. Distance is in the files, as the game can't filter a sound as it plays: no echo, none coming back in the open.

Usage (needs the soundfile and numpy packages, whose library writes mono Vorbis):
    pip install soundfile numpy && python3 tools/bird_calls.py
"""

import math
import pathlib
import random

import numpy
import soundfile

TARGET = pathlib.Path(__file__).resolve().parent.parent / "src/main/resources/assets/cassonade/sounds/birds"
RATE = 22050
SECONDS = 4.5
CALLS = 4
# How much of each harmonic: strong middle ones make the tone nasal, as a goose's is
HARMONICS = [1, 0.8, 0.7, 0.45, 0.25, 0.12]
LOWPASS = 1400
HIGHPASS = 300
GULL_RATE = 22050
GULL_HARMONICS = [0.7, 1, 0.4, 0.15]
GULL_LOWPASS = 3200
ROBIN_RATE = 44100
SPARROW_RATE = 44100
# A chirp's harmonics: a strong second one gives the sparrow's slightly harsh, nasal tone
SPARROW_HARMONICS = [1, 0.6, 0.25]


def one_pole(samples, cutoff):
	"""A low pass, run twice for a steeper slope."""
	alpha = 1 - math.exp(-2 * math.pi * cutoff / RATE)
	for _ in range(2):
		state = 0.0
		for i, value in enumerate(samples):
			state += alpha * (value - state)
			samples[i] = state
	return samples


def normalized(samples):
	peak = max(abs(value) for value in samples)
	return [value / peak * 0.8 for value in samples]


def gull_note(samples, start, length, pitch, loudness):
	"""A "kyow": up a little at once, then falling away at the end."""
	phase = 0.0
	for i in range(int(length * GULL_RATE)):
		t = i / (length * GULL_RATE)
		frequency = pitch * (1 + 0.08 * min(t / 0.2, 1) - 0.28 * max(t - 0.55, 0) / 0.45)
		phase += 2 * math.pi * frequency / GULL_RATE
		envelope = min(t / 0.08, 1) * min((1 - t) / 0.2, 1) * loudness
		samples[int(start * GULL_RATE) + i] += envelope * sum(amount * math.sin(phase * (h + 1)) for h, amount in enumerate(GULL_HARMONICS))


def gull(seed):
	"""The long call: a few drawn-out notes, then a run of short ones, slowing and fading."""
	rng = random.Random(100 + seed)
	notes = rng.randint(8, 16)
	samples = [0.0] * int((1.2 + notes * 0.36) * GULL_RATE)
	pitch = rng.uniform(1120, 1280)
	at = 0.02
	for _ in range(rng.randint(1, 2)):
		gull_note(samples, at, rng.uniform(0.4, 0.55), pitch, 0.8)
		at += 0.6
	for index in range(notes):
		progress = index / notes
		gull_note(samples, at, rng.uniform(0.17, 0.22), pitch * (1 + rng.uniform(-0.015, 0.015)), 1 - 0.6 * progress)
		at += 0.25 + 0.1 * progress
	alpha = 1 - math.exp(-2 * math.pi * GULL_LOWPASS / GULL_RATE)
	state = 0.0
	for i, value in enumerate(samples):
		state += alpha * (value - state)
		samples[i] = state
	return normalized(samples)


def robin(seed):
	"""A phrase of quick pure notes sweeping between 4 and 8 kHz, a few close together, with short gaps and a trill."""
	rng = random.Random(200 + seed)
	samples = [0.0] * int(3.2 * ROBIN_RATE)
	at = 0.02
	while at < 2.5:
		length = rng.uniform(0.05, 0.16)
		start, end = rng.uniform(4000, 8200), rng.uniform(3800, 7000)
		trill = rng.choice((0, 0, 45, 70))
		loudness = rng.uniform(0.5, 1)
		phase = 0.0
		for i in range(int(length * ROBIN_RATE)):
			t = i / (length * ROBIN_RATE)
			frequency = start + (end - start) * t + 350 * trill / 70 * math.sin(2 * math.pi * trill * t * length)
			phase += 2 * math.pi * frequency / ROBIN_RATE
			envelope = min(t / 0.15, 1) * min((1 - t) / 0.25, 1) * loudness
			samples[int(at * ROBIN_RATE) + i] += math.sin(phase) * envelope
		at += length + rng.choice((0.01, 0.02, 0.03, 0.09))
	return normalized(samples)


def sparrow(seed):
	"""A few "chirrup"s, short notes rising then falling around 3 to 5 kHz, at uneven intervals, as sparrows call on a roof."""
	rng = random.Random(300 + seed)
	samples = [0.0] * int(2.4 * SPARROW_RATE)
	at = 0.02
	for _ in range(rng.randint(3, 6)):
		length = rng.uniform(0.07, 0.13)
		pitch = rng.uniform(3200, 4200)
		loudness = rng.uniform(0.6, 1)
		phase = 0.0
		for i in range(int(length * SPARROW_RATE)):
			t = i / (length * SPARROW_RATE)
			# Up at once, then sliding down past where it started
			frequency = pitch * (1 + 0.22 * math.sin(math.pi * min(t / 0.35, 1)) - 0.12 * max(t - 0.35, 0) / 0.65)
			phase += 2 * math.pi * frequency / SPARROW_RATE
			envelope = min(t / 0.1, 1) * min((1 - t) / 0.3, 1) * loudness
			samples[int(at * SPARROW_RATE) + i] += envelope * sum(amount * math.sin(phase * (h + 1)) for h, amount in enumerate(SPARROW_HARMONICS))
		at += length + rng.choice((0.08, 0.12, 0.18, 0.3))
	return normalized(samples)


def swallow(seed):
	"""A twitter: a quick run of tiny notes jumping between 3 and 8 kHz, a few sharper "vit"s among them, as swallows call in flight."""
	rng = random.Random(400 + seed)
	samples = [0.0] * int(2.2 * SPARROW_RATE)
	at = 0.02
	while at < 1.8:
		sharp = rng.random() < 0.2
		length = rng.uniform(0.05, 0.08) if sharp else rng.uniform(0.02, 0.045)
		start, end = (rng.uniform(6000, 7500), rng.uniform(3500, 4500)) if sharp else (rng.uniform(3000, 8000), rng.uniform(3000, 8000))
		loudness = rng.uniform(0.7, 1) if sharp else rng.uniform(0.3, 0.7)
		phase = 0.0
		for i in range(int(length * SPARROW_RATE)):
			t = i / (length * SPARROW_RATE)
			frequency = start + (end - start) * t
			phase += 2 * math.pi * frequency / SPARROW_RATE
			envelope = min(t / 0.15, 1) * min((1 - t) / 0.3, 1) * loudness
			samples[int(at * SPARROW_RATE) + i] += math.sin(phase) * envelope
		at += length + rng.choice((0.005, 0.01, 0.02, 0.06))
	return normalized(samples)


def main():
	TARGET.mkdir(parents=True, exist_ok=True)
	for index in range(CALLS):
		write(f"gull{index + 1}", gull(index), GULL_RATE)
		write(f"robin{index + 1}", robin(index), ROBIN_RATE)
		write(f"sparrow{index + 1}", sparrow(index), SPARROW_RATE)
		write(f"swallow{index + 1}", swallow(index), SPARROW_RATE)


def write(name, samples, rate):
	soundfile.write(TARGET / f"{name}.ogg", numpy.array(samples, dtype=numpy.float32), rate, format="OGG", subtype="VORBIS")


if __name__ == "__main__":
	main()
