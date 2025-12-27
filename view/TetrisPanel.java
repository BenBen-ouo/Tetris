package view;

import controller.GameController;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import javax.swing.*;
import model.Board;
import model.Tetromino;

public final class TetrisPanel extends JPanel implements KeyListener { //面板邏輯
    public int[][] map = new int [10][20]; // 10寬 20高（初始化後會指向 Board 的 map）
    private Board board; // 盤面資料來源
    // 轉為由 GameController 管理狀態
    private final GameController controller;
    private int blockType; // 暫存繪製使用（由 controller 取得）
    private int turnState; // 暫存繪製使用（由 controller 取得）
    private int x, y, hold, next; // 暫存繪製（由 controller 取得）
    private int flag = 0; // 與舊程式相容（由 controller 提供）
    private final Image b1;
    private final Image b2;
    private final Image holdPhoto;
    private final Image nextPhoto;
    private final Image startPhoto;
    private long startTime;
    private float alpha = 1.0f;  
       // 目前的透明度 (1.0 = 不透明, 0.0 = 全透明)

    // 方塊顏色圖片陣列
    private final Image[] color = new Image[7];

    public TetrisPanel() {
        this.setLayout(null);
        this.setBackground(Color.BLACK);

        b1 = Toolkit.getDefaultToolkit().getImage("image/background1.png");
        b2 = Toolkit.getDefaultToolkit().getImage("image/background2.png");
        holdPhoto = Toolkit.getDefaultToolkit().getImage("image/tetris_grid_Hold.png");
        nextPhoto = Toolkit.getDefaultToolkit().getImage("image/tetris_grid_Next.png");
        startPhoto = Toolkit.getDefaultToolkit().getImage("image/blitz_banner.png"); // 換成你的檔名
        startTime = System.currentTimeMillis();
        color[0] = Toolkit.getDefaultToolkit().getImage("image/blue.png");
        color[1] = Toolkit.getDefaultToolkit().getImage("image/green.png");
        color[2] = Toolkit.getDefaultToolkit().getImage("image/red.png");
        color[3] = Toolkit.getDefaultToolkit().getImage("image/deepblue.png");
        color[4] = Toolkit.getDefaultToolkit().getImage("image/yellow.png");
        color[5] = Toolkit.getDefaultToolkit().getImage("image/orange.png");
        color[6] = Toolkit.getDefaultToolkit().getImage("image/pink.png");
        
        JLabel NEXT = new JLabel(); // 下一個方塊標題
        NEXT.setFont(new Font("", Font.BOLD, 50));
        NEXT.setBounds(500, 0, 200, 100);
        NEXT.setForeground(Color.white);
        add(NEXT);

        JLabel HOLD = new JLabel();
        HOLD.setFont(new Font("", Font.BOLD, 50));
        HOLD.setBounds(0, 0, 200, 100);
        HOLD.setForeground(Color.white);
        add(HOLD);

        // 初始化 Board 與 map
        board = new Board();
        map = board.getMap();
        controller = new GameController(board);
        initMap(); // 初始化地圖
        // 由控制器初始化新方塊
        newBlock();
        hold = controller.getHold();
        next = controller.getNext();

        // 計時器不在面板內管理，由外部 TimerService 呼叫 tick()
    }

    // 外部計時器呼叫本方法以驅動遊戲邏輯
    public void tick() {
        controller.tick();
        syncStateFromController();
        repaint();
    }

    public void newBlock() {// 產生新方塊
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

    public void rotate() {
        controller.rotate();
        syncStateFromController();
        repaint();
    }

    public int r_shift() {
        int moved = controller.r_shift();
        syncStateFromController();
        repaint();
        return moved;
    }

    public void l_shift() {
        controller.l_shift();
        syncStateFromController();
        repaint();
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

    void initMap() {
        board.initMap();
        map = board.getMap();
    }

    public void resetAnimation() {
    // 將 startTime 更新為「現在」，並把透明度還原
        this.startTime = System.currentTimeMillis();
        this.alpha = 1.0f;
        repaint();
    }

    @Override
    public void paintComponent(Graphics graphics) {
        
        super.paintComponent(graphics);
        Graphics2D g2d = (Graphics2D) graphics;

        graphics.drawImage(holdPhoto, 0, 0, 150, 148, this);
        graphics.drawImage(nextPhoto, 500, 0, 179, 547, this);

        

        for(int i = 0; i < 10; i++) {
            for(int j = 0; j < 20; j++) {
                if(map[i][j] == 0) {
                    if((i+j)%2 == 0)
                        graphics.drawImage(b1, i*30+3*(i+1)+150, j*30+3*(j+1), null);
                    else
                        graphics.drawImage(b2, i*30+3*(i+1)+150, j*30+3*(j+1), null);
                } else
                    graphics.drawImage(color[map[i][j]-1], i*30+3*(i+1)+150, j*30+3*(j+1), null);
            }
        }
        // 從控制器讀取目前方塊狀態
        blockType = controller.getBlockType();
        turnState = controller.getTurnState();
        x = controller.getX();
        y = controller.getY();
        flag = controller.getFlag();
        next = controller.getNext();
        hold = controller.getHold();

        if(flag == 0) {
            for (int i = 0; i < 16; i++) {
                int[] rotation = Tetromino.values()[blockType].rotation(turnState);
                if (rotation[i] == 1) {
                    graphics.drawImage(color[blockType], (i%4 + x)*33 + 3 + 150, (i/4 + y)*33 + 3, null);
                }
            }
        }
        if(hold >= 0) {
            for (int i = 0; i < 16; i++) {
                int[] holdRot = Tetromino.values()[hold].rotation(0);
                if (holdRot[i] == 1) {
                     graphics.drawImage(color[hold], (i%4)*33 + 15, (i/4)*33 + 45, null); 
                }
            }
        }
        for (int i = 0; i < 16; i++) {
            int[] nextRot = Tetromino.values()[next].rotation(0);
            if (nextRot[i] == 1) {
                graphics.drawImage(color[next], (i%4)*33 + 530, (i/4)*33 + 3 + 80, null);
            }
        }

        long elapsed = System.currentTimeMillis() - startTime;
    
        int imgX = (getWidth() - 700) / 2;
        int imgY = getHeight() - 70 - 60;

        if (elapsed > 2000) {
            // 2秒後開始每秒減少透明度
            alpha = 1.0f - (elapsed - 2000) / 1000.0f;
            if (alpha < 0) alpha = 0;
        }

        if (alpha > 0) {
            // 套用透明度並繪製
            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
            g2d.drawImage(startPhoto, imgX, imgY, 700, 70, this);
            
            // 繪製完畢必須重設透明度，以免影響下一輪繪圖
            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));
            
            // 只要還在消失動畫中，就持續要求重繪
            if (elapsed > 2000) {
                repaint();
            }
        }
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

    @Override
    public void keyReleased(KeyEvent e) {}

    @Override
    public void keyTyped(KeyEvent e) {}

    @Override
    public void keyPressed(KeyEvent e) {
        switch (e.getKeyCode()) {
        case KeyEvent.VK_DOWN:
            down_shift();
            break;
        case KeyEvent.VK_UP:
            rotate();
            break;
        case KeyEvent.VK_RIGHT:
            r_shift();
            break;
        case KeyEvent.VK_LEFT:
            l_shift();
            break;
        case KeyEvent.VK_SPACE:
            while(down_shift() == 1);
            break;
        case KeyEvent.VK_SHIFT:
            controller.holdSwap();
            syncStateFromController();
            repaint();
            break;
        }
    }

    void Sleep(int milliseconds) {
        try {
            Thread.sleep(milliseconds);
        } catch (InterruptedException e) {
            System.out.println("Unexcepted interrupt");
            System.exit(0);
        }
    }

    // 計時邏輯已集中到 TimerService，不再在面板內使用 Swing Timer
}