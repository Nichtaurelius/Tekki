package com.tekki.core;

import java.awt.Color;

/**
 * Simple data holder for stage visuals and enemy tuning parameters.
 */
public class Level {

    private final String name;
    private final Color backgroundColor;
    private final Color floorColor;
    private final float enemySpeedMultiplier;
    private final float enemyAggression;
    private final boolean enemyDashesMore;
    private final int enemyDamage;
    private final String enemySpriteFolder;

    public Level(String name, Color backgroundColor, Color floorColor, float enemySpeedMultiplier, float enemyAggression, boolean enemyDashesMore, int enemyDamage, String enemySpriteFolder) {
        this.name = name;
        this.backgroundColor = backgroundColor;
        this.floorColor = floorColor;
        this.enemySpeedMultiplier = enemySpeedMultiplier;
        this.enemyAggression = enemyAggression;
        this.enemyDashesMore = enemyDashesMore;
        this.enemyDamage = enemyDamage;
        this.enemySpriteFolder = enemySpriteFolder;
    }

    public String getName() {
        return name;
    }

    public Color getBackgroundColor() {
        return backgroundColor;
    }

    public Color getFloorColor() {
        return floorColor;
    }

    public float getEnemySpeedMultiplier() {
        return enemySpeedMultiplier;
    }

    public float getEnemyAggression() {
        return enemyAggression;
    }

    public boolean isEnemyDashesMore() {
        return enemyDashesMore;
    }

    public int getEnemyDamage() {
        return enemyDamage;
    }

    public String getEnemySpriteFolder() {
        return enemySpriteFolder;
    }
}
