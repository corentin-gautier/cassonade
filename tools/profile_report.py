#!/usr/bin/env python3
"""Reads the Flight Recorder files of -Pprofile (build/profile/*.jfr): where the render and server threads spend their time.

For each situation, the share of the samples spent in each of the mod's methods and in the methods the
frames were executing at the top of the stack. Usage: python3 tools/profile_report.py [jfr binary] [folder]
"""
import collections
import json
import pathlib
import subprocess
import sys

JFR = sys.argv[1] if len(sys.argv) > 1 else "jfr"
FOLDER = pathlib.Path(sys.argv[2] if len(sys.argv) > 2 else "build/profile")
SHOWN = 14


def name(frame):
    method = frame["method"]
    owner = method["type"]["name"].replace("/", ".")
    return owner.rsplit(".", 1)[-1] + "." + method["name"]


def is_mod(frame):
    method = frame["method"]
    return method["type"]["name"].startswith("com/cassonade") or "cassonade$" in method["name"]


def report(path, thread="Render thread"):
    output = subprocess.run([JFR, "print", "--json", "--stack-depth", "128", "--events", "jdk.ExecutionSample", str(path)],
                            capture_output=True, check=True, text=True).stdout
    events = json.loads(output)["recording"]["events"]
    inclusive = collections.Counter()
    top = collections.Counter()
    total = 0
    for event in events:
        values = event["values"]
        if values["sampledThread"]["javaName"] != thread or not values["stackTrace"]:
            continue
        frames = values["stackTrace"]["frames"]
        total += 1
        top[name(frames[0])] += 1
        for method in {name(frame) for frame in frames if is_mod(frame)}:
            inclusive[method] += 1
    print(f"\n== {path.stem}: {total} {thread} samples")
    if total:
        print(" the mod's methods, callees included:")
        for method, count in inclusive.most_common(SHOWN):
            print(f"  {100 * count / total:5.1f}%  {method}")
        print(" hottest methods:")
        for method, count in top.most_common(8):
            print(f"  {100 * count / total:5.1f}%  {method}")


for jfr in sorted(FOLDER.glob("*.jfr")):
    report(jfr)
    report(jfr, "Server thread")
