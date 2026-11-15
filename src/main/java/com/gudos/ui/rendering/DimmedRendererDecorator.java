package com.gudos.ui.rendering;

import com.gudos.ui.ScenePanel;

import java.awt.Color;
import java.awt.Graphics2D;

/** Adds a black tint after rendering the wrapped component. */
/*
 * DEFENSE OVERVIEW: DimmedRendererDecorator
 *
 * Purpose: Adds a translucent black layer after its wrapped renderer draws.
 *
 * Follow the flow: GamePanel puts it around the normal camera chain for switching interference.
 *
 * Code-based rationale and tradeoff: Validate alpha=0-255, delegate first, and draw using a copied
 * graphics context. Layer order changes pixels.
 *
 * State to recognize: shade holds black with configured alpha. It dims when called even with
 * reducedEffects; GamePanel bypasses the switching chain in that mode.
 *
 * Defense answer: It is a decorator because it accepts and remains a SceneRenderer, delegates, then
 * adds darkness.
 */
public final class DimmedRendererDecorator extends RendererDecorator {
    private final Color shade;

    /*
     * DEFENSE: DimmedRendererDecorator constructor
     *
     * Pass the wrapped component to the base, reject alpha outside0..255 and create the black tint.
     * The layer is applied only when render is called.
     */
    public DimmedRendererDecorator(SceneRenderer wrapped, int alpha) {
        super(wrapped);
        if (alpha < 0 || alpha > 255) throw new IllegalArgumentException("Alpha must be 0–255");
        shade = new Color(0, 0, 0, alpha);
    }

    /*
     * DEFENSE: render
     *
     * Render the wrapped component first, then fill a copied Graphics2D with the configured
     * translucent black shade and dispose it in finally. It always dims if called; reduced switching
     * is bypassed by GamePanel.
     */
    @Override public void render(Graphics2D graphics, double time, boolean reducedEffects) {
        super.render(graphics, time, reducedEffects);
        Graphics2D effect = (Graphics2D) graphics.create();
        try {
            effect.setColor(shade);
            effect.fillRect(0, 0, ScenePanel.WIDTH, ScenePanel.HEIGHT);
        } finally {
            effect.dispose();
        }
    }
}
