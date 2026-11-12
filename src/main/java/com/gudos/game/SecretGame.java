package com.gudos.game;

import java.awt.Rectangle;

/** Small deterministic platformer simulation; coordinates are in the 1280x720 scene. */
public final class SecretGame {
    public enum State { PLAYING, WON, LOST }
    private final SecretLevel level;
    private double x, y = 570, vy, bundleX, bundleY = 612, witnessX, remaining;
    private boolean carrying, started, grounded = true, witnessRight = true;
    private State state = State.PLAYING;

    public SecretGame(SecretLevel level) {
        this.level = level; x = level.playerX(); bundleX = level.bundleX();
        witnessX = level.witnessLeft(); remaining = level.seconds();
    }

    public void jump() { if (state == State.PLAYING && grounded) { vy = -600; grounded = false; } }
    public void action() {
        if (state != State.PLAYING) return;
        // Delivery takes precedence over dropping when Gudo is inside the stockroom.
        if (carrying && level.destination().contains(x + 20, y + 40)) { state = State.WON; return; }
        if (carrying) { carrying = false; bundleX = x + 12; bundleY = y + 42; }
        else if (Math.abs(bundleX - x) < 55 && Math.abs(bundleY - (y + 42)) < 65) {
            carrying = true; started = true;
        }
    }
    public void update(double seconds, boolean left, boolean right) {
        if (state != State.PLAYING) return;
        // Short steps avoid falling through platforms and skipping a witness at low frame rates.
        while (seconds > 0 && state == State.PLAYING) {
            double dt = Math.min(seconds, 0.02); seconds -= dt;
            if (started && (remaining -= dt) <= 0) { remaining = 0; state = State.LOST; break; }
            x = Math.max(0, Math.min(1240, x + ((right ? 1 : 0) - (left ? 1 : 0)) * 190 * dt));
            double oldBottom = y + 70;
            vy += 1450 * dt; y += vy * dt; grounded = false;
            if (y >= 570) { y = 570; vy = 0; grounded = true; }
            for (Rectangle p : level.platforms()) {
                if (vy >= 0 && oldBottom <= p.y && y + 70 >= p.y && x + 35 > p.x && x < p.x + p.width) {
                    y = p.y - 70; vy = 0; grounded = true;
                }
            }
            if (!carrying && bundleY < 612) bundleY = Math.min(612, bundleY + 300 * dt);
            witnessX += (witnessRight ? 1 : -1) * 75 * dt;
            if (witnessX >= level.witnessRight()) { witnessX = level.witnessRight(); witnessRight = false; }
            if (witnessX <= level.witnessLeft()) { witnessX = level.witnessLeft(); witnessRight = true; }
            if (started) {
                double bx = carrying ? x + 20 : bundleX + 12;
                double by = carrying ? y + 48 : bundleY + 10;
                double distance = (bx - witnessX) * (witnessRight ? 1 : -1);
                // Shelves hide the bundle, not the player. A dropped bundle remains detectable.
                boolean hidden = level.cover().stream().anyMatch(r -> r.contains(bx, by));
                if (!hidden && distance >= 0 && distance < 200 && by > 500) state = State.LOST;
            }
        }
    }
    public SecretLevel level() { return level; }
    public double x() { return x; } public double y() { return y; }
    public double bundleX() { return bundleX; } public double bundleY() { return bundleY; }
    public double witnessX() { return witnessX; } public boolean witnessRight() { return witnessRight; }
    public double remaining() { return remaining; } public boolean started() { return started; }
    public boolean carrying() { return carrying; } public State state() { return state; }
}
