package view;

import java.awt.*;
import javax.swing.*;

public class StartScreen extends JPanel {
    private Tetris parentFrame;
    private final Image startBackground = Toolkit.getDefaultToolkit().getImage("image/startPage_background.png");
    private final Image Background = new ImageIcon("image/background_homepage.jpg").getImage();

    public StartScreen(Tetris frame) {
        this.parentFrame = frame;

        this.setLayout(new GridBagLayout());
        
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
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, getWidth(), getHeight());
        Graphics2D g2d = (Graphics2D) g;

        if(Background != null) {
            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.2f));
            g2d.setColor(Color.BLACK);
            g2d.drawImage(Background, 0, 0, getWidth(), getHeight(), this);
        }
        
        if (startBackground != null) {
            int imgW = 400; 
            int imgH = 300;

            int x = (getWidth() - imgW) / 2;
            int y = (getHeight() - imgH) / 2-100;

            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.7f));
            g2d.drawImage(startBackground, x, y, imgW, imgH, this);
        }
        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));
    }
}
