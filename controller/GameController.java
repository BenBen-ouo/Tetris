package controller;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import model.Board;
import model.PieceGenerator;
import model.SRSSystem;
import model.Tetromino;

// 單一局遊戲狀態與邏輯：方塊生成、移動、旋轉、落下、固定、檢查
public class GameController {
    private final Board board;
    private PieceGenerator generator = new PieceGenerator();

    private int blockType;   // 0~6
    private int turnState;   // 0~3
    private int x, y;        // 目前方塊位置
    private int hold = -1;   // 暫存
    // 多個下一個方塊預覽佇列（預設顯示 4 個）
    private final Deque<Integer> nextQueue = new ArrayDeque<>();
    private int change = 1;  // 是否可切換 hold（本回合只允許一次）
    private int flag = 0;    // 與舊程式相容（0:飄落中, 1:已固定）
    // Lock Delay：現階段設定為 0，且不再於流程中使用（僅保留欄位）
    private int lockDelayTicks = 0;
    // Spin/Combo 顯示：SRS 使用第幾次嘗試（1-based，0 表示未用踢牆）、是否符合 T-Spin、最近一次鎖定顯示、到期時間、Combo 次數
    private int kickIndexUsed = 0;
    private boolean currentTSpin = false;
    private String lastSpinText = "";
    private long lastSpinUntilMs = 0L;
    private int combo = -1;
    // 旋轉完成後是否無法再下落（作為 spin 顯示條件之一）
    private boolean cannotDropAfterRotate = false;
    // ALL CLEAR 顯示：到期時間 TTL（毫秒），在面板中央顯示 3 秒
    private long lastAllClearUntilMs = 0L;

    public GameController(Board board) {
        this.board = board;
        this.board.initMap();
        // 初始化預覽佇列，預設顯示 4 個
        for (int i = 0; i < 4; i++) {
            nextQueue.addLast(generator.next());
        }
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
    // 與舊介面相容：回傳第一個預覽
    public int getNext() { return nextQueue.isEmpty() ? -1 : nextQueue.peekFirst(); }
    // 新增：取得全部預覽佇列（複本，避免外部修改）
    public List<Integer> getNextQueue() { return new ArrayList<>(nextQueue); }
    public int getFlag() { return flag; }
    public int getChange() { return change; }

    public void newBlock() {
        flag = 0;
        // 取用第一個預覽作為目前方塊
        blockType = nextQueue.removeFirst();
        change = 1; // 新方塊出現後允許一次 hold
        // 維持預覽佇列長度：補上一個新生成的方塊
        nextQueue.addLast(generator.next());
        turnState = 0;
        x = 4; y = 0;
        kickIndexUsed = 0;
        currentTSpin = false;
        cannotDropAfterRotate = false;
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
        int toState = (turnState + 1) % 4;
        // 直接可放置：旋轉成功
        if (canPlace(x, y, blockType, toState) == 1) {
            turnState = toState;
            // 旋轉後不可下降與 T-Spin 檢測
            cannotDropAfterRotate = (canPlace(x, y + 1, blockType, turnState) == 0);
            currentTSpin = detectTSpin();
            return;
        }
        // 嘗試 SRS 踢牆（O 不使用踢牆表）
        int[][] kicks = SRSSystem.getKicks(blockType, turnState, toState);
        for (int i = 0; i < kicks.length; i++) {
            int[] k = kicks[i];
            int nx = x + k[0];
            // dy>0 表示向上，因此座標系 y 往上要減少
            int ny = y - k[1];
            if (canPlace(nx, ny, blockType, toState) == 1) {
                x = nx;
                y = ny;
                turnState = toState;
                kickIndexUsed = i + 1; // 1-based
                // 旋轉後不可下降與 T-Spin 檢測
                cannotDropAfterRotate = (canPlace(x, y + 1, blockType, turnState) == 0);
                currentTSpin = detectTSpin();
                return;
            }
        }
        // 全部失敗：不旋轉
    }

    // 逆時針旋轉（Z）：取前一個旋轉狀態
    public void rotateCounterclockwise() {
        int toState = (turnState + 3) % 4; // 等同於 (turnState - 1 + 4) % 4
        // 直接可放置：旋轉成功
        if (canPlace(x, y, blockType, toState) == 1) {
            turnState = toState;
            // 旋轉後不可下降與 T-Spin 檢測
            cannotDropAfterRotate = (canPlace(x, y + 1, blockType, turnState) == 0);
            currentTSpin = detectTSpin();
            return;
        }
        // 嘗試 SRS 踢牆（O 不使用踢牆表）
        int[][] kicks = SRSSystem.getKicks(blockType, turnState, toState);
        for (int i = 0; i < kicks.length; i++) {
            int[] k = kicks[i];
            int nx = x + k[0];
            // dy>0 表示向上，因此座標系 y 往上要減少
            int ny = y - k[1];
            if (canPlace(nx, ny, blockType, toState) == 1) {
                x = nx;
                y = ny;
                turnState = toState;
                kickIndexUsed = i + 1; // 1-based
                // 旋轉後不可下降與 T-Spin 檢測
                cannotDropAfterRotate = (canPlace(x, y + 1, blockType, turnState) == 0);
                currentTSpin = detectTSpin();
                return;
            }
        }
        // 全部失敗：不旋轉
    }

    public void r_shift() {
        if (canPlace(x + 1, y, blockType, turnState) == 1) {
            x++;
        }
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
        // 不能下落：不鎖定、不標記觸地；交由下一次 tick 處理固定
        return 0;
    }

    // 空白鍵：硬降，直接落到底並立即鎖定（不套用 lock delay）
    public void hardDrop() {
        while (canPlace(x, y + 1, blockType, turnState) == 1) {
            y++;
        }
        setBlock(x, y, blockType, turnState);
        int cleared = board.clearFullLines();
        // Combo：連續有消行則累加，沒消行則設為-1
        combo = (cleared > 0) ? (combo + 1) : -1;
        // ALL CLEAR：盤面全空時顯示 3 秒
        if (isBoardEmpty()) {
            lastAllClearUntilMs = System.currentTimeMillis() + 3000;
        } else if (lastAllClearUntilMs != 0L && System.currentTimeMillis() > lastAllClearUntilMs) {
            lastAllClearUntilMs = 0L;
        }
        // Spin 顯示：僅當 (踢牆使用第 3 次以上且旋轉後無法下落) 或 T-Spin
        if ((kickIndexUsed >= 3 && cannotDropAfterRotate) || currentTSpin) {
            String name = Tetromino.values()[blockType].name();
            lastSpinText = (blockType == Tetromino.T.ordinal()) ? "T spin" : (name + " spin");
            lastSpinUntilMs = System.currentTimeMillis() + 3000;
        } else {
            lastSpinText = "";
            lastSpinUntilMs = 0L;
        }
        newBlock();
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
        // 能下落則下落；不能下落則在該 tick 立即固定
        if (canPlace(x, y + 1, blockType, turnState) == 1) {
            y++;
            return;
        }
        setBlock(x, y, blockType, turnState);
        int cleared = board.clearFullLines();
        // Combo：連續有消行則累加，沒消行則設為-1
        combo = (cleared > 0) ? (combo + 1) : -1;
        // ALL CLEAR：盤面全空時顯示 3 秒
        if (isBoardEmpty()) {
            lastAllClearUntilMs = System.currentTimeMillis() + 3000;
        } else if (lastAllClearUntilMs != 0L && System.currentTimeMillis() > lastAllClearUntilMs) {
            lastAllClearUntilMs = 0L;
        }
        // Spin 顯示：僅當 (踢牆使用第 3 次以上且旋轉後無法下落) 或 T-Spin
        if ((kickIndexUsed >= 3 && cannotDropAfterRotate) || currentTSpin) {
            String name = Tetromino.values()[blockType].name();
            lastSpinText = (blockType == Tetromino.T.ordinal()) ? "T spin" : (name + " spin");
            lastSpinUntilMs = System.currentTimeMillis() + 3000;
        } else {
            lastSpinText = "";
            lastSpinUntilMs = 0L;
        }
        newBlock();
    }
    public String getLastSpinText() {
        if (lastSpinUntilMs == 0L) return "";
        return (System.currentTimeMillis() <= lastSpinUntilMs) ? lastSpinText : "";
    }

    public int getCombo() { return combo; }

    public String getAllClearText() {
        if (lastAllClearUntilMs == 0L) return "";
        return (System.currentTimeMillis() <= lastAllClearUntilMs) ? "ALL CLEAR" : "";
    }

    // T-Spin 檢測：僅在當前方塊為 T，且無法再下落，且四角至少三格為填滿（出界視為填滿）
    private boolean detectTSpin() {
        if (blockType != Tetromino.T.ordinal()) return false;
        // 無法再往下
        if (canPlace(x, y + 1, blockType, turnState) == 1) return false;
        int ox = x + 1; // SRS 中心在 4x4 的 (1,1)
        int oy = y + 1;
        int filled = 0;
        filled += isFilled(ox - 1, oy - 1) ? 1 : 0; // 左上
        filled += isFilled(ox + 1, oy - 1) ? 1 : 0; // 右上
        filled += isFilled(ox - 1, oy + 1) ? 1 : 0; // 左下
        filled += isFilled(ox + 1, oy + 1) ? 1 : 0; // 右下
        return filled >= 3;
    }

    private boolean isFilled(int px, int py) {
        if (px < 0 || py < 0 || px >= board.getWidth() || py >= board.getHeight()) return true; // 出界算填滿
        return board.getCell(px, py) != 0;
    }

    private boolean isBoardEmpty() {
        for (int ix = 0; ix < board.getWidth(); ix++) {
            for (int iy = 0; iy < board.getHeight(); iy++) {
                if (board.getCell(ix, iy) != 0) return false;
            }
        }
        return true;
    }

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
