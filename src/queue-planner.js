// Keep the picture and inventory; make choosing the next colour matter.
// Sealed colours are visible early, while three lanes retain the original
// constructive removal order. A full row of premature choices can deadlock.
export function planQueue(level) {
  if (level.board.width <= 16 || level.queueVersion === 1) return level;
  const { width, height } = level.board;
  const occupied = new Set(level.blocks.map(b => b.y * width + b.x));
  const outside = new Set(), pending = [];
  for (let y = 0; y < height; y++) for (let x = 0; x < width; x++) {
    if (!occupied.has(y * width + x) && (x === 0 || y === 0 || x === width - 1 || y === height - 1)) {
      outside.add(y * width + x); pending.push([x, y]);
    }
  }
  for (let i = 0; i < pending.length; i++) {
    const [x, y] = pending[i];
    for (const [a, b] of [[x-1,y],[x+1,y],[x,y-1],[x,y+1]]) {
      const key = b * width + a;
      if (a >= 0 && b >= 0 && a < width && b < height && !occupied.has(key) && !outside.has(key)) {
        outside.add(key); pending.push([a, b]);
      }
    }
  }
  const exposed = new Set(level.blocks.filter(({x,y}) =>
    x === 0 || y === 0 || x === width - 1 || y === height - 1 ||
    [[x-1,y],[x+1,y],[x,y-1],[x,y+1]].some(([a,b]) => outside.has(b * width + a))
  ).map(b => b.color));
  const difficulty = level.difficulty || 0;
  const target = level.levelId === 1 ? 2 : [5, 7, 9][difficulty];
  const bait = [], safe = [];
  // Split only real inventory, never add unusable decoy colours or surplus.
  for (const original of level.containers) {
    let count = original.robotCount;
    while (!exposed.has(original.color) && count > 0 && bait.length < target) {
      const amount = Math.min(count, 8);
      bait.push({ color: original.color, robotCount: amount }); count -= amount;
    }
    if (count) safe.push({ color: original.color, robotCount: count,
      ...(original.specialRobotChance === undefined ? {} : { specialRobotChance: original.specialRobotChance }) });
  }
  const lanes = Array.from({length:4}, () => []);
  const trapLane = (level.levelId + Math.floor(level.levelId / 10)) % 4;
  const workLanes = [0,1,2,3].filter(lane => lane !== trapLane);
  lanes[trapLane] = bait;
  let workIndex = 0;
  safe.forEach((c,i) => {
    const lane = i >= 12 && (i + level.levelId) % 8 === 0 ? trapLane : workLanes[workIndex++ % 3];
    lanes[lane].push(c);
  });
  const containers = []; let id = 1;
  for (let depth = 0; depth < Math.max(...lanes.map(l => l.length)); depth++) {
    const row = [];
    for (let lane = 0; lane < 4; lane++) {
      const c = lanes[lane][depth]; if (!c) continue;
      const previous = containers.findLast(p => p.lane === lane);
      const next = { ...c, id: id++, lane, depth, blockedBy: previous ? [previous.id] : [] };
      containers.push(next); if (lane !== trapLane) row.push(next);
    }
    if (difficulty) {
      const used = new Set();
      for (const c of row) {
        if (used.has(c.id)) continue;
        const members = row.filter(p => p.color === c.color && !used.has(p.id)).slice(0, difficulty + 1);
        if (members.length > 1) for (const p of members) { p.group = c.id; used.add(p.id); }
      }
    }
  }
  return { ...level, containers, queueVersion: 1 };
}
