"""The Minecraft client jar, to read the game's own textures from."""

import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent


def loom_jar():
	"""The client jar Loom downloaded for the build."""
	jars = sorted(p for p in (ROOT / ".gradle/loom-cache/minecraftMaven/net/minecraft").glob("minecraft-clientOnly-*/*/*.jar")
		if not p.name.endswith("-sources.jar"))
	if not jars:
		sys.exit("No Minecraft jar found: run ./gradlew build once, or pass the jar's path.")
	return jars[-1]
