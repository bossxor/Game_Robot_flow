package com.bossxor.robotflow;

import android.content.Context;
import android.opengl.*;
import android.os.SystemClock;
import java.nio.*;
import java.util.*;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/** Real 3D meshes, a perspective camera, lighting and depth testing on GLES 2. */
public final class NativeScene extends GLSurfaceView implements GLSurfaceView.Renderer {
    public volatile NativeGame game;
    public volatile int speed = 1;
    private static final int BASE_SIMULATION_RATE = 2;
    public volatile boolean preview;
    private int program, position, normal, mvpUniform, modelUniform, colorUniform, normalUniform, tintAttribute;
    private final float[] projection = new float[16], view = new float[16], vp = new float[16], model = new float[16], mvp = new float[16], inverse = new float[16], normalMatrix = new float[16];
    private Mesh rounded, sphere, cube, tileMesh;
    private final Map<Integer,TileChunk> chunks = new HashMap<>();
    private int width, height;
    private long previous;
    private double clock;
    private float portalZ;
    public interface BlockTap {void tap(int color);}
    public BlockTap blockTap;
    public boolean clearMode;
    private long perfStarted,perfCost;private int perfFrames;
    private final float[] ink = rgb(0xff353b43), cream = rgb(0xfffff2d8), lime = rgb(0xff89c5a3);
    private static final String VERTEX = "attribute vec3 aPosition; attribute vec3 aNormal; attribute vec4 aTint; varying vec4 vTint; uniform mat4 uMvp; uniform mat4 uModel; uniform mat4 uNormal; varying vec3 vNormal; varying vec3 vWorld; void main(){ vTint=aTint; vec4 p=uModel*vec4(aPosition,1.0); vWorld=p.xyz; vNormal=normalize(mat3(uNormal)*aNormal); gl_Position=uMvp*vec4(aPosition,1.0); }";
    private static final String FRAGMENT = "precision mediump float; varying vec3 vNormal; varying vec3 vWorld; uniform vec4 uColor; varying vec4 vTint; void main(){ vec3 n=normalize(vNormal); vec3 l=normalize(vec3(-0.5,1.0,0.75)); float diffuse=max(dot(n,l),0.0); float rim=pow(1.0-max(n.y,0.0),2.0)*0.055; vec3 color=uColor.rgb*vTint.rgb*(0.60+0.40*diffuse)+rim; gl_FragColor=vec4(color,uColor.a); }";
    public NativeScene(Context context, NativeGame game, boolean preview) {
        super(context); this.game = game; this.preview = preview;
        setEGLContextClientVersion(2); setEGLConfigChooser(8, 8, 8, 8, 24, 0);
        setPreserveEGLContextOnPause(true); setRenderer(this); setRenderMode(RENDERMODE_CONTINUOUSLY);
        setContentDescription("입체 블록 필드와 움직이는 3D 로봇");
        setOnTouchListener((v,e)->{if(e.getAction()==android.view.MotionEvent.ACTION_UP&&clearMode&&blockTap!=null){int color=pickColor(e.getX(),e.getY());if(color>=0){performClick();blockTap.tap(color);}return true;}return clearMode;});
    }
    @Override public boolean performClick(){super.performClick();return true;}
    private int pickColor(float x,float y){NativeGame g=game;if(g==null)return -1;synchronized(g){float[] point=ground(x/Math.max(1,width)*2-1,1-y/Math.max(1,height)*2);int bx=Math.round(point[0]+(g.level.width-1)/2f),by=Math.round(point[2]+(g.level.height-1)/2f);for(NativeGame.Block b:g.blocks)if(!b.removed&&b.x==bx&&b.y==by)return b.color;}return -1;}
    private float[] ground(float nx,float ny){float[] inv=new float[16],a=new float[4],b=new float[4];Matrix.invertM(inv,0,vp,0);Matrix.multiplyMV(a,0,inv,0,new float[]{nx,ny,-1,1},0);Matrix.multiplyMV(b,0,inv,0,new float[]{nx,ny,1,1},0);for(int i=0;i<3;i++){a[i]/=a[3];b[i]/=b[3];}float t=(.22f-a[1])/(b[1]-a[1]);return new float[]{a[0]+t*(b[0]-a[0]),.22f,a[2]+t*(b[2]-a[2])};}
    private static float[] rgb(int color) { return new float[]{((color >> 16) & 255) / 255f, ((color >> 8) & 255) / 255f, (color & 255) / 255f, 1}; }
    private static float[] tint(float[] color, float amount) { return new float[]{color[0] * amount, color[1] * amount, color[2] * amount, color[3]}; }
    private int shader(int kind, String code) {
        int id = GLES20.glCreateShader(kind); GLES20.glShaderSource(id, code); GLES20.glCompileShader(id);
        int[] ok = new int[1]; GLES20.glGetShaderiv(id, GLES20.GL_COMPILE_STATUS, ok, 0);
        if (ok[0] == 0) throw new IllegalStateException("RoboRelay shader: " + GLES20.glGetShaderInfoLog(id));
        return id;
    }
    @Override public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        program = GLES20.glCreateProgram(); int vertex = shader(GLES20.GL_VERTEX_SHADER, VERTEX), fragment = shader(GLES20.GL_FRAGMENT_SHADER, FRAGMENT);
        GLES20.glAttachShader(program, vertex); GLES20.glAttachShader(program, fragment); GLES20.glLinkProgram(program);
        int[] ok = new int[1]; GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, ok, 0);
        if (ok[0] == 0) throw new IllegalStateException(GLES20.glGetProgramInfoLog(program));
        GLES20.glDeleteShader(vertex); GLES20.glDeleteShader(fragment);
        position = GLES20.glGetAttribLocation(program, "aPosition"); normal = GLES20.glGetAttribLocation(program, "aNormal"); tintAttribute=GLES20.glGetAttribLocation(program,"aTint");
        mvpUniform = GLES20.glGetUniformLocation(program, "uMvp"); modelUniform = GLES20.glGetUniformLocation(program, "uModel");
        colorUniform = GLES20.glGetUniformLocation(program, "uColor"); normalUniform = GLES20.glGetUniformLocation(program, "uNormal");
        rounded = boxMesh(.20f, 5); cube = boxMesh(0, 1); tileMesh=boxMesh(.05f,2); sphere = sphereMesh(preview?16:10, preview?24:16); chunks.clear();
        GLES20.glEnable(GLES20.GL_DEPTH_TEST); GLES20.glDepthFunc(GLES20.GL_LEQUAL);
        GLES20.glEnable(GLES20.GL_BLEND); GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);
        previous = SystemClock.elapsedRealtimeNanos();
    }
    @Override public void onSurfaceChanged(GL10 gl, int width, int height) { this.width = width; this.height = Math.max(height, 1); GLES20.glViewport(0, 0, width, height); }
    @Override public void onDrawFrame(GL10 gl) {
        long frameStart=SystemClock.elapsedRealtimeNanos();long now = frameStart; double dt = Math.max(0, Math.min(.04, (now - previous) / 1e9)); previous = now;
        float[] sky=preview?rgb(0xfff7eddf):rgb(new int[]{0xfff1dfbd,0xfff3d5c2,0xffc5d1de}[game==null?0:game.level.theme]);GLES20.glClearColor(sky[0],sky[1],sky[2],1); GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
        GLES20.glUseProgram(program); GLES20.glEnableVertexAttribArray(position); GLES20.glEnableVertexAttribArray(normal);
        NativeGame g = game; if (g == null || width == 0) return;
        synchronized (g) {
            int actualSpeed=g.finishing?5:speed;
            if(preview||!g.paused)clock+=dt*(preview?1:BASE_SIMULATION_RATE*actualSpeed);
            if (preview) { camera(8, 7, true); drawPreview(); }
            else {
                for (int i = 0; i < BASE_SIMULATION_RATE * actualSpeed; i++) g.update(dt);
                camera(g.level.width, g.level.height, false);
                drawBoard(g);
                if(BuildConfig.DEBUG){perfCost+=SystemClock.elapsedRealtimeNanos()-frameStart;perfFrames++;if(now-perfStarted>10_000_000_000L){android.util.Log.d("RoboRelayPerf","render CPU mean="+String.format(java.util.Locale.US,"%.2f",perfCost/1e6/Math.max(1,perfFrames))+"ms bots="+g.bots.size()+" tiles="+g.remainingBlocks()+" GL error="+GLES20.glGetError());perfStarted=now;perfFrames=0;perfCost=0;}}

            }
        }
    }
    private void camera(float sceneWidth, float sceneDepth, boolean home) {
        float aspect = width / (float)height, fov = 38;
        if(!home){
            float half=Math.max(sceneWidth/.762f/2,sceneDepth*.62f*aspect);
            Matrix.orthoM(projection,0,-half,half,-half/aspect,half/aspect,.1f,300);
            Matrix.setLookAtM(view,0,0,sceneWidth*1.875f,sceneWidth*1.25f,0,0,sceneWidth*.146875f,0,1,0);Matrix.multiplyMM(vp,0,projection,0,view,0);portalZ=ground(0,-.92f)[2];if(game!=null)game.portalSpan=half*2;return;
        }
        float halfWidth=4.8f;Matrix.orthoM(projection,0,-halfWidth,halfWidth,-halfWidth/aspect,halfWidth/aspect,.1f,100);
        Matrix.setLookAtM(view,0,0,13,17,0,1.0f,0,0,1,0);Matrix.multiplyMM(vp,0,projection,0,view,0);

    }
    private void draw(Mesh mesh, float x, float y, float z, float sx, float sy, float sz, float rotation, float[] color) {
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER,0);
        GLES20.glDisableVertexAttribArray(tintAttribute);GLES20.glVertexAttrib4f(tintAttribute,1,1,1,1);
        Matrix.setIdentityM(model, 0); Matrix.translateM(model, 0, x, y, z); if (rotation != 0) Matrix.rotateM(model, 0, rotation, 0, 1, 0); Matrix.scaleM(model, 0, sx, sy, sz);
        Matrix.multiplyMM(mvp, 0, vp, 0, model, 0); Matrix.invertM(inverse, 0, model, 0); Matrix.transposeM(normalMatrix, 0, inverse, 0);
        GLES20.glUniformMatrix4fv(mvpUniform, 1, false, mvp, 0); GLES20.glUniformMatrix4fv(modelUniform, 1, false, model, 0); GLES20.glUniformMatrix4fv(normalUniform, 1, false, normalMatrix, 0);
        GLES20.glUniform4fv(colorUniform, 1, color, 0);
        mesh.buffer.position(0); GLES20.glVertexAttribPointer(position, 3, GLES20.GL_FLOAT, false, 24, mesh.buffer);
        mesh.buffer.position(3); GLES20.glVertexAttribPointer(normal, 3, GLES20.GL_FLOAT, false, 24, mesh.buffer);
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, mesh.count);
    }
    private void shadow(float x, float z, float size, float alpha) { draw(sphere, x + .08f, .080f, z + .10f, size, .016f, size * .78f, 0, new float[]{.13f,.26f,.23f,alpha}); }
    private final class TileChunk {
        FloatBuffer vertices; int count,hash=-1,vbo;
        NativeGame owner;
        final List<NativeGame.Block> members=new ArrayList<>();
        void refresh(NativeGame g,int chunk){
            if(owner!=g){owner=g;members.clear();hash=-1;for(NativeGame.Block b:g.blocks)if((b.y/8)*8+b.x/8==chunk)members.add(b);}
            int expected=1;for(NativeGame.Block block:members)expected=31*expected+(block.removed?0:block.color+1);
            if(expected==hash)return;hash=expected;
            if(vertices==null)vertices=ByteBuffer.allocateDirect(64*tileMesh.count*10*4).order(ByteOrder.nativeOrder()).asFloatBuffer();vertices.clear();
            float cx=(g.level.width-1)/2f,cz=(g.level.height-1)/2f;
            for(NativeGame.Block block:members)if(!block.removed){
                float[] col=rgb(NativeGame.COLORS[block.color]);
                for(int n=0;n<tileMesh.count;n++){
                    int k=n*6;float px=tileMesh.buffer.get(k),py=tileMesh.buffer.get(k+1),pz=tileMesh.buffer.get(k+2);
                    vertices.put(block.x-cx+px*.94f).put(.20f+py*.08f).put(block.y-cz+pz*.94f);
                    vertices.put(tileMesh.buffer.get(k+3)).put(tileMesh.buffer.get(k+4)).put(tileMesh.buffer.get(k+5));
                    float shade=py>0?1.08f:.91f;vertices.put(col[0]*shade).put(col[1]*shade).put(col[2]*shade).put(1);
                }
            }count=vertices.position()/10;vertices.position(0);
            if(vbo==0){int[] ids=new int[1];GLES20.glGenBuffers(1,ids,0);vbo=ids[0];}
            GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER,vbo);GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER,count*40,vertices,GLES20.GL_STATIC_DRAW);GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER,0);
        }
        void render(){
            if(count==0)return;
            Matrix.setIdentityM(model,0);GLES20.glUniformMatrix4fv(mvpUniform,1,false,vp,0);GLES20.glUniformMatrix4fv(modelUniform,1,false,model,0);GLES20.glUniformMatrix4fv(normalUniform,1,false,model,0);GLES20.glUniform4f(colorUniform,1,1,1,1);
            GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER,vbo);GLES20.glVertexAttribPointer(position,3,GLES20.GL_FLOAT,false,40,0);
            GLES20.glVertexAttribPointer(normal,3,GLES20.GL_FLOAT,false,40,12);
            GLES20.glEnableVertexAttribArray(tintAttribute);GLES20.glVertexAttribPointer(tintAttribute,4,GLES20.GL_FLOAT,false,40,24);GLES20.glDrawArrays(GLES20.GL_TRIANGLES,0,count);GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER,0);
        }
    }
    private void drawBoard(NativeGame g) {
        float cx=(g.level.width-1)/2f,cz=(g.level.height-1)/2f;
        float border=g.level.width*.1875f,scale=g.level.width/32f;
        draw(rounded,0,-.25f,3*scale,g.level.width*1.25f,.5f,g.level.height+15*scale,0,rgb(new int[]{0xffecd4ab,0xffe9c3a6,0xffaab9c8}[g.level.theme]));
        draw(rounded,0,.0f,0,g.level.width+border+.4f,.24f,g.level.height+border+.4f,0,rgb(0xffbda88b));
        draw(rounded,0,.08f,0,g.level.width+border,.18f,g.level.height+border,0,rgb(0xfffff1d4));
        draw(rounded,0,.16f,0,g.level.width+border*.65f,.04f,g.level.height+border*.65f,0,rgb(0xffc3ad8b));
        draw(rounded,0,.17f,0,g.level.width+.6f,.03f,g.level.height+.6f,0,rgb(0xffad9576));
        for(int cy=0;cy<(g.level.height+7)/8;cy++)for(int x=0;x<(g.level.width+7)/8;x++){int key=cy*8+x;TileChunk tile=chunks.get(key);if(tile==null){tile=new TileChunk();chunks.put(key,tile);}tile.refresh(g,key);tile.render();}
        float road=sceneZ(g,g.level.height+1.8f);
        draw(cube,0,.02f,road,g.level.width+3,.08f,3.3f,0,rgb(0xff8c8c86));
        for(int i=0;i<9;i++)draw(cube,(i-4)*g.level.width/9f,.07f,road+.95f,1.3f,.04f,.11f,0,cream);
        // Low decorations stay beyond the board frame; tall crane arms live in the home scene.
        for(int i=0;i<4;i++)cone((i%2==0?-1:1)*(cx+border/2+1.8f),cz*.65f-i*cz*.35f,.65f*scale);
        float truckX=0, spacing=6.2f*scale;
        if(g.truckState==1)truckX+=(float)Math.pow(g.truckClock/1.4,2)*(g.level.width+10);
        if(g.truckState==2)truckX-=spacing*(1-(float)g.truckClock/1.4);
        int visible=g.visibleTrucks();if(visible>0)truck(truckX,road,1.45f*scale,g.truckState==0?0:(float)g.truckClock*7,g.cargoColors);
        for(int i=1;i<visible;i++){float advance=g.truckState==2?(float)g.truckClock/1.4f:0;truck((-i-(g.truckState==2?1:0)+advance)*spacing,road,1.3f*scale,advance*7,Collections.emptyList());}
        for(NativeGame.Bot bot:g.bots){
            float size=(bot.kind==1?1.9f:1.6f)*scale;
            float motion=bot.phase==0?bot.distance/scale:0;
            float lift=bot.phase==1?(float)Math.sin(Math.min(1,bot.phaseTime/.22)*Math.PI):0;
            float boarding=bot.phase==3?Math.min(1,(float)(bot.phaseTime/.30)):0;
            robot(bot.x-cx+boarding*.65f,sceneZ(g,bot.y)-boarding*.55f,size*(1-boarding*.6f),rgb(NativeGame.COLORS[bot.color]),bot.kind,bot.angle,motion,bot.collected>0&&bot.phase!=3,lift);
        }
        if(g.status==NativeGame.WON){robot(-3*scale,road,1.6f*scale,rgb(NativeGame.COLORS[3]),0,0,(float)clock*2,false);robot(3*scale,road,1.9f*scale,rgb(NativeGame.COLORS[2]),1,0,(float)clock,false);}
        int shown=0;for(NativeGame.Block block:g.blocks)if(block.removed&&g.time-block.removedAt<.3&&shown++<12){float age=(float)(g.time-block.removedAt);draw(cube,block.x-cx,.5f+age*2,block.y-cz,.20f,.20f,.20f,age*120,rgb(NativeGame.COLORS[block.color]));}
    }
    private float sceneZ(NativeGame g,float y){float road=g.level.height+1.8f-(g.level.height-1)/2f+4.8f*g.level.width/32f;if(y>g.level.height+1.8f)return road+(portalZ-road)*Math.min(1,(y-g.level.height-1.8f)/2.6f);return y-(g.level.height-1)/2f+Math.max(0,Math.min(1,(y-g.level.height)/1.8f))*4.8f*g.level.width/32f;}
    private void cone(float x,float z,float scale){draw(rounded,x,.08f,z,scale,.16f,scale,0,ink);draw(rounded,x,.38f*scale,z,.45f*scale,.70f*scale,.45f*scale,0,rgb(0xffff9c43));draw(rounded,x,.42f*scale,z,.47f*scale,.12f*scale,.47f*scale,0,cream);}
    private void crane(float x,float z,float size){draw(cube,x,size/2,z,.22f,size,.22f,0,rgb(0xffffb34d));draw(cube,x-size*.3f,size,z,size,.24f,.23f,0,rgb(0xffffb34d));draw(cube,x-size*.65f,size*.7f,z,.04f,size*.6f,.04f,0,ink);draw(rounded,x-size*.65f,size*.4f,z,.35f,.35f,.35f,0,ink);}
    private void truck(float x,float z,float size,float time,List<Integer> load){
        float[] orange=rgb(0xffffa052);float s=size;
        draw(rounded,x,.5f*s,z,3.7f*s,.4f*s,1.7f*s,0,ink);
        draw(rounded,x+1.35f*s,1.05f*s,z,1.15f*s,1.4f*s,1.7f*s,0,orange);
        draw(rounded,x+1.5f*s,1.4f*s,z+.82f*s,.7f*s,.46f*s,.055f*s,0,ink);
        draw(rounded,x-.55f*s,.93f*s,z,2.4f*s,.3f*s,1.6f*s,0,cream);
        for(int side:new int[]{-1,1})draw(rounded,x-.55f*s,1.05f*s,z+side*.80f*s,2.4f*s,.55f*s,.1f*s,0,orange);
        for(float wheel:new float[]{-1.2f,0,1.4f})for(int side:new int[]{-1,1}){draw(sphere,x+wheel*s,.35f*s,z+side*.85f*s,.70f*s,.70f*s,.25f*s,0,ink);draw(sphere,x+wheel*s,.35f*s,z+side*.99f*s,.34f*s,.34f*s,.07f*s,0,cream);for(int spoke=0;spoke<3;spoke++){float a=time+spoke*2.094f;draw(sphere,x+wheel*s+(float)Math.sin(a)*.11f*s,.35f*s+(float)Math.cos(a)*.11f*s,z+side*1.04f*s,.08f*s,.08f*s,.04f*s,0,ink);}}
        for(int i=0;i<Math.min(12,load.size());i++)draw(rounded,x+(-1.3f+(i%4)*.48f)*s,(1.3f+i/8*.4f)*s,z+((i/4)%2-.5f)*.6f*s,.43f*s,.43f*s,.43f*s,0,rgb(NativeGame.COLORS[load.get(i)]));
        draw(rounded,x+1.35f*s,1.85f*s,z,.18f*s,.18f*s,.18f*s,0,rgb(0xffffa052));
    }
    private void robot(float x,float z,float size,float[] color,int kind,float angle,float distance,boolean carrying){robot(x,z,size,color,kind,angle,distance,carrying,0);}
    private void robot(float x,float z,float size,float[] color,int kind,float angle,float distance,boolean carrying,float crouch){
        float bob=(float)Math.sin(distance*6)*.035f-crouch*.16f;
        shadow(x,z,size*.9f,.16f);
        if(kind==1){
            // A low cab, dual tracks and lifting bucket; no humanoid legs.
            for(int side:new int[]{-1,1}){part(x,z,size,angle,side*.48f,.24f,0,.35f,.38f,1.05f,ink,rounded);for(int i=0;i<3;i++)part(x,z,size,angle,side*.66f,.23f,(i-1)*.34f,.05f,.21f,.21f,cream,sphere);for(int i=0;i<5;i++)part(x,z,size,angle,side*.49f,.44f,((i*.20f+distance*.3f)%1.0f)-.5f,.30f,.04f,.05f,cream,rounded);}
            part(x,z,size,angle,0,.43f,0,1.0f,.38f,.92f,color,rounded);part(x,z,size,angle,0,.84f,-.12f,.78f,.72f,.65f,color,rounded);
            part(x,z,size,angle,0,.88f,.22f,.61f,.45f,.06f,ink,rounded);
            for(int side:new int[]{-1,1})part(x,z,size,angle,side*.4f,.50f-crouch*.12f,.48f,.16f,.20f,.78f,cream,rounded);
            part(x,z,size,angle,0,.33f-crouch*.1f,.92f,.91f,.16f,.45f,ink,rounded);
            if(carrying)for(int i=0;i<3;i++)part(x,z,size,angle,(i-1)*.24f,.55f,.92f,.22f,.24f,.25f,color,rounded);
        }else{
            part(x,z,size,angle,0,.49f+bob,0,.47f,.42f,.35f,color,rounded);part(x,z,size,angle,0,.47f+bob,.20f,.38f,.25f,.04f,rgb(0xffffa052),rounded);
            part(x,z,size,angle,0,.97f+bob,0,.89f,.70f,.66f,color,rounded);
            part(x,z,size,angle,0,1.25f+bob,0,.98f,.30f,.75f,cream,sphere);
            part(x,z,size,angle,0,1.24f+bob,.18f,1.02f,.09f,.80f,cream,rounded);
            part(x,z,size,angle,0,1.39f+bob,0,.11f,.22f,.1f,rgb(0xffffa052),rounded);
            part(x,z,size,angle,0,.99f+bob,.34f,.72f,.42f,.05f,ink,rounded);
            for(int side:new int[]{-1,1}){
                float stride=(float)Math.sin(distance*6+ (side==1?Math.PI:0))*.13f;
                part(x,z,size,angle,side*.19f,.15f,stride,.24f,.23f,.35f,cream,rounded);
                boolean wave=game!=null&&game.status==NativeGame.WON;
                part(x,z,size,angle,side*(wave?.50f:.39f),wave?1.05f+(float)Math.sin(clock*9)*.15f:.49f+bob,carrying?.32f:-stride,.19f,.27f,.23f,cream,rounded);
                part(x,z,size,angle,side*.52f,1+bob,0,.12f,.25f,.3f,cream,sphere);
            }
            if(carrying)part(x,z,size,angle,0,.56f+bob,.42f,.42f,.38f,.36f,color,rounded);
        }
        float faceY=kind==1?.89f:.99f+bob,faceZ=kind==1?.26f:.38f;
        boolean blink=Math.sin(clock*1.7+x)>.995;
        for(int side:new int[]{-1,1})part(x,z,size,angle,side*.17f,faceY,faceZ,.10f,blink?.025f:.12f,.035f,cream,rounded);
        part(x,z,size,angle,0,faceY-.12f,faceZ,.13f,.035f,.035f,cream,rounded);
    }
    private void part(float x,float z,float scale,float angle,float px,float py,float pz,float sx,float sy,float sz,float[] color,Mesh mesh) {
        float radians = (float)Math.toRadians(angle), sin = (float)Math.sin(radians), cos = (float)Math.cos(radians);
        draw(mesh, x + (px*cos+pz*sin)*scale, .21f+py*scale, z + (-px*sin+pz*cos)*scale, sx*scale, sy*scale, sz*scale, angle, color);
    }
    private void drawPreview() {
        draw(rounded,0,-.65f,0,8.8f,.70f,5.8f,0,rgb(0xff81b9aa));
        draw(rounded,0,-.25f,0,8.6f,.36f,5.6f,0,rgb(0xffc7e2c4));
        draw(rounded,0,-.04f,0,8.15f,.12f,5.15f,0,rgb(0xffffebbd));
        for(int i=0;i<3;i++)draw(sphere,2.7f+(i-1)*.42f,3.4f+(i==1?.18f:0),-2.8f,.95f,.60f,.30f,0,cream);
        draw(sphere,-3.25f,3.4f,-2.8f,.95f,.95f,.25f,0,rgb(0xffffcb68));
        crane(3.15f,-2.0f,2.5f);
        for(int i=0;i<5;i++)draw(rounded,(i-2)*.68f,.28f,-1.9f,.58f,.48f,.55f,(i-2)*8,rgb(NativeGame.COLORS[i]));
        robot(-1.65f+(float)Math.sin(clock*.65)*.08f,.12f,2.05f,rgb(NativeGame.COLORS[3]),0,-8+(float)Math.sin(clock*.5)*4,(float)clock*.30f,true);
        robot(1.55f,.20f,2.10f,rgb(NativeGame.COLORS[2]),1,-12,(float)clock*.35f,true);
        draw(rounded,0,.01f,2.0f,8.1f,.06f,1.05f,0,rgb(0xffccbba0));
        for(int i=0;i<5;i++)draw(rounded,(i-2)*1.6f,.065f,2.0f,.70f,.02f,.07f,0,cream);
        truck((float)(clock%12)/12*11-5.5f,2.12f,.48f,(float)clock,Arrays.asList(0,1,2,3));
        cone(-3.5f,1.6f,.50f);cone(3.55f,1.6f,.50f);

    }
    private static class Mesh {
        final FloatBuffer buffer; final int count;
        Mesh(List<Float> data) { count = data.size()/6; buffer = ByteBuffer.allocateDirect(data.size()*4).order(ByteOrder.nativeOrder()).asFloatBuffer(); for (Float f : data) buffer.put(f); buffer.position(0); }
    }
    private static void vertex(List<Float> data, float x,float y,float z,float nx,float ny,float nz) { Collections.addAll(data,x,y,z,nx,ny,nz); }
    private static float[] roundedVertex(float x,float y,float z,float r,int axis,float sign) {
        if (r == 0) return new float[]{x,y,z,axis==0?sign:0,axis==1?sign:0,axis==2?sign:0};
        float limit = .5f-r, qx=Math.max(-limit,Math.min(limit,x)), qy=Math.max(-limit,Math.min(limit,y)), qz=Math.max(-limit,Math.min(limit,z));
        float nx=x-qx,ny=y-qy,nz=z-qz,length=(float)Math.sqrt(nx*nx+ny*ny+nz*nz); nx/=length;ny/=length;nz/=length;
        return new float[]{qx+nx*r,qy+ny*r,qz+nz*r,nx,ny,nz};
    }
    private static Mesh boxMesh(float radius,int steps) {
        List<Float> data = new ArrayList<>();
        for(int axis=0;axis<3;axis++) for(int side=-1;side<=1;side+=2) for(int a=0;a<steps;a++) for(int b=0;b<steps;b++) {
            float u0=a/(float)steps-.5f,u1=(a+1)/(float)steps-.5f,v0=b/(float)steps-.5f,v1=(b+1)/(float)steps-.5f;
            float[][] uv={{u0,v0},{u1,v0},{u1,v1},{u0,v0},{u1,v1},{u0,v1}};
            for(float[] p:uv) { float x=axis==0?side*.5f:p[0], y=axis==1?side*.5f:axis==0?p[0]:p[1], z=axis==2?side*.5f:p[1]; float[] q=roundedVertex(x,y,z,radius,axis,side); for(float f:q)data.add(f); }
        }
        return new Mesh(data);
    }
    private static Mesh sphereMesh(int rings,int segments) {
        List<Float> data=new ArrayList<>();
        for(int i=0;i<rings;i++)for(int j=0;j<segments;j++) {
            int[][] indices={{i,j},{i+1,j},{i+1,j+1},{i,j},{i+1,j+1},{i,j+1}};
            for(int[] p:indices){double phi=Math.PI*p[0]/rings,theta=2*Math.PI*p[1]/segments;float x=(float)(Math.sin(phi)*Math.cos(theta)),y=(float)Math.cos(phi),z=(float)(Math.sin(phi)*Math.sin(theta));vertex(data,x*.5f,y*.5f,z*.5f,x,y,z);}
        }
        return new Mesh(data);
    }
}
