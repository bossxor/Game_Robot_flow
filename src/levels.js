import { Game, COLORS, seededRandom } from './engine.js';
import { TOTAL_LEVELS } from './config.js';
const INTRO_NAMES = ['첫 출격', '컬러 코어', '작은 정비소', '새로운 통로', '로봇 친구들', '민트 회로', '중앙 제어실', '퍼플 엔진', '겹쳐진 신호', '전력 충전', '공장 가동', '깊은 회로', '교차하는 길', '숨은 코어', '다섯 색 공장', '정밀 조립', '메가 프로젝트', '비밀 설계도', '최종 점검', '로봇 플로우'];
const SECTORS = ['입문 공장', '회로 정비소', '컬러 연구동', '에너지 플랜트', '코어 조립소', '자동화 타워', '심층 연구소', '궤도 정거장', '메가 파운드리', '최종 제어실'];
const PATTERNS = ['shell', 'cross', 'diamond', 'circuit', 'spiral', 'gateway'];
export const LEVEL_NAMES = Array.from({ length: TOTAL_LEVELS }, (_, i) => INTRO_NAMES[i] || `${SECTORS[Math.floor(i / 20)]} · ${String(i % 20 + 1).padStart(2, '0')}`);
// Generate containers by simulating legal one-block removals. The resulting
// order is a constructive solution, even with power robots disabled.
export function containersForBlocks(blocks, width, height, seed = 1, overlap = true) {
  const random = seededRandom(seed);
  const dummy = { levelId: 1, board: { width, height }, blocks, containers: COLORS.map((color, id) => ({ id, color, robotCount: 256, depth: 0 })), slotCount: 5, powerRobotChance: 0, megaRobot: { requiredGauge: 100, blockCount: 8 } };
  const sim = new Game(dummy), containers = [];
  while (sim.remainingBlocks) {
    const options = sim.available();
    const color = options[Math.floor(random() * options.length)].color;
    let count = 0, candidates;
    const limit = 4 + Math.floor(random() * 5);
    while (count < limit && (candidates = sim.available(color)).length) {
      const b = candidates[Math.floor(random() * candidates.length)]; sim.removeBlock(b.id); count++;
    }
    const id = containers.length + 1, row = Math.floor((id - 1) / 3), lane = (id - 1) % 3;
    containers.push({ id, color, robotCount: count, depth: overlap ? row : 0, blockedBy: overlap && row > 0 ? [id - 3] : [], lane });
  }
  return containers;
}
export function generateLevel(id, attempt = 0) {
  if (!Number.isInteger(id) || id < 1 || id > TOTAL_LEVELS) throw new Error('레벨 번호는 1~200이어야 합니다.');
  const random = seededRandom(id * 12371), size = id <= 4 ? 7 : id <= 12 ? 9 : id <= 60 ? 11 : id <= 140 ? 12 : 13;
  const count = id <= 2 ? 2 : id <= 5 ? 3 : id <= 8 ? 4 : 5;
  const colors = COLORS.slice(0, count), blocks = [], center = (size - 1) / 2;
  const pattern = id <= 20 ? 'shell' : PATTERNS[(id - 21) % PATTERNS.length];
  for (let y = 0; y < size; y++) for (let x = 0; x < size; x++) {
    const ring = Math.floor(Math.max(Math.abs(x - center), Math.abs(y - center)));
    const corner = (x === 0 || x === size - 1) && (y < 2 || y > size - 3);
    if (corner || (id % 4 === 0 && x === center && y > center + 1)) continue;
    if (pattern === 'diamond' && Math.abs(x - center) + Math.abs(y - center) > center + 2) continue;
    if (pattern === 'cross' && Math.abs(x - center) > 2 && Math.abs(y - center) > 2) continue;
    if (pattern === 'circuit' && y % 4 === 2 && x > 1 && x < size - 2 && x % 3 !== 0) continue;
    if (pattern === 'gateway' && x % 4 === 2 && y > 1 && y < size - 2 && y % 3 !== 0) continue;
    let color = colors[(Math.floor(center) - ring + id - 1) % count];
    if (pattern === 'spiral') color = colors[(Math.floor(center) - ring + Math.floor((Math.atan2(y - center, x - center) + Math.PI) / (Math.PI / 2)) + id) % count];
    if (id > 3 && random() < 0.25) color = colors[Math.floor(random() * count)];
    blocks.push({ x, y, color, type: 'normal' });
  }
  return { levelId: id, name: LEVEL_NAMES[id - 1], chapter: Math.ceil(id / 20), pattern, board: { width: size, height: size }, blocks,
    containers: containersForBlocks(blocks, size, size, id * 8971 + (attempt + (id === 75 ? 1 : 0)) * 105001, id >= 3), slotCount: 5, powerRobotChance: 0.1,
    megaRobot: { requiredGauge: 100, blockCount: 8 }, revealDepth: id <= 10 ? 100 : id <= 15 ? 2 : 1, reward: 80 + id * 10 };
}
