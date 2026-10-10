// Build visible group links independently of queue lane/row adjacency.
export function containerLinks(nodes) {
  const groups=new Map(),edges=[];
  for(const node of nodes)if(node.group){if(!groups.has(node.group))groups.set(node.group,[]);groups.get(node.group).push(node);}
  for(const members of groups.values()){
    const joined=[members.shift()];
    while(members.length){
      let best=Infinity,a,b;
      for(const from of joined)for(const to of members){const d=Math.hypot(from.x-to.x,from.y-to.y);if(d<best){best=d;a=from;b=to;}}
      edges.push({a,b,points:route(a,b)});joined.push(b);members.splice(members.indexOf(b),1);
    }
  }
  return edges;
}
function route(a,b){
  const ax=a.x+a.size/2,ay=a.y+a.size/2,bx=b.x+b.size/2,by=b.y+b.size/2,dx=bx-ax,dy=by-ay,size=Math.min(a.size,b.size);
  if(Math.hypot(dx,dy)<1)return [];
  if(Math.abs(dy)<size*.2&&Math.abs(dx)>size*1.8){const top=Math.min(a.y,b.y)-size*9/48;return [[ax,a.y+size*5/48],[ax,top],[bx,top],[bx,b.y+size*5/48]];}
  if(Math.abs(dx)<size*.2&&Math.abs(dy)>size*1.8){const right=Math.max(a.x+a.size,b.x+b.size)+size*9/48;return [[a.x+a.size-size*5/48,ay],[right,ay],[right,by],[b.x+b.size-size*5/48,by]];}
  const ka=(a.size/2-size*5/48)/Math.max(Math.abs(dx),Math.abs(dy)),kb=(b.size/2-size*5/48)/Math.max(Math.abs(dx),Math.abs(dy));
  return [[ax+dx*ka,ay+dy*ka],[bx-dx*kb,by-dy*kb]];
}
export function splitLink(points){
  if(points.length<2)return [[],[]];
  const lengths=points.slice(1).map((p,i)=>Math.hypot(p[0]-points[i][0],p[1]-points[i][1]));
  const half=lengths.reduce((a,b)=>a+b,0)/2;let travelled=0;
  for(let i=0;i<lengths.length;i++){
    if(travelled+lengths[i]>=half){const t=lengths[i]?(half-travelled)/lengths[i]:0,start=points[i],end=points[i+1],mid=[start[0]+(end[0]-start[0])*t,start[1]+(end[1]-start[1])*t];return [[...points.slice(0,i+1),mid],[mid,...points.slice(i+1)]];}
    travelled+=lengths[i];
  }
  return [points,[]];
}
