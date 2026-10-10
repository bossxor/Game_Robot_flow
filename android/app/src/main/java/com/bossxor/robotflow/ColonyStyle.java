package com.bossxor.robotflow;

import android.content.Context;
import android.graphics.Typeface;

/** Rounded bold UI typeface matching Colony Flow's bubbly labels. Free Nunito ExtraBold (OFL). */
final class ColonyStyle {
    static final int HOME_SKY_TOP = 0xff8fd4f0;
    static final int HOME_SKY_MID = 0xffb8e8a8;
    static final int HOME_SKY_BOT = 0xff6bc46a;
    static final int GAME_BG = 0xff9a90b8;
    static final int PAUSE_PINK = 0xffe85a9a;
    static final int SPEED_GOLD = 0xfff6c84b;
    static final int SPEED_INK = 0xff5a3a18;
    static final int LEVEL_INK = 0xff3d3a4a;
    static final int PLAY_GREEN = 0xff58d04a;
    static final int PLAY_EDGE = 0xffffffff;
    static final int NAV_BAR = 0xff2f3d6e;
    static final int SLOT_WHITE = 0xfffff8ff;
    static final int TRAY_CHECKER_A = 0xffc8b8e0;
    static final int TRAY_CHECKER_B = 0xffb8a8d4;
    static final int COIN_PILL = 0xff3aa0e8;
    static Typeface bold;
    static Typeface load(Context context) {
        if (bold == null) {
            try { bold = Typeface.createFromAsset(context.getAssets(), "fonts/Nunito-ExtraBold.ttf"); }
            catch (Exception e) { bold = Typeface.create("sans-serif-black", Typeface.BOLD); }
        }
        return bold;
    }
    private ColonyStyle() {}
}
