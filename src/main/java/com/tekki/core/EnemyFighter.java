package com.tekki.core;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Random;
import javax.imageio.ImageIO;

/**
 * Simple AI-controlled fighter.
 */
public class EnemyFighter extends Fighter {

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

    private SpriteAnimation criticalHitEffect;

    public EnemyFighter(float startX, float startY, float speedMultiplier, float aggression, boolean dashMore, CharacterProfile profile) {
        super(startX, startY, 50, 100, 100, profile);
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

        loadCriticalEffect();
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
    public void update(float deltaTime) {
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
        updateCriticalEffectAnimation(deltaTime);
    }

    @Override
    public void render(Graphics2D g2d) {
        g2d.setColor(colorForState());
        g2d.fillRect((int) x, (int) y, width, height);

        if (isCriticalEffectActive() && criticalHitEffect != null) {
            BufferedImage frame = criticalHitEffect.getCurrentFrame();
            float scale = height / (float) frame.getHeight();
            int drawWidth = Math.round(frame.getWidth() * scale);
            int drawHeight = Math.round(frame.getHeight() * scale);
            float bottomY = y + height;
            int drawY = Math.round(bottomY - drawHeight);
            int drawX = Math.round(getCenterX() - drawWidth / 2f);

            if (facingRight) {
                g2d.drawImage(frame, drawX, drawY, drawWidth, drawHeight, null);
            } else {
                g2d.drawImage(frame, drawX + drawWidth, drawY, -drawWidth, drawHeight, null);
            }
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

}
