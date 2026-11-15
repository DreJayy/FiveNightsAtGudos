package com.gudos.game;

import org.junit.jupiter.api.Test;
import java.time.Duration;
import static org.junit.jupiter.api.Assertions.*;

/*
 * DEFENSE OVERVIEW: GudoEncounterTest
 *
 * Purpose: JUnit checks for route order, defense, timing and outcome precedence.
 *
 * Follow the flow: Advance exact event times, defend/ignore/pause, compare coarse/fine updates and
 * survive all five nights with reaction delay.
 *
 * Code-based rationale and tradeoff: Headless deterministic tests catch boundary errors that a
 * single visual playthrough can miss.
 *
 * State to recognize: nextOffice and repel are test helpers, not production shortcuts.
 *
 * Defense answer: Ignoring the first Night 1 encounter catches at 60 active seconds; correct
 * directional sustained light repels.
 */
class GudoEncounterTest {
    /*
     * DEFENSE: followsConnectedRoutesAndClearsPreviousRooms
     *
     * Advance Night 1 by exact waits 18,11,11,11, assert 04->03->02->left office, repel, then verify
     * right and front cycles and previous-room cleanup.
     */
    @Test void followsConnectedRoutesAndClearsPreviousRooms() {
        GameSession game = new GameSession(1);
        game.update(18);
        assertEquals(Scene.CAM_04, game.gudo().location());
        assertEquals("occupied", game.sceneState(Scene.CAM_04));
        game.update(11);
        assertEquals(Scene.CAM_03, game.gudo().location());
        assertEquals("default", game.sceneState(Scene.CAM_04));
        game.update(11);
        assertEquals(Scene.CAM_02, game.gudo().location());
        game.update(11);
        assertEquals("left-occupied", game.sceneState(Scene.OFFICE));
        repel(game);
        assertEquals("default", game.sceneState(Scene.OFFICE));
        game.update(12);
        assertEquals(Scene.CAM_06, game.gudo().location());
        nextOffice(game);
        assertEquals("right-occupied", game.sceneState(Scene.OFFICE));
        repel(game);
        game.update(11);
        assertEquals(Scene.CAM_01, game.gudo().location());
        nextOffice(game);
        assertEquals("front-occupied", game.sceneState(Scene.OFFICE));
    }

    /*
     * DEFENSE: ignoringGudoIsFatalEvenWithALongFrameOrOpenMonitor
     *
     * For both monitor states supply a huge delta with a one-second test timeout; assert CAUGHT at
     * exactly 60 active seconds and a stable final record.
     */
    @Test void ignoringGudoIsFatalEvenWithALongFrameOrOpenMonitor() {
        for (boolean monitor : new boolean[]{false, true}) {
            GameSession game = new GameSession(1);
            if (monitor) game.toggleMonitor();
            assertTimeoutPreemptively(Duration.ofSeconds(1), () -> game.update(10000));
            assertEquals(Outcome.CAUGHT, game.result().outcome());
            assertEquals(60, game.elapsed(), 1e-8);
            assertFalse(game.monitorOpen());
            assertFalse(game.lightOn());
            RunRecord record = game.result();
            game.update(100);
            game.abandon();
            assertSame(record, game.result());
        }
    }

    /*
     * DEFENSE: flashlightMustFaceTheDoorAndFadesGradually
     *
     * Show wrong-facing light adds no fade, correct light adds partial fade, interruption decays it
     * and sufficient resumed exposure repels.
     */
    @Test void flashlightMustFaceTheDoorAndFadesGradually() {
        GameSession game = new GameSession(1);
        nextOffice(game);
        game.setOfficePan(0.9);
        game.setLight(true);
        game.update(1);
        assertEquals(0, game.gudo().fadeProgress());
        game.setOfficePan(0.1);
        game.update(0.8);
        assertEquals(0.5, game.gudo().fadeProgress(), 1e-8);
        game.setLight(false);
        game.update(0.4);
        assertTrue(game.gudo().fadeProgress() > 0 && game.gudo().fadeProgress() < 0.5);
        game.setLight(true);
        game.update(1.6);
        assertFalse(game.gudo().atOffice());
        assertNull(game.gudo().location());
        assertEquals(1, game.gudo().repelled());
        assertFalse(game.finished());
    }

    /*
     * DEFENSE: pauseFreezesAttackAndFadeButReleasesLight
     *
     * Pause partway through defense, advance a huge delta, and assert attack/fade frozen and light
     * off; after resume without light exposure decays.
     */
    @Test void pauseFreezesAttackAndFadeButReleasesLight() {
        GameSession game = new GameSession(1);
        nextOffice(game);
        game.setOfficePan(0.1);
        game.setLight(true);
        game.update(0.5);
        double remaining = game.gudo().attackRemaining();
        double fade = game.gudo().fadeProgress();
        game.pause();
        game.update(500);
        assertEquals(remaining, game.gudo().attackRemaining());
        assertEquals(fade, game.gudo().fadeProgress());
        assertFalse(game.lightOn());
        game.resume();
        game.update(0.5);
        assertTrue(game.gudo().fadeProgress() < fade);
    }

    /*
     * DEFENSE: monitorPreventsFlashlightDefense
     *
     * Open monitor during an office threat, attempt light and assert fade remains zero even though
     * the threat continues.
     */
    @Test void monitorPreventsFlashlightDefense() {
        GameSession game = new GameSession(1);
        nextOffice(game);
        game.setOfficePan(0.1);
        game.toggleMonitor();
        game.setLight(true);
        game.update(3);
        assertEquals(0, game.gudo().fadeProgress());
        assertTrue(game.gudo().atOffice());
    }

    /*
     * DEFENSE: allFiveNightsCanBeSurvivedWithReactionTimeAndConservedPower
     *
     * Simulate five full nights with a 1.8-second reaction delay before correct defense; require
     * completion, multiple repels and >50 power. Check later-night attack window is shorter.
     */
    @Test void allFiveNightsCanBeSurvivedWithReactionTimeAndConservedPower() {
        for (int night = 1; night <= 5; night++) {
            GameSession game = new GameSession(night);
            double reaction = 0;
            while (!game.finished()) {
                if (game.gudo().atOffice()) {
                    reaction += 0.05;
                    // Allow time to lower the monitor and turn toward each doorway.
                    if (reaction >= 1.8) {
                        game.setOfficePan(game.gudo().approach().pan());
                        game.setLight(true);
                    }
                } else {
                    reaction = 0;
                    game.setLight(false);
                }
                game.update(0.05);
            }
            assertEquals(Outcome.COMPLETED, game.result().outcome(), "Night " + night);
            assertTrue(game.gudo().repelled() >= 3);
            assertTrue(game.power() > 50);
        }
        assertTrue(new GameSession(5).gudo().attackSeconds() < new GameSession(1).gudo().attackSeconds());
    }

    /*
     * DEFENSE: morningTakesPriorityAtAnAttackDeadline
     *
     * Use a sixty-second Night 1 duration whose morning coincides with the first attack, then assert
     * normal COMPLETED wins the deadline tie.
     */
    @Test void morningTakesPriorityAtAnAttackDeadline() {
        GameSession game = new GameSession(1, 60);
        game.update(1000);
        assertEquals(Outcome.COMPLETED, game.result().outcome());
        assertEquals(60, game.elapsed());
    }

    /*
     * DEFENSE: simulationIsIndependentOfFrameSize
     *
     * Compare one huge update against repeated 1/60-second updates for Night 3; outcome, final time
     * and power must agree within tolerance.
     */
    @Test void simulationIsIndependentOfFrameSize() {
        GameSession coarse = new GameSession(3);
        GameSession fine = new GameSession(3);
        coarse.update(1000);
        while (!fine.finished()) fine.update(1.0 / 60);
        assertEquals(coarse.result().outcome(), fine.result().outcome());
        assertEquals(coarse.elapsed(), fine.elapsed(), 1e-7);
        assertEquals(coarse.power(), fine.power(), 1e-7);
    }

    /*
     * DEFENSE: nextOffice
     *
     * Advance small model steps until an office arrival or a bounded iteration limit; assert the
     * encounter is active and the shift is not already finished.
     */
    private static void nextOffice(GameSession game) {
        for (int i = 0; i < 3000 && !game.gudo().atOffice() && !game.finished(); i++) game.update(0.05);
        assertTrue(game.gudo().atOffice());
        assertFalse(game.finished());
    }

    /*
     * DEFENSE: repel
     *
     * Set exactly the approach pan, hold light for required flashSeconds, release it and assert the
     * office encounter ended.
     */
    private static void repel(GameSession game) {
        game.setOfficePan(game.gudo().approach().pan());
        game.setLight(true);
        game.update(game.gudo().flashSeconds());
        game.setLight(false);
        assertFalse(game.gudo().atOffice());
    }
}
