package com.bossxor.robotflow;

import java.util.*;

/** Same inventory and picture, but sealed colours compete for five work slots. */
public final class QueuePlanner {
    private QueuePlanner() {}
    public static void prepare(NativeGame.Level level) {
        if (level.width <= 16 || level.queueVersion == 1) return;
        int w = level.width, h = level.height;
        boolean[] occupied = new boolean[w*h], outside = new boolean[w*h];
        for (NativeGame.Block b : level.blocks) occupied[b.y*w+b.x] = true;
        ArrayDeque<Integer> pending = new ArrayDeque<>();
        for (int y=0;y<h;y++) for (int x=0;x<w;x++) {
            int key=y*w+x;
            if (!occupied[key] && (x==0 || y==0 || x==w-1 || y==h-1)) {
                outside[key]=true; pending.add(key);
            }
        }
        while (!pending.isEmpty()) {
            int key=pending.remove(), x=key%w, y=key/w;
            for (int[] p : new int[][]{{x-1,y},{x+1,y},{x,y-1},{x,y+1}}) {
                int a=p[0], b=p[1];
                if (a>=0 && b>=0 && a<w && b<h && !occupied[b*w+a] && !outside[b*w+a]) {
                    outside[b*w+a]=true; pending.add(b*w+a);
                }
            }
        }
        boolean[] exposed = new boolean[7];
        for (NativeGame.Block b : level.blocks) {
            int x=b.x,y=b.y;
            if (x==0 || y==0 || x==w-1 || y==h-1 || outside[y*w+x-1] || outside[y*w+x+1] || outside[(y-1)*w+x] || outside[(y+1)*w+x]) exposed[b.color]=true;
        }
        int target=level.id==1?2:new int[]{5,7,9}[level.difficulty];
        List<NativeGame.Capsule> bait=new ArrayList<>(), safe=new ArrayList<>();
        for (NativeGame.Capsule original : level.capsules) {
            int count=original.count;
            while (!exposed[original.color] && count>0 && bait.size()<target) {
                int amount=Math.min(count,8);
                bait.add(new NativeGame.Capsule(0,original.color,amount,0)); count-=amount;
            }
            if (count>0) {
                NativeGame.Capsule c=new NativeGame.Capsule(0,original.color,count,0);
                c.powerChance=original.powerChance; safe.add(c);
            }
        }
        List<List<NativeGame.Capsule>> lanes=new ArrayList<>();
        for (int i=0;i<4;i++) lanes.add(new ArrayList<>());
        int trapLane=(level.id+level.id/10)%4;
        List<Integer> workLanes=new ArrayList<>();
        for(int i=0;i<4;i++)if(i!=trapLane)workLanes.add(i);
        lanes.set(trapLane,bait);int workIndex=0;
        for(int i=0;i<safe.size();i++) {
            int lane=i>=12&&(i+level.id)%8==0?trapLane:workLanes.get(workIndex++%3);
            lanes.get(lane).add(safe.get(i));
        }
        int[] previous=new int[4]; int max=0,id=1;
        for (List<NativeGame.Capsule> lane:lanes) max=Math.max(max,lane.size());
        level.capsules.clear();
        for (int depth=0;depth<max;depth++) {
            List<NativeGame.Capsule> row=new ArrayList<>();
            for (int lane=0;lane<4;lane++) {
                if (depth>=lanes.get(lane).size()) continue;
                NativeGame.Capsule c=lanes.get(lane).get(depth);
                c.id=id++;c.lane=lane;c.depth=depth;
                c.blockedBy=previous[lane]==0?new int[0]:new int[]{previous[lane]};previous[lane]=c.id;
                level.capsules.add(c);if(lane!=trapLane)row.add(c);
            }
            if(level.difficulty>0) for(NativeGame.Capsule c:row) {
                if(c.group!=0)continue;
                List<NativeGame.Capsule> members=new ArrayList<>();
                for(NativeGame.Capsule p:row)if(p.color==c.color&&p.group==0&&members.size()<level.difficulty+1)members.add(p);
                if(members.size()>1)for(NativeGame.Capsule p:members)p.group=c.id;
            }
        }
        level.queueVersion=1;
    }
}
