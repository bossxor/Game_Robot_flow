import test from 'node:test';
import assert from 'node:assert/strict';
import {readFile} from 'node:fs/promises';
import {Game,validateLevel,seededRandom,COLORS} from '../src/engine.js';
import {TOTAL_LEVELS} from '../src/config.js';
import {defaults,sanitizeSave,recordWin} from '../src/storage.js';
import {planQueue} from '../src/queue-planner.js';
const load=async id=>JSON.parse(await readFile(`public/construction-levels/level-${String(id).padStart(2,'0')}.json`,'utf8'));
test('All 500 planned queues preserve inventory; premature inner colours can fill five slots',async()=>{
 for(let id=1;id<=TOTAL_LEVELS;id++){
  const original=await load(id),planned=validateLevel(planQueue(original));
  assert.deepEqual(planned.blocks,original.blocks);assert.equal(planned.slotCount,5);
  for(const color of COLORS)assert.equal(planned.containers.filter(c=>c.color===color).reduce((n,c)=>n+c.robotCount,0),original.containers.filter(c=>c.color===color).reduce((n,c)=>n+c.robotCount,0));
  assert.deepEqual(planQueue(planned),planned);assert.equal(original.queueVersion,undefined);
  const trapLane=(id+Math.floor(id/10))%4,waiting=planned.containers.filter(c=>c.lane===trapLane);
  assert.ok(waiting.length>=(id===1?2:[5,7,9][planned.difficulty]));
  if(id===1)continue;
  const g=new Game(planned);for(const key of g.keys)g.removeBlock(g.blocks.find(b=>b.x===key.x&&b.y===key.y).id);for(let i=0;i<5;i++){const next=g.queue.find(c=>c.lane===trapLane&&g.isUnlocked(c));assert.equal(g.available(next.color).length,0);assert.ok(g.select(next.id));}
  g.update(.1);assert.equal(g.status,'lost',`premature selections in level ${id}`);
 }
});
test('Browser construction data covers 500 distinct boards with exact inventories',async()=>{
 const boards=new Set();assert.equal(TOTAL_LEVELS,500);for(let id=1;id<=TOTAL_LEVELS;id++){const l=validateLevel(await load(id));assert.equal(l.levelId,id);assert.equal(l.board.width,id<=2?20:48);assert.equal(l.slotCount,5);for(const color of COLORS)assert.equal(l.blocks.filter(b=>b.color===color).length,l.containers.filter(c=>c.color===color).reduce((sum,c)=>sum+c.robotCount,0));boards.add(JSON.stringify(l.blocks));}assert.equal(boards.size,500);
});
for(const chance of [0,1])test(`Browser tutorial clears through the shared truck with builder chance ${chance}`,async()=>{
 const l=await load(1);l.powerRobotChance=chance;const g=new Game(l,{random:seededRandom(37)});for(let i=0;i<60000&&g.status==='playing';i++){if(!g.jobs.length&&g.slots.includes(null)){const options=g.queue.filter(c=>g.isUnlocked(c));const next=options.find(c=>g.available(c.color).length)||options[0];if(next)g.select(next.id);}g.update(.1);assert.ok(g.truckCargo<=20);assert.ok(g.jobs.every(j=>j.type==='mini'||j.type==='mega'));}assert.equal(g.status,'won');assert.equal(g.delivered,400);assert.equal(g.truckCargo,0);assert.equal(g.truckPending,0);
});
test('Large board color clear loads every block once and waits for final truck',async()=>{
 const g=new Game(await load(500));g.unlimitedItems=true;for(let pass=0;pass<2;pass++)for(const c of new Set(g.blocks.map(b=>b.color)))g.clearColor(c);assert.equal(g.remainingBlocks,0);assert.equal(g.status,'playing');for(let i=0;i<4000&&g.status==='playing';i++)g.update(.1);assert.equal(g.status,'won');assert.equal(g.delivered,2304);assert.equal(g.truckCargo,0);assert.equal(g.truckPending,0);
});
test('Unlimited item state persists and progression crosses the previous last level',()=>{
 const s=defaults();s.unlimitedItems=true;s.highestUnlockedLevel=200;recordWin(s,200,100,10,100);const loaded=sanitizeSave(s);assert.equal(loaded.unlimitedItems,true);assert.equal(loaded.lastLevel,201);assert.deepEqual(loaded.boosters,s.boosters);
});
test('Linked containers enter atomically and pull-out moves the whole linked group',async()=>{
 const l=await load(10),g=new Game(l);const linked=g.queue.filter(c=>c.group);assert.ok(linked.length);const front=linked[0];for(const q of g.queue)if(q.group===front.group){q.depth=2;q.blockedBy=[g.queue.find(c=>!c.group).id];}assert.ok(g.pullOut(front.id));const group=g.selectionGroup(front);assert.ok(group.every(c=>g.isUnlocked(c)));while(g.slots.filter(c=>!c).length>=group.length)g.slots[g.slots.indexOf(null)]={color:'red',remaining:0};assert.equal(g.select(front.id),false);assert.ok(group.every(c=>!c.taken));g.slots.fill(null);assert.ok(g.select(front.id));assert.equal(g.slots.filter(Boolean).length,group.length);g.update(.1);assert.equal(g.jobs.length,0);
});
test('The last five containers autofill and enter the finishing state',async()=>{
 const l=await load(1);l.containers=l.containers.slice(0,5);l.blocks=l.containers.flatMap((c,i)=>Array.from({length:c.robotCount},(_,j)=>({x:j%20,y:i*2+Math.floor(j/20),color:c.color})));const g=new Game(l);g.update(.1);assert.ok(g.finishing);assert.ok(g.queue.every(c=>c.taken));
});
test('Streak bonus caps at 50 and repeating a cleared stage adds no streak bonus',()=>{
 const s=defaults();for(let i=1;i<=7;i++)recordWin(s,i,10,1,100);assert.equal(s.streak,7);assert.equal(s.lastStreakBonus,50);const before=s.coins;recordWin(s,7,11,1,100);assert.equal(s.lastStreakBonus,0);assert.equal(s.coins-before,25);
});

test('Key and colour seals block items, unlock at the exact threshold, and reset on restart',()=>{
 const level={levelId:1,queueVersion:1,board:{width:5,height:5},slotCount:5,powerRobotChance:0,megaRobot:{requiredGauge:100,blockCount:8},blocks:[{x:0,y:0,color:'red'},{x:1,y:0,color:'red'},{x:2,y:0,color:'blue'}],containers:[{id:1,color:'red',robotCount:2,depth:0,blockedBy:[]},{id:2,color:'blue',robotCount:1,depth:0,blockedBy:[]}],keys:[{id:1,x:0,y:0,lockColor:'blue'}],seals:[{x:2,y:0,width:1,height:1,color:'red',count:2}]};
 const g=new Game(level);assert(g.keyLocked(g.queue[1]));assert.equal(g.select(2),false);assert.equal(g.pullOut(2),false);assert.equal(g.clearColor('blue'),false);
 g.removeBlock(2);assert.equal(g.blocks[2].removed,false);g.removeBlock(0);assert(g.keys[0].owned);assert(g.isUnlocked(g.queue[1]));assert.equal(g.seals[0].open,false);
 g.removeBlock(1);assert(g.seals[0].open);assert.equal(g.available('blue').length,1);const fresh=new Game(level);assert.equal(fresh.keys[0].owned,false);assert.equal(fresh.seals[0].open,false);
 fresh.shuffle();for(const c of fresh.queue)assert.equal(fresh.keyLocked(c),c.color==='blue');
 assert.throws(()=>new Game({...level,seals:[{...level.seals[0],count:0}]}));assert.throws(()=>new Game({...level,keys:[{...level.keys[0],x:99}]}));
});
test('Mystery containers reveal after two cleared colours without bypassing their queue parents',()=>{
 const level={levelId:13,queueVersion:1,board:{width:5,height:5},slotCount:5,powerRobotChance:0,megaRobot:{requiredGauge:100,blockCount:8},blocks:[{x:0,y:0,color:'red'},{x:1,y:0,color:'green'},{x:2,y:0,color:'blue'},{x:3,y:0,color:'orange'}],containers:[{id:1,color:'orange',robotCount:1,depth:0,blockedBy:[]},{id:2,color:'red',robotCount:1,depth:0,blockedBy:[]},{id:3,color:'green',robotCount:1,depth:0,blockedBy:[]},{id:7,color:'blue',robotCount:1,depth:3,blockedBy:[1]}]};
 const g=new Game(level),c=g.queue.at(-1);assert.equal(g.isVisible(c),false);g.removeBlock(0);assert.equal(g.isVisible(c),false);g.removeBlock(1);assert.equal(g.isVisible(c),true);assert.equal(g.select(7),false);
});
