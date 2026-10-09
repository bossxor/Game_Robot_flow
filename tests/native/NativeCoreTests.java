package com.bossxor.robotflow;

import java.nio.file.*;
import java.util.*;

/** Executes the exact Java engine shipped in the APK, without Android stubs. */
public final class NativeCoreTests {
    static int assertions;
    static void check(boolean condition,String message){assertions++;if(!condition)throw new AssertionError(message);}
    static NativeGame.Level simple(){
        NativeGame.Level l=new NativeGame.Level();l.id=1;l.width=l.height=5;l.powerChance=0;
        return l;
    }
    static void rules(){
        SecretTapSequence secret=new SecretTapSequence();
        for(int i=0;i<6;i++)check(!secret.tap(i*200),"six secret taps do not unlock");
        check(secret.tap(1200),"seventh secret tap unlocks");
        for(int i=0;i<6;i++)check(!secret.tap(1400+i*200),"toggle sequence restarts");
        check(secret.tap(2600),"same seven taps toggle again");
        secret=new SecretTapSequence();secret.tap(0);check(!secret.tap(1500),"long gap resets secret taps");
        for(int i=1;i<6;i++)check(!secret.tap(1500+i*100),"only taps after gap count");
        check(secret.tap(2100),"complete fresh sequence after gap");
        NativeGame.Level l=simple();
        for(int i=0;i<25;i++)l.blocks.add(new NativeGame.Block(i%5,i/5,i==12?1:0));
        l.capsules.add(new NativeGame.Capsule(1,0,24,0));l.capsules.add(new NativeGame.Capsule(2,1,1,0));
        NativeGame g=new NativeGame(l,new Random(1));check(g.available(0).size()==16,"all four sides reachable");check(g.available(1).isEmpty(),"sealed inner color is blocked");
        g.remove(22);g.remove(17);check(g.available(1).size()==1,"a removed passage exposes the center");
        NativeGame.Level concurrent=simple();concurrent.blocks.add(new NativeGame.Block(0,4,0));concurrent.blocks.add(new NativeGame.Block(4,4,1));
        concurrent.capsules.add(new NativeGame.Capsule(1,0,1,0));concurrent.capsules.add(new NativeGame.Capsule(2,1,1,0));
        g=new NativeGame(concurrent,new Random(2));check(g.select(1)&&g.select(2),"concurrent selection");for(int n=0;n<200&&g.bots.isEmpty();n++)g.update(.01);check(g.bots.get(0).phase==0&&g.bots.get(0).x==g.origin(0).x,"robot appears directly in front of first container");for(int n=0;n<200&&g.bots.size()<2;n++)g.update(.01);
        check(g.bots.size()==2,"two independently dispatched robots");check(g.bots.get(1).x==g.origin(1).x&&g.bots.get(1).phase==0,"separate container origins");
        HashSet<Integer> targets=new HashSet<>();for(NativeGame.Bot b:g.bots)for(int id:b.targets)check(targets.add(id),"unique target reservation");
        NativeGame.Bot walker=g.bots.get(1);float oldX=walker.x,oldY=walker.y;g.update(.1);
        double distance=Math.hypot(walker.x-oldX,walker.y-oldY);
        check(distance>.05&&distance<.25,"1x walking accelerates gently");
        g.paused=true;double before=g.time;g.update(.1);check(g.time==before,"pause freezes simulation");g.paused=false;
        for(int n=0;n<1000&&g.status==0;n++)g.update(.1);check(g.status==NativeGame.WON&&g.bots.isEmpty(),"victory waits for returns");
        NativeGame.Level deadlock=simple();deadlock.slotCount=1;deadlock.blocks.add(new NativeGame.Block(0,0,0));
        deadlock.capsules.add(new NativeGame.Capsule(1,1,1,0));deadlock.capsules.add(new NativeGame.Capsule(2,0,1,0));
        g=new NativeGame(deadlock);g.select(1);g.update(.1);check(g.status==NativeGame.LOST,"idle full slots lose");check(g.extraSlot()&&g.status==0,"extra slot rescues deadlock");check(g.select(2),"rescue opens a new choice");
        NativeGame.Level special=simple();for(int i=0;i<10;i++)special.blocks.add(new NativeGame.Block(i%5,i/5,0));special.capsules.add(new NativeGame.Capsule(1,0,10,0));special.powerChance=1;
        g=new NativeGame(special);g.select(1);for(int n=0;n<200&&g.bots.isEmpty();n++)g.update(.01);check(g.bots.get(0).kind==0&&g.bots.get(0).targets.size()==1,"legacy power probability never creates a third tier");
        g=new NativeGame(special);g.gauge=100;g.select(1);for(int n=0;n<200&&g.bots.isEmpty();n++)g.update(.01);check(g.bots.get(0).kind==1&&g.bots.get(0).targets.size()==8,"mega robot handles max eight");
        check(g.clearColor(0),"clear reserved color");check(!g.clearColor(0),"color clear once only");for(int n=0;n<1000&&g.status==0;n++)g.update(.1);check(g.collected==10&&g.status==1,"reserved blocks never double count");
        NativeGame.Level multi=simple();multi.blocks.add(new NativeGame.Block(0,0,0));multi.blocks.add(new NativeGame.Block(1,0,1));multi.blocks.add(new NativeGame.Block(2,0,2));
        g=new NativeGame(multi);check(g.clearColor(0),"normal clear allowed");check(!g.clearColor(1),"normal repeated clear blocked");check(g.clearColor(1,true),"unlimited repeated clear allowed");check(!g.clearColor(2),"turning off restores normal per-stage rule");check(g.clearColor(2,true),"unlimited third clear allowed");
        NativeGame.Level overlap=simple();overlap.blocks.add(new NativeGame.Block(0,0,0));overlap.capsules.add(new NativeGame.Capsule(1,0,1,0));NativeGame.Capsule rear=new NativeGame.Capsule(2,1,1,1);rear.lane=0;rear.blockedBy=new int[]{1};overlap.capsules.add(rear);
        g=new NativeGame(overlap);check(!g.select(2),"rear containers are locked");check(g.pullOut(2)&&g.select(2),"targeted pull-out unlocks chosen rear capsule");

        check(NativeGame.COLORS.length==7,"seven team colors");
        float[] walks=new float[2];int[] sizes={32,48};
        for(int i=0;i<sizes.length;i++){NativeGame.Level dense=simple();dense.width=dense.height=sizes[i];dense.blocks.add(new NativeGame.Block(0,sizes[i]-1,0));dense.capsules.add(new NativeGame.Capsule(1,0,1,0));NativeGame walk=new NativeGame(dense);walk.select(1);for(int n=0;n<200&&walk.bots.isEmpty();n++)walk.update(.01);NativeGame.Bot moving=walk.bots.get(0);float distanceBefore=moving.distance;walk.update(.1);walks[i]=moving.distance-distanceBefore;}
        check(Math.abs(walks[1]/48-walks[0]/32)<.00001,"fine grids preserve normalized walking speed");
        NativeGame.Level trucks=simple();for(int i=0;i<25;i++)trucks.blocks.add(new NativeGame.Block(i%5,i/5,0));trucks.capsules.add(new NativeGame.Capsule(1,0,25,0));g=new NativeGame(trucks);g.select(1);
        boolean independent=false,departed=false;for(int n=0;n<3000&&g.status==0;n++){g.update(.1);if(g.truckState==1){departed=true;if(g.slots.get(0)!=null)independent=true;}if(n==40){g.paused=true;double t=g.time,tc=g.truckClock;int cargo=g.truckCargo;g.update(.1);check(g.time==t&&g.truckClock==tc&&g.truckCargo==cargo,"pause freezes truck and robot simulation");g.paused=false;}}
        check(departed&&independent,"shared truck departs while a container still works");check(g.status==1&&g.delivered==25&&g.departures==2,"full and partial cargo leave once, with no lost blocks");
        NativeGame.Level lanes=simple();for(int i=0;i<7;i++)lanes.blocks.add(new NativeGame.Block(i%5,i/5,i));for(int i=0;i<8;i++)lanes.capsules.add(new NativeGame.Capsule(i+1,i%7,1,i/4));g=new NativeGame(lanes);check(!g.select(5),"only lane fronts select");check(g.select(1)&&g.unlocked(g.capsule(5)),"same lane advances immediately");check(!g.unlocked(g.capsule(6)),"other lane stays locked");check(g.clearColor(6),"seventh color works with items");
        NativeGame.Level grouped=simple();grouped.slotCount=1;for(int i=0;i<2;i++){grouped.blocks.add(new NativeGame.Block(i,0,0));NativeGame.Capsule c=new NativeGame.Capsule(i+1,0,1,0);c.group=7;grouped.capsules.add(c);}g=new NativeGame(grouped);check(!g.select(1)&&!g.capsule(1).taken&&!g.capsule(2).taken,"group selection is atomic when space is insufficient");g.extraSlot();check(g.select(2)&&g.slots.get(0)!=null&&g.slots.get(1)!=null,"linked pair enters together");g.update(.1);check(g.bots.isEmpty(),"container move completes before dispatch");for(int i=0;i<11;i++)g.update(.1);check(g.bots.size()==1,"one robot dispatches per global interval");
        NativeGame.Level finish=simple();finish.autoFinish=true;for(int i=0;i<3;i++){finish.blocks.add(new NativeGame.Block(i,0,0));finish.capsules.add(new NativeGame.Capsule(i+1,0,1,0));}g=new NativeGame(finish);g.update(.1);check(g.finishing&&g.queue.stream().allMatch(c->c.taken),"fewer than five remaining containers fill free slots automatically");
        NativeGame.Level truckQueue=simple();truckQueue.width=truckQueue.height=7;for(int i=0;i<41;i++)truckQueue.blocks.add(new NativeGame.Block(i%7,i/7,0));g=new NativeGame(truckQueue);check(g.visibleTrucks()==3,"at most three trucks for remaining cargo");g.clearColor(0);check(g.visibleTrucks()==0,"removed cargo needs no extra waiting trucks");
        NativeGame.Level linkedRear=simple();NativeGame.Capsule parent=new NativeGame.Capsule(3,1,1,0);linkedRear.capsules.add(parent);for(int i=0;i<2;i++){NativeGame.Capsule member=new NativeGame.Capsule(i+1,0,1,2);member.group=9;member.blockedBy=new int[]{3};linkedRear.capsules.add(member);}g=new NativeGame(linkedRear);check(g.pullOut(1)&&g.unlocked(g.capsule(1))&&g.unlocked(g.capsule(2)),"pull-out exposes both members of a linked group");
        System.out.println("Native rule checks passed.");
    }
    static List<NativeGame.Level> read(String path)throws Exception{
        List<NativeGame.Level> result=new ArrayList<>();NativeGame.Level l=null;
        for(String line:Files.readAllLines(Paths.get(path))){String[] p=line.split(" ");
            if(p[0].equals("L")){l=simple();l.id=Integer.parseInt(p[1]);l.width=Integer.parseInt(p[2]);l.height=Integer.parseInt(p[3]);l.difficulty=Integer.parseInt(p[4]);l.autoFinish=p[5].equals("1");result.add(l);}
            else if(p[0].equals("B"))l.blocks.add(new NativeGame.Block(Integer.parseInt(p[1]),Integer.parseInt(p[2]),Integer.parseInt(p[3])));
            else if(p[0].equals("C")){NativeGame.Capsule c=new NativeGame.Capsule(Integer.parseInt(p[1]),Integer.parseInt(p[2]),Integer.parseInt(p[3]),Integer.parseInt(p[4]));c.lane=Integer.parseInt(p[5]);if(p.length>6&&!p[6].equals("-"))c.blockedBy=Arrays.stream(p[6].split(",")).mapToInt(Integer::parseInt).toArray();c.group=Integer.parseInt(p[7]);l.capsules.add(c);}
        }return result;
    }
    public static void main(String[] args)throws Exception{
        rules();if(args[0].equals("--rules"))return;List<NativeGame.Level> levels=read(args[0]);check(levels.size()==500,"all 500 native levels loaded");
        ArrayList<String> failed=new ArrayList<>();
        for(double chance:new double[]{0,1}){
            int pass=0;
            for(NativeGame.Level l:levels){l.powerChance=chance;NativeGame g=new NativeGame(l,new Random(l.id*173L));
                for(int n=0;n<180000&&g.status==0;n++){
                    if(chance==0)g.gauge=0;
                    if(g.bots.isEmpty()&&g.slots.contains(null)){
                        NativeGame.Capsule next=null;
                        for(NativeGame.Capsule c:g.queue)if(g.unlocked(c)){if(next==null)next=c;if(!g.available(c.color).isEmpty()){next=c;break;}}
                        if(next!=null)g.select(next.id);
                    }g.update(.1);
                }
                if(g.status==1){pass++;check(g.remainingBlocks()==0&&g.bots.isEmpty()&&g.delivered==g.blocks.size()&&g.truckCargo==0,"fully recovered native level "+l.id);}
                else failed.add("level="+l.id+" chance="+chance+" status="+g.status+" remaining="+g.remainingBlocks());
            }System.out.println("Native levels, power chance "+chance+": "+pass+" / "+levels.size()+" cleared");
        }
        for(String failure:failed)System.out.println(failure);
        check(failed.isEmpty(),"Native engine level solutions failed: "+failed.size());
        System.out.println("Native tests passed: "+assertions+" assertions.");
    }
}
