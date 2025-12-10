package com.tekki.core;

import java.awt.Graphics2D;

/**
 * Fighter controlled by player input.
 */
public class PlayerFighter extends Fighter {

    public PlayerFighter(float startX, float startY) {
        super(startX, startY, 50, 100, 100);
    }

    @Override
    public void render(Graphics2D g2d) {
        g2d.setColor(colorForState());
        g2d.fillRect((int) x, (int) y, width, height);
    }
}
