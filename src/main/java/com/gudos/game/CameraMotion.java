package com.gudos.game;

/** Each CCTV camera sweeps on its own clock, even while a different feed is watched. */
/*
 * DEFENSE OVERVIEW: CameraMotion
 *
 * Purpose: Computes the automatic CCTV sweep from camera identity and active shift time.
 *
 * Follow the flow: CameraSceneRenderer and the secret-paper hit test call pan(scene,elapsed).
 *
 * Code-based rationale and tradeoff: A cosine gives smooth reversal. Each camera has a different
 * phase and period. Using elapsed means unseen cameras continue their sweep and all sweeps freeze
 * during pause.
 *
 * State to recognize: period = 22 + cameraNumber * 2.3; phase = elapsed/period + cameraNumber *
 * 0.137; pan stays between 0.04 and 0.96.
 *
 * Defense answer: Office panning is player-controlled; CCTV panning is an automatic function of the
 * session clock.
 */
public final class CameraMotion {
    /*
     * DEFENSE: CameraMotion constructor
     *
     * Private constructor prevents instantiating this stateless mathematical utility.
     */
    private CameraMotion() { }

    /*
     * DEFENSE: pan
     *
     * Validate that this is a finite-time camera scene, derive period/phase from its camera number,
     * and return 0.5 - 0.46*cos(2*pi*phase). Reading session elapsed gives smooth independent sweeps
     * that pause with the night.
     */
    public static double pan(Scene scene, double elapsedSeconds) {
        if (scene == null || scene == Scene.OFFICE || !Double.isFinite(elapsedSeconds)) throw new IllegalArgumentException("Invalid CCTV camera");
        // Camera number gives each feed a different sweep speed and starting phase.
        // Using session elapsed time makes sweeps pause with gameplay and continue off-screen.
        double period = 22 + scene.camera() * 2.3;
        double phase = elapsedSeconds / period + scene.camera() * 0.137;
        // Cosine produces a smooth left-right cycle in the normalized range 0.04-0.96.
        return 0.5 - 0.46 * Math.cos(phase * Math.PI * 2);
    }
}
