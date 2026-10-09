package com.bossxor.robotflow;

import android.app.*;
import android.content.*;
import android.graphics.Typeface;
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
    private int tuneNote;
    private final Runnable heartbeat=new Runnable(){@Override public void run(){
        if(foreground){if(header!=null)header.invalidate();if(controls!=null)controls.invalidate();}
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
        if(BuildConfig.DEBUG&&getIntent().hasExtra("inspect_level")){inspect=true;startLevel(getIntent().getIntExtra("inspect_level",1));}
        if(Build.VERSION.SDK_INT>=33)getOnBackInvokedDispatcher().registerOnBackInvokedCallback(OnBackInvokedDispatcher.PRIORITY_DEFAULT,this::handleBack);
    }
    private int dp(float n){return Math.round(n*getResources().getDisplayMetrics().density);}
    private GradientDrawable background(int color,float radius){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(radius));return d;}
    private TextView text(String value,int size,int color,boolean bold){TextView t=new TextView(this);t.setText(value);t.setTextSize(size);t.setTextColor(color);t.setTypeface(Typeface.create("sans-serif",bold?Typeface.BOLD:Typeface.NORMAL));t.setGravity(Gravity.CENTER);t.setIncludeFontPadding(false);return t;}
    private Button button(String label,boolean primary,Runnable action){Button b=new Button(this);b.setText(label);b.setAllCaps(false);b.setTextSize(14);b.setTextColor(primary?LIME:INK);b.setTypeface(Typeface.DEFAULT,Typeface.BOLD);b.setBackground(GameSheet.ripple(this,primary?INK:0xffefe2d1,16));b.setStateListAnimator(null);b.setMinHeight(0);b.setMinimumHeight(0);b.setPadding(dp(12),0,dp(12),0);b.setOnClickListener(v->{effect("click");action.run();});return b;}
    private void gap(LinearLayout layout,int height){layout.addView(new View(this),new LinearLayout.LayoutParams(1,dp(height)));}
    private void dismiss(){if(dialog!=null){dialog.setOnDismissListener(null);dialog.dismiss();dialog=null;}}
    private void rebuild(String mode){
        dismiss();if(scene!=null)scene.onPause();scene=null;header=controls=null;
        screen=mode;root.removeAllViews();notice=null;content=new LinearLayout(this);content.setOrientation(LinearLayout.VERTICAL);content.setBackgroundColor(BACK);root.addView(content,new FrameLayout.LayoutParams(-1,-1));root.requestApplyInsets();
    }
    private void showHome(){
        if(game!=null){synchronized(game){game.paused=true;}game.listener=null;}
        game=null;inspect=false;getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);rebuild("home");
        LinearLayout brand=new LinearLayout(this);brand.setGravity(Gravity.CENTER_VERTICAL);brand.setPadding(dp(20),dp(12),dp(20),dp(8));
        ImageView image=new ImageView(this);image.setImageResource(R.drawable.robot_flow_icon);image.setBackground(background(INK,12));image.setClipToOutline(true);brand.addView(image,new LinearLayout.LayoutParams(dp(40),dp(40)));
        brand.addView(text("ROBOT FLOW",22,INK,true),new LinearLayout.LayoutParams(0,dp(46),1));brand.addView(text("◈ "+save.coins,12,MUTED,true));content.addView(brand,new LinearLayout.LayoutParams(-1,dp(72)));
        try{scene=new NativeScene(this,new NativeGame(LevelLoader.load(getAssets(),1)),true);content.addView(scene,new LinearLayout.LayoutParams(-1,0,1));}catch(Exception error){toast(error.getMessage());}
        LinearLayout bottom=new LinearLayout(this);bottom.setOrientation(LinearLayout.VERTICAL);bottom.setPadding(dp(22),dp(12),dp(22),dp(16));
        bottom.addView(text(String.format(Locale.KOREA,"STAGE %03d",save.current),15,MUTED,true),new LinearLayout.LayoutParams(-1,dp(28)));
        gap(bottom,8);bottom.addView(button("플레이",true,()->startLevel(save.current)),new LinearLayout.LayoutParams(-1,dp(54)));gap(bottom,12);
        LinearLayout links=new LinearLayout(this);String[] labels={"스테이지","로봇","아이템","설정"};
        int[] navIcons={9,11,0,10};
        for(int i=0;i<4;i++){final int n=i;GameIcons icon=new GameIcons(this,navIcons[i],labels[i],()->{effect("click");if(n==0)showStages((save.current-1)/20);else if(n==1)showRobots();else if(n==2)showItems();else showSettings();});LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,dp(56),1);if(i>0)lp.leftMargin=dp(8);links.addView(icon,lp);}

        bottom.addView(links);content.addView(bottom,new LinearLayout.LayoutParams(-1,dp(188)));
    }
    private void startLevel(int id){
        if(id<1||id>LevelLoader.TOTAL||(!inspect&&id>save.unlocked))return;
        try {
            NativeGame next=new NativeGame(LevelLoader.load(getAssets(),id));
            if(game!=null)game.listener=null;rebuild("game");game=next;resultShown=false;if(!inspect){save.current=id;save.persist();}
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            final NativeGame owner=game;
            game.listener=(type,color,x,y)->handler.post(()->{if(game==owner){effect(type);if(type.equals("won")||type.equals("lost"))showResult();}});
            header=new NativeHud(this,game,save,true,this::action);content.addView(header,new LinearLayout.LayoutParams(-1,dp(86)));
            scene=new NativeScene(this,game,false);content.addView(scene,new LinearLayout.LayoutParams(-1,0,1));
            controls=new NativeHud(this,game,save,false,this::action);
            float available=(root.getHeight()>0?root.getHeight()-root.getPaddingBottom():getResources().getDisplayMetrics().heightPixels)/getResources().getDisplayMetrics().density;
            float screenWidth=getResources().getDisplayMetrics().widthPixels/getResources().getDisplayMetrics().density;
            int controlHeight=Math.round(GameLayout.controlsHeight(screenWidth,available));
            content.addView(controls,new LinearLayout.LayoutParams(-1,dp(controlHeight)));
            if(!getPreferences(MODE_PRIVATE).getBoolean("constructionTutorial",false)){
                synchronized(game){game.paused=true;}
                dialog=new GameSheet(this,"","현장에 오신 걸 환영해요","",false);
                dialog.card("01","같은 색을 선택하세요","각 열의 맨 앞 컨테이너를 선택하세요.\n같은 색 로봇이 블록을 회수합니다.",0xffb7dbe2);
                dialog.card("02","트럭으로 짐을 운반해요","컨테이너는 동시에 5개까지 작업합니다.\n공용 트럭은 짐이 차면 출발합니다.",0xfff1d38b);
                dialog.card("03","중장비 로봇을 불러요","게이지를 채우고 컨테이너를 선택하면\n궤도 로봇이 최대 8개를 회수합니다.",LIME);
                dialog.primary("좋아요, 출격!  →",()->{dismiss();getPreferences(MODE_PRIVATE).edit().putBoolean("constructionTutorial",true).apply();synchronized(owner){owner.paused=false;}});dialog.show();
            }
        }catch(Exception error){showHome();toast("레벨 로딩 오류: "+error.getMessage());}
    }
    private void action(String key,int value){
        if(game==null)return;
        if(key.equals("home")){confirmLeave();return;}
        if(key.equals("pause")){showPause();return;}
        synchronized(game){
            if(key.equals("speed")){scene.speed=scene.speed==1?2:1;header.speed=scene.speed;header.invalidate();return;}
            if(key.equals("queue-page")){controls.page=Math.max(0,controls.page+value);controls.invalidate();return;}
            if(key.equals("capsule")){
                if(controls.pullMode){if(save.hasBooster(3)&&game.pullOut(value)){save.consumeBooster(3);controls.pullMode=false;effect("select");}else toast("뒤쪽에 잠긴 컨테이너를 선택하세요.");}
                else if(!game.select(value))toast(game.slots.contains(null)?"이 열의 맨 앞 컨테이너를 선택하세요.":"작업 중인 컨테이너가 끝나면 선택할 수 있어요.");
            }else if(key.equals("booster"))useBooster(value);
            if(controls!=null)controls.invalidate();
        }
    }
    private void useBooster(int which){
        if(!save.hasBooster(which)){toast("보유 아이템이 없어요. 메인 아이템 메뉴에서 구매할 수 있어요.");return;}
        if(which==1){
            ArrayList<Integer> colors=new ArrayList<>();for(int color=0;color<NativeGame.COLORS.length;color++){boolean found=false;for(NativeGame.Block b:game.blocks)if(!b.removed&&b.color==color)found=true;if(found)colors.add(color);}
            final NativeGame owner=game;owner.paused=true;dismiss();dialog=new GameSheet(this,"","어떤 색을 회수할까요?",save.unlimitedItems?"선택한 색의 블록을 모두 회수해요. 무제한 모드 적용 중.":"선택한 색의 블록을 모두 회수해요. 스테이지당 1회.",true);dialog.onClose(()->owner.paused=false);
            for(int color:colors){final int selected=color;dialog.secondary("●  "+NativeGame.COLOR_NAMES[color],()->{dismiss();owner.paused=false;if(game==owner&&owner.clearColor(selected,save.unlimitedItems))consume(1);else toast("컬러 제거는 스테이지당 1회 사용할 수 있어요.");});}
            dialog.secondary("돌아가기",()->{dismiss();owner.paused=false;});dialog.show();return;
        }
        if(which==3){controls.pullMode=!controls.pullMode;controls.invalidate();if(controls.pullMode)toast("잠긴 컨테이너를 선택하세요. 다시 누르면 취소돼요.");return;}
        final NativeGame owner=game;owner.paused=true;dismiss();
        dialog=new GameSheet(this,"",which==0?"슬롯을 하나 더 열까요?":"순서를 바꿔볼까요?",save.unlimitedItems?"아이템 무제한 모드 · 보유 수량은 줄어들지 않아요.":"보유 부스터 "+save.boosters[which]+"개 · 사용하면 1개가 차감돼요.",true);
        dialog.card(which==0?0:2,which==0?"출격 공간 +1":"새로운 출격 순서",which==0?"활성 슬롯을 1개 추가해요. 최대 6개까지\n열 수 있고 이번 스테이지에만 적용돼요.":"남은 컨테이너의 순서를 섞어요.\n현재 작업 중인 로봇은 계속 회수해요.",LIME);
        dialog.onClose(()->owner.paused=false);dialog.primary("부스터 사용",()->{dismiss();owner.paused=false;if(game!=owner)return;boolean used=which==0?owner.extraSlot():owner.shuffle();if(used)consume(which);else toast(which==0?"슬롯은 최대 6개입니다.":"섞을 컨테이너가 부족해요.");});dialog.secondary("돌아가기",()->{dismiss();owner.paused=false;});dialog.show();
    }
    private void consume(int which){save.consumeBooster(which);if(controls!=null)controls.invalidate();effect("power");toast("부스터를 사용했어요.");}
    private void showPause(){
        if(game==null||game.status!=NativeGame.PLAYING)return;
        final NativeGame owner=game;synchronized(owner){owner.paused=true;}
        dismiss();dialog=new GameSheet(this,"","","",true);dialog.onClose(()->owner.paused=false);
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
        dialog.onClose(()->owner.paused=false);dialog.primary("계속 플레이",()->{dismiss();owner.paused=false;});dialog.secondary("메인 화면으로",this::showHome);dialog.show();
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
            dialog.secondary("부스터로 이어가기",()->{dismiss();resultShown=false;toast("하단에서 사용할 부스터를 선택하세요.");});
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
        dismiss();dialog=new GameSheet(this,"","작업 로봇","7가지 팀 색상으로 출격합니다.",true);
        dialog.card("●","운반원 · COURIER","안전모와 작은 부츠 · 한 번에 1개\n양손으로 짐을 들어 트럭에 싣습니다.",0xff83c6a3);
        dialog.card("▰","중장비 · BUILDER","궤도와 리프트 · 최대 8개\n게이지가 차면 다음 출격에 합류합니다.",LIME);
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
