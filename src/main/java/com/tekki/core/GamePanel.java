package com.tekki.core;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import javax.swing.JPanel;
import javax.swing.Timer;

/**
 * GamePanel hosts rendering and update loop using a Swing Timer.
 * For STEP 0 it simply shows a background and start text.
 */
public class GamePanel extends JPanel implements ActionListener, KeyListener {

    private static final int PANEL_WIDTH = 960;
    private static final int PANEL_HEIGHT = 540;
    private static final int TARGET_FPS = 60;

    private final Timer gameTimer;
    private long frameCounter = 0;
    private GameState gameState = GameState.MENU;

    public GamePanel() {
        setPreferredSize(new Dimension(PANEL_WIDTH, PANEL_HEIGHT));
        setBackground(Color.DARK_GRAY);
        setFocusable(true);
        addKeyListener(this);

        // Start a Swing Timer to call actionPerformed ~60 times per second.
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

        // Enable antialiasing for smoother text.
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        // Fill background for now; later we will add level art.
        g2d.setColor(new Color(30, 40, 60));
        g2d.fillRect(0, 0, getWidth(), getHeight());

        // Draw placeholder UI based on the current game state.
        g2d.setFont(new Font("SansSerif", Font.BOLD, 32));
        switch (gameState) {
            case MENU -> drawMenu(g2d);
            case FIGHT -> drawCenteredText(g2d, "Fight!", Color.WHITE);
            case LEVEL_TRANSITION -> drawCenteredText(g2d, "Level Transition...", Color.YELLOW);
            case GAME_OVER -> drawCenteredText(g2d, "Game Over - Press ENTER", Color.RED);
            case VICTORY -> drawCenteredText(g2d, "Victory!", new Color(0, 200, 0));
            default -> drawMenu(g2d);
        }

        g2d.dispose();
    }

    /**
     * Called by the Swing Timer to progress the game and request repaint.
     */
    @Override
    public void actionPerformed(ActionEvent e) {
        frameCounter++;
        repaint();
    }

    /**
     * Draws the menu with a blinking prompt.
     */
    private void drawMenu(Graphics2D g2d) {
        String prompt = "Press ENTER to start";
        int textWidth = g2d.getFontMetrics().stringWidth(prompt);
        int x = (getWidth() - textWidth) / 2;
        int y = getHeight() / 2;

        // Alternate between two colors every half second for a subtle animation.
        if ((frameCounter / (TARGET_FPS / 2)) % 2 == 0) {
            g2d.setColor(Color.WHITE);
        } else {
            g2d.setColor(new Color(200, 200, 255));
        }
        g2d.drawString(prompt, x, y);
    }

    /**
     * Utility to draw centered text with a specific color.
     */
    private void drawCenteredText(Graphics2D g2d, String text, Color color) {
        int textWidth = g2d.getFontMetrics().stringWidth(text);
        int x = (getWidth() - textWidth) / 2;
        int y = getHeight() / 2;
        g2d.setColor(color);
        g2d.drawString(text, x, y);
    }

    @Override
    public void keyTyped(KeyEvent e) {
        // Not used in this step.
    }

    @Override
    public void keyPressed(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_ENTER) {
            if (gameState == GameState.MENU) {
                gameState = GameState.FIGHT;
            } else if (gameState == GameState.GAME_OVER || gameState == GameState.VICTORY) {
                gameState = GameState.MENU;
            }
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        // Not used in this step.
    }
}
