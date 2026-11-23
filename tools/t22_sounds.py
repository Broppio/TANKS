#!/usr/bin/env python3
"""Synthesizes the T-22's sounds (mono Ogg Vorbis, 44.1 kHz) into assets/warsteel/sounds/t22/.

Loops are built in the frequency domain (shaped noise with random phases plus tones on whole
cycles of the loop), so they repeat without a seam:
  t22_turbine    geared steam turbines and blowers: Superb Warfare's engine sound, pitched by power
  t22_machinery  boilers, fans and pumps at idle (WarSteel plays it whenever steam is up)
  t22_wash       bow wave and wake along the hull (by speed)
  t22_whistle    steam whistle: the horn, held on Superb Warfare's decoy key
One-shots:
  t22_torpedo    compressed-air torpedo launch and splash
  t22_flood_1..3 sea rushing in through a hole
  t22_creak_1..3 hull plating straining as she floods
  t22_groan      the hull breaking / going under
Usage: python3 tools/t22_sounds.py   (needs numpy and ffmpeg)
"""
import os, subprocess, tempfile, wave
import numpy as np

SR = 44100
ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
OUT = os.path.join(ROOT, 'src/main/resources/assets/warsteel/sounds/t22')
rng = np.random.default_rng(22)


def shaped_noise(seconds, gain_fn):
    """Periodic noise whose amplitude spectrum is gain_fn(freqs)."""
    n = int(seconds * SR)
    f = np.fft.rfftfreq(n, 1 / SR)
    spec = gain_fn(np.maximum(f, 1e-3)) * np.exp(2j * np.pi * rng.random(len(f)))
    spec[0] = 0
    x = np.fft.irfft(spec, n)
    return x / (np.abs(x).max() + 1e-9)


def band(f, lo, hi, slope=2.0):
    """Smooth band-pass gain (roll-off of `slope` orders either side)."""
    return 1.0 / (1.0 + (lo / f) ** (2 * slope)) / (1.0 + (f / hi) ** (2 * slope))


def tone(seconds, freq, loop=True, phase=0.0):
    n = int(seconds * SR)
    if loop:   # a whole number of cycles over the loop
        freq = max(1, round(freq * seconds)) / seconds
    t = np.arange(n) / SR
    return np.sin(2 * np.pi * freq * t + phase)


def periodic_lfo(seconds, cycles, depth):
    t = np.arange(int(seconds * SR)) / SR
    return 1.0 + depth * np.sin(2 * np.pi * cycles / seconds * t)


def env(n, attack, release, sustain_shape=None):
    e = np.ones(n)
    a, r = int(attack * SR), int(release * SR)
    if a:
        e[:a] = np.linspace(0, 1, a) ** 2
    if r:
        e[-r:] *= np.linspace(1, 0, r) ** 1.5
    return e


def lowpass(x, cutoff):
    X = np.fft.rfft(x)
    f = np.fft.rfftfreq(len(x), 1 / SR)
    X *= 1.0 / np.sqrt(1.0 + (f / cutoff) ** 4)
    return np.fft.irfft(X, len(x))


def write(name, x, peak=0.85):
    x = x / (np.abs(x).max() + 1e-9) * peak
    pcm = (x * 32767).astype(np.int16)
    with tempfile.NamedTemporaryFile(suffix='.wav', delete=False) as tmp:
        path = tmp.name
    with wave.open(path, 'wb') as w:
        w.setnchannels(1)
        w.setsampwidth(2)
        w.setframerate(SR)
        w.writeframes(pcm.tobytes())
    out = os.path.join(OUT, name + '.ogg')
    subprocess.run(['ffmpeg', '-y', '-loglevel', 'error', '-i', path, '-c:a', 'libvorbis', '-q:a', '4', out], check=True)
    os.unlink(path)
    print('wrote', out, os.path.getsize(out), 'bytes')


os.makedirs(OUT, exist_ok=True)

# --- loops -----------------------------------------------------------------------------------
L = 6.0
# Turbines: a geared whine (blade-pass tone and its harmonics) over a rushing blower and a deep hull rumble.
rumble = shaped_noise(L, lambda f: band(f, 25, 140, 1.5) / np.sqrt(f))
blower = shaped_noise(L, lambda f: band(f, 400, 2600, 1.5))
whine = sum(a * tone(L, 610 * k) for k, a in ((1, 1.0), (2, 0.35), (3, 0.12)))
whine *= periodic_lfo(L, 3, 0.15)
gear = 0.5 * tone(L, 182) + 0.25 * tone(L, 364)
write('t22_turbine', 1.0 * rumble + 0.45 * blower + 0.22 * whine + 0.18 * gear)

# Machinery at idle: boiler roar, forced-draught fans, the thud of feed pumps.
roar = shaped_noise(L, lambda f: band(f, 40, 700, 1.2) / f ** 0.3)
fans = shaped_noise(L, lambda f: band(f, 150, 1100, 2.0)) * periodic_lfo(L, 4, 0.1)
pump_t = np.arange(int(L * SR)) / SR
pump = np.zeros_like(pump_t)
for k in range(12):   # 2 strokes a second, on the loop
    t0 = k * L / 12
    d = pump_t - t0
    m = d >= 0
    pump[m] += np.exp(-d[m] * 18) * np.sin(2 * np.pi * 55 * d[m])
write('t22_machinery', 1.0 * roar + 0.35 * fans + 0.5 * pump + 0.15 * tone(L, 120))

# Bow wave and wake: broadband hiss with slow surges as the bow lifts and falls.
L2 = 8.0
hiss = shaped_noise(L2, lambda f: band(f, 120, 5000, 1.2) / f ** 0.2)
churn = shaped_noise(L2, lambda f: band(f, 40, 400, 1.5))
surge = periodic_lfo(L2, 2, 0.35) * periodic_lfo(L2, 5, 0.12)
write('t22_wash', (0.8 * hiss + 0.6 * churn) * surge)

# --- one-shots -------------------------------------------------------------------------------
# Steam whistle (the horn, a loop held while the key is down): a breathy chord.
d = 3.0
whistle = sum(a * (tone(d, f0) + 0.4 * tone(d, 2 * f0) + 0.2 * tone(d, 3 * f0)) for f0, a in ((233, 1.0), (294, 0.8), (349, 0.6)))
breath = shaped_noise(d, lambda f: band(f, 300, 3000, 1.5))
write('t22_whistle', (whistle + 0.5 * breath) * periodic_lfo(d, 3, 0.04))

# Torpedo launch: the air impulse, a hiss as it leaves the tube, the splash.
d = 2.2
n = int(d * SR)
t = np.arange(n) / SR
thump = np.exp(-t * 22) * np.sin(2 * np.pi * 48 * t) * 1.5
hiss = shaped_noise(d, lambda f: band(f, 800, 7000, 1.5))[:n] * np.exp(-t * 5) * 0.7
splash = shaped_noise(d, lambda f: band(f, 100, 4000, 1.2))[:n]
splash *= np.where(t > 0.55, np.exp(-(t - 0.55) * 3.5) * np.minimum(1, (t - 0.55) * 30), 0)
write('t22_torpedo', thump + hiss + splash)

# Flooding: a deep rushing roar with gurgles.
for k in range(1, 4):
    d = 3.0
    n = int(d * SR)
    t = np.arange(n) / SR
    rush = shaped_noise(d, lambda f: band(f, 30, 900, 1.3) / f ** 0.25)[:n]
    gurgle = np.zeros(n)
    for _ in range(14):
        t0 = rng.uniform(0, d - 0.3)
        f0 = rng.uniform(90, 260)
        dd = t - t0
        m = (dd >= 0) & (dd < 0.25)
        gurgle[m] += np.sin(2 * np.pi * (f0 + 400 * dd[m]) * dd[m]) * np.exp(-dd[m] * 14)
    write(f't22_flood_{k}', (rush + 0.35 * gurgle) * env(n, 0.25, 0.8))

# Creaks: plating under strain, a slowly sliding, rough resonance.
for k in range(1, 4):
    d = rng.uniform(1.6, 2.6)
    n = int(d * SR)
    t = np.arange(n) / SR
    f0 = rng.uniform(55, 95)
    slide = f0 * (1 + rng.uniform(-0.35, 0.35) * t / d)
    ph = 2 * np.pi * np.cumsum(slide) / SR
    stick = 1 + 0.6 * np.sign(np.sin(2 * np.pi * rng.uniform(9, 17) * t))   # stick-slip roughness
    body = sum(np.sin(h * ph) / h ** 0.8 for h in range(1, 9)) * stick
    grit = shaped_noise(d, lambda f: band(f, 200, 2500, 1.5))[:n] * 0.25
    write(f't22_creak_{k}', lowpass(body + grit, 1800) * env(n, 0.3, 0.6))

# Groan: the hull breaking as she goes, long and low with a tearing burst.
d = 5.0
n = int(d * SR)
t = np.arange(n) / SR
slide = 42 * (1 - 0.3 * t / d)
ph = 2 * np.pi * np.cumsum(slide) / SR
body = sum(np.sin(h * ph) / h for h in range(1, 12)) * (1 + 0.5 * np.sign(np.sin(2 * np.pi * 7 * t)))
tear = shaped_noise(d, lambda f: band(f, 150, 3000, 1.2))[:n] * np.where((t > 1.8) & (t < 3.0), 1.0, 0.15)
write('t22_groan', lowpass(body + 0.6 * tear, 1500) * env(n, 0.6, 1.5))
