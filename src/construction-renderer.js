import {PALETTE} from './engine.js';
import {SelectionCamera} from './selection-camera.js';
/** Browser Canvas preview; the APK independently renders native GLES meshes. */
export class ConstructionRenderer{
 constructor(canvas,game){this.canvas=canvas;this.ctx=canvas.getContext('2d');this.game=game;this.camera=new SelectionCamera();}
 setClearMode(enabled){this.camera.setActive(enabled);}
 pan(dx,dy){this.camera.pan(dx,dy,this.canvas.clientWidth,this.canvas.clientHeight);}
 draw(){const c=this.canvas,g=this.game,w=c.clientWidth,h=c.clientHeight,dpr=Math.min(devicePixelRatio||1,2);if(w===0||h===0)return;if(c.width!==Math.round(w*dpr)||c.height!==Math.round(h*dpr)){c.width=Math.round(w*dpr);c.height=Math.round(h*dpr);}const x=this.ctx;x.setTransform(dpr,0,0,dpr,0,0);x.clearRect(0,0,w,h);
  const box=(a,b,c,d,r,col)=>{x.fillStyle=col;x.beginPath();x.roundRect(a,b,c,d,r);x.fill();};
  const bw=Math.min(w*.762,Math.max(60,(h-8-25*w/384)/1.125)),bh=bw*.875,left=(w-bw)/2,top=bw*.09+8,cell=bw/g.width,cy=bh/g.height;
  this.bounds={left,top,cell,cy,cx:w/2,centerY:top+bh/2};
  this.camera.advance(performance.now());x.translate(w/2+this.camera.panX,top+bh/2+this.camera.panY);x.scale(this.camera.zoom,this.camera.zoom);x.translate(-w/2,-top-bh/2);
  box(left-bw*.1,top-bw*.085,bw*1.2,bh+bw*.17,18,'#aaa1b5');box(left-bw*.095,top-bw*.09,bw*1.19,bh+bw*.17,18,'#e4dbe9');box(left-7,top-7,bw+14,bh+14,6,'#827a90');
  for(const b of g.blocks){if(b.removed||g.sealed(b))continue;const a=left+b.x*cell,d=top+b.y*cy;box(a,d+.6,cell*.94,cy*.94,.7,'#8e7e70');box(a,d,cell*.94,cy*.85,.7,PALETTE[b.color]);x.fillStyle='#ffffff18';x.fillRect(a+.25,d+.2,cell*.8,.4);}
  for(const seal of g.seals)if(!seal.open){
   const a=left+seal.x*cell,b=top+seal.y*cy,sw=seal.width*cell,sh=seal.height*cy;
   box(a,b,sw,sh,4,'#805347');box(a+2,b+2,sw-4,sh-4,3,PALETTE[seal.color]);
   x.font=`900 ${sw*.5}px sans-serif`;x.textAlign='center';x.textBaseline='middle';x.lineWidth=3;x.strokeStyle='#353b43';x.strokeText(Math.max(0,seal.count-g.colorCollected[seal.color]),a+sw/2,b+sh/2);x.fillStyle='white';x.fillText(Math.max(0,seal.count-g.colorCollected[seal.color]),a+sw/2,b+sh/2);
  }
  for(const key of g.keys)if(!key.owned){const a=left+(key.x+.5)*cell,b=top+(key.y+.5)*cy;x.strokeStyle='#ffdf37';x.lineWidth=cell*.45;x.beginPath();x.arc(a,b-cell*.8,cell*.65,0,Math.PI*2);x.moveTo(a,b-cell*.2);x.lineTo(a,b+cell*1.5);x.moveTo(a,b+cell);x.lineTo(a+cell*.6,b+cell);x.stroke();}
  const dockY=top+bh+bw*.16;
  // A quiet sand road and construction cones frame the gameplay.
  for(const j of g.jobs){const px=left+(j.x+.5)*cell,py=j.y>g.height+1.8?dockY+(h-18-dockY)*Math.min(1,(j.y-g.height-1.8)/.8):top+(j.y+.5)*cy;this.robot(x,px,py,Math.max(12,w*.047),PALETTE[j.color],j.type,g.paused?0:Math.sin(g.time*12+j.id),j.collected>0);}
  const s=w/384,spacing=72*s,truckX=w/2+(g.truckState==='leaving'?Math.pow(Math.min(1,g.truckTime/1.4),2)*w*.75:g.truckState==='entering'?-spacing*(1-g.truckTime/.5):0);
  const outstanding=g.remainingBlocks+g.jobs.reduce((n,j)=>n+j.collected,0)+g.truckPending+g.truckCargo,visible=Math.min(3,Math.ceil(outstanding/20));
  const truck=(cx,cargo)=>{x.save();x.translate(cx,dockY);x.scale(s*.75,s*.75);box(-43,12,88,5,3,'#c7b38e');for(const p of [-27,26]){x.fillStyle='#353b43';x.beginPath();x.arc(p,10,6,0,Math.PI*2);x.fill();}box(-39,-13,52,24,5,'#fff3df');box(-39,-9,52,4,2,'#ffa052');box(13,-15,30,26,6,'#ffa052');box(17,-11,20,12,3,'#353b43');for(let i=0;i<Math.ceil(cargo/2);i++)box(-34+(i%5)*9,-20-Math.floor(i/5)*8,8,8,2,Object.values(PALETTE)[i%7]);x.restore();};
  if(visible||g.truckState==='leaving')truck(truckX,g.truckCargo);for(let i=1;i<visible;i++)truck(w/2-(i+(g.truckState==='entering'?1-g.truckTime/.5:0))*spacing,0);
  if(g.status==='won'){this.robot(x,w*.4,dockY,18*s,PALETTE.green,'mini',Math.sin(performance.now()/80),false);this.robot(x,w*.6,dockY,20*s,PALETTE.yellow,'mega',Math.sin(performance.now()/80),false);}
 }
 pick(x,y){if(!this.bounds)return null;const{left,top,cell,cy,cx,centerY}=this.bounds,point=this.camera.screenToWorld(x,y,cx,centerY),bx=Math.floor((point.x-left)/cell),by=Math.floor((point.y-top)/cy);return this.game.blocks.find(b=>!b.removed&&!this.game.sealed(b)&&b.x===bx&&b.y===by)?.color;}
 robot(x,a,b,s,col,type,gait,carrying){x.save();x.translate(a,b);x.fillStyle='#353b4326';x.beginPath();x.ellipse(0,s*.8,s*.7,s*.2,0,0,Math.PI*2);x.fill();const box=(a,b,w,h,r,col)=>{x.fillStyle=col;x.beginPath();x.roundRect(a,b,w,h,r);x.fill();};
  if(type==='mega'){box(-s*.8,s*.22,s*1.6,s*.5,3,'#353b43');box(-s*.68,-s*.35,s*1.36,s*.7,4,col);box(-s*.45,-s*.8,s*.9,s*.6,3,'#fff3df');box(-s*.3,-s*.68,s*.6,s*.22,2,'#353b43');x.strokeStyle='#ffa052';x.lineWidth=2;x.beginPath();x.moveTo(s*.75,s*.5);x.lineTo(s*.9,-s*.3);x.stroke();x.fillStyle='#ffa052';x.beginPath();x.arc(s*.4,-s*.7,2,0,Math.PI*2);x.fill();}
  else{box(-s*.4,s*.38+gait,s*.27,s*.3,2,col);box(s*.13,s*.38-gait,s*.27,s*.3,2,col);box(-s*.3,0,s*.6,s*.5,3,'#fff3df');box(-s*.65,-s*.65,s*1.3,s,4,col);box(-s*.45,-s*.42,s*.9,s*.36,2,'#353b43');box(-s*.27,-s*.32,s*.12,s*.12,1,'#fff3df');box(s*.15,-s*.32,s*.12,s*.12,1,'#fff3df');x.strokeStyle='#353b43';x.lineWidth=1;x.beginPath();x.moveTo(0,-s*.65);x.lineTo(0,-s*.95);x.stroke();}
  if(carrying)box(s*.45,s*.1,s*.5,s*.5,2,col);x.restore();
 }
}
