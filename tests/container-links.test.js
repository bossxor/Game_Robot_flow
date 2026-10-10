import test from 'node:test';
import assert from 'node:assert/strict';
import {containerLinks,splitLink} from '../src/container-links.js';
const card=(id,group,x,y,color='red')=>({id,group,x,y,color,size:48});
test('horizontal and vertical groups link their actual members',()=>{
  const horizontal=containerLinks([card(1,8,0,0,'red'),card(2,8,60,0,'blue'),card(3,0,120,0)]);
  assert.equal(horizontal.length,1);assert.equal(horizontal[0].a.color,'red');assert.equal(horizontal[0].b.color,'blue');
  assert.deepEqual(horizontal[0].points,[[43,24],[65,24]]);
  assert.deepEqual(containerLinks([card(1,8,0,0),card(2,8,0,60)])[0].points,[[24,43],[24,65]]);
});
test('separated partners route around an unrelated container',()=>{
  const [edge]=containerLinks([card(1,8,0,68),card(2,0,60,68),card(3,8,120,68)]);
  assert.deepEqual(edge.points,[[24,73],[24,59],[144,59],[144,73]]);
  assert.equal(edge.b.id,3);
});
test('different groups stay separate and three members use two links',()=>{
  const edges=containerLinks([card(1,8,0,0),card(2,8,60,0),card(3,8,120,0),card(4,9,0,60),card(5,9,60,60),card(6,0,120,60)]);
  assert.equal(edges.length,3);assert(edges.every(({a,b})=>a.group===b.group));
  assert.equal(containerLinks([card(1,8,0,0)]).length,0);
});
test('colour halves meet at half the full routed length',()=>{
  const points=[[0,0],[0,10],[60,10],[60,20]],halves=splitLink(points);
  assert.deepEqual(halves[0].at(-1),[30,10]);assert.deepEqual(halves[1][0],[30,10]);
  const length=ps=>ps.slice(1).reduce((sum,p,i)=>sum+Math.hypot(p[0]-ps[i][0],p[1]-ps[i][1]),0);
  assert.equal(length(halves[0]),40);assert.equal(length(halves[1]),40);
  assert.deepEqual(splitLink([]),[[],[]]);
});

test('top work-slot links remain inside the controls viewport',()=>{
 const [edge]=containerLinks([card(1,8,0,6),card(2,0,60,6),card(3,8,120,6)]);
 assert.deepEqual(edge.points,[[24,49],[24,63],[144,63],[144,49]]);
});
