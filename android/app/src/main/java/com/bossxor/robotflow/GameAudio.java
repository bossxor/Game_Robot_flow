package com.bossxor.robotflow;
import android.content.Context;
import android.media.*;
import java.io.*;
import java.util.*;
/** Original soft synthesized chimes; no system alert or telephone tones. */
final class GameAudio {
 private final SoundPool pool=new SoundPool.Builder().setMaxStreams(5).setAudioAttributes(new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build()).build();
 private final Map<String,Integer> sounds=new HashMap<>();private final Set<Integer> ready=new HashSet<>();private int musicStream;
 GameAudio(Context c){pool.setOnLoadCompleteListener((p,id,status)->{if(status==0)ready.add(id);});try{load(c,"click",880,.07);load(c,"collect",1046,.08);load(c,"select",659,.13);load(c,"mega",523,.30);load(c,"won",784,.40);load(c,"lost",220,.25);for(int i=0;i<4;i++)load(c,"note"+i,new double[]{261.63,329.63,392,440}[i],.40);}catch(IOException ignored){}}
 private void load(Context c,String name,double frequency,double duration)throws IOException{
  int rate=22050,n=(int)(rate*duration);File file=new File(c.getCacheDir(),"robotflow-"+name+".wav");
  try(DataOutputStream out=new DataOutputStream(new FileOutputStream(file))){out.writeBytes("RIFF");le(out,36+n*2,4);out.writeBytes("WAVEfmt ");le(out,16,4);le(out,1,2);le(out,1,2);le(out,rate,4);le(out,rate*2,4);le(out,2,2);le(out,16,2);out.writeBytes("data");le(out,n*2,4);for(int i=0;i<n;i++){double t=i/(double)rate,env=Math.min(1,t/.012)*Math.pow(1-i/(double)n,2);double signal=(Math.sin(t*frequency*Math.PI*2)+.18*Math.sin(t*frequency*Math.PI*4))*.26*env;le(out,(int)(signal*32767),2);}}
  sounds.put(name,pool.load(file.getAbsolutePath(),1));
 }
 private static void le(DataOutputStream out,int n,int bytes)throws IOException{for(int i=0;i<bytes;i++)out.write((n>>(8*i))&255);}
 private int play(String key,float volume){Integer id=sounds.get(key);return id!=null&&ready.contains(id)?pool.play(id,volume,volume,1,0,1):0;}
 void effect(String key,float volume){play(sounds.containsKey(key)?key:"click",volume);}
 void music(int note,float volume){musicStream=play("note"+(note%4),volume*.35f);}
 void stopMusic(){pool.stop(musicStream);}void stop(){pool.autoPause();}void resume(){pool.autoResume();}void release(){pool.release();}
}
