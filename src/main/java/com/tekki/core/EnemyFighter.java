package com.tekki.core;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Random;
import javax.imageio.ImageIO;

/**
 * Simple AI-controlled fighter without critical-hit visual effects.
 */
public class EnemyFighter extends Fighter {

    private static final float RENDER_SCALE = 2.5f;
    private static final int COLLISION_WIDTH = 50;
    private static final int COLLISION_HEIGHT = 100;
    private static final int AVATAR_FOOT_OFFSET_FROM_BOTTOM = 78;

    private final float attackRange;
    private final float preferredDistance;
    private float aiDecisionTimer;
    private final float aiDecisionInterval;
    private final float baseAttackCooldown;
    private float attackCooldownTimer;
    private final Random random = new Random();

    private boolean isDashing = false;
    private float dashSpeed = 800f;
    private float dashDuration = 0.15f;
    private float dashTimer = 0f;
    private float dashCooldown = 1.0f;
    private float dashCooldownTimer = 0f;

    private float speedMultiplier;
    private float aggression;
    private boolean dashMore;

    private final String spriteFolder;

    private SpriteAnimation idleAnimation;
    private SpriteAnimation runAnimation;
    private SpriteAnimation jumpAnimation;
    private SpriteAnimation fallAnimation;
    private SpriteAnimation attack1Animation;
    private SpriteAnimation attack2Animation;
    private SpriteAnimation attack3Animation;
    private SpriteAnimation takeHitAnimation;
    private SpriteAnimation deathAnimation;
    private SpriteAnimation currentAnimation;
    private SpriteAnimation activeAttackAnimation;
    private boolean useFirstAttackNext = true;

    public EnemyFighter(float startX, float startY, float speedMultiplier, float aggression, boolean dashMore,
                        CharacterProfile profile, String spriteFolder) {
        super(startX, startY, COLLISION_WIDTH, COLLISION_HEIGHT, 100, profile);
        this.name = profile != null ? profile.getName() : "CPU Fighter";
        this.attackRange = 110f;
        this.preferredDistance = 95f;
        this.aiDecisionInterval = Math.max(0.2f, 0.4f / Math.max(0.5f, aggression));
        this.baseAttackCooldown = 1.0f / Math.max(0.5f, aggression);
        this.aiDecisionTimer = aiDecisionInterval;
        this.attackCooldownTimer = 0f;
        this.speedMultiplier = Math.max(0.5f, speedMultiplier);
        this.aggression = Math.max(0.5f, aggression);
        this.dashMore = dashMore;
        this.dashCooldown = (dashMore ? 0.75f : 1.0f) / this.aggression;
        this.dashSpeed = 800f * this.speedMultiplier * (dashMore ? 1.3f : 1.0f);
        this.spriteFolder = spriteFolder;

        loadAnimations();
        this.currentAnimation = idleAnimation;
    }

    /**
     * Decide movement and actions relative to the player.
     */
    public void updateAI(float deltaTime, PlayerFighter player) {
        aiDecisionTimer -= deltaTime;
        attackCooldownTimer -= deltaTime;
        if (dashCooldownTimer > 0f) {
            dashCooldownTimer -= deltaTime;
        }

        float dx = player.getCenterX() - getCenterX();
        facingRight = dx >= 0;

        if (aiDecisionTimer <= 0f) {
            aiDecisionTimer = aiDecisionInterval;
            float distance = Math.abs(dx);

            if (state != FighterState.ATTACKING && state != FighterState.DEFENDING && state != FighterState.DASHING) {
                if (distance > attackRange) {
                    if (shouldDash(distance)) {
                        startDashToward(player);
                    } else {
                        if (dx > 0) {
                            moveRight();
                        } else {
                            moveLeft();
                        }
                    }
                } else {
                    int roll = random.nextInt(100);
                    if (roll < (int) (25 * aggression)) {
                        stopMoving();
                        startDefending();
                    } else if (roll < (int) (65 * aggression) && attackCooldownTimer <= 0f) {
                        stopMoving();
                        startAttack();
                        attackCooldownTimer = Math.max(0.35f, baseAttackCooldown);
                    } else {
                        if (distance < preferredDistance) {
                            if (dx > 0) {
                                moveLeft();
                            } else {
                                moveRight();
                            }
                        } else if (!isDashing) {
                            stopMoving();
                        }
                    }
                }
            }
        }

        if (state == FighterState.DEFENDING && random.nextInt(100) < 20) {
            stopDefending();
        }
    }

    @Override
    public void moveLeft() {
        if (state == FighterState.DEFENDING || state == FighterState.DASHING) {
            return;
        }
        speedX = -450f * speedMultiplier;
        facingRight = false;
        if (state != FighterState.ATTACKING && state != FighterState.JUMPING) {
            state = FighterState.WALKING;
        }
    }

    @Override
    public void moveRight() {
        if (state == FighterState.DEFENDING || state == FighterState.DASHING) {
            return;
        }
        speedX = 450f * speedMultiplier;
        facingRight = true;
        if (state != FighterState.ATTACKING && state != FighterState.JUMPING) {
            state = FighterState.WALKING;
        }
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
        if (state == FighterState.KO) {
            updateCurrentAnimation(deltaTime);
            return;
        }

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

    @Override
    public void render(Graphics2D g2d) {
        SpriteAnimation animationToDraw = currentAnimation != null ? currentAnimation : idleAnimation;
        BufferedImage frame = animationToDraw != null ? animationToDraw.getCurrentFrame() : null;
        if (frame == null) {
            return;
        }

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

    private boolean shouldDash(float distance) {
        float baseChance = dashMore ? 0.55f : 0.35f;
        float scaledChance = Math.min(0.95f, baseChance * aggression);
        return onGround && distance > preferredDistance && distance < preferredDistance + 180f && dashCooldownTimer <= 0f
                && random.nextFloat() < scaledChance;
    }

    private void startDashToward(PlayerFighter player) {
        if (!onGround || isDashing || state == FighterState.DEFENDING || state == FighterState.ATTACKING || dashCooldownTimer > 0
                || state == FighterState.HIT) {
            return;
        }
        isDashing = true;
        dashTimer = dashDuration;
        speedX = player.getCenterX() >= getCenterX() ? dashSpeed : -dashSpeed;
        state = FighterState.DASHING;
    }

    private void loadAnimations() {
        String basePath = "Tekki/src/main/resources/sprites/enemies/" + spriteFolder + "/";
        EnemyAnimationConfig.AnimationSet animationConfig = EnemyAnimationConfig.forEnemy(spriteFolder);

        idleAnimation = createAnimation(animationConfig, EnemyAnimationConfig.AnimationType.IDLE,
                basePath + "Idle.png", 0.12f, true);
        runAnimation = createAnimation(animationConfig, EnemyAnimationConfig.AnimationType.RUN,
                basePath + "Run.png", 0.08f, true);
        jumpAnimation = createAnimation(animationConfig, EnemyAnimationConfig.AnimationType.JUMP,
                basePath + "Jump.png", 0.1f, false);
        fallAnimation = createAnimation(animationConfig, EnemyAnimationConfig.AnimationType.FALL,
                basePath + "Fall.png", 0.1f, false);
        attack1Animation = createAnimation(animationConfig, EnemyAnimationConfig.AnimationType.ATTACK1,
                basePath + "Attack1.png", 0.04f, false);
        attack2Animation = createAnimation(animationConfig, EnemyAnimationConfig.AnimationType.ATTACK2,
                basePath + "Attack2.png", 0.07f, false);
        attack3Animation = createAnimation(animationConfig, EnemyAnimationConfig.AnimationType.ATTACK3,
                basePath + "Attack3.png", 0.07f, false);
        takeHitAnimation = createAnimation(animationConfig, EnemyAnimationConfig.AnimationType.TAKE_HIT,
                basePath + "Take Hit.png", 0.09f, false);
        deathAnimation = createAnimation(animationConfig, EnemyAnimationConfig.AnimationType.DEATH,
                basePath + "Death.png", 0.12f, false);
    }

    private SpriteAnimation createAnimation(EnemyAnimationConfig.AnimationSet animationConfig,
            EnemyAnimationConfig.AnimationType type, String path, float frameDuration, boolean loop) {
        Integer frameCount = animationConfig.getFrameCount(type);
        if (frameCount == null) {
            return null;
        }
        BufferedImage sheet = loadSpriteFromFile(path);
        return new SpriteAnimation(sheet, frameCount, frameDuration, loop);
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

    @Override
    protected void onCriticalHitTriggered() {
        // Enemies currently have no critical-hit visual effect assets.
    }
}
