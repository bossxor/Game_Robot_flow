package com.bossxor.robotflow;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONObject;
import java.io.*;
import java.nio.charset.StandardCharsets;

/** Native local progress. Reads old localStorage JSON once when upgrading. */
public final class PlayerSave {
    private final SharedPreferences prefs;
    public int current, unlocked, coins, gameSpeed;
    public long lastDailyDay;
    public boolean music, sound, vibration, unlimitedItems;
    public float musicVolume, soundVolume;
    public final int[] boosters = new int[4];
    public PlayerSave(Context context) {
        // v2 clears redesign-era progress (hearts/ads/streak migration not carried).
        prefs = context.getSharedPreferences("robotflow.native.v2", Context.MODE_PRIVATE);
        if (!prefs.contains("initialized")) migrateLegacy(context);
        current = clamp(prefs.getInt("current", 1), 1, LevelLoader.TOTAL); unlocked = clamp(prefs.getInt("unlocked", 1), current, LevelLoader.TOTAL);
        coins = Math.max(0, prefs.getInt("coins", 300));
        gameSpeed = normalizeSpeed(prefs.getInt("gameSpeed", 1));
        lastDailyDay = prefs.getLong("lastDailyDay", 0);
        music = prefs.getBoolean("music", true); sound = prefs.getBoolean("sound", true); vibration = prefs.getBoolean("vibration", true);
        unlimitedItems = prefs.getBoolean("unlimitedItems", false);
        musicVolume = prefs.getFloat("musicVolume", .35f); soundVolume = prefs.getFloat("soundVolume", .65f);
        for (int i = 0; i < 4; i++) boosters[i] = Math.max(0, prefs.getInt("booster" + i, i == 1 ? 2 : 3));
        persist();
    }
    private static int clamp(int n, int min, int max) { return Math.max(min, Math.min(max, n)); }
    public static int normalizeSpeed(int speed) { return speed == 3 || speed == 5 ? speed : 1; }
    public static int nextSpeed(int speed) { return speed == 1 ? 3 : speed == 3 ? 5 : 1; }
    public void persist() {
        SharedPreferences.Editor e = prefs.edit().putBoolean("initialized", true).putInt("current", current).putInt("unlocked", unlocked).putInt("coins", coins).putInt("gameSpeed", gameSpeed).putLong("lastDailyDay", lastDailyDay)
                .putBoolean("music", music).putBoolean("sound", sound).putBoolean("vibration", vibration).putBoolean("unlimitedItems", unlimitedItems).putFloat("musicVolume", musicVolume).putFloat("soundVolume", soundVolume);
        for (int i = 0; i < 4; i++) e.putInt("booster" + i, boosters[i]); e.apply();
    }
    public boolean cleared(int level) { return prefs.contains("record" + level); }
    public boolean hasBooster(int which) { return unlimitedItems || boosters[which] > 0; }
    public void consumeBooster(int which) { if (!unlimitedItems) boosters[which] = Math.max(0, boosters[which] - 1); persist(); }
    public String boosterCount(int which) { return unlimitedItems ? "∞" : String.valueOf(boosters[which]); }
    public float record(int level) { return prefs.getFloat("record" + level, 0); }
    public int win(NativeGame game) {
        int id = game.level.id;boolean first=!cleared(id);int reward=first?game.level.reward:game.level.reward/4;
        float best = record(id); if (best == 0 || game.time < best) prefs.edit().putFloat("record" + id, (float)game.time).apply();
        coins += reward; unlocked = Math.max(unlocked, Math.min(LevelLoader.TOTAL, id + 1)); current = Math.min(LevelLoader.TOTAL, id + 1); persist(); return reward;
    }
    public boolean dailyAvailable() { return lastDailyDay != System.currentTimeMillis() / 86_400_000L; }
    public int claimDaily() {
        long day = System.currentTimeMillis() / 86_400_000L;
        if (lastDailyDay == day) return 0;
        lastDailyDay = day; coins += 50; persist(); return 50;
    }
    public void reset() { prefs.edit().clear().commit(); current = unlocked = 1; coins = 300; gameSpeed = 1; lastDailyDay = 0; music = sound = vibration = true; unlimitedItems = false; musicVolume = .35f; soundVolume = .65f; for(int i=0;i<4;i++)boosters[i]=i==1?2:3; persist(); }
    private void migrateLegacy(Context context) {
        File directory = new File(context.getApplicationInfo().dataDir, "app_webview/Default/Local Storage/leveldb");
        File[] files = directory.listFiles((dir,name) -> name.endsWith(".log") || name.endsWith(".ldb")); if (files == null) return;
        java.util.Arrays.sort(files, (a,b) -> Long.compare(a.lastModified(), b.lastModified()));
        JSONObject latest = null;
        for (File file : files) {
            if (file.length() > 8_000_000) continue;
            try (InputStream in = new FileInputStream(file); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                byte[] buffer = new byte[8192]; int n; while((n=in.read(buffer))>0)out.write(buffer,0,n);
                byte[] bytes = out.toByteArray();
                for (java.nio.charset.Charset charset : new java.nio.charset.Charset[]{StandardCharsets.UTF_8, StandardCharsets.UTF_16LE}) {
                    String data = new String(bytes, charset); int start = 0;
                    while ((start = data.indexOf("{\"version\":1,", start)) >= 0) {
                        int depth=0,end=start; boolean quoted=false,escaped=false;
                        for(;end<data.length();end++) { char c=data.charAt(end); if(quoted){if(escaped)escaped=false;else if(c=='\\')escaped=true;else if(c=='"')quoted=false;}else if(c=='"')quoted=true;else if(c=='{')depth++;else if(c=='}'&&--depth==0){end++;break;} }
                        try { JSONObject candidate = new JSONObject(data.substring(start,end)); if(candidate.has("highestUnlockedLevel")&&candidate.has("boosters"))latest=candidate; } catch(Exception ignored) { }
                        start++;
                    }
                }
            } catch (IOException ignored) { }
        }
        if(latest == null)return;
        try {
            SharedPreferences.Editor e=prefs.edit().putInt("current",clamp(latest.optInt("lastLevel",1),1,LevelLoader.TOTAL)).putInt("unlocked",clamp(latest.optInt("highestUnlockedLevel",1),1,LevelLoader.TOTAL)).putInt("coins",latest.optInt("coins",300));
            JSONObject b=latest.optJSONObject("boosters"),s=latest.optJSONObject("settings"),records=latest.optJSONObject("cleared");
            String[] keys={"extraSlot","colorClear","shuffle","pullOut"};
            if(b!=null)for(int i=0;i<4;i++)e.putInt("booster"+i,b.optInt(keys[i],i==1?2:3));
            if(s!=null)e.putBoolean("music",s.optBoolean("music",true)).putBoolean("sound",s.optBoolean("soundEffect",true)).putBoolean("vibration",s.optBoolean("vibration",true)).putFloat("musicVolume",(float)s.optDouble("musicVolume",.35)).putFloat("soundVolume",(float)s.optDouble("sfxVolume",.65));
            if(records!=null)for(int i=1;i<=LevelLoader.TOTAL;i++){JSONObject r=records.optJSONObject(String.valueOf(i));if(r!=null)e.putFloat("record"+i,(float)r.optDouble("bestTime",0));}
            e.apply();
        } catch (Exception ignored) { }
    }
}
