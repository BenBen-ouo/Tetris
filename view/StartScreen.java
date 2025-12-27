package view;

import javax.swing.*;
import java.awt.*;

public class StartScreen extends JPanel {
    private Tetris parentFrame;

    public StartScreen(Tetris frame) {
        this.parentFrame = frame;

        this.setLayout(new GridBagLayout());
        this.setBackground(Color.BLACK);

        JLabel titleLabel = new JLabel("TETRIS GAME");
        titleLabel.setFont(new Font("SansSerif", Font.CENTER_BASELINE, 70));
        titleLabel.setForeground(Color.WHITE);

        JButton startButton = new JButton("START GAME");
        startButton.setFont(new Font("SansSerif", Font.BOLD, 30));
        startButton.setPreferredSize(new Dimension(300, 80));
        startButton.addActionListener(e -> parentFrame.startGame());
        
        startButton.setFocusPainted(false);
        startButton.setOpaque(true); // 確保背景顏色能顯示
        startButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                startButton.setBackground(Color.DARK_GRAY); // 滑鼠進入時變色
                startButton.setForeground(Color.WHITE);
            }

            public void mouseExited(java.awt.event.MouseEvent evt) {
                startButton.setBackground(UIManager.getColor("Button.background")); // 滑鼠離開時變回原色
                startButton.setForeground(Color.BLACK);
            }
        });

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.insets = new Insets(0, 0, 50, 0);
        add(titleLabel, gbc);

        gbc.gridy = 1;
        gbc.insets = new Insets(0, 0, 0, 0);
        add(startButton, gbc);
    }
}