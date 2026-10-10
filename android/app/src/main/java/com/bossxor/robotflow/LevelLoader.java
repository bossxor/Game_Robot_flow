package com.bossxor.robotflow;

import android.content.res.AssetManager;
import org.json.*;
import java.io.*;
import java.nio.charset.StandardCharsets;

public final class LevelLoader {
    public static final int TOTAL = 500;
    private static int color(String key) throws JSONException {
        for (int i = 0; i < NativeGame.COLOR_KEYS.length; i++) if (NativeGame.COLOR_KEYS[i].equals(key)) return i;
        throw new JSONException("Unknown color: " + key);
    }
    public static NativeGame.Level load(AssetManager assets, int id) throws IOException, JSONException {
        if (id < 1 || id > TOTAL) throw new IOException("Invalid level " + id);
        String path = String.format(java.util.Locale.US, "levels/level-%02d.json", id);
        String text;
        try (InputStream stream = assets.open(path); ByteArrayOutputStream bytes = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192]; int n; while ((n = stream.read(buffer)) > 0) bytes.write(buffer, 0, n);
            text = new String(bytes.toByteArray(), StandardCharsets.UTF_8);
        }
        JSONObject json = new JSONObject(text), board = json.getJSONObject("board");
        NativeGame.Level level = new NativeGame.Level(); level.id = json.getInt("levelId"); level.name = json.optString("name", "로봇 공장");
        level.width = board.getInt("width"); level.height = board.getInt("height"); level.slotCount = json.optInt("slotCount", 5);
        level.difficulty=json.optInt("difficulty",0);level.theme=json.optInt("theme",0);level.autoFinish=json.optBoolean("autoFinish",false);
        level.powerChance = json.optDouble("powerRobotChance", .1); level.revealDepth = json.optInt("revealDepth", 100); level.reward = json.optInt("reward", 100);
        JSONObject mega = json.getJSONObject("megaRobot"); level.megaRequired = mega.getInt("requiredGauge"); level.megaCapacity = mega.getInt("blockCount");
        JSONArray blocks = json.getJSONArray("blocks"), capsules = json.getJSONArray("containers");
        JSONArray seals=json.optJSONArray("seals"), keys=json.optJSONArray("keys");
        if(seals!=null)for(int i=0;i<seals.length();i++){
            JSONObject s=seals.getJSONObject(i);int x=s.getInt("x"),y=s.getInt("y"),w=s.getInt("width"),h=s.getInt("height"),count=s.getInt("count");
            if(x<0||y<0||w<1||h<1||x+w>level.width||y+h>level.height||count<1)throw new JSONException("Invalid sealed region");
            level.seals.add(new NativeGame.Seal(x,y,w,h,color(s.getString("color")),count));
        }
        if(keys!=null)for(int i=0;i<keys.length();i++){
            JSONObject k=keys.getJSONObject(i);int keyId=k.getInt("id"),x=k.getInt("x"),y=k.getInt("y");
            if(keyId<1||x<0||y<0||x>=level.width||y>=level.height)throw new JSONException("Invalid board key");
            for(NativeGame.BoardKey existing:level.keys)if(existing.id==keyId)throw new JSONException("Duplicate board key");
            level.keys.add(new NativeGame.BoardKey(keyId,x,y,color(k.getString("lockColor"))));
        }
        for (int i = 0; i < blocks.length(); i++) { JSONObject b = blocks.getJSONObject(i); level.blocks.add(new NativeGame.Block(b.getInt("x"), b.getInt("y"), color(b.getString("color")))); }
        for (int i = 0; i < capsules.length(); i++) {
            JSONObject c = capsules.getJSONObject(i); NativeGame.Capsule capsule = new NativeGame.Capsule(c.getInt("id"), color(c.getString("color")), c.getInt("robotCount"), c.optInt("depth", 0));
            capsule.lane = c.optInt("lane", i % 4); capsule.powerChance = c.optDouble("specialRobotChance", -1);
            capsule.group=c.optInt("group",0);
            JSONArray parents = c.optJSONArray("blockedBy");
            if (parents != null) { capsule.blockedBy = new int[parents.length()]; for (int j = 0; j < parents.length(); j++) capsule.blockedBy[j] = parents.getInt(j); }
            else for (NativeGame.Capsule p : level.capsules) if (p.depth < capsule.depth) { int[] next = java.util.Arrays.copyOf(capsule.blockedBy, capsule.blockedBy.length + 1); next[next.length - 1] = p.id; capsule.blockedBy = next; }
            level.capsules.add(capsule);
        }
        return level;
    }
}
