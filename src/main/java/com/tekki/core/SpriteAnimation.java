package com.tekki.core;

import java.awt.image.BufferedImage;

public class SpriteAnimation {

    private final BufferedImage[] frames;
    private final float frameDuration;
    private float time;
    private int currentFrame;
    private final boolean looping;

    public SpriteAnimation(BufferedImage spriteSheet,
                           int sheetFrameWidth,
                           int sheetFrameHeight,
                           int frameWidth,
                           int frameHeight,
                           int frameCount,
                           float frameDuration,
                           boolean looping) {
        this.frames = new BufferedImage[frameCount];
        this.frameDuration = frameDuration;
        this.looping = looping;
        this.time = 0f;
        this.currentFrame = 0;

        int offsetX = Math.max(0, (sheetFrameWidth - frameWidth) / 2);
        int offsetY = Math.max(0, (sheetFrameHeight - frameHeight) / 2);

        for (int i = 0; i < frameCount; i++) {
            int srcX = i * sheetFrameWidth + offsetX;
            int srcY = offsetY;
            frames[i] = spriteSheet.getSubimage(srcX, srcY, frameWidth, frameHeight);
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

