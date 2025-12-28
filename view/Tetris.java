package view;

import javax.swing.*;
import controller.TimerService;

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
        if (timerService == null) {
            timerService = new TimerService(1000, () -> gamePanel.tick());
        }
        if (gamePanel != null) {
            gamePanel.resetGame(); 
        }
        this.add(gamePanel);
        this.addKeyListener(gamePanel); 
        this.revalidate();
        this.repaint();
        this.requestFocusInWindow();

        gamePanel.startGameFlow(() -> {
            if (timerService != null) {
                timerService.start();
            }
        });
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