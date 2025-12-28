package model;

// Guideline SRS 踢牆表（Kick Tables）
// 兩組表：一組給 S/Z/J/L/T，另一組給 I；O 不使用踢牆表。
// 0: 0→1 (CW), 1: 1→2 (CW), 2: 2→3 (CW), 3: 3→0 (CW)
// 4: 0→3 (CCW), 5: 3→2 (CCW), 6: 2→1 (CCW), 7: 1→0 (CCW)
// 每個索引下有 5 組嘗試偏移 (dx, dy)。
// 方向定義：dx>0 向右、dx<0 向左；dy>0 向上、dy<0 向下。

public final class SRSSystem {
	// [8 個 from→to] x [5 次嘗試] x [2: dx, dy]
	private static final int[][][] KICKS_JLSTZ = new int[][][] {
		{{ 0, 0 }, { -1, 0 }, { -1, 1 }, { 0, -2 }, { -1, -2 }}, // 0→1
		{{ 0, 0 }, { 1, 0 }, { 1, -1 }, { 0, 2 }, { 1, 2 }},     // 1→2
		{{ 0, 0 }, { 1, 0 }, { 1, 1 }, { 0, -2 }, { 1, -2 }},    // 2→3
		{{ 0, 0 }, { -1, 0 }, { -1, -1 }, { 0, 2 }, { -1, 2 }},  // 3→0
		
		{{ 0, 0 }, { 1, 0 }, { 1, 1 }, { 0, -2 }, { 1, -2 }},    // 0→3
		{{ 0, 0 }, { -1, 0 }, { -1, -1 }, { 0, 2 }, { -1, 2 }},  // 3→2
		{{ 0, 0 }, { -1, 0 }, { -1, 1 }, { 0, -2 }, { -1, -2 }}, // 2→1
		{{ 0, 0 }, { 1, 0 }, { 1, -1 }, { 0, 2 }, { 1, 2 }}      // 1→0
	};
	private static final int[][][] KICKS_I = new int[][][]{
		{{ 0, 0 }, { -2, 0 }, { 1, 0 }, { -2, -1 }, { 1, 2 }},   // 0→1
		{{ 0, 0 }, { -1, 0 }, { 2, 0 }, { -1, 2 }, { 2, -1 }},   // 1→2
		{{ 0, 0 }, { 2, 0 }, { -1, 0 }, { 2, 1 }, { -1, -2 }},   // 2→3
		{{ 0, 0 }, { 1, 0 }, { -2, 0 }, { 1, -2 }, { -2, 1 }},   // 3→0
		
		{{ 0, 0 }, { 2, 0 }, { -1, 0 }, { 2, 1 }, { -1, -2 }},   // 0→3
		{{ 0, 0 }, { 1, 0 }, { -2, 0 }, { 1, -2 }, { -2, 1 }},   // 3→2
		{{ 0, 0 }, { -2, 0 }, { 1, 0 }, { -2, -1 }, { 1, 2 }},   // 2→1
		{{ 0, 0 }, { -1, 0 }, { 2, 0 }, { -1, 2 }, { 2, -1 }}    // 1→0
	};

	private SRSSystem() {}

	// 取得指定方塊類型與旋轉 from→to 的 5 組偏移
	public static int[][] getKicks(int typeIndex, int fromState, int toState) {
		int idx = indexFor(fromState, toState);
		if (idx < 0) return new int[0][2];
		// O 不使用踢牆表
		if (typeIndex == Tetromino.O.ordinal()) return new int[0][2];
		if (typeIndex == Tetromino.I.ordinal()) return KICKS_I[idx];
		return KICKS_JLSTZ[idx];
	}

	// 內部：計算 from→to 對應的索引，CW 前四，CCW 後四
	private static int indexFor(int from, int to) {
		int cwTo = (from + 1) & 3;
		int ccwTo = (from + 3) & 3;
		if (to == cwTo) {
			// 0→1, 1→2, 2→3, 3→0 分別為 0,1,2,3
			return from & 3;
		}
		if (to == ccwTo) {
			// 0→3, 3→2, 2→1, 1→0 分別為 4,5,6,7
			switch (from & 3) {
				case 0: return 4;
				case 3: return 5;
				case 2: return 6;
				case 1: return 7;
				default: return -1;
			}
		}
		return -1;
	}
}
