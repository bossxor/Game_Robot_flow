package com.bossxor.robotflow;
import android.content.Context;
import android.graphics.*;
import android.view.View;
/** Illustrated scalable controls shared by menus and game HUD. */
final class GameIcons extends View {
 private final int icon; private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
 GameIcons(Context c,int i,String desc,Runnable action){super(c);icon=i;setContentDescription(desc);setFocusable(true);setClickable(true);setBackground(GameSheet.ripple(c,0xfffff7e9,18));if(action!=null)setOnClickListener(v->action.run());else{setClickable(false);setFocusable(false);setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);setBackground(null);}}
 @Override protected void onDraw(Canvas c){super.onDraw(c);draw(c,p,icon,getWidth()/2f,getHeight()/2f,Math.min(getWidth(),getHeight())*.72f);}
 private static void box(Canvas c,Paint p,float x,float y,float w,float h,int col){p.setColor(col);c.drawRoundRect(x,y,x+w,y+h,5,5,p);}
 private static void line(Canvas c,Paint p,float x,float y,float a,float b,int col){p.setColor(col);p.setStrokeWidth(5);p.setStrokeCap(Paint.Cap.ROUND);c.drawLine(x,y,a,b,p);}
 static void draw(Canvas c,Paint p,int id,float x,float y,float size){
 c.save();c.translate(x-size/2,y-size/2);c.scale(size/64,size/64);p.setStyle(Paint.Style.FILL);
 int ink=0xff353b43,cream=0xfffff2d8,orange=0xffffa052,blue=0xff61ace2,mint=0xff83c6a3,purple=0xff8b78b8;
 if(id==0){box(c,p,8,22,44,27,orange);box(c,p,5,43,50,7,ink);box(c,p,26,15,8,25,cream);box(c,p,39,38,22,22,mint);line(c,p,50,42,50,56,cream);line(c,p,43,49,57,49,cream);
 }else if(id==1){c.rotate(-35,32,32);box(c,p,25,25,13,33,ink);box(c,p,17,12,29,21,blue);box(c,p,17,26,29,7,cream);box(c,p,20,7,6,12,purple);box(c,p,30,5,6,14,orange);
 }else if(id==2){box(c,p,7,27,22,25,orange);box(c,p,37,15,22,25,blue);line(c,p,9,16,31,16,mint);line(c,p,31,16,25,10,mint);line(c,p,55,49,34,49,purple);line(c,p,34,49,40,55,purple);line(c,p,18,30,18,47,cream);line(c,p,48,19,48,35,cream);
 }else if(id==3){box(c,p,26,5,12,22,purple);box(c,p,17,20,30,12,blue);line(c,p,20,29,12,45,ink);line(c,p,12,45,23,54,ink);line(c,p,44,29,52,45,ink);line(c,p,52,45,41,54,ink);box(c,p,26,39,12,17,orange);
 }else if(id==4){p.setColor(ink);Path a=new Path();a.moveTo(22,12);a.lineTo(52,32);a.lineTo(22,52);a.close();c.drawPath(a,p);
 }else if(id==5){box(c,p,16,13,11,38,ink);box(c,p,37,13,11,38,ink);
 }else if(id==6){box(c,p,10,24,14,18,ink);Path a=new Path();a.moveTo(21,24);a.lineTo(37,13);a.lineTo(37,52);a.lineTo(21,42);a.close();p.setColor(ink);c.drawPath(a,p);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(4);c.drawArc(30,15,57,49,-65,130,false,p);p.setStyle(Paint.Style.FILL);
 }else if(id==7){p.setColor(ink);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(6);c.drawArc(12,13,52,53,-70,285,false,p);p.setStyle(Paint.Style.FILL);Path a=new Path();a.moveTo(42,6);a.lineTo(57,19);a.lineTo(38,23);a.close();c.drawPath(a,p);
 }else if(id==8){Path a=new Path();a.moveTo(7,29);a.lineTo(32,9);a.lineTo(57,29);a.close();p.setColor(orange);c.drawPath(a,p);box(c,p,14,28,36,27,ink);box(c,p,27,36,11,19,cream);
 }else if(id==9){for(int i=0;i<4;i++)box(c,p,12+(i%2)*23,12+(i/2)*23,18,18,i==0?orange:ink);
 }else if(id==10){p.setColor(ink);c.drawCircle(32,32,20,p);for(int i=0;i<8;i++){c.save();c.rotate(i*45,32,32);box(c,p,27,5,10,15,ink);c.restore();}p.setColor(cream);c.drawCircle(32,32,9,p);
 }else{box(c,p,9,11,46,41,orange);box(c,p,13,26,38,19,ink);box(c,p,20,31,6,8,cream);box(c,p,38,31,6,8,cream);line(c,p,25,55,39,55,ink);}c.restore();
 }
}
