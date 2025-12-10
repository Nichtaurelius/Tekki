package com.tekki.core;

import java.awt.Color;
import java.awt.Graphics2D;
import java.util.Random;

/**
 * Simple AI-controlled fighter.
 */
public class EnemyFighter extends Fighter {

    private final float attackRange;
    private final float preferredDistance;
    private float aiDecisionTimer;
    private final float aiDecisionInterval;
    private final float attackCooldown;
    private float attackCooldownTimer;
    private final Random random = new Random();

    private boolean isDashing = false;
    private float dashSpeed = 800f;
    private float dashDuration = 0.15f;
    private float dashTimer = 0f;
    private float dashCooldown = 1.0f;
    private float dashCooldownTimer = 0f;

    public EnemyFighter(float startX, float startY) {
        super(startX, startY, 50, 100, 100);
        this.name = "CPU Fighter";
        this.attackRange = 110f;
        this.preferredDistance = 95f;
        this.aiDecisionInterval = 0.4f;
        this.attackCooldown = 1.0f;
        this.aiDecisionTimer = aiDecisionInterval;
        this.attackCooldownTimer = 0f;
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
                    if (roll < 30) {
                        stopMoving();
                        startDefending();
                    } else if (roll < 70 && attackCooldownTimer <= 0f) {
                        stopMoving();
                        startAttack();
                        attackCooldownTimer = attackCooldown;
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
    }

    @Override
    public void render(Graphics2D g2d) {
        g2d.setColor(enemyColorForState());
        g2d.fillRect((int) x, (int) y, width, height);
    }

    private Color enemyColorForState() {
        return switch (state) {
            case IDLE -> new Color(200, 80, 200);
            case WALKING -> new Color(220, 140, 80);
            case ATTACKING -> new Color(255, 80, 120);
            case DEFENDING -> new Color(200, 200, 100);
            case JUMPING -> new Color(180, 120, 255);
            case DASHING -> new Color(255, 160, 220);
            case HIT -> new Color(255, 200, 120);
            case KO -> Color.DARK_GRAY;
        };
    }

    private boolean shouldDash(float distance) {
        return onGround && distance > preferredDistance && distance < preferredDistance + 180f && dashCooldownTimer <= 0f
                && random.nextInt(100) < 35;
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
