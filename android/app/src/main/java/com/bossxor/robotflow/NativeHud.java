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
        boolean rocket;
        float progress;
        NativeGame.Capsule cargo;
        Motion(float x, float y) { this.x = this.fromX = this.goalX = x; this.y = this.fromY = this.goalY = y; }
        void advance(long now) {
            progress = Math.min(1, (now - began) / (rocket ? 420f : 280f));
            float t = rocket ? Math.max(0, Math.min(1, (progress - .08f) / .74f)) : progress;
            float ease = rocket ? t*t*(3-2*t) : 1-(float)Math.pow(1-t,3);
            x=fromX+(goalX-fromX)*ease; y=fromY+(goalY-fromY)*ease;
            if(rocket)y-=(float)Math.sin(Math.PI*t)*14;
        }
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
        canvas.drawColor(new int[]{0xffeee4cd,0xffdcc9e8,0xffccdfe9}[game.level.theme]); targets.clear(); animating = false;
        synchronized (game) { if (header) drawHeader(canvas, width); else drawControls(canvas, width, height); }
        canvas.restore();
        if (animating && !game.paused) postInvalidateOnAnimation();
    }
    private void drawHeader(Canvas c, float width) {
        box(c, 13, 26, 38, 38, 0xffd4b895, 12);
        box(c, 13, 23, 38, 38, CREAM, 12);
        GameIcons.draw(c, paint, 5, 32, 42, 27); hit(8, 18, 48, 48, "pause", 0);
        label(c, "Level " + game.level.id, width / 2, 43, 23, INK);
        box(c, width - 86, 26, 72, 38, 0xffbe945e, 12);
        box(c, width - 86, 23, 72, 38, 0xffffca76, 12);
        label(c, "▶ " + (game.finishing?NativeGame.FINISH_SPEED:speed) + "×", width - 50, 49, 17, INK); hit(width - 90, 18, 80, 48, "speed", 0);
        float badgeWidth=game.level.difficulty==2?110:78;
        int badgeColor=new int[]{0xff3677e7,0xff9644db,0xffef365c}[game.level.difficulty];
        box(c,width/2-badgeWidth/2,49,badgeWidth,22,shade(badgeColor,.65f),9);
        box(c,width/2-badgeWidth/2,47,badgeWidth,22,badgeColor,9);
        label(c,new String[]{"Normal","Hard","Super Hard"}[game.level.difficulty],width/2,63,12,Color.WHITE);
        // The builder gauge stays close to the title instead of splitting the container rows.
        box(c, width / 2 - 42, 75, 84, 3, 0xffdac6a6, 2);
        if (game.gauge > 0) box(c, width / 2 - 42, 75, 84f * game.gauge / game.level.megaRequired, 3, 0xffee9d4d, 2);
    }
    private static final int MUTED_COLOR=0xff93806b;
    private static final int[] CARD_COLORS=NativeGame.COLORS;
    private static int shade(int color, float factor) {
        return Color.rgb(Math.min(255, (int)(Color.red(color) * factor)), Math.min(255, (int)(Color.green(color) * factor)), Math.min(255, (int)(Color.blue(color) * factor)));
    }
    private void number(Canvas c, String value, float x, float y, float width, boolean bright) {
        paint.setShader(null); paint.setTypeface(Typeface.create("sans-serif-condensed",Typeface.BOLD)); paint.setTextAlign(Paint.Align.CENTER);
        float size = 28; paint.setTextSize(size);
        while (paint.measureText(value) > width - 6 && size > 15) paint.setTextSize(--size);
        paint.setStyle(Paint.Style.STROKE); paint.setStrokeJoin(Paint.Join.ROUND); paint.setStrokeWidth(3.5f);
        paint.setColor(0xff263344); c.drawText(value, x, y, paint);
        paint.setStyle(Paint.Style.FILL); paint.setColor(bright ? Color.WHITE : 0xffeeeefa); c.drawText(value, x, y, paint);
    }
    private void card(Canvas c, NativeGame.Capsule capsule, float x, float y, boolean active, boolean front, boolean touched) {
        float size = GameLayout.CARD;
        c.save(); if (touched) { c.translate(x + size / 2, y + size / 2); c.scale(.96f, .96f); c.translate(-x - size / 2, -y - size / 2); }
        boolean visible=active||game.visible(capsule);
        int color = visible?CARD_COLORS[capsule.color]:0xff646779;
        box(c,x-2,y+7,size+4,size+6,0x290b1930,13);
        box(c,x-1,y+GameLayout.CARD_DEPTH,size+2,size,0xfffffaf1,12);
        box(c,x+1,y+7,size-2,size,shade(color,.61f),11);
        paint.setShader(new LinearGradient(x,y,x,y+size,new int[]{shade(color,1.14f),color,shade(color,.88f)},new float[]{0,.45f,1},Shader.TileMode.CLAMP));
        paint.setStyle(Paint.Style.FILL);c.drawRoundRect(x,y,x+size,y+size,12,12,paint);paint.setShader(null);
        box(c,x+5,y+4,size-10,5,0x45ffffff,4);
        box(c,x+3,y+11,3,size-24,0x22ffffff,2);
        if(!front&&!active)box(c,x,y,size,size+7,0x180d153d,12);
        if(front&&!active){paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(2);paint.setColor(Color.WHITE);c.drawRoundRect(x-1,y-1,x+size+1,y+size+9,13,13,paint);paint.setStyle(Paint.Style.FILL);}
        number(c,visible?String.valueOf(active?Math.max(0,capsule.count-capsule.loaded):capsule.remaining):"?",x+size/2,y+32,size,active||front);
        if(!active&&game.keyLocked(capsule)){
            paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(3);paint.setColor(0xffffdf37);c.drawRoundRect(x+33,y-7,x+43,y+7,5,5,paint);paint.setStyle(Paint.Style.FILL);
            box(c,x+29,y+1,18,14,0xffffdf37,4);box(c,x+36,y+6,4,5,INK,2);
        }
        c.restore();
    }
    private Motion position(int id, float x, float y, long now) {
        Motion m = motions.get(id);
        if (m == null) { m = new Motion(x, y); motions.put(id, m); }
        if (m.goalX != x || m.goalY != y) { m.fromX = m.x; m.fromY = m.y; m.goalX = x; m.goalY = y; m.began = now; m.rocket=false; }
        m.advance(now);
        if (m.progress < 1) animating = true;
        return m;
    }
    private Motion launch(NativeGame.Capsule capsule,float x,long now) {
        Motion m=motions.get(capsule.id);
        if(m==null){m=new Motion(GameLayout.columnX(capsule.lane,4),GameLayout.QUEUE_Y);motions.put(capsule.id,m);}
        if(m.goalY!=GameLayout.ACTIVE_Y){
            m.fromX=m.x;m.fromY=m.y;m.goalX=x;m.goalY=GameLayout.ACTIVE_Y;m.began=now;
            m.rocket=android.animation.ValueAnimator.areAnimatorsEnabled();
            m.cargo=new NativeGame.Capsule(capsule.id,capsule.color,Math.max(0,capsule.count-capsule.loaded),capsule.depth);m.cargo.group=capsule.group;
            if(!m.rocket){m.fromX=x;m.fromY=GameLayout.ACTIVE_Y;}
        }
        m.goalX=x;m.advance(now);return m;
    }
    public void captureLaunches() {
        synchronized(game){
            long now=SystemClock.elapsedRealtime();
            for(int slot=0;slot<game.slots.size();slot++){
                NativeGame.Capsule cargo=game.slots.get(slot);
                if(cargo!=null)launch(cargo,GameLayout.columnX(slot,game.slots.size()),now);
            }
        }
    }
    private void rocket(Canvas c,Motion m,long now) {
        float p=m.progress,size=GameLayout.CARD;
        if(p>=1)return;
        animating=true;c.save();c.translate(m.x+size/2,m.y+size/2);
        float travel=(float)Math.sin(Math.PI*Math.min(1,p/.82f));
        c.rotate(Math.signum(m.goalX-m.fromX)*10*travel);
        float squash=p<.08f?1-.06f*p/.08f:p>.82f?1-.06f*(float)Math.sin((p-.82f)/.18f*Math.PI):1;
        c.scale(1+(1-squash)*.5f,squash);c.translate(-size/2,-size/2);
        if(p<.82f){
            float flame=15+8*(float)Math.sin(now*.09),base=size+9;
            box(c,size/2-8,size+4,16,8,0xff657b98,4);
            Path plume=new Path();plume.moveTo(size/2-8,base);plume.quadTo(size/2-12,base+flame*.55f,size/2,base+flame);plume.quadTo(size/2+12,base+flame*.55f,size/2+8,base);plume.close();
            paint.setColor(0xffff9748);c.drawPath(plume,paint);
            plume.reset();plume.moveTo(size/2-4,base);plume.lineTo(size/2,base+flame*.72f);plume.lineTo(size/2+4,base);plume.close();paint.setColor(0xfffff1a6);c.drawPath(plume,paint);
            box(c,-4,size-10,8,19,0xffd2eef8,4);box(c,size-4,size-10,8,19,0xffd2eef8,4);
        }
        card(c,m.cargo,0,0,true,true,false);c.restore();
    }
    private boolean touched(String key, int id) { return pressed != null && pressed.key.equals(key) && pressed.id == id; }
    private static class LinkNode {
        final NativeGame.Capsule cargo;final float x,y;
        LinkNode(NativeGame.Capsule cargo,float x,float y){this.cargo=cargo;this.x=x;this.y=y;}
    }
    private void links(Canvas canvas,List<LinkNode> nodes) {
        Map<Integer,List<LinkNode>> groups=new LinkedHashMap<>();
        for(LinkNode n:nodes)if(n.cargo.group>0)groups.computeIfAbsent(n.cargo.group,k->new ArrayList<>()).add(n);
        for(List<LinkNode> group:groups.values()){
            List<LinkNode> joined=new ArrayList<>();joined.add(group.remove(0));
            while(!group.isEmpty()){
                LinkNode a=null,b=null;double best=Double.MAX_VALUE;
                for(LinkNode from:joined)for(LinkNode to:group){double d=Math.hypot(from.x-to.x,from.y-to.y);if(d<best){best=d;a=from;b=to;}}
                connector(canvas,a,b);joined.add(b);group.remove(b);
            }
        }
    }
    private void connector(Canvas canvas,LinkNode a,LinkNode b) {
        float size=GameLayout.CARD,ax=a.x+size/2,ay=a.y+size/2,bx=b.x+size/2,by=b.y+size/2;
        float dx=bx-ax,dy=by-ay;if(Math.hypot(dx,dy)<1)return;
        Path path=new Path();
        if(Math.abs(dy)<size*.2f&&Math.abs(dx)>size*1.8f){
            float rail=Math.min(a.y,b.y)-9;boolean below=rail<8;if(below)rail=Math.max(a.y,b.y)+size+9;
            path.moveTo(ax,a.y+(below?size-5:5));path.lineTo(ax,rail);path.lineTo(bx,rail);path.lineTo(bx,b.y+(below?size-5:5));
        }else if(Math.abs(dx)<size*.2f&&Math.abs(dy)>size*1.8f){
            float right=Math.max(a.x,b.x)+size+9;path.moveTo(a.x+size-5,ay);path.lineTo(right,ay);path.lineTo(right,by);path.lineTo(b.x+size-5,by);
        }else{
            float inset=(size/2-5)/Math.max(Math.abs(dx),Math.abs(dy));
            path.moveTo(ax+dx*inset,ay+dy*inset);path.lineTo(bx-dx*inset,by-dy*inset);
        }
        paint.setShader(null);paint.setStyle(Paint.Style.STROKE);paint.setStrokeJoin(Paint.Join.ROUND);paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeWidth(16);paint.setColor(0x55404c64);canvas.drawPath(path,paint);
        PathMeasure measure=new PathMeasure(path,false);float middle=measure.getLength()/2;Path half=new Path();
        paint.setStrokeWidth(12);paint.setStrokeCap(Paint.Cap.BUTT);
        measure.getSegment(0,middle,half,true);paint.setColor((game.visible(a.cargo)?CARD_COLORS[a.cargo.color]:0xff646779));canvas.drawPath(half,paint);
        half.reset();measure.getSegment(middle,measure.getLength(),half,true);paint.setColor((game.visible(b.cargo)?CARD_COLORS[b.cargo.color]:0xff646779));canvas.drawPath(half,paint);
        float[] start=new float[2],end=new float[2];measure.getPosTan(0,start,null);measure.getPosTan(measure.getLength(),end,null);
        paint.setStyle(Paint.Style.FILL);paint.setColor((game.visible(a.cargo)?CARD_COLORS[a.cargo.color]:0xff646779));canvas.drawCircle(start[0],start[1],6,paint);
        paint.setColor((game.visible(b.cargo)?CARD_COLORS[b.cargo.color]:0xff646779));canvas.drawCircle(end[0],end[1],6,paint);
        paint.setStyle(Paint.Style.STROKE);paint.setStrokeCap(Paint.Cap.ROUND);paint.setStrokeWidth(2);paint.setColor(0x66ffffff);canvas.drawPath(path,paint);
        paint.setStyle(Paint.Style.FILL);paint.setStrokeCap(Paint.Cap.BUTT);
    }
    private void drawControls(Canvas c, float width, float height) {
        float scale = width / GameLayout.WIDTH, logicalHeight = height / scale;
        c.save(); c.scale(scale, scale); long now = SystemClock.elapsedRealtime();
        List<LinkNode> queuedLinks=new ArrayList<>(),activeLinks=new ArrayList<>();
        List<List<NativeGame.Capsule>> lanes = new ArrayList<>(); int max = 0;
        for (int lane = 0; lane < 4; lane++) {
            List<NativeGame.Capsule> row = new ArrayList<>();
            for (NativeGame.Capsule a : game.queue) if (!a.taken && a.lane == lane) row.add(a);
            row.sort(Comparator.comparingInt(a -> a.depth)); lanes.add(row); max = Math.max(max, row.size());
        }
        int rows=Math.min(max,GameLayout.rowCapacity(logicalHeight));
        int[] counts=new int[4];for(int i=0;i<4;i++)counts[i]=lanes.get(i).size();
        int[] live=GameLayout.liveLanes(counts);
        page = 0;
        for(int row=0;row<rows;row++){
            for(int column=0;column<live.length;column++){
                List<NativeGame.Capsule> lane=lanes.get(live[column]);if(page+row>=lane.size())continue;
                NativeGame.Capsule a=lane.get(page+row);
                float x=GameLayout.columnX(column,live.length),y=GameLayout.QUEUE_Y+row*GameLayout.ROW_STEP;
                Motion m=position(a.id,x,y,now);card(c,a,m.x,m.y,false,game.unlocked(a),touched("capsule",a.id));queuedLinks.add(new LinkNode(a,m.x,m.y));
                if(game.unlocked(a)&&game.risky(a)){paint.setStyle(Paint.Style.STROKE);paint.setColor(0xffd86c52);paint.setStrokeWidth(2);c.drawRoundRect(m.x-2,m.y-2,m.x+50,m.y+50,12,12,paint);paint.setStyle(Paint.Style.FILL);}
                hit((m.x-4)*scale,(m.y-3)*scale,(GameLayout.CARD+8)*scale,GameLayout.ROW_STEP*scale,"capsule",a.id);
            }
        }
        // Draw flying containers after the queue so their move into the work area is readable.
        for (int slot = 0; slot < game.slots.size(); slot++) {
            float slotX=GameLayout.columnX(slot,game.slots.size());
            box(c,slotX-1,GameLayout.ACTIVE_Y+4,50,54,0xffc8b294,11);box(c,slotX,GameLayout.ACTIVE_Y,48,48,0xfffff3df,10);
            NativeGame.Capsule a = game.slots.get(slot); if (a == null) continue;
            Motion m = launch(a, GameLayout.columnX(slot, game.slots.size()), now);
            if(!m.rocket||m.progress>=1){card(c, a, m.x, m.y, true, true, false);activeLinks.add(new LinkNode(a,m.x,m.y));}
        }
        // Keep the flight visible even if 10x simulation retires its cargo before landing.
        for(Motion m:motions.values())if(m.rocket){m.advance(now);rocket(c,m,now);if(m.progress<1)activeLinks.add(new LinkNode(m.cargo,m.x,m.y));}
        links(c,queuedLinks);links(c,activeLinks);
        // Numbers stay above the pipes, including moving linked containers.
        for(LinkNode n:queuedLinks)number(c,game.visible(n.cargo)?String.valueOf(n.cargo.remaining):"?",n.x+24,n.y+32,48,game.unlocked(n.cargo));
        for(LinkNode n:activeLinks)number(c,String.valueOf(Math.max(0,n.cargo.count-n.cargo.loaded)),n.x+24,n.y+32,48,true);
        if (pullMode) label(c, "꺼낼 컨테이너를 선택하세요", 192, 65, 9, INK);
        if (clearMode) label(c,"색 선택 · 드래그로 이동",192,65,9,INK);
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
        if (t.key.equals("capsule")) { NativeGame.Capsule a = game.capsule(t.id); return (game.visible(a)?NativeGame.COLOR_NAMES[a.color]+" 컨테이너, "+a.remaining+"개":"미공개 컨테이너")+", "+(game.keyLocked(a)?"열쇠 필요":game.unlocked(a)?"선택 가능":"뒤쪽"); }
        if (t.key.equals("booster")) return new String[]{"작업 공간 추가", "컬러 제거", "컨테이너 섞기", "뒤쪽 컨테이너 꺼내기"}[t.id] + ", " + save.boosterCount(t.id);
        return t.key.equals("pause") ? "일시정지" : "배속 " + (game.finishing?NativeGame.FINISH_SPEED:speed);
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
            if (!header && Math.abs(y - downY) > 30) { invalidate(); return true; }
            if (selected != null && selected.rect.contains(x, y)) { performClick(); actions.action(selected.key, selected.id); }
            invalidate(); return true;
        }
        if (event.getAction() == MotionEvent.ACTION_CANCEL) { pressed = null; invalidate(); }
        return true;
    }
    @Override public boolean performClick() { super.performClick(); return true; }
}
