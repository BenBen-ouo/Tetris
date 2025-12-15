package model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

// 7-Bag 生成器：每袋包含 0~6 各一次，隨機排序，用完再補一袋
public class PieceGenerator {
    private final List<Integer> bag = new ArrayList<>();

    public PieceGenerator() {
        refill();
    }

    private void refill() {
        bag.clear();
        for (int i = 0; i < 7; i++) bag.add(i);
        Collections.shuffle(bag);
    }

    public int next() {
        if (bag.isEmpty()) refill();
        return bag.remove(bag.size() - 1);
    }
}
