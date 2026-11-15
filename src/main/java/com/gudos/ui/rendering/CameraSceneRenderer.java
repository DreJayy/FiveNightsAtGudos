package com.gudos.ui.rendering;

import com.gudos.assets.AssetManager;
import com.gudos.assets.PanoramaRenderer;
import com.gudos.game.CameraMotion;
import com.gudos.game.GameSession;
import com.gudos.game.Scene;
import com.gudos.ui.ScenePanel;
import com.gudos.ui.Theme;

import java.awt.Color;
import java.awt.Graphics2D;
import java.util.Objects;

/** Concrete component: the current CCTV room, without animated interference or HUD. */
/*
 * DEFENSE OVERVIEW: CameraSceneRenderer
 *
 * Purpose: Draws the current CCTV room without VHS or switching effects.
 *
 * Follow the flow: GamePanel puts it inside VhsRendererDecorator; render reads selected camera, room
 * state and sweep from the live session.
 *
 * Code-based rationale and tradeoff: Composition keeps one core room renderer separate from optional
 * visual layers. The object is created once and reads updated model values each frame.
 *
 * State to recognize: session and assets are required; projection is a reusable PanoramaRenderer. It
 * applies the base shade and vignette after room projection.
 *
 * Defense answer: This is the concrete component in Decorator, not a decorator itself.
 */
public final class CameraSceneRenderer implements SceneRenderer {
    private final GameSession session;
    private final AssetManager assets;
    private final PanoramaRenderer projection = new PanoramaRenderer();

    /*
     * DEFENSE: CameraSceneRenderer constructor
     *
     * Require nonnull GameSession and AssetManager, then retain them. Its projection buffer is owned
     * by this concrete component and reused per frame.
     */
    public CameraSceneRenderer(GameSession session, AssetManager assets) {
        this.session = Objects.requireNonNull(session);
        this.assets = Objects.requireNonNull(assets);
    }

    /*
     * DEFENSE: render
     *
     * Read current camera and scene-state key from GameSession, compute sweep from session elapsed,
     * project the corresponding cached image and draw it at logical viewport size. Apply the room
     * shade/vignette, leaving VHS/switch effects to wrappers.
     */
    @Override public void render(Graphics2D graphics, double time, boolean reducedEffects) {
        Scene scene = Scene.camera(session.camera());
        double sweep = CameraMotion.pan(scene, session.elapsed());
        graphics.drawImage(projection.render(assets.scene(scene, session.sceneState(scene)), sweep),
                0, 0, ScenePanel.WIDTH, ScenePanel.HEIGHT, null);
        graphics.setColor(new Color(0, 0, 0, 32));
        graphics.fillRect(0, 0, ScenePanel.WIDTH, ScenePanel.HEIGHT);
        Theme.vignette(graphics, 205);
    }
}
