package com.tekki.core;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JPanel;
import javax.swing.Timer;

/**
 * GamePanel hosts rendering and update loop using a Swing Timer.
 */
public class GamePanel extends JPanel implements ActionListener, KeyListener {

    private static final int PANEL_WIDTH = 960;
    private static final int PANEL_HEIGHT = 540;
    private static final int TARGET_FPS = 60;
    private static final int FLOOR_HEIGHT = 60;

    private final Timer gameTimer;
    private long frameCounter = 0;
    private GameState gameState = GameState.MENU;

    private int score = 0;

    private List<Level> levels = new ArrayList<>();
    private int currentLevelIndex = 0;
    private Level currentLevel;
    private float levelTransitionTimer = 0f;
    private float stageIntroTimer = 0f;
    private boolean showingStageIntro = false;
    private static final float STAGE_INTRO_DURATION = 2.0f;
    private float hitFlashTimer = 0f;
    private static final float HIT_FLASH_DURATION = 0.15f;
    private Color hitFlashColor = Color.WHITE;
    private float koOverlayTimer = 0f;
    private static final float KO_OVERLAY_DURATION = 1.5f;

    private PlayerFighter player;
    private EnemyFighter enemy;

    private boolean leftPressed;
    private boolean rightPressed;
    private boolean attackPressed;
    private boolean defendPressed;
    private boolean jumpPressed;

    public GamePanel() {
        setPreferredSize(new Dimension(PANEL_WIDTH, PANEL_HEIGHT));
        setBackground(Color.DARK_GRAY);
        setFocusable(true);
        addKeyListener(this);

        initLevels();
        currentLevel = levels.get(0);

        int delayMs = 1000 / TARGET_FPS;
        gameTimer = new Timer(delayMs, this);
        gameTimer.start();
    }

    @Override
    public void addNotify() {
        super.addNotify();
        requestFocusInWindow();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g.create();

        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        g2d.setColor(new Color(30, 40, 60));
        g2d.fillRect(0, 0, getWidth(), getHeight());

        g2d.setFont(new Font("SansSerif", Font.BOLD, 32));
        switch (gameState) {
            case MENU -> drawMenu(g2d);
            case FIGHT -> drawFight(g2d);
            case LEVEL_TRANSITION -> drawLevelTransition(g2d);
            case GAME_OVER -> drawGameOver(g2d);
            case VICTORY -> drawVictory(g2d);
            default -> drawMenu(g2d);
        }

        g2d.dispose();
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        frameCounter++;
        float deltaTime = 1f / TARGET_FPS;
        updateGame(deltaTime);
        repaint();
    }

    private void updateGame(float deltaTime) {
        if (hitFlashTimer > 0f) {
            hitFlashTimer -= deltaTime;
            if (hitFlashTimer < 0f) {
                hitFlashTimer = 0f;
            }
        }
        if (koOverlayTimer > 0f) {
            koOverlayTimer -= deltaTime;
            if (koOverlayTimer < 0f) {
                koOverlayTimer = 0f;
            }
        }
        if (showingStageIntro && gameState == GameState.FIGHT) {
            stageIntroTimer -= deltaTime;
            if (stageIntroTimer <= 0f) {
                showingStageIntro = false;
                stageIntroTimer = 0f;
            }
        }

        if (gameState == GameState.FIGHT) {
            if (player == null || enemy == null) {
                startLevel(currentLevelIndex);
            }

            if (!player.isKO() && !enemy.isKO()) {
                if (defendPressed) {
                    player.startDefending();
                    attackPressed = false;
                    jumpPressed = false;
                    player.stopMoving();
                } else {
                    player.stopDefending();

                    if (player.getState() != FighterState.DASHING) {
                        if (leftPressed && !rightPressed) {
                            player.moveLeft();
                        } else if (rightPressed && !leftPressed) {
                            player.moveRight();
                        } else {
                            player.stopMoving();
                        }

                        if (jumpPressed) {
                            player.jump();
                            jumpPressed = false;
                        }

                        if (attackPressed && player.getState() != FighterState.JUMPING && player.getState() != FighterState.DASHING && player.getState() != FighterState.DEFENDING) {
                            player.startAttack();
                            attackPressed = false;
                        }
                    } else {
                        attackPressed = false;
                        jumpPressed = false;
                    }
                }

                player.update(deltaTime);
                enemy.updateAI(deltaTime, player);
                enemy.update(deltaTime);
                handleCombat();
            }
        } else if (gameState == GameState.LEVEL_TRANSITION) {
            levelTransitionTimer -= deltaTime;
            if (levelTransitionTimer <= 0f) {
                int nextIndex = currentLevelIndex + 1;
                if (nextIndex < levels.size()) {
                    startLevel(nextIndex);
                    gameState = GameState.FIGHT;
                } else {
                    gameState = GameState.VICTORY;
                }
            }
        }
    }

    private void handleCombat() {
        if (player == null || enemy == null) {
            return;
        }

        Rectangle playerHit = player.getAttackHitbox();
        Rectangle enemyHit = enemy.getAttackHitbox();
        Rectangle playerBounds = player.getBounds();
        Rectangle enemyBounds = enemy.getBounds();

        if (player.canHit() && playerHit != null && playerHit.intersects(enemyBounds)) {
            int damage = 10;
            if (player.getState() == FighterState.DASHING) {
                damage *= 2;
            }
            enemy.takeDamage(damage);
            player.markHit();
            score += damage;
            hitFlashTimer = HIT_FLASH_DURATION;
            hitFlashColor = new Color(180, 0, 180);
        }

        if (enemy.canHit() && enemyHit != null && enemyHit.intersects(playerBounds)) {
            int enemyDamage = currentLevel != null ? currentLevel.getEnemyDamage() : 10;
            player.takeDamage(enemyDamage);
            enemy.markHit();
            hitFlashTimer = HIT_FLASH_DURATION;
            hitFlashColor = Color.WHITE;
        }

        if (gameState == GameState.FIGHT) {
            if (enemy.isKO()) {
                koOverlayTimer = KO_OVERLAY_DURATION;
                if (currentLevelIndex + 1 < levels.size()) {
                    gameState = GameState.LEVEL_TRANSITION;
                    levelTransitionTimer = 2.0f;
                } else {
                    gameState = GameState.VICTORY;
                }
            } else if (player.isKO()) {
                koOverlayTimer = KO_OVERLAY_DURATION;
                gameState = GameState.GAME_OVER;
            }
        }
    }

    private void drawMenu(Graphics2D g2d) {
        g2d.setColor(Color.BLACK);
        g2d.fillRect(0, 0, getWidth(), getHeight());

        g2d.setColor(new Color(0, 0, 0, 120));
        g2d.fillRect(0, 0, getWidth(), getHeight());

        g2d.setFont(new Font("SansSerif", Font.BOLD, 72));
        drawCenteredTextAt(g2d, "JAVA BRAWL", Color.WHITE, 150);

        g2d.setFont(new Font("SansSerif", Font.BOLD, 32));
        String prompt = "Press ENTER to Start";
        Color promptColor = (frameCounter / (TARGET_FPS / 2)) % 2 == 0 ? Color.WHITE : new Color(200, 200, 255);
        drawCenteredTextAt(g2d, prompt, promptColor, 260);

        g2d.setFont(new Font("SansSerif", Font.PLAIN, 16));
        drawCenteredTextAt(g2d, "© 2025 Student Project – Prototype Build", new Color(200, 200, 200), getHeight() - 30);
    }

    private void drawFight(Graphics2D g2d) {
        Color bg = currentLevel != null ? currentLevel.getBackgroundColor() : new Color(50, 70, 90);
        Color floor = currentLevel != null ? currentLevel.getFloorColor() : new Color(80, 60, 40);

        g2d.setColor(bg);
        g2d.fillRect(0, 0, getWidth(), getHeight());

        g2d.setColor(floor);
        int floorTopY = PANEL_HEIGHT - FLOOR_HEIGHT;
        g2d.fillRect(0, floorTopY, getWidth(), FLOOR_HEIGHT);

        if (player != null) {
            player.render(g2d);
        }
        if (enemy != null) {
            enemy.render(g2d);
        }

        if (showingStageIntro && currentLevel != null) {
            drawStageIntro(g2d);
        }

        drawHud(g2d);

        if (hitFlashTimer > 0f) {
            drawHitFlash(g2d);
        }
    }

    private void drawHud(Graphics2D g2d) {
        int barWidth = 300;
        int barHeight = 20;
        int padding = 18;
        int topMargin = 40;
        int textSpacing = 18;

        g2d.setColor(Color.DARK_GRAY);
        g2d.fillRect(padding, topMargin, barWidth, barHeight);
        g2d.fillRect(getWidth() - barWidth - padding, topMargin, barWidth, barHeight);

        if (player != null) {
            float ratio = player.getHealth() / (float) player.getMaxHealth();
            int fill = (int) (barWidth * ratio);
            g2d.setColor(new Color(80, 200, 120));
            g2d.fillRect(padding, topMargin, fill, barHeight);
            g2d.setColor(Color.WHITE);
            g2d.drawString(player.getName() + " HP: " + player.getHealth() + "/" + player.getMaxHealth(), padding, topMargin + barHeight + textSpacing);
        }

        if (enemy != null) {
            float ratio = enemy.getHealth() / (float) enemy.getMaxHealth();
            int fill = (int) (barWidth * ratio);
            g2d.setColor(new Color(200, 120, 80));
            g2d.fillRect(getWidth() - barWidth - padding, topMargin, fill, barHeight);
            g2d.setColor(Color.WHITE);
            g2d.drawString("CPU " + enemy.getName() + " HP: " + enemy.getHealth() + "/" + enemy.getMaxHealth(), getWidth() - barWidth - padding, topMargin + barHeight + textSpacing);
        }

        g2d.setColor(Color.WHITE);
        if (currentLevel != null && !levels.isEmpty()) {
            String scoreLabel = "Score: " + score;
            int scoreWidth = g2d.getFontMetrics().stringWidth(scoreLabel);
            int scoreX = (getWidth() - scoreWidth) / 2;
            int scoreY = topMargin + barHeight + 10;
            g2d.drawString(scoreLabel, scoreX, scoreY);

            String stageLabel = "Stage " + (currentLevelIndex + 1) + "/" + levels.size() + " – " + currentLevel.getName();
            int stageWidth = g2d.getFontMetrics().stringWidth(stageLabel);
            int stageX = (getWidth() - stageWidth) / 2;
            int stageY = scoreY + textSpacing;
            g2d.drawString(stageLabel, stageX, stageY);
        }

        if (player != null) {
            drawDashIndicator(g2d, padding, getHeight() - 70, barWidth / 2, 12);
        }
    }

    private void drawStageIntro(Graphics2D g2d) {
        float alpha = Math.min(1f, stageIntroTimer / STAGE_INTRO_DURATION);
        Color overlay = new Color(0f, 0f, 0f, 0.5f * alpha);
        g2d.setColor(overlay);
        g2d.fillRect(0, 0, getWidth(), getHeight());
        String label = "Stage " + (currentLevelIndex + 1) + "/" + levels.size() + " - " + currentLevel.getName();
        g2d.setFont(new Font("SansSerif", Font.BOLD, 42));
        drawCenteredText(g2d, label, Color.WHITE);
    }

    private void drawHitFlash(Graphics2D g2d) {
        float alpha = Math.min(1f, hitFlashTimer / HIT_FLASH_DURATION);
        float flashAlpha = 0.35f * alpha;
        float r = hitFlashColor.getRed() / 255f;
        float g = hitFlashColor.getGreen() / 255f;
        float b = hitFlashColor.getBlue() / 255f;
        Color flash = new Color(r, g, b, flashAlpha);
        g2d.setColor(flash);
        g2d.fillRect(0, 0, getWidth(), getHeight());
    }

    private void drawLevelTransition(Graphics2D g2d) {
        g2d.setColor(new Color(60, 60, 30));
        g2d.fillRect(0, 0, getWidth(), getHeight());
        g2d.setFont(new Font("SansSerif", Font.BOLD, 42));
        String next = currentLevelIndex + 1 < levels.size() ? levels.get(currentLevelIndex + 1).getName() : "";
        String message = "Next Stage: " + next;
        drawCenteredText(g2d, message, Color.WHITE);
        g2d.setFont(new Font("SansSerif", Font.PLAIN, 22));
        drawCenteredTextOffset(g2d, "Get Ready...", Color.LIGHT_GRAY, 40);
    }

    private void drawGameOver(Graphics2D g2d) {
        g2d.setColor(new Color(80, 20, 30));
        g2d.fillRect(0, 0, getWidth(), getHeight());

        g2d.setFont(new Font("SansSerif", Font.BOLD, 64));
        if (koOverlayTimer > 0f) {
            drawKOOverlay(g2d);
        }
        drawCenteredText(g2d, "YOU LOSE", Color.WHITE);
        g2d.setFont(new Font("SansSerif", Font.PLAIN, 24));
        drawCenteredTextOffset(g2d, "Final Score: " + score, Color.LIGHT_GRAY, 40);
        drawCenteredTextOffset(g2d, "Press ENTER to return to menu", Color.WHITE, 80);
    }

    private void drawVictory(Graphics2D g2d) {
        g2d.setColor(new Color(20, 80, 60));
        g2d.fillRect(0, 0, getWidth(), getHeight());

        g2d.setFont(new Font("SansSerif", Font.BOLD, 64));
        if (koOverlayTimer > 0f) {
            drawKOOverlay(g2d);
        }
        drawCenteredText(g2d, "YOU WIN", Color.WHITE);
        g2d.setFont(new Font("SansSerif", Font.PLAIN, 24));
        drawCenteredTextOffset(g2d, "Final Score: " + score, Color.LIGHT_GRAY, 40);
        drawCenteredTextOffset(g2d, "Press ENTER to return to menu", Color.WHITE, 80);
    }

    private void drawKOOverlay(Graphics2D g2d) {
        float t = Math.min(1f, koOverlayTimer / KO_OVERLAY_DURATION);
        int size = (int) (120 + 60 * t);
        g2d.setFont(new Font("SansSerif", Font.BOLD, size));
        drawCenteredText(g2d, "KO", new Color(255, 180, 80));
    }

    private void drawCenteredText(Graphics2D g2d, String text, Color color) {
        int textWidth = g2d.getFontMetrics().stringWidth(text);
        int x = (getWidth() - textWidth) / 2;
        int y = getHeight() / 2;
        g2d.setColor(color);
        g2d.drawString(text, x, y);
    }

    private void drawCenteredTextAt(Graphics2D g2d, String text, Color color, int y) {
        int textWidth = g2d.getFontMetrics().stringWidth(text);
        int x = (getWidth() - textWidth) / 2;
        g2d.setColor(color);
        g2d.drawString(text, x, y);
    }

    private void drawCenteredTextOffset(Graphics2D g2d, String text, Color color, int yOffset) {
        int textWidth = g2d.getFontMetrics().stringWidth(text);
        int x = (getWidth() - textWidth) / 2;
        int y = (getHeight() / 2) + yOffset;
        g2d.setColor(color);
        g2d.drawString(text, x, y);
    }

    private void drawDashIndicator(Graphics2D g2d, int x, int y, int width, int height) {
        if (player == null) {
            return;
        }
        g2d.setColor(Color.DARK_GRAY);
        g2d.fillRect(x, y, width, height);
        if (player.getDashCooldown() > 0f) {
            float remaining = Math.max(0f, player.getDashCooldownTimer());
            float ratio = 1f - Math.min(1f, remaining / player.getDashCooldown());
            int fill = (int) (width * ratio);
            g2d.setColor(player.isDashReady() ? new Color(100, 220, 255) : new Color(120, 120, 120));
            g2d.fillRect(x, y, fill, height);
        }
        g2d.setColor(Color.WHITE);
        String label = player.isDashReady() ? "Dash: READY" : "Dash: COOLDOWN";
        g2d.drawString(label, x, y + height + 14);
    }

    private void initLevels() {
        levels.clear();
        levels.add(new Level("Dojo", new Color(50, 70, 90), new Color(90, 70, 50), 1.0f, 1.0f, false, 10));
        levels.add(new Level("Rooftop", new Color(40, 40, 90), new Color(80, 80, 90), 2.0f, 2.2f, true, 20));
    }

    private void startLevel(int levelIndex) {
        currentLevelIndex = levelIndex;
        currentLevel = levels.get(currentLevelIndex);
        CharacterProfile playerProfile = new CharacterProfile("Player 1", new Color(70, 140, 255), null);
        CharacterProfile enemyProfile = currentLevelIndex == 0
                ? new CharacterProfile("CPU Dojo", new Color(210, 100, 190), null)
                : new CharacterProfile("CPU Rooftop", new Color(240, 120, 80), null);
        float floorTopY = PANEL_HEIGHT - FLOOR_HEIGHT;

        player = new PlayerFighter(120f, 0f, playerProfile);
        player.setGroundFromFloorTop(floorTopY);
        player.snapToGround();

        enemy = new EnemyFighter(PANEL_WIDTH - 220f, 0f, currentLevel.getEnemySpeedMultiplier(),
                currentLevel.getEnemyAggression(), currentLevel.isEnemyDashesMore(), enemyProfile);
        enemy.setGroundFromFloorTop(floorTopY);
        enemy.snapToGround();
        leftPressed = false;
        rightPressed = false;
        attackPressed = false;
        defendPressed = false;
        jumpPressed = false;
        showingStageIntro = true;
        stageIntroTimer = STAGE_INTRO_DURATION;
    }

    private void resetToMenu() {
        gameState = GameState.MENU;
        score = 0;
        player = null;
        enemy = null;
        leftPressed = false;
        rightPressed = false;
        attackPressed = false;
        defendPressed = false;
        jumpPressed = false;
        currentLevelIndex = 0;
        currentLevel = levels.get(0);
        levelTransitionTimer = 0f;
        stageIntroTimer = 0f;
        showingStageIntro = false;
        hitFlashTimer = 0f;
        koOverlayTimer = 0f;
    }

    @Override
    public void keyTyped(KeyEvent e) {
    }

    @Override
    public void keyPressed(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_ENTER) {
            if (gameState == GameState.MENU) {
                score = 0;
                currentLevelIndex = 0;
                currentLevel = levels.get(0);
                player = null;
                enemy = null;
                levelTransitionTimer = 0f;
                gameState = GameState.FIGHT;
            } else if (gameState == GameState.GAME_OVER || gameState == GameState.VICTORY) {
                resetToMenu();
            } else if (gameState == GameState.LEVEL_TRANSITION) {
                levelTransitionTimer = 0f;
            }
        }

        if (gameState != GameState.FIGHT) {
            return;
        }

        if (e.getKeyCode() == KeyEvent.VK_A || e.getKeyCode() == KeyEvent.VK_LEFT) {
            leftPressed = true;
        }
        if (e.getKeyCode() == KeyEvent.VK_D || e.getKeyCode() == KeyEvent.VK_RIGHT) {
            rightPressed = true;
        }
        if (e.getKeyCode() == KeyEvent.VK_J) {
            attackPressed = true;
        }
        if (e.getKeyCode() == KeyEvent.VK_K) {
            defendPressed = true;
        }
        if (e.getKeyCode() == KeyEvent.VK_W || e.getKeyCode() == KeyEvent.VK_UP) {
            jumpPressed = true;
        }
        if (e.getKeyCode() == KeyEvent.VK_SHIFT) {
            if (player != null) {
                player.startDash();
            }
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        if (gameState != GameState.FIGHT) {
            return;
        }
        if (e.getKeyCode() == KeyEvent.VK_A || e.getKeyCode() == KeyEvent.VK_LEFT) {
            leftPressed = false;
        }
        if (e.getKeyCode() == KeyEvent.VK_D || e.getKeyCode() == KeyEvent.VK_RIGHT) {
            rightPressed = false;
        }
        if (e.getKeyCode() == KeyEvent.VK_K) {
            defendPressed = false;
        }
        if (e.getKeyCode() == KeyEvent.VK_W || e.getKeyCode() == KeyEvent.VK_UP) {
            jumpPressed = false;
        }
    }
}
