package com.gudos.game;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;

/** Builder: assembles the pieces of a secret level before producing the finished level. */
public final class SecretLevelBuilder {
    private double playerX, bundleX, left, right, seconds = 60;
    private Rectangle destination;
    private final List<Rectangle> platforms = new ArrayList<>(), cover = new ArrayList<>();

    public SecretLevelBuilder player(double x) { playerX = x; return this; }
    public SecretLevelBuilder bundle(double x) { bundleX = x; return this; }
    public SecretLevelBuilder destination(Rectangle r) { destination = new Rectangle(r); return this; }
    public SecretLevelBuilder platform(Rectangle r) { platforms.add(new Rectangle(r)); return this; }
    public SecretLevelBuilder cover(Rectangle r) { cover.add(new Rectangle(r)); return this; }
    public SecretLevelBuilder witness(double from, double to) { left = from; right = to; return this; }
    public SecretLevelBuilder timeLimit(double value) { seconds = value; return this; }
    public SecretLevel build() {
        if (destination == null || right <= left || seconds <= 0) throw new IllegalStateException("Incomplete secret level");
        return new SecretLevel(playerX, bundleX, destination, platforms, cover, left, right, seconds);
    }
}
