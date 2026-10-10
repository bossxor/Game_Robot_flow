import test from 'node:test';
import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';
import { Game, validateLevel, seededRandom } from '../src/engine.js';
import { defaults, loadSave, storeSave, sanitizeSave, recordWin } from '../src/storage.js';
const TOTAL_LEVELS = 200; // Historical prototype fixtures retained as regression coverage.
function level(blocks, containers, extra = {}) {
  return { levelId: 1, board: { width: 5, height: 5 }, blocks: blocks.map(b => ({ type: 'normal', ...b })), containers, slotCount: 5, powerRobotChance: 0, megaRobot: { requiredGauge: 100, blockCount: 8 }, ...extra };
}
function run(game, limit = 2000) { for (let i = 0; i < limit && game.status === 'playing'; i++) game.update(0.1); }
test('All four sides are reachable, while a sealed center is inaccessible', () => {
  const blocks = Array.from({ length: 25 }, (_, i) => ({ x: i % 5, y: Math.floor(i / 5), color: i === 12 ? 'blue' : 'red' }));
  const g = new Game(level(blocks, [{ id: 1, color: 'red', robotCount: 24, depth: 0 }, { id: 2, color: 'blue', robotCount: 1, depth: 0 }]));
  assert.equal(g.available('red').length, 16); assert.equal(g.available('blue').length, 0);
  g.removeBlock(22); g.removeBlock(17); assert.equal(g.available('blue').length, 1);
});
test('Concurrent robots originate at their own containers and never reserve the same block', () => {
  const g = new Game(level([{ x: 0, y: 4, color: 'red' }, { x: 4, y: 4, color: 'blue' }], [{ id: 1, color: 'red', robotCount: 1, depth: 0 }, { id: 2, color: 'blue', robotCount: 1, depth: 0 }]));
  g.select(1); g.select(2); g.update(0.01);
  assert.equal(g.jobs.length, 2); assert.equal(g.jobs[0].x, g.slotOrigin(0).x); assert.equal(g.jobs[1].x, g.slotOrigin(1).x);
  assert.equal(new Set(g.jobs.flatMap(j => j.selected)).size, 2); run(g); assert.equal(g.status, 'won'); assert.equal(g.jobs.length, 0);
});
test('Full slots do not lose while robots can work; idle deadlock does lose', () => {
  const blocks = [{ x: 0, y: 0, color: 'red' }];
  const g = new Game(level(blocks, [{ id: 1, color: 'red', robotCount: 1, depth: 0 }], { slotCount: 1 }));
  g.select(1); g.update(0.02); assert.equal(g.status, 'playing'); run(g); assert.equal(g.status, 'won');
  const blocked = new Game(level(blocks, [{ id: 1, color: 'blue', robotCount: 1, depth: 0 }, { id: 2, color: 'red', robotCount: 1, depth: 0 }], { slotCount: 1 }));
  blocked.select(1); blocked.update(0.1); assert.equal(blocked.status, 'lost'); assert.ok(blocked.extraSlot()); assert.equal(blocked.status, 'playing'); blocked.select(2); run(blocked); assert.equal(blocked.status, 'won');
});
test('Blocked container dependency unlocks on selection; pull-out is targeted', () => {
  const g = new Game(level([{ x: 0, y: 0, color: 'red' }], [{ id: 1, color: 'red', robotCount: 1, depth: 0 }, { id: 2, color: 'blue', robotCount: 1, depth: 1, blockedBy: [1] }]));
  assert.equal(g.select(2), false); assert.ok(g.pullOut(2)); assert.ok(g.select(2)); assert.equal(g.slots[0].id, 2);
});
test('Power and mega capacities are bounded, and mega consumes the full gauge on next selection', () => {
  const blocks = Array.from({ length: 10 }, (_, i) => ({ x: i % 5, y: Math.floor(i / 5), color: 'red' }));
  const g = new Game(level(blocks, [{ id: 1, color: 'red', robotCount: 10, depth: 0 }], { powerRobotChance: 1 }));
  g.select(1); g.update(0.01); assert.equal(g.jobs[0].type, 'power'); assert.equal(g.jobs[0].selected.length, 3);
  const mega = new Game(g.level); mega.gauge = 100; mega.select(1); mega.update(0.01);
  assert.equal(mega.jobs[0].type, 'mega'); assert.equal(mega.jobs[0].selected.length, 8); assert.equal(mega.gauge, 15);
});
test('Pause freezes all simulation; clearing reserved blocks cannot double count', () => {
  const g = new Game(level([{ x: 0, y: 0, color: 'red' }], [{ id: 1, color: 'red', robotCount: 1, depth: 0 }]));
  g.select(1); g.update(0.01); const before = structuredClone(g.jobs); g.paused = true; g.update(0.1); assert.deepEqual(g.jobs, before);
  g.paused = false; assert.ok(g.clearColor('red')); assert.equal(g.clearColor('red'), false); run(g); assert.equal(g.collected, 1); assert.equal(g.status, 'won');
});
test('Shuffle preserves total remaining robot inventory and dependency topology', () => {
  const g = new Game(level([{ x: 0, y: 0, color: 'red' }], [{ id: 1, color: 'red', robotCount: 1, depth: 0 }, { id: 2, color: 'blue', robotCount: 2, depth: 1, blockedBy: [1] }]), { random: () => 0 });
  assert.ok(g.shuffle()); assert.equal(g.queue[0].color, 'blue'); assert.deepEqual(g.queue[1].blockedBy, [1]); assert.equal(g.queue.reduce((n, c) => n + c.remaining, 0), 3);
});
test('Invalid imports reject duplicate cells, dependency cycles and insufficient robots', () => {
  const valid = level([{ x: 0, y: 0, color: 'red' }], [{ id: 1, color: 'red', robotCount: 1, depth: 0 }]);
  assert.throws(() => validateLevel({ ...valid, blocks: [...valid.blocks, ...valid.blocks] }));
  assert.throws(() => validateLevel({ ...valid, containers: [{ id: 1, color: 'red', robotCount: 1, depth: 0, blockedBy: [2] }, { id: 2, color: 'red', robotCount: 1, depth: 1, blockedBy: [1] }] }));
  assert.throws(() => validateLevel({ ...valid, containers: [{ id: 1, color: 'blue', robotCount: 1, depth: 0 }] }));
});
for (let id = 1; id <= TOTAL_LEVELS; id++) test(`JSON level ${id}: legal solution, no specials, clear after all robots return`, async () => {
  const data = JSON.parse(await readFile(`public/levels/level-${String(id).padStart(2, '0')}.json`, 'utf8'));
  const g = new Game({ ...data, powerRobotChance: 0 }, { random: seededRandom(id) });
  for (let tick = 0; tick < 12000 && g.status === 'playing'; tick++) {
    g.gauge = 0;
    if (!g.jobs.length && g.slots.includes(null)) {
      const options = g.queue.filter(c => g.isUnlocked(c));
      const next = options.find(c => g.available(c.color).length) || options[0];
      if (next) assert.ok(g.select(next.id));
    }
    g.update(0.1);
  }
  assert.equal(g.status, 'won'); assert.equal(g.remainingBlocks, 0); assert.equal(g.jobs.length, 0);
});
for (const chance of [0.1, 1]) test(`All ${TOTAL_LEVELS} levels also clear with power chance ${chance}`, async () => {
  for (let id = 1; id <= TOTAL_LEVELS; id++) {
    const data = JSON.parse(await readFile(`public/levels/level-${String(id).padStart(2, '0')}.json`, 'utf8'));
    const g = new Game({ ...data, powerRobotChance: chance }, { random: seededRandom(id * 173) });
    for (let tick = 0; tick < 12000 && g.status === 'playing'; tick++) {
      if (!g.jobs.length && g.slots.includes(null)) {
        const options = g.queue.filter(c => g.isUnlocked(c));
        const next = options.find(c => g.available(c.color).length) || options[0];
        if (next) g.select(next.id);
      }
      g.update(0.1);
    }
    assert.equal(g.status, 'won', `level ${id}`);
  }
});
test('Local save tolerates broken JSON; rewards, records and unlocks persist', () => {
  let content = '{bad json'; const storage = { getItem: () => content, setItem: (_, value) => { content = value; } };
  assert.deepEqual(loadSave(storage), defaults()); const save = defaults(); recordWin(save, 1, 60, 8, 100); recordWin(save, 1, 80, 10, 100);
  assert.equal(save.coins, 425); assert.equal(save.cleared[1].bestTime, 60); assert.equal(save.highestUnlockedLevel, 2);
  storeSave(save, storage); assert.deepEqual(loadSave(storage), save);
  const corrupt = sanitizeSave({ ...save, coins: -9, settings: { musicVolume: 7 }, boosters: { extraSlot: -4 } });
  assert.equal(corrupt.coins, 0); assert.equal(corrupt.settings.musicVolume, 1); assert.equal(corrupt.boosters.extraSlot, 0);
});
test('Existing saves migrate without reset and unlock through level 500', () => {
  const old = defaults(); old.highestUnlockedLevel = 20; old.lastLevel = 20;
  const save = sanitizeSave(old); assert.equal(save.lastLevel, 20);
  recordWin(save, 20, 100, 15, 100); assert.equal(save.highestUnlockedLevel, 21);
  recordWin(save, 199, 100, 15, 100); assert.equal(save.highestUnlockedLevel, 200);
  recordWin(save, 200, 100, 15, 100); assert.equal(save.highestUnlockedLevel, 201);
  recordWin(save, 500, 100, 15, 100); assert.equal(save.highestUnlockedLevel, 500);
  assert.equal(sanitizeSave(save).cleared[200].bestTime, 100);
});
test('The 200 shipped boards are distinct and cover six board patterns', async () => {
  const layouts = new Set(), patterns = new Set();
  for (let id = 1; id <= TOTAL_LEVELS; id++) {
    const data = JSON.parse(await readFile(`public/levels/level-${String(id).padStart(2, '0')}.json`, 'utf8'));
    assert.equal(data.levelId, id); assert.ok(data.name);
    layouts.add(JSON.stringify(data.blocks)); patterns.add(data.pattern);
  }
  assert.equal(layouts.size, TOTAL_LEVELS); assert.equal(patterns.size, 6);
});
