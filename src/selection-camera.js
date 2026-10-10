export class SelectionCamera {
  zoom=1;active=false;panX=0;panY=0;last=0;
  setActive(active){this.active=active;if(!active){this.panX=0;this.panY=0;}}
  advance(now){const dt=this.last?Math.min(.04,Math.max(0,(now-this.last)/1000)):0;this.last=now;const target=this.active?1.6:1;this.zoom+=(target-this.zoom)*Math.min(1,dt*14);if(Math.abs(target-this.zoom)<.002)this.zoom=target;}
  pan(dx,dy,width,height){if(!this.active)return;this.panX=Math.max(-width*.4,Math.min(width*.4,this.panX+dx));this.panY=Math.max(-height*.4,Math.min(height*.4,this.panY+dy));}
  worldToScreen(x,y,cx,cy){return {x:cx+(x-cx)*this.zoom+this.panX,y:cy+(y-cy)*this.zoom+this.panY};}
  screenToWorld(x,y,cx,cy){return {x:cx+(x-cx-this.panX)/this.zoom,y:cy+(y-cy-this.panY)/this.zoom};}
}
