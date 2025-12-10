package com.tekki.core;

import java.awt.Graphics2D;

/**
 * Fighter controlled by player input.
 */
public class PlayerFighter extends Fighter {

    private boolean isDashing = false;
    private float dashSpeed = 900f;
    private float dashDuration = 0.15f;
    private float dashTimer = 0f;
    private float dashCooldown = 0.5f;
    private float dashCooldownTimer = 0f;

    public PlayerFighter(float startX, float startY) {
        super(startX, startY, 50, 100, 100);
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
    }

    @Override
    public void render(Graphics2D g2d) {
        g2d.setColor(colorForState());
        g2d.fillRect((int) x, (int) y, width, height);
    }
}
