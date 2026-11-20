#!/bin/bash
# Makes WarSteel's sounds positional. Minecraft (OpenAL) only places and attenuates MONO sounds;
# stereo files play at full volume for everyone, wherever they are. This downmixes every world
# sound to mono and builds muffled "far" and "very far" versions of each gun's report, which
# Superb Warfare plays to listeners further from the gun (Fire3PFar / Fire3PVeryFar).
# 1P (gunner's own) fire sounds are only played to the gunner and stay stereo.
# Usage: tools/localize_sounds.sh   (run from the repository root; needs ffmpeg)
set -e
D=src/main/resources/assets/warsteel/sounds
T=$(mktemp -d)
mono() {  # in out [filter]
  ffmpeg -loglevel error -y -i "$1" -ac 1 ${3:+-af "$3"} -c:a libvorbis -q:a 5 "$2"
}
for f in "$D"/*_3p_fire.ogg "$D"/*_engine.ogg "$D"/default_reload.ogg; do
  cp "$f" "$T/src.ogg"
  mono "$T/src.ogg" "$f"
done
for f in "$D"/*_3p_fire.ogg; do
  b=${f%_3p_fire.ogg}
  # Far: highs absorbed by distance, a ground echo.
  mono "$f" "${b}_3p_far.ogg" "highpass=f=50,lowpass=f=1400,aecho=0.8:0.6:180:0.35,volume=0.9"
  # Very far: a low rumble with a long rolling echo.
  mono "$f" "${b}_3p_veryfar.ogg" "lowpass=f=420,lowpass=f=420,aecho=0.8:0.7:350|900:0.4|0.25,volume=1.2"
done
rm -rf "$T"
