package com.bossxor.robotflow;

/** Measured reference proportions, in a 384-unit logical viewport. */
final class GameLayout {
    static final float WIDTH = 384;
    static final float CARD = 48, CARD_DEPTH = 9, GAP = 12;
    static final float ACTIVE_Y = 6, QUEUE_Y = 68, ROW_STEP = 60;
    static float controlsHeight(float width, float usableHeight) {
        return Math.min(width * 320 / WIDTH, usableHeight * .44f);
    }
    static int rowCapacity(float logicalHeight){return Math.max(1,Math.min(3,(int)((trayTop(logicalHeight)-QUEUE_Y-CARD-CARD_DEPTH)/ROW_STEP)+1));}
    static float compactHeight(float maxHeight,float width,int remainingRows){float scale=width/WIDTH;int capacity=rowCapacity(maxHeight/scale);return maxHeight-(capacity-Math.min(capacity,Math.max(0,remainingRows)))*ROW_STEP*scale;}
    static float rowLeft(int count) { return (WIDTH - (count * CARD + (count - 1) * GAP)) / 2; }
    static float columnX(int column, int count) { return rowLeft(count) + column * (CARD + GAP); }
    static int[] liveLanes(int[] counts) {
        int count=0;for(int n:counts)if(n>0)count++;
        int[] lanes=new int[count];int at=0;
        for(int i=0;i<counts.length;i++)if(counts[i]>0)lanes[at++]=i;
        return lanes;
    }
    static float trayTop(float height) { return height - 72; }
    private GameLayout() {}
}
