package model;

// 通用遊戲設定（ARR/DAS/SDF），後續三種模式共用
public class GameSettings {
    private int arrMs = 50;  // Auto Repeat Rate
    private int dasMs = 170; // Delayed Auto Shift
    private int sdf = 20;    // Soft Drop Factor（倍率）

    public int getArrMs() { return arrMs; }
    public void setArrMs(int arrMs) { this.arrMs = Math.max(0, arrMs); }

    public int getDasMs() { return dasMs; }
    public void setDasMs(int dasMs) { this.dasMs = Math.max(0, dasMs); }

    public int getSdf() { return sdf; }
    public void setSdf(int sdf) { this.sdf = Math.max(1, sdf); }
}
