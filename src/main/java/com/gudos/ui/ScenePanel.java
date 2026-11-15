package com.gudos.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Point2D;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;

/** A letterboxed 1280×720 Java2D scene with real, proportionally placed Swing controls. */
/*
 * DEFENSE OVERVIEW: ScenePanel
 *
 * Purpose: Provides a common scaled scene, real Swing component layout and keyboard routing.
 *
 * Follow the flow: Subclasses place controls in 1280x720 coordinates and implement render; Swing
 * invokes paintComponent/paintChildren/doLayout.
 *
 * Code-based rationale and tradeoff: Uniform scale and centered offsets preserve aspect ratio.
 * Reverse transforms preserve input accuracy. The final paint skeleton is a Template Method style
 * design with subclass hooks.
 *
 * State to recognize: Slot preserves original bounds/font to avoid cumulative resizing errors. age
 * measures panel animation time and differs from GameSession.elapsed.
 *
 * Defense answer: The room is drawn first, then Swing children, then an optional overlay. ScenePanel
 * shares mechanics; each screen supplies its content.
 */
public abstract class ScenePanel extends JPanel {
    public static final int WIDTH = 1280;
    public static final int HEIGHT = 720;
    // Preserve original design bounds/font sizes so window resizing does not accumulate scaling errors.
    /*
     * DEFENSE TYPE: Slot
     *
     * Original logical control bounds and font retained for correct repeated resizing.
     */
    private record Slot(JComponent component, Rectangle bounds, Font font) { }
    private final List<Slot> slots = new ArrayList<>();
    // Time spent updating this panel; animations use it independently of GameSession's shift clock.
    protected double age;

    /*
     * DEFENSE: ScenePanel constructor
     *
     * Initialize a focusable black 1280x720 logical scene with null layout. place/doLayout implement
     * its intentional proportional layout for real Swing children.
     */
    protected ScenePanel() {
        setLayout(null);
        setBackground(Theme.BACKGROUND);
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setFocusable(true);
    }

    // Register a real Swing control using coordinates in the fixed 1280x720 design space.
    /*
     * DEFENSE: place
     *
     * Remember the control, its original logical bounds and font; add it as a real Swing child and
     * return it. doLayout can recalculate from the original design on every resize without
     * accumulating scale error.
     */
    protected <T extends JComponent> T place(T component, int x, int y, int w, int h) {
        slots.add(new Slot(component, new Rectangle(x, y, w, h), component.getFont()));
        add(component);
        return component;
    }

    // Uniform scaling preserves aspect ratio; offsets center the scene inside any unused borders.
    /*
     * DEFENSE: sceneScale
     *
     * Use the smaller of physical-width/1280 and physical-height/720. Uniform scaling keeps the
     * scene aspect ratio; extra space becomes letterboxing.
     */
    public double sceneScale() { return Math.min(getWidth() / (double) WIDTH, getHeight() / (double) HEIGHT); }
    /*
     * DEFENSE: offsetX
     *
     * Center the scaled logical width inside the physical component. This offset must also be
     * subtracted from mouse coordinates.
     */
    public int offsetX() { return (int) Math.round((getWidth() - WIDTH * sceneScale()) / 2); }
    /*
     * DEFENSE: offsetY
     *
     * Center the scaled logical height inside the physical component. Rendering, Swing controls and
     * reverse input all use the same transform.
     */
    public int offsetY() { return (int) Math.round((getHeight() - HEIGHT * sceneScale()) / 2); }

    // Reverse the rendering transform so mouse input remains accurate after a window resize.
    /*
     * DEFENSE: scenePoint
     *
     * Reverse letterbox offset and uniform scale to turn window-local pixels into logical scene
     * coordinates. If there is no positive size, return a neutral point instead of dividing by zero.
     */
    public Point2D scenePoint(Point point) {
        double scale = sceneScale();
        return scale <= 0 ? new Point2D.Double() : new Point2D.Double((point.x - offsetX()) / scale, (point.y - offsetY()) / scale);
    }

    // Apply the same scene transform to actual Swing button/input bounds and font sizes.
    /*
     * DEFENSE: doLayout
     *
     * For every original Slot, apply current scene scale/offset to bounds and derive its font size
     * from the stored original. This keeps actual Swing controls aligned with Java2D artwork.
     */
    @Override public void doLayout() {
        double scale = sceneScale();
        for (Slot slot : slots) {
            Rectangle b = slot.bounds();
            slot.component().setBounds(offsetX() + (int) Math.round(b.x * scale), offsetY() + (int) Math.round(b.y * scale),
                    (int) Math.round(b.width * scale), (int) Math.round(b.height * scale));
            if (slot.font() != null) slot.component().setFont(slot.font().deriveFont((float) Math.max(9, slot.font().getSize2D() * scale)));
        }
    }

    // All screens share this setup/cleanup; subclasses implement only render() for their content.
    // A copied graphics context isolates transforms and colors from Swing's original context.
    /*
     * DEFENSE: paintComponent
     *
     * Template Method skeleton: clear background through Swing, copy Graphics2D, apply
     * quality/translation/scale/clip, call subclass render, then dispose the copy. final keeps
     * subclasses from bypassing the common scene transform.
     */
    @Override protected final void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        if (sceneScale() <= 0) return;
        Graphics2D g = (Graphics2D) graphics.create();
        Theme.quality(g);
        g.translate(offsetX(), offsetY());
        g.scale(sceneScale(), sceneScale());
        g.clipRect(0, 0, WIDTH, HEIGHT);
        render(g);
        g.dispose();
    }

    /*
     * DEFENSE: render
     *
     * Subclass hook for logical 1280x720 background/content drawing. ScenePanel owns transform setup
     * and disposal; subclasses supply only what the screen looks like.
     */
    protected abstract void render(Graphics2D g);

    // Swing controls paint after the background; the optional overlay paints above those controls.
    /*
     * DEFENSE: paintChildren
     *
     * Let Swing paint actual controls, then create a transformed graphics copy and invoke
     * renderOverlay. This permits static/intro effects to cover controls after they are drawn.
     */
    @Override protected void paintChildren(Graphics graphics) {
        super.paintChildren(graphics);
        if (sceneScale() <= 0) return;
        Graphics2D g = (Graphics2D) graphics.create();
        Theme.quality(g);
        g.translate(offsetX(), offsetY());
        g.scale(sceneScale(), sceneScale());
        g.clipRect(0, 0, WIDTH, HEIGHT);
        renderOverlay(g);
        g.dispose();
    }

    /*
     * DEFENSE: renderOverlay
     *
     * Optional hook after children are painted. The base implementation is deliberately empty;
     * MenuPanel/GamePanel supply their own visual overlay.
     */
    protected void renderOverlay(Graphics2D g) { }

    /*
     * DEFENSE: tick
     *
     * Add seconds to the panel age used by visual animation. This age is separate from the
     * simulation elapsed clock, so decorative animation and rule timing are not confused.
     */
    public void tick(double seconds) { age += seconds; }

    /** Dispatch before Swing focus traversal/default-button bindings swallow game keys. */
    /*
     * DEFENSE: dispatchKey
     *
     * Only dispatch while showing. Resolve the exact KeyStroke from the focused-window InputMap and
     * corresponding enabled Action, invoke it, and report handled. Application calls this before
     * Swing defaults consume game keys.
     */
    public boolean dispatchKey(KeyEvent event) {
        if (!isShowing()) return false;
        Object name = getInputMap(WHEN_IN_FOCUSED_WINDOW).get(KeyStroke.getKeyStrokeForEvent(event));
        Action action = name == null ? null : getActionMap().get(name);
        if (action == null || !action.isEnabled()) return false;
        action.actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, name.toString()));
        return true;
    }

    // Map a keystroke to a named Swing action that runs the supplied callback.
    /*
     * DEFENSE: bind
     *
     * Associate a KeyStroke with a named Action in WHEN_IN_FOCUSED_WINDOW and store an
     * AbstractAction that runs the supplied Runnable. Press/release bindings allow held-state
     * controls.
     */
    protected void bind(String stroke, String name, Runnable action) {
        getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(stroke), name);
        getActionMap().put(name, new AbstractAction() {
            /*
             * DEFENSE: actionPerformed
             *
             * Run the Runnable associated with this named scene key binding. The callback owns the
             * action; the shared binding adapter contains no game rule.
             */
            @Override public void actionPerformed(java.awt.event.ActionEvent event) {
                action.run();
            }
        });
    }
}
