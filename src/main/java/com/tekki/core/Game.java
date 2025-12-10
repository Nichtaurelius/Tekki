package com.tekki.core;

import java.awt.EventQueue;
import javax.swing.JFrame;

/**
 * Entry point that sets up the main window and adds the GamePanel.
 */
public class Game {

    public static void main(String[] args) {
        // Use EventQueue.invokeLater to respect Swing threading rules.
        EventQueue.invokeLater(() -> {
            JFrame window = new JFrame("Tekki - Step 0");
            window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

            // Add our custom panel that handles rendering and updates.
            GamePanel panel = new GamePanel();
            window.setContentPane(panel);
            window.pack();
            window.setLocationRelativeTo(null);
            window.setResizable(false);
            window.setVisible(true);
        });
    }
}
