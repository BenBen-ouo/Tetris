package view;

import javax.swing.*;
import controller.TimerService;
import java.awt.*;


public class Tetris extends JFrame {
    public final int WIDTH = 1140, HEIGHT = 760;
    private StartScreen startScreen;
    private TetrisPanel gamePanel;
    private TimerService timerService;

    public Tetris() {
        this.setTitle("Tetris");
        this.setSize(WIDTH, HEIGHT);
        this.setLayout(null);
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        startScreen = new StartScreen(this);
        gamePanel = new TetrisPanel();
        timerService = new TimerService(1000, () -> gamePanel.tick());

        startScreen.setBounds(0, 0, WIDTH, HEIGHT);
        gamePanel.setBounds(0, 0, WIDTH, HEIGHT);
        
        this.add(startScreen);
        this.startScreen.setVisible(true);
    }
    

    public void startGame() {
        this.remove(startScreen);
    
        if (timerService != null) {
            timerService.stop(); 
        }

        // 重置遊戲面板與控制器
        gamePanel.resetGame(); 
    
        // 重新定義計時器邏輯
        timerService = new TimerService(1000, () -> {
        // 倒數結束前 (countdown < 5)，計時器不應該跑 tick
            if (gamePanel.getCountdown() < 5) return;

            gamePanel.tick();
            if (gamePanel.isGameOver()) {
                triggerGameOverManually();
            }
        });

        this.add(gamePanel);
        this.addKeyListener(gamePanel); 
        this.revalidate();
        this.repaint();
        this.requestFocusInWindow();

        // 倒數完才啟動計時器
        gamePanel.startGameFlow(() -> {
            if (timerService != null) {
                timerService.start();
            }
        });
    }

    private void handleGameOver() {
        if (timerService != null) timerService.stop();
        this.removeKeyListener(gamePanel); 

        JDialog gameOverDlg = new JDialog(this, true);
        gameOverDlg.setUndecorated(true); 

        // 1. 設定主面板，並加上更粗、更黑的邊框 (這裡設為 8 像素)
        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new GridLayout(2, 1, 0, 0)); // 改為 0 間距，手動控制邊距
        contentPanel.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY, 10)); 
        contentPanel.setBackground(Color.WHITE);

        // 2. 文字標籤：加上 EmptyBorder 來把文字往下推
        // BorderFactory.createEmptyBorder(上, 左, 下, 右)
        JLabel label = new JLabel("Game Over！", SwingConstants.CENTER);
        label.setFont(new Font("Microsoft JhengHei", Font.BOLD, 18));
        label.setForeground(Color.BLACK);
        label.setBorder(BorderFactory.createEmptyBorder(20, 0, 0, 0)); // 頂部增加 20 像素間距

        JButton backBtn = new JButton("Back");    
        backBtn.setFocusable(false);
        backBtn.setPreferredSize(new Dimension(150, 40)); // 稍微調整按鈕大小

        JPanel btnPanel = new JPanel();
        btnPanel.setBackground(Color.WHITE);
        // 也可以給按鈕面板加點上方間距，讓它不要離文字太近
        btnPanel.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
        btnPanel.add(backBtn);
        backBtn.addActionListener(e -> {
            gameOverDlg.dispose();
            showStartScreen();
        });

        contentPanel.add(label);
        contentPanel.add(btnPanel);
    
        gameOverDlg.add(contentPanel);
        gameOverDlg.setSize(320, 180); // 稍微加寬加高，讓加粗的邊框不會擠壓到內容
        gameOverDlg.setLocationRelativeTo(this);
    
        gameOverDlg.setVisible(true);
    }

    public void showStartScreen() {
        if (timerService != null) {
            timerService.stop();
            timerService = null; 
        }

        this.removeKeyListener(gamePanel);

        getContentPane().removeAll();

        startScreen = new StartScreen(this);
        startScreen.setBounds(0, 0, WIDTH, HEIGHT); 
        add(startScreen);

        revalidate(); 
        repaint();
        startScreen.requestFocusInWindow();
    }
    public void triggerGameOverManually() {
        if (timerService != null) {
            timerService.stop();
        }
        handleGameOver();
    }

    public static void main(String[] args) {
        // Swing 建議在 Event Dispatch Thread 中建立 GUI
        SwingUtilities.invokeLater(new Runnable() {//在事件分派執行緒中執行，run方法內建立並顯示Tetris GUI
            @Override
            public void run() {
                Tetris gui = new Tetris();
                gui.setVisible(true);
            }
        });
    }
}