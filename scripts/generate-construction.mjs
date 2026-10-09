import { mkdir, readFile, writeFile } from 'node:fs/promises';
const colors=['red','blue','yellow','green','purple','orange','pink'];
const names=['안전모 친구','자재 트럭','높이 올려요','안전한 길','새로운 집','튼튼한 다리','작은 굴착기','공구 시간','현장 표지','꽃 피는 현장'];
function picture(id,size){
  const kind=(id-1)%10,shift=Math.floor((id-1)/10)%7;
  const board=Array.from({length:size},()=>Array(size).fill(1));
  const rect=(x0,y0,x1,y1,c)=>{for(let y=0;y<size;y++)for(let x=0;x<size;x++)if(x/size>=x0&&x/size<x1&&y/size>=y0&&y/size<y1)board[y][x]=c;};
  const ellipse=(cx,cy,rx,ry,c)=>{for(let y=0;y<size;y++)for(let x=0;x<size;x++)if(((x/size-cx)/rx)**2+((y/size-cy)/ry)**2<=1)board[y][x]=c;};
  rect(0,.84,1,1,3);ellipse(.86,.12,.075,.075,2);
  if(kind===0||kind===2){
    ellipse(.5,.47,.35,.33,3);rect(.19,.36,.81,.67,4);rect(.29,.46,.37,.53,2);rect(.63,.46,.71,.53,2);rect(.43,.58,.57,.61,6);
    ellipse(.5,.3,.34,.2,5);rect(.14,.31,.86,.37,2);rect(.47,.12,.53,.31,2);rect(.33,.7,.67,.83,5);rect(.24,.79,.41,.89,4);rect(.59,.79,.76,.89,4);
  }else if(kind===1||kind===6){
    rect(.12,.42,.6,.71,5);rect(.59,.36,.85,.72,2);rect(.65,.42,.8,.57,4);rect(.12,.36,.29,.47,0);rect(.31,.3,.47,.47,3);rect(.47,.38,.6,.47,6);
    for(const x of [.25,.51,.75]){ellipse(x,.74,.095,.095,4);ellipse(x,.74,.045,.045,2);}rect(.1,.68,.88,.72,4);
    if(kind===6){rect(.68,.22,.76,.4,5);rect(.72,.18,.89,.24,2);rect(.84,.23,.92,.46,5);}
  }else if(kind===3){
    for(let y=0;y<size;y++)for(let x=0;x<size;x++){let nx=x/size,ny=y/size;if(ny>.17&&ny<.82&&Math.abs(nx-.5)<(ny-.12)*.43)board[y][x]=(ny>.4&&ny<.48||ny>.64&&ny<.71)?2:5;}
    rect(.14,.8,.86,.86,4);rect(.18,.78,.82,.82,5);
  }else if(kind===4){
    for(let y=0;y<size;y++)for(let x=0;x<size;x++)if(y/size>.18&&y/size<.43&&Math.abs(x/size-.5)<(y/size-.15)*1.5)board[y][x]=0;
    rect(.22,.42,.78,.84,2);rect(.3,.5,.44,.66,1);rect(.56,.5,.7,.66,1);rect(.44,.69,.56,.85,5);rect(.62,.22,.7,.38,4);
  }else if(kind===5){
    rect(.12,.42,.88,.52,5);rect(.18,.52,.27,.84,2);rect(.73,.52,.82,.84,2);rect(.22,.25,.26,.43,4);rect(.74,.25,.78,.43,4);
    rect(.24,.24,.76,.28,0);for(let i=0;i<5;i++)rect(.31+i*.085,.28,.33+i*.085,.42,2);rect(.12,.75,.88,.83,1);
  }else if(kind===7){
    ellipse(.51,.29,.22,.18,5);ellipse(.51,.22,.12,.13,1);rect(.41,.34,.61,.76,5);ellipse(.51,.78,.16,.1,4);ellipse(.51,.78,.06,.04,2);
  }else if(kind===8){
    rect(.47,.5,.53,.86,4);ellipse(.5,.34,.3,.25,2);ellipse(.5,.34,.25,.2,0);rect(.47,.2,.53,.37,2);ellipse(.5,.43,.04,.035,2);
  }else{
    rect(.48,.44,.53,.87,3);ellipse(.37,.69,.13,.06,3);ellipse(.64,.62,.13,.06,3);
    for(let i=0;i<6;i++){const a=i*Math.PI/3;ellipse(.5+Math.cos(a)*.15,.36+Math.sin(a)*.15,.095,.09,6);}ellipse(.5,.36,.1,.1,2);
  }
  // A quiet border and movable accents vary levels while preserving readable art.
  for(let y=0;y<size;y++)for(let x=0;x<size;x++)if(x===0||y===0||x===size-1||y===size-1)board[y][x]=[4,1,3,2][Math.floor((x+y)/6)%4];
  if(id>10){const x=2+(id*7)%(size-5),y=2+(id*11)%(size-5);board[y][x]=6;board[y][x+1]=2;}
  return board.map(row=>row.map(c=>(c+shift)%7));
}
function containers(board){
  const n=board.length,live=board.map(r=>r.map(()=>true)),sequence=[];
  const front=()=>{const reached=new Set(),pending=[];for(let y=-1;y<=n;y++)for(let x=-1;x<=n;x++)if(x===-1||y===-1||x===n||y===n){reached.add(`${x},${y}`);pending.push([x,y]);}
    for(let i=0;i<pending.length;i++){const [x,y]=pending[i];for(const [a,b] of [[x+1,y],[x-1,y],[x,y+1],[x,y-1]])if(a>=0&&a<n&&b>=0&&b<n&&!live[b][a]&&!reached.has(`${a},${b}`)){reached.add(`${a},${b}`);pending.push([a,b]);}}
    const out=[];for(let y=0;y<n;y++)for(let x=0;x<n;x++)if(live[y][x]&&[[x+1,y],[x-1,y],[x,y+1],[x,y-1]].some(([a,b])=>reached.has(`${a},${b}`)))out.push([x,y]);return out;};
  let remaining=n*n;
  while(remaining){let reachable=front();let frequency=Array(7).fill(0);for(const [x,y] of reachable)frequency[board[y][x]]++;
    const color=frequency.indexOf(Math.max(...frequency));let count=0;const limit=(n<=20?[24,18,32,20,28,16]:[40,25,60,35,100,45,75,20])[sequence.length% (n<=20?6:8)];
    while(count<limit){const available=front().filter(([x,y])=>board[y][x]===color);if(!available.length)break;
      for(const [x,y] of available){if(count===limit)break;live[y][x]=false;remaining--;count++;}}
    sequence.push({color,count});
  }
  const prev=Array(4).fill(null);
  return sequence.map((c,i)=>{const lane=i%4,id=i+1,blockedBy=prev[lane]?[prev[lane]]:[];prev[lane]=id;return {id,color:colors[c.color],robotCount:c.count,depth:Math.floor(i/4),lane,blockedBy};});
}
await mkdir('public/construction-levels',{recursive:true});
for(let id=1;id<=500;id++){
  const size=id<=2?20:48,board=picture(id,size),blocks=board.flatMap((r,y)=>r.map((color,x)=>({x,y,color:colors[color]})));
  const level={levelId:id,name:names[(id-1)%10],board:{width:size,height:size},slotCount:5,revealDepth:100,reward:100+Math.floor(id/10)*10,powerRobotChance:0,megaRobot:{requiredGauge:100,blockCount:8},blocks,containers:containers(board)};
  const path=`public/construction-levels/level-${String(id).padStart(2,'0')}.json`,text=JSON.stringify(level)+'\n';
  const previous=await readFile(path,'utf8').catch(error=>{if(error.code==='ENOENT')return null;throw error;});
  if(previous!==text)await writeFile(path,text);
}
console.log('Generated 500 construction pictures: tutorial 400 tiles, main boards 2304 tiles.');
