package view;

import model.Board;
import model.Tetromino;
import controller.GameController;
import java.awt.*;

public class GhostPieceRenderer {

    /**
     * 計算並繪製方塊落點陰影
     */
    public void render(Graphics2D g2d, Board board, GameController controller, 
                       int x, int y, int blockType, int turnState, 
                       Image[] colorImages, int offsetX, int offsetY) {
        
        // 1. 計算陰影應該在的 Y 座標
        int ghostY = calculateGhostY(board, controller, x, y, blockType, turnState);

        // 2. 設定半透明度 (0.3f 表示 30% 不透明)
        Composite originalComposite = g2d.getComposite();
        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.3f));

        // 3. 繪製陰影方塊
        int[] rotation = Tetromino.values()[blockType].rotation(turnState);
        for (int i = 0; i < 16; i++) {
            if (rotation[i] == 1) {
                int drawX = (i % 4 + x) * 33 + 3 + 150 + offsetX;
                int drawY = (i / 4 + ghostY) * 33 + 3 + offsetY;
                g2d.drawImage(colorImages[blockType], drawX, drawY, null);
            }
        }

        // 4. 恢復原始透明度，避免影響後續繪圖
        g2d.setComposite(originalComposite);
    }

    private int calculateGhostY(Board board, GameController controller, 
                                int x, int y, int blockType, int turnState) {
        int ghostY = y;
        // 模擬方塊不斷下落，直到撞到東西為止
        while (controller.canPlace(board, x, ghostY + 1, blockType, turnState) == 1) {
            ghostY++;
        }
        return ghostY;
    }
}