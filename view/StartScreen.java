package view;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class StartScreen extends JPanel {
    private Tetris parentFrame;

    public StartScreen(Tetris frame) {
        this.parentFrame = frame;

        this.setLayout(new GridBagLayout());
        this.setBackground(Color.DARK_GRAY);

        JLabel titleLabel = new JLabel("TETRIS GAME");
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 70));
        titleLabel.setForeground(Color.WHITE);

        JButton startButton = new JButton("START GAME");
        startButton.setFont(new Font("SansSerif", Font.BOLD, 30));
        startButton.setPreferredSize(new Dimension(300, 80));

        startButton.addActionListener(e -> parentFrame.startGame());

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
