package com.gudos.game;

import com.gudos.game.minigame.SecretGame;
import com.gudos.game.minigame.SecretLevel;
import com.gudos.game.minigame.SecretLevelBuilder;
import org.junit.jupiter.api.Test;
import java.awt.Rectangle;
import static org.junit.jupiter.api.Assertions.*;

/*
 * DEFENSE OVERVIEW: SecretGameTest
 *
 * Purpose: JUnit checks for pickup timer, delivery, hiding, patrol beams and wider-world movement.
 *
 * Follow the flow: Build small test levels; simulate before/after pickup; carry/drop/hide/deliver;
 * make each patrol independently dangerous.
 *
 * Code-based rationale and tradeoff: Small controlled configurations isolate one rule per test. They
 * do not replace the production level playability test.
 *
 * State to recognize: level helper supplies safe or dangerous patrol bounds for each scenario.
 *
 * Defense answer: Only the bundle center is detected, and dropping/hiding does not reset or pause
 * its sixty-second countdown.
 */
class SecretGameTest {
    /*
     * DEFENSE: level
     *
     * Build a small reproducible level with bundle/player at x100 and caller-chosen patrol endpoints
     * so individual rule tests can be isolated.
     */
    private SecretLevel level(double witnessLeft, double witnessRight) {
        return new SecretLevelBuilder().player(100).bundle(100)
                .destination(new Rectangle(200, 500, 100, 140))
                .witness(witnessLeft, witnessRight).timeLimit(60).build();
    }

    /*
     * DEFENSE: countdownStartsOnlyAtPickupAndContinuesAfterDropping
     *
     * Explore ten seconds without pickup and verify sixty seconds remain; pick up/drop and verify
     * countdown continues, then reaches LOST on timeout.
     */
    @Test void countdownStartsOnlyAtPickupAndContinuesAfterDropping() {
        SecretGame game = new SecretGame(level(900, 1100));
        game.update(10, false, false);
        assertFalse(game.started());
        assertEquals(60, game.remaining());
        game.action();
        game.update(1, false, false);
        game.action();
        game.update(1, false, false);
        assertEquals(58, game.remaining(), 0.001);
        assertFalse(game.carrying());
        game.update(59, false, false);
        assertEquals(SecretGame.State.LOST, game.state());
    }

    /*
     * DEFENSE: deliveryRequiresCarryingAndWins
     *
     * Pick up, walk into the test destination, press the action again and assert WON. Delivery has
     * priority over dropping.
     */
    @Test void deliveryRequiresCarryingAndWins() {
        SecretGame game = new SecretGame(level(900, 1100));
        game.action();
        game.update(.55, false, true);
        game.action();
        assertEquals(SecretGame.State.WON, game.state());
    }

    /*
     * DEFENSE: witnessSeesCarriedOrDroppedBundleUnlessItIsBehindCover
     *
     * Use a nearby dangerous patrol to lose with exposed bundle, then add cover and show a dropped
     * hidden bundle remains PLAYING.
     */
    @Test void witnessSeesCarriedOrDroppedBundleUnlessItIsBehindCover() {
        SecretGame game = new SecretGame(level(50, 70));
        game.action();
        game.update(.02, false, false);
        assertEquals(SecretGame.State.LOST, game.state());

        SecretLevel sheltered = new SecretLevelBuilder().player(100).bundle(100)
                .destination(new Rectangle(900, 500, 100, 140)).cover(new Rectangle(90, 500, 80, 140))
                .witness(50, 70).build();
        SecretGame hidden = new SecretGame(sheltered);
        hidden.action();
        hidden.action();
        hidden.update(.1, false, false);
        assertEquals(SecretGame.State.PLAYING, hidden.state());
    }

    /*
     * DEFENSE: witnessBeamIsShortAndPointsInPatrolDirection
     *
     * Test triangle points before and after the patrol turns at its right endpoint; confirm range
     * and direction match visible beam geometry.
     */
    @Test void witnessBeamIsShortAndPointsInPatrolDirection() {
        SecretGame game = new SecretGame(level(300, 400));
        assertTrue(game.witnessBeam().contains(380, 618));
        assertFalse(game.witnessBeam().contains(450, 618));
        assertFalse(game.witnessBeam().contains(250, 618));
        game.update(2, false, false); // Reach the right endpoint and turn around.
        assertFalse(game.witnessRight());
        assertTrue(game.witnessBeam().contains(game.witnessX() - 60, 618));
        assertFalse(game.witnessBeam().contains(game.witnessX() + 60, 618));
    }

    /*
     * DEFENSE: hidingWorksWhileCarryingOrAfterDroppingAndTimerContinues
     *
     * Place the bundle behind cover, verify safety while carried and dropped, then move away and
     * assert both hidden state and countdown decrease persist.
     */
    @Test void hidingWorksWhileCarryingOrAfterDroppingAndTimerContinues() {
        SecretLevel sheltered = new SecretLevelBuilder().player(100).bundle(100)
                .destination(new Rectangle(900, 500, 100, 140)).cover(new Rectangle(90, 500, 100, 140))
                .witness(50, 70).build();
        SecretGame game = new SecretGame(sheltered);
        game.action();
        assertTrue(game.bundleHidden());
        game.update(.1, false, false); // Witness faces the bundle, but the shelf hides it.
        assertEquals(SecretGame.State.PLAYING, game.state());
        game.action(); // Dropped bundle remains behind the shelf when Gudo walks away.
        game.update(.5, false, true);
        assertTrue(game.bundleHidden());
        assertEquals(SecretGame.State.PLAYING, game.state());
        assertEquals(59.4, game.remaining(), 0.001);
    }

    /*
     * DEFENSE: eachPatrolCanDetectTheBundleAndCoverProtectsAgainstAll
     *
     * Make each of three patrol slots dangerous in turn and require LOST; then overlap all three
     * near a covered bundle and require PLAYING.
     */
    @Test void eachPatrolCanDetectTheBundleAndCoverProtectsAgainstAll() {
        for (int dangerous = 0; dangerous < 3; dangerous++) {
            SecretLevelBuilder builder = new SecretLevelBuilder().player(100).bundle(100)
                    .destination(new Rectangle(900, 500, 100, 140));
            for (int i = 0; i < 3; i++) builder.witness(i == dangerous ? 50 : 800 + 100 * i, i == dangerous ? 70 : 850 + 100 * i);
            SecretGame game = new SecretGame(builder.build());
            assertEquals(3, game.witnessCount());
            game.action();
            game.update(.02, false, false);
            assertEquals(SecretGame.State.LOST, game.state(), "patrol " + dangerous);
        }
        SecretGame hidden = new SecretGame(new SecretLevelBuilder().player(100).bundle(100)
                .destination(new Rectangle(900, 500, 100, 140)).cover(new Rectangle(90, 500, 80, 140))
                .witness(40, 50).witness(50, 60).witness(60, 70).build());
        hidden.action();
        hidden.update(.02, false, false);
        assertEquals(SecretGame.State.PLAYING, hidden.state());
    }

    /*
     * DEFENSE: extendedLevelAllowsWalkingBeyondTheOriginalScreen
     *
     * Build width 2560 and walk before pickup for 7.2 seconds; assert player x passes 1400 without
     * starting countdown/detection.
     */
    @Test void extendedLevelAllowsWalkingBeyondTheOriginalScreen() {
        var level = new SecretLevelBuilder().width(2560).player(100).bundle(100)
                .destination(new Rectangle(2410, 510, 125, 130))
                .cover(new Rectangle(1400, 500, 165, 140)).witness(2100, 2200).build();
        SecretGame game = new SecretGame(level);
        game.update(7.2, false, true); // No timer or witness detection until the bundle is picked up.
        assertTrue(game.x() > 1400);
        assertTrue(game.x() < 1565);
    }
}
