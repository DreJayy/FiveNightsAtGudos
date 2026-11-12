package com.gudos.game;

import java.awt.Rectangle;
import java.util.List;

/** Immutable level assembled independently of its Swing presentation. */
public record SecretLevel(double playerX, double bundleX, Rectangle destination,
                          List<Rectangle> platforms, List<Rectangle> cover,
                          double witnessLeft, double witnessRight, double seconds) {
    public SecretLevel {
        destination = new Rectangle(destination);
        platforms = platforms.stream().map(Rectangle::new).toList();
        cover = cover.stream().map(Rectangle::new).toList();
    }
}
