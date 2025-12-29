package model;

public class Notification {
    private String spinText = "";
    private long spinUntil = 0L;

    private long allClearUntil = 0L; // 顯示固定文案 "ALL CLEAR"

    private int combo = 0;

    private String lineClearText = "";
    private long lineClearUntil = 0L;

    public void showSpin(String text, long ttlMs) {
        this.spinText = text != null ? text : "";
        this.spinUntil = System.currentTimeMillis() + Math.max(0, ttlMs);
    }

    public void clearSpin() {
        this.spinText = "";
        this.spinUntil = 0L;
    }

    public String getSpinText() {
        if (spinUntil == 0L) return "";
        return System.currentTimeMillis() <= spinUntil ? spinText : "";
    }

    public void showAllClear(long ttlMs) {
        this.allClearUntil = System.currentTimeMillis() + Math.max(0, ttlMs);
    }

    public void clearAllClear() {
        this.allClearUntil = 0L;
    }

    public String getAllClearText() {
        if (allClearUntil == 0L) return "";
        return System.currentTimeMillis() <= allClearUntil ? "ALL CLEAR" : "";
    }

    public void setCombo(int count) {
        this.combo = Math.max(0, count);
    }

    public int getCombo() { return combo; }

    public void showLineClear(int lines, long ttlMs) {
        if (lines <= 0) {
            this.lineClearText = "";
            this.lineClearUntil = 0L;
            return;
        }
        if (lines >= 4) {
            this.lineClearText = "Tetris";
        } else {
            this.lineClearText = lines + (lines == 1 ? " line" : " lines");
        }
        this.lineClearUntil = System.currentTimeMillis() + Math.max(0, ttlMs);
    }

    public String getLineClearText() {
        if (lineClearUntil == 0L) return "";
        return System.currentTimeMillis() <= lineClearUntil ? lineClearText : "";
    }
}
