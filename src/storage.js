import { TOTAL_LEVELS } from './config.js';
export const SAVE_KEY = 'robot-flow.save.v1';
export function defaults() {
  return { version: 1, currentLevel: 1, highestUnlockedLevel: 1, lastLevel: 1, coins: 300, cleared: {}, streak:0,lastStreakBonus:0,boosters: { extraSlot: 3, colorClear: 2, shuffle: 3, pullOut: 3 }, unlimitedItems: false, tutorialCompleted: false, robotUnlocks: ['mini', 'mega'], settings: { music: true, soundEffect: true, vibration: true, musicVolume: 0.35, sfxVolume: 0.65, language: 'ko' } };
}
export function sanitizeSave(raw) {
  const base = defaults();
  if (!raw || raw.version !== 1) return base;
  const num = (n, fallback, min, max) => Number.isFinite(n) ? Math.max(min, Math.min(max, Math.floor(n))) : fallback;
  for (const k of ['currentLevel', 'highestUnlockedLevel', 'lastLevel']) base[k] = num(raw[k], 1, 1, TOTAL_LEVELS);
  base.currentLevel = Math.min(base.currentLevel, base.highestUnlockedLevel);
  base.lastLevel = Math.min(base.lastLevel, base.highestUnlockedLevel);
  base.coins = num(raw.coins, 300, 0, 10000000);
  for (const key of Object.keys(base.boosters)) base.boosters[key] = num(raw.boosters?.[key], base.boosters[key], 0, 999);
  for (const key of ['music', 'soundEffect', 'vibration']) if (typeof raw.settings?.[key] === 'boolean') base.settings[key] = raw.settings[key];
  for (const key of ['musicVolume', 'sfxVolume']) if (Number.isFinite(raw.settings?.[key])) base.settings[key] = Math.max(0, Math.min(1, raw.settings[key]));
  base.tutorialCompleted = raw.tutorialCompleted === true;
  base.unlimitedItems = raw.unlimitedItems === true;
  base.streak=num(raw.streak,0,0,1000000);
  base.lastStreakBonus=num(raw.lastStreakBonus,0,0,50);
  for (let i = 1; i <= TOTAL_LEVELS; i++) if (raw.cleared?.[i] && Number.isFinite(raw.cleared[i].bestTime) && raw.cleared[i].bestTime >= 0) base.cleared[i] = { bestTime: raw.cleared[i].bestTime, moves: num(raw.cleared[i].moves, 0, 0, 10000) };
  return base;
}
export function loadSave(storage = globalThis.localStorage) {
  try { return sanitizeSave(JSON.parse(storage.getItem(SAVE_KEY))); } catch { return defaults(); }
}
export function storeSave(save, storage = globalThis.localStorage) {
  try { storage.setItem(SAVE_KEY, JSON.stringify(save)); return true; } catch { return false; }
}
export function recordWin(save, id, time, moves, reward) {
  const first = !save.cleared[id];
  if (first || time < save.cleared[id].bestTime) save.cleared[id] = { bestTime: time, moves };
  save.streak=save.streak||0;save.lastStreakBonus=first?Math.min(++save.streak,5)*10:0;
  save.coins += (first ? reward : Math.floor(reward / 4))+save.lastStreakBonus;
  save.highestUnlockedLevel = Math.max(save.highestUnlockedLevel, Math.min(TOTAL_LEVELS, id + 1));
  save.currentLevel = save.highestUnlockedLevel; save.lastLevel = save.currentLevel;
  return (first ? reward : Math.floor(reward / 4))+save.lastStreakBonus;
}
