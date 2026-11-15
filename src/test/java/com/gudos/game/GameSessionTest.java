package com.gudos.game;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/*
 * DEFENSE OVERVIEW: GameSessionTest
 *
 * Purpose: JUnit checks for normal shift rules and stable ending records.
 *
 * Follow the flow: Drive the model directly through survival, power failure, pause, input exclusion,
 * abandon, custom scene states and invalid inputs.
 *
 * Code-based rationale and tradeoff: A custom short duration accelerates rule checks; full survival
 * still uses 360 simulated seconds.
 *
 * State to recognize: Test loops choose the correct approach pan/light when needed.
 *
 * Defense answer: The same production model can be tested headlessly because it owns rules without
 * owning Swing.
 */
class GameSessionTest {
    /*
     * DEFENSE: ordinaryShiftReachesMorningAndKeepsPower
     *
     * Simulate a full 360-second Night 1 with correct approach/light reactions; assert COMPLETED, 6
     * AM, repelled encounters and plausible conserved power.
     */
    @Test void ordinaryShiftReachesMorningAndKeepsPower() {
        GameSession game = new GameSession(1);
        while (!game.finished()) {
            game.setOfficePan(game.gudo().approach().pan());
            game.setLight(game.gudo().atOffice());
            game.update(0.1);
        }
        assertTrue(game.finished());
        assertEquals(Outcome.COMPLETED, game.result().outcome());
        assertEquals(360, game.elapsed(), 0.0001);
        assertTrue(game.power() > 60 && game.power() < 69.4);
        assertTrue(game.gudo().repelled() >= 3);
        assertEquals("6 AM", game.clockLabel());
        assertEquals("06:00 AM", game.preciseClock());
    }

    /*
     * DEFENSE: monitorCanExhaustThePowerBeforeMorning
     *
     * Use a 1000-second test duration, keep monitor up except when defending, and assert eventual
     * POWER_OUT at zero power with equipment off.
     */
    @Test void monitorCanExhaustThePowerBeforeMorning() {
        GameSession game = new GameSession(1, 1000);
        while (!game.finished()) {
            if (game.monitorOpen() == game.gudo().atOffice()) game.toggleMonitor();
            game.setOfficePan(game.gudo().approach().pan());
            game.setLight(game.gudo().atOffice());
            game.update(0.1);
        }
        assertEquals(Outcome.POWER_OUT, game.result().outcome());
        assertEquals(0, game.power(), 0.0001);
        assertTrue(game.elapsed() > 300 && game.elapsed() < 400);
        assertFalse(game.monitorOpen());
    }

    /*
     * DEFENSE: pausingStopsBothClockAndDrain
     *
     * Advance a session, pause and supply a huge delta; assert clock/power stay fixed and light is
     * released. Resume and verify timing/movement continue.
     */
    @Test void pausingStopsBothClockAndDrain() {
        GameSession game = new GameSession(2);
        game.update(8);
        game.setLight(true);
        game.pause();
        double power = game.power();
        game.update(500);
        assertEquals(8, game.elapsed(), 0.0001);
        assertEquals(power, game.power(), 0.0001);
        assertFalse(game.lightOn());
        assertFalse(game.finished());
        game.resume();
        game.update(8);
        assertEquals(16, game.elapsed(), 0.0001);
        assertEquals(Scene.CAM_04, game.gudo().location());
    }

    /*
     * DEFENSE: lightAndMonitorAreMutuallyExclusive
     *
     * Turn on light, open monitor, try light while blocked, select a camera, then close monitor and
     * verify camera shortcuts are ignored. Assert usage reflects device state.
     */
    @Test void lightAndMonitorAreMutuallyExclusive() {
        GameSession game = new GameSession(1);
        game.setLight(true);
        assertTrue(game.lightOn());
        assertEquals(3, game.usage());
        game.toggleMonitor();
        assertFalse(game.lightOn());
        game.setLight(true);
        assertFalse(game.lightOn());
        game.selectCamera(6);
        assertEquals(6, game.camera());
        game.toggleMonitor();
        game.selectCamera(2);
        assertEquals(6, game.camera(), "Closed monitor should ignore camera shortcuts");
    }

    /*
     * DEFENSE: abandonedAndCompletedResultsAreStable
     *
     * Abandon after eighteen seconds and repeatedly update/abandon/toggle; assert the same RunRecord
     * object and ABANDONED/00:18 remain.
     */
    @Test void abandonedAndCompletedResultsAreStable() {
        GameSession game = new GameSession(1);
        game.update(18);
        game.pause();
        game.abandon();
        RunRecord first = game.result();
        game.update(100);
        game.abandon();
        game.toggleMonitor();
        assertSame(first, game.result());
        assertEquals(Outcome.ABANDONED, first.outcome());
        assertEquals("00:18", first.durationLabel());
    }

    /*
     * DEFENSE: stateHooksAreIndependentAndDefaultToCleanRenders
     *
     * Set one custom camera state and verify unrelated scenes remain default. These hooks do not
     * move Gudo.
     */
    @Test void stateHooksAreIndependentAndDefaultToCleanRenders() {
        GameSession game = new GameSession(1);
        assertEquals("default", game.sceneState(Scene.CAM_03));
        game.setSceneState(Scene.CAM_03, "future-state");
        assertEquals("future-state", game.sceneState(Scene.CAM_03));
        assertEquals("default", game.sceneState(Scene.OFFICE));
    }

    /*
     * DEFENSE: invalidInputsAreRejected
     *
     * Assert invalid nights/durations/deltas/cameras and result-before-finish throw their defined
     * exceptions instead of creating silent inconsistent state.
     */
    @Test void invalidInputsAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> new GameSession(0));
        assertThrows(IllegalArgumentException.class, () -> new GameSession(6));
        assertThrows(IllegalArgumentException.class, () -> new GameSession(1, 0));
        GameSession game = new GameSession(1);
        assertThrows(IllegalArgumentException.class, () -> game.update(-1));
        assertThrows(IllegalArgumentException.class, () -> game.update(Double.NaN));
        assertThrows(IllegalArgumentException.class, () -> game.selectCamera(7));
        assertThrows(IllegalStateException.class, game::result);
    }
}
