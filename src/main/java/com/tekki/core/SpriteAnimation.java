package com.tekki.core;

import java.awt.image.BufferedImage;

public class SpriteAnimation {

    private final BufferedImage[] frames;
    private final float frameDuration;
    private float time;
    private int currentFrame;
    private final boolean looping;

    public SpriteAnimation(BufferedImage spriteSheet,
                           int frameCount,
                           float frameDuration,
                           boolean looping) {
        this.frames = new BufferedImage[frameCount];
        this.frameDuration = frameDuration;
        this.looping = looping;
        this.time = 0f;
        this.currentFrame = 0;

        // echte Breite/Höhe aus dem Bild ableiten
        int sheetWidth  = spriteSheet.getWidth();
        int sheetHeight = spriteSheet.getHeight();

        // Breite eines Blocks = Gesamtbreite / Anzahl Frames
        int blockWidth  = sheetWidth / frameCount;
        int blockHeight = sheetHeight; // eine Zeile

        for (int i = 0; i < frameCount; i++) {
            int blockX = i * blockWidth;
            int blockY = 0;

            int width  = blockWidth;
            int height = blockHeight;

            // Sicherheit: nicht über den Rand schneiden
            width = Math.min(width, sheetWidth - blockX);
            if (width <= 0) {
                width = Math.max(1, sheetWidth - blockX);
            }
            if (blockX + width > sheetWidth) {
                width = sheetWidth - blockX;
            }
            width = Math.max(1, width);

            frames[i] = spriteSheet.getSubimage(blockX, blockY, width, height);
        }
    }

    public void update(float deltaTime) {
        if (frames.length == 0) {
            return;
        }

        time += deltaTime;
        int frameAdvance = (int) (time / frameDuration);
        if (frameAdvance > 0) {
            time -= frameAdvance * frameDuration;
            currentFrame += frameAdvance;
            if (looping) {
                currentFrame %= frames.length;
            } else if (currentFrame >= frames.length) {
                currentFrame = frames.length - 1;
            }
        }
    }

    public void reset() {
        time = 0f;
        currentFrame = 0;
    }

    public BufferedImage getCurrentFrame() {
        return frames[currentFrame];
    }
}

