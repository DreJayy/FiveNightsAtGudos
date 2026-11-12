package com.gudos.ui;

import com.gudos.game.SecretGame;
import java.awt.*;
import java.util.function.Consumer;

/** Java2D pixel-art presentation of the hidden platformer. */
public final class SecretPanel extends ScenePanel {
    private final SecretGame game;
    private final Consumer<Boolean> finished;
    private boolean left, right, jump, action, paused, reported;

    public SecretPanel(SecretGame game, Consumer<Boolean> finished) {
        this.game = game; this.finished = finished;
        bind("pressed A", "left", () -> left = true); bind("released A", "left-up", () -> left = false);
        bind("pressed LEFT", "arrow-left", () -> left = true); bind("released LEFT", "arrow-left-up", () -> left = false);
        bind("pressed D", "right", () -> right = true); bind("released D", "right-up", () -> right = false);
        bind("pressed RIGHT", "arrow-right", () -> right = true); bind("released RIGHT", "arrow-right-up", () -> right = false);
        bind("pressed SPACE", "jump", () -> { if (!jump && !paused) game.jump(); jump = true; });
        bind("released SPACE", "jump-up", () -> jump = false);
        bind("pressed E", "action", () -> { if (!action && !paused) game.action(); action = true; });
        bind("released E", "action-up", () -> action = false);
        bind("pressed ESCAPE", "pause", () -> { paused = !paused; release(); });
    }
    public void pause() { paused = true; release(); }
    private void release() { left = right = jump = action = false; }
    @Override public void tick(double seconds) {
        super.tick(seconds);
        if (!paused) game.update(seconds, left, right);
        if (!reported && game.state() != SecretGame.State.PLAYING) {
            reported = true; finished.accept(game.state() == SecretGame.State.WON);
        }
    }
    @Override protected void render(Graphics2D g) {
        g.setColor(new Color(23, 21, 25)); g.fillRect(0, 0, WIDTH, HEIGHT);
        g.setColor(new Color(52, 45, 40)); g.fillRect(0, 180, WIDTH, 460);
        for (int i = 0; i < WIDTH; i += 80) { g.setColor(new Color(61, 53, 47)); g.drawRect(i, 180, 80, 460); }
        g.setColor(new Color(95, 84, 69)); g.fillRect(0, 640, WIDTH, 80);
        g.setColor(new Color(189, 178, 142)); g.fillRect(105, 225, 90, 110);
        Theme.text(g, "MISSING", 110, 250, Theme.pixel(22), Color.BLACK);
        Theme.text(g, "LAST SEEN", 110, 290, Theme.pixel(18), Color.BLACK);
        Theme.text(g, "AFTER HOURS", 110, 315, Theme.pixel(17), Color.BLACK);
        g.setColor(new Color(48, 37, 34)); g.fill(game.level().destination());
        Theme.text(g, "STOCKROOM", 1080, 510, Theme.pixel(25), Theme.PAPER);
        for (Rectangle p : game.level().platforms()) { g.setColor(new Color(126, 100, 71)); g.fill(p); }
        for (Rectangle c : game.level().cover()) {
            g.setColor(new Color(79, 64, 51)); g.fill(c);
            g.setColor(new Color(137, 111, 79)); g.draw(c);
            Theme.text(g, "SHELF", c.x + 8, c.y + 25, Theme.pixel(20), Theme.PAPER);
        }
        int wx = (int) game.witnessX();
        g.setColor(new Color(250, 220, 120, 45));
        int direction = game.witnessRight() ? 1 : -1;
        // Match the highlighted area to the simulation's actual witness detection region.
        g.fillRect(direction > 0 ? wx : wx - 200, 500, 200, 140);
        pixelPerson(g, wx, 570, new Color(120, 149, 150));
        pixelPerson(g, (int) game.x(), (int) game.y(), new Color(160, 75, 64));
        int bx = game.carrying() ? (int) game.x() + 20 : (int) game.bundleX();
        int by = game.carrying() ? (int) game.y() + 42 : (int) game.bundleY();
        g.setColor(new Color(202, 191, 164)); g.fillRect(bx, by, 32, 20);
        g.setColor(new Color(105, 85, 76)); g.drawLine(bx + 4, by + 9, bx + 28, by + 9);
        Theme.text(g, "SIR GUDO  /  AFTER HOURS", 30, 48, Theme.pixel(34), Theme.PAPER);
        Theme.text(g, game.started() ? "TIME: " + (int) Math.ceil(game.remaining()) : "TIME: --", 1030, 48, Theme.pixel(34), Theme.PAPER);
        Theme.text(g, "A/D OR ARROWS: MOVE   SPACE: JUMP   E: PICK UP / DROP / DELIVER   ESC: PAUSE", 30, 90, Theme.pixel(24), Theme.PAPER);
        Theme.text(g, "KEEP THE BUNDLE OUT OF THE WITNESS'S LIGHT.", 30, 120, Theme.pixel(24), Theme.PAPER);
        if (paused) Theme.text(g, "PAUSED - PRESS ESC", 450, 350, Theme.pixel(50), Color.WHITE);
    }
    private static void pixelPerson(Graphics2D g, int x, int y, Color shirt) {
        g.setColor(new Color(191, 153, 124)); g.fillRect(x + 9, y, 22, 20);
        g.setColor(shirt); g.fillRect(x + 5, y + 20, 30, 29);
        g.setColor(new Color(25, 24, 28)); g.fillRect(x + 5, y + 49, 10, 21); g.fillRect(x + 25, y + 49, 10, 21);
    }
}
