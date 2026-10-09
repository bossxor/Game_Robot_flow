import test from 'node:test';
import assert from 'node:assert/strict';
import {readFile} from 'node:fs/promises';
import {Game,validateLevel,seededRandom,COLORS} from '../src/engine.js';
import {TOTAL_LEVELS} from '../src/config.js';
import {defaults,sanitizeSave,recordWin} from '../src/storage.js';
const load=async id=>JSON.parse(await readFile(`public/construction-levels/level-${String(id).padStart(2,'0')}.json`,'utf8'));
test('Browser construction data covers 500 distinct boards with exact inventories',async()=>{
 const boards=new Set();assert.equal(TOTAL_LEVELS,500);for(let id=1;id<=TOTAL_LEVELS;id++){const l=validateLevel(await load(id));assert.equal(l.levelId,id);assert.equal(l.board.width,id<=2?20:48);assert.equal(l.slotCount,5);for(const color of COLORS)assert.equal(l.blocks.filter(b=>b.color===color).length,l.containers.filter(c=>c.color===color).reduce((sum,c)=>sum+c.robotCount,0));boards.add(JSON.stringify(l.blocks));}assert.equal(boards.size,500);
});
for(const chance of [0,1])test(`Browser tutorial clears through the shared truck with builder chance ${chance}`,async()=>{
 const l=await load(1);l.powerRobotChance=chance;const g=new Game(l,{random:seededRandom(37)});for(let i=0;i<60000&&g.status==='playing';i++){if(!g.jobs.length&&g.slots.includes(null)){const options=g.queue.filter(c=>g.isUnlocked(c));const next=options.find(c=>g.available(c.color).length)||options[0];if(next)g.select(next.id);}g.update(.1);assert.ok(g.truckCargo<=20);assert.ok(g.jobs.every(j=>j.type==='mini'||j.type==='mega'));}assert.equal(g.status,'won');assert.equal(g.delivered,400);assert.equal(g.truckCargo,0);assert.equal(g.truckPending,0);
});
test('Large board color clear loads every block once and waits for final truck',async()=>{
 const g=new Game(await load(500));g.unlimitedItems=true;for(const c of new Set(g.blocks.map(b=>b.color)))assert.ok(g.clearColor(c));assert.equal(g.remainingBlocks,0);assert.equal(g.status,'playing');for(let i=0;i<4000&&g.status==='playing';i++)g.update(.1);assert.equal(g.status,'won');assert.equal(g.delivered,2304);assert.equal(g.truckCargo,0);assert.equal(g.truckPending,0);
});
test('Unlimited item state persists and progression crosses the previous last level',()=>{
 const s=defaults();s.unlimitedItems=true;s.highestUnlockedLevel=200;recordWin(s,200,100,10,100);const loaded=sanitizeSave(s);assert.equal(loaded.unlimitedItems,true);assert.equal(loaded.lastLevel,201);assert.deepEqual(loaded.boosters,s.boosters);
});
