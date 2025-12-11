package com.tekki.core;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;

/**
 * Fighter controlled by player input.
 */
public class PlayerFighter extends Fighter {

    private static final int SPRITE_FRAME_WIDTH = 96;
    private static final int SPRITE_FRAME_HEIGHT = 64;
    private static final int SHEET_FRAME_SIZE = 96;
    private static final float RENDER_SCALE = 1.0f;

    private boolean isDashing = false;
    private float dashSpeed = 900f;
    private float dashDuration = 0.15f;
    private float dashTimer = 0f;
    private float dashCooldown = 0.5f;
    private float dashCooldownTimer = 0f;

    private SpriteAnimation idleAnimation;
    private SpriteAnimation runAnimation;
    private SpriteAnimation attackAnimation;
    private SpriteAnimation hurtAnimation;
    private SpriteAnimation currentAnimation;

    public PlayerFighter(float startX, float startY, CharacterProfile profile) {
        super(
                startX,
                startY,
                (int) (SPRITE_FRAME_WIDTH * RENDER_SCALE),
                (int) (SPRITE_FRAME_HEIGHT * RENDER_SCALE),
                100,
                profile
        );

        loadAnimations();

        // make sure fighter bounds match sprite size
        this.width = (int) (SPRITE_FRAME_WIDTH * RENDER_SCALE);
        this.height = (int) (SPRITE_FRAME_HEIGHT * RENDER_SCALE);

        this.name = profile != null ? profile.getName() : "Player 1";
        this.currentAnimation = idleAnimation;
    }

    /**
     * Helper to load a sprite sheet from a file path relative to the project root.
     */
    private BufferedImage loadSpriteFromFile(String relativePath) {
        File file = new File(relativePath);
        if (!file.exists()) {
            throw new IllegalStateException("Sprite file not found: " + file.getAbsolutePath());
        }
        try {
            return ImageIO.read(file);
        } catch (IOException e) {
            throw new IllegalStateException("Could not read sprite file: " + file.getAbsolutePath(), e);
        }
    }

    private void loadAnimations() {
        // IMPORTANT:
        // These paths are relative to your project root (C:\Studium\Tekki)
        BufferedImage idleSheet   = loadSpriteFromFile("Tekki/src/main/resources/sprites/player/IDLE.png");
        BufferedImage runSheet    = loadSpriteFromFile("Tekki/src/main/resources/sprites/player/RUN.png");
        BufferedImage attackSheet = loadSpriteFromFile("Tekki/src/main/resources/sprites/player/ATTACK 1.png");
        BufferedImage hurtSheet   = loadSpriteFromFile("Tekki/src/main/resources/sprites/player/HURT.png");

        idleAnimation = new SpriteAnimation(idleSheet, SHEET_FRAME_SIZE, SHEET_FRAME_SIZE, SPRITE_FRAME_WIDTH, SPRITE_FRAME_HEIGHT, 10, 0.09f, true);
        runAnimation = new SpriteAnimation(runSheet, SHEET_FRAME_SIZE, SHEET_FRAME_SIZE, SPRITE_FRAME_WIDTH, SPRITE_FRAME_HEIGHT, 16, 0.06f, true);
        attackAnimation = new SpriteAnimation(attackSheet, SHEET_FRAME_SIZE, SHEET_FRAME_SIZE, SPRITE_FRAME_WIDTH, SPRITE_FRAME_HEIGHT, 7, 0.06f, false);
        hurtAnimation = new SpriteAnimation(hurtSheet, SHEET_FRAME_SIZE, SHEET_FRAME_SIZE, SPRITE_FRAME_WIDTH, SPRITE_FRAME_HEIGHT, 4, 0.08f, false);

        currentAnimation = idleAnimation;
    }

    public void startDash() {
        if (!onGround || state == FighterState.DEFENDING || state == FighterState.DASHING || dashCooldownTimer > 0f) {
            return;
        }
        isDashing = true;
        dashTimer = dashDuration;
        dashCooldownTimer = 0f;
        speedX = facingRight ? dashSpeed : -dashSpeed;
        state = FighterState.DASHING;
    }

    public void startDefending() {
        super.startDefending();
    }

    public void stopDefending() {
        super.stopDefending();
    }

    @Override
    public void startAttack() {
        FighterState previousState = state;
        super.startAttack();
        if (state == FighterState.ATTACKING && previousState != FighterState.ATTACKING && attackAnimation != null) {
            attackAnimation.reset();
        }
    }

    @Override
    public void update(float deltaTime) {
        if (dashCooldownTimer > 0f) {
            dashCooldownTimer -= deltaTime;
        }

        if (isDashing) {
            dashTimer -= deltaTime;
            if (dashTimer <= 0f) {
                isDashing = false;
                dashCooldownTimer = dashCooldown;
                speedX = 0f;
                if (state == FighterState.DASHING) {
                    state = onGround ? FighterState.IDLE : FighterState.JUMPING;
                }
            }
        }

        super.update(deltaTime);
        updateCurrentAnimation(deltaTime);
    }

    private void updateCurrentAnimation(float deltaTime) {
        if (state == FighterState.KO) {
            if (currentAnimation != hurtAnimation) {
                hurtAnimation.reset();
            }
            currentAnimation = hurtAnimation;
        } else if (state == FighterState.HIT) {
            if (currentAnimation != hurtAnimation) {
                hurtAnimation.reset();
            }
            currentAnimation = hurtAnimation;
        } else if (state == FighterState.ATTACKING) {
            if (currentAnimation != attackAnimation) {
                attackAnimation.reset();
            }
            currentAnimation = attackAnimation;
        } else if (state == FighterState.WALKING || state == FighterState.DASHING || state == FighterState.JUMPING) {
            currentAnimation = runAnimation;
        } else {
            currentAnimation = idleAnimation;
        }

        if (currentAnimation != null) {
            currentAnimation.update(deltaTime);
        }
    }

    @Override
    public void render(Graphics2D g2d) {
        BufferedImage frame = currentAnimation != null
                ? currentAnimation.getCurrentFrame()
                : idleAnimation.getCurrentFrame();

        int drawWidth = (int) (SPRITE_FRAME_WIDTH * RENDER_SCALE);
        int drawHeight = (int) (SPRITE_FRAME_HEIGHT * RENDER_SCALE);

        if (facingRight) {
            g2d.drawImage(frame, (int) x, (int) y, drawWidth, drawHeight, null);
        } else {
            int drawX = (int) x + drawWidth;
            g2d.drawImage(frame, drawX, (int) y, -drawWidth, drawHeight, null);
        }
    }

    public float getDashCooldown() {
        return dashCooldown;
    }

    public float getDashCooldownTimer() {
        return dashCooldownTimer;
    }

    public boolean isDashReady() {
        return dashCooldownTimer <= 0f;
    }
}
