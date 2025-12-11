package com.tekki.core;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import javax.imageio.ImageIO;

/**
 * Fighter controlled by player input.
 */
public class PlayerFighter extends Fighter {

    private static final int SPRITE_FRAME_SIZE = 32;
    private static final float RENDER_SCALE = 4.0f;

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
        super(startX, startY, (int) (SPRITE_FRAME_SIZE * RENDER_SCALE), (int) (SPRITE_FRAME_SIZE * RENDER_SCALE), 100, profile);
        loadAnimations();
        this.width = (int) (SPRITE_FRAME_SIZE * RENDER_SCALE);
        this.height = (int) (SPRITE_FRAME_SIZE * RENDER_SCALE);
        this.name = profile != null ? profile.getName() : "Player 1";
        this.currentAnimation = idleAnimation;
    }

    private void loadAnimations() {
        BufferedImage idleSheet = loadSpriteSheet("/sprites/player/IDLE.png", 10, new Color(0, 200, 255, 160));
        BufferedImage runSheet = loadSpriteSheet("/sprites/player/RUN.png", 16, new Color(0, 255, 0, 160));
        BufferedImage attackSheet = loadSpriteSheet("/sprites/player/ATTACK 1.png", 7, new Color(255, 150, 0, 160));
        BufferedImage hurtSheet = loadSpriteSheet("/sprites/player/HURT.png", 4, new Color(255, 0, 0, 160));

        idleAnimation = new SpriteAnimation(idleSheet, SPRITE_FRAME_SIZE, SPRITE_FRAME_SIZE, 10, 0.09f, true);
        runAnimation = new SpriteAnimation(runSheet, SPRITE_FRAME_SIZE, SPRITE_FRAME_SIZE, 16, 0.06f, true);
        attackAnimation = new SpriteAnimation(attackSheet, SPRITE_FRAME_SIZE, SPRITE_FRAME_SIZE, 7, 0.06f, false);
        hurtAnimation = new SpriteAnimation(hurtSheet, SPRITE_FRAME_SIZE, SPRITE_FRAME_SIZE, 4, 0.08f, false);
    }

    private BufferedImage loadSpriteSheet(String path, int frameCount, Color placeholderColor) {
        try (var resourceStream = getClass().getResourceAsStream(path)) {
            if (resourceStream != null) {
                return ImageIO.read(resourceStream);
            }
        } catch (IOException ignored) {
            // Fall through to placeholder creation.
        }

        BufferedImage placeholder = new BufferedImage(SPRITE_FRAME_SIZE * frameCount, SPRITE_FRAME_SIZE, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = placeholder.createGraphics();
        try {
            g2d.setColor(placeholderColor);
            g2d.fillRect(0, 0, placeholder.getWidth(), placeholder.getHeight());
            g2d.setColor(placeholderColor.darker());
            for (int i = 0; i < frameCount; i++) {
                int xOffset = i * SPRITE_FRAME_SIZE;
                g2d.drawRect(xOffset, 0, SPRITE_FRAME_SIZE - 1, SPRITE_FRAME_SIZE - 1);
            }
        } finally {
            g2d.dispose();
        }
        return placeholder;
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
        if (state == FighterState.ATTACKING) {
            if (currentAnimation != attackAnimation) {
                attackAnimation.reset();
            }
            currentAnimation = attackAnimation;
        } else if (state == FighterState.HIT || state == FighterState.KO) {
            if (currentAnimation != hurtAnimation) {
                hurtAnimation.reset();
            }
            currentAnimation = hurtAnimation;
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
        BufferedImage frame = currentAnimation != null ? currentAnimation.getCurrentFrame() : idleAnimation.getCurrentFrame();
        int drawWidth = (int) (SPRITE_FRAME_SIZE * RENDER_SCALE);
        int drawHeight = (int) (SPRITE_FRAME_SIZE * RENDER_SCALE);

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

