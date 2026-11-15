package com.gudos.ui;

import java.awt.*;

/** Animated waiting screen; the calling worker performs loading and decides when to leave it. */
/*
 * LoadingPanel
 *
 * Purpose: Draws a waiting message and animated dots.
 *
 * Execution flow: Main uses it during boot; Application uses it for asynchronous records/progress
 * reads.
 *
 * Design rationale: The panel does not perform slow work. The worker and
 * coordinator decide when loading is complete.
 *
 * State: message is supplied by the caller; age chooses one to three dots.
 *
 * Summary: Animated loading is evidence that the EDT can repaint while another thread does
 * IO.
 */
public final class LoadingPanel extends ScenePanel {
    private final String message;

    /*
     * Retain the human loading message. Rendering animates dots while another component owns actual
     * work.
     */
    public LoadingPanel(String message) { this.message = message; }

    /*
     * Fill black, animate one-to-three dots from panel age and center the supplied message. It never
     * starts IO; the caller owns the background task.
     */
    @Override protected void render(Graphics2D g) {
        g.setColor(Theme.BACKGROUND);
        g.fillRect(0, 0, WIDTH, HEIGHT);
        String label = "Loading" + ".".repeat(1 + (int) age % 3);
        Theme.text(g, label, 542, 341, Theme.pixel(52), Color.WHITE);
        g.setFont(Theme.pixel(25));
        Theme.text(g, message, (WIDTH - g.getFontMetrics().stringWidth(message)) / 2, 395, Theme.pixel(25), Theme.MUTED);
    }
}
