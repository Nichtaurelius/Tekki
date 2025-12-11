package com.tekki.core;

import java.awt.Color;

/**
 * Simple holder for character metadata that can later be extended with textures.
 */
public class CharacterProfile {

    private final String name;
    private final Color baseColor;
    private final String texturePath;

    public CharacterProfile(String name, Color baseColor, String texturePath) {
        this.name = name;
        this.baseColor = baseColor;
        this.texturePath = texturePath;
    }

    public String getName() {
        return name;
    }

    public Color getBaseColor() {
        return baseColor;
    }

    public String getTexturePath() {
        return texturePath;
    }
}
