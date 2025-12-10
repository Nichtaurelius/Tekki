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

    public EnemyFighter(float startX, float startY) {
        super(startX, startY, 50, 100, 100);
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

        float dx = player.getCenterX() - getCenterX();
        facingRight = dx >= 0;

        if (aiDecisionTimer <= 0f) {
            aiDecisionTimer = aiDecisionInterval;
            float distance = Math.abs(dx);

            if (state != FighterState.ATTACKING && state != FighterState.DEFENDING && state != FighterState.DASHING) {
                if (distance > attackRange) {
                    if (dx > 0) {
                        moveRight();
                    } else {
                        moveLeft();
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
                        } else {
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
}
