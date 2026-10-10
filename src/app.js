import { Game, COLORS, PALETTE, COLOR_NAMES, SYMBOLS, validateLevel } from './engine.js';
import { LEVEL_NAMES, containersForBlocks } from './levels.js';
import { loadSave, storeSave, recordWin, defaults } from './storage.js';
import { AudioSystem } from './audio.js';
import { Renderer } from './renderer.js';
import { TOTAL_LEVELS, STAGES_PER_PAGE } from './config.js';
const app = document.querySelector('#app');
let save = loadSave(), audio = new AudioSystem(save.settings), game = null, renderer = null, screen = 'splash', speed = 1, mode = null, custom = false, reward = 0;
let last = performance.now(), uiClock = 0, toastTimer, loadToken = 0;
const escape = value => String(value).replace(/[&<>"']/g, c => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));
const icon = `<img src="./public/robot-flow-icon.png" alt="" width="36" height="36" style="border-radius:22%">`;
const robotArt = `<div class="hero-bot"><span class="antenna"></span><div class="bot-face"><i></i><i></i></div><span class="bot-mouth"></span><b class="arm left"></b><b class="arm right"></b><b class="foot left"></b><b class="foot right"></b></div>`;
const button = (label, action, cls = '', extra = '') => `<button class="${cls}" data-action="${action}" ${extra}>${label}</button>`;
function persist() { if (!storeSave(save)) toast('저장 공간을 사용할 수 없습니다. 이번 실행 동안만 유지됩니다.'); }
function toast(message) { const el = document.querySelector('#toast'); el.textContent = message; el.classList.add('show'); clearTimeout(toastTimer); toastTimer = setTimeout(() => el.classList.remove('show'), 2800); }
function header() { return `<header class="topbar"><a class="brand" href="#" data-action="home">${icon}<span>ROBO<span class="brand-light">RELAY</span><small>ROBOT RECOVERY LAB</small></span></a><div class="currency"><span>◈</span> ${save.coins.toLocaleString()} <small>COINS</small></div></header>`; }
function home() {
  loadToken++; screen = 'home'; game = null; renderer = null; mode = null;
  app.innerHTML = `${header()}<main class="home"><section class="home-copy"><div class="eyebrow"><span class="status-dot"></span> FACTORY ONLINE · SEASON 01</div><h1>작은 로봇들,<br>커다란 <em>가능성.</em></h1><p>색을 연결하고, 새로운 길을 열어보세요.<br>당신의 선택이 로봇 공장을 움직입니다.</p><div class="play-card"><div><small>CONTINUE YOUR JOURNEY</small><h2>STAGE ${String(save.lastLevel).padStart(2, '0')}</h2><span>${LEVEL_NAMES[save.lastLevel - 1]}</span></div>${button('플레이 <span>↗</span>', 'play', 'primary')}</div><div class="home-links">${button('▦ <span>스테이지</span>', 'stages')}${button('♙ <span>로봇 연구소</span>', 'robots')}${button('✦ <span>아이템</span>', 'items')}${button('⚙ <span>설정</span>', 'settings')}</div></section><section class="factory-art" aria-label="로봇 정비소"><div class="art-grid"></div><span class="art-label">UNIT 001 / MINI ROBOT</span><div class="orbit orbit-one"></div><div class="orbit orbit-two"></div>${robotArt}<div class="mini-bot one">${icon}</div><div class="mini-bot two">${icon}</div><div class="pedestal"></div><div class="art-caption"><span class="status-dot"></span> 모든 로봇, 출격 준비 완료 <span>100%</span></div><span class="art-number">01</span></section><footer class="home-footer"><span>생각은 전략적으로. 움직임은 경쾌하게.</span>${button('레벨 에디터 ↗', 'editor', 'text-button')}</footer></main>`;
}
async function startLevel(id, level = null) {
  const token = ++loadToken;
  if (!level && (id < 1 || id > save.highestUnlockedLevel)) return;
  closeModal(); screen = 'loading'; app.innerHTML = `<main class="splash">${icon}<p>로봇 공장을 준비하고 있어요…</p></main>`;
  try {
    if (!level) { const response = await fetch(`./public/levels/level-${String(id).padStart(2, '0')}.json`); if (!response.ok) throw new Error('레벨 파일을 불러올 수 없습니다.'); level = await response.json(); }
    validateLevel(level); if (token !== loadToken) return;
    custom = !!arguments[1]; speed = 1; mode = null; reward = 0;
    game = new Game(level, { onEvent: onGameEvent }); screen = 'game';
    if (!custom) { save.lastLevel = id; persist(); }
    app.innerHTML = `${header()}<main class="game-layout"><aside class="mission-panel"><div class="eyebrow">RECOVERY MISSION</div><h1>STAGE ${String(id).padStart(2, '0')}</h1><p>${escape(level.name || '커스텀 스테이지')}</p><div class="mission-robot">${robotArt}</div><h3>색을 맞추고 길을 열어주세요.</h3><p class="muted">밝은 테두리의 블록에 접근할 수 있어요. 로봇은 각 슬롯에서 출발해 블록을 회수하고 돌아옵니다.</p><div class="legend">${COLORS.map(c => `<span><i style="background:${PALETTE[c]}">${SYMBOLS[c]}</i>${COLOR_NAMES[c]}</span>`).join('')}</div><div class="tip"><small>LAB NOTE</small><p>슬롯이 가득 차도 로봇이 작업 중이면 괜찮아요. 아무도 작업할 수 없을 때 공장이 멈춥니다.</p></div>${button('← 메인 화면', 'home', 'text-button')}</aside><section class="game-main"><div class="game-heading"><div><small>STAGE ${String(id).padStart(2, '0')}</small><h2>${escape(level.name || '커스텀 스테이지')}</h2></div><div class="game-controls">${button('1×', 'speed', 'icon-button', 'id="speed-button" aria-label="2배속 전환"')}${button('↺', 'restart', 'icon-button', 'aria-label="다시 시작"')}${button('Ⅱ', 'pause', 'icon-button', 'aria-label="일시정지"')}</div></div><div class="progress-label"><span>회수 진행률</span><strong id="progress-text">0 / ${game.blocks.length}</strong></div><div class="progress-track"><div id="progress-bar"></div></div><div class="board-wrap"><canvas id="board" role="img" aria-label="블록 필드와 로봇 이동 애니메이션"></canvas><div id="mega-callout" class="mega-callout">MEGA ROBOT<br><small>대형 로봇 출격!</small></div></div><div class="slot-caption"><span>ACTIVE SLOTS <b id="slot-count">0 / 5</b></span><span id="work-status" role="status" aria-live="polite">출격할 컨테이너를 선택하세요</span></div><div class="mega-meter"><span>✦ <b>MEGA</b></span><div class="mega-track"><div id="mega-bar"></div></div><strong id="mega-value">0%</strong></div><div class="queue-heading"><h3>대기 컨테이너</h3><span id="queue-count"></span></div><div id="mode-hint" class="mode-hint" role="status"></div><div id="container-queue" class="container-queue"></div><div class="booster-heading"><span>BOOSTERS</span><small>필요할 때, 한 번의 도움</small></div><div id="boosters" class="boosters"></div><div class="game-foot"><span id="timer">00:00</span><span>ROBO RELAY / RECOVERY LAB</span><span id="moves">0회 출격</span></div></section><aside class="right-panel"><div class="eyebrow">HOW TO PLAY</div><h2>한 번의 선택,<br>새로운 통로.</h2><ol><li><b>색상 확인</b><span>접근 가능한 블록의 색을 살펴보세요.</span></li><li><b>컨테이너 선택</b><span>앞에 놓인 컨테이너부터 출격할 수 있어요.</span></li><li><b>슬롯 관리</b><span>작업할 곳이 없는 색은 슬롯에서 기다려요.</span></li></ol><div class="rank-card"><span>01 / MINI</span><strong>1 BLOCK</strong><p>작지만 확실하게</p></div><div class="rank-card"><span>02 / POWER</span><strong>3 BLOCKS</strong><p>10% 확률의 특별한 만남</p></div><div class="rank-card mega"><span>03 / MEGA</span><strong>8 BLOCKS</strong><p>게이지 100%, 다음 선택에 출격</p></div></aside></main>`;
    renderer = new Renderer(document.querySelector('#board'), game); updateUI(); renderer.draw();
    if (!save.tutorialCompleted) showTutorial();
  } catch (error) { if (token === loadToken) { home(); toast(error.message); } }
}
function onGameEvent(e) {
  audio.effect(e.type); renderer?.event(e);
  if (e.type === 'mega') { const el = document.querySelector('#mega-callout'); el?.classList.add('visible'); setTimeout(() => el?.classList.remove('visible'), 1600); }
  if (e.type === 'won') {
    if (!custom) { reward = recordWin(save, game.level.levelId, game.time, game.moves, game.level.reward || 100); persist(); }
    mode = null; showResult();
  }
  if (e.type === 'lost') { mode = null; showResult(); }
}
function updateUI() {
  if (!game || screen !== 'game') return;
  const percent = Math.round(game.collected / game.blocks.length * 100), gauge = Math.round(game.gauge / game.level.megaRobot.requiredGauge * 100);
  document.querySelector('#progress-text').textContent = `${game.collected} / ${game.blocks.length} · ${percent}%`;
  document.querySelector('#progress-bar').style.width = `${percent}%`;
  document.querySelector('#mega-bar').style.width = `${gauge}%`;
  document.querySelector('#mega-value').textContent = gauge >= 100 ? 'READY' : `${gauge}%`;
  document.querySelector('.mega-meter').classList.toggle('ready', gauge >= 100);
  document.querySelector('#slot-count').textContent = `${game.slots.filter(Boolean).length} / ${game.slots.length}`;
  document.querySelector('#work-status').textContent = game.jobs.length ? `${game.jobs.length}기 로봇 작업 중` : game.slots.some(Boolean) ? '새로운 통로를 기다리는 중' : '출격할 컨테이너를 선택하세요';
  document.querySelector('#timer').textContent = `${String(Math.floor(game.time / 60)).padStart(2, '0')}:${String(Math.floor(game.time % 60)).padStart(2, '0')}`;
  document.querySelector('#moves').textContent = `${game.moves}회 출격`;
  const remaining = game.queue.filter(c => !c.taken); document.querySelector('#queue-count').textContent = `${remaining.length}개 남음`;
  const queueHTML = remaining.map(c => {
    const unlocked = game.isUnlocked(c), visible = game.isVisible(c), selectable = mode === 'pullOut' ? !unlocked : unlocked && game.slots.includes(null);
    return `<button class="container ${unlocked ? 'unlocked' : 'locked'} ${mode === 'pullOut' && !unlocked ? 'pull-target' : ''}" data-action="container" data-id="${c.id}" style="--color:${visible ? PALETTE[c.color] : '#bcc5b7'};--depth:${c.depth}" ${!selectable ? 'disabled' : ''} aria-label="${visible ? COLOR_NAMES[c.color] + ' 로봇 ' + c.remaining + '기' : '숨겨진 컨테이너'}${!unlocked ? ', 잠김' : ''}"><span class="container-top">${unlocked ? 'READY' : '⌁ DEPTH ' + c.depth}</span><span class="container-face">${visible ? SYMBOLS[c.color] : '?'}</span><strong>${visible ? c.remaining : '?'}</strong><small>${visible ? COLOR_NAMES[c.color] : '미공개'}</small></button>`;
  }).join('');
  const queueEl = document.querySelector('#container-queue'); if (queueEl._rendered !== queueHTML) { queueEl.innerHTML = queueHTML; queueEl._rendered = queueHTML; }
  const definitions = [['extraSlot', '⊞', '슬롯 추가'], ['colorClear', '◉', '컬러 클리어'], ['shuffle', '⇄', '셔플'], ['pullOut', '↥', '꺼내기']];
  const boostersHTML = definitions.map(([key, glyph, label]) => button(`<span class="booster-icon">${glyph}</span><span>${label}</span><b>${save.boosters[key]}</b>`, `booster-${key}`, mode === key ? 'selected' : '', !save.boosters[key] || game.paused || game.status === 'won' || (key === 'extraSlot' && game.slots.length >= 6) || (key === 'colorClear' && game.colorClearUsed) ? 'disabled' : '')).join('');
  const boosterEl = document.querySelector('#boosters'); if (boosterEl._rendered !== boostersHTML) { boosterEl.innerHTML = boostersHTML; boosterEl._rendered = boostersHTML; }
  document.querySelector('#mode-hint').innerHTML = mode === 'pullOut' ? `뒤쪽 컨테이너를 선택해 앞으로 가져오세요. ${button('취소', 'cancel-mode', 'text-button')}` : '';
}
function modal(title, content, kind = '') {
  closeModal(); const layer = document.createElement('div'); layer.id = 'modal-layer'; layer.className = `modal-layer ${kind}`;
  layer.innerHTML = `<section class="modal" role="dialog" aria-modal="true" aria-labelledby="modal-title" tabindex="-1"><div class="modal-heading"><h2 id="modal-title">${title}</h2>${button('×', 'close', 'icon-button', 'aria-label="닫기"')}</div>${content}</section>`;
  document.body.append(layer); layer.querySelector('.modal').focus();
}
function closeModal() { document.querySelector('#modal-layer')?.remove(); }
function showTutorial() {
  game.paused = true;
  modal('첫 출격을 환영합니다', `<p>같은 색 로봇이 블록을 회수합니다. 블록이 사라지면 안쪽으로 들어가는 길이 열려요.</p><div class="tutorial-row"><span>① 색상 확인</span><span>② 컨테이너 선택</span><span>③ 로봇 출격</span></div><p>빈 슬롯은 5개. 파워 로봇은 최대 3개, 메가 로봇은 최대 8개의 블록을 회수해요.</p>${button('알겠어요, 시작!', 'tutorial-done', 'primary full')}`);
}
function showResult() {
  const won = game.status === 'won';
  modal(won ? '회수 미션 완료!' : '공장이 잠시 멈췄어요', won ? `<div class="result-icon">✦</div><p>${game.level.levelId === TOTAL_LEVELS && !custom ? '200개 미션을 모두 완수했어요!' : '숨겨진 로봇이 완성되었어요.'}</p><div class="result-stats"><span><small>플레이 시간</small><b>${Math.floor(game.time)}초</b></span><span><small>컨테이너 출격</small><b>${game.moves}회</b></span><span><small>획득 코인</small><b>+${reward}</b></span></div>${button(custom ? '다시 테스트' : game.level.levelId === TOTAL_LEVELS ? '스테이지 선택' : '다음 스테이지 →', custom ? 'restart' : game.level.levelId === TOTAL_LEVELS ? 'stages' : 'next', 'primary full')}${button('메인 화면', 'home', 'secondary full')}` : `<p>작업할 수 있는 로봇이 없어요. 컨테이너의 선택 순서를 바꾸거나 부스터로 길을 열어보세요.</p>${button('다시 시작', 'restart', 'primary full')}${button('부스터 사용하기', 'rescue', 'secondary full')}${button('메인 화면', 'home', 'text-button full')}`, 'result');
}
function stages(page = Math.floor((save.lastLevel - 1) / STAGES_PER_PAGE)) {
  if (game) game.paused = true;
  const pageCount = Math.ceil(TOTAL_LEVELS / STAGES_PER_PAGE);
  page = Math.max(0, Math.min(pageCount - 1, page));
  const start = page * STAGES_PER_PAGE;
  modal('스테이지 선택', `<p class="muted">${Object.keys(save.cleared).length} / ${TOTAL_LEVELS} 미션 완료</p><div class="stage-pagination">${button('←', 'stage-page', 'icon-button', `data-page="${page - 1}" aria-label="이전 챕터" ${page === 0 ? 'disabled' : ''}`)}<strong>CHAPTER ${page + 1} <small>${start + 1}–${Math.min(start + STAGES_PER_PAGE, TOTAL_LEVELS)}</small></strong>${button('→', 'stage-page', 'icon-button', `data-page="${page + 1}" aria-label="다음 챕터" ${page === pageCount - 1 ? 'disabled' : ''}`)}</div><div class="stages">${LEVEL_NAMES.slice(start, start + STAGES_PER_PAGE).map((name, i) => { const id = start + i + 1, unlocked = id <= save.highestUnlockedLevel, record = save.cleared[id]; return button(`<strong>${String(id).padStart(2, '0')}</strong><span>${name}</span><small>${record ? '✓ ' + Math.round(record.bestTime) + '초' : unlocked ? '플레이 →' : '잠김'}</small>`, 'stage', record ? 'cleared' : '', `data-id="${id}" ${!unlocked ? 'disabled' : ''}`); }).join('')}</div>`);
}
function settings() {
  if (game) game.paused = true;
  modal('설정', `<div class="settings-list">${[['music', '배경음악'], ['soundEffect', '효과음'], ['vibration', '진동']].map(([key, name]) => `<label>${name}<input type="checkbox" data-setting="${key}" ${save.settings[key] ? 'checked' : ''}></label>`).join('')}${[['musicVolume', '음악 볼륨'], ['sfxVolume', '효과음 볼륨']].map(([key, name]) => `<label>${name}<input type="range" min="0" max="1" step="0.05" value="${save.settings[key]}" data-setting="${key}" aria-label="${name}"></label>`).join('')}<label>언어<span>한국어</span></label></div><p class="muted">설정과 스테이지 진행은 기기에 자동 저장됩니다.</p>${button('데이터 초기화', 'reset-confirm', 'danger full')}`);
}
function pause() {
  if (!game || game.status !== 'playing') return;
  game.paused = true;
  modal('잠시 쉬어가요', `${button('계속하기', 'resume', 'primary full')}${button('다시 시작', 'restart', 'secondary full')}${button('음악 · 효과음 · 진동 설정', 'settings', 'secondary full')}${button('메인 화면으로', 'home', 'text-button full')}`);
}
function items() {
  if (game) game.paused = true;
  modal('부스터 보관함', `<p class="muted">보유 코인 ${save.coins} ◈ · 구매한 아이템은 자동 저장됩니다.</p><div class="item-list">${[['extraSlot', '⊞', '슬롯 추가', '이번 스테이지의 슬롯을 6개로 늘려요.', 100], ['colorClear', '◉', '컬러 클리어', '선택한 색 블록을 전부 회수해요. 판당 1회.', 200], ['shuffle', '⇄', '셔플', '대기 컨테이너의 순서를 다시 섞어요.', 80], ['pullOut', '↥', '꺼내기', '원하는 뒤쪽 컨테이너를 앞으로 가져와요.', 120]].map(([key, glyph, name, desc, price]) => `<article><span>${glyph}</span><div><h3>${name} <small>보유 ${save.boosters[key]}</small></h3><p>${desc}</p></div>${button(`${price} ◈`, 'buy', 'secondary', `data-key="${key}" data-price="${price}" ${save.coins < price ? 'disabled' : ''}`)}</article>`).join('')}</div>`);
}
function robots() {
  modal('로봇 연구소', `<p class="muted">우리 공장의 든든한 회수 팀</p><div class="robot-roster">${[['MINI', '미니 로봇', 1, '모든 컨테이너에서 출격하는 기본 로봇.'], ['POWER', '파워 로봇', 3, '약 10% 확률로 등장하는 특별한 로봇.'], ['MEGA', '메가 로봇', 8, '게이지 100%일 때 다음 선택한 색으로 출격.']].map(([code, name, count, desc]) => `<article>${icon}<small>${code}</small><h3>${name}</h3><b>최대 ${count}개 회수</b><p>${desc}</p></article>`).join('')}</div>`);
}
function useBooster(key, value) {
  if (!game || !save.boosters[key]) { toast('보유한 부스터가 없어요.'); return; }
  const success = key === 'extraSlot' ? game.extraSlot() : key === 'colorClear' ? game.clearColor(value) : key === 'shuffle' ? game.shuffle() : game.pullOut(value);
  if (!success) { toast('지금은 사용할 수 없어요.'); return; }
  save.boosters[key]--; persist(); mode = null; closeModal(); updateUI(); toast('부스터를 사용했어요.');
}
function editor() {
  if (game) game.paused = true;
  const level = game?.level || { levelId: 1, name: '나만의 공장', board: { width: 7, height: 7 }, blocks: Array.from({ length: 49 }, (_, i) => ({ x: i % 7, y: Math.floor(i / 7), color: COLORS[Math.floor(i / 7) % 5], type: 'normal' })), slotCount: 5, powerRobotChance: 0.1, megaRobot: { requiredGauge: 100, blockCount: 8 } };
  const copy = structuredClone(level); copy.containers ||= containersForBlocks(copy.blocks, copy.board.width, copy.board.height);
  modal('레벨 JSON 에디터', `<p class="muted">좌표·색·겹침 관계를 수정하고 즉시 플레이해보세요. 자동 구성은 접근 가능한 순서로 컨테이너를 생성합니다.</p><textarea id="level-json" spellcheck="false" aria-label="레벨 JSON">${escape(JSON.stringify(copy, null, 2))}</textarea><label class="file-label">JSON 불러오기 <input id="import-level" type="file" accept=".json,application/json"></label><div class="editor-actions">${button('컨테이너 자동 구성', 'editor-generate', 'secondary')}${button('JSON 저장', 'editor-export', 'secondary')}${button('테스트 플레이 →', 'editor-play', 'primary')}</div>`, 'editor');
}
function editorData() { return JSON.parse(document.querySelector('#level-json').value); }
document.addEventListener('click', async e => {
  const target = e.target.closest('[data-action]'); if (!target || target.disabled) return;
  e.preventDefault(); audio.unlock(); audio.effect('click'); const action = target.dataset.action;
  if (action === 'home') { closeModal(); home(); }
  else if (action === 'play') await startLevel(save.lastLevel);
  else if (action === 'stage') await startLevel(Number(target.dataset.id));
  else if (action === 'stages') stages();
  else if (action === 'stage-page') stages(Number(target.dataset.page));
  else if (action === 'settings') settings();
  else if (action === 'items') items();
  else if (action === 'robots') robots();
  else if (action === 'editor') editor();
  else if (action === 'pause') pause();
  else if (action === 'restart') { const level = custom ? game.level : null; await startLevel(game.level.levelId, level); }
  else if (action === 'next') await startLevel(Math.min(TOTAL_LEVELS, game.level.levelId + 1));
  else if (action === 'close' || action === 'resume' || action === 'tutorial-done') {
    if (action === 'tutorial-done' || (!save.tutorialCompleted && game)) { save.tutorialCompleted = true; persist(); }
    closeModal(); if (game) game.paused = false;
  }
  else if (action === 'rescue') { closeModal(); toast('아래 부스터를 선택하세요.'); }
  else if (action === 'speed') { speed = speed === 1 ? 2 : 1; target.textContent = `${speed}×`; }
  else if (action === 'container') {
    if (mode === 'pullOut') useBooster('pullOut', Number(target.dataset.id));
    else if (!game.select(Number(target.dataset.id))) toast('슬롯과 컨테이너 잠금을 확인해주세요.');
    updateUI();
  }
  else if (action === 'cancel-mode') { mode = null; updateUI(); }
  else if (action.startsWith('booster-')) {
    const key = action.slice(8);
    if (key === 'colorClear') modal('어떤 색을 회수할까요?', `<div class="color-picker">${COLORS.filter(c => game.blocks.some(b => !b.removed && b.color === c)).map(c => button(`${SYMBOLS[c]} ${COLOR_NAMES[c]}`, 'clear-color', '', `data-color="${c}" style="--color:${PALETTE[c]}"`)).join('')}</div><p class="muted">스테이지당 한 번 사용할 수 있어요.</p>`);
    else if (key === 'pullOut') { if (!game.queue.some(c => !c.taken && !game.isUnlocked(c))) toast('뒤쪽에 잠긴 컨테이너가 없어요.'); else { mode = mode === key ? null : key; updateUI(); } }
    else useBooster(key);
  }
  else if (action === 'clear-color') useBooster('colorClear', target.dataset.color);
  else if (action === 'buy') { const price = Number(target.dataset.price), key = target.dataset.key; if (save.coins >= price) { save.coins -= price; save.boosters[key]++; persist(); items(); toast('아이템을 구매했어요.'); } }
  else if (action === 'reset-confirm') modal('진행 데이터를 초기화할까요?', `<p>스테이지 기록, 코인, 아이템, 설정이 처음 상태로 돌아갑니다.</p>${button('초기화', 'reset', 'danger full')}${button('취소', 'settings', 'secondary full')}`);
  else if (action === 'reset') { save = defaults(); audio.settings = save.settings; persist(); closeModal(); home(); toast('데이터를 초기화했어요.'); }
  else if (action.startsWith('editor-')) {
    try {
      const level = editorData();
      if (action === 'editor-generate') {
        // Validate board input before constructing the automatic solution.
        validateLevel({ ...level, containers: COLORS.map((color, id) => ({ id, color, robotCount: 256, depth: 0 })) });
        level.containers = containersForBlocks(level.blocks, level.board.width, level.board.height); validateLevel(level);
        document.querySelector('#level-json').value = JSON.stringify(level, null, 2); toast('컨테이너를 자동 구성했어요.');
      } else {
        validateLevel(level);
        if (action === 'editor-play') await startLevel(level.levelId, level);
        else {
          if (window.Android?.exportLevel) { window.Android.exportLevel(JSON.stringify(level, null, 2)); toast('Android 공유 메뉴에서 JSON을 저장하세요.'); }
          else { const url = URL.createObjectURL(new Blob([JSON.stringify(level, null, 2)], { type: 'application/json' })); const a = document.createElement('a'); a.href = url; a.download = `robot-flow-level-${level.levelId}.json`; a.click(); setTimeout(() => URL.revokeObjectURL(url), 1000); }
        }
      }
    } catch (error) { toast('JSON 확인: ' + error.message); }
  }
});
document.addEventListener('input', e => {
  const key = e.target.dataset.setting; if (!key) return;
  save.settings[key] = e.target.type === 'checkbox' ? e.target.checked : Number(e.target.value); persist();
});
document.addEventListener('change', async e => {
  if (e.target.id !== 'import-level' || !e.target.files[0]) return;
  try { if (e.target.files[0].size > 1000000) throw new Error('파일은 1MB 이하여야 합니다.'); const content = await e.target.files[0].text(); validateLevel(JSON.parse(content)); document.querySelector('#level-json').value = content; toast('레벨을 불러왔어요.'); } catch (error) { toast(error.message); }
});
document.addEventListener('keydown', e => {
  const layer = document.querySelector('#modal-layer');
  if (e.key === 'Escape') { if (layer) { closeModal(); if (game) game.paused = false; } else pause(); }
  if (e.key === 'Tab' && layer) {
    const elements = [...layer.querySelectorAll('button:not(:disabled), input, textarea')], first = elements[0], end = elements.at(-1);
    if (e.shiftKey && (document.activeElement === first || document.activeElement.classList.contains('modal'))) { e.preventDefault(); end?.focus(); }
    else if (!e.shiftKey && document.activeElement === end) { e.preventDefault(); first?.focus(); }
  }
});
function backgroundPause() { if (game?.status === 'playing') pause(); audio.suspend(); }
document.addEventListener('visibilitychange', () => { if (document.hidden) backgroundPause(); });
window.addEventListener('robotflow:background', backgroundPause);
window.robotFlowBack = () => { if (document.querySelector('#modal-layer')) { closeModal(); if (game) game.paused = false; return true; } if (game) { if (game.status === 'playing') pause(); else home(); return true; } return false; };
function frame(now) {
  const dt = Math.min((now - last) / 1000, 0.05); last = now;
  if (game && screen === 'game') {
    // Split accelerated time to keep movement and rule checks stable.
    for (let i = 0; i < speed; i++) game.update(dt);
    renderer?.draw(game.paused ? 0 : dt * speed); uiClock += dt;
    if (uiClock > 0.15) { updateUI(); uiClock = 0; }
  }
  requestAnimationFrame(frame);
}
app.innerHTML = `<main class="splash">${icon}<h1>ROBO RELAY</h1><p>로봇 공장에 오신 것을 환영합니다.</p><div class="splash-loader"></div></main>`;
setTimeout(home, 650); requestAnimationFrame(frame);
