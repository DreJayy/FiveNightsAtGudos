package com.gudos.game;

import org.junit.jupiter.api.Test;
import java.awt.Rectangle;
import static org.junit.jupiter.api.Assertions.*;

class SecretGameTest {
    private SecretLevel level(double witnessLeft, double witnessRight) {
        return new SecretLevelBuilder().player(100).bundle(100)
                .destination(new Rectangle(200, 500, 100, 140))
                .witness(witnessLeft, witnessRight).timeLimit(60).build();
    }

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

    @Test void deliveryRequiresCarryingAndWins() {
        SecretGame game = new SecretGame(level(900, 1100));
        game.action();
        game.update(.55, false, true);
        game.action();
        assertEquals(SecretGame.State.WON, game.state());
    }

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
}
