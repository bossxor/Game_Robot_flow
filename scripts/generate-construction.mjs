import { mkdir, readFile, writeFile } from 'node:fs/promises';
const colors=['red','blue','yellow','green','purple','orange','pink'];
const names=['인사하는 로봇','덤프트럭','충전 배터리','톱니바퀴','우주 로켓','현장 크레인','철도 운반차','안전모','굴착기','로봇 공장','작업 드론','화물선','무전기','로봇 강아지','볼트와 너트','발전 풍차','전구','튼튼한 다리','공구 상자','위성 안테나','지게차','도로 롤러','자재 타워','구조 로봇'];
function picture(id,size){
  const kind=(id-1)%names.length,cycle=Math.floor((id-1)/names.length);
  const board=Array.from({length:size},()=>Array(size).fill(0));
  const dx=id<=24?0:((cycle%3)-1)*.045,dy=id<=24?0:((Math.floor(cycle/3)%3)-1)*.035;
  const paint=(test,c)=>{for(let y=0;y<size;y++)for(let x=0;x<size;x++)if(test((x+.5)/size-dx,(y+.5)/size-dy))board[y][x]=c;};
  const rect=(a,b,c,d,k)=>paint((x,y)=>x>=a&&x<c&&y>=b&&y<d,k);
  const ellipse=(a,b,c,d,k)=>paint((x,y)=>((x-a)/c)**2+((y-b)/d)**2<=1,k);
  const line=(a,b,c,d,w,k)=>{const vx=c-a,vy=d-b;paint((x,y)=>{const t=Math.max(0,Math.min(1,((x-a)*vx+(y-b)*vy)/(vx*vx+vy*vy)));return Math.hypot(x-a-t*vx,y-b-t*vy)<w;},k);};
  const wheel=(x,y)=>{ellipse(x,y,.095,.095,2);ellipse(x,y,.045,.045,3);};
  const face=(x,y,w,h)=>{rect(x-w/2,y-h/2,x+w/2,y+h/2,2);ellipse(x-w*.23,y,.035,.035,3);ellipse(x+w*.23,y,.035,.035,3);rect(x-.06,y+h*.22,x+.06,y+h*.29,3);};
  switch(kind){
    case 0: ellipse(.5,.44,.32,.29,1);face(.5,.44,.48,.23);rect(.32,.7,.68,.87,1);rect(.21,.85,.4,.94,2);rect(.6,.85,.79,.94,2);line(.5,.16,.5,.07,.025,2);ellipse(.5,.075,.05,.05,3);break;
    case 1: rect(.12,.36,.57,.65,1);rect(.61,.43,.88,.72,3);rect(.67,.47,.83,.59,2);line(.1,.67,.9,.67,.035,2);wheel(.26,.73);wheel(.71,.73);rect(.18,.28,.29,.36,4);rect(.34,.2,.47,.36,5);break;
    case 2: rect(.29,.18,.71,.88,2);rect(.43,.1,.57,.18,3);rect(.34,.23,.66,.82,1);paint((x,y)=>y>.32&&y<.72&&Math.abs(x-.5+(y-.5)*.22)<.06,3);break;
    case 3: ellipse(.5,.5,.31,.31,1);for(let i=0;i<8;i++){const a=i*Math.PI/4;line(.5+Math.cos(a)*.27,.5+Math.sin(a)*.27,.5+Math.cos(a)*.42,.5+Math.sin(a)*.42,.065,1);}ellipse(.5,.5,.19,.19,2);ellipse(.5,.5,.10,.10,3);break;
    case 4: ellipse(.5,.43,.17,.32,1);paint((x,y)=>y>.68&&y<.9&&Math.abs(x-.5)<(.9-y)*.5,4);paint((x,y)=>y>.52&&y<.75&&Math.abs(x-.5)<(y-.5)*1.2,3);ellipse(.5,.4,.095,.095,2);ellipse(.5,.4,.05,.05,3);break;
    case 5: rect(.55,.25,.63,.88,1);rect(.12,.2,.87,.27,1);rect(.54,.31,.69,.49,2);line(.19,.27,.19,.64,.018,2);rect(.10,.64,.28,.78,3);rect(.42,.85,.75,.92,2);line(.59,.26,.82,.2,.02,3);break;
    case 6: rect(.1,.48,.38,.69,1);rect(.43,.42,.68,.69,3);rect(.73,.35,.91,.69,4);line(.1,.71,.92,.71,.02,2);for(const x of [.19,.32,.51,.63,.8,.9])wheel(x,.76);rect(.48,.49,.62,.58,2);break;
    case 7: ellipse(.5,.48,.34,.29,1);rect(.1,.56,.9,.65,3);rect(.45,.2,.55,.55,3);rect(.2,.65,.8,.74,2);break;
    case 8: rect(.12,.55,.58,.79,1);rect(.24,.33,.54,.6,3);rect(.29,.38,.49,.51,2);line(.52,.39,.72,.22,.045,1);line(.72,.22,.86,.59,.045,1);rect(.75,.55,.93,.67,3);line(.1,.81,.61,.81,.07,2);break;
    case 9: rect(.14,.4,.86,.86,1);paint((x,y)=>y>.28&&y<.43&&x>.14&&x<.86&&((x-.14)% .24)<(y-.28)*1.6,3);rect(.68,.13,.8,.44,2);for(const x of [.23,.43,.63])rect(x,.51,x+.12,.66,2);rect(.43,.71,.57,.86,3);break;
    case 10: ellipse(.5,.5,.19,.14,1);face(.5,.5,.24,.12);for(const x of [.22,.78])for(const y of [.26,.74]){line(.5,.5,x,y,.025,2);ellipse(x,y,.13,.045,3);ellipse(x,y,.035,.035,2);}break;
    case 11: paint((x,y)=>y>.59&&y<.82&&x>.13+(y-.59)*.5&&x<.88-(y-.59)*.5,1);rect(.3,.35,.65,.59,3);rect(.38,.2,.47,.35,2);for(const x of [.35,.51])rect(x,.41,x+.08,.49,2);line(.08,.87,.92,.87,.025,4);break;
    case 12: rect(.28,.31,.72,.86,1);rect(.57,.1,.63,.31,2);rect(.35,.39,.65,.58,2);for(let i=0;i<3;i++)rect(.37,.66+i*.06,.63,.68+i*.06,3);break;
    case 13: rect(.31,.51,.76,.76,1);ellipse(.26,.44,.20,.20,1);face(.23,.43,.22,.11);rect(.1,.21,.2,.4,2);rect(.31,.2,.39,.38,2);for(const x of [.37,.65])rect(x,.72,x+.08,.9,2);line(.74,.56,.86,.36,.035,3);break;
    case 14: rect(.17,.29,.29,.79,1);rect(.08,.2,.38,.31,2);for(let i=0;i<5;i++)line(.16,.37+i*.075,.3,.42+i*.075,.018,3);ellipse(.65,.5,.23,.24,1);ellipse(.65,.5,.12,.12,2);break;
    case 15: line(.5,.43,.5,.9,.04,1);for(let i=0;i<3;i++){const a=i*Math.PI*2/3-.5;line(.5,.43,.5+Math.cos(a)*.34,.43+Math.sin(a)*.34,.06,3);}ellipse(.5,.43,.07,.07,2);rect(.35,.89,.65,.94,2);break;
    case 16: ellipse(.5,.4,.29,.29,1);rect(.37,.57,.63,.76,1);rect(.37,.76,.63,.84,2);rect(.43,.84,.57,.9,3);line(.5,.67,.4,.39,.015,3);line(.5,.67,.6,.39,.015,3);break;
    case 17: rect(.08,.43,.92,.51,1);for(const x of [.17,.73])rect(x,.51,x+.1,.87,3);line(.2,.43,.5,.2,.025,2);line(.5,.2,.78,.43,.025,2);for(const x of [.32,.44,.56,.68])line(x,.34,x,.44,.012,2);line(.1,.9,.9,.9,.025,4);break;
    case 18: rect(.13,.38,.87,.8,1);rect(.36,.24,.64,.38,2);rect(.41,.29,.59,.38,0);rect(.13,.51,.87,.56,2);rect(.46,.49,.54,.64,3);break;
    case 19: ellipse(.42,.48,.29,.28,1);ellipse(.42,.4,.22,.13,2);line(.43,.51,.7,.2,.022,3);ellipse(.71,.19,.05,.05,3);line(.49,.69,.66,.85,.035,2);rect(.52,.84,.8,.92,1);break;
    case 20: rect(.13,.52,.57,.76,1);rect(.32,.28,.55,.52,3);rect(.37,.33,.51,.45,2);line(.64,.3,.64,.81,.025,2);line(.64,.81,.9,.81,.025,2);wheel(.24,.77);wheel(.52,.77);rect(.71,.59,.89,.78,4);break;
    case 21: rect(.24,.36,.62,.68,1);rect(.32,.25,.57,.37,3);rect(.36,.29,.52,.36,2);ellipse(.2,.72,.14,.12,2);rect(.58,.61,.89,.84,3);ellipse(.87,.72,.07,.12,1);break;
    case 22: for(let i=0;i<4;i++){const w=.7-i*.12;rect(.5-w/2,.76-i*.18,.5+w/2,.91-i*.18,(i%3)+1);rect(.49,.76-i*.18,.51,.91-i*.18,2);}break;
    case 23: ellipse(.5,.43,.3,.26,1);face(.5,.43,.43,.20);rect(.34,.66,.66,.84,3);rect(.46,.68,.54,.82,2);rect(.39,.72,.61,.78,2);for(const x of [.25,.64])rect(x,.83,x+.11,.94,1);break;
  }
  // Empty corners and silhouettes change the approach routes, not only the palette.
  const shape=kind%6;
  for(let y=0;y<size;y++)for(let x=0;x<size;x++){
    const nx=(x+.5)/size,ny=(y+.5)/size;
    const outside=shape===0?((nx-.5)/.49)**2+((ny-.5)/.49)**2>1:shape===1?Math.abs(nx-.5)+Math.abs(ny-.5)>.78:shape===2?(nx<.06||nx>.94||ny<.04||ny>.96):shape===3?(nx<.12&&ny<.3||nx>.88&&ny>.7):shape===4?(Math.abs(nx-.5)>.46-.12*Math.abs(ny-.5)):false;
    if(outside)board[y][x]=null;
    else if(cycle>0&&board[y][x]===0&&((x+cycle*3)% (9+cycle%5)===0)&&y>2&&y<size-3)board[y][x]=1;
  }
  const count=id===1?3:id<=3?4:id<=8?5:id<=15?6:7;
  const palette=[2,1,0,3,5,4,6].map((c,i)=>colors[(c+cycle+Math.floor(kind/4))%7]).slice(0,count);
  let result=board.map(r=>r.map(c=>c===null?null:palette[c%count]));
  if(cycle%2)result=result.map(r=>r.reverse());
  return result;
}
function containers(board,id){
  const n=board.length,live=board.map(r=>r.map(c=>c!==null)),sequence=[];
  const front=()=>{const reached=new Set(),pending=[];for(let y=-1;y<=n;y++)for(let x=-1;x<=n;x++)if(x===-1||y===-1||x===n||y===n){reached.add(`${x},${y}`);pending.push([x,y]);}
    for(let i=0;i<pending.length;i++){const [x,y]=pending[i];for(const [a,b] of [[x+1,y],[x-1,y],[x,y+1],[x,y-1]])if(a>=0&&a<n&&b>=0&&b<n&&!live[b][a]&&!reached.has(`${a},${b}`)){reached.add(`${a},${b}`);pending.push([a,b]);}}
    const out=[];for(let y=0;y<n;y++)for(let x=0;x<n;x++)if(live[y][x]&&[[x+1,y],[x-1,y],[x,y+1],[x,y-1]].some(([a,b])=>reached.has(`${a},${b}`)))out.push([x,y]);return out;};
  let remaining=live.flat().filter(Boolean).length;
  while(remaining){const reachable=front(),frequency=Array(7).fill(0);for(const [x,y] of reachable)frequency[colors.indexOf(board[y][x])]++;
    const color=colors[frequency.indexOf(Math.max(...frequency))];let count=0;
    const limits=id<=3?[18,25,32,40]:id%3===0?[30,45,60,90]:id%3===1?[20,35,50,75]:[25,40,65,100];
    const limit=limits[(sequence.length+id)%limits.length];
    while(count<limit){const available=front().filter(([x,y])=>board[y][x]===color);if(!available.length)break;for(const [x,y] of available){if(count===limit)break;live[y][x]=false;remaining--;count++;}}
    sequence.push({color,count});
  }
  const prev=Array(4).fill(null);
  return sequence.map((c,i)=>{const lane=i%4,id=i+1,blockedBy=prev[lane]?[prev[lane]]:[];prev[lane]=id;return {id,color:c.color,robotCount:c.count,depth:Math.floor(i/4),lane,blockedBy};});
}
await mkdir('public/construction-levels',{recursive:true});
for(let id=1;id<=500;id++){
  const size=id<=6?[28,32,36,40,44,48][id-1]:48,board=picture(id,size),blocks=board.flatMap((r,y)=>r.flatMap((color,x)=>color===null?[]:[{x,y,color}]));
  const difficulty=id%10===0?2:id%5===0?1:0,capsules=containers(board,id);
  if(difficulty){for(let i=0;i<capsules.length-1;i++){const a=capsules[i],count=difficulty===2?3:2,members=capsules.slice(i,i+count);if(members.length===count&&members.every(c=>c.depth===a.depth&&c.color===a.color)){for(const c of members)c.group=a.id;i+=count-1;}}}
  const level={levelId:id,name:names[(id-1)%names.length],difficulty,theme:(id-1)%3,autoFinish:true,board:{width:size,height:size},slotCount:5,revealDepth:100,reward:100+Math.floor(id/10)*10+difficulty*30,powerRobotChance:0,megaRobot:{requiredGauge:100,blockCount:8},blocks,containers:capsules};
  // Fine isolated details use the constructive four-lane queue instead of split bait.
  if((id-1)%24===0||(id-1)%24===12)level.queueVersion=1;
  const boundary=blocks.filter(b=>[[b.x-1,b.y],[b.x+1,b.y],[b.x,b.y-1],[b.x,b.y+1]].some(([x,y])=>x<0||y<0||x>=size||y>=size||board[y][x]===null));
  if(id>=3&&(id%5===3||id%10===0)){
    const exposed=new Set(boundary.map(b=>b.color)),lockColor=colors.find(c=>!exposed.has(c)&&blocks.some(b=>b.color===c));
    const tile=[...boundary].sort((a,b)=>b.y-a.y||Math.abs(a.x-size/2)-Math.abs(b.x-size/2))[0];
    if(lockColor&&tile)level.keys=[{id:1,x:tile.x,y:tile.y,lockColor}];
  }
  if(id>=4&&(id%5===4||id%10===0)){
    const x=Math.floor(size/2)-3,y=Math.floor(size/2)-3;
    const counts=new Map();for(const b of blocks)if(b.x<x||b.x>=x+6||b.y<y||b.y>=y+6)counts.set(b.color,(counts.get(b.color)||0)+1);
    const color=boundary.map(b=>b.color).find(c=>counts.get(c)>=24);
    if(color)level.seals=[{x,y,width:6,height:6,color,count:id<=10?18:24+6*(id%4)}];
  }
  const path=`public/construction-levels/level-${String(id).padStart(2,'0')}.json`,text=JSON.stringify(level)+'\n';
  const previous=await readFile(path,'utf8').catch(error=>{if(error.code==='ENOENT')return null;throw error;});
  if(previous!==text)await writeFile(path,text);
}
console.log('Generated 500 rebuilt levels: 24 pictures, six board silhouettes, varied inventories and gradual colour introductions.');
