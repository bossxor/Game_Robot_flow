package com.bossxor.robotflow;

import android.app.*;
import android.content.*;
import android.graphics.Typeface;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.media.AudioManager;
import android.os.*;
import android.view.*;
import android.widget.*;
import android.window.OnBackInvokedDispatcher;
import java.util.*;

/** Native Android menus, fixed touch controls and a real OpenGL 3D scene. */
public final class MainActivity extends Activity {
    private static final int INK=0xff353b43, BACK=0xfff7eddf, LIME=0xffffca76, MUTED=0xff8e877e;
    private final Handler handler=new Handler(Looper.getMainLooper());
    private FrameLayout root;
    private LinearLayout content;
    private PlayerSave save;
    private NativeGame game;
    private NativeScene scene;
    private NativeHud header,controls;
    private GameAudio audio;
    private String screen="home";
    private boolean foreground,resultShown,inspect;
    private GameSheet dialog;
    private TextView notice;
    private long lastSound;
    private int tuneNote,maxControlsHeight,targetControlsHeight;
    private android.animation.ValueAnimator queueResize;
    private final Runnable heartbeat=new Runnable(){@Override public void run(){
        if(foreground){if(header!=null)header.invalidate();if(controls!=null){resizeQueue();controls.invalidate();}}
        handler.postDelayed(this,100);
    }};
    private final Runnable musicLoop=new Runnable(){@Override public void run(){
        if(foreground&&save.music&&(game==null||!game.paused)&&audio!=null)audio.music(tuneNote++,save.musicVolume);
        handler.postDelayed(this,1500);
    }};
    @Override public void onCreate(Bundle state){
        super.onCreate(state);save=new PlayerSave(this);setVolumeControlStream(AudioManager.STREAM_MUSIC);
        getWindow().setStatusBarColor(INK);getWindow().setNavigationBarColor(BACK);
        root=new FrameLayout(this);root.setBackgroundColor(BACK);
        root.setOnApplyWindowInsetsListener((v,insets)->{
            if(Build.VERSION.SDK_INT>=30){android.graphics.Insets s=insets.getInsets(WindowInsets.Type.systemBars()|WindowInsets.Type.displayCutout());v.setPadding(s.left,screen.equals("game")?0:s.top,s.right,s.bottom);}
            else v.setPadding(insets.getSystemWindowInsetLeft(),insets.getSystemWindowInsetTop(),insets.getSystemWindowInsetRight(),insets.getSystemWindowInsetBottom());return insets;
        });setContentView(root);setupAudio();showHome();handler.post(heartbeat);handler.postDelayed(musicLoop,900);
        if(BuildConfig.DEBUG&&getIntent().hasExtra("inspect_level")){inspect=true;startLevel(getIntent().getIntExtra("inspect_level",1));
            if(game!=null&&getIntent().hasExtra("inspect_queue_rows")){int rows=Math.max(0,getIntent().getIntExtra("inspect_queue_rows",4)),columns=Math.max(0,Math.min(4,getIntent().getIntExtra("inspect_queue_columns",4)));synchronized(game){game.paused=true;game.level.autoFinish=false;for(int lane=0;lane<4;lane++){List<NativeGame.Capsule> pending=new ArrayList<>();for(NativeGame.Capsule c:game.queue)if(c.lane==lane)pending.add(c);pending.sort(Comparator.comparingInt(c->c.depth));int kept=lane<columns?Math.min(rows,pending.size()):0;for(int i=0;i<pending.size()-kept;i++)pending.get(i).taken=true;}}}}
        if(Build.VERSION.SDK_INT>=33)getOnBackInvokedDispatcher().registerOnBackInvokedCallback(OnBackInvokedDispatcher.PRIORITY_DEFAULT,this::handleBack);
    }
    private int dp(float n){return Math.round(n*getResources().getDisplayMetrics().density);}
    private GradientDrawable background(int color,float radius){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(radius));return d;}
    private TextView text(String value,int size,int color,boolean bold){TextView t=new TextView(this);t.setText(value);t.setTextSize(size);t.setTextColor(color);t.setTypeface(Typeface.create("sans-serif",bold?Typeface.BOLD:Typeface.NORMAL));t.setGravity(Gravity.CENTER);t.setIncludeFontPadding(false);return t;}
    private Button button(String label,boolean primary,Runnable action){Button b=new Button(this);b.setText(label);b.setAllCaps(false);b.setTextSize(14);b.setTextColor(INK);b.setTypeface(Typeface.DEFAULT,Typeface.BOLD);b.setBackground(GameSheet.ripple(this,primary?LIME:0xffefe2d1,16));b.setStateListAnimator(null);b.setMinHeight(0);b.setMinimumHeight(0);b.setPadding(dp(12),0,dp(12),0);b.setOnClickListener(v->{effect("click");action.run();});return b;}
    private void gap(LinearLayout layout,int height){layout.addView(new View(this),new LinearLayout.LayoutParams(1,dp(height)));}
    private void dismiss(){if(dialog!=null){dialog.setOnDismissListener(null);dialog.dismiss();dialog=null;}}
    private void rebuild(String mode){
        dismiss();if(queueResize!=null){queueResize.cancel();queueResize=null;}if(scene!=null)scene.onPause();scene=null;header=controls=null;
        screen=mode;root.removeAllViews();notice=null;content=new LinearLayout(this);content.setOrientation(LinearLayout.VERTICAL);content.setBackgroundColor(BACK);root.addView(content,new FrameLayout.LayoutParams(-1,-1));root.requestApplyInsets();
    }
    private void showHome(){
        if(game!=null){synchronized(game){game.paused=true;}game.listener=null;}
        game=null;inspect=false;getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);rebuild("home");
        Typeface round=ColonyStyle.load(this);
        content.setBackground(new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,new int[]{ColonyStyle.HOME_SKY_TOP,ColonyStyle.HOME_SKY_MID,ColonyStyle.HOME_SKY_BOT}));
        // Colony-style top: profile · coins · settings (no hearts).
        LinearLayout top=new LinearLayout(this);top.setOrientation(LinearLayout.HORIZONTAL);top.setGravity(Gravity.CENTER_VERTICAL);top.setPadding(dp(14),dp(10),dp(14),dp(6));
        View profile=new View(this);GradientDrawable profileBg=background(0xff4aa8e8,28);profile.setBackground(profileBg);profile.setContentDescription("프로필");LinearLayout.LayoutParams profileLp=new LinearLayout.LayoutParams(dp(52),dp(52));top.addView(profile,profileLp);
        TextView coins=text("◈ "+save.coins,18,Color.WHITE,true);coins.setTypeface(round);coins.setBackground(background(ColonyStyle.COIN_PILL,20));coins.setPadding(dp(16),dp(8),dp(16),dp(8));LinearLayout.LayoutParams coinLp=new LinearLayout.LayoutParams(-2,-2);coinLp.leftMargin=dp(10);top.addView(coins,coinLp);
        View spacer=new View(this);top.addView(spacer,new LinearLayout.LayoutParams(0,1,1));
        GameIcons settings=new GameIcons(this,10,"설정",()->{effect("click");showSettings();});settings.homeStyle();top.addView(settings,new LinearLayout.LayoutParams(dp(52),dp(52)));
        content.addView(top,new LinearLayout.LayoutParams(-1,-2));
        try{
            FrameLayout hero=new FrameLayout(this);
            scene=new NativeScene(this,new NativeGame(LevelLoader.load(getAssets(),save.current)),true);
            hero.addView(scene,new FrameLayout.LayoutParams(-1,-1));
            boolean compactHome=getResources().getDisplayMetrics().heightPixels/getResources().getDisplayMetrics().density<700;
            TextView stage=text("LEVEL\n"+save.current,compactHome?34:42,0xffffffff,true);stage.setTypeface(round);
            android.text.SpannableString stageLabel=new android.text.SpannableString("LEVEL\n"+save.current);stageLabel.setSpan(new android.text.style.RelativeSizeSpan(.45f),0,5,0);stage.setText(stageLabel);
            stage.setShadowLayer(dp(2),0,dp(3),0xff4a2080);
            FrameLayout.LayoutParams badge=new FrameLayout.LayoutParams(1,1,Gravity.TOP|Gravity.LEFT);hero.addView(stage,badge);
            hero.addOnLayoutChangeListener((v,left,topEdge,right,bottomEdge,oldLeft,oldTop,oldRight,oldBottom)->{
                if(right-left==oldRight-oldLeft&&bottomEdge-topEdge==oldBottom-oldTop)return;
                float[] bounds=NativeScene.levelBadgeBounds(right-left,bottomEdge-topEdge);FrameLayout.LayoutParams lp=(FrameLayout.LayoutParams)stage.getLayoutParams();
                lp.leftMargin=Math.round(bounds[0]);lp.topMargin=Math.round(bounds[1]);lp.width=Math.round(bounds[2]);lp.height=Math.round(bounds[3]);stage.setLayoutParams(lp);
                stage.setTextSize(Math.min(42,bounds[3]/getResources().getDisplayMetrics().density*.46f));
            });
            content.addView(hero,new LinearLayout.LayoutParams(-1,0,1));
        }catch(Exception error){toast(error.getMessage());}
        LinearLayout bottom=new LinearLayout(this);bottom.setOrientation(LinearLayout.VERTICAL);bottom.setPadding(dp(20),dp(0),dp(20),dp(0));
        int difficulty=save.current%10==0?2:save.current%5==0?1:0;
        TextView badge=text(new String[]{"Normal","Hard","Super Hard"}[difficulty],15,Color.WHITE,true);badge.setTypeface(round);
        badge.setBackground(background(new int[]{0xff3677e7,0xff9644db,0xffef365c}[difficulty],12));
        LinearLayout.LayoutParams difficultyBadge=new LinearLayout.LayoutParams(dp(difficulty==2?130:100),dp(28));difficultyBadge.gravity=Gravity.CENTER;difficultyBadge.bottomMargin=dp(-10);bottom.addView(badge,difficultyBadge);
        Button play=button("Play",true,()->startLevel(save.current));play.setTypeface(round);play.setTextSize(32);play.setTextColor(0xffffffff);play.setShadowLayer(dp(2),0,dp(2),0xff2a7a30);
        GradientDrawable face=new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,new int[]{0xff7ae85a,ColonyStyle.PLAY_GREEN});face.setCornerRadius(dp(28));face.setStroke(dp(5),ColonyStyle.PLAY_EDGE);
        GradientDrawable base=background(0xff2f9a3a,28);android.graphics.drawable.LayerDrawable layers=new android.graphics.drawable.LayerDrawable(new android.graphics.drawable.Drawable[]{base,face});layers.setLayerInset(1,0,0,0,dp(6));play.setBackground(new android.graphics.drawable.RippleDrawable(android.content.res.ColorStateList.valueOf(0x22ffffff),layers,null));play.setPadding(0,0,0,dp(6));
        LinearLayout.LayoutParams playLp=new LinearLayout.LayoutParams(-1,dp(72));playLp.setMargins(dp(8),dp(4),dp(8),dp(10));bottom.addView(play,playLp);
        // Colony-style bottom nav bar (no shop ads / hearts).
        LinearLayout nav=new LinearLayout(this);nav.setOrientation(LinearLayout.HORIZONTAL);nav.setGravity(Gravity.CENTER);nav.setBackground(background(ColonyStyle.NAV_BAR,22));nav.setPadding(dp(8),dp(10),dp(8),dp(10));
        String[] labels={"스테이지","일일","홈","아이템","설정"};int[] navIcons={9,0,8,0,10};
        for(int i=0;i<5;i++){final int n=i;LinearLayout column=new LinearLayout(this);column.setOrientation(LinearLayout.VERTICAL);column.setGravity(Gravity.CENTER);
            boolean homeTab=n==2;GameIcons icon=new GameIcons(this,navIcons[i],labels[i],()->{effect("click");if(n==0)showStages((save.current-1)/20);else if(n==1)claimDailyReward();else if(n==2)showHome();else if(n==3)showItems();else showSettings();});
            icon.homeStyle();if(homeTab)icon.setBackground(background(0xff4a6ad8,18));
            column.addView(icon,new LinearLayout.LayoutParams(dp(homeTab?58:48),dp(homeTab?58:48)));
            TextView label=text(labels[i],10,homeTab?0xffffd76a:0xffc8d0f0,true);label.setTypeface(round);column.addView(label);
            LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,-2,1);nav.addView(column,lp);
        }
        bottom.addView(nav,new LinearLayout.LayoutParams(-1,dp(92)));
        content.addView(bottom,new LinearLayout.LayoutParams(-1,-2));
    }
    private void claimDailyReward(){
        int gained=save.claimDaily();
        if(gained<=0){toast("오늘은 이미 받았어요.");return;}
        effect("power");toast("일일 보상 ◈ "+gained);showHome();
    }

    private void startLevel(int id){
        if(id<1||id>LevelLoader.TOTAL||(!inspect&&id>save.unlocked))return;
        try {
            NativeGame next=new NativeGame(LevelLoader.load(getAssets(),id));
            if(game!=null)game.listener=null;rebuild("game");game=next;resultShown=false;if(!inspect){save.current=id;save.persist();}
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            final NativeGame owner=game;
            game.listener=(type,color,x,y)->{if(type.equals("select")&&game==owner&&controls!=null)controls.captureLaunches();handler.post(()->{if(game==owner){effect(type);if(type.equals("lost"))showResult();if(type.equals("won"))handler.postDelayed(()->{if(game==owner)showResult();},850);if(type.equals("finishRush"))toast("마지막 작업 · 10배속");}});};
            header=new NativeHud(this,game,save,true,this::action);header.speed=save.gameSpeed;content.addView(header,new LinearLayout.LayoutParams(-1,dp(86)));
            scene=new NativeScene(this,game,false);scene.speed=save.gameSpeed;scene.blockTap=color->{synchronized(owner){if(game==owner&&controls.clearMode){owner.paused=false;if(owner.clearColor(color,save.unlimitedItems)){controls.clearMode=false;scene.setClearMode(false);owner.paused=false;consume(1);}else{owner.paused=true;toast("컬러 제거는 스테이지당 1회 사용할 수 있어요.");}}}};content.addView(scene,new LinearLayout.LayoutParams(-1,0,1));
            controls=new NativeHud(this,game,save,false,this::action);
            float available=(root.getHeight()>0?root.getHeight()-root.getPaddingBottom():getResources().getDisplayMetrics().heightPixels)/getResources().getDisplayMetrics().density;
            float screenWidth=getResources().getDisplayMetrics().widthPixels/getResources().getDisplayMetrics().density;
            int controlHeight=Math.round(GameLayout.controlsHeight(screenWidth,available));
            maxControlsHeight=targetControlsHeight=dp(controlHeight);content.addView(controls,new LinearLayout.LayoutParams(-1,maxControlsHeight));
            if(game.level.difficulty>0)toast(game.level.difficulty==2?"SUPER HARD · 묶음 작업에 도전하세요":"HARD · 작업 순서를 살펴보세요");if(!getPreferences(MODE_PRIVATE).getBoolean("constructionTutorial",false)){
                synchronized(game){game.paused=true;}
                dialog=new GameSheet(this,"","현장에 오신 걸 환영해요","",false);
                dialog.card("01","같은 색을 선택하세요","각 열의 맨 앞 컨테이너를 선택하세요.\n같은 색 로봇이 블록을 회수합니다.",0xffb7dbe2);
                dialog.card("02","트럭으로 짐을 운반해요","컨테이너는 동시에 5개까지 작업합니다.\n공용 트럭은 짐이 차면 출발합니다.",0xfff1d38b);
                dialog.card("03","배속으로 속도를 고르세요","1배 · 3배 · 5배를 고를 수 있어요.\n마지막 작업은 자동으로 10배로 끝납니다.",LIME);
                dialog.primary("좋아요, 출격!  →",()->{dismiss();getPreferences(MODE_PRIVATE).edit().putBoolean("constructionTutorial",true).apply();synchronized(owner){owner.paused=false;}});dialog.show();
            }
        }catch(Exception error){showHome();toast("레벨 로딩 오류: "+error.getMessage());}
    }
    private void resizeQueue(){
        if(game==null||controls==null||controls.getWidth()==0)return;int[] lanes=new int[4];synchronized(game){for(NativeGame.Capsule c:game.queue)if(!c.taken)lanes[c.lane]++;}int rows=0;for(int n:lanes)rows=Math.max(rows,n);
        int next=Math.round(GameLayout.compactHeight(maxControlsHeight,controls.getWidth(),rows));if(next==targetControlsHeight)return;targetControlsHeight=next;if(queueResize!=null)queueResize.cancel();final NativeHud owner=controls;
        queueResize=android.animation.ValueAnimator.ofInt(owner.getHeight(),next);queueResize.setDuration(220);queueResize.addUpdateListener(a->{if(controls==owner){android.view.ViewGroup.LayoutParams lp=owner.getLayoutParams();lp.height=(Integer)a.getAnimatedValue();owner.setLayoutParams(lp);}});queueResize.start();
    }
    private void action(String key,int value){
        if(game==null)return;
        if(key.equals("home")){confirmLeave();return;}
        if(key.equals("pause")){showPause();return;}
        synchronized(game){
            if(key.equals("speed")){if(game.finishing)return;save.gameSpeed=PlayerSave.nextSpeed(save.gameSpeed);save.persist();scene.speed=save.gameSpeed;header.speed=save.gameSpeed;header.invalidate();return;}
            if(key.equals("queue-page")){controls.page=Math.max(0,controls.page+value);controls.invalidate();return;}
            if(key.equals("capsule")){
                if(controls.pullMode){if(save.hasBooster(3)&&game.pullOut(value)){save.consumeBooster(3);controls.pullMode=false;effect("select");}else toast("뒤쪽에 잠긴 컨테이너를 선택하세요.");}
                else if(!game.select(value))toast(game.keyLocked(game.capsule(value))?"보드의 열쇠를 먼저 회수하세요.":"묶음 전체가 들어갈 빈칸과 맨 앞 컨테이너를 확인하세요.");
            }else if(key.equals("booster"))useBooster(value);
            if(controls!=null)controls.invalidate();
        }
    }
    private void useBooster(int which){
        if(!save.hasBooster(which)){toast("보유 아이템이 없어요.");return;}
        if(which==1){controls.clearMode=!controls.clearMode;controls.pullMode=false;scene.setClearMode(controls.clearMode);game.paused=controls.clearMode;if(controls.clearMode)toast("없앨 색의 블록을 터치하세요 · 드래그로 이동");return;}
        if(which==3){controls.pullMode=!controls.pullMode;controls.clearMode=false;scene.setClearMode(false);game.paused=false;if(controls.pullMode)toast("꺼낼 컨테이너를 선택하세요.");return;}
        controls.clearMode=controls.pullMode=false;scene.setClearMode(false);game.paused=false;
        boolean used=which==0?game.extraSlot():game.shuffle();if(used)consume(which);else toast(which==0?"작업 공간은 최대 6칸입니다.":"섞을 컨테이너가 부족해요.");
    }
    private void consume(int which){save.consumeBooster(which);if(controls!=null)controls.invalidate();effect("power");toast("부스터를 사용했어요.");}
    private void showPause(){
        if(game==null||game.status!=NativeGame.PLAYING)return;
        final NativeGame owner=game;synchronized(owner){if(controls!=null)controls.clearMode=false;if(scene!=null)scene.setClearMode(false);owner.paused=true;}
        dismiss();dialog=new GameSheet(this,"","","",true);dialog.onClose(()->owner.paused=controls!=null&&controls.clearMode);
        dialog.icons(new int[]{4,6,7,8},new String[]{"계속 플레이","사운드와 진동 설정","다시 시작","메인"},new Runnable[]{()->{dismiss();owner.paused=false;},this::showSettings,this::confirmRestart,this::confirmLeave});dialog.show();
    }

    private void confirmRestart(){
        if(game==null)return;final NativeGame owner=game;owner.paused=true;dismiss();
        dialog=new GameSheet(this,"","다시 도전할까요?","현재 스테이지의 블록과 슬롯이 처음으로 돌아가요.",true);
        dialog.onClose(this::showPause);dialog.primary("처음부터 다시 시작",()->startLevel(owner.level.id));dialog.secondary("이어서 플레이",()->{dismiss();owner.paused=false;});dialog.show();
    }
    private void confirmLeave(){
        if(game==null){showHome();return;}final NativeGame owner=game;owner.paused=true;dismiss();
        dialog=new GameSheet(this,"","메인으로 돌아갈까요?","완료하지 않은 스테이지는 다시 시작해야 해요.",true);
        dialog.onClose(()->owner.paused=controls!=null&&controls.clearMode);dialog.primary("계속 플레이",()->{dismiss();owner.paused=controls!=null&&controls.clearMode;});dialog.secondary("메인 화면으로",this::showHome);dialog.show();
    }
    private void showResult(){
        if(game==null||resultShown)return;resultShown=true;boolean won=game.status==NativeGame.WON;int id=game.level.id;
        int reward=won&&!inspect?save.win(game):0;dismiss();
        dialog=new GameSheet(this,won?"MISSION COMPLETE":"ONE MORE TRY",won?"작업 완료":"다시 도전",won?(id==LevelLoader.TOTAL?"새 현장이 준비되면 다시 만나요.":"다음 현장이 열렸어요."):"슬롯이 가득 차서 로봇이 출발하지 못하고 있어요.",false);
        if(won){
            dialog.card("✦","STAGE "+String.format(Locale.KOREA,"%03d",id)+" 완료",String.format(Locale.KOREA,"%d초 · %d회 출격 · %d개 블록 회수",(int)game.time,game.moves,game.collected),LIME);
            dialog.card("◈","코인 +"+reward,"다음 미션을 위한 부스터를 준비해보세요.",0xfff1d38b);
            dialog.primary(id==LevelLoader.TOTAL?"메인":"다음 현장",()->{if(id==LevelLoader.TOTAL)showHome();else startLevel(id+1);});
            dialog.secondary("메인 화면으로",this::showHome);
        }else{
            dialog.card("↗","아직 기회가 있어요","슬롯 추가나 컬러 제거로 길을 열거나\n다른 순서로 다시 도전해보세요.",0xffb7dbe2);
            dialog.primary("다시 도전하기",()->startLevel(id));
            dialog.secondary("메인 화면으로",this::showHome);
        }dialog.show();
    }
    private void showStages(int page){
        if(game!=null)game.listener=null;game=null;rebuild("stages");page=Math.max(0,Math.min((save.unlocked-1)/20,page));final int chapter=page;
        content.setPadding(dp(18),dp(14),dp(18),dp(14));content.addView(text("스테이지 선택",25,INK,true),new LinearLayout.LayoutParams(-1,dp(48)));
        LinearLayout nav=new LinearLayout(this);nav.setGravity(Gravity.CENTER_VERTICAL);
        Button prev=button("‹",false,()->showStages(chapter-1));prev.setEnabled(page>0);nav.addView(prev,new LinearLayout.LayoutParams(dp(48),dp(42)));
        nav.addView(text("구역 "+(page+1),13,MUTED,true),new LinearLayout.LayoutParams(0,dp(46),1));
        Button next=button("›",false,()->showStages(chapter+1));next.setEnabled(page<(save.unlocked-1)/20);nav.addView(next,new LinearLayout.LayoutParams(dp(48),dp(42)));content.addView(nav);gap(content,12);
        for(int row=0;row<5;row++){
            LinearLayout line=new LinearLayout(this);
            for(int column=0;column<4;column++){
                final int id=page*20+row*4+column+1;boolean unlocked=id<=save.unlocked;
                String label=unlocked?String.format(Locale.KOREA,"%03d\n%s",id,save.cleared(id)?"✓ "+(int)save.record(id)+"초":"플레이"):"?";
                Button b=button(label,save.cleared(id),()->startLevel(id));b.setEnabled(unlocked);b.setAlpha(unlocked?1:.45f);b.setTextSize(12);
                LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,-1,1);if(column>0)p.leftMargin=dp(7);line.addView(b,p);
            }
            LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,0,1);p.bottomMargin=dp(8);content.addView(line,p);
        }
        content.addView(button("← 메인 화면",false,this::showHome),new LinearLayout.LayoutParams(-1,dp(48)));
    }
    private void showSettings(){
        final NativeGame owner=game;if(owner!=null)owner.paused=true;dismiss();
        dialog=new GameSheet(this,"","설정","",true);
        final GameSheet settings=dialog;
        settings.secretGesture(()->{save.unlimitedItems=!save.unlimitedItems;save.persist();if(controls!=null)controls.invalidate();settings.notifyMode("아이템 무제한 모드 "+(save.unlimitedItems?"On":"Off"));});
        Runnable done=()->{setupAudio();if(owner!=null&&owner.status==NativeGame.PLAYING)showPause();};dialog.onClose(done);
        dialog.toggle("배경음악","",save.music,v->{save.music=v;save.persist();if(!v&&audio!=null)audio.stopMusic();});
        dialog.toggle("효과음","",save.sound,v->{save.sound=v;save.persist();});
        dialog.toggle("진동","",save.vibration,v->{save.vibration=v;save.persist();});
        dialog.gap(4);
        dialog.slider("음악 볼륨",save.musicVolume,v->{save.musicVolume=v;save.persist();},this::setupAudio);
        dialog.slider("효과음 볼륨",save.soundVolume,v->{save.soundVolume=v;save.persist();},()->{setupAudio();effect("click");});
        dialog.gap(8);dialog.primary("설정 완료",()->{dismiss();done.run();});
        dialog.secondary("진행 데이터 관리",this::confirmReset);dialog.show();
    }
    private void confirmReset(){
        dismiss();dialog=new GameSheet(this,"","진행을 초기화할까요?","이 작업은 되돌릴 수 없어요.",true);
        dialog.card("!","기록과 아이템이 초기화돼요","완료한 스테이지, 코인, 부스터와\n설정이 처음 상태로 돌아갑니다.",0xffefbbab);
        dialog.onClose(this::showSettings);dialog.primary("기록 유지하기",this::showSettings);dialog.secondary("모든 진행 초기화",()->{save.reset();setupAudio();showHome();toast("진행 데이터를 초기화했어요.");});dialog.show();
    }
    private void showRobots(){
        dismiss();dialog=new GameSheet(this,"","작업 로봇","컨테이너 색 본체에 흰 포인트로 구분합니다.",true);
        dialog.card("●","운반 로봇","몸·머리·다리는 담당 컨테이너 색\n손·얼굴 플레이트만 흰색입니다.",0xff83c6a3);
        dialog.primary("닫기",this::dismiss);dialog.show();
    }
    private void showItems(){
        dismiss();dialog=new GameSheet(this,"","부스터 보관함","보유 코인  ◈ "+save.coins+"   ·   전략에 작은 도움을 더해요.",true);
        String[] names={"슬롯 추가","컬러 제거","셔플","꺼내기"};int[] icons={0,1,2,3};
        String[] descriptions={"빈 슬롯 1개를 추가해요. 최대 6개.","선택한 색을 모두 회수해요. 스테이지당 1회.","남은 컨테이너의 순서를 다시 섞어요.","뒤쪽에 잠긴 컨테이너 하나를 꺼내요."};int[] prices={100,200,80,120};
        for(int i=0;i<4;i++){final int item=i;dialog.card(icons[i],names[i]+"  ·  "+save.boosterCount(i)+(save.unlimitedItems?"":"개"),descriptions[i],i==1?0xfff1d38b:0xffd8e4ce);
            boolean canBuy=!save.unlimitedItems&&save.coins>=prices[i];TextView buy=GameSheet.action(this,save.unlimitedItems?"∞  무제한 사용 중":"◈ "+prices[i]+"  ·  1개 구매",canBuy,()->confirmPurchase(item,names[item],prices[item]));buy.setEnabled(canBuy);buy.setAlpha(canBuy||save.unlimitedItems?1:.45f);dialog.body.addView(buy,new LinearLayout.LayoutParams(-1,dp(46)));dialog.gap(14);}
        dialog.note("부스터는 게임 화면 하단에서 사용할 수 있어요.");dialog.primary("준비 완료",this::dismiss);dialog.show();
    }
    private void confirmPurchase(int item,String name,int price){
        dismiss();dialog=new GameSheet(this,"",name+" 구매","코인을 사용해 부스터 1개를 보관함에 추가해요.",true);
        dialog.card("◈",price+" 코인", "구매 후 잔액  "+(save.coins-price)+" 코인",0xfff1d38b);dialog.onClose(this::showItems);
        dialog.primary("구매 확정",()->{if(save.coins>=price){save.coins-=price;save.boosters[item]++;save.persist();showHome();showItems();toast(name+" 1개가 보관함에 도착했어요.");}else{showItems();toast("코인이 부족해요.");}});dialog.secondary("돌아가기",this::showItems);dialog.show();
    }
    private void setupAudio(){if(audio==null)audio=new GameAudio(this);}
    private void effect(String type){
        long now=SystemClock.elapsedRealtime();if(type.equals("collect")&&now-lastSound<80)return;
        if(save.sound&&audio!=null&&!type.equals("mini")){audio.effect(type,save.soundVolume);lastSound=now;}
        if(save.vibration&&(type.equals("mega")||type.equals("power")||type.equals("lost"))){Vibrator v=(Vibrator)getSystemService(VIBRATOR_SERVICE);if(v!=null&&v.hasVibrator())v.vibrate(VibrationEffect.createOneShot(type.equals("mega")?100:25,VibrationEffect.DEFAULT_AMPLITUDE));}
    }
    private void toast(String value){
        if(notice!=null){notice.animate().cancel();root.removeView(notice);}
        final TextView message=GameSheet.label(this,value,12,LIME,true);notice=message;message.setGravity(Gravity.CENTER);message.setPadding(dp(18),dp(14),dp(18),dp(14));message.setBackground(GameSheet.shape(this,INK,18));message.setElevation(dp(10));
        FrameLayout.LayoutParams p=new FrameLayout.LayoutParams(-1,-2,Gravity.BOTTOM);p.setMargins(dp(20),0,dp(20),dp(screen.equals("game")?300:24));root.addView(message,p);message.setAlpha(0);message.setTranslationY(dp(12));message.animate().alpha(1).translationY(0).setDuration(180).start();
        message.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
        handler.postDelayed(()->{if(notice==message){message.animate().alpha(0).setDuration(180).withEndAction(()->{root.removeView(message);if(notice==message)notice=null;}).start();}},2800);
    }
    @Override protected void onPause(){foreground=false;if(game!=null&&game.status==NativeGame.PLAYING){synchronized(game){game.paused=true;}if(dialog==null||!dialog.isShowing())showPause();}if(scene!=null)scene.onPause();if(audio!=null)audio.stop();super.onPause();}
    @Override protected void onResume(){super.onResume();foreground=true;if(audio!=null)audio.resume();if(scene!=null)scene.onResume();if(Build.VERSION.SDK_INT>=30){WindowInsetsController controller=getWindow().getInsetsController();if(controller!=null){controller.hide(WindowInsets.Type.statusBars());controller.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);}}}
    private void handleBack(){if(dialog!=null&&dialog.isShowing()){if(game!=null&&!getPreferences(MODE_PRIVATE).getBoolean("constructionTutorial",false))return;if(game!=null&&game.status!=NativeGame.PLAYING)showHome();else dialog.cancel();}else if(game!=null&&game.status==NativeGame.PLAYING)showPause();else if(!screen.equals("home"))showHome();else finish();}
    @Override public boolean dispatchKeyEvent(KeyEvent event){if(Build.VERSION.SDK_INT<33&&event.getKeyCode()==KeyEvent.KEYCODE_BACK){if(event.getAction()==KeyEvent.ACTION_UP)handleBack();return true;}return super.dispatchKeyEvent(event);}
    @Override protected void onDestroy(){handler.removeCallbacksAndMessages(null);if(scene!=null)scene.onPause();if(audio!=null)audio.release();super.onDestroy();}
}
