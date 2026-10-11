// Checks offline construction against a strict, local subset of the Plugin API.
// This does not replace testing in the real Figma editor.
const fs=require('fs'),vm=require('vm'),assert=require('assert');let count=0;const nodes=[],variables=[],styles=[];
class Node {
 constructor(type){this.id='test:'+ ++count;this.type=type;this.children=[];this.width=100;this.height=100;this.x=0;this.y=0;nodes.push(this);}
 appendChild(n){if(n.parent)n.parent.children=n.parent.children.filter(x=>x!==n);this.children.push(n);n.parent=this;}
 resize(w,h){assert(w>0&&h>0);this.width=w;this.height=h;}
 setBoundVariable(field,v){assert(v.id);}
 setVariableCodeSyntax(){}
 findAll(fn){return this.children.flatMap(c=>[...(fn(c)?[c]:[]),...c.findAll(fn)]);}
 addComponentProperty(name,type,value){const key=name+'#'+this.id;this.props=this.props||{};this.props[key]=value;return key;}
 createInstance(){const clone=(source)=>{const n=new Node(source.type);for(const key of ['fontName','fontSize','styleId','characters','name','width','height','componentPropertyReferences','props','reactions'])if(source[key]!==undefined)n[key]=source[key];for(const child of source.children)n.appendChild(clone(child));return n;};const n=clone(this);n.type='INSTANCE';n.component=this;return n;}
 setProperties(p){assert(this.props);for(const key of Object.keys(p))assert(Object.hasOwn(this.props,key));for(const child of this.findAll(x=>x.type==='TEXT')){const ref=child.componentPropertyReferences?.characters;if(ref&&p[ref]!==undefined)child.characters=p[ref];}}
 async setReactionsAsync(r){this.reactions=r;}
 async setTextStyleIdAsync(id){assert(styles.some(s=>s.id===id));this.styleId=id;}
 set textStyleId(v){throw new Error('Sync textStyleId forbidden in dynamic-page');}
}
const root=new Node('DOCUMENT'),defaultPage=new Node('PAGE');root.appendChild(defaultPage);
let closed,finished=new Promise(resolve=>closed=resolve);
const figma={root,currentPage:defaultPage,
 listAvailableFontsAsync:async()=>[{fontName:{family:'Noto Sans SC',style:'Regular'}},{fontName:{family:'Noto Sans SC',style:'Bold'}}],loadFontAsync:async f=>assert(f.family==='Noto Sans SC'),
 createPage:()=>{let n=new Node('PAGE');root.appendChild(n);return n;},setCurrentPageAsync:async p=>{figma.currentPage=p;},
 createFrame:()=>new Node('FRAME'),createRectangle:()=>new Node('RECTANGLE'),createText:()=>new Node('TEXT'),createComponent:()=>new Node('COMPONENT'),
 createNodeFromSvg:svg=>{assert(svg.includes('<path'));let n=new Node('FRAME');n.appendChild(new Node('VECTOR'));return n;},
 createTextStyle:()=>{let s={id:'style:'+styles.length};styles.push(s);return s;},
 combineAsVariants:(items,parent)=>{let n=new Node('COMPONENT_SET');parent.appendChild(n);items.forEach(c=>n.appendChild(c));return n;},
 viewport:{scrollAndZoomIntoView:()=>{}},
 variables:{
  createVariableCollection:name=>({id:'collection:'+name,name,modes:[{modeId:'test-mode'}],renameMode:()=>{}}),
  createVariable:(name,collection,type)=>{let v={id:'var:'+variables.length,name,type,setValueForMode:(id,value)=>{v.value=value;},setVariableCodeSyntax:()=>{}};variables.push(v);return v;},
  createVariableAlias:v=>({type:'VARIABLE_ALIAS',id:v.id}),setBoundVariableForPaint:(p,k,v)=>({...p,boundVariables:{[k]:{id:v.id}}})},
 closePlugin:message=>closed(message)
};
(async()=>{
 vm.runInNewContext(fs.readFileSync(process.argv[2],'utf8'),{figma,console:{log:()=>{},error:console.error}});const message=await finished;
 assert(message.includes('已创建'),message);
 const page=root.children.find(p=>p.name==='VoiceStudio 1.3.2 · 离线工作室 21屏');assert(page);
 const screens=page.children.filter(n=>/^\d{2} /.test(n.name));assert.equal(screens.length,21);
 assert.equal(page.findAll(n=>n.type==='COMPONENT').length,13);
 const links=page.findAll(n=>n.reactions);assert(links.length>=32);assert(links.every(n=>n.reactions.every(r=>r.actions.every(a=>screens.some(s=>s.id===a.destinationId)))));
 const cards=page.findAll(n=>n.type==='INSTANCE'&&n.name==='作品卡片');assert.equal(cards.length,4);assert(cards.some(c=>c.findAll(n=>n.type==='TEXT').some(t=>t.characters==='视频开场旁白')));
 const text=page.findAll(n=>n.type==='TEXT');assert(text.length>=70);assert(text.every(n=>n.fontName));
 assert(variables.every(v=>Array.isArray(v.scopes)&&!v.scopes.includes('ALL_SCOPES')));
 assert(variables.filter(v=>v.name.startsWith('color/')).every(v=>v.value.type==='VARIABLE_ALIAS'));
 console.log(JSON.stringify({localMock:'passed',screens:21,buttonVariants:4,navComponents:8,workCardComponents:1,prototypeLinks:links.length,textNodes:text.length,variables:variables.length,realFigmaExecution:'not performed'}));
})().catch(e=>{console.error(e);process.exit(1);});
