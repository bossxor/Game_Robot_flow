import test from 'node:test';
import assert from 'node:assert/strict';
import {SelectionCamera} from '../src/selection-camera.js';
import {ConstructionRenderer} from '../src/construction-renderer.js';
test('selection enlarges the view and restores the exact normal view',()=>{
  const c=new SelectionCamera();c.advance(100);c.setActive(true);
  for(let t=116;t<600;t+=16)c.advance(t);
  assert.equal(c.zoom,1.6);c.pan(40,-20,384,400);c.setActive(false);
  for(let t=600;t<1200;t+=16)c.advance(t);
  assert.equal(c.zoom,1);assert.equal(c.panX,0);assert.equal(c.panY,0);
});
test('zoomed and dragged coordinates select the original block',()=>{
  const game={blocks:[{x:2,y:3,color:'blue',removed:false},{x:4,y:3,color:'red',removed:true}]};
  const r=new ConstructionRenderer({getContext:()=>({}),clientWidth:384,clientHeight:400},game);
  r.bounds={left:40,top:30,cell:6,cy:5,cx:192,centerY:170};r.setClearMode(true);r.camera.zoom=1.6;r.pan(50,-30);
  const point=r.camera.worldToScreen(55,47.5,192,170);
  assert.equal(r.pick(point.x,point.y),'blue');
  const empty=r.camera.worldToScreen(67,47.5,192,170);assert.equal(r.pick(empty.x,empty.y),undefined);
});
test('drag bounds are finite and an inactive camera cannot pan',()=>{
  const c=new SelectionCamera();c.pan(100,100,384,400);assert.equal(c.panX,0);
  c.setActive(true);c.pan(10000,-10000,384,400);assert.equal(c.panX,153.60000000000002);assert.equal(c.panY,-160);
  const p=c.worldToScreen(27,93,100,150),q=c.screenToWorld(p.x,p.y,100,150);assert(Math.abs(q.x-27)<1e-9&&Math.abs(q.y-93)<1e-9);
});
