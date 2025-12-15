package model;

public class Board {
    private final int width = 10;
    private final int height = 20;
    private final int[][] map = new int[width][height];

    public int getWidth() { return width; }
    public int getHeight() { return height; }

    public int[][] getMap() { return map; }

    public void initMap() {
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                map[i][j] = 0;
            }
        }
    }

    public int getCell(int x, int y) { return map[x][y]; }

    public void setCell(int x, int y, int value) { map[x][y] = value; }

    // 清除已填滿的行，回傳清除的行數
    public int clearFullLines() {
        int idx = height - 1;
        int cleared = 0;
        for (int i = height - 1; i >= 0; i--) {
            int cnt = 0;
            for (int j = 0; j < width; j++) {
                if (map[j][i] != 0) cnt++;
            }
            if (cnt == width) {
                cleared++;
                for (int j = 0; j < width; j++) {
                    map[j][i] = 0;
                }
            } else {
                for (int j = 0; j < width; j++) {
                    map[j][idx] = map[j][i];
                }
                idx--;
            }
        }
        return cleared;
    }
}
