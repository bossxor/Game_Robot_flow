import { PALETTE, SYMBOLS } from './engine.js';
export function drawRobot(ctx, x, y, size, color, type = 'mini', carrying = false) {
  ctx.save(); ctx.translate(x, y);
  if (type === 'mega') { ctx.shadowColor = color; ctx.shadowBlur = 16; }
  ctx.fillStyle = '#143a35';
  ctx.fillRect(-size * 0.32, size * 0.28, size * 0.2, size * 0.18); ctx.fillRect(size * 0.12, size * 0.28, size * 0.2, size * 0.18);
  ctx.fillStyle = color; ctx.beginPath(); ctx.roundRect(-size / 2, -size / 2, size, size * 0.88, size * 0.22); ctx.fill();
  ctx.shadowBlur = 0; ctx.fillStyle = '#143a35'; ctx.beginPath(); ctx.roundRect(-size * 0.36, -size * 0.30, size * 0.72, size * 0.32, size * 0.09); ctx.fill();
  ctx.fillStyle = '#f7f7df'; ctx.fillRect(-size * 0.24, -size * 0.20, size * 0.10, size * 0.10); ctx.fillRect(size * 0.14, -size * 0.20, size * 0.10, size * 0.10);
  ctx.strokeStyle = '#143a35'; ctx.lineWidth = Math.max(1.5, size * 0.05); ctx.beginPath(); ctx.moveTo(0, -size / 2); ctx.lineTo(0, -size * 0.7); ctx.stroke();
  ctx.fillStyle = color; ctx.beginPath(); ctx.arc(0, -size * 0.72, size * 0.065, 0, Math.PI * 2); ctx.fill();
  if (type !== 'mini') { ctx.fillStyle = '#143a35'; ctx.font = `bold ${size * 0.24}px sans-serif`; ctx.textAlign = 'center'; ctx.fillText(type === 'mega' ? 'M' : 'P', 0, size * 0.26); }
  if (carrying) { ctx.fillStyle = color; ctx.fillRect(size * 0.42, size * 0.1, size * 0.32, size * 0.32); ctx.strokeStyle = '#fff'; ctx.strokeRect(size * 0.42, size * 0.1, size * 0.32, size * 0.32); }
  ctx.restore();
}
export class Renderer {
  constructor(canvas, game) { this.canvas = canvas; this.ctx = canvas.getContext('2d'); this.game = game; this.particles = []; this.shake = 0; this.reducedMotion = matchMedia('(prefers-reduced-motion: reduce)').matches; }
  event(e) {
    if (e.type === 'mega' && !this.reducedMotion) this.shake = 0.5;
    if (e.type === 'collect' && !this.reducedMotion) for (let i = 0; i < 7; i++) this.particles.push({ x: e.block.x, y: e.block.y, vx: (Math.random() - 0.5) * 3.5, vy: -Math.random() * 4, life: 0.55, color: PALETTE[e.block.color] });
  }
  draw(dt = 0.016) {
    const { canvas, ctx, game: g } = this, w = canvas.clientWidth || 540, h = w * (g.height + 5) / (g.width + 3), dpr = Math.min(devicePixelRatio || 1, 2);
    if (canvas.width !== Math.round(w * dpr) || canvas.height !== Math.round(h * dpr)) { canvas.width = Math.round(w * dpr); canvas.height = Math.round(h * dpr); }
    ctx.setTransform(dpr, 0, 0, dpr, 0, 0); ctx.clearRect(0, 0, w, h);
    const cell = w / (g.width + 3), ox = cell * 1.5, oy = cell * 1.5;
    const pos = p => ({ x: ox + p.x * cell, y: oy + p.y * cell });
    ctx.save();
    if (this.shake > 0) { this.shake -= dt; ctx.translate((Math.random() - 0.5) * 8, (Math.random() - 0.5) * 8); }
    // The completed collectible is visible through every recovered cell.
    ctx.fillStyle = '#e4e9de'; ctx.beginPath(); ctx.roundRect(ox - cell * 0.62, oy - cell * 0.62, g.width * cell + cell * 0.24, g.height * cell + cell * 0.24, 18); ctx.fill();
    ctx.save(); ctx.globalAlpha = 0.24 + (1 - g.remainingBlocks / g.blocks.length) * 0.65;
    drawRobot(ctx, w / 2, oy + (g.height - 1) * cell / 2, cell * g.width * 0.6, '#a8bd7a', 'mega'); ctx.restore();
    ctx.strokeStyle = '#d2ddce'; ctx.lineWidth = 1;
    for (let y = 0; y < g.height; y++) for (let x = 0; x < g.width; x++) { const p = pos({ x, y }); ctx.beginPath(); ctx.arc(p.x, p.y, 1.3, 0, Math.PI * 2); ctx.stroke(); }
    for (const b of g.blocks) {
      if (b.removed) continue;
      const p = pos(b), s = cell * 0.88;
      ctx.fillStyle = '#143a351c'; ctx.beginPath(); ctx.roundRect(p.x - s / 2, p.y - s / 2 + 3, s, s, cell * 0.14); ctx.fill();
      ctx.fillStyle = PALETTE[b.color]; ctx.beginPath(); ctx.roundRect(p.x - s / 2, p.y - s / 2, s, s - 1, cell * 0.14); ctx.fill();
      ctx.fillStyle = '#ffffff45'; ctx.beginPath(); ctx.roundRect(p.x - s / 2 + 3, p.y - s / 2 + 2, s - 6, 3, 2); ctx.fill();
      ctx.fillStyle = '#143a3566'; ctx.font = `bold ${cell * 0.25}px sans-serif`; ctx.textAlign = 'center'; ctx.textBaseline = 'middle'; ctx.fillText(SYMBOLS[b.color], p.x, p.y + 1);
      if (b.reachable) { ctx.strokeStyle = '#ffffffbb'; ctx.lineWidth = 1.5; ctx.beginPath(); ctx.roundRect(p.x - s / 2 + 1, p.y - s / 2 + 1, s - 2, s - 3, cell * 0.14); ctx.stroke(); }
      else { ctx.fillStyle = '#143a3522'; ctx.beginPath(); ctx.roundRect(p.x - s / 2, p.y - s / 2, s, s - 1, cell * 0.14); ctx.fill(); }
    }
    ctx.setLineDash([3, 6]); ctx.strokeStyle = '#acc2b080';
    for (let i = 0; i < g.slots.length; i++) { const p = pos(g.slotOrigin(i)); ctx.beginPath(); ctx.moveTo(p.x, oy + g.height * cell + cell * 0.2); ctx.lineTo(p.x, p.y - 15); ctx.stroke(); }
    ctx.setLineDash([]);
    ctx.fillStyle = '#8b9f92'; ctx.font = `600 ${Math.max(9, cell * 0.23)}px sans-serif`; ctx.textAlign = 'center'; ctx.fillText('R O B O T   T R A N S I T', w / 2, oy + (g.height + 0.55) * cell);
    for (let i = 0; i < g.slots.length; i++) {
      const p = pos(g.slotOrigin(i)), c = g.slots[i], s = Math.min(cell * 1.25, w / g.slots.length * 0.84);
      ctx.fillStyle = c ? '#143a35' : '#dfe6d9'; ctx.beginPath(); ctx.roundRect(p.x - s / 2, p.y - s * 0.58, s, s * 1.05, 10); ctx.fill();
      if (c) {
        drawRobot(ctx, p.x, p.y - s * 0.06, s * 0.48, PALETTE[c.color]);
        ctx.fillStyle = '#fff'; ctx.font = `bold ${s * 0.23}px sans-serif`; ctx.textAlign = 'center'; ctx.fillText(String(c.remaining), p.x, p.y + s * 0.30);
      } else { ctx.fillStyle = '#8ca593'; ctx.font = `${s * 0.38}px sans-serif`; ctx.fillText('+', p.x, p.y); }
    }
    for (const j of g.jobs) {
      const p = pos(j), size = cell * (j.type === 'mega' ? 1.15 : j.type === 'power' ? 0.70 : 0.40);
      drawRobot(ctx, p.x, p.y + (this.reducedMotion ? 0 : Math.sin(g.time * 24 + j.id) * 1.2), size, PALETTE[j.color], j.type, j.collected > 0);
    }
    this.particles = this.particles.filter(p => p.life > 0);
    for (const p of this.particles) { p.life -= dt; p.x += p.vx * dt; p.y += p.vy * dt; p.vy += dt * 6; const v = pos(p); ctx.globalAlpha = Math.max(0, p.life / 0.55); ctx.fillStyle = p.color; ctx.fillRect(v.x - 2, v.y - 2, 4, 4); }
    ctx.restore();
  }
}
