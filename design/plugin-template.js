/* Offline import helper. Creates real editable Figma nodes from the Android design. */
const DESIGN = __DESIGN__;
const ICON_PATHS = __ICONS__;
async function buildVoiceStudio() {
  const existing = figma.root.children.find(p=>p.name==='VoiceStudio 1.3.2 · 离线工作室 21屏');
  if(existing){await figma.setCurrentPageAsync(existing);figma.viewport.scrollAndZoomIntoView(existing.children);return {status:'existing',pageId:existing.id};}
  const fonts=await figma.listAvailableFontsAsync();
  const families=['Noto Sans SC','Noto Sans CJK SC','Source Han Sans SC'];
  const family=families.find(f=>fonts.some(x=>x.fontName.family===f&&x.fontName.style==='Regular'));
  if(!family)throw new Error('请安装 Noto Sans SC 字体后重试。');
  const regular={family,style:'Regular'};
  const boldEntry=fonts.find(f=>f.fontName.family===family&&f.fontName.style==='Bold');
  const bold=boldEntry?boldEntry.fontName:regular;
  await Promise.all([figma.loadFontAsync(regular),figma.loadFontAsync(bold)]);
  const page=figma.createPage();page.name='VoiceStudio 1.3.2 · 导入中';await figma.setCurrentPageAsync(page);
  const ids=[page.id],vars={},buttons={},links=[],navComponents={};let cardComponent=null,cardProperties={};
  const androidColors={bg:'MainActivity.BG',surface:'Color.WHITE',ink:'MainActivity.INK',muted:'MainActivity.MUTED',accent:'MainActivity.PURPLE',tint:'MainActivity.PALE',border:'MainActivity.LINE',success:'MainActivity.SUCCESS'};
  const color=h=>({r:parseInt(h.slice(1,3),16)/255,g:parseInt(h.slice(3,5),16)/255,b:parseInt(h.slice(5,7),16)/255});
  const primitives=figma.variables.createVariableCollection('VoiceStudio / Primitives');primitives.renameMode(primitives.modes[0].modeId,'Value');
  const semantic=figma.variables.createVariableCollection('VoiceStudio / Semantic');semantic.renameMode(semantic.modes[0].modeId,'Light');
  for(const [name,hex] of Object.entries(DESIGN.colors)){
    const p=figma.variables.createVariable('palette/'+name,primitives,'COLOR');p.setValueForMode(primitives.modes[0].modeId,color(hex));p.scopes=[];p.hiddenFromPublishing=true;p.setVariableCodeSyntax('ANDROID',androidColors[name]);p.setVariableCodeSyntax('WEB','var(--vs-palette-'+name+')');
    const v=figma.variables.createVariable('color/'+name,semantic,'COLOR');v.setValueForMode(semantic.modes[0].modeId,figma.variables.createVariableAlias(p));v.scopes=['FRAME_FILL','SHAPE_FILL','TEXT_FILL','STROKE_COLOR'];v.setVariableCodeSyntax('ANDROID',androidColors[name]);v.setVariableCodeSyntax('WEB','var(--vs-'+name+')');vars[name]=v;
  }
  for(const [name,value,scope] of [['radius/card',22,'CORNER_RADIUS'],['radius/button',15,'CORNER_RADIUS'],['space/padding',14,'GAP'],['space/section',20,'GAP'],['space/control',8,'GAP'],['space/icon-label',6,'GAP'],['space/card',18,'GAP'],['space/compact',9,'GAP'],['radius/input',13,'CORNER_RADIUS'],['radius/voice',20,'CORNER_RADIUS']]){
    const v=figma.variables.createVariable(name,semantic,'FLOAT');v.setValueForMode(semantic.modes[0].modeId,value);v.scopes=[scope];v.setVariableCodeSyntax('ANDROID','dp('+value+')');v.setVariableCodeSyntax('WEB','var(--vs-'+name.replace(/\//g,'-')+')');vars[name]=v;
  }
  function paint(name){return figma.variables.setBoundVariableForPaint({type:'SOLID',color:color(DESIGN.colors[name])},'color',vars[name]);}
  const styles={};
  for(const size of [11,12,13,14,15,16,17,18,22,23,25,28])for(const isBold of [false,true]){
    const style=figma.createTextStyle();style.name='VoiceStudio / '+size+' / '+(isBold?'Bold':'Regular');style.fontName=isBold?bold:regular;style.fontSize=size;style.lineHeight={unit:'PERCENT',value:150};styles[size+'-'+isBold]=style;
  }
  const componentArea=figma.createFrame();page.appendChild(componentArea);ids.push(componentArea.id);componentArea.name='组件 / Button';componentArea.resize(740,1450);componentArea.x=100;componentArea.y=1080;componentArea.fills=[];
  const variants=[];
  for(let i=0;i<4;i++){
    const type=i===3?'Secondary':'Primary',state=['Default','Loading','Disabled','Default'][i];
    const c=figma.createComponent();ids.push(c.id);componentArea.appendChild(c);c.name='Style='+type+', State='+state;c.resize(314,48);c.layoutMode='HORIZONTAL';c.primaryAxisSizingMode='FIXED';c.counterAxisSizingMode='FIXED';c.primaryAxisAlignItems='CENTER';c.counterAxisAlignItems='CENTER';c.cornerRadius=15;c.setBoundVariable('cornerRadius',vars['radius/button']);c.fills=[paint(type==='Primary'?'accent':'tint')];c.opacity=state==='Disabled'?.5:1;c.description='VoiceStudio 配音工作室的操作按钮。Label 为可编辑文字，状态为 Default、Loading 或 Disabled。';
    const label=figma.createText();ids.push(label.id);c.appendChild(label);label.fontName=bold;label.fontSize=15;await label.setTextStyleIdAsync(styles['15-true'].id);label.characters=state==='Loading'?'取消生成':'操作按钮';label.fills=[paint(type==='Primary'?'surface':'accent')];const key=c.addComponentProperty('Label','TEXT',label.characters);label.componentPropertyReferences={characters:key};variants.push(c);buttons[type+'/'+state]={component:c,key};
  }
  const set=figma.combineAsVariants(variants,componentArea);ids.push(set.id);set.name='VoiceStudio / Button';set.description='独立手机应用的主按钮、辅助按钮及生成状态。';set.x=16;set.y=16;set.resize(670,160);
  variants.forEach((c,i)=>{c.x=(i%2)*340;c.y=Math.floor(i/2)*72;});
  const screens=[];
  function frame(parent,name,x,y,w,h,fill,radius){const n=figma.createFrame();ids.push(n.id);parent.appendChild(n);n.name=name;n.resize(w,h);n.x=x;n.y=y;n.fills=fill?[paint(fill)]:[];n.cornerRadius=radius||0;n.clipsContent=false;return n;}
  async function layer(parent,s){
    let n;
    if(s.type==='group'){
      if(s.navIcon){
        const key=s.navIcon+'/'+s.selected;
        if(!navComponents[key]){
          const c=figma.createComponent();componentArea.appendChild(c);ids.push(c.id);c.name='Nav / '+s.navIcon+' / '+(s.selected?'Selected':'Default');c.resize(s.w,s.h);c.x=(Object.keys(navComponents).length%4)*165;c.y=230+Math.floor(Object.keys(navComponents).length/4)*85;c.layoutMode='VERTICAL';c.primaryAxisSizingMode='FIXED';c.counterAxisSizingMode='FIXED';c.primaryAxisAlignItems='CENTER';c.counterAxisAlignItems='CENTER';c.itemSpacing=6;c.setBoundVariable('itemSpacing',vars['space/icon-label']);c.fills=[paint(s.color)];c.cornerRadius=15;c.setBoundVariable('cornerRadius',vars['radius/button']);c.description='底部导航入口，分别提供默认与选中状态。';
          for(const child of s.children)await layer(c,child);
          navComponents[key]=c;
        }
        n=navComponents[key].createInstance();ids.push(n.id,...n.findAll(()=>true).map(x=>x.id));parent.appendChild(n);n.x=s.x;n.y=s.y;
      }else if(s.name==='作品卡片'){
        if(!cardComponent){
          const c=figma.createComponent();componentArea.appendChild(c);ids.push(c.id);c.name='VoiceStudio / WorkCard';c.resize(s.w,s.h);c.x=16;c.y=440;c.fills=[paint('surface')];c.cornerRadius=22;c.setBoundVariable('cornerRadius',vars['radius/card']);c.layoutMode='VERTICAL';c.primaryAxisSizingMode='FIXED';c.counterAxisSizingMode='FIXED';c.paddingLeft=18;c.paddingRight=18;c.paddingTop=18;c.paddingBottom=18;c.itemSpacing=9;for(const edge of ['paddingLeft','paddingRight','paddingTop','paddingBottom'])c.setBoundVariable(edge,vars['space/card']);c.setBoundVariable('itemSpacing',vars['space/compact']);c.description='作品条目，标题、音色时间与文件信息均可覆盖；含播放、导出、分享及管理菜单。';
          for(const child of s.children)await layer(c,child);
          for(const name of ['作品标题','作品信息','音频属性']){const text=c.findAll(n=>n.type==='TEXT'&&n.name===name)[0];const key=c.addComponentProperty(name,'TEXT',text.characters);text.componentPropertyReferences={characters:key};cardProperties[name]=key;}
          cardComponent=c;
        }
        n=cardComponent.createInstance();ids.push(n.id,...n.findAll(()=>true).map(x=>x.id));parent.appendChild(n);n.x=s.x;n.y=s.y;const properties={};
        function collect(nodes){for(const node of nodes){if(cardProperties[node.name])properties[cardProperties[node.name]]=node.text;if(node.children)collect(node.children);}}collect(s.children);n.setProperties(properties);
      }else{
        n=frame(parent,s.name,s.x,s.y,s.w,s.h,s.color,s.radius);if(s.radius===22)n.setBoundVariable('cornerRadius',vars['radius/card']);else if(s.radius===15)n.setBoundVariable('cornerRadius',vars['radius/button']);else if(s.radius===13)n.setBoundVariable('cornerRadius',vars['radius/input']);else if(s.radius===20)n.setBoundVariable('cornerRadius',vars['radius/voice']);
        if(s.layout){n.layoutMode=s.layout;n.primaryAxisSizingMode='FIXED';n.counterAxisSizingMode='FIXED';n.itemSpacing=s.gap||0;if(s.gap===8)n.setBoundVariable('itemSpacing',vars['space/control']);n.counterAxisAlignItems='CENTER';}
        for(const child of s.children)await layer(n,child);
      }
    }else if(s.type==='text'){
      n=figma.createText();ids.push(n.id);parent.appendChild(n);n.fontName=s.bold?bold:regular;n.fontSize=s.size;await n.setTextStyleIdAsync(styles[s.size+'-'+s.bold].id);n.characters=s.text;n.fills=[paint(s.color)];n.textAutoResize='HEIGHT';n.resize(s.w,Math.max(1,s.h));n.textAlignHorizontal=s.align==='center'?'CENTER':'LEFT';n.x=s.x;n.y=s.y;
    }else if(s.type==='rect'){
      n=figma.createRectangle();ids.push(n.id);parent.appendChild(n);n.resize(s.w,s.h);n.x=s.x;n.y=s.y;n.cornerRadius=s.radius;n.fills=[paint(s.color)];if(s.stroke){n.strokes=[paint(s.stroke)];n.strokeWeight=1;}
    }else if(s.type==='icon'){
      const svg='<svg xmlns="http://www.w3.org/2000/svg" width="32" height="32" viewBox="0 0 32 32"><path d="'+ICON_PATHS[s.icon]+'" fill="none" stroke="'+DESIGN.colors[s.color]+'" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/></svg>';
      n=figma.createNodeFromSvg(svg);parent.appendChild(n);n.resize(s.w,s.h);n.x=s.x;n.y=s.y;for(const vector of n.findAll(x=>x.type==='VECTOR')){if(Array.isArray(vector.strokes))vector.strokes=vector.strokes.map(p=>p.type==='SOLID'?figma.variables.setBoundVariableForPaint(p,'color',vars[s.color]):p);}ids.push(n.id,...n.findAll(()=>true).map(x=>x.id));
    }else if(s.type==='button'){
      const b=buttons[(s.primary?'Primary':'Secondary')+'/'+(s.state||'Default')];n=b.component.createInstance();ids.push(n.id,...n.findAll(()=>true).map(x=>x.id));parent.appendChild(n);n.resize(s.w,s.h);n.setProperties({[b.key]:s.text});n.x=s.x;n.y=s.y;
    }else throw new Error('Unknown layer '+s.type);
    if(s.opacity!==undefined)n.opacity=s.opacity;n.name=s.name;if(Number.isInteger(s.link))links.push({node:n,target:s.link});return n;
  }
  for(let i=0;i<DESIGN.screens.length;i++){
    const spec=DESIGN.screens[i],screen=frame(page,spec.name,100+(i%2)*490,100+Math.floor(i/2)*1060,spec.w,spec.h,'bg',0);screens.push(screen);
    for(const child of spec.children)await layer(screen,child);
    screen.clipsContent=true;
  }
  // Components stay outside the four mobile screen frames.
  componentArea.x=1120;componentArea.y=100;
  for(const link of links)await link.node.setReactionsAsync([{trigger:{type:'ON_CLICK'},actions:[{type:'NODE',destinationId:screens[link.target].id,navigation:'NAVIGATE',transition:null,preserveScrollPosition:false}]}]);
  page.name='VoiceStudio 1.3.2 · 离线工作室 21屏';figma.currentPage.selection=screens;figma.viewport.scrollAndZoomIntoView(screens);
  return {status:'created',pageId:page.id,font:family,screenIds:screens.map(s=>s.id),nodeIds:ids,variables:Object.values(vars).map(v=>v.id),prototypeLinks:links.length,navComponents:Object.keys(navComponents).length,workCardComponent:cardComponent.id};
}
buildVoiceStudio().then(result=>{console.log(JSON.stringify(result));figma.closePlugin(result.status==='existing'?'VoiceStudio 界面已存在，已打开。':'已创建 21 个 VoiceStudio 可编辑界面。');}).catch(error=>{console.error(error);figma.closePlugin('导入失败：'+error.message);});
