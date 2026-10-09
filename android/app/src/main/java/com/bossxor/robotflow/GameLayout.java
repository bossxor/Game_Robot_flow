package com.bossxor.robotflow;

/** Measured reference proportions, in a 384-unit logical viewport. */
final class GameLayout {
    static final float WIDTH = 384;
    static final float CARD = 48, CARD_DEPTH = 9, GAP = 12;
    static final float ACTIVE_Y = 6, QUEUE_Y = 68, ROW_STEP = 62;
    static float controlsHeight(float width, float usableHeight) {
        return Math.min(width * .80f, usableHeight * .42f);
    }
    static float rowLeft(int count) { return (WIDTH - (count * CARD + (count - 1) * GAP)) / 2; }
    static float columnX(int column, int count) { return rowLeft(count) + column * (CARD + GAP); }
    static float trayTop(float height) { return height - 96; }
    private GameLayout() {}
}
