package com.bossxor.robotflow;

import android.app.Dialog;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.view.*;
import android.view.animation.DecelerateInterpolator;
import android.widget.*;

/** Shared game UI: branded sheets, cards, touch feedback and readable actions. */
final class GameSheet extends Dialog {
    static final int INK=0xff353b43, CREAM=0xfff7eddf, LIME=0xffffca76, MUTED=0xff8e877e, CARD=0xffefe2d1;
    final LinearLayout body;
    private final LinearLayout panel;
    private final Context context;
    private Runnable closed;
    private boolean closing;
    private final TextView heading;
    GameSheet(Context context,String eyebrow,String title,String subtitle,boolean cancelable){
        super(context);this.context=context;requestWindowFeature(Window.FEATURE_NO_TITLE);
        setCancelable(cancelable);setCanceledOnTouchOutside(cancelable);
        panel=new LinearLayout(context);panel.setOrientation(LinearLayout.VERTICAL);panel.setBackground(shape(context,CREAM,28));panel.setClipToOutline(true);panel.setElevation(dp(16));
        LinearLayout hero=new LinearLayout(context);hero.setOrientation(LinearLayout.VERTICAL);hero.setPadding(dp(24),dp(22),dp(24),dp(22));hero.setBackgroundColor(INK);
        LinearLayout top=new LinearLayout(context);top.setGravity(Gravity.CENTER_VERTICAL);
        TextView tag=label(context,eyebrow,10,LIME,true);tag.setLetterSpacing(.15f);top.addView(tag,new LinearLayout.LayoutParams(0,dp(32),1));
        if(cancelable){TextView x=action(context,"×",false,this::cancel);x.setPadding(0,0,0,0);x.setMinHeight(0);x.setTextSize(26);x.setTextColor(LIME);x.setBackground(ripple(context,0xff2b5049,14));top.addView(x,new LinearLayout.LayoutParams(dp(44),dp(44)));x.setContentDescription("닫기");}if(!eyebrow.isEmpty())hero.addView(top);
        heading=label(context,title,25,CREAM,true);heading.setPadding(0,dp(8),0,dp(8));hero.addView(heading);
        TextView sub=label(context,subtitle,12,0xffd9c8b4,false);sub.setLineSpacing(dp(3),1);if(!subtitle.isEmpty())hero.addView(sub);if(!title.isEmpty()){panel.addView(hero);}
        ScrollView scroll=new ScrollView(context);scroll.setFillViewport(false);scroll.setVerticalScrollBarEnabled(false);scroll.setOverScrollMode(View.OVER_SCROLL_NEVER);
        body=new LinearLayout(context);body.setOrientation(LinearLayout.VERTICAL);body.setPadding(dp(20),dp(18),dp(20),dp(20));scroll.addView(body);panel.addView(scroll,new LinearLayout.LayoutParams(-1,-2));
        setContentView(panel);setOnCancelListener(d->closeCallback());
    }
    void icons(int[] ids,String[] names,Runnable[] runs){
        for(int row=0;row<2;row++){LinearLayout line=new LinearLayout(context);for(int col=0;col<2;col++){int i=row*2+col;GameIcons icon=new GameIcons(context,ids[i],names[i],runs[i]);icon.setOnLongClickListener(v->{android.widget.Toast.makeText(context,names[i],android.widget.Toast.LENGTH_SHORT).show();return true;});LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,dp(100),1);lp.setMargins(dp(6),dp(6),dp(6),dp(6));line.addView(icon,lp);}body.addView(line);}
    }
    void onClose(Runnable callback){closed=callback;}
    void secretGesture(Runnable unlocked){
        SecretTapSequence taps=new SecretTapSequence();
        heading.setOnClickListener(v->{if(taps.tap(android.os.SystemClock.elapsedRealtime()))unlocked.run();});
    }
    void notifyMode(String message){
        final TextView banner=label(context,message,13,INK,true);banner.setGravity(Gravity.CENTER);banner.setPadding(dp(12),dp(12),dp(12),dp(12));banner.setBackground(shape(context,LIME,14));banner.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
        body.addView(banner,0,new LinearLayout.LayoutParams(-1,dp(48)));banner.setAlpha(0);banner.animate().alpha(1).setDuration(150).start();
        banner.postDelayed(()->body.removeView(banner),2500);
    }
    private void closeCallback(){if(!closing){closing=true;if(closed!=null)closed.run();}}
    @Override public void cancel(){super.cancel();}
    @Override public void show(){
        super.show();Window w=getWindow();if(w==null)return;
        w.setBackgroundDrawableResource(android.R.color.transparent);w.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);w.setDimAmount(.64f);
        int width=Math.min(context.getResources().getDisplayMetrics().widthPixels-dp(32),dp(420));w.setLayout(width,-2);w.setGravity(Gravity.CENTER);
        int maxHeight=(int)(context.getResources().getDisplayMetrics().heightPixels*.84f);
        panel.measure(View.MeasureSpec.makeMeasureSpec(width,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(maxHeight,View.MeasureSpec.AT_MOST));
        if(panel.getMeasuredHeight()>=maxHeight)w.setLayout(width,maxHeight);
        panel.setAlpha(0);panel.setTranslationY(dp(24));panel.animate().alpha(1).translationY(0).setDuration(220).setInterpolator(new DecelerateInterpolator()).start();
    }
    int dp(float v){return dp(context,v);}
    static int dp(Context c,float v){return Math.round(v*c.getResources().getDisplayMetrics().density);}
    static GradientDrawable shape(Context c,int color,float radius){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(c,radius));return d;}
    static RippleDrawable ripple(Context c,int color,float radius){return new RippleDrawable(ColorStateList.valueOf(0x22749184),shape(c,color,radius),shape(c,Color.WHITE,radius));}
    static TextView label(Context c,String value,int size,int color,boolean bold){TextView t=new TextView(c);t.setText(value);t.setTextSize(size);t.setTextColor(color);t.setTypeface(Typeface.create("sans-serif",bold?Typeface.BOLD:Typeface.NORMAL));t.setIncludeFontPadding(false);return t;}
    static TextView action(Context c,String title,boolean primary,Runnable run){
        TextView b=label(c,title,14,primary?LIME:INK,true);b.setGravity(Gravity.CENTER);b.setPadding(dp(c,12),dp(c,12),dp(c,12),dp(c,12));b.setBackground(ripple(c,primary?INK:CARD,16));b.setClickable(true);b.setFocusable(true);b.setMinHeight(dp(c,48));
        b.setOnClickListener(v->{v.animate().scaleX(1).scaleY(1).setDuration(120).start();run.run();});
        b.setOnTouchListener((v,e)->{if(e.getAction()==MotionEvent.ACTION_DOWN)v.animate().scaleX(.97f).scaleY(.97f).setDuration(80).start();else if(e.getAction()==MotionEvent.ACTION_UP||e.getAction()==MotionEvent.ACTION_CANCEL)v.animate().scaleX(1).scaleY(1).setDuration(120).start();return false;});return b;
    }
    void gap(int size){body.addView(new View(context),new LinearLayout.LayoutParams(1,dp(size)));}
    void note(String value){TextView t=label(context,value,12,MUTED,false);t.setLineSpacing(dp(4),1);body.addView(t);gap(12);}
    void primary(String value,Runnable run){body.addView(action(context,value,true,run),new LinearLayout.LayoutParams(-1,dp(52)));gap(8);}
    void secondary(String value,Runnable run){body.addView(action(context,value,false,run),new LinearLayout.LayoutParams(-1,dp(48)));gap(8);}
    void card(String icon,String title,String description,int color){
        LinearLayout row=new LinearLayout(context);row.setGravity(Gravity.CENTER_VERTICAL);row.setPadding(dp(14),dp(14),dp(14),dp(14));row.setBackground(shape(context,CARD,18));
        TextView badge=label(context,icon,23,INK,true);badge.setGravity(Gravity.CENTER);badge.setBackground(shape(context,color,14));row.addView(badge,new LinearLayout.LayoutParams(dp(48),dp(48)));
        LinearLayout copy=new LinearLayout(context);copy.setOrientation(LinearLayout.VERTICAL);copy.setPadding(dp(12),0,0,0);copy.addView(label(context,title,14,INK,true));TextView desc=label(context,description,11,MUTED,false);desc.setPadding(0,dp(5),0,0);desc.setLineSpacing(dp(2),1);copy.addView(desc);row.addView(copy,new LinearLayout.LayoutParams(0,-2,1));body.addView(row);gap(10);
    }
    void card(int icon,String title,String description,int color){
        LinearLayout row=new LinearLayout(context);row.setGravity(Gravity.CENTER_VERTICAL);row.setPadding(dp(14),dp(14),dp(14),dp(14));row.setBackground(shape(context,CARD,18));
        GameIcons badge=new GameIcons(context,icon,title,null);row.addView(badge,new LinearLayout.LayoutParams(dp(48),dp(48)));
        LinearLayout copy=new LinearLayout(context);copy.setOrientation(LinearLayout.VERTICAL);copy.setPadding(dp(12),0,0,0);copy.addView(label(context,title,14,INK,true));TextView desc=label(context,description,11,MUTED,false);desc.setPadding(0,dp(5),0,0);desc.setLineSpacing(dp(2),1);copy.addView(desc);row.addView(copy,new LinearLayout.LayoutParams(0,-2,1));body.addView(row);gap(10);
    }
    void toggle(String title,String subtitle,boolean checked,java.util.function.Consumer<Boolean> changed){
        LinearLayout row=new LinearLayout(context);row.setGravity(Gravity.CENTER_VERTICAL);row.setPadding(dp(14),dp(12),dp(12),dp(12));row.setBackground(shape(context,CARD,16));
        LinearLayout copy=new LinearLayout(context);copy.setOrientation(LinearLayout.VERTICAL);copy.addView(label(context,title,14,INK,true));TextView sub=label(context,subtitle,10,MUTED,false);sub.setPadding(0,dp(4),0,0);if(!subtitle.isEmpty())copy.addView(sub);row.addView(copy,new LinearLayout.LayoutParams(0,-2,1));
        TextView chip=action(context,"",false,()->{});final boolean[] value={checked};Runnable refresh=()->{chip.setText(value[0]?"ON ●":"○ OFF");chip.setTextColor(value[0]?LIME:MUTED);chip.setBackground(ripple(context,value[0]?INK:0xffdce4d5,24));chip.setContentDescription(title+(value[0]?" 켜짐":" 꺼짐"));};refresh.run();chip.setOnClickListener(v->{value[0]=!value[0];refresh.run();changed.accept(value[0]);});row.addView(chip,new LinearLayout.LayoutParams(dp(80),dp(44)));body.addView(row);gap(8);
    }
    void slider(String title,float value,java.util.function.Consumer<Float> changed,Runnable finished){
        LinearLayout box=new LinearLayout(context);box.setOrientation(LinearLayout.VERTICAL);box.setPadding(dp(14),dp(10),dp(14),dp(10));box.setBackground(shape(context,CARD,16));
        TextView heading=label(context,title+"  "+Math.round(value*100)+"%",12,INK,true);box.addView(heading);
        SeekBar slider=new SeekBar(context);slider.setMax(100);slider.setProgress(Math.round(value*100));slider.setProgressTintList(ColorStateList.valueOf(0xff8aa85d));slider.setProgressBackgroundTintList(ColorStateList.valueOf(0xffccd9bf));GradientDrawable thumb=shape(context,INK,12);thumb.setSize(dp(22),dp(22));thumb.setStroke(dp(3),LIME);slider.setThumb(thumb);slider.setThumbOffset(dp(11));slider.setPadding(dp(12),0,dp(12),0);slider.setContentDescription(title);
        slider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){public void onProgressChanged(SeekBar s,int p,boolean user){heading.setText(title+"  "+p+"%");changed.accept(p/100f);}public void onStartTrackingTouch(SeekBar s){}public void onStopTrackingTouch(SeekBar s){finished.run();}});box.addView(slider,new LinearLayout.LayoutParams(-1,dp(44)));body.addView(box);gap(8);
    }
}
