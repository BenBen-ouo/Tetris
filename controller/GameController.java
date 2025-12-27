package controller;

import model.Board;
import model.PieceGenerator;
import model.Tetromino;

// 單一局遊戲狀態與邏輯：方塊生成、移動、旋轉、落下、固定、檢查
public class GameController {
    private final Board board;
    private PieceGenerator generator = new PieceGenerator();

    private int blockType;   // 0~6
    private int turnState;   // 0~3
    private int x, y;        // 目前方塊位置
    private int hold = -1;   // 暫存
    private int next;        // 下一個
    private int change = 1;  // 是否可切換 hold（本回合只允許一次）
    private int flag = 0;    // 與舊程式相容（0:飄落中, 1:已固定）

    public GameController(Board board) {
        this.board = board;
        this.board.initMap();
        this.next = generator.next();
        newBlock();
    }

    // 可替換生成器（測試或不同隨機策略）
    public void setGenerator(PieceGenerator generator) {
        if (generator != null) {
            this.generator = generator;
        }
    }

    public Board getBoard() { return board; }
    public int getBlockType() { return blockType; }
    public int getTurnState() { return turnState; }
    public int getX() { return x; }
    public int getY() { return y; }
    public int getHold() { return hold; }
    public int getNext() { return next; }
    public int getFlag() { return flag; }
    public int getChange() { return change; }

    public void newBlock() {
        flag = 0;
        blockType = next;
        change = 1; // 新方塊出現後允許一次 hold
        next = generator.next();
        turnState = 0;
        x = 4; y = 0;
        if (gameOver(x, y) == 1) {
            board.initMap();
        }
    }

    public void setBlock(int x, int y, int type, int state) {
        flag = 1;
        int[] rotation = Tetromino.values()[type].rotation(state);
        for (int i = 0; i < 16; i++) {
            if (rotation[i] == 1) {
                board.setCell(x + i % 4, y + i / 4, type + 1);
            }
        }
    }

    public int gameOver(int x, int y) {
        if (canPlace(x, y, blockType, turnState) == 0) return 1;
        return 0;
    }

    public int canPlace(int x, int y, int type, int state) {
        int[] rotation = Tetromino.values()[type].rotation(state);
        for (int i = 0; i < 16; i++) {
            if (rotation[i] == 1) {
                int px = x + i % 4;
                int py = y + i / 4;
                if (px >= board.getWidth() || py >= board.getHeight() || px < 0 || py < 0)
                    return 0;
                if (board.getCell(px, py) != 0)
                    return 0;
            }
        }
        return 1;
    }

    public void rotateClockwise() {
        int tmpState = (turnState + 1) % 4; // 目前先維持原本順時針一段
        if (canPlace(x, y, blockType, tmpState) == 1) {
            turnState = tmpState;
        }
    }

    // 逆時針旋轉（Z）：取前一個旋轉狀態
    public void rotateCCW() {
        int tmpState = (turnState + 3) % 4; // 等同於 (turnState - 1 + 4) % 4
        if (canPlace(x, y, blockType, tmpState) == 1) {
            turnState = tmpState;
        }
    }

    public int r_shift() {
        if (canPlace(x + 1, y, blockType, turnState) == 1) {
            x++;
            return 1;
        }
        return 0;
    }

    public void l_shift() {
        if (canPlace(x - 1, y, blockType, turnState) == 1) {
            x--;
        }
    }

    public int down_shift() {
        if (canPlace(x, y + 1, blockType, turnState) == 1) {
            y++;
            return 1;
        }
        if (canPlace(x, y + 1, blockType, turnState) == 0) {
            setBlock(x, y, blockType, turnState);
            board.clearFullLines();
            newBlock();
        }
        return 0;
    }

    // 切換暫存（Shift）：本回合只允許一次
    public void holdSwap() {
        if (change == 1) {
            if (hold >= 0) {
                int tmp = hold;
                hold = blockType;
                blockType = tmp;
                x = 4;
                y = 0;
                turnState = 0;
                change = 0;
            } else {
                hold = blockType;
                newBlock();
                change = 0; // 本回合已使用
            }
        }
    }

    // 計時器集中化預留：之後由 TimerService 或控制器驅動 tick
    public void tick() {
        // 預留：依模式進行 soft drop、加速、計分/計時等
        // 目前維持原行為：每 tick 嘗試下落一格
        down_shift();
    }

    // 之後可擴充：SRS踢牆、B2B、Combo、Hold 切換等邏輯

    // ===== 靜態輔助方法：提供給現有面板逐步遷移呼叫，不改變行為 =====
    public static int canPlace(Board board, int x, int y, int type, int state) {
        int[] rotation = Tetromino.values()[type].rotation(state);
        for (int i = 0; i < 16; i++) {
            if (rotation[i] == 1) {
                int px = x + i % 4;
                int py = y + i / 4;
                if (px >= board.getWidth() || py >= board.getHeight() || px < 0 || py < 0)
                    return 0;
                if (board.getCell(px, py) != 0)
                    return 0;
            }
        }
        return 1;
    }

    public static void setBlock(Board board, int x, int y, int type, int state) {
        int[] rotation = Tetromino.values()[type].rotation(state);
        for (int i = 0; i < 16; i++) {
            if (rotation[i] == 1) {
                board.setCell(x + i % 4, y + i / 4, type + 1);
            }
        }
    }

    public static int clearFullLines(Board board) {
        return board.clearFullLines();
    }
}
