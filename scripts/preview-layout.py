"""Static UI proportion preview from native level data; not a GLES screenshot."""
import json
from pathlib import Path
from PIL import Image, ImageDraw, ImageFont

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / 'artifacts' / 'preview'
OUT.mkdir(parents=True, exist_ok=True)
S = 3
COLORS = dict(zip(['red','blue','yellow','green','purple','orange','pink'],
                 ['#ef7868','#61ace2','#f4ca60','#83c6a3','#8b78b8','#ffa052','#ed96bc']))
FONT = 'C:/Windows/Fonts/malgunbd.ttf'

def render(level_id):
    data = json.loads((ROOT / f'public/construction-levels/level-{level_id:02}.json').read_text(encoding='utf-8'))
    im = Image.new('RGB', (384*S, 832*S), '#f1dfbd')
    d = ImageDraw.Draw(im)
    def box(x,y,w,h,color,r=10,outline=None,width=1):
        d.rounded_rectangle((x*S,y*S,(x+w)*S,(y+h)*S),radius=r*S,fill=color,outline=outline,width=width*S)
    def text(s,x,y,size,color='#353b43',stroke=0):
        d.text((x*S,y*S),str(s),font=ImageFont.truetype(FONT,int(size*S)),fill=color,anchor='mm',
               stroke_width=stroke*S,stroke_fill='#353b43')
    def circle(x,y,r,color):
        d.ellipse(((x-r)*S,(y-r)*S,(x+r)*S,(y+r)*S),fill=color)
    def line(points,color,width=2):
        d.line([(x*S,y*S) for x,y in points],fill=color,width=width*S)
    box(13,26,38,38,'#d4b895',12); box(13,23,38,38,'#fff3df',12)
    box(24,32,5,19,'#353b43',2); box(35,32,5,19,'#353b43',2)
    text(f'STAGE {level_id:03}',192,43,25)
    box(298,26,72,38,'#be945e',12); box(298,23,72,38,'#ffca76',12)
    text('▶ 1×',334,41,17)
    box(150,69,84,3,'#d4b895',2)
    # Native camera targets a 76.2% field and about 90% outer frame width.
    box(18,124,348,301,'#bda88b',18)
    box(19,121,346,301,'#fff1d4',18)
    box(29,132,326,280,'#c3ad8b',12)
    box(45,147,294,256,'#ad9576',4)
    n = data['board']['width']; cell=292.6/n; ch=254/n
    for b in data['blocks']:
        x=45.7+b['x']*cell; y=148+b['y']*ch
        col=COLORS[b['color']]
        box(x,y+.4,cell*.94,ch*.94,'#8e7e70',.7)
        box(x,y,cell*.94,ch*.84,col,.55)
        highlight=tuple(min(255,int(int(col[j:j+2],16)*1.08)) for j in (1,3,5))
        d.line([((x+.4)*S,(y+.3)*S),((x+cell*.83)*S,(y+.3)*S)],fill=highlight,width=1)
    # Theme sketch only: the actual app uses animated two-model GLES meshes.
    def robot(x,y,col):
        circle(x,y+16,9,'#c7b38e');box(x-6,y+3,12,12,'#fff3df',4)
        box(x-10,y-10,20,18,col,6);box(x-7,y-5,14,8,'#353b43',3)
        circle(x-3,y-1,1.4,'#fff3df');circle(x+3,y-1,1.4,'#fff3df')
        line([(x,y-10),(x,y-16)],'#353b43',2);circle(x,y-16,2,'#ffa052')
        box(x-7,y+13,5,5,col,2);box(x+2,y+13,5,5,col,2)
    robot(32,439,'#61ace2');robot(329,436,'#ef7868')
    box(150,477,94,5,'#c7b38e',2)
    circle(174,480,6,'#353b43');circle(225,480,6,'#353b43')
    box(157,451,54,27,'#fff3df',6);box(158,455,52,5,'#ffa052',2)
    box(211,449,30,29,'#ffa052',6);box(215,453,20,12,'#353b43',3)
    for i in range(4):box(163+i*10,442,9,9,list(COLORS.values())[i],2)
    # Controls use the exact logical dimensions from GameLayout.java.
    origin=832-307.2
    def card(c,x,y,front=True):
        col=COLORS[c['color']]
        box(x-1,y+6,50,55,'#c5ad87',11)
        box(x,y+9,48,48,col,10)
        if not front:
            col=tuple(int(int(col[j:j+2],16)*.80) for j in (1,3,5))
        box(x,y,48,48,col,10,outline='#fff3df' if front else None,width=2)
        text(c['robotCount'],x+24,y+24,24,'#ffffff' if front else '#ded9d0',1)
    lanes=[[c for c in data['containers'] if c['lane']==i] for i in range(4)]
    selected=[lanes[i].pop(0) for i in (0,1)]
    for slot,c in enumerate(selected):card(c,48+slot*60,origin+6)
    for i,lane in enumerate(lanes):
        for row,c in enumerate(lane[:3]):card(c,78+i*60,origin+68+row*62,row==0)
    tray=832-96
    box(0,tray+2,384,110,'#b98b55',24);box(2,tray,380,110,'#e1c49b',24)
    box(5,tray+4,374,106,'#ecd6b3',21)
    for i in range(4):
        x=61+i*87;y=tray+18
        circle(x,y+2,29,'#be9465');circle(x,y,28,'#fff3df')
        if i==0:
            box(x-16,y-7,32,16,'#ffa052',7);box(x-19,y+6,38,5,'#f4ca60',2)
            text('+',x+10,y+7,20,'#353b43')
        elif i==1:
            line([(x-10,y+12),(x+8,y-12)],'#353b43',5)
            circle(x-9,y+8,7,'#61ace2');circle(x+9,y-11,8,'#ed96bc')
        elif i==2:
            box(x-18,y-13,18,21,'#61ace2',4);box(x+2,y-7,18,21,'#ffa052',4)
            text('↔',x,y+4,22)
        else:
            line([(x,y-16),(x,y+3)],'#353b43',4)
            line([(x-12,y+13),(x-9,y),(x+9,y),(x+12,y+13)],'#61ace2',5)
            circle(x,y-13,5,'#ffa052')
        circle(x+20,y+22,12,'#6e9468');circle(x+20,y+20,11,'#a7d883');text(3,x+20,y+20,14)
    text('UI 배치 미리보기 · 실제 3D 화면 아님',192,815,10,'#786d5a')
    path=OUT/f'layout-stage-{level_id:03}.png';im.save(path)
    return im

if __name__ == '__main__':
    images=[render(i) for i in (3,248,500)]
    sheet=Image.new('RGB',(384*S*3+32*S,832*S+24*S),'#353b43')
    for i,im in enumerate(images):sheet.paste(im,((8+i*392)*S,12*S))
    sheet.save(OUT/'robot-flow-layout-preview.png')
    print(OUT/'robot-flow-layout-preview.png')
