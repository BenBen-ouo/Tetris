package controller;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import model.Board;
import model.Notification;
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
    // 旋轉/通知相關：踢牆次序與 T-Spin 判定，集中由 Notification 顯示
    private int kickIndexUsed = 0;
    private boolean currentTSpin = false;
    private boolean cannotDropAfterRotate = false;
    private final Notification notification = new Notification();
    // 每塊方塊僅允許一次硬降
    private boolean hardDropAllowed = true;
    // 遊戲結束狀態（觸發於鎖定後占用到最上兩層中間四格的八格）
    private boolean gameOver = false;
    // 動態死亡線：僅允許死亡線以下的行可放方塊（0-based）。初始為 2（僅允許以下 18 行）。
    private int minAllowedRow = 2;
    // 普通模式：每放好三個方塊下降一格的計數器
    private int normalLocksCounter = 0;
    // 四格寬模式（Narrow Mode）
    private boolean narrowMode = false;
    private int[][] backupMap = null; // 進入四格寬前的盤面備份（恢復 10x20 用）
    private boolean narrowFirstClearOccurred = false; // 進入四格寬後的第一次消行不算 combo
    private int narrowComboCount = 0; // 第一次消行後連續消行的次數（達 5 即退出四格寬）
    // 全域連續消行計數（第二次連消才顯示 combo x1）
    private int consecutiveClears = 0;
    // 模式切換三階段：2=第一個凍結tick(不動)、1=第二個凍結tick(僅隱藏左右)、0=恢復
    private int transitionStage = 0;
    private boolean transitionEntering = false;
    private boolean hideOuterDuringTransition = false; // 第二個tick隱藏左右欄
    private boolean showNarrowLabel = false; // 進入第三個tick才顯示字串
    private boolean forceShowOuterDuringTransition = false; // 退出窄化第二個tick顯示左右欄
    private boolean pendingCommitEnter = false;
    private boolean pendingCommitExit = false;
    // 進入窄化的全新 5 階段過渡：
    // 5: 顯示 Change mode !（凍結）
    // 4: 隱藏左三欄（凍結）
    // 3: 隱藏右三欄（凍結）
    // 2: 正式提交窄化並執行四欄消行（凍結）
    // 1: 顯示 Try combo x5（凍結）
    // 0: 過渡結束，允許操作
    private int enterSteps = 0;
    private boolean hideLeftDuringTransition = false;
    private boolean hideRightDuringTransition = false;
    private boolean showChangeModeLabel = false;
    // 退出窄化的 5 階段過渡（與進入對稱）：
    // 5: 顯示 Change mode !（凍結）
    // 4: 回復左三欄（讀取備份並顯示；凍結）
    // 3: 回復右三欄（讀取備份並顯示；凍結）
    // 2: 顯示 Try Tetris !（偏上）且紅線上移（凍結）
    // 1: 正式提交退出窄化（仍凍結）；0: 結束過渡、允許操作
    private int exitSteps = 0;
    private boolean showTryTetrisLabel = false;

    public GameController(Board board) {
        this.board = board;
        this.board.initMap();
        // 初始化預覽佇列，預設決定 5 個（含畫面顯示的前五個順序）
        for (int i = 0; i < 5; i++) {
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
    public int getNext() {
        Integer v = nextQueue.peekFirst();
        return v == null ? -1 : v;
    }
    // 新增：取得全部預覽佇列（複本，避免外部修改）
    public List<Integer> getNextQueue() { return new ArrayList<>(nextQueue); }
    public int getFlag() { return flag; }
    public int getChange() { return change; }
    public Notification getNotification() { return notification; }
    public boolean isFrozen() { return enterSteps > 0 || exitSteps > 0 || transitionStage > 0 || pendingCommitEnter || pendingCommitExit; }
    public int getMinAllowedRow() { return minAllowedRow; }
    public boolean isNarrowMode() { return narrowMode; }
    public boolean shouldHideOuterColumns() { return hideOuterDuringTransition; }
    public boolean shouldHideLeftColumns() { return hideLeftDuringTransition; }
    public boolean shouldHideRightColumns() { return hideRightDuringTransition; }
    public boolean shouldShowNarrowLabel() { return showNarrowLabel; }
    public boolean shouldForceShowOuterColumns() { return forceShowOuterDuringTransition; }
    public boolean shouldShowChangeModeLabel() { return showChangeModeLabel; }
    public boolean shouldShowTryTetrisLabel() { return showTryTetrisLabel; }

    public void newBlock() {
        if (gameOver) return; // 遊戲已結束，不再產生新方塊
        // 1. 取得新方塊前，先重設座標與狀態
        this.turnState = 0;
        this.x = 3; 
        this.y = 0;
        this.change = 1;
        this.hardDropAllowed = true;
        // 重置旋轉相關判定，避免上一塊的旋轉狀態影響新方塊
        this.kickIndexUsed = 0;
        this.currentTSpin = false;
        this.cannotDropAfterRotate = false;
    
    // 2. 取出下一個方塊
        if (!nextQueue.isEmpty()) {
            this.blockType = nextQueue.removeFirst();
            nextQueue.addLast(generator.next());
        }

        // 3. 重要：檢查這個「剛出生」的方塊位置是否合法
        // 如果一出生就不能放，才設定 flag = 1
        if (canPlace(x, y, blockType, turnState) == 0) {
            this.flag = 1; 
        }
        else {
            this.flag = 0; // 否則確保它是 0
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
                // 四格寬模式：限制僅能在 x=3..6 的範圍放置
                if (narrowMode && (px < 3 || px > 6)) return 0;
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
        if (this.flag == 1) return 0;
        if (canPlace(x, y + 1, blockType, turnState) == 1) {
            y++;
            return 1;
        }
        // 不能下落：不鎖定、不標記觸地；交由下一次 tick 處理固定
        return 0;
    }

    // 空白鍵：硬降，直接落到底並立即鎖定（不套用 lock delay）
    public void hardDrop() {
        if (gameOver) return;
        if (isFrozen()) return; // 凍結或待提交期間不處理
        if (!hardDropAllowed) return;
        hardDropAllowed = false;
        while (canPlace(x, y + 1, blockType, turnState) == 1) {
            y++;
        }
        setBlock(x, y, blockType, turnState);
        // 放好方塊的當下判定遊戲結束
        if (isAboveDeathLineOccupied()) {
            gameOver = true;
            return;
        }
        int cleared = narrowMode ? clearFullLinesNarrow() : board.clearFullLines();
        if (!narrowMode) {
            // 新規則：每放好三個方塊下降一格；若消除四行（Tetris），紅線上移兩格並重置計數
            boolean isTetris = (cleared == 4);
            if (isTetris) {
                minAllowedRow = Math.max(2, minAllowedRow - 2); // 上限為 18 行（minAllowedRow=2）
                normalLocksCounter = 0; // 重新計算新的三個方塊
                // 上移不需立即判斷結束（風險下降）
            } else {
                normalLocksCounter += 1;
                if (normalLocksCounter >= 3) {
                    int before = minAllowedRow;
                    minAllowedRow = Math.min(10, minAllowedRow + 1);
                    normalLocksCounter = 0;
                    if (!narrowMode && before < 10 && minAllowedRow >= 10) {
                        scheduleEnterNarrowMode();
                    }
                    // 紅線往下移動後，立即判斷是否遊戲結束
                    if (minAllowedRow > before && isAboveDeathLineOccupied()) {
                        gameOver = true;
                        return;
                    }
                }
            }
        } else {
            // 四格寬模式邏輯：第一次消行不算 combo；combo 斷掉時紅線下移 1 格（最多到剩 5 格）
            if (cleared > 0) {
                if (!narrowFirstClearOccurred) {
                    narrowFirstClearOccurred = true; // 第一次消除，不計入 combo
                } else {
                    narrowComboCount += 1; // 累加 combo
                    if (narrowComboCount >= 5) {
                        scheduleExitNarrowMode();
                    }
                }
            } else {
                if (narrowFirstClearOccurred) {
                    minAllowedRow = Math.min(15, minAllowedRow + 1);
                    narrowFirstClearOccurred = false;
                    narrowComboCount = 0;
                    // 判定線往下移動後，立即判斷是否遊戲結束
                    if (isAboveDeathLineOccupied()) {
                        gameOver = true;
                        return;
                    }
                }
            }
        }
        // Combo 顯示規則：第二次連消才顯示 x1
        if (cleared > 0) consecutiveClears++; else consecutiveClears = 0;
        notification.setCombo(consecutiveClears >= 2 ? (consecutiveClears - 1) : 0);
        // 消除行訊息（1 line, 2 lines, 3 lines, Tetris）
        notification.showLineClear(cleared, 1500);
        // ALL CLEAR：盤面全空時顯示 3 秒
        if (isBoardEmpty()) {
            notification.showAllClear(3000);
        }
        // Spin 顯示：僅當 (踢牆使用第 3 次以上且旋轉後無法下落) 或 T-Spin，且排除 O
        if (blockType != Tetromino.O.ordinal() && ((kickIndexUsed >= 3 && cannotDropAfterRotate) || currentTSpin)) {
            String name = Tetromino.values()[blockType].name();
            String text = (blockType == Tetromino.T.ordinal()) ? "T spin" : (name + " spin");
            notification.showSpin(text, 3000);
        } else {
            notification.clearSpin();
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
                x = 3;
                y = 0;
                turnState = 0;
                change = 0;
                // 重置旋轉相關判定，暫存交換後視為新出現的方塊
                this.kickIndexUsed = 0;
                this.currentTSpin = false;
                this.cannotDropAfterRotate = false;
            } else {
                hold = blockType;
                newBlock();
                change = 0; // 本回合已使用
            }
        }
    }

    // 計時器集中化預留：之後由 TimerService 或控制器驅動 tick
    public void tick() {
        if (gameOver) return;
        // 先處理「進入窄化」的 5 階段過渡（完全凍結操作）
        if (enterSteps > 0) {
            if (enterSteps == 5) {
                // 顯示 Change mode !
                showChangeModeLabel = true;
                hideLeftDuringTransition = false;
                hideRightDuringTransition = false;
                enterSteps = 4;
                return;
            } else if (enterSteps == 4) {
                // 隱藏左三欄
                showChangeModeLabel = false;
                hideLeftDuringTransition = true;
                hideRightDuringTransition = false;
                enterSteps = 3;
                return;
            } else if (enterSteps == 3) {
                // 隱藏右三欄（此時左右皆隱藏）
                hideLeftDuringTransition = true;
                hideRightDuringTransition = true;
                enterSteps = 2;
                return;
            } else if (enterSteps == 2) {
                // 正式提交窄化並執行四欄消行
                enterNarrowMode();
                // 進入窄化後維持左右隱藏
                hideLeftDuringTransition = true;
                hideRightDuringTransition = true;
                showNarrowLabel = false; // 下一步才顯示 Try combo x5
                enterSteps = 1;
                return;
            } else if (enterSteps == 1) {
                // 顯示 Try combo x5（仍凍結）
                showNarrowLabel = true;
                enterSteps = 0; // 下一個 tick 才解除凍結
                return;
            }
        }

        // 退出窄化的 5 階段過渡（完全凍結操作）
        if (exitSteps > 0) {
            if (exitSteps == 5) {
                // 顯示 Change mode !
                showChangeModeLabel = true;
                showTryTetrisLabel = false;
                // 初始維持窄化隱藏左右
                forceShowOuterDuringTransition = false;
                hideLeftDuringTransition = false;
                hideRightDuringTransition = false;
                exitSteps = 4;
                return;
            } else if (exitSteps == 4) {
                // 回復左三欄（從備份寫回並顯示左側）
                showChangeModeLabel = false;
                restoreLeftColumnsFromBackup();
                forceShowOuterDuringTransition = true; // 允許顯示外側
                hideLeftDuringTransition = false;       // 顯示左側
                hideRightDuringTransition = true;       // 仍隱藏右側
                exitSteps = 3;
                return;
            } else if (exitSteps == 3) {
                // 回復右三欄（從備份寫回並顯示右側）
                restoreRightColumnsFromBackup();
                forceShowOuterDuringTransition = true;
                hideLeftDuringTransition = false;
                hideRightDuringTransition = false; // 左右都顯示
                exitSteps = 2;
                return;
            } else if (exitSteps == 2) {
                // 顯示 Try Tetris !，紅線往上移（回到普通模式高度）
                showTryTetrisLabel = true;
                minAllowedRow = 2; // 紅線回到第 18 格高
                forceShowOuterDuringTransition = true;
                hideLeftDuringTransition = false;
                hideRightDuringTransition = false;
                exitSteps = 1;
                return;
            } else if (exitSteps == 1) {
                // 正式退出窄化（提交），下一個 tick 才解除凍結
                exitNarrowMode();
                showTryTetrisLabel = false; // 提示只在上一個步驟顯示
                forceShowOuterDuringTransition = false; // 恢復一般顯示規則
                exitSteps = 0;
                return;
            }
        }

        // 既有的進/出窄化兩階段（僅保留給舊的退出流程使用；現已由 exitSteps 取代）
        // 模式切換三階段：2 -> 1 -> 0
        if (transitionStage > 0) {
            if (transitionStage == 2) {
                transitionStage = 1; // 第一個tick：完全不動
                return;
            } else if (transitionStage == 1) {
                // 第二個tick：若是進入窄化，隱藏左右欄；若是退出窄化，顯示左右欄
                hideOuterDuringTransition = transitionEntering;
                forceShowOuterDuringTransition = !transitionEntering;
                pendingCommitEnter = transitionEntering;
                pendingCommitExit = !transitionEntering;
                transitionStage = 0; // 下一個tick開始恢復
                return;
            }
        }
        // 進入第三個tick的開始：提交模式切換（僅退出窄化時使用）
        if (pendingCommitEnter) {
            pendingCommitEnter = false;
            enterNarrowMode();
            hideOuterDuringTransition = false;
            showNarrowLabel = true; // 這個tick開始顯示字串與允許動作
            forceShowOuterDuringTransition = false;
        }
        if (pendingCommitExit) {
            pendingCommitExit = false;
            exitNarrowMode();
            showNarrowLabel = false;
            forceShowOuterDuringTransition = false;
        }
        // 能下落則下落；不能下落則在該 tick 立即固定
        if (canPlace(x, y + 1, blockType, turnState) == 1) {
            y++;
            return;
        }
        setBlock(x, y, blockType, turnState);
        // 放好方塊的當下判定遊戲結束
        if (isAboveDeathLineOccupied()) {
            gameOver = true;
            return;
        }
        int cleared = narrowMode ? clearFullLinesNarrow() : board.clearFullLines();
        if (!narrowMode) {
            // 新規則：每放好三個方塊下降一格；若消除四行（Tetris），紅線上移兩格並重置計數
            boolean isTetris = (cleared == 4);
            if (isTetris) {
                minAllowedRow = Math.max(2, minAllowedRow - 2);
                normalLocksCounter = 0;
            } else {
                normalLocksCounter += 1;
                if (normalLocksCounter >= 3) {
                    int before = minAllowedRow;
                    minAllowedRow = Math.min(10, minAllowedRow + 1);
                    normalLocksCounter = 0;
                    if (!narrowMode && before < 10 && minAllowedRow >= 10) {
                        scheduleEnterNarrowMode();
                    }
                    if (minAllowedRow > before && isAboveDeathLineOccupied()) {
                        gameOver = true;
                        return;
                    }
                }
            }
        } else {
            // 四格寬模式邏輯
            if (cleared > 0) {
                if (!narrowFirstClearOccurred) {
                    narrowFirstClearOccurred = true;
                } else {
                    narrowComboCount += 1;
                    if (narrowComboCount >= 5) {
                        scheduleExitNarrowMode();
                    }
                }
            } else {
                if (narrowFirstClearOccurred) {
                    minAllowedRow = Math.min(15, minAllowedRow + 1);
                    narrowFirstClearOccurred = false;
                    narrowComboCount = 0;
                    // 判定線往下移動後，立即判斷是否遊戲結束
                    if (isAboveDeathLineOccupied()) {
                        gameOver = true;
                        return;
                    }
                }
            }
        }
        // Combo 顯示規則：第二次連消才顯示 x1
        if (cleared > 0) consecutiveClears++; else consecutiveClears = 0;
        notification.setCombo(consecutiveClears >= 2 ? (consecutiveClears - 1) : 0);
        // 消除行訊息（1 line, 2 lines, 3 lines, Tetris）
        notification.showLineClear(cleared, 1500);
        // ALL CLEAR：盤面全空時顯示 3 秒
        if (isBoardEmpty()) {
            notification.showAllClear(3000);
        }
        // Spin 顯示：僅當 (踢牆使用第 3 次以上且旋轉後無法下落) 或 T-Spin，且排除 O
        if (blockType != Tetromino.O.ordinal() && ((kickIndexUsed >= 3 && cannotDropAfterRotate) || currentTSpin)) {
            String name = Tetromino.values()[blockType].name();
            String text = (blockType == Tetromino.T.ordinal()) ? "T spin" : (name + " spin");
            notification.showSpin(text, 3000);
        } else {
            notification.clearSpin();
        }
        newBlock();
    }
    public String getLastSpinText() {
        return notification.getSpinText();
    }

    public int getCombo() { return notification.getCombo(); }

    public String getAllClearText() {
        return notification.getAllClearText();
    }

    public String getLineClearText() {
        return notification.getLineClearText();
    }

    public boolean isGameOver() { return gameOver; }

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

    // 是否有方塊位於死亡線以上（row < minAllowedRow）
    private boolean isAboveDeathLineOccupied() {
        int minX = narrowMode ? 3 : 0;
        int maxX = narrowMode ? 6 : board.getWidth() - 1;
        for (int iy = 0; iy < minAllowedRow; iy++) {
            for (int ix = minX; ix <= maxX; ix++) {
                if (board.getCell(ix, iy) != 0) return true;
            }
        }
        return false;
    }

    private void enterNarrowMode() {
        narrowMode = true;
        // 備份 10x20 盤面
        backupMap = new int[board.getWidth()][board.getHeight()];
        for (int ix = 0; ix < board.getWidth(); ix++) {
            for (int iy = 0; iy < board.getHeight(); iy++) {
                backupMap[ix][iy] = board.getCell(ix, iy);
            }
        }
        // 進入極窄模式時 combo 重新計算
        consecutiveClears = 0;
        notification.setCombo(0);
        narrowFirstClearOccurred = false;
        narrowComboCount = 0;
        // 普通模式鎖定計數器歸零（離開普通模式）
        normalLocksCounter = 0;
        // 先刪除滿四格寬（x=3..6 皆非 0）的列；僅壓縮中間四欄
        clearFullLinesNarrow();
    }

    private void exitNarrowMode() {
        // 恢復為 10x20：左右三欄從備份還原，中間四欄保留目前狀態
        if (backupMap != null) {
            for (int iy = 0; iy < board.getHeight(); iy++) {
                // 左側三欄 0..2
                for (int ix = 0; ix <= 2; ix++) {
                    board.setCell(ix, iy, backupMap[ix][iy]);
                }
                // 右側三欄 7..9
                for (int ix = 7; ix < board.getWidth(); ix++) {
                    board.setCell(ix, iy, backupMap[ix][iy]);
                }
            }
        }
        narrowMode = false;
        narrowFirstClearOccurred = false;
        narrowComboCount = 0;
        // 紅線回到第 18 格高（minAllowedRow = 2）
        minAllowedRow = 2;
        // 進入普通模式時 combo 重新計算
        consecutiveClears = 0;
        notification.setCombo(0);
        // 清除提示（可保留或清除，這裡保留其他通知不動）
    }

    private void restoreLeftColumnsFromBackup() {
        if (backupMap == null) return;
        for (int iy = 0; iy < board.getHeight(); iy++) {
            for (int ix = 0; ix <= 2; ix++) {
                board.setCell(ix, iy, backupMap[ix][iy]);
            }
        }
    }

    private void restoreRightColumnsFromBackup() {
        if (backupMap == null) return;
        for (int iy = 0; iy < board.getHeight(); iy++) {
            for (int ix = 7; ix < board.getWidth(); ix++) {
                board.setCell(ix, iy, backupMap[ix][iy]);
            }
        }
    }

    // 僅針對中間四欄（x=3..6）判定與清除滿列，並只壓縮中間四欄
    private int clearFullLinesNarrow() {
        int cleared = 0;
        int h = board.getHeight();
        // 從下往上逐列檢查，遇滿列則清除並上方中間四欄全部下移
        for (int ry = h - 1; ry >= 0; ry--) {
            boolean full = true;
            for (int ix = 3; ix <= 6; ix++) {
                if (board.getCell(ix, ry) == 0) { full = false; break; }
            }
            if (full) {
                cleared++;
                // 將 ry 之上的中間四欄下移一格
                for (int ty = ry; ty > 0; ty--) {
                    for (int ix = 3; ix <= 6; ix++) {
                        board.setCell(ix, ty, board.getCell(ix, ty - 1));
                    }
                }
                // 最頂列中間四欄清空
                for (int ix = 3; ix <= 6; ix++) {
                    board.setCell(ix, 0, 0);
                }
                ry++; // 因下移重新檢查同一行
            }
        }
        return cleared;
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

    private void scheduleEnterNarrowMode() {
        // 啟動 5 階段過渡
        transitionEntering = true;
        enterSteps = 5;
        // 重置過渡顯示旗標
        showChangeModeLabel = true;
        hideLeftDuringTransition = false;
        hideRightDuringTransition = false;
        hideOuterDuringTransition = false; // 舊旗標僅供退出流程使用
        showNarrowLabel = false;
        pendingCommitEnter = false;
        pendingCommitExit = false;
    }

    private void scheduleExitNarrowMode() {
        // 啟動 5 階段退出過渡
        transitionEntering = false;
        exitSteps = 5;
        showChangeModeLabel = true;
        showTryTetrisLabel = false;
        hideLeftDuringTransition = false;
        hideRightDuringTransition = false;
        hideOuterDuringTransition = false;
        forceShowOuterDuringTransition = false;
        showNarrowLabel = false;
        pendingCommitEnter = false;
        pendingCommitExit = false;
    }
}
