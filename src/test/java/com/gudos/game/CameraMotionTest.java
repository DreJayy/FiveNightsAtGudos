package com.gudos.game;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/*
 * CameraMotionTest
 *
 * Purpose: JUnit checks for independent bounded automatic camera sweeps.
 *
 * Execution flow: Sample all six cameras over a shift; compare phase/speed and CAM 01 repeat after
 * 24.3 seconds; reject OFFICE.
 *
 * Design rationale: Deterministic formula inputs allow exact repeatability checks
 * without a window or a running timer.
 *
 * State: No sleeps are needed; tests supply elapsed seconds directly.
 *
 * Summary: Each camera moves in bounds and has a different phase/period.
 */
class CameraMotionTest {
    /*
     * For six feeds compare time zero versus seven seconds, then sample 360 seconds and assert the
     * pan remains near 0.04..0.96.
     */
    @Test void everyCameraSweepsAutomaticallyWithinThePanorama() {
        for (int camera = 1; camera <= 6; camera++) {
            Scene scene = Scene.camera(camera);
            assertNotEquals(CameraMotion.pan(scene, 0), CameraMotion.pan(scene, 7), 0.01);
            for (int second = 0; second < 360; second++) {
                double pan = CameraMotion.pan(scene, second);
                assertTrue(pan >= 0.039 && pan <= 0.961);
            }
        }
    }

    /*
     * Compare two camera phases, assert CAM 01 repeats after its 24.3-second period, and reject
     * trying to sweep OFFICE.
     */
    @Test void camerasHaveIndependentPhasesAndPeriods() {
        assertNotEquals(CameraMotion.pan(Scene.CAM_01, 0), CameraMotion.pan(Scene.CAM_02, 0));
        assertEquals(CameraMotion.pan(Scene.CAM_01, 0), CameraMotion.pan(Scene.CAM_01, 24.3), 0.00001);
        assertThrows(IllegalArgumentException.class, () -> CameraMotion.pan(Scene.OFFICE, 0));
    }
}
