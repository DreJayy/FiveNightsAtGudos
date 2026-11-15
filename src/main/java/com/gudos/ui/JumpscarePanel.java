package com.gudos.ui;

import com.gudos.assets.AssetManager;
import com.gudos.game.Scene;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.ConvolveOp;
import java.awt.image.Kernel;

/** A generated still animated with a lunge, shake and cached directional blur. */
/*
 * DEFENSE OVERVIEW: JumpscarePanel
 *
 * Purpose: Animates the caught still and calls back once after 2.1 seconds.
 *
 * Follow the flow: Application.finishShift on CAUGHT starts sound/save and shows this; completion
 * opens the prepared ResultPanel.
 *
 * Code-based rationale and tradeoff: Precompute a horizontal blur once. Zoom/shake reuse the still;
 * reducedEffects keeps it stationary. Guard delivered avoids repeated navigation.
 *
 * State to recognize: DURATION=2.1; still/blurred are cached images; age drives zoom/shake/final
 * fade; delivered gates callback.
 *
 * Defense answer: The scare is a visual transition after the simulation has already ended. Database
 * saving starts during it.
 */
public final class JumpscarePanel extends ScenePanel {
    public static final double DURATION = 2.1;
    private final BufferedImage still;
    private final BufferedImage blurred;
    private final boolean reducedEffects;
    private final Runnable complete;
    private boolean delivered;

    /*
     * DEFENSE: JumpscarePanel constructor
     *
     * Fetch the jumpscare image, prepare a viewport-sized copy and precompute the nine-sample
     * horizontal convolution blur. Store reduced-effects and exactly-once completion callback; do
     * not advance/save the model here.
     */
    public JumpscarePanel(AssetManager assets, boolean reducedEffects, Runnable complete) {
        this.reducedEffects = reducedEffects;
        this.complete = complete;
        still = assets.scene(Scene.OFFICE, "jumpscare");
        BufferedImage frame = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = frame.createGraphics();
        g.drawImage(still, 0, 0, WIDTH, HEIGHT, null);
        g.dispose();
        // Precompute horizontal blur once; rendering reuses it instead of filtering every frame.
        float[] streak = {0.06f, 0.09f, 0.12f, 0.15f, 0.16f, 0.15f, 0.12f, 0.09f, 0.06f};
        blurred = new ConvolveOp(new Kernel(9, 1, streak), ConvolveOp.EDGE_NO_OP, null).filter(frame, null);
    }

    // Deliver the completion callback exactly once, letting Application show the result screen.
    /*
     * DEFENSE: tick
     *
     * Advance age and, after 2.1 seconds, set delivered before invoking the completion callback.
     * Even later ticks cannot show the result twice.
     */
    @Override public void tick(double seconds) {
        super.tick(seconds);
        if (age >= DURATION && !delivered) {
            delivered = true;
            complete.run();
        }
    }

    /*
     * DEFENSE: render
     *
     * Draw the cached still with zoom/shake and optional cached blur; reducedEffects uses a
     * stationary image. Fade to black in the final 0.35 seconds. This transition happens after the
     * model outcome is already CAUGHT.
     */
    @Override protected void render(Graphics2D g) {
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, WIDTH, HEIGHT);
        // Animate one still with zoom/shake; reduced effects keeps the image stationary.
        double zoom = reducedEffects ? 1 : 1.06 + Math.min(0.14, age * 0.18);
        int w = (int) Math.ceil(WIDTH * zoom);
        int h = (int) Math.ceil(HEIGHT * zoom);
        int shakeX = reducedEffects ? 0 : (int) (Math.sin(age * 109) * 18 + Math.cos(age * 73) * 8);
        int shakeY = reducedEffects ? 0 : (int) (Math.cos(age * 97) * 13);
        int x = (WIDTH - w) / 2 + shakeX;
        int y = (HEIGHT - h) / 2 + shakeY;
        g.drawImage(still, x, y, w, h, null);
        if (!reducedEffects) {
            g.setComposite(AlphaComposite.SrcOver.derive(0.42f));
            g.drawImage(blurred, x + (int) (Math.sin(age * 89) * 18), y, w, h, null);
            g.setComposite(AlphaComposite.SrcOver);
        }
        // A single fade to black, never a flashing sequence.
        if (age > DURATION - 0.35) {
            g.setColor(new Color(0, 0, 0, (int) (255 * Math.min(1, (age - DURATION + 0.35) / 0.35))));
            g.fillRect(0, 0, WIDTH, HEIGHT);
        }
    }
}
