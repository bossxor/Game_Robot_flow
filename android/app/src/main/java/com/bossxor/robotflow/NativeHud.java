package com.bossxor.robotflow;

import android.content.Context;
import android.graphics.*;
import android.os.SystemClock;
import android.view.*;
import android.view.accessibility.*;
import java.util.*;

/** Square containers, centered lanes, raised item dock and a compact header. */
public final class NativeHud extends View {
    public interface Actions { void action(String key, int value); }
    private final NativeGame game;
    private final PlayerSave save;
    private final Actions actions;
    private final boolean header;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final float density;
    public boolean pullMode;
    public boolean clearMode;
    public int page, speed = 1;
    private static final int INK = 0xff353b43, BACK = 0xfff1dfbd, CREAM = 0xfffff3df;
    private final ArrayList<Target> targets = new ArrayList<>();
    private final Map<Integer, Motion> motions = new HashMap<>();
    private Target pressed;
    private float downY;
    private int accessibilityFocus = -1;
    private boolean animating;

    private static class Target {
        final RectF rect; final String key; final int id;
        Target(RectF rect, String key, int id) { this.rect = rect; this.key = key; this.id = id; }
    }
    private static class Motion {
        float x, y, fromX, fromY, goalX, goalY;
        long began;
        Motion(float x, float y) { this.x = this.fromX = this.goalX = x; this.y = this.fromY = this.goalY = y; }
    }
    public NativeHud(Context context, NativeGame game, PlayerSave save, boolean header, Actions actions) {
        super(context); this.game = game; this.save = save; this.header = header; this.actions = actions;
        density = getResources().getDisplayMetrics().density;
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_YES);
        setContentDescription(header ? "스테이지, 일시정지, 배속" : "컨테이너 대기열과 네 가지 아이템");
    }
    private void label(Canvas canvas, String text, float x, float y, float size, int color) {
        paint.setShader(null); paint.setStyle(Paint.Style.FILL); paint.setColor(color);
        paint.setTextSize(size); paint.setTypeface(Typeface.DEFAULT_BOLD); paint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText(text, x, y, paint);
    }
    private void box(Canvas canvas, float x, float y, float w, float h, int color, float radius) {
        paint.setShader(null); paint.setStyle(Paint.Style.FILL); paint.setColor(color);
        canvas.drawRoundRect(x, y, x + w, y + h, radius, radius, paint);
    }
    private void hit(float x, float y, float w, float h, String key, int id) {
        targets.add(new Target(new RectF(x, y, x + w, y + h), key, id));
    }
    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas); canvas.save(); canvas.scale(density, density);
        float width = getWidth() / density, height = getHeight() / density;
        canvas.drawColor(BACK); targets.clear(); animating = false;
        synchronized (game) { if (header) drawHeader(canvas, width); else drawControls(canvas, width, height); }
        canvas.restore();
        if (animating && !game.paused) postInvalidateOnAnimation();
    }
    private void drawHeader(Canvas c, float width) {
        box(c, 13, 26, 38, 38, 0xffd4b895, 12);
        box(c, 13, 23, 38, 38, CREAM, 12);
        GameIcons.draw(c, paint, 5, 32, 42, 27); hit(8, 18, 48, 48, "pause", 0);
        label(c, String.format(Locale.KOREA, "STAGE %03d", game.level.id), width / 2, 53, 25, INK);
        box(c, width - 86, 26, 72, 38, 0xffbe945e, 12);
        box(c, width - 86, 23, 72, 38, 0xffffca76, 12);
        label(c, "▶ " + (game.finishing?3:speed) + "×", width - 50, 49, 17, INK); hit(width - 90, 18, 80, 48, "speed", 0);
        label(c,new String[]{"NORMAL","HARD","SUPER HARD"}[game.level.difficulty],width/2,65,8,game.level.difficulty==2?0xffb66a95:game.level.difficulty==1?0xffb56b36:MUTED_COLOR);
        // The builder gauge stays close to the title instead of splitting the container rows.
        box(c, width / 2 - 42, 69, 84, 3, 0xffdac6a6, 2);
        if (game.gauge > 0) box(c, width / 2 - 42, 69, 84f * game.gauge / game.level.megaRequired, 3, 0xffee9d4d, 2);
    }
    private static final int MUTED_COLOR=0xff93806b;
    private static int shade(int color, float factor) {
        return Color.rgb(Math.min(255, (int)(Color.red(color) * factor)), Math.min(255, (int)(Color.green(color) * factor)), Math.min(255, (int)(Color.blue(color) * factor)));
    }
    private void number(Canvas c, String value, float x, float y, float width, boolean bright) {
        paint.setShader(null); paint.setTypeface(Typeface.DEFAULT_BOLD); paint.setTextAlign(Paint.Align.CENTER);
        float size = 26; paint.setTextSize(size);
        while (paint.measureText(value) > width - 6 && size > 15) paint.setTextSize(--size);
        paint.setStyle(Paint.Style.STROKE); paint.setStrokeJoin(Paint.Join.ROUND); paint.setStrokeWidth(2.5f);
        paint.setColor(bright ? INK : 0x99505258); c.drawText(value, x, y, paint);
        paint.setStyle(Paint.Style.FILL); paint.setColor(bright ? Color.WHITE : 0xffd4cec6); c.drawText(value, x, y, paint);
    }
    private void card(Canvas c, NativeGame.Capsule capsule, float x, float y, boolean active, boolean front, boolean touched) {
        float size = GameLayout.CARD;
        c.save(); if (touched) { c.translate(x + size / 2, y + size / 2); c.scale(.96f, .96f); c.translate(-x - size / 2, -y - size / 2); }
        int color = NativeGame.COLORS[capsule.color];
        box(c, x - 1, y + 6, size + 2, size + 7, 0x22000000, 11);
        box(c, x, y + GameLayout.CARD_DEPTH, size, size, shade(color, .64f), 10);
        box(c, x, y + 5, size, size, shade(color, .83f), 10);
        paint.setShader(new LinearGradient(x, y, x, y + size, shade(color, 1.12f), shade(color, .95f), Shader.TileMode.CLAMP));
        paint.setStyle(Paint.Style.FILL); c.drawRoundRect(x, y, x + size, y + size, 10, 10, paint); paint.setShader(null);
        box(c, x + 5, y + 4, size - 10, 3, 0x55ffffff, 2);
        if (!front && !active) box(c, x, y, size, size + 7, 0x28000000, 10);
        if (front && !active) { paint.setStyle(Paint.Style.STROKE); paint.setStrokeWidth(1.8f); paint.setColor(CREAM); c.drawRoundRect(x - 1, y - 1, x + size + 1, y + size + 9, 11, 11, paint); paint.setStyle(Paint.Style.FILL); }
        number(c, String.valueOf(active ? Math.max(0, capsule.count - capsule.loaded) : capsule.remaining), x + size / 2, y + 30, size, active || front);
        c.restore();
    }
    private Motion position(int id, float x, float y, long now) {
        Motion m = motions.get(id);
        if (m == null) { m = new Motion(x, y); motions.put(id, m); }
        if (m.goalX != x || m.goalY != y) { m.fromX = m.x; m.fromY = m.y; m.goalX = x; m.goalY = y; m.began = now; }
        float t = Math.min(1, (now - m.began) / 220f), ease = 1 - (float)Math.pow(1 - t, 3);
        m.x = m.fromX + (m.goalX - m.fromX) * ease; m.y = m.fromY + (m.goalY - m.fromY) * ease;
        if (t < 1) animating = true;
        return m;
    }
    private boolean touched(String key, int id) { return pressed != null && pressed.key.equals(key) && pressed.id == id; }
    private void drawControls(Canvas c, float width, float height) {
        float scale = width / GameLayout.WIDTH, logicalHeight = height / scale;
        c.save(); c.scale(scale, scale); long now = SystemClock.elapsedRealtime();
        List<List<NativeGame.Capsule>> lanes = new ArrayList<>(); int max = 0;
        for (int lane = 0; lane < 4; lane++) {
            List<NativeGame.Capsule> row = new ArrayList<>();
            for (NativeGame.Capsule a : game.queue) if (!a.taken && a.lane == lane) row.add(a);
            row.sort(Comparator.comparingInt(a -> a.depth)); lanes.add(row); max = Math.max(max, row.size());
        }
        int rows=Math.max(2,Math.min(5,(int)((GameLayout.trayTop(logicalHeight)-GameLayout.QUEUE_Y-GameLayout.CARD-GameLayout.CARD_DEPTH)/GameLayout.ROW_STEP)+1));
        page = Math.min(page, Math.max(0, max - rows));
        for (int lane = 0; lane < lanes.size(); lane++) for (int row = 0; row < rows; row++) {
            int index = page + row; if (index >= lanes.get(lane).size()) continue;
            NativeGame.Capsule a = lanes.get(lane).get(index);
            float x = GameLayout.columnX(lane, 4), y = GameLayout.QUEUE_Y + row * GameLayout.ROW_STEP;
            Motion m = position(a.id, x, y, now);
            card(c, a, m.x, m.y, false, game.unlocked(a), touched("capsule", a.id));
            if(a.group>0){paint.setShader(null);paint.setColor(0xfffff3df);paint.setStrokeWidth(4);NativeGame.Capsule next=null;for(NativeGame.Capsule other:game.queue)if(!other.taken&&other.group==a.group&&other.lane==a.lane+1&&other.depth==a.depth)next=other;if(next!=null)c.drawLine(x+GameLayout.CARD-2,y+25,x+GameLayout.CARD+GameLayout.GAP+2,y+25,paint);}
            if(game.unlocked(a)&&game.risky(a)){paint.setStyle(Paint.Style.STROKE);paint.setColor(0xffd86c52);paint.setStrokeWidth(2);c.drawRoundRect(x-2,y-2,x+50,y+50,12,12,paint);paint.setStyle(Paint.Style.FILL);}
            hit((x - 4) * scale, (y - 3) * scale, (GameLayout.CARD + 8) * scale, GameLayout.ROW_STEP * scale, "capsule", a.id);
        }
        // Draw flying containers after the queue so their move into the work area is readable.
        for (int slot = 0; slot < game.slots.size(); slot++) {
            float slotX=GameLayout.columnX(slot,game.slots.size());
            box(c,slotX-1,GameLayout.ACTIVE_Y+4,50,54,0xffc8b294,11);box(c,slotX,GameLayout.ACTIVE_Y,48,48,0xfffff3df,10);
            NativeGame.Capsule a = game.slots.get(slot); if (a == null) continue;
            Motion m = position(a.id, GameLayout.columnX(slot, game.slots.size()), GameLayout.ACTIVE_Y, now);
            card(c, a, m.x, m.y, true, true, false);
            boolean light=game.time-a.lastSpawnAt<.18||((game.time-a.lastSpawnAt)>.36&&a.remaining>0);
            box(c,m.x+9,m.y+2,30,4,light?0xffffe49c:shade(NativeGame.COLORS[a.color],.7f),2);
            for(NativeGame.Bot bot:game.bots)if(bot.capsuleId==a.id&&bot.phase==5){float t=Math.min(1,(float)(bot.phaseTime/NativeGame.LAUNCH_TIME)),px=m.x+24,py=m.y+24-t*34;box(c,px-7,py-9,14,14,NativeGame.COLORS[a.color],4);box(c,px-5,py-5,10,5,INK,2);box(c,px-3,py-4,2,2,CREAM,1);box(c,px+1,py-4,2,2,CREAM,1);box(c,px-5,py+5,3,4,CREAM,1);box(c,px+2,py+5,3,4,CREAM,1);}
        }
        if (pullMode) label(c, "꺼낼 컨테이너를 선택하세요", 192, 65, 9, INK);
        if (clearMode) label(c,"없앨 색의 블록을 터치하세요",192,65,9,INK);
        float tray = GameLayout.trayTop(logicalHeight);
        box(c, 0, tray + 2, 384, 110, 0xffb98b55, 24);
        box(c, 2, tray, 380, 110, 0xffe1c49b, 24);
        box(c, 5, tray + 4, 374, 106, 0xffecd6b3, 21);
        for (int i = 0; i < 4; i++) {
            float x = 61 + i * 87, y = tray + 18;
            paint.setColor(0x22000000); c.drawCircle(x, y + 4, 29, paint);
            paint.setColor(0xffbe9465); c.drawCircle(x, y + 2, 29, paint);
            paint.setColor((pullMode && i == 3)||(clearMode&&i==1) ? 0xffffca76 : CREAM); c.drawCircle(x, y, 28, paint);
            GameIcons.draw(c, paint, i, x, y - 1, 42);
            paint.setColor(0xff6e9468); c.drawCircle(x + 20, y + 22, 12, paint);
            paint.setColor(0xffa7d883); c.drawCircle(x + 20, y + 20, 11, paint);
            label(c, save.boosterCount(i), x + 20, y + 25, save.unlimitedItems ? 17 : 14, INK);
            hit((x - 32) * scale, (y - 30) * scale, 64 * scale, 64 * scale, "booster", i);
        }
        c.restore();
    }
    private String description(Target t) {
        if (t.key.equals("capsule")) { NativeGame.Capsule a = game.capsule(t.id); return NativeGame.COLOR_NAMES[a.color] + " 컨테이너, " + a.remaining + "개, " + (game.unlocked(a) ? "선택 가능" : "뒤쪽"); }
        if (t.key.equals("booster")) return new String[]{"작업 공간 추가", "컬러 제거", "컨테이너 섞기", "뒤쪽 컨테이너 꺼내기"}[t.id] + ", " + save.boosterCount(t.id);
        return t.key.equals("pause") ? "일시정지" : "배속 " + speed;
    }
    @Override public AccessibilityNodeProvider getAccessibilityNodeProvider() {
        return new AccessibilityNodeProvider() {
            @Override public AccessibilityNodeInfo createAccessibilityNodeInfo(int id) {
                AccessibilityNodeInfo node = AccessibilityNodeInfo.obtain();
                if (id == View.NO_ID) { node.setSource(NativeHud.this); NativeHud.this.onInitializeAccessibilityNodeInfo(node); for (int i = 0; i < targets.size(); i++) node.addChild(NativeHud.this, i); return node; }
                if (id < 0 || id >= targets.size()) return null; Target t = targets.get(id);
                node.setSource(NativeHud.this, id); node.setParent(NativeHud.this); node.setClassName("android.widget.Button"); node.setPackageName(getContext().getPackageName()); node.setContentDescription(description(t)); node.setEnabled(true); node.setClickable(true); node.setFocusable(true); node.setVisibleToUser(isShown());
                Rect r = new Rect((int)(t.rect.left * density), (int)(t.rect.top * density), (int)(t.rect.right * density), (int)(t.rect.bottom * density)); node.setBoundsInParent(r); int[] at = new int[2]; getLocationOnScreen(at); r.offset(at[0], at[1]); node.setBoundsInScreen(r); node.addAction(AccessibilityNodeInfo.ACTION_CLICK); node.setAccessibilityFocused(accessibilityFocus == id); node.addAction(accessibilityFocus == id ? 128 : 64); return node;
            }
            @Override public boolean performAction(int id, int action, android.os.Bundle args) {
                if (id < 0 || id >= targets.size()) return false;
                if (action == 16) { Target t = targets.get(id); actions.action(t.key, t.id); return true; }
                if (action == 64 || action == 128) { accessibilityFocus = action == 64 ? id : -1; AccessibilityEvent e = AccessibilityEvent.obtain(action == 64 ? 32768 : 65536); e.setSource(NativeHud.this, id); e.setPackageName(getContext().getPackageName()); e.setClassName("android.widget.Button"); getParent().requestSendAccessibilityEvent(NativeHud.this, e); invalidate(); return true; }
                return false;
            }
        };
    }
    @Override public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX() / density, y = event.getY() / density;
        if (event.getAction() == MotionEvent.ACTION_DOWN) { downY = y; pressed = null; for (Target t : targets) if (t.rect.contains(x, y)) pressed = t; invalidate(); return true; }
        if (event.getAction() == MotionEvent.ACTION_UP) {
            Target selected = pressed; pressed = null;
            float scale = getWidth() / density / GameLayout.WIDTH;
            if (!header && Math.abs(y - downY) > 30 && downY > GameLayout.QUEUE_Y * scale && downY < GameLayout.trayTop(getHeight() / density / scale) * scale) { page = Math.max(0, page + (y < downY ? 1 : -1)); invalidate(); return true; }
            if (selected != null && selected.rect.contains(x, y)) { performClick(); actions.action(selected.key, selected.id); }
            invalidate(); return true;
        }
        if (event.getAction() == MotionEvent.ACTION_CANCEL) { pressed = null; invalidate(); }
        return true;
    }
    @Override public boolean performClick() { super.performClick(); return true; }
}
