package com.gudos.ui.rendering;

import java.awt.Graphics2D;

/** Component shared by plain renderers and decorators, in the 1280x720 scene space. */
/*
 * DEFENSE OVERVIEW: SceneRenderer
 *
 * Purpose: Defines the component interface shared by a room renderer and all custom renderer
 * decorators.
 *
 * Follow the flow: GamePanel stores composed implementations through SceneRenderer and calls
 * render(graphics,time,reducedEffects).
 *
 * Code-based rationale and tradeoff: The same interface allows nested wrappers and substitutability.
 * Without it, effect layers would need to know each concrete renderer type.
 *
 * State to recognize: The contract draws in ScenePanel 1280x720 coordinates; time is animation
 * context and reducedEffects controls optional interference.
 *
 * Defense answer: Component = SceneRenderer; concrete component = CameraSceneRenderer; wrappers =
 * RendererDecorator subclasses.
 */
public interface SceneRenderer {
    /*
     * DEFENSE: render
     *
     * Draw the component in 1280x720 logical coordinates for the supplied animation time and
     * reduced-effects setting. Concrete room and wrapper classes must obey this same callable
     * contract.
     */
    void render(Graphics2D graphics, double time, boolean reducedEffects);
}
