package com.bossxor.robotflow;
import android.content.Context;
import android.graphics.*;
import android.view.View;
/** Outlined toy title and coin badge for the home screen. */
final class HomeBrand extends View {
 private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG); private final int coins; private final Path star=new Path(); private final Typeface logoFont=Typeface.create("sans-serif-black",Typeface.BOLD);
 HomeBrand(Context context){this(context,0);}
 HomeBrand(Context context,int coins){super(context);this.coins=coins;setContentDescription("ROBO RELAY, 보유 코인 "+coins);}
 private void title(Canvas c,String label,float y,int fill,float size){p.setTypeface(logoFont);p.setTextAlign(Paint.Align.CENTER);p.setTextSize(size);p.setStrokeJoin(Paint.Join.ROUND);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(10);p.setColor(0xff578c99);c.drawText(label,192,y+5,p);p.setColor(0xfffffff4);c.drawText(label,192,y,p);p.setStyle(Paint.Style.FILL);p.setShader(new LinearGradient(0,y-size,0,y,new int[]{0xffe0ffff,fill,fill},new float[]{0,.4f,1},Shader.TileMode.CLAMP));c.drawText(label,192,y,p);p.setShader(null);}
 @Override protected void onDraw(Canvas c){c.save();c.scale(getWidth()/384f,getHeight()/144f);p.setColor(0xff70ada8);c.drawRoundRect(279,10,367,40,15,15,p);p.setColor(0xfffffff4);c.drawRoundRect(279,6,367,35,15,15,p);p.setTextAlign(Paint.Align.CENTER);p.setTypeface(Typeface.DEFAULT_BOLD);p.setTextSize(12);p.setColor(0xffb68029);c.drawText("◈ "+coins,323,25,p);c.save();c.rotate(-3,192,88);title(c,"ROBO",79,0xff4cbecc,47);title(c,"RELAY",125,0xffaa74dd,46);c.restore();for(int i=0;i<2;i++){float x=i==0?50:330,y=i==0?80:109;p.setColor(i==0?0xfff5c75e:0xff90c6e1);star.reset();star.moveTo(x,y-10);star.lineTo(x+3,y-3);star.lineTo(x+10,y);star.lineTo(x+3,y+3);star.lineTo(x,y+10);star.lineTo(x-3,y+3);star.lineTo(x-10,y);star.lineTo(x-3,y-3);star.close();c.drawPath(star,p);}c.restore();}
}
