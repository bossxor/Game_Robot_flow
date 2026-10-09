package com.bossxor.robotflow;
import android.content.Context;
import android.graphics.*;
import android.os.SystemClock;
import android.view.*;
import java.util.*;
/** Flat four-lane queue and independent active containers. */
public final class NativeHud extends View {
 public interface Actions {void action(String key,int value);}
 private final NativeGame game;private final PlayerSave save;private final Actions actions;private final boolean header;
 private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);private final float density;
 public boolean pullMode;public int page,speed=1;
 private static final int INK=0xff353b43,BACK=0xfff7eddf,MUTED=0xff8e877e;
 private final ArrayList<Target> targets=new ArrayList<>();private Target pressed;private float downY;
 private int accessibilityFocus=-1;
 private final Map<Integer,float[]> positions=new HashMap<>();private long lastFrame;
 private static class Target {RectF r;String key;int id;Target(RectF r,String k,int id){this.r=r;key=k;this.id=id;}}
 public NativeHud(Context c,NativeGame g,PlayerSave s,boolean h,Actions a){super(c);game=g;save=s;header=h;actions=a;density=getResources().getDisplayMetrics().density;setContentDescription(h?"스테이지, 일시정지, 배속":"4열 컨테이너 대기열과 4가지 아이템");}
 private void text(Canvas c,String t,float x,float y,float size,int color){p.setColor(color);p.setTextSize(size);p.setTypeface(Typeface.DEFAULT_BOLD);p.setTextAlign(Paint.Align.CENTER);c.drawText(t,x,y,p);}
 private void box(Canvas c,float x,float y,float w,float h,int col,float r){p.setColor(col);c.drawRoundRect(x,y,x+w,y+h,r,r,p);}
 private void hit(float x,float y,float w,float h,String key,int id){targets.add(new Target(new RectF(x,y,x+w,y+h),key,id));}
 @Override protected void onDraw(Canvas c){super.onDraw(c);c.save();c.scale(density,density);float w=getWidth()/density,h=getHeight()/density;c.drawColor(BACK);targets.clear();synchronized(game){if(header)header(c,w);else controls(c,w,h);}if(pressed!=null){p.setColor(0x33ffffff);c.drawRoundRect(pressed.r,12,12,p);}c.restore();}
 private void header(Canvas c,float w){box(c,12,8,44,44,0xfffff8ed,14);GameIcons.draw(c,p,5,34,30,29);hit(10,6,48,48,"pause",0);text(c,String.format(Locale.KOREA,"STAGE %03d",game.level.id),w/2,30,20,INK);box(c,w-70,8,58,44,0xffffca76,14);text(c,speed+"×",w-41,36,18,INK);hit(w-72,6,62,48,"speed",0);box(c,83,47,w-166,3,0xffe2d4c3,2);if(game.collected>0)box(c,83,47,(w-166)*game.collected/game.blocks.size(),3,0xffef9860,2);}
 private void card(Canvas c,NativeGame.Capsule a,float x,float y,float w,float h,boolean active,boolean front){int col=NativeGame.COLORS[a.color];box(c,x,y+3,w,h,darken(col),12);box(c,x,y,w,h-3,col,12);box(c,x+5,y+4,w-10,3,0x55ffffff,2);text(c,String.valueOf(active?Math.max(0,a.count-a.loaded):a.remaining),x+w/2,y+h*.68f,h*.47f,INK);if(!front&&!active)box(c,x,y,w,h-3,0x35f7eddf,12);if(a.mega)GameIcons.draw(c,p,11,x+w-9,y+10,15);}
 private static int darken(int a){return Color.rgb((int)(Color.red(a)*.77),(int)(Color.green(a)*.77),(int)(Color.blue(a)*.77));}
 private void controls(Canvas c,float w,float h){
 float scale=w/384;c.save();c.scale(scale,scale);float vh=h/scale;
 text(c,game.bots.size()+" 작업 중",326,13,9,MUTED);
 float aw=(356-6*(game.slots.size()-1))/game.slots.size();long now=SystemClock.elapsedRealtime();float ease=lastFrame==0?1:Math.min(1,(now-lastFrame)/140f);lastFrame=now;boolean animating=false;
 for(int i=0;i<game.slots.size();i++){NativeGame.Capsule a=game.slots.get(i);if(a==null)continue;float x=14+i*(aw+6),y=22;float[] pos=positions.get(a.id);if(pos!=null){pos[0]+=(x-pos[0])*ease;pos[1]+=(y-pos[1])*ease;if(Math.abs(pos[1]-y)>.5)animating=true;x=pos[0];y=pos[1];}card(c,a,x,y,aw,43,true,true);}
 box(c,14,75,356,5,0xffe3d4c1,3);if(game.gauge>0)box(c,14,75,356f*game.gauge/game.level.megaRequired,5,0xffffa052,3);GameIcons.draw(c,p,11,20,77,16);if(pullMode)text(c,"꺼낼 컨테이너를 선택하세요",192,98,10,INK);
 int max=0;for(int lane=0;lane<4;lane++){List<NativeGame.Capsule> list=new ArrayList<>();for(NativeGame.Capsule a:game.queue)if(!a.taken&&a.lane==lane)list.add(a);max=Math.max(max,list.size());}page=Math.min(page,Math.max(0,max-3));
 float qy=105,rh=Math.min(49,(vh-105-67)/3),cw=80;
 for(int lane=0;lane<4;lane++){List<NativeGame.Capsule> list=new ArrayList<>();for(NativeGame.Capsule a:game.queue)if(!a.taken&&a.lane==lane)list.add(a);list.sort(Comparator.comparingInt(a->a.depth));for(int row=0;row<3;row++){int index=page+row;if(index>=list.size())continue;NativeGame.Capsule a=list.get(index);float x=20+lane*91,y=qy+row*rh;float[] pos=positions.get(a.id);if(pos==null){pos=new float[]{x,y};positions.put(a.id,pos);}else{pos[0]+=(x-pos[0])*ease;pos[1]+=(y-pos[1])*ease;if(Math.abs(pos[1]-y)>.5)animating=true;}card(c,a,pos[0],pos[1],cw,rh-6,false,game.unlocked(a));hit(x*scale,y*scale,cw*scale,rh*scale,"capsule",a.id);}}
 if(max>3){text(c,"⌃",376,135,10,MUTED);text(c,"⌄",376,208,10,MUTED);hit(365*scale,106*scale,19*scale,45*scale,"queue-page",-1);hit(365*scale,180*scale,19*scale,45*scale,"queue-page",1);}
 float bottom=vh-56;box(c,0,bottom-6,384,65,0xffe9dbc8,20);for(int i=0;i<4;i++){float x=48+i*96;p.setColor(pullMode&&i==3?0xffffca76:0xfffff8ed);c.drawCircle(x,bottom+23,27,p);GameIcons.draw(c,p,i,x,bottom+20,39);box(c,x+9,bottom+30,25,20,0xff83c6a3,10);text(c,save.boosterCount(i),x+21.5f,bottom+45,save.unlimitedItems?15:12,INK);hit((x-35)*scale,(bottom-5)*scale,70*scale,60*scale,"booster",i);}c.restore();if(animating&&!game.paused)postInvalidateOnAnimation();
 }
 private String description(Target t){
  if(t.key.equals("capsule")){NativeGame.Capsule a=game.capsule(t.id);return NativeGame.COLOR_NAMES[a.color]+" 컨테이너, "+a.remaining+"개, "+(game.unlocked(a)?"선택 가능":"뒤쪽");}
  if(t.key.equals("booster"))return new String[]{"작업 공간 추가","컬러 제거","컨테이너 섞기","뒤쪽 컨테이너 꺼내기"}[t.id]+", "+save.boosterCount(t.id);
  if(t.key.equals("pause"))return "일시정지";if(t.key.equals("speed"))return "배속 "+speed;return t.id<0?"앞쪽 컨테이너 보기":"뒤쪽 컨테이너 보기";
 }
 @Override public android.view.accessibility.AccessibilityNodeProvider getAccessibilityNodeProvider(){return new android.view.accessibility.AccessibilityNodeProvider(){
  @Override public android.view.accessibility.AccessibilityNodeInfo createAccessibilityNodeInfo(int id){
   android.view.accessibility.AccessibilityNodeInfo node=android.view.accessibility.AccessibilityNodeInfo.obtain();
   if(id==View.NO_ID){node.setSource(NativeHud.this);NativeHud.this.onInitializeAccessibilityNodeInfo(node);for(int i=0;i<targets.size();i++)node.addChild(NativeHud.this,i);return node;}
   if(id<0||id>=targets.size())return null;Target t=targets.get(id);node.setSource(NativeHud.this,id);node.setParent(NativeHud.this);node.setClassName("android.widget.Button");node.setPackageName(getContext().getPackageName());node.setContentDescription(description(t));node.setEnabled(true);node.setClickable(true);node.setFocusable(true);node.setVisibleToUser(isShown());Rect rect=new Rect((int)(t.r.left*density),(int)(t.r.top*density),(int)(t.r.right*density),(int)(t.r.bottom*density));node.setBoundsInParent(rect);int[] at=new int[2];getLocationOnScreen(at);rect.offset(at[0],at[1]);node.setBoundsInScreen(rect);node.addAction(android.view.accessibility.AccessibilityNodeInfo.ACTION_CLICK);node.setAccessibilityFocused(accessibilityFocus==id);node.addAction(accessibilityFocus==id?128:64);return node;
  }
  @Override public boolean performAction(int id,int action,android.os.Bundle args){if(id<0||id>=targets.size())return false;if(action==16){Target t=targets.get(id);actions.action(t.key,t.id);return true;}if(action==64||action==128){accessibilityFocus=action==64?id:-1;android.view.accessibility.AccessibilityEvent e=android.view.accessibility.AccessibilityEvent.obtain(action==64?32768:65536);e.setSource(NativeHud.this,id);e.setPackageName(getContext().getPackageName());e.setClassName("android.widget.Button");getParent().requestSendAccessibilityEvent(NativeHud.this,e);invalidate();return true;}return false;}
 };}
 @Override public boolean onTouchEvent(MotionEvent e){float x=e.getX()/density,y=e.getY()/density;if(e.getAction()==MotionEvent.ACTION_DOWN){downY=y;pressed=null;for(Target t:targets)if(t.r.contains(x,y)){pressed=t;break;}invalidate();return true;}if(e.getAction()==MotionEvent.ACTION_UP){Target t=pressed;pressed=null;if(!header&&Math.abs(y-downY)>30&&downY>100&&downY<getHeight()/density-65){page=Math.max(0,page+(y<downY?1:-1));invalidate();return true;}if(t!=null&&t.r.contains(x,y)){performClick();actions.action(t.key,t.id);}invalidate();return true;}if(e.getAction()==MotionEvent.ACTION_CANCEL){pressed=null;invalidate();}return true;}
 @Override public boolean performClick(){super.performClick();return true;}
}
