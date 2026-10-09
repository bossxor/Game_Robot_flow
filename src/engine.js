export const COLORS = ['red', 'blue', 'yellow', 'green', 'purple'];
export const PALETTE = { red: '#f07865', blue: '#66b6de', yellow: '#f5ca62', green: '#84c997', purple: '#b19add' };
export const COLOR_NAMES = { red: '코랄', blue: '블루', yellow: '옐로', green: '민트', purple: '퍼플' };
export const SYMBOLS = { red: '●', blue: '◆', yellow: '★', green: '▲', purple: '✚' };
export function seededRandom(seed) {
  return () => { seed |= 0; seed = seed + 0x6d2b79f5 | 0; let t = Math.imul(seed ^ seed >>> 15, 1 | seed); t ^= t + Math.imul(t ^ t >>> 7, 61 | t); return ((t ^ t >>> 14) >>> 0) / 4294967296; };
}
export function validateLevel(level) {
  const fail = message => { throw new Error(message); };
  if (!level || !Number.isInteger(level.levelId) || level.levelId < 1) fail('levelId는 양의 정수여야 합니다.');
  const { width, height } = level.board || {};
  if (![width, height].every(n => Number.isInteger(n) && n >= 3 && n <= 16)) fail('필드 크기는 3~16칸이어야 합니다.');
  if (!Array.isArray(level.blocks) || !level.blocks.length) fail('블록이 필요합니다.');
  const positions = new Set();
  for (const b of level.blocks) {
    if (!Number.isInteger(b.x) || !Number.isInteger(b.y) || b.x < 0 || b.x >= width || b.y < 0 || b.y >= height || !COLORS.includes(b.color) || (b.type && b.type !== 'normal')) fail('블록 좌표, 색상 또는 타입이 올바르지 않습니다.');
    const key = `${b.x},${b.y}`;
    if (positions.has(key)) fail('같은 좌표에 블록이 중복되었습니다.');
    positions.add(key);
  }
  if (!Number.isInteger(level.slotCount) || level.slotCount < 1 || level.slotCount > 6) fail('슬롯 수는 1~6이어야 합니다.');
  if (!Number.isFinite(level.powerRobotChance) || level.powerRobotChance < 0 || level.powerRobotChance > 1) fail('파워 로봇 확률은 0~1이어야 합니다.');
  if (!level.megaRobot || !Number.isInteger(level.megaRobot.requiredGauge) || level.megaRobot.requiredGauge < 1 || !Number.isInteger(level.megaRobot.blockCount) || level.megaRobot.blockCount < 1 || level.megaRobot.blockCount > 8) fail('메가 로봇 설정을 확인하세요.');
  if (!Array.isArray(level.containers) || !level.containers.length || level.containers.length > 300) fail('컨테이너는 1~300개여야 합니다.');
  const ids = new Set(level.containers.map(c => c.id));
  if (ids.size !== level.containers.length) fail('컨테이너 ID가 중복되었습니다.');
  for (const c of level.containers) {
    if (!Number.isInteger(c.id) || !COLORS.includes(c.color) || !Number.isInteger(c.robotCount) || c.robotCount < 1 || c.robotCount > 256 || !Number.isInteger(c.depth) || c.depth < 0 || c.depth > 100) fail('컨테이너 데이터가 올바르지 않습니다.');
    if (c.blockedBy && (!Array.isArray(c.blockedBy) || c.blockedBy.some(id => id === c.id || !ids.has(id)))) fail('blockedBy에 잘못된 ID가 있습니다.');
  }
  const visiting = new Set(), visited = new Set();
  const visit = id => {
    if (visiting.has(id)) fail('컨테이너 잠금 관계에 순환이 있습니다.');
    if (visited.has(id)) return;
    visiting.add(id);
    for (const parent of level.containers.find(c => c.id === id).blockedBy || []) visit(parent);
    visiting.delete(id); visited.add(id);
  };
  ids.forEach(visit);
  for (const color of COLORS) {
    const demand = level.blocks.filter(b => b.color === color).length;
    const supply = level.containers.filter(c => c.color === color).reduce((n, c) => n + c.robotCount, 0);
    if (supply < demand) fail(`${COLOR_NAMES[color]} 로봇 수가 블록 수보다 적습니다.`);
  }
  return level;
}

export class Game {
  constructor(level, { random = Math.random, onEvent = () => {} } = {}) {
    validateLevel(level);
    this.level = structuredClone(level);
    this.width = level.board.width; this.height = level.board.height;
    this.blocks = level.blocks.map((b, id) => ({ ...b, id, removed: false, reserved: false }));
    this.queue = level.containers.map(c => ({ ...c, remaining: c.robotCount, taken: false, mega: false }));
    this.slots = Array(level.slotCount).fill(null);
    this.jobs = []; this.gauge = 0; this.time = 0; this.moves = 0; this.collected = 0;
    this.status = 'playing'; this.paused = false; this.random = random; this.onEvent = onEvent;
    this.spawnClock = 0; this.jobId = 0; this.colorClearUsed = false;
    this.completedColors = new Set();
    this.refreshReachable();
  }
  get remainingBlocks() { return this.blocks.filter(b => !b.removed).length; }
  isUnlocked(c) {
    if (c.taken) return false;
    if (c.blockedBy) return c.blockedBy.every(id => this.queue.find(p => p.id === id)?.taken);
    return !this.queue.some(p => !p.taken && p.depth < c.depth);
  }
  isVisible(c) {
    const depth = Math.min(...this.queue.filter(p => !p.taken).map(p => p.depth));
    return this.isUnlocked(c) || c.depth - depth <= (this.level.revealDepth ?? 100);
  }
  slotOrigin(index) { return { x: (index + 0.5) * (this.width + 1) / this.slots.length - 0.5, y: this.height + 2.6 }; }
  // A padded grid joins all four outside edges. Only empty cells are walkable.
  flood(start = { x: -1, y: this.height + 1 }) {
    const occupied = new Set(this.blocks.filter(b => !b.removed).map(b => `${b.x},${b.y}`));
    const key = p => `${p.x},${p.y}`;
    const queue = [start], parents = new Map([[key(start), null]]), distances = new Map([[key(start), 0]]);
    for (let i = 0; i < queue.length; i++) {
      const p = queue[i];
      for (const q of [{ x: p.x + 1, y: p.y }, { x: p.x - 1, y: p.y }, { x: p.x, y: p.y + 1 }, { x: p.x, y: p.y - 1 }]) {
        if (q.x < -1 || q.x > this.width || q.y < -1 || q.y > this.height + 2 || occupied.has(key(q)) || parents.has(key(q))) continue;
        parents.set(key(q), p); distances.set(key(q), distances.get(key(p)) + 1); queue.push(q);
      }
    }
    return { parents, distances };
  }
  neighbors(b) { return [{ x: b.x - 1, y: b.y }, { x: b.x + 1, y: b.y }, { x: b.x, y: b.y - 1 }, { x: b.x, y: b.y + 1 }]; }
  refreshReachable() {
    const { parents } = this.flood();
    for (const b of this.blocks) b.reachable = !b.removed && this.neighbors(b).some(p => parents.has(`${p.x},${p.y}`));
  }
  available(color) { return this.blocks.filter(b => !b.removed && !b.reserved && b.reachable && (!color || b.color === color)); }
  addGauge(n) { this.gauge = Math.min(this.level.megaRobot.requiredGauge, this.gauge + n); }
  select(id) {
    if (this.status !== 'playing' || this.paused) return false;
    const c = this.queue.find(c => c.id === id), index = this.slots.indexOf(null);
    if (!c || !this.isUnlocked(c) || index < 0) return false;
    c.taken = true; c.mega = this.gauge >= this.level.megaRobot.requiredGauge;
    if (c.mega) this.gauge = 0;
    this.slots[index] = c; this.moves++;
    if (this.available(c.color).length) this.addGauge(15);
    this.onEvent({ type: 'select', color: c.color });
    this.spawnClock = 0.15;
    return true;
  }
  spawn(c, index) {
    if (c.remaining <= 0 || this.jobs.filter(j => j.containerId === c.id).length >= 6) return;
    const origin = this.slotOrigin(index);
    let start = { x: Math.max(-1, Math.min(this.width, Math.round(origin.x))), y: this.height + 2 };
    let flood = this.flood(start), targets = this.available(c.color);
    if (!targets.length) return;
    const type = c.mega ? 'mega' : this.random() < (c.specialRobotChance ?? this.level.powerRobotChance) ? 'power' : 'mini';
    const capacity = type === 'mega' ? this.level.megaRobot.blockCount : type === 'power' ? 3 : 1;
    const route = [{ ...start }], selected = [];
    for (let k = 0; k < capacity && targets.length; k++) {
      const options = targets.flatMap(b => this.neighbors(b).filter(p => flood.distances.has(`${p.x},${p.y}`)).map(p => ({ b, p, d: flood.distances.get(`${p.x},${p.y}`), tie: this.random() })));
      options.sort((a, b) => a.d - b.d || a.tie - b.tie);
      if (!options.length) break;
      const { b, p } = options[0];
      const path = []; let cursor = p;
      while (cursor) { path.push(cursor); cursor = flood.parents.get(`${cursor.x},${cursor.y}`); }
      route.push(...path.reverse().slice(1), { x: b.x, y: b.y, collect: b.id }, { ...p });
      b.reserved = true; selected.push(b.id);
      targets = targets.filter(t => t.id !== b.id); start = p; flood = this.flood(start);
    }
    if (!selected.length) return;
    const returnStart = { x: Math.max(-1, Math.min(this.width, Math.round(origin.x))), y: this.height + 2 };
    let cursor = returnStart; const returnPath = [];
    while (cursor) { returnPath.push(cursor); cursor = flood.parents.get(`${cursor.x},${cursor.y}`); }
    route.push(...returnPath.reverse().slice(1), origin);
    c.remaining--; c.mega = false; c.hasWorked = true;
    this.jobs.push({ id: this.jobId++, containerId: c.id, color: c.color, type, x: origin.x, y: origin.y, route, step: 0, selected, collected: 0 });
    if (type === 'power') this.addGauge(5);
    this.onEvent({ type, color: c.color });
  }
  removeBlock(id) {
    const b = this.blocks[id];
    if (!b || b.removed) return;
    b.removed = true; b.reserved = false; this.collected++;
    this.onEvent({ type: 'collect', block: { ...b } });
    if (!this.blocks.some(other => !other.removed && other.color === b.color)) {
      this.completedColors.add(b.color);
      // Bonus robots may finish a color before every capsule is needed.
      // Recycle those surplus capsules so they cannot introduce a false deadlock.
      this.queue.filter(c => !c.taken && c.color === b.color).forEach(c => { c.taken = true; c.retired = true; });
    }
    this.refreshReachable();
  }
  update(dt) {
    if (this.paused || this.status !== 'playing') return;
    dt = Math.max(0, Math.min(dt, 0.1)); this.time += dt;
    for (const j of this.jobs) {
      let travel = dt * (j.type === 'mega' ? 10 : 14);
      while (travel > 0 && j.step < j.route.length) {
        const p = j.route[j.step], distance = Math.hypot(p.x - j.x, p.y - j.y);
        if (distance <= travel) {
          j.x = p.x; j.y = p.y; travel -= distance; j.step++;
          if (p.collect !== undefined) { this.removeBlock(p.collect); j.collected++; }
        } else { j.x += (p.x - j.x) / distance * travel; j.y += (p.y - j.y) / distance * travel; travel = 0; }
      }
    }
    this.jobs = this.jobs.filter(j => j.step < j.route.length);
    for (let i = 0; i < this.slots.length; i++) {
      const c = this.slots[i];
      if (c && !this.jobs.some(j => j.containerId === c.id) && (c.remaining === 0 || this.completedColors.has(c.color) || this.remainingBlocks === 0)) {
        this.slots[i] = null; this.addGauge(10); this.onEvent({ type: 'complete', color: c.color });
      }
    }
    this.spawnClock += dt;
    if (this.spawnClock >= 0.13) {
      this.spawnClock = 0;
      this.slots.forEach((c, i) => { if (c) this.spawn(c, i); });
    }
    if (this.remainingBlocks === 0 && this.jobs.length === 0) this.finish('won');
    else if (!this.jobs.length) {
      const canWork = this.slots.some(c => c && c.remaining > 0 && this.available(c.color).length);
      const canSelect = this.slots.includes(null) && this.queue.some(c => this.isUnlocked(c));
      if (!canWork && !canSelect) this.finish('lost');
    }
  }
  finish(status) { this.status = status; this.onEvent({ type: status }); }
  revive() { if (this.status === 'lost') this.status = 'playing'; }
  extraSlot() {
    if (this.paused || this.status === 'won' || this.slots.length >= 6) return false;
    this.slots.push(null); this.revive(); return true;
  }
  clearColor(color) {
    if (this.paused || this.status === 'won' || this.colorClearUsed || !this.blocks.some(b => !b.removed && b.color === color)) return false;
    this.blocks.filter(b => !b.removed && b.color === color).forEach(b => this.removeBlock(b.id));
    this.slots.forEach(c => { if (c?.color === color) c.hasWorked = true; });
    this.colorClearUsed = true; this.revive(); return true;
  }
  shuffle() {
    if (this.paused || this.status === 'won') return false;
    const remaining = this.queue.filter(c => !c.taken);
    if (remaining.length < 2) return false;
    const contents = remaining.map(c => ({ color: c.color, remaining: c.remaining, robotCount: c.robotCount, specialRobotChance: c.specialRobotChance }));
    for (let i = contents.length - 1; i > 0; i--) { const j = Math.floor(this.random() * (i + 1)); [contents[i], contents[j]] = [contents[j], contents[i]]; }
    remaining.forEach((c, i) => Object.assign(c, contents[i])); this.revive(); return true;
  }
  pullOut(id) {
    if (this.paused || this.status === 'won') return false;
    const c = this.queue.find(c => c.id === id);
    if (!c || c.taken || this.isUnlocked(c)) return false;
    c.blockedBy = []; c.depth = 0; this.revive(); return true;
  }
}
