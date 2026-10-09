import { writeFile, mkdir } from 'node:fs/promises';
import { generateLevel } from '../src/levels.js';
import { TOTAL_LEVELS } from '../src/config.js';
import { Game, seededRandom } from '../src/engine.js';
function canClear(level, chance) {
  const game = new Game({ ...level, powerRobotChance: chance }, { random: seededRandom(level.levelId * (chance === 0 ? 1 : 173)) });
  for (let tick = 0; tick < 12000 && game.status === 'playing'; tick++) {
    if (chance === 0) game.gauge = 0;
    if (!game.jobs.length && game.slots.includes(null)) {
      const options = game.queue.filter(c => game.isUnlocked(c));
      const next = options.find(c => game.available(c.color).length) || options[0];
      if (next) game.select(next.id);
    }
    game.update(0.1);
  }
  return game.status === 'won';
}
await mkdir('public/levels', { recursive: true });
for (let id = 1; id <= TOTAL_LEVELS; id++) {
  let level;
  for (let attempt = 0; attempt < 100; attempt++) {
    const candidate = generateLevel(id, attempt);
    if ([0, 0.1, 1].every(chance => canClear(candidate, chance))) { level = candidate; break; }
  }
  if (!level) throw new Error(`Level ${id} has no verified container arrangement.`);
  await writeFile(`public/levels/level-${String(id).padStart(2, '0')}.json`, JSON.stringify(level, null, 2) + '\n');
}
console.log(`Generated ${TOTAL_LEVELS} playable JSON levels.`);
