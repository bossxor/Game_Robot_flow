package com.bossxor.robotflow;

import java.util.*;

/** The Android game's rule engine. No WebView or JavaScript runtime. */
public final class NativeGame {
    public static final int FINISH_SPEED = 10;
    public static final int PLAYING = 0, WON = 1, LOST = 2;
    public static final String[] COLOR_KEYS = {"red", "blue", "yellow", "green", "purple", "orange", "pink"};
    public static final String[] COLOR_NAMES = {"레드", "블루", "옐로", "그린", "퍼플", "오렌지", "핑크"};
    public static final int[] COLORS = {0xffff3158, 0xff3655f5, 0xffffd52e, 0xff38d447, 0xffb63cff, 0xffff9425, 0xffff59c7};
    public static class Level {
        public int id, width, height, difficulty, theme, queueVersion, slotCount = 5, revealDepth = 100, reward = 100;
        public boolean autoFinish;
        public int megaRequired = 100, megaCapacity = 8;
        public double powerChance = 0;
        public String name = "로봇 공장";
        public final List<Block> blocks = new ArrayList<>();
        public final List<Capsule> capsules = new ArrayList<>();
        public final List<Seal> seals = new ArrayList<>();
        public final List<BoardKey> keys = new ArrayList<>();
    }
    public static class Seal {
        public int x,y,width,height,color,count;
        public boolean open;
        public Seal(int x,int y,int width,int height,int color,int count){this.x=x;this.y=y;this.width=width;this.height=height;this.color=color;this.count=count;}
        boolean contains(Block b){return b.x>=x&&b.x<x+width&&b.y>=y&&b.y<y+height;}
    }
    public static class BoardKey {
        public int id,x,y,lockColor;
        public boolean owned;
        public BoardKey(int id,int x,int y,int lockColor){this.id=id;this.x=x;this.y=y;this.lockColor=lockColor;}
    }
    public static class Block {
        public int id, x, y, color;
        public boolean removed, reserved, reachable;
        public double removedAt = -1;
        public Block(int x, int y, int color) { this.x = x; this.y = y; this.color = color; }
    }
    public static class Capsule {
        public int id, color, count, remaining, depth, lane, loaded, group, lockKey;
        public double selectedAt, lastSpawnAt = -100;
        public int[] blockedBy = new int[0];
        public boolean taken, retired, mega, mystery;
        public double powerChance = -1;
        public Capsule(int id, int color, int count, int depth) { this.id = id; this.color = color; this.count = this.remaining = count; this.depth = depth; this.lane = (id-1)%4; }
    }
    public static class Point {
        public float x, y;
        public int collect = -1;
        public Point(float x, float y) { this.x = x; this.y = y; }
        public Point(float x, float y, int collect) { this(x, y); this.collect = collect; }
    }
    public static class Bot {
        public int id, capsuleId, color, kind, step, collected, phase;
        public float x, y, angle, distance, velocity;
        public double phaseTime;
        public final List<Point> route = new ArrayList<>();
        public final List<Integer> targets = new ArrayList<>();
    }
    public interface Listener { void event(String type, int color, float x, float y); }
    public final Level level;
    public final List<Block> blocks = new ArrayList<>();
    public final List<Capsule> queue = new ArrayList<>();
    public final List<Bot> bots = new ArrayList<>();
    public final List<Capsule> slots = new ArrayList<>();
    public final List<Seal> seals = new ArrayList<>();
    public final List<BoardKey> keys = new ArrayList<>();
    public final int[] colorCollected = new int[7];
    public final Random random;
    public Listener listener;
    public int status = PLAYING, gauge, collected, moves, nextBotId;
    public boolean paused, colorClearUsed, finishing;
    private int spawnSlot;
    public static final double SPAWN_INTERVAL = .52;
    public double time, spawnClock, megaAt = -100;
    private final boolean[] completedColors = new boolean[7];
    public int truckState, truckCargo, delivered, departures, clearedByItem;
    public double truckClock;
    public final List<Integer> cargoColors = new ArrayList<>();
    public static final int TRUCK_CAPACITY = 20;
    private final int width, height, gridWidth, gridHeight;
    private final Block[][] cells;
    public float portalSpan;

    public NativeGame(Level level) { this(level, new Random()); }
    public NativeGame(Level level, Random random) {
        QueuePlanner.prepare(level);
        this.level = level; this.random = random;
        width = level.width; height = level.height; gridWidth = width + 2; gridHeight = height + 7;
        portalSpan=width/.762f;
        cells = new Block[height][width];
        for (Block original : level.blocks) {
            Block b = new Block(original.x, original.y, original.color); b.id = blocks.size();
            blocks.add(b); cells[b.y][b.x] = b;
        }
        for(Seal s:level.seals)seals.add(new Seal(s.x,s.y,s.width,s.height,s.color,s.count));
        for(BoardKey k:level.keys)keys.add(new BoardKey(k.id,k.x,k.y,k.lockColor));
        for (Capsule original : level.capsules) {
            Capsule c = new Capsule(original.id, original.color, original.count, original.depth);
            c.blockedBy = original.blockedBy.clone(); c.lane = original.lane; c.powerChance = original.powerChance; c.group=original.group;
            c.mystery=level.id>=13&&c.depth>=3&&c.id%7==0;
            for(BoardKey k:keys)if(c.color==k.lockColor)c.lockKey=k.id;
            queue.add(c);
        }
        for (int i = 0; i < level.slotCount; i++) slots.add(null);
        refreshReachable();
    }
    public int remainingBlocks() { return blocks.size() - collected; }
    private void event(String type, int color, float x, float y) { if (listener != null) listener.event(type, color, x, y); }
    public Capsule capsule(int id) { for (Capsule c : queue) if (c.id == id) return c; return null; }
    public boolean unlocked(Capsule c) {
        if (c.taken || keyLocked(c)) return false;
        for (Capsule q : queue) if (!q.taken && q.lane == c.lane && q.depth < c.depth) return false;
        for (int parent : c.blockedBy) { Capsule p = capsule(parent); if (p == null || !p.taken) return false; }
        return true;
    }
    public boolean visible(Capsule c) {
        if(c.taken)return true;
        if(c.mystery){int colors=0;for(boolean done:completedColors)if(done)colors++;if(colors<2&&!unlocked(c))return false;}
        int min = Integer.MAX_VALUE;
        for (Capsule q : queue) if (!q.taken) min = Math.min(min, q.depth);
        return unlocked(c) || c.depth - min <= level.revealDepth;
    }
    public boolean keyLocked(Capsule c){if(c==null)return false;for(BoardKey k:keys)if(k.id==c.lockKey&&!k.owned)return true;return false;}
    public boolean sealed(Block b){for(Seal s:seals)if(!s.open&&s.contains(b))return true;return false;}
    public Point origin(int slot) { return new Point((width-1)/2f+(slot-(slots.size()-1)/2f)*60/384*portalSpan, height + 4.4f); }
    public List<Block> available(int color) {
        List<Block> found = new ArrayList<>();
        for (Block b : blocks) if (!b.removed && !b.reserved && b.reachable && !sealed(b) && (color < 0 || color == b.color)) found.add(b);
        return found;
    }
    public Point truckPoint() { return new Point(width/2f, height+1.8f); }
    private int key(int x, int y) { return (y + 1) * gridWidth + x + 1; }
    private int gx(int key) { return key % gridWidth - 1; }
    private int gy(int key) { return key / gridWidth - 1; }
    private boolean walkable(int x, int y) { return x < 0 || y < 0 || x >= width || y >= height || cells[y][x] == null || cells[y][x].removed; }
    private static class Flood { int[] parent, distance; }
    private Flood flood(int x, int y) {
        Flood f = new Flood(); int total = gridWidth * gridHeight;
        f.parent = new int[total]; f.distance = new int[total]; Arrays.fill(f.parent, -2); Arrays.fill(f.distance, -1);
        int[] pending = new int[total]; int first = 0, last = 1, start = key(x, y);
        pending[0] = start; f.parent[start] = -1; f.distance[start] = 0;
        while (first < last) {
            int p = pending[first++], px = gx(p), py = gy(p);
            for (int direction = 0; direction < 4; direction++) {
                int nx = px + (direction == 0 ? 1 : direction == 1 ? -1 : 0);
                int ny = py + (direction == 2 ? 1 : direction == 3 ? -1 : 0);
                if (nx < -1 || nx > width || ny < -1 || ny > height + 5 || !walkable(nx, ny)) continue;
                int k = key(nx, ny); if (f.parent[k] != -2) continue;
                f.parent[k] = p; f.distance[k] = f.distance[p] + 1; pending[last++] = k;
            }
        }
        return f;
    }
    private int[] neighbors(Block b) { return new int[]{key(b.x - 1, b.y), key(b.x + 1, b.y), key(b.x, b.y - 1), key(b.x, b.y + 1)}; }
    public void refreshReachable() {
        Flood f = flood(-1, height + 1);
        for (Block b : blocks) { b.reachable = false; if (!b.removed) for (int k : neighbors(b)) if (f.distance[k] >= 0) { b.reachable = true; break; } }
    }
    private void addGauge(int amount) { gauge = Math.min(level.megaRequired, gauge + amount); }
    public List<Capsule> selectionGroup(Capsule selected) {
        List<Capsule> result=new ArrayList<>();if(selected==null)return result;
        for(Capsule c:queue)if(!c.taken&&(c==selected||(selected.group>0&&c.group==selected.group)))result.add(c);
        return result;
    }
    public boolean risky(Capsule selected){
        List<Capsule> group=selectionGroup(selected);int free=0;for(Capsule c:slots)if(c==null)free++;
        if(group.size()!=free)return false;for(Capsule c:group)if(!available(c.color).isEmpty())return false;return true;
    }
    public int visibleTrucks(){return Math.min(3,Math.max(truckState==1?1:0,(blocks.size()-delivered-clearedByItem+truckCargo+TRUCK_CAPACITY-1)/TRUCK_CAPACITY));}
    public synchronized boolean select(int id) {
        Capsule selected=capsule(id);List<Capsule> group=selectionGroup(selected);int free=0;for(Capsule c:slots)if(c==null)free++;
        if(status!=PLAYING||paused||group.isEmpty()||free<group.size())return false;
        for(Capsule c:group)if(!unlocked(c))return false;
        for(Capsule c:group){int slot=slots.indexOf(null);c.taken=true;c.selectedAt=time;c.mega=false;slots.set(slot,c);event("select",c.color,origin(slot).x,origin(slot).y);}
        moves++;return true;
    }
    private List<Point> path(Flood f, int end) {
        List<Point> result = new ArrayList<>();
        for (int cursor = end; cursor >= 0; cursor = f.parent[cursor]) result.add(new Point(gx(cursor), gy(cursor)));
        Collections.reverse(result); return result;
    }
    private int workers(int id) { int n = 0; for (Bot b : bots) if (b.capsuleId == id) n++; return n; }
    private void spawn(Capsule c, int slot) {
        // Keep the physical walking speed and worker density stable on finer picture grids.
        if (c.remaining <= 0 || workers(c.id) >= 4 || time-c.selectedAt<.65) return;
        List<Block> targets = available(c.color); if (targets.isEmpty()) return;
        Point origin = origin(slot); int start = key(Math.max(-1, Math.min(width, Math.round(origin.x))), height + 4);
        Flood f = flood(gx(start), gy(start));
        Bot bot = new Bot(); bot.id = nextBotId++; bot.capsuleId = c.id; bot.color = c.color; bot.x = origin.x; bot.y = origin.y;
        bot.kind = 0;
        int capacity = Math.min(c.remaining, 1);
        bot.route.add(new Point(gx(start), gy(start)));
        for (int n = 0; n < capacity && !targets.isEmpty(); n++) {
            Block best = null; int approach = -1, distance = Integer.MAX_VALUE, ties = 0;
            for (Block b : targets) for (int k : neighbors(b)) {
                int d = f.distance[k]; if (d < 0) continue;
                if (d < distance) { best = b; approach = k; distance = d; ties = 1; }
                else if (d == distance && random.nextInt(++ties) == 0) { best = b; approach = k; }
            }
            if (best == null) break;
            List<Point> segment = path(f, approach); bot.route.addAll(segment.subList(1, segment.size()));
            bot.route.add(new Point(best.x, best.y, best.id)); bot.route.add(new Point(gx(approach), gy(approach)));
            best.reserved = true; bot.targets.add(best.id); targets.remove(best);
            f = flood(gx(approach), gy(approach));
        }
        if (bot.targets.isEmpty()) return;
        int dock = key(width/2, height+1);
        List<Point> segment = path(f, dock); bot.route.addAll(segment.subList(1, segment.size())); bot.route.add(truckPoint());
        c.remaining -= bot.targets.size(); c.mega = false; c.lastSpawnAt=time; bots.add(bot);
        if (bot.kind == 1) megaAt = time;
        event(bot.kind == 1 ? "mega" : "mini", c.color, bot.x, bot.y);
    }
    public void remove(int id) {
        Block b = blocks.get(id); if (b.removed || sealed(b)) return;
        b.removed = true; b.reserved = false; b.removedAt = time; collected++;
        colorCollected[b.color]++;
        for(Seal s:seals)if(!s.open&&colorCollected[s.color]>=s.count){s.open=true;event("reveal",s.color,s.x,s.y);}
        for(BoardKey k:keys)if(!k.owned&&k.x==b.x&&k.y==b.y){k.owned=true;event("key",k.lockColor,k.x,k.y);}
        event("collect", b.color, b.x, b.y);
        boolean any = false; for (Block other : blocks) if (!other.removed && other.color == b.color) { any = true; break; }
        if (!any) {
            completedColors[b.color] = true;
            for (Capsule c : queue) if (!c.taken && c.color == b.color) { c.taken = true; c.retired = true; }
        }
        refreshReachable();
    }
    private void departTruck() { if (truckState == 0 && truckCargo > 0) { truckState = 1; truckClock = 0; event("truck",-1,0,0); } }
    public synchronized void update(double delta) {
        if (paused || status != PLAYING) return;
        double dt = Math.max(0, Math.min(.1, delta)); time += dt;
        if (truckState != 0) {
            truckClock += dt;
            if (truckClock >= 1.4) {
                if (truckState == 1) { truckCargo=0; cargoColors.clear(); departures++; truckState=2; }
                else truckState=0;
                truckClock=0;
            }
        }
        for (Bot bot : bots) {
            if(bot.phase==3){bot.phaseTime+=dt;if(bot.phaseTime>=.30)bot.phase=4;continue;}
            if (bot.phase == 1) {
                bot.phaseTime += dt;
                if (bot.phaseTime >= .22) { Point p=bot.route.get(bot.step); if(!blocks.get(p.collect).removed){remove(p.collect);bot.collected++;} bot.step++;bot.phase=0;bot.phaseTime=0; }
                continue;
            }
            if (bot.step >= bot.route.size()) {
                bot.phase=2; bot.velocity=0;
                if (bot.collected==0) { bot.phase=4; continue; }
                if (truckState==0 && truckCargo+bot.collected<=TRUCK_CAPACITY) {
                    bot.phaseTime+=dt;
                    if(bot.phaseTime>=.28){
                        truckCargo+=bot.collected;delivered+=bot.collected;Capsule c=capsule(bot.capsuleId);if(c!=null)c.loaded+=bot.collected;
                        for(int n=0;n<bot.collected;n++)cargoColors.add(bot.color);addGauge(bot.collected*2);bot.phase=3;bot.phaseTime=0;
                        if(truckCargo==TRUCK_CAPACITY)departTruck();
                    }
                } else if (truckState==0) departTruck();
                continue;
            }
            float densityScale=Math.max(1,width/32f);
            bot.velocity=Math.min((bot.kind==1?3.8f:5.2f)*densityScale,bot.velocity+(float)dt*12*densityScale);
            float travel=(float)dt*bot.velocity;
            while(travel>0 && bot.step<bot.route.size()){
                Point p=bot.route.get(bot.step);float dx=p.x-bot.x,dy=p.y-bot.y,d=(float)Math.hypot(dx,dy);
                if(d>.01f){float desired=(float)Math.toDegrees(Math.atan2(dx,dy));float turn=(desired-bot.angle+540)%360-180;bot.angle+=turn*Math.min(1,(float)dt*12);}
                float moved=Math.min(d,travel);bot.distance+=moved;
                if(d<=travel){bot.x=p.x;bot.y=p.y;travel-=d;if(p.collect>=0){bot.phase=1;bot.phaseTime=0;bot.velocity=0;break;}bot.step++;}
                else{bot.x+=dx/d*travel;bot.y+=dy/d*travel;travel=0;}
            }
        }
        bots.removeIf(b->b.phase==4);
        for(int i=0;i<slots.size();i++){
            Capsule c=slots.get(i);
            if(c!=null&&workers(c.id)==0&&(c.remaining==0||completedColors[c.color]||remainingBlocks()==0)){slots.set(i,null);addGauge(8);event("complete",c.color,0,0);}
        }
        spawnClock+=dt;
        if(level.autoFinish&&!finishing){int waiting=0,free=0;for(Capsule c:queue)if(!c.taken)waiting++;for(Capsule c:slots)if(c==null)free++;
            if(waiting<=5&&waiting<=free){boolean progress=true;while(progress){progress=false;for(Capsule c:queue)if(!c.taken&&unlocked(c)&&select(c.id)){progress=true;break;}}boolean all=true;for(Capsule c:queue)if(!c.taken)all=false;if(all){finishing=true;event("finishRush",-1,0,0);}}
        }
        if(spawnClock>=SPAWN_INTERVAL){spawnClock=0;for(int n=0;n<slots.size();n++){int i=spawnSlot++%slots.size();Capsule c=slots.get(i);if(c!=null){int before=bots.size();spawn(c,i);if(bots.size()>before)break;}}}
        if(remainingBlocks()==0&&bots.isEmpty()){
            if(truckState==0&&truckCargo>0)departTruck();
            if(truckCargo==0&&truckState!=1){status=WON;event("won",-1,0,0);}
        }else if(bots.isEmpty()){
            boolean canWork=false,canSelect=false;
            for(Capsule c:slots)if(c!=null&&c.remaining>0&&!available(c.color).isEmpty())canWork=true;
            if(slots.contains(null))for(Capsule c:queue)if(unlocked(c))canSelect=true;
            if(!canWork&&!canSelect){status=LOST;event("lost",-1,0,0);}
        }
    }
    public synchronized boolean extraSlot() {
        if (paused || status == WON || slots.size() >= 6) return false;
        slots.add(null);
        status = PLAYING; return true;
    }
    public synchronized boolean clearColor(int color) {
        return clearColor(color, false);
    }
    public synchronized boolean clearColor(int color, boolean unlimited) {
        if (paused || status == WON || (colorClearUsed && !unlimited) || color < 0 || color >= COLORS.length) return false;
        boolean any = false; for (Block b : blocks) if (!b.removed && !sealed(b) && b.color == color) { remove(b.id);clearedByItem++; any = true; }
        if (!any) return false; colorClearUsed = true; status = PLAYING; return true;
    }
    public synchronized boolean shuffle() {
        if (paused || status == WON) return false;
        List<Capsule> waiting = new ArrayList<>(); for (Capsule c : queue) if (!c.taken) waiting.add(c);
        if (waiting.size() < 2) return false;
        for (int i = waiting.size() - 1; i > 0; i--) {
            Capsule a = waiting.get(i), b = waiting.get(random.nextInt(i + 1));
            int color = a.color, count = a.count, remaining = a.remaining, lockKey=a.lockKey; double chance = a.powerChance;
            a.color = b.color; a.count = b.count; a.remaining = b.remaining; a.powerChance = b.powerChance; a.lockKey=b.lockKey;
            b.color = color; b.count = count; b.remaining = remaining; b.powerChance = chance; b.lockKey=lockKey;
        }
        status = PLAYING; return true;
    }
    public synchronized boolean pullOut(int id) {
        Capsule c = capsule(id); if (paused || status == WON || c == null || c.taken || unlocked(c)) return false;
        for(Capsule member:selectionGroup(c))if(keyLocked(member))return false;
        for(Capsule member:selectionGroup(c)){int front=member.depth;for(Capsule q:queue)if(!q.taken&&q.lane==member.lane)front=Math.min(front,q.depth);member.depth=front-1;member.blockedBy=new int[0];} status = PLAYING; return true;
    }
}
