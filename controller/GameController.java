package controller;

import java.util.Random;
import model.Board;
import model.Tetromino;

// 單一局遊戲狀態與邏輯：方塊生成、移動、旋轉、落下、固定、檢查
public class GameController {
    private final Board board;
    private final Random random = new Random();

    private int blockType;   // 0~6
    private int turnState;   // 0~3
    private int x, y;        // 目前方塊位置
    private int hold = -1;   // 暫存
    private int next;        // 下一個
    private int change = 1;  // 是否可切換 hold
    private int flag = 0;    // 與舊程式相容（0:飄落中, 1:已固定）

    public GameController(Board board) {
        this.board = board;
        this.board.initMap();
        this.next = random.nextInt(7);
        newBlock();
    }

    public Board getBoard() { return board; }
    public int getBlockType() { return blockType; }
    public int getTurnState() { return turnState; }
    public int getX() { return x; }
    public int getY() { return y; }
    public int getHold() { return hold; }
    public int getNext() { return next; }
    public int getFlag() { return flag; }

    public void newBlock() {
        flag = 0;
        blockType = next;
        change = 1;
        next = random.nextInt(7);
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

    public void rotate() {
        int tmpState = (turnState + 1) % 4; // 目前先維持原本順時針一段
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
