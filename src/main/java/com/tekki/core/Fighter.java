package com.tekki.core;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;

/**
 * Base fighter containing shared properties such as position, movement, and basic state handling.
 */
public abstract class Fighter {

    protected float x;
    protected float y;
    protected int width;
    protected int height;
    protected int maxHealth;
    protected int health;
    protected float speedX;
    protected boolean facingRight = true;
    protected FighterState state = FighterState.IDLE;

    protected float yVelocity = 0f;
    protected float gravity = 1500f;
    protected float jumpStrength = -650f;
    protected boolean onGround = false;
    protected float groundY = 380f;

    private float attackTimer = 0f;
    private final float attackDuration = 0.25f;

    protected Fighter(float x, float y, int width, int height, int maxHealth) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.maxHealth = maxHealth;
        this.health = maxHealth;
    }

    /**
     * Update position and timers for this fighter.
     */
    public void update(float deltaTime) {
        x += speedX * deltaTime;
        applyVerticalMovement(deltaTime);
        updateAttack(deltaTime);
    }

    private void applyVerticalMovement(float deltaTime) {
        y += yVelocity * deltaTime;
        yVelocity += gravity * deltaTime;

        if (y >= groundY) {
            y = groundY;
            yVelocity = 0f;
            if (!onGround) {
                onGround = true;
                if (state == FighterState.JUMPING || state == FighterState.DASHING) {
                    state = Math.abs(speedX) > 0.01f ? FighterState.WALKING : FighterState.IDLE;
                }
            }
        } else {
            onGround = false;
        }
    }

    /**
     * Simple rectangle representing the current hurtbox.
     */
    public Rectangle getBounds() {
        return new Rectangle((int) x, (int) y, width, height);
    }

    /**
     * Center X helper for AI and rendering alignment.
     */
    public float getCenterX() {
        return x + width / 2f;
    }

    /**
     * Move fighter left by setting horizontal velocity and state.
     */
    public void moveLeft() {
        if (state == FighterState.DEFENDING || state == FighterState.DASHING) {
            return;
        }
        speedX = -450f;
        facingRight = false;
        if (state != FighterState.ATTACKING && state != FighterState.JUMPING) {
            state = FighterState.WALKING;
        }
    }

    /**
     * Move fighter right by setting horizontal velocity and state.
     */
    public void moveRight() {
        if (state == FighterState.DEFENDING || state == FighterState.DASHING) {
            return;
        }
        speedX = 450f;
        facingRight = true;
        if (state != FighterState.ATTACKING && state != FighterState.JUMPING) {
            state = FighterState.WALKING;
        }
    }

    /**
     * Stop horizontal movement.
     */
    public void stopMoving() {
        if (state == FighterState.DASHING) {
            return;
        }
        speedX = 0f;
        if (state != FighterState.ATTACKING && state != FighterState.DEFENDING && state != FighterState.JUMPING) {
            state = FighterState.IDLE;
        }
    }

    /**
     * Begin a simple attack animation.
     */
    public void startAttack() {
        if (state == FighterState.ATTACKING || state == FighterState.DEFENDING || state == FighterState.DASHING || state == FighterState.JUMPING) {
            return;
        }
        attackTimer = attackDuration;
        state = FighterState.ATTACKING;
    }

    /**
     * Handle attack timer countdown.
     */
    protected void updateAttack(float deltaTime) {
        if (state == FighterState.ATTACKING) {
            attackTimer -= deltaTime;
            if (attackTimer <= 0f) {
                attackTimer = 0f;
                state = speedX != 0 ? FighterState.WALKING : FighterState.IDLE;
            }
        }
    }

    /**
     * Begin defending; cancels movement and attack animation.
     */
    public void startDefending() {
        if (!onGround || state == FighterState.DASHING || state == FighterState.JUMPING) {
            return;
        }
        speedX = 0f;
        attackTimer = 0f;
        state = FighterState.DEFENDING;
    }

    /**
     * Stop defending; return to idle or walking depending on movement.
     */
    public void stopDefending() {
        if (state == FighterState.DEFENDING) {
            state = speedX != 0 ? FighterState.WALKING : FighterState.IDLE;
        }
    }

    /**
     * Attempt to jump if on the ground.
     */
    public void jump() {
        if (!onGround || state == FighterState.DEFENDING || state == FighterState.DASHING) {
            return;
        }
        yVelocity = jumpStrength;
        onGround = false;
        state = FighterState.JUMPING;
    }

    /**
     * Render the fighter. Subclasses should override for custom visuals.
     */
    public abstract void render(Graphics2D g2d);

    /**
     * Utility to choose a simple color based on state.
     */
    protected Color colorForState() {
        return switch (state) {
            case IDLE -> new Color(60, 120, 255);
            case WALKING -> new Color(60, 200, 120);
            case ATTACKING -> new Color(200, 60, 60);
            case DEFENDING -> new Color(230, 210, 60);
            case JUMPING -> new Color(120, 120, 255);
            case DASHING -> new Color(255, 120, 200);
            case HIT -> new Color(255, 180, 60);
            case KO -> Color.GRAY;
        };
    }

    public FighterState getState() {
        return state;
    }

    public boolean isOnGround() {
        return onGround;
    }
}
