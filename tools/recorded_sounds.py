"""Cuts the mod's real recordings (sounds/recorded/) from their sources on Wikimedia Commons and Freesound, credited in sounds/CREDITS.md:
mono, the stretch kept, filtered and brought to one loudness, the volume left to client/ambience. Echo only on the owl,
heard among trees at night: once isolated, it sounded too bare.

Usage (needs ffmpeg, and the soundfile and numpy packages; the sources downloaded under the names below):
    python3 tools/recorded_sounds.py <folder of the downloaded sources>
"""

import pathlib
import subprocess
import sys

import numpy
import soundfile

TARGET = pathlib.Path(__file__).resolve().parent.parent / "src/main/resources/assets/cassonade/sounds/recorded"
RATE = 32000
LOUDNESS = 0.1
FADE = 0.6
REVERB_SECONDS = 1.6
REVERB_LEVEL = 0.45
# Cut from the wood around them: only their notes' band, and silence between the notes
ISOLATED = [
	("cuckoo1", "cuckoo1.mp3", 0, 6, 380, 1500),
	("cuckoo2", "cuckoo1.mp3", 8, 17, 380, 1500),
	("owl1", "owl_song.mp3", 5, 9.5, 500, 3000),
	("owl2", "owl_song.mp3", 37, 41.5, 500, 3000),
]
# Name, source, start, end, lowest and highest frequency kept
SOUNDS = [
	("geese1", "goose1.ogg", 8, 20, 250, 2400),
	("geese2", "goose1.ogg", 30, 42, 250, 2400),
	("geese3", "goose1.ogg", 50, 62, 250, 2400),
	("cricket1", "cricket.ogg", 2, 10, 2500, 13000),
	("cricket2", "cricket.ogg", 12, 20, 2500, 13000),
	("cricket3", "treecricket.oga", 3, 11, 1500, 9000),
	("cricket4", "treecricket.oga", 13, 21, 1500, 9000),
	("grasshopper1", "grasshopper.oga", 0.5, 12.5, 2500, 15500),
	("grasshopper2", "grasshopper.oga", 15.5, 28, 2500, 15500),
	# A lone wolf, a single howl each: the second recording's one before the car that drowns it out
	("wolf1", "wolf_denali.mp3", 0, 4.3, 250, 3000),
	("wolf2", "wolf_yellowstone.mp3", 3.2, 7, 250, 3000),
]
# Stereo and seamless, played without a position: heard all around. Name, source, start, length in seconds, lowest and highest frequency kept
LOOPS = [
	("waves", "waves.ogg", 5, 105, 60, 14000),
	# From afar: the highs are lost on the way. The lows go too: without the waves' highs over it, the steady hum under 150 Hz
	# in this recording sounded like a far-off engine
	("waves_far", "waves.ogg", 25, 40, 300, 1200),
	# A storm's gusts on an open hill, a sandstorm's in the rain. Only a deep rumble is kept: the hiss above it, once
	# heard alone, sounded like rain falling behind the wind. The last seconds buffet the microphone the most, so they are left out
	("wind", "wind.ogg", 1, 55, 40, 800),
	# A blizzard's wind over a snowy road, in the rain: calm, mostly low. Before and after, something knocks near the microphone
	("blizzard", "snowstorm.mp3", 70, 42, 30, 12000),
	# A nor'easter's gale through a snowstorm, in a thunderstorm
	("blizzard_thunder", "noreaster.mp3", 0.5, 37, 30, 12000),
	# A gale over a dry plain, a sandstorm's in a thunderstorm: its most even stretch, between gusts that clip the microphone
	("wind_thunder", "anatolia.mp3", 61, 23, 30, 14000),
]
LOOP_FADE = 4


def cut(source, start, end, low, high):
	command = ["ffmpeg", "-loglevel", "error", "-ss", str(start), "-t", str(end - start), "-i", str(source),
			"-af", f"highpass=f={low},lowpass=f={high}", "-ac", "1", "-ar", str(RATE), "-f", "f32le", "-"]
	return numpy.frombuffer(subprocess.run(command, capture_output=True, check=True).stdout, dtype=numpy.float32).copy()


def loop(source, start, length, low, high):
	"""Runs a little past its end, the excess faded over its beginning at constant power: nothing marks the point where it starts again."""
	command = ["ffmpeg", "-loglevel", "error", "-ss", str(start), "-t", str(length + LOOP_FADE), "-i", str(source),
			"-af", f"highpass=f={low},lowpass=f={high}", "-ac", "2", "-ar", str(RATE), "-f", "f32le", "-"]
	samples = numpy.frombuffer(subprocess.run(command, capture_output=True, check=True).stdout, dtype=numpy.float32).reshape(-1, 2).copy()
	fade = LOOP_FADE * RATE
	body, tail = samples[:-fade], samples[-fade:]
	ramp = numpy.linspace(0, numpy.pi / 2, fade)[:, None]
	body[:fade] = body[:fade] * numpy.sin(ramp) + tail * numpy.cos(ramp)
	return body


def fade_ends(samples):
	length = int(FADE * RATE)
	ramp = numpy.linspace(0, 1, length)
	samples[:length] *= ramp
	samples[-length:] *= ramp[::-1]
	return samples


def isolate(samples, low, high):
	"""Only the frequencies given, sharply, then the sound opened only where a note is well above what is left around it."""
	frequencies = numpy.fft.rfftfreq(len(samples), 1 / RATE)
	ramp = 40
	band = numpy.clip((frequencies - low + ramp) / ramp, 0, 1) * numpy.clip((high + ramp - frequencies) / ramp, 0, 1)
	samples = numpy.fft.irfft(numpy.fft.rfft(samples) * band, len(samples)).astype(numpy.float32)
	window = int(0.015 * RATE)
	level = numpy.sqrt(numpy.convolve(samples ** 2, numpy.ones(window) / window, "same"))
	opened = (level > 0.2 * numpy.percentile(level, 99.5)).astype(numpy.float32)
	# Held open a moment over each note's tail, then eased, so that the gate is never heard to shut
	hold = int(0.04 * RATE)
	opened = (numpy.convolve(opened, numpy.ones(hold), "same") > 0).astype(numpy.float32)
	fade = int(0.025 * RATE)
	opened = numpy.convolve(opened, numpy.ones(fade) / fade, "same")
	return samples * opened.astype(numpy.float32)


def reverb(samples):
	"""A wood's soft tail: fading noise, its highs dying first as among leaves, mixed in under the dry sound."""
	rng = numpy.random.default_rng(7)
	length = int(REVERB_SECONDS * RATE)
	time = numpy.arange(length) / RATE
	tail = rng.normal(size=length) * numpy.exp(-time * 6.9 / REVERB_SECONDS)
	# A low pass over the tail, gentle: a running average of a few milliseconds
	window = int(0.0012 * RATE)
	tail = numpy.convolve(tail, numpy.ones(window) / window, "same")
	padded = numpy.concatenate((samples, numpy.zeros(length)))
	wet = numpy.fft.irfft(numpy.fft.rfft(padded, len(padded) + length) * numpy.fft.rfft(tail, len(padded) + length))[:len(padded)]
	dry_loudness = numpy.sqrt(numpy.mean(padded ** 2))
	return padded + wet / numpy.sqrt(numpy.mean(wet ** 2)) * dry_loudness * REVERB_LEVEL


def level(samples):
	samples = samples / numpy.sqrt(numpy.mean(samples ** 2)) * LOUDNESS
	# Kept under full scale by turning it down, not clipping: a loud peak must not crackle
	return samples * min(1, 0.95 / numpy.max(numpy.abs(samples)))


def main():
	folder = pathlib.Path(sys.argv[1])
	TARGET.mkdir(parents=True, exist_ok=True)
	for name, source, start, end, low, high in SOUNDS:
		write(name, level(fade_ends(cut(folder / source, start, end, low, high))))
	for name, source, start, end, low, high in ISOLATED:
		isolated = isolate(cut(folder / source, start, end, 60, 7000), low, high)
		write(name, level(fade_ends(reverb(isolated) if name.startswith("owl") else isolated)))
	for name, source, start, length, low, high in LOOPS:
		write(name, level(loop(folder / source, start, length, low, high)))


def write(name, samples):
	soundfile.write(TARGET / f"{name}.ogg", samples.astype(numpy.float32), RATE, format="OGG", subtype="VORBIS")


if __name__ == "__main__":
	main()
