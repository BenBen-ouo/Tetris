package view;

import javax.swing.*;

public class Tetris extends JFrame {
    public final int WIDTH = 700, HEIGHT = 800;
    private StartScreen startScreen;
    private TetrisPanel gamePanel;

    public Tetris() {
        this.setTitle("Tetris Test");
        this.setSize(WIDTH, HEIGHT);
        this.setLayout(null); // 保持為 null 以方便定位面板
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // 初始化兩個面板
        startScreen = new StartScreen(this);
        gamePanel = new TetrisPanel();

        // 設定面板大小和位置，確保佈滿整個可見區域
        startScreen.setBounds(0, 0, WIDTH, HEIGHT);
        gamePanel.setBounds(0, 0, WIDTH, HEIGHT);
        
        // 初始時只加入並顯示 StartScreen
        this.add(startScreen);
        this.startScreen.setVisible(true);

        // 將 KeyListener 暫時不加給任何元件，直到遊戲開始 (startScreen 不需要 KeyListener)
    }
    

    public void startGame() {
        // 1. 移除主畫面
        this.remove(startScreen);
        
        // 2. 加入遊戲面板
        this.add(gamePanel);
        
        // 3. 確保 KeyListener 作用在遊戲面板上
        //    注意： KeyListener 必須加到 JFrame 或一個能獲取焦點的元件上。
        //    這裡我們直接加給 JFrame，但將事件傳遞給 gamePanel。
        this.addKeyListener(gamePanel); 
        
        // 4. 讓遊戲面板可見並重新繪製視窗
        this.gamePanel.setVisible(true);
        this.revalidate(); // 重新計算佈局
        this.repaint(); // 重新繪製畫面
        
        // 確保 JFrame 獲得焦點，以便鍵盤輸入能被捕獲
        this.requestFocusInWindow();
        
        // 啟動遊戲的 Timer (原本在 TetrisPanel 建構子中的 Timer 必須移到這裡或 TetrisPanel 的 public 方法中)
        // 假設你已經將 TetrisPanel 建構子中的 Timer 啟動邏輯移出並改為一個 startTimer() 方法：
        gamePanel.startTimer(); 
    }
    
    
    public static void main(String[] args) {
        // Swing 建議在 Event Dispatch Thread 中建立 GUI
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                Tetris gui = new Tetris();
                gui.setVisible(true);
            }
        });
    }
}