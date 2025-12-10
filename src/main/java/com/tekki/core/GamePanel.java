package com.tekki.core;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JPanel;
import javax.swing.Timer;

/**
 * GamePanel hosts rendering and update loop using a Swing Timer.
 * For STEP 0 it simply shows a background and start text.
 */
public class GamePanel extends JPanel implements ActionListener {

    private static final int PANEL_WIDTH = 960;
    private static final int PANEL_HEIGHT = 540;
    private static final int TARGET_FPS = 60;

    private final Timer gameTimer;
    private long frameCounter = 0;

    public GamePanel() {
        setPreferredSize(new Dimension(PANEL_WIDTH, PANEL_HEIGHT));
        setBackground(Color.DARK_GRAY);

        // Start a Swing Timer to call actionPerformed ~60 times per second.
        int delayMs = 1000 / TARGET_FPS;
        gameTimer = new Timer(delayMs, this);
        gameTimer.start();
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

        // Draw a simple flashing "Press ENTER" prompt to show the loop is alive.
        g2d.setFont(new Font("SansSerif", Font.BOLD, 32));
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
}
