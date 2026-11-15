package com.gudos.ui;

import com.gudos.assets.AssetManager;
import com.gudos.game.GameSession;

import java.awt.*;

/** Shows the same session's paused state; Application performs resume or abandoned-shift saving. */
/*
 * DEFENSE OVERVIEW: PausePanel
 *
 * Purpose: Displays a frozen active attempt and routes Resume or Return to Menu.
 *
 * Follow the flow: Escape in gameplay -> Application.pauseShift -> PausePanel; Resume/Escape ->
 * resumeShift; Return to Menu -> leaveShift confirmation/save.
 *
 * Code-based rationale and tradeoff: Reuse the same model when resuming. Leaving creates ABANDONED,
 * so pause is different from completing an attempt.
 *
 * State to recognize: session is read for night and precise clock; the panel owns no database.
 *
 * Defense answer: Clock, drain, encounter and camera sweep stop. Resuming preserves memory; closing
 * the app does not create a checkpoint.
 */
public final class PausePanel extends BackdropPanel {
    private final GameSession session;

    /*
     * DEFENSE: PausePanel constructor
     *
     * Retain the existing GameSession for display and install Resume/Escape -> resumeShift plus
     * Return to Menu -> leaveShift. This screen does not create a new attempt.
     */
    public PausePanel(AssetManager assets, Navigation navigation, GameSession session) {
        super(assets);
        this.session = session;
        place(new GameButton("Resume", GameButton.Style.TEXT, navigation::resumeShift), 423, 350, 450, 60);
        place(new GameButton("RETURN TO MENU", GameButton.Style.SECONDARY, navigation::leaveShift), 485, 439, 315, 46);
        bind("ESCAPE", "resume", navigation::resumeShift);
    }

    /*
     * DEFENSE: render
     *
     * Draw the frozen session night/precise clock and explain paused/abandoned behavior. Resume and
     * Return to Menu callbacks were installed by the constructor.
     */
    @Override protected void render(Graphics2D g) {
        backdrop(g);
        Theme.text(g, "Paused", 530, 265, Theme.pixel(76), Color.WHITE);
        Theme.text(g, "Night %d  /  %s".formatted(session.night(), session.preciseClock()), 510, 310, Theme.pixel(28), Color.WHITE);
        Theme.text(g, "Clock, power and Sir Gudo are stopped.", 451, 535, Theme.pixel(25), Theme.MUTED);
        Theme.text(g, "Leaving records an unfinished attempt.", 451, 572, Theme.pixel(24), Theme.MUTED);
    }
}
