package view;

import controller.GameController;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.List;
import javax.swing.*;
import model.Board;
import model.Tetromino;

public final class TetrisPanel extends JPanel implements KeyListener { //面板邏輯
    public int[][] map = new int [10][20]; // 10寬 20高（初始化後會指向 Board 的 map）
    private Board board; // 盤面資料來源
    // 轉為由 GameController 管理狀態
    private GameController controller;
    private int blockType; // 暫存繪製使用（由 controller 取得）
    private int turnState; // 暫存繪製使用（由 controller 取得）
    private int x, y, hold, next; // 暫存繪製（由 controller 取得）
    private int flag = 0; // 與舊程式相容（由 controller 提供）
    static Image currentImg = null;
    private final Image b1;
    private final Image b2;
    private final Image holdPhoto;
    private final Image nextPhoto;
    private final Image startPhoto;
    private final Image backgroundImage;
    private final Image img3, img2, img1, imgGo;
    int countdown = -1;// -1表示倒數還沒有開始
    private long startTime;
    private float alpha = 1.0f;  
    private final int TOTAL_W = 660;
    private int nextSpacing = 100;
    private JButton homeButton;

    // 方塊顏色圖片陣列
    private final Image[] color = new Image[7];

    public TetrisPanel() {
        this.setLayout(null);
        this.setBackground(Color.BLACK);

        b1 = Toolkit.getDefaultToolkit().getImage("image/background1.png");
        b2 = Toolkit.getDefaultToolkit().getImage("image/background2.png");
        holdPhoto = Toolkit.getDefaultToolkit().getImage("image/tetris_grid_Hold.png");
        nextPhoto = Toolkit.getDefaultToolkit().getImage("image/tetris_grid_Next.png");
        startPhoto = Toolkit.getDefaultToolkit().getImage("image/custom_game.png");
        startTime = System.currentTimeMillis();
        img1 = Toolkit.getDefaultToolkit().getImage("image/countdown_one.png");
        img2 = Toolkit.getDefaultToolkit().getImage("image/countdown_two.png");
        img3 = Toolkit.getDefaultToolkit().getImage("image/countdown_three.png");
        imgGo = Toolkit.getDefaultToolkit().getImage("image/countdown_go.png");
        backgroundImage = Toolkit.getDefaultToolkit().getImage("image/background_gamepage.jpg");
        color[0] = Toolkit.getDefaultToolkit().getImage("image/blue.png");
        color[1] = Toolkit.getDefaultToolkit().getImage("image/green.png");
        color[2] = Toolkit.getDefaultToolkit().getImage("image/red.png");
        color[3] = Toolkit.getDefaultToolkit().getImage("image/deepblue.png");
        color[4] = Toolkit.getDefaultToolkit().getImage("image/yellow.png");
        color[5] = Toolkit.getDefaultToolkit().getImage("image/orange.png");
        color[6] = Toolkit.getDefaultToolkit().getImage("image/pink.png");
        
        board = new Board();
        map = board.getMap();
        controller = new GameController(board);
        hold = controller.getHold();
        next = controller.getNext();

        homeButton = new JButton("Home");
        homeButton.setFont(new Font("SansSerif", Font.BOLD, 12));
        homeButton.setBounds(10, 10, 80, 50);
        homeButton.setVisible(false);
        homeButton.addActionListener(e -> {

        Tetris frame = (Tetris) SwingUtilities.getWindowAncestor(this);
            frame.showStartScreen();
        });
        homeButton.setFocusPainted(false);
        homeButton.setOpaque(true); // 確保背景顏色能顯示
        homeButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                homeButton.setBackground(Color.DARK_GRAY); // 滑鼠進入時變色
                homeButton.setForeground(Color.WHITE);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                homeButton.setBackground(UIManager.getColor("Button.background")); // 滑鼠離開時變回原色
                homeButton.setForeground(Color.BLACK);
            }
        });
        this.add(homeButton);
    }

    // 與控制器同步狀態（供繪製與既有流程使用）
    private void syncStateFromController() {
        blockType = controller.getBlockType();
        turnState = controller.getTurnState();
        x = controller.getX();
        y = controller.getY();
        flag = controller.getFlag();
        next = controller.getNext();
        hold = controller.getHold();
    }

    // 外部計時器呼叫本方法以驅動遊戲邏輯
    public void tick() {
        controller.tick();
        syncStateFromController();
        repaint();
    }

    void initMap() { //少用，若用可用syncStateFromController
        board.initMap();
        map = board.getMap();
    }
    
    public void newBlock() { //少用，若用可用syncStateFromController
        controller.newBlock();
        syncStateFromController();
        repaint();
    }

    public void setBlock(int x, int y, int type, int state) {// 固定方塊到地圖上
        flag = 1;
        GameController.setBlock(board, x, y, type, state);
    }

    public int gameOver(int x, int y) {// 判斷遊戲是否結束
        if(blow(x, y, blockType, turnState) == 0)
            return 1;
        return 0;
    }

    public int blow(int x, int y, int type, int state) {
        return GameController.canPlace(board, x, y, type, state);
    }

    public int down_shift() {
        int canDown = controller.down_shift();
        // 若剛固定並產生新方塊，控制器已處理清行與 newBlock
        syncStateFromController();
        repaint();
        return canDown;
    }

    void delLine() {
        GameController.clearFullLines(board);
        // /*if(access == 1) Sleep(500);*/ 保留暫不實作
    }

    public void resetAnimation() {
    // 將 startTime 更新為「現在」，並把透明度還原
        this.startTime = System.currentTimeMillis();
        this.alpha = 1.0f;
        repaint();
    }
    public int getCountdown() {
        return this.countdown;
    }

    public void startGameFlow(Runnable onFinished) {
        this.startTime = System.currentTimeMillis();
        this.alpha = 1.0f;
        this.countdown = -1; // 還沒進入 321 倒數
        repaint();

        Timer bannerTimer = new Timer(2000, e -> {
            startCountdown(onFinished); 
        });
        bannerTimer.setRepeats(false); // 透過這行設定讓它只跑一次
        bannerTimer.start();
    }

    private void startCountdown(Runnable onFinished) {
        this.countdown = 0;
    
        Timer countTimer = new Timer(1000, e -> {
            countdown++;
            if(countdown == 4){
                // homeButton.setVisible(true);
                repaint();
                onFinished.run();   
            }
            else if(countdown == 5){
                ((Timer)e.getSource()).stop(); 
                homeButton.setVisible(true);
                repaint();
            }
            else{repaint();}
        });
        countTimer.start();
    }
    public void resetGame() {
        this.board = new Board();
        this.map = board.getMap();
        this.controller = new GameController(this.board);
    
        syncStateFromController();

        this.countdown = -1;
        this.alpha = 1.0f;
        this.startTime = System.currentTimeMillis();
    
        // 重置遊戲時，隱藏 Home 按鈕 (等待倒數後才出現)
        if (homeButton != null) {
            homeButton.setVisible(false);
        }
        repaint();
    }
        // 在 TetrisPanel.java 類別中新增
    public boolean isGameOver() {
        return controller.getFlag() == 1;
    }
    
    
    @Override
    public void paintComponent(Graphics graphics) {
        int offsetX = (getWidth() - TOTAL_W) / 2;
        int offsetY = 25;
        super.paintComponent(graphics);
        Graphics2D g2d = (Graphics2D) graphics;

        graphics.drawImage(holdPhoto, offsetX, offsetY, 150, 148, this);
        graphics.drawImage(nextPhoto, 500 + offsetX, offsetY, 179, 547, this);

        Composite oldComposite = g2d.getComposite();
        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.2f));
        g2d.drawImage(backgroundImage, 0, 0, getWidth(), getHeight(), this);
        g2d.setComposite(oldComposite);

        for(int i = 0; i < 10; i++) {
            for(int j = 0; j < 20; j++) {
                if(map[i][j] == 0) {
                    if((i+j)%2 == 0)
                        graphics.drawImage(b1, i*30+3*(i+1)+150+offsetX, j*30+3*(j+1)+offsetY, null);
                    else
                        graphics.drawImage(b2, i*30+3*(i+1)+150+offsetX, j*30+3*(j+1)+offsetY, null);
                } else
                    graphics.drawImage(color[map[i][j]-1], i*30+3*(i+1)+150+offsetX, j*30+3*(j+1)+offsetY, null);
            }
        }
        // 從控制器讀取目前方塊狀態（改用集中方法）
        syncStateFromController();

        if(flag == 0) {
            for (int i = 0; i < 16; i++) {
                int[] rotation = Tetromino.values()[blockType].rotation(turnState);
                if (rotation[i] == 1) {
                    graphics.drawImage(color[blockType], (i%4 + x)*33 + 3 + 150 + offsetX, (i/4 + y)*33 + 3 + offsetY, null);
                }
            }
        }
        if(hold >= 0) {
            for (int i = 0; i < 16; i++) {
                int[] holdRot = Tetromino.values()[hold].rotation(0);
                if (holdRot[i] == 1) {
                     graphics.drawImage(color[hold], (i%4)*33 + 15 + offsetX, (i/4)*33 + 45 + offsetY, null); 
                }
            }
        }
        // 繪製多個 Next 預覽：同一水平位置，往下堆疊
        List<Integer> nexts = controller.getNextQueue();
        int previewCount = Math.min(4, nexts.size());
        for (int j = 0; j < previewCount; j++) {
            int nextType = nexts.get(j);
            int[] nextRot = Tetromino.values()[nextType].rotation(0);
            for (int i = 0; i < 16; i++) {
                if (nextRot[i] == 1) {
                    graphics.drawImage(
                        color[nextType],
                        (i%4)*33 + 530 + offsetX,
                        (i/4)*33 + 3 + 80 + offsetY + j * nextSpacing,
                        null
                    );
                }
            }
        }

        long elapsed = System.currentTimeMillis() - startTime;
    
        // int imgX = (getWidth() - 700) / 2;
        // int imgY = getHeight() - 70 - 60;

        if (elapsed > 2000) {
            // 2秒後開始每秒減少透明度
            alpha = 1.0f - (elapsed - 2000) / 1000.0f;
            if (alpha < 0) alpha = 0;
        }

        if (alpha > 0) {
            // 套用透明度並繪製
            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
            g2d.drawImage(startPhoto, 320, 300, 445, 100, this);
            
            // 繪製完畢必須重設透明度，以免影響下一輪繪圖
            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));
            
            // 只要還在消失動畫中，就持續要求重繪
            if (elapsed > 2000) {
                repaint();
            }
        }
        if (countdown >= 1 && countdown <= 4) {
            
            if (countdown == 1) currentImg = img3;
            else if (countdown == 2) currentImg = img2;
            else if (countdown == 3) currentImg = img1;
            else if (countdown == 4) currentImg = imgGo;

            if (currentImg != null) {
                // 設定倒數圖片大小與位置（正中央）
                int countDownWidth = 200;
                int countDownHeight = 200;
                int countDownX = (getWidth() - countDownWidth) / 2;
                int countDownY = (getHeight() - countDownHeight) / 2;
                g2d.drawImage(currentImg, countDownX, countDownY, countDownWidth, countDownHeight, this);
            }
        }
        if (countdown == 5) currentImg = null;
    }

    @Override
    public void keyReleased(KeyEvent e) {}

    @Override
    public void keyTyped(KeyEvent e) {}

    @Override
    public void keyPressed(KeyEvent e) {
        if(countdown == 5){
            switch (e.getKeyCode()) {
                case KeyEvent.VK_DOWN:
                    down_shift();
                    break;
                case KeyEvent.VK_UP:
                    controller.rotateClockwise();
                    syncStateFromController();
                    repaint();
                    break;
                case KeyEvent.VK_X: // X 順時針旋轉，與上方向鍵一致
                    controller.rotateClockwise();
                    syncStateFromController();
                    repaint();
                    break;
                case KeyEvent.VK_Z: // Z 逆時針旋轉，取前一個旋轉狀態
                    controller.rotateCounterclockwise();
                    syncStateFromController();
                    repaint();
                    break;
                case KeyEvent.VK_RIGHT:
                    controller.r_shift();
                    syncStateFromController();
                    repaint();
                    break;
                case KeyEvent.VK_LEFT:
                    controller.l_shift();
                    syncStateFromController();
                    repaint();
                    break;
                case KeyEvent.VK_SPACE:
                    // 只有在還沒結束時才執行
                    if (!isGameOver()) {
                        while(down_shift() == 1); 
                        // 強制同步狀態
                        syncStateFromController();
                        repaint();
                        // 關鍵：如果這一下 Space 導致遊戲結束，立刻通知主程式
                        if (isGameOver()) {
                            Tetris frame = (Tetris) SwingUtilities.getWindowAncestor(this);
                            if (frame != null) {
                    // 停止面板內的任何倒數計時（如果有）
                                // 並呼叫結束處理
                                frame.triggerGameOverManually(); 
                            }
                        }
                    }
                    break;
                case KeyEvent.VK_SHIFT: 
                    controller.holdSwap();
                    syncStateFromController();
                    repaint();
                    break;
            }
        }
        else{return;}
    }

    // void Sleep(int milliseconds) {
    //     try {
    //         Thread.sleep(milliseconds);
    //     } catch (InterruptedException e) {
    //         System.out.println("Unexcepted interrupt");
    //         System.exit(0);
    //     }
    // }
}