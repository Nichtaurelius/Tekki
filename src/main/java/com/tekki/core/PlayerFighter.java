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

    private static final float RENDER_SCALE = 0.5f;
    private static final int COLLISION_WIDTH = 96;
    private static final int COLLISION_HEIGHT = 64;
    private static final int AVATAR_VISUAL_HEIGHT = 110;
    private static final int AVATAR_VISUAL_WIDTH = 50;
    private static final int AVATAR_FOOT_OFFSET_FROM_BOTTOM = 45;

    private boolean isDashing = false;
    private float dashSpeed = 900f;
    private float dashDuration = 0.15f;
    private float dashTimer = 0f;
    private float dashCooldown = 2.0f;
    private float dashCooldownTimer = 0f;

    private SpriteAnimation idleAnimation;
    private SpriteAnimation runAnimation;
    private SpriteAnimation jumpAnimation;
    private SpriteAnimation fallAnimation;
    private SpriteAnimation attack1Animation;
    private SpriteAnimation attack2Animation;
    private SpriteAnimation takeHitAnimation;
    private SpriteAnimation deathAnimation;
    private SpriteAnimation currentAnimation;
    private SpriteAnimation activeAttackAnimation;
    private boolean useFirstAttackNext = true;

    public PlayerFighter(float startX, float startY, CharacterProfile profile) {
        super(
                startX,
                startY,
                COLLISION_WIDTH,
                COLLISION_HEIGHT,
                100,
                profile
        );

        loadAnimations();

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
        BufferedImage idleSheet    = loadSpriteFromFile("Tekki/src/main/resources/sprites/player/Idle.png");
        BufferedImage runSheet     = loadSpriteFromFile("Tekki/src/main/resources/sprites/player/Run.png");
        BufferedImage jumpSheet    = loadSpriteFromFile("Tekki/src/main/resources/sprites/player/Jump.png");
        BufferedImage fallSheet    = loadSpriteFromFile("Tekki/src/main/resources/sprites/player/Fall.png");
        BufferedImage attack1Sheet = loadSpriteFromFile("Tekki/src/main/resources/sprites/player/Attack1.png");
        BufferedImage attack2Sheet = loadSpriteFromFile("Tekki/src/main/resources/sprites/player/Attack2.png");
        BufferedImage takeHitSheet = loadSpriteFromFile("Tekki/src/main/resources/sprites/player/Take Hit.png");
        BufferedImage deathSheet   = loadSpriteFromFile("Tekki/src/main/resources/sprites/player/Death.png");

        idleAnimation = new SpriteAnimation(idleSheet, 8, 0.12f, true);
        runAnimation = new SpriteAnimation(runSheet, 8, 0.08f, true);
        jumpAnimation = new SpriteAnimation(jumpSheet, 2, 0.1f, false);
        fallAnimation = new SpriteAnimation(fallSheet, 2, 0.1f, false);
        attack1Animation = new SpriteAnimation(attack1Sheet, 6, 0.04f, false);
        attack2Animation = new SpriteAnimation(attack2Sheet, 6, 0.07f, false);
        takeHitAnimation = new SpriteAnimation(takeHitSheet, 4, 0.09f, false);
        deathAnimation = new SpriteAnimation(deathSheet, 6, 0.12f, false);

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
        if (state == FighterState.ATTACKING && previousState != FighterState.ATTACKING) {
            activeAttackAnimation = useFirstAttackNext ? attack1Animation : attack2Animation;
            if (activeAttackAnimation != null) {
                activeAttackAnimation.reset();
            }
            useFirstAttackNext = !useFirstAttackNext;
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
        SpriteAnimation nextAnimation = idleAnimation;

        if (state == FighterState.KO) {
            nextAnimation = deathAnimation;
        } else if (state == FighterState.HIT) {
            nextAnimation = takeHitAnimation;
        } else if (state == FighterState.ATTACKING) {
            nextAnimation = activeAttackAnimation != null ? activeAttackAnimation : attack1Animation;
        } else if (state == FighterState.JUMPING || !onGround) {
            nextAnimation = yVelocity < 0 ? jumpAnimation : fallAnimation;
        } else if (state == FighterState.WALKING || state == FighterState.DASHING) {
            nextAnimation = runAnimation;
        }

        if (nextAnimation != currentAnimation && nextAnimation != null) {
            nextAnimation.reset();
        }

        currentAnimation = nextAnimation;

        if (currentAnimation != null) {
            currentAnimation.update(deltaTime);
        }
    }

    @Override
    public void render(Graphics2D g2d) {
        BufferedImage frame = currentAnimation != null
                ? currentAnimation.getCurrentFrame()
                : idleAnimation.getCurrentFrame();

        int drawWidth = (int) (frame.getWidth() * RENDER_SCALE);
        int drawHeight = (int) (frame.getHeight() * RENDER_SCALE);

        float collisionBottomY = y + height;
        float frameFootFromTop = frame.getHeight() - AVATAR_FOOT_OFFSET_FROM_BOTTOM;
        int drawY = Math.round(collisionBottomY - frameFootFromTop * RENDER_SCALE);

        float centerX = getCenterX();
        int drawX = Math.round(centerX - drawWidth / 2f);

        if (facingRight) {
            g2d.drawImage(frame, drawX, drawY, drawWidth, drawHeight, null);
        } else {
            g2d.drawImage(frame, drawX + drawWidth, drawY, -drawWidth, drawHeight, null);
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
