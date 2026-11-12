package com.gudos.audio;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import java.io.ByteArrayInputStream;

/** Synthesized 8-bit sounds: no external audio assets or libraries required. */
public final class Chiptune {
    private Chiptune() { }

    public static AudioInputStream create(Sound sound) {
        if (sound == Sound.SECRET_AMBIENCE) return ambience();
        if (sound != Sound.SECRET_JUMP && sound != Sound.SECRET_PICKUP) throw new IllegalArgumentException("Not a secret cue");
        int rate = 22050, count = (int) (rate * (sound == Sound.SECRET_JUMP ? 0.15 : 0.22));
        byte[] pcm = new byte[count];
        double phase = 0;
        for (int i = 0; i < count; i++) {
            double t = i / (double) rate;
            // Stepped pitches and a short fade keep the cues retro without sharp clicks.
            double frequency = sound == Sound.SECRET_JUMP ? 330 + 110 * (int) (t / 0.03)
                    : (t < 0.07 ? 440 : t < 0.14 ? 660 : 880);
            phase += frequency / rate;
            double envelope = Math.min(1, Math.min(i / 100.0, (count - 1 - i) / 180.0));
            pcm[i] = (byte) ((phase % 1 < 0.5 ? 1 : -1) * 55 * Math.max(0, envelope));
        }
        return new AudioInputStream(new ByteArrayInputStream(pcm), new AudioFormat(rate, 8, 1, true, false), count);
    }

    /** Slow, low-volume two-voice square-wave loop, faded at the ends to avoid a loop click. */
    private static AudioInputStream ambience() {
        int rate = 22050, count = rate * 8;
        byte[] pcm = new byte[count];
        double[] notes = {110.0, 130.81, 123.47, 98.0, 110.0, 146.83, 130.81, 98.0};
        double bassPhase = 0, melodyPhase = 0;
        for (int i = 0; i < count; i++) {
            double seconds = i / (double) rate;
            int note = Math.min(notes.length - 1, (int) seconds);
            bassPhase += 55.0 / rate;
            melodyPhase += notes[note] / rate;
            // Fade each note in and out; a separate loop fade softens the wraparound.
            double withinNote = seconds - note;
            double noteEnvelope = Math.min(1, Math.min(withinNote * 8, (1 - withinNote) * 8));
            double loopEnvelope = Math.min(1, Math.min(seconds * 4, (8 - seconds) * 4));
            double bass = bassPhase % 1 < .5 ? 1 : -1;
            double melody = melodyPhase % 1 < .25 ? 1 : -1;
            pcm[i] = (byte) ((bass * 18 + melody * 14 * Math.max(0, noteEnvelope)) * Math.max(0, loopEnvelope));
        }
        return new AudioInputStream(new ByteArrayInputStream(pcm), new AudioFormat(rate, 8, 1, true, false), count);
    }
}
