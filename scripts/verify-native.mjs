import { readFile, writeFile, mkdir } from 'node:fs/promises';
import { spawnSync } from 'node:child_process';
import { join, resolve } from 'node:path';
await mkdir('.build/native-tests', { recursive: true });
const colors = ['red', 'blue', 'yellow', 'green', 'purple', 'orange', 'pink'];
const lines = [];
for (let id = 1; id <= 500; id++) {
  const level = JSON.parse(await readFile(`public/construction-levels/level-${String(id).padStart(2, '0')}.json`, 'utf8'));
  lines.push(`L ${id} ${level.board.width} ${level.board.height} ${level.difficulty||0} ${level.autoFinish?1:0}`);
  for (const b of level.blocks) lines.push(`B ${b.x} ${b.y} ${colors.indexOf(b.color)}`);
  for (const c of level.containers) lines.push(`C ${c.id} ${colors.indexOf(c.color)} ${c.robotCount} ${c.depth} ${c.lane} ${(c.blockedBy || []).join(',') || '-'} ${c.group||0}`);
}
await writeFile('.build/native-tests/levels.txt', lines.join('\n'));
const bin = process.env.JAVA_HOME ? join(process.env.JAVA_HOME, 'bin') : '';
function command(name, args) {
  const executable = bin ? join(bin, name + (process.platform === 'win32' ? '.exe' : '')) : name;
  const result = spawnSync(executable, args, { encoding: 'utf8' });
  if (result.stdout) process.stdout.write(result.stdout);
  if (result.stderr) process.stderr.write(result.stderr);
  if (result.error) throw result.error;
  if (result.status !== 0) process.exit(result.status || 1);
}
command('javac', ['-encoding', 'UTF-8', '-d', '.build/native-tests', 'android/app/src/main/java/com/bossxor/robotflow/NativeGame.java', 'android/app/src/main/java/com/bossxor/robotflow/SecretTapSequence.java', 'android/app/src/main/java/com/bossxor/robotflow/GameLayout.java', 'tests/native/NativeCoreTests.java']);
command('java', ['-cp', '.build/native-tests', 'com.bossxor.robotflow.NativeCoreTests', resolve('.build/native-tests/levels.txt')]);
