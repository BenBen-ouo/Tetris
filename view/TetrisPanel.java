package view;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import javax.swing.*;

public final class TetrisPanel extends JPanel implements KeyListener { //面板邏輯
    public int[][] map = new int [10][20]; // 10寬 20高 
    private int blockType; // 0~6 代表7種方塊
    private int turnState; // 0~3 代表方塊的4種旋轉狀態
    private int x, y, hold, next, change; //x,y為方塊位置，hold為暫存方塊，next為下一個方塊，change為是否能換
    private int flag = 0;
    private final Image b1;
    private final Image b2;

    // 方塊顏色圖片陣列
    private final Image[] color = new Image[7];
    // 方塊形狀陣列要改
    public final int shapes[][][] = new int[][][] {
            // I
            { { 0, 0, 0, 0, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0 },
                    { 0, 1, 0, 0, 0, 1, 0, 0, 0, 1, 0, 0, 0, 1, 0, 0 },
                    { 0, 0, 0, 0, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0 },
                    { 0, 1, 0, 0, 0, 1, 0, 0, 0, 1, 0, 0, 0, 1, 0, 0 } },
            // S
            { { 0, 1, 1, 0, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 },
                    { 1, 0, 0, 0, 1, 1, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0 },
                    { 0, 1, 1, 0, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 },
                    { 1, 0, 0, 0, 1, 1, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0 } },
            // Z
            { { 1, 1, 0, 0, 0, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0 },
                    { 0, 1, 0, 0, 1, 1, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0 },
                    { 1, 1, 0, 0, 0, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0 },
                    { 0, 1, 0, 0, 1, 1, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0 } },
            // J
            { { 0, 1, 0, 0, 0, 1, 0, 0, 1, 1, 0, 0, 0, 0, 0, 0 },
                    { 1, 0, 0, 0, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0 },
                    { 1, 1, 0, 0, 1, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0 },
                    { 1, 1, 1, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0 } },
            // O
            { { 1, 1, 0, 0, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 },
                    { 1, 1, 0, 0, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 },
                    { 1, 1, 0, 0, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 },
                    { 1, 1, 0, 0, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 } },
            // L
            { { 1, 0, 0, 0, 1, 0, 0, 0, 1, 1, 0, 0, 0, 0, 0, 0 },
                    { 1, 1, 1, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 },
                    { 1, 1, 0, 0, 0, 1, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0 },
                    { 0, 0, 1, 0, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0 } },
            // T
            { { 0, 1, 0, 0, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0 },
                    { 0, 1, 0, 0, 1, 1, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0 },
                    { 1, 1, 1, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 },
                    { 0, 1, 0, 0, 0, 1, 1, 0, 0, 1, 0, 0, 0, 0, 0, 0 } }
    };

    public TetrisPanel() {
        this.setLayout(null);
        this.setBackground(Color.BLACK);

        b1 = Toolkit.getDefaultToolkit().getImage("image/background1.png");
        b2 = Toolkit.getDefaultToolkit().getImage("image/background2.png");
        color[0] = Toolkit.getDefaultToolkit().getImage("image/blue.png");
        color[1] = Toolkit.getDefaultToolkit().getImage("image/green.png");
        color[2] = Toolkit.getDefaultToolkit().getImage("image/red.png");
        color[3] = Toolkit.getDefaultToolkit().getImage("image/deepblue.png");
        color[4] = Toolkit.getDefaultToolkit().getImage("image/yellow.png");
        color[5] = Toolkit.getDefaultToolkit().getImage("image/orange.png");
        color[6] = Toolkit.getDefaultToolkit().getImage("image/pink.png");
        
        JLabel NEXT = new JLabel("NEXT"); // 下一個方塊標題
        NEXT.setFont(new Font("", Font.BOLD, 50));
        NEXT.setBounds(500, 0, 200, 100);
        NEXT.setForeground(Color.white);
        add(NEXT);

        JLabel HOLD = new JLabel("HOLD");
        HOLD.setFont(new Font("", Font.BOLD, 50));
        HOLD.setBounds(0, 0, 200, 100);
        HOLD.setForeground(Color.white);
        add(HOLD);

        initMap(); // 初始化地圖
        newBlock(); // 產生新方塊
        hold = -1; // 初始暫存方塊為空

        next = (int)(Math.random()*7); // 下一個方塊類型要改邏輯

        Timer timer = new Timer(1000, new TimerListener());
        timer.start(); // 啟動計時器
    }

    public void newBlock() {
        flag = 0;
        blockType = next;
        change = 1;
        next = (int)(Math.random()*7);
        turnState = 0;
        x = 4; y = 0;
        if(gameOver(x, y) == 1) {
            initMap();
        }
        repaint();
    }

    public void setBlock(int x, int y, int type, int state) {
        flag = 1;
        for(int i = 0; i < 16; i++) {
            if(shapes[type][state][i] == 1) {
                map[x+i%4][y+i/4] = type+1;
            }
        }
    }

    public int gameOver(int x, int y) {
        if(blow(x, y, blockType, turnState) == 0)
            return 1;
        return 0;
    }

    public int blow(int x, int y, int type, int state) {
        for(int i = 0; i < 16; i++) {
            if(shapes[type][state][i] == 1) {
                if(x+i%4 >= 10 || y+i/4 >= 20 || x+i%4 < 0 || y+i/4 < 0)
                    return 0;
                if(map[x+i%4][y+i/4] != 0)
                    return 0;
            }
        }
        return 1;
    }

    public void rotate() {
        int tmpState = turnState;
        tmpState = (tmpState+1)%4;
        if(blow(x, y, blockType, tmpState) == 1) {
            turnState = tmpState;
        }
        repaint();
    }

    public int r_shift() {
        int canShift = 0;
        if(blow(x+1, y, blockType, turnState) == 1) {
            x++;
            canShift = 1;
        }
        repaint();
        return canShift;
    }

    public void l_shift() {
        if(blow(x - 1, y, blockType, turnState) == 1) {
            x--;
        }
        repaint();
    }

    public int down_shift() {
        int canDown = 0;
        if (blow(x, y + 1, blockType, turnState) == 1) {
            y++;
            canDown = 1;
        }
        repaint();
        if (blow(x, y + 1, blockType, turnState) == 0) {
            //Sleep(500); ///目前不確定功能
            setBlock(x, y, blockType, turnState);
            newBlock();
            delLine();
            canDown = 0;
        }
        return canDown;
    }

    void delLine() {
        int idx = 19;
        for(int i = 19; i >= 0; i--) {
            int cnt = 0;
            for(int j = 0; j < 10; j++) {
                if(map[j][i] != 0)
                    cnt++;
            }
            if(cnt == 10) {
                for(int j = 0; j < 10; j++) {
                    map[j][i] = 0;
                }
            } else {
                for(int j = 0; j < 10; j++) {
                    map[j][idx] = map[j][i];
                }
                idx--;
            }
        }   
        /*if(access == 1)
            Sleep(500);*/
            ///目前不確定功能
    }

    void initMap() {
        for(int i = 0; i < 10; i++)
            for(int j = 0; j < 20; j++)
                map[i][j] = 0;
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        for(int i = 0; i < 10; i++) {
            for(int j = 0; j < 20; j++) {
                if(map[i][j] == 0) {
                    if((i+j)%2 == 0)
                        g.drawImage(b1, i*30+3*(i+1)+150, j*30+3*(j+1), null);
                    else
                        g.drawImage(b2, i*30+3*(i+1)+150, j*30+3*(j+1), null);
                } else
                    g.drawImage(color[map[i][j]-1], i*30+3*(i+1)+150, j*30+3*(j+1), null);
            }
        }
        if(flag == 0) {
            for (int i = 0; i < 16; i++) {
                if (shapes[blockType][turnState][i] == 1) {
                    g.drawImage(color[blockType], (i%4+x)*33+3+150, (i/4+y)*33+3, null);
                }
            }
        }
        if(hold >= 0) {
            for (int i = 0; i < 16; i++) {
                if (shapes[hold][0][i] == 1) {
                    g.drawImage(color[hold], (i%4)*33+3, (i/4)*33+3+80, null);
                }
            }
        }
        for (int i = 0; i < 16; i++) {
            if (shapes[next][0][i] == 1) {
                g.drawImage(color[next], (i%4)*33+530, (i/4)*33+3+80, null);
            }
        }
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
            if(hold >= 0 && change == 1) {
                int tmp;
                tmp = hold;
                hold = blockType;
                blockType = tmp;
                x = 4;
                y = 0;
                change = 0;
            } else if(change == 1) {
                hold = blockType;
                newBlock();
            }
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

    class TimerListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            repaint();
            down_shift();
        }
    }
}
