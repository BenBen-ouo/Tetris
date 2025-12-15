package view;

import javax.swing.*;
import controller.TimerService;

public class Tetris extends JFrame {
    public final int WIDTH = 700, HEIGHT = 750;
    private StartScreen startScreen;
    private TetrisPanel gamePanel;
    private TimerService timerService;

    public Tetris() {
        this.setTitle("Tetris Test");
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
        this.add(gamePanel);
        this.addKeyListener(gamePanel); 
        
        this.gamePanel.setVisible(true);
        this.revalidate(); // 重新計算佈局
        this.repaint(); // 重新繪製畫面
        this.requestFocusInWindow();

        // 開始由外部計時器驅動遊戲
        timerService.start();
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