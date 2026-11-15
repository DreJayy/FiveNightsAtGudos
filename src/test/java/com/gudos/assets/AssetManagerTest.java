package com.gudos.assets;

import com.gudos.audio.Sound;
import com.gudos.game.Scene;
import com.gudos.ui.Theme;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javax.sound.sampled.AudioSystem;
import java.io.BufferedInputStream;

import static org.junit.jupiter.api.Assertions.*;

/*
 * DEFENSE OVERVIEW: AssetManagerTest
 *
 * Purpose: JUnit checks for bundled scenes, state fallbacks, active audio, static and font.
 *
 * Follow the flow: Construct AssetManager once; verify dimensions/distinct states; decode every
 * Sound without a mixer; inspect PCM, duration and attribution.
 *
 * Code-based rationale and tradeoff: Asset decoding can be tested without speakers. These tests
 * prove packaging/decoding, not perceived volume on every machine.
 *
 * State to recognize: Uses before-all cached assets. Source WAV excerpts are checked for a
 * nontrivial peak; garble/digital clips are two seconds.
 *
 * Defense answer: All active resources referenced by code should decode and required occupied images
 * should differ from clean ones.
 */
class AssetManagerTest {
    private static AssetManager assets;
    /*
     * DEFENSE: load
     *
     * Construct one AssetManager before this suite so all tests inspect the same decoded resource
     * set.
     */
    @BeforeAll static void load() throws Exception { assets = new AssetManager(); }

    /*
     * DEFENSE: allSevenBundledScenesLoadAtOriginalResolution
     *
     * Loop through OFFICE and six cameras and assert each default image is 1916x821. This detects
     * missing or unexpectedly resized clean assets.
     */
    @Test void allSevenBundledScenesLoadAtOriginalResolution() {
        for (Scene scene : Scene.values()) {
            assertEquals(1916, assets.scene(scene, "default").getWidth());
            assertEquals(821, assets.scene(scene, "default").getHeight());
        }
    }

    /*
     * DEFENSE: futureUnmappedStatesFallbackToCleanRoom
     *
     * Request an unmapped camera state and assert it is the exact same clean image object. This
     * proves the intended fallback contract.
     */
    @Test void futureUnmappedStatesFallbackToCleanRoom() {
        assertSame(assets.scene(Scene.CAM_03, "default"), assets.scene(Scene.CAM_03, "future-state"));
    }

    /*
     * DEFENSE: everyEnemyRenderIsBundledAndDistinctFromTheCleanRoom
     *
     * Check all six occupied camera images and all directional/jumpscare office states are different
     * objects from clean. Also verify occupied camera aspect ratio and minimum width.
     */
    @Test void everyEnemyRenderIsBundledAndDistinctFromTheCleanRoom() {
        for (Scene scene : Scene.values()) {
            if (scene == Scene.OFFICE) continue;
            var occupied = assets.scene(scene, "occupied");
            assertNotSame(assets.scene(scene, "default"), occupied);
            assertTrue(occupied.getWidth() > 1800);
            assertEquals(21.0 / 9, occupied.getWidth() / (double) occupied.getHeight(), 0.02);
        }
        for (String state : new String[]{"left-occupied", "front-occupied", "right-occupied", "jumpscare"}) {
            assertNotSame(assets.scene(Scene.OFFICE, "default"), assets.scene(Scene.OFFICE, state), state);
        }
    }

    /*
     * DEFENSE: everyActiveWavCanBeDecodedWithoutAudioHardware
     *
     * Iterate every Sound, decode a resource or generate a secret cue, and assert nonempty audio
     * with a positive sample rate. No Clip/mixer is required, so this is not a speaker test.
     */
    @Test void everyActiveWavCanBeDecodedWithoutAudioHardware() throws Exception {
        for (Sound sound : Sound.values()) {
            try (var stream = sound.path() == null ? com.gudos.audio.Chiptune.create(sound) : AudioSystem.getAudioInputStream(new BufferedInputStream(AssetManager.resource(sound.path())))) {
                assertTrue(stream.getFrameLength() > 0, sound.name());
                assertTrue(stream.getFormat().getSampleRate() > 0, sound.name());
            }
        }
    }

    /*
     * DEFENSE: importantSourceCuesContainAudiblePcmAndInterferenceIsBoundedToTwoSeconds
     *
     * Verify active source cues use expected mono 16-bit little-endian PCM, bounded durations and a
     * peak>=4000; assert garble/digital durations equal two seconds and scare fits its transition.
     */
    @Test void importantSourceCuesContainAudiblePcmAndInterferenceIsBoundedToTwoSeconds() throws Exception {
        for (Sound sound : new Sound[]{Sound.FLASHLIGHT_ON, Sound.FLASHLIGHT_OFF, Sound.GUDO_STEP,
                Sound.GUDO_WARNING, Sound.GUDO_POUND, Sound.GUDO_WHISPER, Sound.GARBLE_1,
                Sound.GARBLE_2, Sound.GARBLE_3, Sound.CAMERA_DIGITAL, Sound.GUDO_RUSH,
                Sound.GUDO_RETREAT, Sound.JUMPSCARE}) {
            try (var stream = AudioSystem.getAudioInputStream(new BufferedInputStream(AssetManager.resource(sound.path())))) {
                var format = stream.getFormat();
                assertEquals(16, format.getSampleSizeInBits(), sound.name());
                assertEquals(1, format.getChannels(), sound.name());
                assertFalse(format.isBigEndian(), sound.name());
                double duration = stream.getFrameLength() / format.getFrameRate();
                assertTrue(duration > 0.05 && duration <= (sound == Sound.GUDO_STEP ? 8 : 2.81), sound.name());
                if (sound == Sound.GARBLE_1 || sound == Sound.GARBLE_2 || sound == Sound.GARBLE_3
                        || sound == Sound.CAMERA_DIGITAL) assertEquals(2, duration, 0.001, sound.name());
                if (sound == Sound.JUMPSCARE) assertTrue(duration <= com.gudos.ui.JumpscarePanel.DURATION);
                byte[] pcm = stream.readAllBytes();
                int peak = 0;
                for (int sample = 0; sample + 1 < pcm.length; sample += 2) {
                    int value = (short) ((pcm[sample] & 0xff) | (pcm[sample + 1] << 8));
                    peak = Math.max(peak, Math.abs(value));
                }
                assertTrue(peak >= 4000, sound.name() + " must not be a silent or near-silent extract");
            }
        }
    }

    /*
     * DEFENSE: secretAmbienceIsAnEightSecondSeamlessSynthesizedLoop
     *
     * Generate secret ambience and check eight seconds of samples, zero-valued endpoints and at
     * least one audible sample. This checks the designed loop seam at sample endpoints.
     */
    @Test void secretAmbienceIsAnEightSecondSeamlessSynthesizedLoop() throws Exception {
        try (var stream = com.gudos.audio.Chiptune.create(Sound.SECRET_AMBIENCE)) {
            assertEquals((long) stream.getFormat().getSampleRate() * 8, stream.getFrameLength());
            byte[] samples = stream.readAllBytes();
            assertEquals(0, samples[0]);
            assertEquals(0, samples[samples.length - 1]);
            boolean audible = false;
            for (byte sample : samples) if (sample != 0) { audible = true; break; }
            assertTrue(audible);
        }
    }

    /*
     * DEFENSE: downloadedStaticIsAnimatedAndPixelFontIsBundled
     *
     * Assert five distinct noise frames, expected GIF frame dimensions, VT323 font family and
     * preserved attribution text.
     */
    @Test void downloadedStaticIsAnimatedAndPixelFontIsBundled() throws Exception {
        assertEquals(5, assets.noiseFrameCount());
        assertEquals(640, assets.noise(0).getWidth());
        assertEquals(361, assets.noise(0).getHeight());
        assertNotSame(assets.noise(0), assets.noise(1));
        assertTrue(Theme.pixel(32).getFamily().contains("VT323"));
        try (var credits = AssetManager.resource("/licenses/asset-credits.txt")) {
            assertTrue(new String(credits.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8).contains("GravelTravel22"));
        }
    }
}
