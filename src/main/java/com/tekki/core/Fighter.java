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

    protected boolean hasHitDuringCurrentAttack = false;

    protected float yVelocity = 0f;
    protected float gravity = 1500f;
    protected float jumpStrength = -650f;
    protected boolean onGround = false;
    protected float groundY = 380f;

    protected String name = "Fighter";
    protected CharacterProfile profile;

    private float attackTimer = 0f;
    private final float attackDuration = 0.25f;

    private final int attackBoxWidth = 50;
    private final int attackBoxHeight = 50;

    protected float hitStunDuration = 0.35f;
    protected float hitStunTimer = 0f;

    protected Fighter(float x, float y, int width, int height, int maxHealth, CharacterProfile profile) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.maxHealth = maxHealth;
        this.health = maxHealth;
        this.profile = profile;
        if (profile != null) {
            this.name = profile.getName();
        }
    }

    /**
     * Update position and timers for this fighter.
     */
    public void update(float deltaTime) {
        if (state == FighterState.KO) {
            speedX = 0f;
            return;
        }

        if (state == FighterState.HIT) {
            speedX = 0f;
            hitStunTimer -= deltaTime;
            if (hitStunTimer <= 0f) {
                hitStunTimer = 0f;
                state = Math.abs(speedX) > 0.01f ? FighterState.WALKING : FighterState.IDLE;
            }
        }

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
        if (state == FighterState.ATTACKING || state == FighterState.DEFENDING || state == FighterState.DASHING || state == FighterState.JUMPING || state == FighterState.HIT || state == FighterState.KO) {
            return;
        }
        attackTimer = attackDuration;
        state = FighterState.ATTACKING;
        hasHitDuringCurrentAttack = false;
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

    public Rectangle getAttackHitbox() {
        if (state != FighterState.ATTACKING) {
            return null;
        }
        Rectangle bounds = getBounds();
        int attackX = facingRight ? bounds.x + bounds.width : bounds.x - attackBoxWidth;
        int attackY = bounds.y + (bounds.height / 4);
        return new Rectangle(attackX, attackY, attackBoxWidth, attackBoxHeight);
    }

    public boolean canHit() {
        return state == FighterState.ATTACKING && !hasHitDuringCurrentAttack && state != FighterState.KO;
    }

    public void markHit() {
        hasHitDuringCurrentAttack = true;
    }

    public void takeDamage(int amount) {
        if (state == FighterState.KO) {
            return;
        }
        health -= amount;
        if (health < 0) {
            health = 0;
        }

        if (health == 0) {
            speedX = 0f;
            yVelocity = 0f;
            state = FighterState.KO;
        } else {
            state = FighterState.HIT;
            hitStunTimer = hitStunDuration;
        }
    }

    public int getHealth() {
        return health;
    }

    public int getMaxHealth() {
        return maxHealth;
    }

    public int getHeight() {
        return height;
    }

    public int getWidth() {
        return width;
    }

    public boolean isKO() {
        return state == FighterState.KO || health <= 0;
    }

    /**
     * Render the fighter. Subclasses should override for custom visuals.
     */
    public abstract void render(Graphics2D g2d);

    /**
     * Utility to choose a simple color based on state.
     */
    protected Color colorForState() {
        Color base = profile != null ? profile.getBaseColor() : new Color(60, 120, 255);
        return switch (state) {
            case IDLE -> base;
            case WALKING -> adjustBrightness(base, 1.2f);
            case ATTACKING -> adjustTint(base, new Color(200, 60, 60));
            case DEFENDING -> adjustBrightness(base, 1.1f);
            case JUMPING -> adjustBrightness(base, 1.15f);
            case DASHING -> adjustTint(base, new Color(255, 120, 200));
            case HIT -> adjustTint(base, new Color(255, 180, 60));
            case KO -> Color.GRAY;
        };
    }

    private Color adjustBrightness(Color color, float factor) {
        int r = Math.min(255, Math.round(color.getRed() * factor));
        int g = Math.min(255, Math.round(color.getGreen() * factor));
        int b = Math.min(255, Math.round(color.getBlue() * factor));
        return new Color(r, g, b);
    }

    private Color adjustTint(Color base, Color tint) {
        int r = Math.min(255, (base.getRed() + tint.getRed()) / 2);
        int g = Math.min(255, (base.getGreen() + tint.getGreen()) / 2);
        int b = Math.min(255, (base.getBlue() + tint.getBlue()) / 2);
        return new Color(r, g, b);
    }

    public FighterState getState() {
        return state;
    }

    public boolean isOnGround() {
        return onGround;
    }

    public String getName() {
        return profile != null ? profile.getName() : name;
    }

    public CharacterProfile getProfile() {
        return profile;
    }

    public void setGroundFromFloorTop(float floorTopY) {
        this.groundY = floorTopY - height;
    }

    public void snapToGround() {
        this.y = groundY;
        this.yVelocity = 0f;
        this.onGround = true;
    }
}
