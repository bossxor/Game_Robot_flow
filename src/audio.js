export class AudioSystem {
  constructor(settings) { this.settings = settings; this.ctx = null; this.timer = null; this.note = 0; }
  unlock() {
    try {
      this.ctx ||= new (window.AudioContext || window.webkitAudioContext)();
      this.ctx.resume();
      if (!this.timer) this.timer = setInterval(() => this.music(), 420);
    } catch { /* Audio is optional on unsupported devices. */ }
  }
  tone(frequency, duration, volume, type = 'sine') {
    if (!this.ctx || this.ctx.state !== 'running' || volume <= 0) return;
    const oscillator = this.ctx.createOscillator(), gain = this.ctx.createGain(), t = this.ctx.currentTime;
    oscillator.type = type; oscillator.frequency.setValueAtTime(frequency, t);
    gain.gain.setValueAtTime(0, t); gain.gain.linearRampToValueAtTime(volume, t + 0.012); gain.gain.exponentialRampToValueAtTime(0.001, t + duration);
    oscillator.connect(gain); gain.connect(this.ctx.destination); oscillator.start(t); oscillator.stop(t + duration + 0.01);
  }
  music() {
    if (!this.settings.music || document.hidden) return;
    const notes = [130.81, 164.81, 196, 261.63, 146.83, 174.61, 220, 293.66];
    this.tone(notes[this.note++ % notes.length], 0.38, this.settings.musicVolume * 0.055, 'triangle');
  }
  effect(type) {
    if (this.settings.soundEffect) {
      const notes = { select: 420, collect: 740, power: 220, mega: 100, complete: 520, won: 880, lost: 140, click: 380 };
      this.tone(notes[type] || 380, type === 'mega' || type === 'won' ? 0.55 : 0.09, this.settings.sfxVolume * (type === 'collect' ? 0.025 : 0.09), type === 'mega' ? 'sawtooth' : 'sine');
      if (type === 'won') { setTimeout(() => this.tone(1108, 0.3, this.settings.sfxVolume * 0.09), 120); setTimeout(() => this.tone(1318, 0.45, this.settings.sfxVolume * 0.09), 240); }
    }
    if (this.settings.vibration && ['power', 'mega', 'lost'].includes(type)) {
      const duration = type === 'mega' ? 120 : 35;
      if (window.Android?.vibrate) window.Android.vibrate(duration); else navigator.vibrate?.(duration);
    }
  }
  suspend() { this.ctx?.suspend(); }
}
