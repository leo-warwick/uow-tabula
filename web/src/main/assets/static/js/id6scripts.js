(function(window,undefined){var S={version:"3.0.3"};
var ua=navigator.userAgent.toLowerCase();
if(ua.indexOf("windows")>-1||ua.indexOf("win32")>-1){S.isWindows=true
}else{if(ua.indexOf("macintosh")>-1||ua.indexOf("mac os x")>-1){S.isMac=true
}else{if(ua.indexOf("linux")>-1){S.isLinux=true
}}}S.isIE=ua.indexOf("msie")>-1;
S.isIE6=ua.indexOf("msie 6")>-1;
S.isIE7=ua.indexOf("msie 7")>-1;
S.isGecko=ua.indexOf("gecko")>-1&&ua.indexOf("safari")==-1;
S.isWebKit=ua.indexOf("applewebkit/")>-1;
var inlineId=/#(.+)$/,galleryName=/^(light|shadow)box\[(.*?)\]/i,inlineParam=/\s*([a-z_]*?)\s*=\s*(.+)\s*/,fileExtension=/[0-9a-z]+$/i,scriptPath=/(.+\/)shadowbox\.js/i;
var open=false,initialized=false,lastOptions={},slideDelay=0,slideStart,slideTimer;
S.current=-1;
S.dimensions=null;
S.ease=function(state){return 1+Math.pow(state-1,3)
};
S.errorInfo={fla:{name:"Flash",url:"http://www.adobe.com/products/flashplayer/"},qt:{name:"QuickTime",url:"http://www.apple.com/quicktime/download/"},wmp:{name:"Windows Media Player",url:"http://www.microsoft.com/windows/windowsmedia/"},f4m:{name:"Flip4Mac",url:"http://www.flip4mac.com/wmv_download.htm"}};
S.gallery=[];
S.onReady=noop;
S.path=null;
S.player=null;
S.playerId="sb-player";
S.options={animate:true,animateFade:true,autoplayMovies:true,continuous:false,enableKeys:true,flashParams:{bgcolor:"#000000",allowfullscreen:true},flashVars:{},flashVersion:"9.0.115",handleOversize:"resize",handleUnsupported:"link",onChange:noop,onClose:noop,onFinish:noop,onOpen:noop,showMovieControls:true,skipSetup:false,slideshowDelay:0,viewportPadding:20};
S.getCurrent=function(){return S.current>-1?S.gallery[S.current]:null
};
S.hasNext=function(){return S.gallery.length>1&&(S.current!=S.gallery.length-1||S.options.continuous)
};
S.isOpen=function(){return open
};
S.isPaused=function(){return slideTimer=="pause"
};
S.applyOptions=function(options){lastOptions=apply({},S.options);
apply(S.options,options)
};
S.revertOptions=function(){apply(S.options,lastOptions)
};
S.init=function(options,callback){if(initialized){return
}initialized=true;
if(S.skin.options){apply(S.options,S.skin.options)
}if(options){apply(S.options,options)
}if(!S.path){var path,scripts=document.getElementsByTagName("script");
for(var i=0,len=scripts.length;
i<len;
++i){path=scriptPath.exec(scripts[i].src);
if(path){S.path=path[1];
break
}}}if(callback){S.onReady=callback
}bindLoad()
};
S.open=function(obj){if(open){return
}var gc=S.makeGallery(obj);
S.gallery=gc[0];
S.current=gc[1];
obj=S.getCurrent();
if(obj==null){return
}S.applyOptions(obj.options||{});
filterGallery();
if(S.gallery.length){obj=S.getCurrent();
if(S.options.onOpen(obj)===false){return
}open=true;
S.skin.onOpen(obj,load)
}};
S.close=function(){if(!open){return
}open=false;
if(S.player){S.player.remove();
S.player=null
}if(typeof slideTimer=="number"){clearTimeout(slideTimer);
slideTimer=null
}slideDelay=0;
listenKeys(false);
S.options.onClose(S.getCurrent());
S.skin.onClose();
S.revertOptions()
};
S.play=function(){if(!S.hasNext()){return
}if(!slideDelay){slideDelay=S.options.slideshowDelay*1000
}if(slideDelay){slideStart=now();
slideTimer=setTimeout(function(){slideDelay=slideStart=0;
S.next()
},slideDelay);
if(S.skin.onPlay){S.skin.onPlay()
}}};
S.pause=function(){if(typeof slideTimer!="number"){return
}slideDelay=Math.max(0,slideDelay-(now()-slideStart));
if(slideDelay){clearTimeout(slideTimer);
slideTimer="pause";
if(S.skin.onPause){S.skin.onPause()
}}};
S.change=function(index){if(!(index in S.gallery)){if(S.options.continuous){index=(index<0?S.gallery.length+index:0);
if(!(index in S.gallery)){return
}}else{return
}}S.current=index;
if(typeof slideTimer=="number"){clearTimeout(slideTimer);
slideTimer=null;
slideDelay=slideStart=0
}S.options.onChange(S.getCurrent());
load(true)
};
S.next=function(){S.change(S.current+1)
};
S.previous=function(){S.change(S.current-1)
};
S.setDimensions=function(height,width,maxHeight,maxWidth,topBottom,leftRight,padding,preserveAspect){var originalHeight=height,originalWidth=width;
var extraHeight=2*padding+topBottom;
if(height+extraHeight>maxHeight){height=maxHeight-extraHeight
}var extraWidth=2*padding+leftRight;
if(width+extraWidth>maxWidth){width=maxWidth-extraWidth
}var changeHeight=(originalHeight-height)/originalHeight,changeWidth=(originalWidth-width)/originalWidth,oversized=(changeHeight>0||changeWidth>0);
if(preserveAspect&&oversized){if(changeHeight>changeWidth){width=Math.round((originalWidth/originalHeight)*height)
}else{if(changeWidth>changeHeight){height=Math.round((originalHeight/originalWidth)*width)
}}}S.dimensions={height:height+topBottom,width:width+leftRight,innerHeight:height,innerWidth:width,top:Math.floor((maxHeight-(height+extraHeight))/2+padding),left:Math.floor((maxWidth-(width+extraWidth))/2+padding),oversized:oversized};
return S.dimensions
};
S.makeGallery=function(obj){var gallery=[],current=-1;
if(typeof obj=="string"){obj=[obj]
}if(typeof obj.length=="number"){each(obj,function(i,o){if(o.content){gallery[i]=o
}else{gallery[i]={content:o}
}});
current=0
}else{if(obj.tagName){var cacheObj=S.getCache(obj);
obj=cacheObj?cacheObj:S.makeObject(obj)
}if(obj.gallery){gallery=[];
var o;
for(var key in S.cache){o=S.cache[key];
if(o.gallery&&o.gallery==obj.gallery){if(current==-1&&o.content==obj.content){current=gallery.length
}gallery.push(o)
}}if(current==-1){gallery.unshift(obj);
current=0
}}else{gallery=[obj];
current=0
}}each(gallery,function(i,o){gallery[i]=apply({},o)
});
return[gallery,current]
};
S.makeObject=function(link,options){var obj={content:link.href,title:link.getAttribute("title")||"",link:link};
if(options){options=apply({},options);
each(["player","title","height","width","gallery"],function(i,o){if(typeof options[o]!="undefined"){obj[o]=options[o];
delete options[o]
}});
obj.options=options
}else{obj.options={}
}if(!obj.player){obj.player=S.getPlayer(obj.content)
}var rel=link.getAttribute("rel");
if(rel){var match=rel.match(galleryName);
if(match){obj.gallery=escape(match[2])
}each(rel.split(";"),function(i,p){match=p.match(inlineParam);
if(match){obj[match[1]]=match[2]
}})
}return obj
};
S.getPlayer=function(content){if(content.indexOf("#")>-1&&content.indexOf(document.location.href)==0){return"inline"
}var q=content.indexOf("?");
if(q>-1){content=content.substring(0,q)
}var ext,m=content.match(fileExtension);
if(m){ext=m[0].toLowerCase()
}if(ext){if(S.img&&S.img.ext.indexOf(ext)>-1){return"img"
}if(S.swf&&S.swf.ext.indexOf(ext)>-1){return"swf"
}if(S.flv&&S.flv.ext.indexOf(ext)>-1){return"flv"
}if(S.qt&&S.qt.ext.indexOf(ext)>-1){if(S.wmp&&S.wmp.ext.indexOf(ext)>-1){return"qtwmp"
}else{return"qt"
}}if(S.wmp&&S.wmp.ext.indexOf(ext)>-1){return"wmp"
}}return"iframe"
};
function filterGallery(){var err=S.errorInfo,plugins=S.plugins,obj,remove,needed,m,format,replace,inlineEl,flashVersion;
for(var i=0;
i<S.gallery.length;
++i){obj=S.gallery[i];
remove=false;
needed=null;
switch(obj.player){case"flv":case"swf":if(!plugins.fla){needed="fla"
}break;
case"qt":if(!plugins.qt){needed="qt"
}break;
case"wmp":if(S.isMac){if(plugins.qt&&plugins.f4m){obj.player="qt"
}else{needed="qtf4m"
}}else{if(!plugins.wmp){needed="wmp"
}}break;
case"qtwmp":if(plugins.qt){obj.player="qt"
}else{if(plugins.wmp){obj.player="wmp"
}else{needed="qtwmp"
}}break
}if(needed){if(S.options.handleUnsupported=="link"){switch(needed){case"qtf4m":format="shared";
replace=[err.qt.url,err.qt.name,err.f4m.url,err.f4m.name];
break;
case"qtwmp":format="either";
replace=[err.qt.url,err.qt.name,err.wmp.url,err.wmp.name];
break;
default:format="single";
replace=[err[needed].url,err[needed].name]
}obj.player="html";
obj.content='<div class="sb-message">'+sprintf(S.lang.errors[format],replace)+"</div>"
}else{remove=true
}}else{if(obj.player=="inline"){m=inlineId.exec(obj.content);
if(m){inlineEl=get(m[1]);
if(inlineEl){obj.content=inlineEl.innerHTML
}else{remove=true
}}else{remove=true
}}else{if(obj.player=="swf"||obj.player=="flv"){flashVersion=(obj.options&&obj.options.flashVersion)||S.options.flashVersion;
if(S.flash&&!S.flash.hasFlashPlayerVersion(flashVersion)){obj.width=310;
obj.height=177
}}}}if(remove){S.gallery.splice(i,1);
if(i<S.current){--S.current
}else{if(i==S.current){S.current=i>0?i-1:i
}}--i
}}}function listenKeys(on){if(!S.options.enableKeys){return
}(on?addEvent:removeEvent)(document,"keydown",handleKey)
}function handleKey(e){if(e.metaKey||e.shiftKey||e.altKey||e.ctrlKey){return
}var code=keyCode(e),handler;
switch(code){case 81:case 88:case 27:handler=S.close;
break;
case 37:handler=S.previous;
break;
case 39:handler=S.next;
break;
case 32:handler=typeof slideTimer=="number"?S.pause:S.play;
break
}if(handler){preventDefault(e);
handler()
}}function load(changing){listenKeys(false);
var obj=S.getCurrent();
var player=(obj.player=="inline"?"html":obj.player);
if(typeof S[player]!="function"){throw"unknown player "+player
}if(changing){S.player.remove();
S.revertOptions();
S.applyOptions(obj.options||{})
}S.player=new S[player](obj,S.playerId);
if(S.gallery.length>1){var next=S.gallery[S.current+1]||S.gallery[0];
if(next.player=="img"){var a=new Image();
a.src=next.content
}var prev=S.gallery[S.current-1]||S.gallery[S.gallery.length-1];
if(prev.player=="img"){var b=new Image();
b.src=prev.content
}}S.skin.onLoad(changing,waitReady)
}function waitReady(){if(!open){return
}if(typeof S.player.ready!="undefined"){var timer=setInterval(function(){if(open){if(S.player.ready){clearInterval(timer);
timer=null;
S.skin.onReady(show)
}}else{clearInterval(timer);
timer=null
}},10)
}else{S.skin.onReady(show)
}}function show(){if(!open){return
}S.player.append(S.skin.body,S.dimensions);
S.skin.onShow(finish)
}function finish(){if(!open){return
}if(S.player.onLoad){S.player.onLoad()
}S.options.onFinish(S.getCurrent());
if(!S.isPaused()){S.play()
}listenKeys(true)
}if(!Array.prototype.indexOf){Array.prototype.indexOf=function(obj,from){var len=this.length>>>0;
from=from||0;
if(from<0){from+=len
}for(;
from<len;
++from){if(from in this&&this[from]===obj){return from
}}return -1
}
}function now(){return(new Date).getTime()
}function apply(original,extension){for(var property in extension){original[property]=extension[property]
}return original
}function each(obj,callback){var i=0,len=obj.length;
for(var value=obj[0];
i<len&&callback.call(value,i,value)!==false;
value=obj[++i]){}}function sprintf(str,replace){return str.replace(/\{(\w+?)\}/g,function(match,i){return replace[i]
})
}function noop(){}function get(id){return document.getElementById(id)
}function remove(el){el.parentNode.removeChild(el)
}var supportsOpacity=true,supportsFixed=true;
function checkSupport(){var body=document.body,div=document.createElement("div");
supportsOpacity=typeof div.style.opacity==="string";
div.style.position="fixed";
div.style.margin=0;
div.style.top="20px";
body.appendChild(div,body.firstChild);
supportsFixed=div.offsetTop==20;
body.removeChild(div)
}S.getStyle=(function(){var opacity=/opacity=([^)]*)/,getComputedStyle=document.defaultView&&document.defaultView.getComputedStyle;
return function(el,style){var ret;
if(!supportsOpacity&&style=="opacity"&&el.currentStyle){ret=opacity.test(el.currentStyle.filter||"")?(parseFloat(RegExp.$1)/100)+"":"";
return ret===""?"1":ret
}if(getComputedStyle){var computedStyle=getComputedStyle(el,null);
if(computedStyle){ret=computedStyle[style]
}if(style=="opacity"&&ret==""){ret="1"
}}else{ret=el.currentStyle[style]
}return ret
}
})();
S.appendHTML=function(el,html){if(el.insertAdjacentHTML){el.insertAdjacentHTML("BeforeEnd",html)
}else{if(el.lastChild){var range=el.ownerDocument.createRange();
range.setStartAfter(el.lastChild);
var frag=range.createContextualFragment(html);
el.appendChild(frag)
}else{el.innerHTML=html
}}};
S.getWindowSize=function(dimension){if(document.compatMode==="CSS1Compat"){return document.documentElement["client"+dimension]
}return document.body["client"+dimension]
};
S.setOpacity=function(el,opacity){var style=el.style;
if(supportsOpacity){style.opacity=(opacity==1?"":opacity)
}else{style.zoom=1;
if(opacity==1){if(typeof style.filter=="string"&&(/alpha/i).test(style.filter)){style.filter=style.filter.replace(/\s*[\w\.]*alpha\([^\)]*\);?/gi,"")
}}else{style.filter=(style.filter||"").replace(/\s*[\w\.]*alpha\([^\)]*\)/gi,"")+" alpha(opacity="+(opacity*100)+")"
}}};
S.clearOpacity=function(el){S.setOpacity(el,1)
};
function getTarget(e){return e.target
}function getPageXY(e){return[e.pageX,e.pageY]
}function preventDefault(e){e.preventDefault()
}function keyCode(e){return e.keyCode
}function addEvent(el,type,handler){jQuery(el).bind(type,handler)
}function removeEvent(el,type,handler){jQuery(el).unbind(type,handler)
}jQuery.fn.shadowbox=function(options){return this.each(function(){var el=jQuery(this);
var opts=jQuery.extend({},options||{},jQuery.metadata?el.metadata():jQuery.meta?el.data():{});
var cls=this.className||"";
opts.width=parseInt((cls.match(/w:(\d+)/)||[])[1])||opts.width;
opts.height=parseInt((cls.match(/h:(\d+)/)||[])[1])||opts.height;
Shadowbox.setup(el,opts)
})
};
var loaded=false,DOMContentLoaded;
if(document.addEventListener){DOMContentLoaded=function(){document.removeEventListener("DOMContentLoaded",DOMContentLoaded,false);
S.load()
}
}else{if(document.attachEvent){DOMContentLoaded=function(){if(document.readyState==="complete"){document.detachEvent("onreadystatechange",DOMContentLoaded);
S.load()
}}
}}function doScrollCheck(){if(loaded){return
}try{document.documentElement.doScroll("left")
}catch(e){setTimeout(doScrollCheck,1);
return
}S.load()
}function bindLoad(){if(document.readyState==="complete"){return S.load()
}if(document.addEventListener){document.addEventListener("DOMContentLoaded",DOMContentLoaded,false);
window.addEventListener("load",S.load,false)
}else{if(document.attachEvent){document.attachEvent("onreadystatechange",DOMContentLoaded);
window.attachEvent("onload",S.load);
var topLevel=false;
try{topLevel=window.frameElement===null
}catch(e){}if(document.documentElement.doScroll&&topLevel){doScrollCheck()
}}}}S.load=function(){if(loaded){return
}if(!document.body){return setTimeout(S.load,13)
}loaded=true;
checkSupport();
S.onReady();
if(!S.options.skipSetup){S.setup()
}S.skin.init()
};
S.plugins={};
if(navigator.plugins&&navigator.plugins.length){var names=[];
each(navigator.plugins,function(i,p){names.push(p.name)
});
names=names.join(",");
var f4m=names.indexOf("Flip4Mac")>-1;
S.plugins={fla:names.indexOf("Shockwave Flash")>-1,qt:names.indexOf("QuickTime")>-1,wmp:!f4m&&names.indexOf("Windows Media")>-1,f4m:f4m}
}else{var detectPlugin=function(name){var axo;
try{axo=new ActiveXObject(name)
}catch(e){}return !!axo
};
S.plugins={fla:detectPlugin("ShockwaveFlash.ShockwaveFlash"),qt:detectPlugin("QuickTime.QuickTime"),wmp:detectPlugin("wmplayer.ocx"),f4m:false}
}var relAttr=/^(light|shadow)box/i,expando="shadowboxCacheKey",cacheKey=1;
S.cache={};
S.select=function(selector){var links=[];
if(!selector){var rel;
each(document.getElementsByTagName("a"),function(i,el){rel=el.getAttribute("rel");
if(rel&&relAttr.test(rel)){links.push(el)
}})
}else{var length=selector.length;
if(length){if(typeof selector=="string"){if(S.find){links=S.find(selector)
}}else{if(length==2&&typeof selector[0]=="string"&&selector[1].nodeType){if(S.find){links=S.find(selector[0],selector[1])
}}else{for(var i=0;
i<length;
++i){links[i]=selector[i]
}}}}else{links.push(selector)
}}return links
};
S.setup=function(selector,options){each(S.select(selector),function(i,link){S.addCache(link,options)
})
};
S.teardown=function(selector){each(S.select(selector),function(i,link){S.removeCache(link)
})
};
S.addCache=function(link,options){var key=link[expando];
if(key==undefined){key=cacheKey++;
link[expando]=key;
addEvent(link,"click",handleClick)
}S.cache[key]=S.makeObject(link,options)
};
S.removeCache=function(link){removeEvent(link,"click",handleClick);
delete S.cache[link[expando]];
link[expando]=null
};
S.getCache=function(link){var key=link[expando];
return(key in S.cache&&S.cache[key])
};
S.clearCache=function(){for(var key in S.cache){S.removeCache(S.cache[key].link)
}S.cache={}
};
function handleClick(e){S.open(this);
if(S.gallery.length){preventDefault(e)
}}S.find=(function(){var chunker=/((?:\((?:\([^()]+\)|[^()]+)+\)|\[(?:\[[^[\]]*\]|['"][^'"]*['"]|[^[\]'"]+)+\]|\\.|[^ >+~,(\[\\]+)+|[>+~])(\s*,\s*)?((?:.|\r|\n)*)/g,done=0,toString=Object.prototype.toString,hasDuplicate=false,baseHasDuplicate=true;
[0,0].sort(function(){baseHasDuplicate=false;
return 0
});
var Sizzle=function(selector,context,results,seed){results=results||[];
var origContext=context=context||document;
if(context.nodeType!==1&&context.nodeType!==9){return[]
}if(!selector||typeof selector!=="string"){return results
}var parts=[],m,set,checkSet,extra,prune=true,contextXML=isXML(context),soFar=selector;
while((chunker.exec(""),m=chunker.exec(soFar))!==null){soFar=m[3];
parts.push(m[1]);
if(m[2]){extra=m[3];
break
}}if(parts.length>1&&origPOS.exec(selector)){if(parts.length===2&&Expr.relative[parts[0]]){set=posProcess(parts[0]+parts[1],context)
}else{set=Expr.relative[parts[0]]?[context]:Sizzle(parts.shift(),context);
while(parts.length){selector=parts.shift();
if(Expr.relative[selector]){selector+=parts.shift()
}set=posProcess(selector,set)
}}}else{if(!seed&&parts.length>1&&context.nodeType===9&&!contextXML&&Expr.match.ID.test(parts[0])&&!Expr.match.ID.test(parts[parts.length-1])){var ret=Sizzle.find(parts.shift(),context,contextXML);
context=ret.expr?Sizzle.filter(ret.expr,ret.set)[0]:ret.set[0]
}if(context){var ret=seed?{expr:parts.pop(),set:makeArray(seed)}:Sizzle.find(parts.pop(),parts.length===1&&(parts[0]==="~"||parts[0]==="+")&&context.parentNode?context.parentNode:context,contextXML);
set=ret.expr?Sizzle.filter(ret.expr,ret.set):ret.set;
if(parts.length>0){checkSet=makeArray(set)
}else{prune=false
}while(parts.length){var cur=parts.pop(),pop=cur;
if(!Expr.relative[cur]){cur=""
}else{pop=parts.pop()
}if(pop==null){pop=context
}Expr.relative[cur](checkSet,pop,contextXML)
}}else{checkSet=parts=[]
}}if(!checkSet){checkSet=set
}if(!checkSet){throw"Syntax error, unrecognized expression: "+(cur||selector)
}if(toString.call(checkSet)==="[object Array]"){if(!prune){results.push.apply(results,checkSet)
}else{if(context&&context.nodeType===1){for(var i=0;
checkSet[i]!=null;
i++){if(checkSet[i]&&(checkSet[i]===true||checkSet[i].nodeType===1&&contains(context,checkSet[i]))){results.push(set[i])
}}}else{for(var i=0;
checkSet[i]!=null;
i++){if(checkSet[i]&&checkSet[i].nodeType===1){results.push(set[i])
}}}}}else{makeArray(checkSet,results)
}if(extra){Sizzle(extra,origContext,results,seed);
Sizzle.uniqueSort(results)
}return results
};
Sizzle.uniqueSort=function(results){if(sortOrder){hasDuplicate=baseHasDuplicate;
results.sort(sortOrder);
if(hasDuplicate){for(var i=1;
i<results.length;
i++){if(results[i]===results[i-1]){results.splice(i--,1)
}}}}return results
};
Sizzle.matches=function(expr,set){return Sizzle(expr,null,null,set)
};
Sizzle.find=function(expr,context,isXML){var set,match;
if(!expr){return[]
}for(var i=0,l=Expr.order.length;
i<l;
i++){var type=Expr.order[i],match;
if((match=Expr.leftMatch[type].exec(expr))){var left=match[1];
match.splice(1,1);
if(left.substr(left.length-1)!=="\\"){match[1]=(match[1]||"").replace(/\\/g,"");
set=Expr.find[type](match,context,isXML);
if(set!=null){expr=expr.replace(Expr.match[type],"");
break
}}}}if(!set){set=context.getElementsByTagName("*")
}return{set:set,expr:expr}
};
Sizzle.filter=function(expr,set,inplace,not){var old=expr,result=[],curLoop=set,match,anyFound,isXMLFilter=set&&set[0]&&isXML(set[0]);
while(expr&&set.length){for(var type in Expr.filter){if((match=Expr.match[type].exec(expr))!=null){var filter=Expr.filter[type],found,item;
anyFound=false;
if(curLoop===result){result=[]
}if(Expr.preFilter[type]){match=Expr.preFilter[type](match,curLoop,inplace,result,not,isXMLFilter);
if(!match){anyFound=found=true
}else{if(match===true){continue
}}}if(match){for(var i=0;
(item=curLoop[i])!=null;
i++){if(item){found=filter(item,match,i,curLoop);
var pass=not^!!found;
if(inplace&&found!=null){if(pass){anyFound=true
}else{curLoop[i]=false
}}else{if(pass){result.push(item);
anyFound=true
}}}}}if(found!==undefined){if(!inplace){curLoop=result
}expr=expr.replace(Expr.match[type],"");
if(!anyFound){return[]
}break
}}}if(expr===old){if(anyFound==null){throw"Syntax error, unrecognized expression: "+expr
}else{break
}}old=expr
}return curLoop
};
var Expr=Sizzle.selectors={order:["ID","NAME","TAG"],match:{ID:/#((?:[\w\u00c0-\uFFFF-]|\\.)+)/,CLASS:/\.((?:[\w\u00c0-\uFFFF-]|\\.)+)/,NAME:/\[name=['"]*((?:[\w\u00c0-\uFFFF-]|\\.)+)['"]*\]/,ATTR:/\[\s*((?:[\w\u00c0-\uFFFF-]|\\.)+)\s*(?:(\S?=)\s*(['"]*)(.*?)\3|)\s*\]/,TAG:/^((?:[\w\u00c0-\uFFFF\*-]|\\.)+)/,CHILD:/:(only|nth|last|first)-child(?:\((even|odd|[\dn+-]*)\))?/,POS:/:(nth|eq|gt|lt|first|last|even|odd)(?:\((\d*)\))?(?=[^-]|$)/,PSEUDO:/:((?:[\w\u00c0-\uFFFF-]|\\.)+)(?:\((['"]*)((?:\([^\)]+\)|[^\2\(\)]*)+)\2\))?/},leftMatch:{},attrMap:{"class":"className","for":"htmlFor"},attrHandle:{href:function(elem){return elem.getAttribute("href")
}},relative:{"+":function(checkSet,part){var isPartStr=typeof part==="string",isTag=isPartStr&&!/\W/.test(part),isPartStrNotTag=isPartStr&&!isTag;
if(isTag){part=part.toLowerCase()
}for(var i=0,l=checkSet.length,elem;
i<l;
i++){if((elem=checkSet[i])){while((elem=elem.previousSibling)&&elem.nodeType!==1){}checkSet[i]=isPartStrNotTag||elem&&elem.nodeName.toLowerCase()===part?elem||false:elem===part
}}if(isPartStrNotTag){Sizzle.filter(part,checkSet,true)
}},">":function(checkSet,part){var isPartStr=typeof part==="string";
if(isPartStr&&!/\W/.test(part)){part=part.toLowerCase();
for(var i=0,l=checkSet.length;
i<l;
i++){var elem=checkSet[i];
if(elem){var parent=elem.parentNode;
checkSet[i]=parent.nodeName.toLowerCase()===part?parent:false
}}}else{for(var i=0,l=checkSet.length;
i<l;
i++){var elem=checkSet[i];
if(elem){checkSet[i]=isPartStr?elem.parentNode:elem.parentNode===part
}}if(isPartStr){Sizzle.filter(part,checkSet,true)
}}},"":function(checkSet,part,isXML){var doneName=done++,checkFn=dirCheck;
if(typeof part==="string"&&!/\W/.test(part)){var nodeCheck=part=part.toLowerCase();
checkFn=dirNodeCheck
}checkFn("parentNode",part,doneName,checkSet,nodeCheck,isXML)
},"~":function(checkSet,part,isXML){var doneName=done++,checkFn=dirCheck;
if(typeof part==="string"&&!/\W/.test(part)){var nodeCheck=part=part.toLowerCase();
checkFn=dirNodeCheck
}checkFn("previousSibling",part,doneName,checkSet,nodeCheck,isXML)
}},find:{ID:function(match,context,isXML){if(typeof context.getElementById!=="undefined"&&!isXML){var m=context.getElementById(match[1]);
return m?[m]:[]
}},NAME:function(match,context){if(typeof context.getElementsByName!=="undefined"){var ret=[],results=context.getElementsByName(match[1]);
for(var i=0,l=results.length;
i<l;
i++){if(results[i].getAttribute("name")===match[1]){ret.push(results[i])
}}return ret.length===0?null:ret
}},TAG:function(match,context){return context.getElementsByTagName(match[1])
}},preFilter:{CLASS:function(match,curLoop,inplace,result,not,isXML){match=" "+match[1].replace(/\\/g,"")+" ";
if(isXML){return match
}for(var i=0,elem;
(elem=curLoop[i])!=null;
i++){if(elem){if(not^(elem.className&&(" "+elem.className+" ").replace(/[\t\n]/g," ").indexOf(match)>=0)){if(!inplace){result.push(elem)
}}else{if(inplace){curLoop[i]=false
}}}}return false
},ID:function(match){return match[1].replace(/\\/g,"")
},TAG:function(match,curLoop){return match[1].toLowerCase()
},CHILD:function(match){if(match[1]==="nth"){var test=/(-?)(\d*)n((?:\+|-)?\d*)/.exec(match[2]==="even"&&"2n"||match[2]==="odd"&&"2n+1"||!/\D/.test(match[2])&&"0n+"+match[2]||match[2]);
match[2]=(test[1]+(test[2]||1))-0;
match[3]=test[3]-0
}match[0]=done++;
return match
},ATTR:function(match,curLoop,inplace,result,not,isXML){var name=match[1].replace(/\\/g,"");
if(!isXML&&Expr.attrMap[name]){match[1]=Expr.attrMap[name]
}if(match[2]==="~="){match[4]=" "+match[4]+" "
}return match
},PSEUDO:function(match,curLoop,inplace,result,not){if(match[1]==="not"){if((chunker.exec(match[3])||"").length>1||/^\w/.test(match[3])){match[3]=Sizzle(match[3],null,null,curLoop)
}else{var ret=Sizzle.filter(match[3],curLoop,inplace,true^not);
if(!inplace){result.push.apply(result,ret)
}return false
}}else{if(Expr.match.POS.test(match[0])||Expr.match.CHILD.test(match[0])){return true
}}return match
},POS:function(match){match.unshift(true);
return match
}},filters:{enabled:function(elem){return elem.disabled===false&&elem.type!=="hidden"
},disabled:function(elem){return elem.disabled===true
},checked:function(elem){return elem.checked===true
},selected:function(elem){elem.parentNode.selectedIndex;
return elem.selected===true
},parent:function(elem){return !!elem.firstChild
},empty:function(elem){return !elem.firstChild
},has:function(elem,i,match){return !!Sizzle(match[3],elem).length
},header:function(elem){return/h\d/i.test(elem.nodeName)
},text:function(elem){return"text"===elem.type
},radio:function(elem){return"radio"===elem.type
},checkbox:function(elem){return"checkbox"===elem.type
},file:function(elem){return"file"===elem.type
},password:function(elem){return"password"===elem.type
},submit:function(elem){return"submit"===elem.type
},image:function(elem){return"image"===elem.type
},reset:function(elem){return"reset"===elem.type
},button:function(elem){return"button"===elem.type||elem.nodeName.toLowerCase()==="button"
},input:function(elem){return/input|select|textarea|button/i.test(elem.nodeName)
}},setFilters:{first:function(elem,i){return i===0
},last:function(elem,i,match,array){return i===array.length-1
},even:function(elem,i){return i%2===0
},odd:function(elem,i){return i%2===1
},lt:function(elem,i,match){return i<match[3]-0
},gt:function(elem,i,match){return i>match[3]-0
},nth:function(elem,i,match){return match[3]-0===i
},eq:function(elem,i,match){return match[3]-0===i
}},filter:{PSEUDO:function(elem,match,i,array){var name=match[1],filter=Expr.filters[name];
if(filter){return filter(elem,i,match,array)
}else{if(name==="contains"){return(elem.textContent||elem.innerText||getText([elem])||"").indexOf(match[3])>=0
}else{if(name==="not"){var not=match[3];
for(var i=0,l=not.length;
i<l;
i++){if(not[i]===elem){return false
}}return true
}else{throw"Syntax error, unrecognized expression: "+name
}}}},CHILD:function(elem,match){var type=match[1],node=elem;
switch(type){case"only":case"first":while((node=node.previousSibling)){if(node.nodeType===1){return false
}}if(type==="first"){return true
}node=elem;
case"last":while((node=node.nextSibling)){if(node.nodeType===1){return false
}}return true;
case"nth":var first=match[2],last=match[3];
if(first===1&&last===0){return true
}var doneName=match[0],parent=elem.parentNode;
if(parent&&(parent.sizcache!==doneName||!elem.nodeIndex)){var count=0;
for(node=parent.firstChild;
node;
node=node.nextSibling){if(node.nodeType===1){node.nodeIndex=++count
}}parent.sizcache=doneName
}var diff=elem.nodeIndex-last;
if(first===0){return diff===0
}else{return(diff%first===0&&diff/first>=0)
}}},ID:function(elem,match){return elem.nodeType===1&&elem.getAttribute("id")===match
},TAG:function(elem,match){return(match==="*"&&elem.nodeType===1)||elem.nodeName.toLowerCase()===match
},CLASS:function(elem,match){return(" "+(elem.className||elem.getAttribute("class"))+" ").indexOf(match)>-1
},ATTR:function(elem,match){var name=match[1],result=Expr.attrHandle[name]?Expr.attrHandle[name](elem):elem[name]!=null?elem[name]:elem.getAttribute(name),value=result+"",type=match[2],check=match[4];
return result==null?type==="!=":type==="="?value===check:type==="*="?value.indexOf(check)>=0:type==="~="?(" "+value+" ").indexOf(check)>=0:!check?value&&result!==false:type==="!="?value!==check:type==="^="?value.indexOf(check)===0:type==="$="?value.substr(value.length-check.length)===check:type==="|="?value===check||value.substr(0,check.length+1)===check+"-":false
},POS:function(elem,match,i,array){var name=match[2],filter=Expr.setFilters[name];
if(filter){return filter(elem,i,match,array)
}}}};
var origPOS=Expr.match.POS;
for(var type in Expr.match){Expr.match[type]=new RegExp(Expr.match[type].source+/(?![^\[]*\])(?![^\(]*\))/.source);
Expr.leftMatch[type]=new RegExp(/(^(?:.|\r|\n)*?)/.source+Expr.match[type].source)
}var makeArray=function(array,results){array=Array.prototype.slice.call(array,0);
if(results){results.push.apply(results,array);
return results
}return array
};
try{Array.prototype.slice.call(document.documentElement.childNodes,0)
}catch(e){makeArray=function(array,results){var ret=results||[];
if(toString.call(array)==="[object Array]"){Array.prototype.push.apply(ret,array)
}else{if(typeof array.length==="number"){for(var i=0,l=array.length;
i<l;
i++){ret.push(array[i])
}}else{for(var i=0;
array[i];
i++){ret.push(array[i])
}}}return ret
}
}var sortOrder;
if(document.documentElement.compareDocumentPosition){sortOrder=function(a,b){if(!a.compareDocumentPosition||!b.compareDocumentPosition){if(a==b){hasDuplicate=true
}return a.compareDocumentPosition?-1:1
}var ret=a.compareDocumentPosition(b)&4?-1:a===b?0:1;
if(ret===0){hasDuplicate=true
}return ret
}
}else{if("sourceIndex" in document.documentElement){sortOrder=function(a,b){if(!a.sourceIndex||!b.sourceIndex){if(a==b){hasDuplicate=true
}return a.sourceIndex?-1:1
}var ret=a.sourceIndex-b.sourceIndex;
if(ret===0){hasDuplicate=true
}return ret
}
}else{if(document.createRange){sortOrder=function(a,b){if(!a.ownerDocument||!b.ownerDocument){if(a==b){hasDuplicate=true
}return a.ownerDocument?-1:1
}var aRange=a.ownerDocument.createRange(),bRange=b.ownerDocument.createRange();
aRange.setStart(a,0);
aRange.setEnd(a,0);
bRange.setStart(b,0);
bRange.setEnd(b,0);
var ret=aRange.compareBoundaryPoints(Range.START_TO_END,bRange);
if(ret===0){hasDuplicate=true
}return ret
}
}}}function getText(elems){var ret="",elem;
for(var i=0;
elems[i];
i++){elem=elems[i];
if(elem.nodeType===3||elem.nodeType===4){ret+=elem.nodeValue
}else{if(elem.nodeType!==8){ret+=getText(elem.childNodes)
}}}return ret
}(function(){var form=document.createElement("div"),id="script"+(new Date).getTime();
form.innerHTML="<a name='"+id+"'/>";
var root=document.documentElement;
root.insertBefore(form,root.firstChild);
if(document.getElementById(id)){Expr.find.ID=function(match,context,isXML){if(typeof context.getElementById!=="undefined"&&!isXML){var m=context.getElementById(match[1]);
return m?m.id===match[1]||typeof m.getAttributeNode!=="undefined"&&m.getAttributeNode("id").nodeValue===match[1]?[m]:undefined:[]
}};
Expr.filter.ID=function(elem,match){var node=typeof elem.getAttributeNode!=="undefined"&&elem.getAttributeNode("id");
return elem.nodeType===1&&node&&node.nodeValue===match
}
}root.removeChild(form);
root=form=null
})();
(function(){var div=document.createElement("div");
div.appendChild(document.createComment(""));
if(div.getElementsByTagName("*").length>0){Expr.find.TAG=function(match,context){var results=context.getElementsByTagName(match[1]);
if(match[1]==="*"){var tmp=[];
for(var i=0;
results[i];
i++){if(results[i].nodeType===1){tmp.push(results[i])
}}results=tmp
}return results
}
}div.innerHTML="<a href='#'></a>";
if(div.firstChild&&typeof div.firstChild.getAttribute!=="undefined"&&div.firstChild.getAttribute("href")!=="#"){Expr.attrHandle.href=function(elem){return elem.getAttribute("href",2)
}
}div=null
})();
if(document.querySelectorAll){(function(){var oldSizzle=Sizzle,div=document.createElement("div");
div.innerHTML="<p class='TEST'></p>";
if(div.querySelectorAll&&div.querySelectorAll(".TEST").length===0){return
}Sizzle=function(query,context,extra,seed){context=context||document;
if(!seed&&context.nodeType===9&&!isXML(context)){try{return makeArray(context.querySelectorAll(query),extra)
}catch(e){}}return oldSizzle(query,context,extra,seed)
};
for(var prop in oldSizzle){Sizzle[prop]=oldSizzle[prop]
}div=null
})()
}(function(){var div=document.createElement("div");
div.innerHTML="<div class='test e'></div><div class='test'></div>";
if(!div.getElementsByClassName||div.getElementsByClassName("e").length===0){return
}div.lastChild.className="e";
if(div.getElementsByClassName("e").length===1){return
}Expr.order.splice(1,0,"CLASS");
Expr.find.CLASS=function(match,context,isXML){if(typeof context.getElementsByClassName!=="undefined"&&!isXML){return context.getElementsByClassName(match[1])
}};
div=null
})();
function dirNodeCheck(dir,cur,doneName,checkSet,nodeCheck,isXML){for(var i=0,l=checkSet.length;
i<l;
i++){var elem=checkSet[i];
if(elem){elem=elem[dir];
var match=false;
while(elem){if(elem.sizcache===doneName){match=checkSet[elem.sizset];
break
}if(elem.nodeType===1&&!isXML){elem.sizcache=doneName;
elem.sizset=i
}if(elem.nodeName.toLowerCase()===cur){match=elem;
break
}elem=elem[dir]
}checkSet[i]=match
}}}function dirCheck(dir,cur,doneName,checkSet,nodeCheck,isXML){for(var i=0,l=checkSet.length;
i<l;
i++){var elem=checkSet[i];
if(elem){elem=elem[dir];
var match=false;
while(elem){if(elem.sizcache===doneName){match=checkSet[elem.sizset];
break
}if(elem.nodeType===1){if(!isXML){elem.sizcache=doneName;
elem.sizset=i
}if(typeof cur!=="string"){if(elem===cur){match=true;
break
}}else{if(Sizzle.filter(cur,[elem]).length>0){match=elem;
break
}}}elem=elem[dir]
}checkSet[i]=match
}}}var contains=document.compareDocumentPosition?function(a,b){return a.compareDocumentPosition(b)&16
}:function(a,b){return a!==b&&(a.contains?a.contains(b):true)
};
var isXML=function(elem){var documentElement=(elem?elem.ownerDocument||elem:0).documentElement;
return documentElement?documentElement.nodeName!=="HTML":false
};
var posProcess=function(selector,context){var tmpSet=[],later="",match,root=context.nodeType?[context]:context;
while((match=Expr.match.PSEUDO.exec(selector))){later+=match[0];
selector=selector.replace(Expr.match.PSEUDO,"")
}selector=Expr.relative[selector]?selector+"*":selector;
for(var i=0,l=root.length;
i<l;
i++){Sizzle(selector,root[i],tmpSet)
}return Sizzle.filter(later,tmpSet)
};
return Sizzle
})();
S.flash=(function(){var swfobject=function(){var UNDEF="undefined",OBJECT="object",SHOCKWAVE_FLASH="Shockwave Flash",SHOCKWAVE_FLASH_AX="ShockwaveFlash.ShockwaveFlash",FLASH_MIME_TYPE="application/x-shockwave-flash",EXPRESS_INSTALL_ID="SWFObjectExprInst",win=window,doc=document,nav=navigator,domLoadFnArr=[],regObjArr=[],objIdArr=[],listenersArr=[],script,timer=null,storedAltContent=null,storedAltContentId=null,isDomLoaded=false,isExpressInstallActive=false;
var ua=function(){var w3cdom=typeof doc.getElementById!=UNDEF&&typeof doc.getElementsByTagName!=UNDEF&&typeof doc.createElement!=UNDEF,playerVersion=[0,0,0],d=null;
if(typeof nav.plugins!=UNDEF&&typeof nav.plugins[SHOCKWAVE_FLASH]==OBJECT){d=nav.plugins[SHOCKWAVE_FLASH].description;
if(d&&!(typeof nav.mimeTypes!=UNDEF&&nav.mimeTypes[FLASH_MIME_TYPE]&&!nav.mimeTypes[FLASH_MIME_TYPE].enabledPlugin)){d=d.replace(/^.*\s+(\S+\s+\S+$)/,"$1");
playerVersion[0]=parseInt(d.replace(/^(.*)\..*$/,"$1"),10);
playerVersion[1]=parseInt(d.replace(/^.*\.(.*)\s.*$/,"$1"),10);
playerVersion[2]=/r/.test(d)?parseInt(d.replace(/^.*r(.*)$/,"$1"),10):0
}}else{if(typeof win.ActiveXObject!=UNDEF){var a=null,fp6Crash=false;
try{a=new ActiveXObject(SHOCKWAVE_FLASH_AX+".7")
}catch(e){try{a=new ActiveXObject(SHOCKWAVE_FLASH_AX+".6");
playerVersion=[6,0,21];
a.AllowScriptAccess="always"
}catch(e){if(playerVersion[0]==6){fp6Crash=true
}}if(!fp6Crash){try{a=new ActiveXObject(SHOCKWAVE_FLASH_AX)
}catch(e){}}}if(!fp6Crash&&a){try{d=a.GetVariable("$version");
if(d){d=d.split(" ")[1].split(",");
playerVersion=[parseInt(d[0],10),parseInt(d[1],10),parseInt(d[2],10)]
}}catch(e){}}}}var u=nav.userAgent.toLowerCase(),p=nav.platform.toLowerCase(),webkit=/webkit/.test(u)?parseFloat(u.replace(/^.*webkit\/(\d+(\.\d+)?).*$/,"$1")):false,ie=false,windows=p?/win/.test(p):/win/.test(u),mac=p?/mac/.test(p):/mac/.test(u);
/*@cc_on
			ie = true;
			@if (@_win32)
				windows = true;
			@elif (@_mac)
				mac = true;
			@end
		@*/
return{w3cdom:w3cdom,pv:playerVersion,webkit:webkit,ie:ie,win:windows,mac:mac}
}();
var onDomLoad=function(){if(!ua.w3cdom){return
}addDomLoadEvent(main);
if(ua.ie&&ua.win){try{doc.write("<script id=__ie_ondomload defer=true src=//:><\/script>");
script=getElementById("__ie_ondomload");
if(script){addListener(script,"onreadystatechange",checkReadyState)
}}catch(e){}}if(ua.webkit&&typeof doc.readyState!=UNDEF){timer=setInterval(function(){if(/loaded|complete/.test(doc.readyState)){callDomLoadFunctions()
}},10)
}if(typeof doc.addEventListener!=UNDEF){doc.addEventListener("DOMContentLoaded",callDomLoadFunctions,null)
}addLoadEvent(callDomLoadFunctions)
}();
function checkReadyState(){if(script.readyState=="complete"){script.parentNode.removeChild(script);
callDomLoadFunctions()
}}function callDomLoadFunctions(){if(isDomLoaded){return
}if(ua.ie&&ua.win){var s=createElement("span");
try{var t=doc.getElementsByTagName("body")[0].appendChild(s);
t.parentNode.removeChild(t)
}catch(e){return
}}isDomLoaded=true;
if(timer){clearInterval(timer);
timer=null
}var dl=domLoadFnArr.length;
for(var i=0;
i<dl;
i++){domLoadFnArr[i]()
}}function addDomLoadEvent(fn){if(isDomLoaded){fn()
}else{domLoadFnArr[domLoadFnArr.length]=fn
}}function addLoadEvent(fn){if(typeof win.addEventListener!=UNDEF){win.addEventListener("load",fn,false)
}else{if(typeof doc.addEventListener!=UNDEF){doc.addEventListener("load",fn,false)
}else{if(typeof win.attachEvent!=UNDEF){addListener(win,"onload",fn)
}else{if(typeof win.onload=="function"){var fnOld=win.onload;
win.onload=function(){fnOld();
fn()
}
}else{win.onload=fn
}}}}}function main(){var rl=regObjArr.length;
for(var i=0;
i<rl;
i++){var id=regObjArr[i].id;
if(ua.pv[0]>0){var obj=getElementById(id);
if(obj){regObjArr[i].width=obj.getAttribute("width")?obj.getAttribute("width"):"0";
regObjArr[i].height=obj.getAttribute("height")?obj.getAttribute("height"):"0";
if(hasPlayerVersion(regObjArr[i].swfVersion)){if(ua.webkit&&ua.webkit<312){fixParams(obj)
}setVisibility(id,true)
}else{if(regObjArr[i].expressInstall&&!isExpressInstallActive&&hasPlayerVersion("6.0.65")&&(ua.win||ua.mac)){showExpressInstall(regObjArr[i])
}else{displayAltContent(obj)
}}}}else{setVisibility(id,true)
}}}function fixParams(obj){var nestedObj=obj.getElementsByTagName(OBJECT)[0];
if(nestedObj){var e=createElement("embed"),a=nestedObj.attributes;
if(a){var al=a.length;
for(var i=0;
i<al;
i++){if(a[i].nodeName=="DATA"){e.setAttribute("src",a[i].nodeValue)
}else{e.setAttribute(a[i].nodeName,a[i].nodeValue)
}}}var c=nestedObj.childNodes;
if(c){var cl=c.length;
for(var j=0;
j<cl;
j++){if(c[j].nodeType==1&&c[j].nodeName=="PARAM"){e.setAttribute(c[j].getAttribute("name"),c[j].getAttribute("value"))
}}}obj.parentNode.replaceChild(e,obj)
}}function showExpressInstall(regObj){isExpressInstallActive=true;
var obj=getElementById(regObj.id);
if(obj){if(regObj.altContentId){var ac=getElementById(regObj.altContentId);
if(ac){storedAltContent=ac;
storedAltContentId=regObj.altContentId
}}else{storedAltContent=abstractAltContent(obj)
}if(!(/%$/.test(regObj.width))&&parseInt(regObj.width,10)<310){regObj.width="310"
}if(!(/%$/.test(regObj.height))&&parseInt(regObj.height,10)<137){regObj.height="137"
}doc.title=doc.title.slice(0,47)+" - Flash Player Installation";
var pt=ua.ie&&ua.win?"ActiveX":"PlugIn",dt=doc.title,fv="MMredirectURL="+win.location+"&MMplayerType="+pt+"&MMdoctitle="+dt,replaceId=regObj.id;
if(ua.ie&&ua.win&&obj.readyState!=4){var newObj=createElement("div");
replaceId+="SWFObjectNew";
newObj.setAttribute("id",replaceId);
obj.parentNode.insertBefore(newObj,obj);
obj.style.display="none";
var fn=function(){obj.parentNode.removeChild(obj)
};
addListener(win,"onload",fn)
}createSWF({data:regObj.expressInstall,id:EXPRESS_INSTALL_ID,width:regObj.width,height:regObj.height},{flashvars:fv},replaceId)
}}function displayAltContent(obj){if(ua.ie&&ua.win&&obj.readyState!=4){var el=createElement("div");
obj.parentNode.insertBefore(el,obj);
el.parentNode.replaceChild(abstractAltContent(obj),el);
obj.style.display="none";
var fn=function(){obj.parentNode.removeChild(obj)
};
addListener(win,"onload",fn)
}else{obj.parentNode.replaceChild(abstractAltContent(obj),obj)
}}function abstractAltContent(obj){var ac=createElement("div");
if(ua.win&&ua.ie){ac.innerHTML=obj.innerHTML
}else{var nestedObj=obj.getElementsByTagName(OBJECT)[0];
if(nestedObj){var c=nestedObj.childNodes;
if(c){var cl=c.length;
for(var i=0;
i<cl;
i++){if(!(c[i].nodeType==1&&c[i].nodeName=="PARAM")&&!(c[i].nodeType==8)){ac.appendChild(c[i].cloneNode(true))
}}}}}return ac
}function createSWF(attObj,parObj,id){var r,el=getElementById(id);
if(el){if(typeof attObj.id==UNDEF){attObj.id=id
}if(ua.ie&&ua.win){var att="";
for(var i in attObj){if(attObj[i]!=Object.prototype[i]){if(i.toLowerCase()=="data"){parObj.movie=attObj[i]
}else{if(i.toLowerCase()=="styleclass"){att+=' class="'+attObj[i]+'"'
}else{if(i.toLowerCase()!="classid"){att+=" "+i+'="'+attObj[i]+'"'
}}}}}var par="";
for(var j in parObj){if(parObj[j]!=Object.prototype[j]){par+='<param name="'+j+'" value="'+parObj[j]+'" />'
}}el.outerHTML='<object classid="clsid:D27CDB6E-AE6D-11cf-96B8-444553540000"'+att+">"+par+"</object>";
objIdArr[objIdArr.length]=attObj.id;
r=getElementById(attObj.id)
}else{if(ua.webkit&&ua.webkit<312){var e=createElement("embed");
e.setAttribute("type",FLASH_MIME_TYPE);
for(var k in attObj){if(attObj[k]!=Object.prototype[k]){if(k.toLowerCase()=="data"){e.setAttribute("src",attObj[k])
}else{if(k.toLowerCase()=="styleclass"){e.setAttribute("class",attObj[k])
}else{if(k.toLowerCase()!="classid"){e.setAttribute(k,attObj[k])
}}}}}for(var l in parObj){if(parObj[l]!=Object.prototype[l]){if(l.toLowerCase()!="movie"){e.setAttribute(l,parObj[l])
}}}el.parentNode.replaceChild(e,el);
r=e
}else{var o=createElement(OBJECT);
o.setAttribute("type",FLASH_MIME_TYPE);
for(var m in attObj){if(attObj[m]!=Object.prototype[m]){if(m.toLowerCase()=="styleclass"){o.setAttribute("class",attObj[m])
}else{if(m.toLowerCase()!="classid"){o.setAttribute(m,attObj[m])
}}}}for(var n in parObj){if(parObj[n]!=Object.prototype[n]&&n.toLowerCase()!="movie"){createObjParam(o,n,parObj[n])
}}el.parentNode.replaceChild(o,el);
r=o
}}}return r
}function createObjParam(el,pName,pValue){var p=createElement("param");
p.setAttribute("name",pName);
p.setAttribute("value",pValue);
el.appendChild(p)
}function removeSWF(id){var obj=getElementById(id);
if(obj&&(obj.nodeName=="OBJECT"||obj.nodeName=="EMBED")){if(ua.ie&&ua.win){if(obj.readyState==4){removeObjectInIE(id)
}else{win.attachEvent("onload",function(){removeObjectInIE(id)
})
}}else{obj.parentNode.removeChild(obj)
}}}function removeObjectInIE(id){var obj=getElementById(id);
if(obj){for(var i in obj){if(typeof obj[i]=="function"){obj[i]=null
}}obj.parentNode.removeChild(obj)
}}function getElementById(id){var el=null;
try{el=doc.getElementById(id)
}catch(e){}return el
}function createElement(el){return doc.createElement(el)
}function addListener(target,eventType,fn){target.attachEvent(eventType,fn);
listenersArr[listenersArr.length]=[target,eventType,fn]
}function hasPlayerVersion(rv){var pv=ua.pv,v=rv.split(".");
v[0]=parseInt(v[0],10);
v[1]=parseInt(v[1],10)||0;
v[2]=parseInt(v[2],10)||0;
return(pv[0]>v[0]||(pv[0]==v[0]&&pv[1]>v[1])||(pv[0]==v[0]&&pv[1]==v[1]&&pv[2]>=v[2]))?true:false
}function createCSS(sel,decl){if(ua.ie&&ua.mac){return
}var h=doc.getElementsByTagName("head")[0],s=createElement("style");
s.setAttribute("type","text/css");
s.setAttribute("media","screen");
if(!(ua.ie&&ua.win)&&typeof doc.createTextNode!=UNDEF){s.appendChild(doc.createTextNode(sel+" {"+decl+"}"))
}h.appendChild(s);
if(ua.ie&&ua.win&&typeof doc.styleSheets!=UNDEF&&doc.styleSheets.length>0){var ls=doc.styleSheets[doc.styleSheets.length-1];
if(typeof ls.addRule==OBJECT){ls.addRule(sel,decl)
}}}function setVisibility(id,isVisible){var v=isVisible?"visible":"hidden";
if(isDomLoaded&&getElementById(id)){getElementById(id).style.visibility=v
}else{createCSS("#"+id,"visibility:"+v)
}}function urlEncodeIfNecessary(s){var regex=/[\\\"<>\.;]/;
var hasBadChars=regex.exec(s)!=null;
return hasBadChars?encodeURIComponent(s):s
}var cleanup=function(){if(ua.ie&&ua.win){window.attachEvent("onunload",function(){var ll=listenersArr.length;
for(var i=0;
i<ll;
i++){listenersArr[i][0].detachEvent(listenersArr[i][1],listenersArr[i][2])
}var il=objIdArr.length;
for(var j=0;
j<il;
j++){removeSWF(objIdArr[j])
}for(var k in ua){ua[k]=null
}ua=null;
for(var l in swfobject){swfobject[l]=null
}swfobject=null
})
}}();
return{registerObject:function(objectIdStr,swfVersionStr,xiSwfUrlStr){if(!ua.w3cdom||!objectIdStr||!swfVersionStr){return
}var regObj={};
regObj.id=objectIdStr;
regObj.swfVersion=swfVersionStr;
regObj.expressInstall=xiSwfUrlStr?xiSwfUrlStr:false;
regObjArr[regObjArr.length]=regObj;
setVisibility(objectIdStr,false)
},getObjectById:function(objectIdStr){var r=null;
if(ua.w3cdom){var o=getElementById(objectIdStr);
if(o){var n=o.getElementsByTagName(OBJECT)[0];
if(!n||(n&&typeof o.SetVariable!=UNDEF)){r=o
}else{if(typeof n.SetVariable!=UNDEF){r=n
}}}}return r
},embedSWF:function(swfUrlStr,replaceElemIdStr,widthStr,heightStr,swfVersionStr,xiSwfUrlStr,flashvarsObj,parObj,attObj){if(!ua.w3cdom||!swfUrlStr||!replaceElemIdStr||!widthStr||!heightStr||!swfVersionStr){return
}widthStr+="";
heightStr+="";
if(hasPlayerVersion(swfVersionStr)){setVisibility(replaceElemIdStr,false);
var att={};
if(attObj&&typeof attObj===OBJECT){for(var i in attObj){if(attObj[i]!=Object.prototype[i]){att[i]=attObj[i]
}}}att.data=swfUrlStr;
att.width=widthStr;
att.height=heightStr;
var par={};
if(parObj&&typeof parObj===OBJECT){for(var j in parObj){if(parObj[j]!=Object.prototype[j]){par[j]=parObj[j]
}}}if(flashvarsObj&&typeof flashvarsObj===OBJECT){for(var k in flashvarsObj){if(flashvarsObj[k]!=Object.prototype[k]){if(typeof par.flashvars!=UNDEF){par.flashvars+="&"+k+"="+flashvarsObj[k]
}else{par.flashvars=k+"="+flashvarsObj[k]
}}}}addDomLoadEvent(function(){createSWF(att,par,replaceElemIdStr);
if(att.id==replaceElemIdStr){setVisibility(replaceElemIdStr,true)
}})
}else{if(xiSwfUrlStr&&!isExpressInstallActive&&hasPlayerVersion("6.0.65")&&(ua.win||ua.mac)){isExpressInstallActive=true;
setVisibility(replaceElemIdStr,false);
addDomLoadEvent(function(){var regObj={};
regObj.id=regObj.altContentId=replaceElemIdStr;
regObj.width=widthStr;
regObj.height=heightStr;
regObj.expressInstall=xiSwfUrlStr;
showExpressInstall(regObj)
})
}}},getFlashPlayerVersion:function(){return{major:ua.pv[0],minor:ua.pv[1],release:ua.pv[2]}
},hasFlashPlayerVersion:hasPlayerVersion,createSWF:function(attObj,parObj,replaceElemIdStr){if(ua.w3cdom){return createSWF(attObj,parObj,replaceElemIdStr)
}else{return undefined
}},removeSWF:function(objElemIdStr){if(ua.w3cdom){removeSWF(objElemIdStr)
}},createCSS:function(sel,decl){if(ua.w3cdom){createCSS(sel,decl)
}},addDomLoadEvent:addDomLoadEvent,addLoadEvent:addLoadEvent,getQueryParamValue:function(param){var q=doc.location.search||doc.location.hash;
if(param==null){return urlEncodeIfNecessary(q)
}if(q){var pairs=q.substring(1).split("&");
for(var i=0;
i<pairs.length;
i++){if(pairs[i].substring(0,pairs[i].indexOf("="))==param){return urlEncodeIfNecessary(pairs[i].substring((pairs[i].indexOf("=")+1)))
}}}return""
},expressInstallCallback:function(){if(isExpressInstallActive&&storedAltContent){var obj=getElementById(EXPRESS_INSTALL_ID);
if(obj){obj.parentNode.replaceChild(storedAltContent,obj);
if(storedAltContentId){setVisibility(storedAltContentId,true);
if(ua.ie&&ua.win){storedAltContent.style.display="block"
}}storedAltContent=null;
storedAltContentId=null;
isExpressInstallActive=false
}}}}
}();
return swfobject
})();
S.lang={code:"en",of:"of",loading:"loading",cancel:"Cancel",next:"Next",previous:"Previous",play:"Play",pause:"Pause",close:"Close",errors:{single:'You must install the <a href="{0}">{1}</a> browser plugin to view this content.',shared:'You must install both the <a href="{0}">{1}</a> and <a href="{2}">{3}</a> browser plugins to view this content.',either:'You must install either the <a href="{0}">{1}</a> or the <a href="{2}">{3}</a> browser plugin to view this content.'}};
var pre,proxyId="sb-drag-proxy",dragData,dragProxy,dragTarget;
function resetDrag(){dragData={x:0,y:0,startX:null,startY:null}
}function updateProxy(){var dims=S.dimensions;
apply(dragProxy.style,{height:dims.innerHeight+"px",width:dims.innerWidth+"px"})
}function enableDrag(){resetDrag();
var style=["position:absolute","cursor:"+(S.isGecko?"-moz-grab":"move"),"background-color:"+(S.isIE?"#fff;filter:alpha(opacity=0)":"transparent")].join(";");
S.appendHTML(S.skin.body,'<div id="'+proxyId+'" style="'+style+'"></div>');
dragProxy=get(proxyId);
updateProxy();
addEvent(dragProxy,"mousedown",startDrag)
}function disableDrag(){if(dragProxy){removeEvent(dragProxy,"mousedown",startDrag);
remove(dragProxy);
dragProxy=null
}dragTarget=null
}function startDrag(e){preventDefault(e);
var xy=getPageXY(e);
dragData.startX=xy[0];
dragData.startY=xy[1];
dragTarget=get(S.player.id);
addEvent(document,"mousemove",positionDrag);
addEvent(document,"mouseup",endDrag);
if(S.isGecko){dragProxy.style.cursor="-moz-grabbing"
}}function positionDrag(e){var player=S.player,dims=S.dimensions,xy=getPageXY(e);
var moveX=xy[0]-dragData.startX;
dragData.startX+=moveX;
dragData.x=Math.max(Math.min(0,dragData.x+moveX),dims.innerWidth-player.width);
var moveY=xy[1]-dragData.startY;
dragData.startY+=moveY;
dragData.y=Math.max(Math.min(0,dragData.y+moveY),dims.innerHeight-player.height);
apply(dragTarget.style,{left:dragData.x+"px",top:dragData.y+"px"})
}function endDrag(){removeEvent(document,"mousemove",positionDrag);
removeEvent(document,"mouseup",endDrag);
if(S.isGecko){dragProxy.style.cursor="-moz-grab"
}}S.img=function(obj,id){this.obj=obj;
this.id=id;
this.ready=false;
var self=this;
pre=new Image();
pre.onload=function(){self.height=obj.height?parseInt(obj.height,10):pre.height;
self.width=obj.width?parseInt(obj.width,10):pre.width;
self.ready=true;
pre.onload=null;
pre=null
};
pre.src=obj.content
};
S.img.ext=["bmp","gif","jpg","jpeg","png"];
S.img.prototype={append:function(body,dims){var img=document.createElement("img");
img.id=this.id;
img.src=this.obj.content;
img.style.position="absolute";
var height,width;
if(dims.oversized&&S.options.handleOversize=="resize"){height=dims.innerHeight;
width=dims.innerWidth
}else{height=this.height;
width=this.width
}img.setAttribute("height",height);
img.setAttribute("width",width);
body.appendChild(img)
},remove:function(){var el=get(this.id);
if(el){remove(el)
}disableDrag();
if(pre){pre.onload=null;
pre=null
}},onLoad:function(){var dims=S.dimensions;
if(dims.oversized&&S.options.handleOversize=="drag"){enableDrag()
}},onWindowResize:function(){var dims=S.dimensions;
switch(S.options.handleOversize){case"resize":var el=get(this.id);
el.height=dims.innerHeight;
el.width=dims.innerWidth;
break;
case"drag":if(dragTarget){var top=parseInt(S.getStyle(dragTarget,"top")),left=parseInt(S.getStyle(dragTarget,"left"));
if(top+this.height<dims.innerHeight){dragTarget.style.top=dims.innerHeight-this.height+"px"
}if(left+this.width<dims.innerWidth){dragTarget.style.left=dims.innerWidth-this.width+"px"
}updateProxy()
}break
}}};
S.iframe=function(obj,id){this.obj=obj;
this.id=id;
var overlay=get("sb-overlay");
this.height=obj.height?parseInt(obj.height,10):overlay.offsetHeight;
this.width=obj.width?parseInt(obj.width,10):overlay.offsetWidth
};
S.iframe.prototype={append:function(body,dims){var html='<iframe id="'+this.id+'" name="'+this.id+'" height="100%" width="100%" frameborder="0" marginwidth="0" marginheight="0" style="visibility:hidden" onload="this.style.visibility=\'visible\'" scrolling="auto"';
if(S.isIE){html+=' allowtransparency="true"';
if(S.isIE6){html+=" src=\"javascript:false;document.write('');\""
}}html+="></iframe>";
body.innerHTML=html
},remove:function(){var el=get(this.id);
if(el){remove(el);
if(S.isGecko){delete window.frames[this.id]
}}},onLoad:function(){var win=S.isIE?get(this.id).contentWindow:window.frames[this.id];
win.location.href=this.obj.content
}};
S.html=function(obj,id){this.obj=obj;
this.id=id;
this.height=obj.height?parseInt(obj.height,10):300;
this.width=obj.width?parseInt(obj.width,10):500
};
S.html.prototype={append:function(body,dims){var div=document.createElement("div");
div.id=this.id;
div.className="html";
div.innerHTML=this.obj.content;
body.appendChild(div)
},remove:function(){var el=get(this.id);
if(el){remove(el)
}}};
S.swf=function(obj,id){this.obj=obj;
this.id=id;
this.height=obj.height?parseInt(obj.height,10):300;
this.width=obj.width?parseInt(obj.width,10):300
};
S.swf.ext=["swf"];
S.swf.prototype={append:function(body,dims){var tmp=document.createElement("div");
tmp.id=this.id;
body.appendChild(tmp);
var height=dims.innerHeight,width=dims.innerWidth,swf=this.obj.content,version=S.options.flashVersion,express=S.path+"expressInstall.swf",flashvars=S.options.flashVars,params=S.options.flashParams;
S.flash.embedSWF(swf,this.id,width,height,version,express,flashvars,params)
},remove:function(){S.flash.expressInstallCallback();
S.flash.removeSWF(this.id)
},onWindowResize:function(){var dims=S.dimensions,el=get(this.id);
el.height=dims.innerHeight;
el.width=dims.innerWidth
}};
var jwControllerHeight=20;
S.flv=function(obj,id){this.obj=obj;
this.id=id;
this.height=obj.height?parseInt(obj.height,10):300;
if(S.options.showMovieControls){this.height+=jwControllerHeight
}this.width=obj.width?parseInt(obj.width,10):300
};
S.flv.ext=["flv","m4v"];
S.flv.prototype={append:function(body,dims){var tmp=document.createElement("div");
tmp.id=this.id;
body.appendChild(tmp);
var height=dims.innerHeight,width=dims.innerWidth,swf=S.path+"player.swf",version=S.options.flashVersion,express=S.path+"expressInstall.swf",flashvars=apply({file:this.obj.content,height:height,width:width,autostart:(S.options.autoplayMovies?"true":"false"),controlbar:(S.options.showMovieControls?"bottom":"none"),backcolor:"0x000000",frontcolor:"0xCCCCCC",lightcolor:"0x557722"},S.options.flashVars),params=S.options.flashParams;
S.flash.embedSWF(swf,this.id,width,height,version,express,flashvars,params)
},remove:function(){S.flash.expressInstallCallback();
S.flash.removeSWF(this.id)
},onWindowResize:function(){var dims=S.dimensions,el=get(this.id);
el.height=dims.innerHeight;
el.width=dims.innerWidth
}};
var qtControllerHeight=16;
S.qt=function(obj,id){this.obj=obj;
this.id=id;
this.height=obj.height?parseInt(obj.height,10):300;
if(S.options.showMovieControls){this.height+=qtControllerHeight
}this.width=obj.width?parseInt(obj.width,10):300
};
S.qt.ext=["dv","mov","moov","movie","mp4","avi","mpg","mpeg"];
S.qt.prototype={append:function(body,dims){var opt=S.options,autoplay=String(opt.autoplayMovies),controls=String(opt.showMovieControls);
var html="<object",movie={id:this.id,name:this.id,height:this.height,width:this.width,kioskmode:"true"};
if(S.isIE){movie.classid="clsid:02BF25D5-8C17-4B23-BC80-D3488ABDDC6B";
movie.codebase="http://www.apple.com/qtactivex/qtplugin.cab#version=6,0,2,0"
}else{movie.type="video/quicktime";
movie.data=this.obj.content
}for(var m in movie){html+=" "+m+'="'+movie[m]+'"'
}html+=">";
var params={src:this.obj.content,scale:"aspect",controller:controls,autoplay:autoplay};
for(var p in params){html+='<param name="'+p+'" value="'+params[p]+'">'
}html+="</object>";
body.innerHTML=html
},remove:function(){try{document[this.id].Stop()
}catch(e){}var el=get(this.id);
if(el){remove(el)
}}};
var wmpControllerHeight=(S.isIE?70:45);
S.wmp=function(obj,id){this.obj=obj;
this.id=id;
this.height=obj.height?parseInt(obj.height,10):300;
if(S.options.showMovieControls){this.height+=wmpControllerHeight
}this.width=obj.width?parseInt(obj.width,10):300
};
S.wmp.ext=["asf","avi","mpg","mpeg","wm","wmv"];
S.wmp.prototype={append:function(body,dims){var opt=S.options,autoplay=opt.autoplayMovies?1:0;
var movie='<object id="'+this.id+'" name="'+this.id+'" height="'+this.height+'" width="'+this.width+'"',params={autostart:opt.autoplayMovies?1:0};
if(S.isIE){movie+=' classid="clsid:6BF52A52-394A-11d3-B153-00C04F79FAA6"';
params.url=this.obj.content;
params.uimode=opt.showMovieControls?"full":"none"
}else{movie+=' type="video/x-ms-wmv"';
movie+=' data="'+this.obj.content+'"';
params.showcontrols=opt.showMovieControls?1:0
}movie+=">";
for(var p in params){movie+='<param name="'+p+'" value="'+params[p]+'">'
}movie+="</object>";
body.innerHTML=movie
},remove:function(){if(S.isIE){try{window[this.id].controls.stop();
window[this.id].URL="movie"+now()+".wmv";
window[this.id]=function(){}
}catch(e){}}var el=get(this.id);
if(el){setTimeout(function(){remove(el)
},10)
}}};
var overlayOn=false,visibilityCache=[],pngIds=["sb-nav-close","sb-nav-next","sb-nav-play","sb-nav-pause","sb-nav-previous"],container,overlay,wrapper,doWindowResize=true;
function animate(el,property,to,duration,callback){var isOpacity=(property=="opacity"),anim=isOpacity?S.setOpacity:function(el,value){el.style[property]=""+value+"px"
};
if(duration==0||(!isOpacity&&!S.options.animate)||(isOpacity&&!S.options.animateFade)){anim(el,to);
if(callback){callback()
}return
}var from=parseFloat(S.getStyle(el,property))||0;
var delta=to-from;
if(delta==0){if(callback){callback()
}return
}duration*=1000;
var begin=now(),ease=S.ease,end=begin+duration,time;
var interval=setInterval(function(){time=now();
if(time>=end){clearInterval(interval);
interval=null;
anim(el,to);
if(callback){callback()
}}else{anim(el,from+ease((time-begin)/duration)*delta)
}},10)
}function setSize(){container.style.height=S.getWindowSize("Height")+"px";
container.style.width=S.getWindowSize("Width")+"px"
}function setPosition(){container.style.top=document.documentElement.scrollTop+"px";
container.style.left=document.documentElement.scrollLeft+"px"
}function toggleTroubleElements(on){if(on){each(visibilityCache,function(i,el){el[0].style.visibility=el[1]||""
})
}else{visibilityCache=[];
each(S.options.troubleElements,function(i,tag){each(document.getElementsByTagName(tag),function(j,el){visibilityCache.push([el,el.style.visibility]);
el.style.visibility="hidden"
})
})
}}function toggleNav(id,on){var el=get("sb-nav-"+id);
if(el){el.style.display=on?"":"none"
}}function toggleLoading(on,callback){var loading=get("sb-loading"),playerName=S.getCurrent().player,anim=(playerName=="img"||playerName=="html");
if(on){S.setOpacity(loading,0);
loading.style.display="block";
var wrapped=function(){S.clearOpacity(loading);
if(callback){callback()
}};
if(anim){animate(loading,"opacity",1,S.options.fadeDuration,wrapped)
}else{wrapped()
}}else{var wrapped=function(){loading.style.display="none";
S.clearOpacity(loading);
if(callback){callback()
}};
if(anim){animate(loading,"opacity",0,S.options.fadeDuration,wrapped)
}else{wrapped()
}}}function buildBars(callback){var obj=S.getCurrent();
get("sb-title-inner").innerHTML=obj.title||"";
var close,next,play,pause,previous;
if(S.options.displayNav){close=true;
var len=S.gallery.length;
if(len>1){if(S.options.continuous){next=previous=true
}else{next=(len-1)>S.current;
previous=S.current>0
}}if(S.options.slideshowDelay>0&&S.hasNext()){pause=!S.isPaused();
play=!pause
}}else{close=next=play=pause=previous=false
}toggleNav("close",close);
toggleNav("next",next);
toggleNav("play",play);
toggleNav("pause",pause);
toggleNav("previous",previous);
var counter="";
if(S.options.displayCounter&&S.gallery.length>1){var len=S.gallery.length;
if(S.options.counterType=="skip"){var i=0,end=len,limit=parseInt(S.options.counterLimit)||0;
if(limit<len&&limit>2){var h=Math.floor(limit/2);
i=S.current-h;
if(i<0){i+=len
}end=S.current+(limit-h);
if(end>len){end-=len
}}while(i!=end){if(i==len){i=0
}counter+='<a onclick="Shadowbox.change('+i+');"';
if(i==S.current){counter+=' class="sb-counter-current"'
}counter+=">"+(++i)+"</a>"
}}else{counter=[S.current+1,S.lang.of,len].join(" ")
}}get("sb-counter").innerHTML=counter;
callback()
}function showBars(callback){var titleInner=get("sb-title-inner"),infoInner=get("sb-info-inner"),duration=0.35;
titleInner.style.visibility=infoInner.style.visibility="";
if(titleInner.innerHTML!=""){animate(titleInner,"marginTop",0,duration)
}animate(infoInner,"marginTop",0,duration,callback)
}function hideBars(anim,callback){var title=get("sb-title"),info=get("sb-info"),titleHeight=title.offsetHeight,infoHeight=info.offsetHeight,titleInner=get("sb-title-inner"),infoInner=get("sb-info-inner"),duration=(anim?0.35:0);
animate(titleInner,"marginTop",titleHeight,duration);
animate(infoInner,"marginTop",infoHeight*-1,duration,function(){titleInner.style.visibility=infoInner.style.visibility="hidden";
callback()
})
}function adjustHeight(height,top,anim,callback){var wrapperInner=get("sb-wrapper-inner"),duration=(anim?S.options.resizeDuration:0);
animate(wrapper,"top",top,duration);
animate(wrapperInner,"height",height,duration,callback)
}function adjustWidth(width,left,anim,callback){var duration=(anim?S.options.resizeDuration:0);
animate(wrapper,"left",left,duration);
animate(wrapper,"width",width,duration,callback)
}function setDimensions(height,width){var bodyInner=get("sb-body-inner"),height=parseInt(height),width=parseInt(width),topBottom=wrapper.offsetHeight-bodyInner.offsetHeight,leftRight=wrapper.offsetWidth-bodyInner.offsetWidth,maxHeight=overlay.offsetHeight,maxWidth=overlay.offsetWidth,padding=parseInt(S.options.viewportPadding)||20,preserveAspect=(S.player&&S.options.handleOversize!="drag");
return S.setDimensions(height,width,maxHeight,maxWidth,topBottom,leftRight,padding,preserveAspect)
}var K={};
K.markup='<div id="sb-container"><div id="sb-overlay"></div><div id="sb-wrapper"><div id="sb-title"><div id="sb-title-inner"></div></div><div id="sb-wrapper-inner"><div id="sb-body"><div id="sb-body-inner"></div><div id="sb-loading"><div id="sb-loading-inner"><span>{loading}</span></div></div></div></div><div id="sb-info"><div id="sb-info-inner"><div id="sb-counter"></div><div id="sb-nav"><a id="sb-nav-close" title="{close}" onclick="Shadowbox.close()"></a><a id="sb-nav-next" title="{next}" onclick="Shadowbox.next()"></a><a id="sb-nav-play" title="{play}" onclick="Shadowbox.play()"></a><a id="sb-nav-pause" title="{pause}" onclick="Shadowbox.pause()"></a><a id="sb-nav-previous" title="{previous}" onclick="Shadowbox.previous()"></a></div></div></div></div></div>';
K.options={animSequence:"sync",counterLimit:10,counterType:"default",displayCounter:true,displayNav:true,fadeDuration:0.35,initialHeight:160,initialWidth:320,modal:false,overlayColor:"#000",overlayOpacity:0.5,resizeDuration:0.35,showOverlay:true,troubleElements:["select","object","embed","canvas"]};
K.init=function(){S.appendHTML(document.body,sprintf(K.markup,S.lang));
K.body=get("sb-body-inner");
container=get("sb-container");
overlay=get("sb-overlay");
wrapper=get("sb-wrapper");
if(!supportsFixed){container.style.position="absolute"
}if(!supportsOpacity){var el,m,re=/url\("(.*\.png)"\)/;
each(pngIds,function(i,id){el=get(id);
if(el){m=S.getStyle(el,"backgroundImage").match(re);
if(m){el.style.backgroundImage="none";
el.style.filter="progid:DXImageTransform.Microsoft.AlphaImageLoader(enabled=true,src="+m[1]+",sizingMethod=scale);"
}}})
}var timer;
addEvent(window,"resize",function(){if(timer){clearTimeout(timer);
timer=null
}if(open){timer=setTimeout(K.onWindowResize,10)
}})
};
K.onOpen=function(obj,callback){doWindowResize=false;
container.style.display="block";
setSize();
var dims=setDimensions(S.options.initialHeight,S.options.initialWidth);
adjustHeight(dims.innerHeight,dims.top);
adjustWidth(dims.width,dims.left);
if(S.options.showOverlay){overlay.style.backgroundColor=S.options.overlayColor;
S.setOpacity(overlay,0);
if(!S.options.modal){addEvent(overlay,"click",S.close)
}overlayOn=true
}if(!supportsFixed){setPosition();
addEvent(window,"scroll",setPosition)
}toggleTroubleElements();
container.style.visibility="visible";
if(overlayOn){animate(overlay,"opacity",S.options.overlayOpacity,S.options.fadeDuration,callback)
}else{callback()
}};
K.onLoad=function(changing,callback){toggleLoading(true);
while(K.body.firstChild){remove(K.body.firstChild)
}hideBars(changing,function(){if(!open){return
}if(!changing){wrapper.style.visibility="visible"
}buildBars(callback)
})
};
K.onReady=function(callback){if(!open){return
}var player=S.player,dims=setDimensions(player.height,player.width);
var wrapped=function(){showBars(callback)
};
switch(S.options.animSequence){case"hw":adjustHeight(dims.innerHeight,dims.top,true,function(){adjustWidth(dims.width,dims.left,true,wrapped)
});
break;
case"wh":adjustWidth(dims.width,dims.left,true,function(){adjustHeight(dims.innerHeight,dims.top,true,wrapped)
});
break;
default:adjustWidth(dims.width,dims.left,true);
adjustHeight(dims.innerHeight,dims.top,true,wrapped)
}};
K.onShow=function(callback){toggleLoading(false,callback);
doWindowResize=true
};
K.onClose=function(){if(!supportsFixed){removeEvent(window,"scroll",setPosition)
}removeEvent(overlay,"click",S.close);
wrapper.style.visibility="hidden";
var callback=function(){container.style.visibility="hidden";
container.style.display="none";
toggleTroubleElements(true)
};
if(overlayOn){animate(overlay,"opacity",0,S.options.fadeDuration,callback)
}else{callback()
}};
K.onPlay=function(){toggleNav("play",false);
toggleNav("pause",true)
};
K.onPause=function(){toggleNav("pause",false);
toggleNav("play",true)
};
K.onWindowResize=function(){if(!doWindowResize){return
}setSize();
var player=S.player,dims=setDimensions(player.height,player.width);
adjustWidth(dims.width,dims.left);
adjustHeight(dims.innerHeight,dims.top);
if(player.onWindowResize){player.onWindowResize()
}};
S.skin=K;
window.Shadowbox=S
})(window);
if(window.$===window.jQuery){try{delete window.$
}catch(e){window["$"]=undefined
}}(function(a){window.is_ie=!!(window.attachEvent&&!window.opera);
if(!Array.prototype.push){Array.prototype.push=function(b){this[this.length]=b
}
}if(!String.prototype.startsWith){String.prototype.startsWith=function(b){return this.lastIndexOf(b,0)===0
}
}if(!String.prototype.endsWith){String.prototype.endsWith=function(b){var c=this.length-b.length;
return c>=0&&this.indexOf(b,c)===c
}
}window.redirectToGo=function(b,c){var f="http://go.warwick.ac.uk/"+b;
f=f+"?goSearchReferer="+encodeURIComponent(window.location);
if(c){f=f+"&goSearchQuery="+c
}window.location=f
};
window.WRollback=function(c,b,f){this.element=c;
this.over=b;
this.out=f;
this.element.onmouseover=b;
this.element.onmouseout=f
};
WRollback.prototype.disable=function(){this.element.onmouseover=null;
this.element.onmouseout=null
};
WRollback.prototype.enable=function(){this.element.onmouseover=this.over;
this.element.onmouseout=this.out
};
window.WTogglePopup=function(b,g,c,f){this.$button=a("#"+b);
this.$div=a("#"+g);
this.duration=0.2;
this.closeOnDocumentClick=c||false;
this.callback=f||function(){};
this.$button.click(a.proxy(this.toggle,this))
};
WTogglePopup.prototype.toggle=function(c){if(c){c.preventDefault()
}var b;
if(this.closeOnDocumentClick){b=a.proxy(function(){if(this.$div.is(":visible")){a(document).click(a.proxy(this.hideIfVisible,this));
this.callback(true)
}else{a(document).unbind("click",a.proxy(this.hideIfVisible,this));
this.callback(false)
}},this)
}else{b=a.proxy(function(){if(this.$div.is(":visible")){this.callback(true)
}else{this.callback(false)
}},this)
}this.$div.toggle(this.duration,b);
return false
};
WTogglePopup.prototype.hideIfVisible=function(b){if(this.$div.is(":visible")){this.toggle(b)
}return true
};
window.WCookie=function(c,f,b,g){this.name=c;
this.expires="";
this.path="/";
this.value="";
if(f){this.value=f;
if(b){this.hours=b;
this.expires="; expires="+WCookie._getGMTStringForHoursAhead(b)
}else{this.expires=""
}if(g){this.path=g
}else{this.path="/"
}this.save()
}else{this.value=this.load()
}};
WCookie._getGMTStringForHoursAhead=function(b){var c=new Date();
c.setTime(c.getTime()+(b*60*60*1000));
return c.toGMTString()
};
WCookie.prototype.load=function(){var g=this.name+"=";
var b=document.cookie.split(";");
for(var f=0;
f<b.length;
f++){var h=b[f];
while(h.charAt(0)==" "){h=h.substring(1,h.length)
}if(h.indexOf(g)==0){return h.substring(g.length,h.length)
}}return null
};
WCookie.prototype.save=function(){document.cookie=this.name+"="+this.value+this.expires+"; path="+this.path
};
WCookie.prototype.erase=function(){this.value="";
this.hours=-1;
this.expires="; expires="+WCookie._getGMTStringForHoursAhead(this.hours);
this.save()
};
if(typeof(StringBuilder)=="undefined"){window.StringBuilder=function(b){this.strings=new Array("");
this.append(b)
};
StringBuilder.prototype.append=function(b){if(b){this.strings.push(b)
}};
StringBuilder.prototype.clear=function(){this.strings.length=1
};
StringBuilder.prototype.toString=function(){return this.strings.join("")
}
}String.prototype.postEncode=function(){var b=new StringBuilder();
var g=this.length;
for(var f=0;
f<g;
f++){var h=this.charCodeAt(f).toString();
if(h>127){b.append("%26%23");
b.append(h);
b.append("%3B")
}else{b.append(encodeURIComponent(this.substr(f,1)))
}}return b.toString()
};
String.prototype.characterEscape=function(){var b=new StringBuilder();
var g=this.length;
for(var f=0;
f<g;
f++){var h=this.charCodeAt(f).toString();
if(h>127){b.append("&#");
b.append(h);
b.append(";")
}else{b.append(this.substr(f,1))
}}return b.toString()
};
String.prototype.trim=function(){return this.replace(/^\s\s*/,"").replace(/\s\s*$/,"")
};
window.WForm={postEncode:function(b){return a(b).serializeArray().map(function(c,f){return f==null?null:jQuery.isArray(f)?jQuery.map(f,function(h,g){return{name:h.name,value:WForm.Element.postEncode(h.value)}
}):{name:f.name,value:WForm.Element.postEncode(f.value)}
}).join("&")
},Element:{postEncode:function(b){var c=a(b).val();
return c==null?null:jQuery.isArray(c)?jQuery.map(c,function(g,f){return{name:b.name,value:WForm.Element.postEncode(g)}
}):{name:b.name,value:WForm.Element.postEncode(c)}
}}};
window.addEvent=function(f,c,b){a(f).bind(c,b)
};
window.cancelDefaultEvents=function(b){if(b.preventDefault){b.preventDefault()
}b.returnValue=false
};
window.sbrToAbsoluteUrl=function(c){var f=c;
if(f.indexOf(":")<0){var g=""+window.location.href;
var b=document.getElementsByTagName("base");
if(b.length>0){g=b[0].href
}if(f.startsWith("//")){f=g.substring(0,g.indexOf(":")+1)+f
}else{if(f.indexOf("/")===0){f=f.substring(1);
g=g.substring(0,g.indexOf("/",7))
}if(g.charAt(g.length-1)!="/"){g+="/"
}f=g+f
}}return f
};
if(!window.Url){window.Url={}
}window.Url.unparam=function(h,k){if(typeof(h)=="undefined"||h==null){return{}
}var b=h.trim().match(/([^?#]*)(#.*)?$/);
if(!b){return{}
}var n={};
var g=b[1].split(k||"&");
for(var f=0;
f<g.length;
f++){var l=g[f].split("=",2);
var c=decodeURIComponent(l[0]);
var h=l.length==2?decodeURIComponent(l[1]):null;
n[c]=h
}return n
};
if(typeof window.Event=="undefined"){window.Event={}
}Event.onDOMReady=a.proxy(a(document).ready,a(document))
})(jQuery);
jQuery.fn.reverse=[].reverse;
Shadowbox.initialized=false;
var closeBoxSrc=(function(){var a=document.getElementsByTagName("script");
for(var c=a.length-1;
c>=0;
c--){var f=a[c].getAttribute("defer");
var b=(f!=undefined&&f.length>0);
if(!b){return a[c].src.replace(/[^/]+\/[^/]+$/,"")+"images/mediaplayers/closebox.png"
}}return""
}());
var shadowboxOptions={overlayOpacity:0.7,overlayColor:"#fff",viewportPadding:40,counterType:"skip",onOpen:function(){(function(a){if(a("#sb-wrapper > :first-child").attr("id")=="sb-title"){a("#sb-info-inner").append(a("#sb-counter"));
a("#sb-info").after(a("#sb-title"));
a("#sb-nav-close").remove();
a("#sb-nav a").reverse().each(function(){$a=a(this);
$a.parent().append($a)
});
a("#sb-nav-next").html("&raquo;");
a("#sb-nav-previous").html("&laquo;");
if(a("#sb-wrapper .close-button").length==0){a("#sb-wrapper").prepend(a('<img class="close-button" src="'+closeBoxSrc+'" />').click(function(){Shadowbox.close()
}))
}}})(jQuery)
}};
var initShadowbox=function(){if(jQuery("body").data("initShadowbox")||jQuery("#main-content a[rel^=lightbox]").length>0){jQuery("body").removeData("initShadowbox");
if(Shadowbox.initialized){Shadowbox.clearCache();
Shadowbox.setup(false,shadowboxOptions)
}else{Shadowbox.initialized=true;
Shadowbox.init(shadowboxOptions)
}}};
Event.onDOMReady(initShadowbox);
var initLightbox=initShadowbox;
var NavigableList=function(a){this.options={linkElement:null,listElement:null,inputElement:null,queryURL:null,jsonCallback:null,containerElement:null,selectFunction:null,enabledFunction:null,urlRewriterFunction:null,transition:"none",transitionSpeed:"fast"};
jQuery.extend(this.options,a);
this.isStaticList=(this.options.queryURL)?false:true;
jQuery(document).keydown(jQuery.proxy(this.handleKeyPress,this));
this.close(true);
if(this.isStaticList){this.closeTimer=null;
this.registerMouseListeners()
}else{var b=this;
this.options.inputElement.blur(function(){setTimeout(function(){b.close()
},500)
}).delayedObserver(function(){if(b.options.inputElement.val().length>0&&(!b.options.enabledFunction||b.options.enabledFunction())){var c=b.options.queryURL+b.options.inputElement.val();
if(b.options.urlRewriterFunction){c=b.options.urlRewriterFunction(c)
}if(b.options.jsonCallback){jQuery.ajax({url:c,dataType:(c.indexOf("callback=?")!=-1)?"jsonp":"json",crossDomain:(c.indexOf("callback=?")!=-1),success:function(f){jQuery.proxy(b.options.jsonCallback,b)(f);
if(b.options.containerElement.find("li").length>0){b.options.listElement=b.options.containerElement.find("ul");
b.registerMouseListeners();
b.open()
}else{b.close()
}}})
}else{jQuery.get(c,function(f){b.options.containerElement.empty().append(f);
if(b.options.containerElement.find("li").length>0){b.options.listElement=b.options.containerElement.find("ul");
b.registerMouseListeners();
b.open()
}else{b.close()
}})
}}else{b.close()
}},0.1)
}};
NavigableList.prototype.isVisible=function(){return this.options.containerElement.is(":visible")
};
NavigableList.prototype.handleKeyPress=function(a){if(this.isVisible()){switch(a.which){case jQuery.event.keyCodes.ENTER:return this.select(a);
break;
case jQuery.event.keyCodes.ESCAPE:a.preventDefault();
this.close();
break;
case jQuery.event.keyCodes.UP:a.preventDefault();
this.moveUp();
return false;
break;
case jQuery.event.keyCodes.DOWN:a.preventDefault();
this.moveDown();
return false;
break
}}};
NavigableList.prototype.select=function(a){if(!this.options.listElement){return
}if(this.isVisible()&&this.options.selectFunction&&this.options.listElement.find("li.hover").length>0){a.preventDefault();
return this.options.selectFunction(this.options.listElement.find("li.hover"))
}return true
};
NavigableList.prototype.moveUp=function(){if(!this.options.listElement){return
}if(this.options.listElement.find("li.hover").prevAll("li:not(.disabled,.separator)").length==0){this.options.listElement.find("li.hover").removeClass("hover");
this.options.listElement.find("li:last").addClass("hover")
}else{var a=this.options.listElement.find("li.hover");
a.prevAll("li:not(.disabled,.separator):first").addClass("hover");
a.removeClass("hover")
}};
NavigableList.prototype.moveDown=function(){if(!this.options.listElement){return
}if(this.options.listElement.find("li.hover").nextAll("li:not(.disabled,.separator)").length==0){this.options.listElement.find("li.hover").removeClass("hover");
this.options.listElement.find("li:first").addClass("hover")
}else{var a=this.options.listElement.find("li.hover");
a.nextAll("li:not(.disabled,.separator):first").addClass("hover");
a.removeClass("hover")
}};
NavigableList.prototype.open=function(){switch(this.options.transition){case"fade":this.options.containerElement.fadeIn(this.options.transitionSpeed).addClass("visible");
break;
case"slide":this.options.containerElement.slideDown(this.options.transitionSpeed).addClass("visible");
break;
case"none":default:this.options.containerElement.show().addClass("visible")
}};
NavigableList.prototype.close=function(a){if(a){this.options.containerElement.hide().removeClass("visible");
return
}switch(this.options.transition){case"fade":this.options.containerElement.fadeOut(this.options.transitionSpeed).removeClass("visible");
break;
case"slide":this.options.containerElement.slideUp(this.options.transitionSpeed).removeClass("visible");
break;
case"none":default:this.options.containerElement.hide().removeClass("visible")
}};
NavigableList.prototype.registerMouseListeners=function(){if(!this.options.listElement){return
}this.options.listElement.find("li:not(.disabled,.separator)").mouseover(jQuery.proxy(function(a){this.options.listElement.find("li.hover").removeClass("hover");
jQuery(a.currentTarget).addClass("hover")
},this)).mousedown(jQuery.proxy(function(a){this.select(a)
},this));
if(this.options.linkElement){jQuery.each([this.options.linkElement,this.options.listElement],jQuery.proxy(function(b,a){a.mouseenter(jQuery.proxy(function(){clearTimeout(this.closeTimer);
this.closeTimer=null;
this.open()
},this)).mouseleave(jQuery.proxy(function(){if(!this.closeTimer){this.closeTimer=setTimeout(jQuery.proxy(function(){this.close()
},this),500)
}},this))
},this))
}};
NavigableList.prototype.unregisterMouseListeners=function(){if(!this.options.listElement){return
}this.options.listElement.find("li:not(.disabled,.separator)").unbind("mouseover").unbind("mousedown");
if(this.options.linkElement){jQuery.each([this.options.linkElement,this.options.listElement],jQuery.proxy(function(b,a){a.unbind("mouseenter").unbind("mouseleave")
},this))
}};
(function(a){window.CtrlAltShortcuts={};
a(function(){a(document).keydown(function(g){var b=String.fromCharCode(g.which);
var f=g.altKey;
var c=g.ctrlKey;
if(c&&f&&CtrlAltShortcuts[b]){g.preventDefault();
CtrlAltShortcuts[b](g)
}})
})
})(jQuery);
if(typeof FlashObject=="undefined"){FlashObject=function(f,k,a,c,b,g){this.swf=f;
this.id=k;
this.width=a;
this.height=c;
this.version=7;
this.align=g;
this.redirect="";
this.sq=document.location.search.split("?")[1]||"";
this.altTxt="Please <a href='http://www.macromedia.com/go/getflashplayer'>upgrade your Flash Player</a>.";
this.bypassTxt="<p>Already have Flash Player? <a href='?detectflash=false&"+this.sq+"'>Click here if you have Flash Player "+this.version+" installed</a>.</p>";
this.params=new Object();
this.variables=new Object();
this.addParam("quality","high");
this.doDetect="";
this.detectedVersion;
this.embedType=b||"flash";
this.addParam("AutoStart","true");
this.addParam("WindowLess","false");
this.addParam("VideoBorderWidth","0");
this.addParam("VideoBorderColor","ffffff");
this.addParam("ShowControls","true")
};
FlashObject.prototype.addParam=function(a,b){this.params[a]=b
};
FlashObject.prototype.getParams=function(){return this.params
};
FlashObject.prototype.getParam=function(a){return this.params[a]
};
FlashObject.prototype.addVariable=function(a,b){this.variables[a]=b
};
FlashObject.prototype.getVariable=function(a){return this.variables[a]
};
FlashObject.prototype.getVariables=function(){return this.variables
};
FlashObject.prototype.getParamTags=function(){var a="";
for(var b in this.getParams()){a+='<param name="'+b+'" value="'+this.getParam(b)+'" />'
}if(a==""){a=null
}return a
};
FlashObject.prototype.getHTML=function(){if(this.embedType=="slideShow"){imageDir=this.getParam("imageDir");
url="http://www2.warwick.ac.uk/sitebuilder2/render/tocImages.xml?sbrPage="+imageDir+"&showGallery=off&rn="+new Date().getTime();
this.addVariable("xmlPath",unescape(url))
}var a="";
if(window.ActiveXObject&&navigator.userAgent.indexOf("Mac")==-1){if(this.embedType=="flash"||this.embedType=="slideShow"){a+='<object classid="clsid:d27cdb6e-ae6d-11cf-96b8-444553540000" codebase="http://fpdownload.macromedia.com/pub/shockwave/cabs/flash/swflash.cab#version=7,0,0,0" width="'+this.width+'" height="'+this.height+'" id="'+this.id+'">';
a+='<param name="allowScriptAccess" value="sameDomain" />';
a+='<param name="movie" value="'+this.swf+'" />';
a+='<param name="quality" value="high" />';
if(this.getParamTags()!=null){a+=this.getParamTags()
}if(this.getVariablePairs()!=null){a+='<param name="flashVars" value="'+this.getVariablePairs()+'" />'
}a+="</object>"
}else{if(this.embedType=="mediaplayer"){a=this.getMplayerCode()
}else{a+="<p>embed type "+this.embedType+" not recognised!</p>"
}}}else{if(this.embedType=="flash"||this.embedType=="slideShow"){a+='<embed allowScriptAccess="sameDomain" type="application/x-shockwave-flash" pluginspage="http://www.macromedia.com/go/getflashplayer" src="'+this.swf+'" width="'+this.width+'" height="'+this.height+'" id="'+this.id+'" name="'+this.id+'"';
for(var b in this.getParams()){a+=" "+b+'="'+this.getParam(b)+'"'
}if(this.getVariablePairs()!=null){a+=' flashVars="'+this.getVariablePairs()+'"'
}a+="></embed>"
}else{if(this.embedType=="mediaplayer"){a=this.getMplayerCode()
}else{a+="<p>embed type "+this.embedType+" not recognised!</p>"
}}}if(this.align){a='<div align="'+this.align+'">'+a+"</div>"
}else{a="<div>"+a+"</div>"
}return a
};
FlashObject.prototype.getMplayerCode=function(){var a='<object id="'+this.id+'" ';
a+='width="'+this.width+'" ';
a+='height="'+this.height+'" ';
a+='vspace="0" standby="Loading Microsoft&amp;#65533;Windows&amp;#65533; Media Player components..." ';
a+='codebase="http://activex.microsoft.com/activex/controls/mplayer/en/nsmp2inf.cab#Version=5,1,52,701" ';
a+='classid="CLSID:22D6F312-B0F6-11D0-94AB-0080C74C7E95" ';
a+='type="application/x-oleobject" ';
a+='hspace="0"> ';
a+='<param name="Filename" value="'+this.swf+'" /> ';
for(var b in this.getParams()){a+='<param name="'+b+'" value="'+this.getParam(b)+' ">'
}a+='<embed name="'+this.id+'" ';
a+='type="application/x-mplayer2" ';
a+='pluginspage="http://www.microsoft.com/Windows/Downloads/Contents/Products/MediaPlayer/" ';
a+="width="+this.width+" ";
a+="height="+this.height+" ";
a+='hspace="0" vspace="0" ';
a+='filename="'+this.swf+'" ';
for(var b in this.getParams()){value=this.getParam(b);
if(value=="true"||value=="false"){a+=b+"="+(value=="true"?"1":"0")+" "
}else{a+=b+"="+value+" "
}}a+="</embed></object>";
return a
};
FlashObject.prototype.getVariablePairs=function(){var b=new Array();
for(var a in this.getVariables()){b.push(a+"="+escape(this.getVariable(a)))
}if(b.length>0){return b.join("&")
}else{return null
}};
FlashObject.prototype.write=function(a){if(detectFlash(this.version)||this.doDetect=="false"){if(a){document.getElementById(a).innerHTML=this.getHTML()
}else{document.write(this.getHTML())
}}else{if(this.redirect!=""){document.location.replace(this.redirect)
}else{if(getFlashVersion()==0){var b=getQueryParamValueFromString(this.swf,"theFile");
this.altTxt="<p><a href='"+b+"'>"+getFilenameFromUrl(b)+"</a></p>"
}if(a){document.getElementById(a).innerHTML=this.altTxt
}else{document.write(this.altTxt+""+this.bypassTxt+"<p><a href='"+b+"'>"+getFilenameFromUrl(b)+"</a></p>")
}}}}
}function getFlashVersion(){var a=FlashVersionDetector.getMajorVersionInteger();
return(a<0)?0:a
}function detectFlash(a){if(getFlashVersion()>=a){return true
}else{return false
}}function getQueryParamValue(b){var a=document.location.search;
return(getQueryParamValueFromString(a,b))
}function getQueryParamValueFromString(a,f){var c=a;
var g=c.indexOf(f);
var b=(c.indexOf("&",g)!=-1)?c.indexOf("&",g):c.length;
if(c.length>1&&g!=-1){return c.substring(c.indexOf("=",g)+1,b)
}else{return""
}}function getFilenameFromUrl(b){var a=b.substring(b.lastIndexOf("/")+1);
return a
}if(Array.prototype.push==null){Array.prototype.push=function(a){this[this.length]=a;
return this.length
}
}if(String.prototype.toAbsoluteUrl==null){String.prototype.toAbsoluteUrl=function(){var b=this;
if(b.indexOf(":")<0){var c=""+window.location.href;
var a=document.getElementsByTagName("base");
if(a.length>0){c=a[0].href
}if(b.startsWith("//")){b=c.substring(0,c.indexOf(":")+1)+b
}else{if(b.indexOf("/")===0){b=b.substring(1);
c=c.substring(0,c.indexOf("/",7))
}if(c.charAt(c.length-1)!="/"){c+="/"
}b=c+b
}}return b
}
}if(typeof FlashVersionDetector=="undefined"){var isIE=(navigator.appVersion.indexOf("MSIE")!=-1)?true:false;
var isWin=(navigator.appVersion.toLowerCase().indexOf("win")!=-1)?true:false;
var isOpera=(navigator.userAgent.indexOf("Opera")!=-1)?true:false;
var FlashVersionDetector={getActiveXControlVersion:function(){var a;
var b;
var c;
try{b=new ActiveXObject("ShockwaveFlash.ShockwaveFlash.7");
a=b.GetVariable("$version")
}catch(c){}if(!a){try{b=new ActiveXObject("ShockwaveFlash.ShockwaveFlash.6");
a="WIN 6,0,21,0";
b.AllowScriptAccess="always";
a=b.GetVariable("$version")
}catch(c){}}if(!a){try{b=new ActiveXObject("ShockwaveFlash.ShockwaveFlash.3");
a=b.GetVariable("$version")
}catch(c){}}if(!a){try{b=new ActiveXObject("ShockwaveFlash.ShockwaveFlash.3");
a="WIN 3,0,18,0"
}catch(c){}}if(!a){try{b=new ActiveXObject("ShockwaveFlash.ShockwaveFlash");
a="WIN 2,0,0,11"
}catch(c){a=-1
}}return a
},getVersion:function(){var k=-1;
if(navigator.plugins!=null&&navigator.plugins.length>0){if(navigator.plugins["Shockwave Flash 2.0"]||navigator.plugins["Shockwave Flash"]){var h=navigator.plugins["Shockwave Flash 2.0"]?" 2.0":"";
var a=navigator.plugins["Shockwave Flash"+h].description;
var g=a.split(" ");
var c=g[2].split(".");
var l=c[0];
var b=c[1];
var f=g[3];
if(f==""){f=g[4]
}if(f[0]=="d"){f=f.substring(1)
}else{if(f[0]=="r"){f=f.substring(1);
if(f.indexOf("d")>0){f=f.substring(0,f.indexOf("d"))
}}}var k=l+"."+b+"."+f
}}else{if(navigator.userAgent.toLowerCase().indexOf("webtv/2.6")!=-1){k=4
}else{if(navigator.userAgent.toLowerCase().indexOf("webtv/2.5")!=-1){k=3
}else{if(navigator.userAgent.toLowerCase().indexOf("webtv")!=-1){k=2
}else{if(isIE&&isWin&&!isOpera){k=FlashVersionDetector.getActiveXControlVersion()
}}}}}return k
},isClientHasVersionString:function(a){versionArray=a.replace(/,/g,".").split(".");
if(versionArray.length==3){return FlashVersionDetector.isClientHasVersion(versionArray[0],versionArray[1],versionArray[2])
}else{if(versionArray.length==4){return FlashVersionDetector.isClientHasVersion(versionArray[0],versionArray[1],versionArray[3])
}else{return false
}}},isClientHasVersion:function(h,f,c){versionStr=FlashVersionDetector.getVersion();
if(versionStr==-1){return false
}else{if(versionStr!=0){if(isIE&&isWin&&!isOpera){tempArray=versionStr.split(" ");
tempString=tempArray[1];
versionArray=tempString.split(",")
}else{versionArray=versionStr.split(".")
}var g=versionArray[0];
var a=versionArray[1];
var b=versionArray[2];
if(g>parseFloat(h)){return true
}else{if(g==parseFloat(h)){if(a>parseFloat(f)){return true
}else{if(a==parseFloat(f)){if(b>=parseFloat(c)){return true
}}}}}return false
}}},getMajorVersionInteger:function(){versionStr=FlashVersionDetector.getVersion();
if(versionStr==-1){return -1
}else{if(versionStr!=0){if(isIE&&isWin&&!isOpera){tempArray=versionStr.split(" ");
tempString=tempArray[1];
versionArray=tempString.split(",")
}else{versionArray=versionStr.split(".")
}return parseInt(versionArray[0],10)
}}}}
}(function(){if(!
/*@cc_on!@*/
0){return
}var e="abbr,article,aside,audio,bb,canvas,datagrid,datalist,details,dialog,eventsource,figure,footer,header,hgroup,mark,menu,meter,nav,output,progress,section,time,video".split(",");
for(var i=0;
i<e.length;
i++){document.createElement(e[i])
}})();
(function(a){window.ButtonPlayer=function(c,b){this.$container=a("#"+b);
this.basename=c.getBasename();
this.$container.attr("title",this.basename+" | Click to play");
this.$container.addClass("buttonPlayer");
this.$playButton=a('<div class="play button"><div class="glyph"></div></div>');
this.$container.append(this.$playButton);
this.$pauseButton=a('<div class="pause button"><div class="glyph"></div></div>');
this.$container.append(this.$pauseButton);
this.$progressContainer=a('<div class="progressContainer" />');
this.$container.append(this.$progressContainer);
this.$progressLoading=a('<div class="progressLoading" />');
this.$progress=a('<div class="progress" />');
this.$progressLoading.append(this.$progress);
this.$progressContainer.append(this.$progressLoading);
this.$stopButton=a('<div class="stop button"><div class="glyph"></div></div>');
this.$container.append(this.$stopButton);
this.$pauseButton.hide();
this.audio=c;
this.duration=null;
this.executor=null;
this.$playButton.click(a.proxy(function(){this.$playButton.hide();
this.$pauseButton.show();
this.audio.play();
if(this.executor==null){this.executor=setTimeout(a.proxy(function(){var g=this.audio.a;
var f=0;
if(g.buffered.length>0){f=parseInt(((g.buffered.end(0)/g.duration)*100))
}this.$progressLoading.css({width:f+"%"})
},this),400);
this.$pauseButton.click(a.proxy(function(){this.$pauseButton.hide();
this.$playButton.show();
this.audio.pause()
},this));
this.$stopButton.click(a.proxy(function(){this.$pauseButton.hide();
this.$playButton.show();
this.audio.stop();
clearTimeout(this.executor)
},this));
this.audio.observeEnd(a.proxy(function(){this.$pauseButton.hide();
this.$playButton.show();
this.$progress.css({width:"0%"});
clearTimeout(this.executor)
},this));
this.audio.observeDurationchange(a.proxy(function(h){this.duration=h;
if(h!=NaN&&h!=Infinity){var f=parseInt(h);
var g="";
jQuery.each(new Array("s","m","h"),function(l,k){v=f%60;
g=""+v+k+" "+g;
f=(f-v)/60;
if(f<=0){return false
}});
this.$container.attr("title",this.basename+" | Duration: "+g)
}},this));
this.audio.observeTimeupdate(a.proxy(function(g){if(this.duration!=null){var f=(g*100)/this.duration;
this.$progress.css({width:f+"%"})
}},this))
}},this))
};
window.LongPlayer=function(f,c){var g=jQuery;
this.$container=g("#"+c);
this.$container.addClass("longPlayer");
this.added=false;
var b=g('<div class="clicktoplay"><div class="playicon"></div></div>');
this.$container.append(b);
b.click(g.proxy(function(h){b.hide();
if(!this.added){f.addSelf(this.$container)
}f.a.controls=true;
f.play()
},this))
};
window.NativeAudio=function(b){this.delayedSrc=b
};
NativeAudio.prototype.lazyInit=function(){if(!this.a){this.a=document.createElement("audio");
if(this.delayedSrc){this.a.src=this.delayedSrc;
this.delayedSrc=null
}}};
NativeAudio.prototype.play=function(){this.lazyInit();
this.a.play()
};
NativeAudio.prototype.getBasename=function(){var c=this.delayedSrc||this.a.src;
var b=c.lastIndexOf("/");
if(b>-1&&b<c.length){c=c.substring(b+1)
}return c
};
NativeAudio.prototype.pause=function(){this.lazyInit();
this.a.pause()
};
NativeAudio.prototype.stop=function(){this.a.pause();
this.a.currentTime=0
};
NativeAudio.prototype.seek=function(b){this.lazyInit();
this.a.currentTime=b
};
NativeAudio.prototype.observeTimeupdate=function(b){a(this.a).bind("timeupdate",a.proxy(function(){b(this.a.currentTime)
},this))
};
NativeAudio.prototype.observeDurationchange=function(b){a(this.a).bind("durationchange",a.proxy(function(){b(this.a.duration)
},this))
};
NativeAudio.prototype.observeEnd=function(b){a(this.a).bind("ended",b);
a(this.a).bind("timeupdate",a.proxy(function(){if(this.a.currentTime==this.a.duration){b()
}},this))
};
NativeAudio.prototype.addSelf=function(b){this.lazyInit();
b.append(this.a)
};
NativeAudio.canPlayType=function(c){var b=document.createElement("audio");
var f=navigator.userAgent;
var g=f.match(/Windows.+Safari/)&&!f.match(/Chrome/);
return !!(!g&&b.canPlayType&&b.canPlayType(c).replace(/no/,""))
};
NativeAudio.canPlayMp3=function(){return NativeAudio.canPlayType("audio/mpeg;")
}
})(jQuery);
if(typeof SortableTables=="undefined"){var SortableTables={DATE_RE:/^(\d\d?)[\/\.-](\d\d?)[\/\.-]((\d\d)?\d\d)$/,FILESIZE_RE:/^\(?([\d\.]+)\s([KM]B)\)?$/,tables:{},init:function(){if(!document.getElementsByTagName){return
}tbls=document.getElementsByTagName("table");
for(ti=0;
ti<tbls.length;
ti++){thisTbl=tbls[ti];
if((" "+thisTbl.className+" ").indexOf("sitebuilder_sortable")!=-1){if(!thisTbl.id){var a="sortableTable_"+Math.floor(Math.random()*100000);
thisTbl.id=a
}SortableTables.tables[thisTbl.id]=new SortableTable(thisTbl)
}}},registerCallback:function(a,b){SortableTables.tables[a.id].callback=b
}};
var SortableTable=function(k){this.table=k;
if(k.rows&&k.rows.length>0){var o=k.rows[0]
}if(!o){return
}var l=jQuery.proxy(function(f,p){return jQuery.proxy(function(){this.resort(f,p);
return false
},this)
},this);
for(var h=0;
h<o.cells.length;
h++){var c=o.cells[h];
if((" "+c.className+" ").indexOf("sortable")!=-1){var b=this._getInnerText(c);
var n=(c.title)?c.title:b;
c.title="Click to sort by "+n.toLowerCase();
var g=document.createElement("a");
g.href="#";
g.className="sortheader";
g.onclick=l(g,h);
g.innerHTML=b+'<span class="sortarrow">&nbsp;&nbsp;&nbsp;</span>';
c.innerHTML="";
c.appendChild(g)
}}this.callback()
};
SortableTable.prototype.poundRegex=new RegExp("(\u00A3|&pound;)","g");
SortableTable.prototype.resort=function(k,n){var r;
for(var w=0;
w<k.childNodes.length;
w++){if(k.childNodes[w].tagName&&k.childNodes[w].tagName.toLowerCase()=="span"){r=k.childNodes[w]
}}var a=this._getInnerText(r);
var c=k.parentNode;
var f=n||c.cellIndex;
var u=this.table;
if(u.rows.length<=1){return
}var l=this._getInnerText(u.rows[1].cells[f]);
var q=this._sort_caseInsensitive;
if(l.match(/^[\d\.]+$/)){q=this._sort_numeric
}else{if(l.match(this.poundRegex)){q=this._sort_pounds
}}possdate=l.match(SortableTables.DATE_RE);
if(possdate){first=parseInt(possdate[1]);
second=parseInt(possdate[2]);
if(first>12){q=this._sort_ddmm
}else{if(second>12){q=this._sort_mmdd
}else{q=this._sort_ddmm
}}}if((" "+u.rows[0].cells[f].className+" ").indexOf("sortable_filesize")!=-1){q=this._sort_filesize
}this.sortColumn=f;
var g=new Array();
var h=new Array();
for(i=0;
i<u.rows[0].length;
i++){g[i]=u.rows[0][i]
}for(j=1;
j<u.rows.length;
j++){h[j-1]=u.rows[j]
}h.sort(jQuery.proxy(q,this));
var p;
if(r.getAttribute("sortdir")=="down"){p="&nbsp;&nbsp;&uarr;";
h.reverse();
r.setAttribute("sortdir","up");
k.className="sortheader sortup"
}else{p="&nbsp;&nbsp;&darr;";
r.setAttribute("sortdir","down");
k.className="sortheader sortdown"
}for(i=0;
i<h.length;
i++){if(!h[i].className||(h[i].className&&(h[i].className.indexOf("sortbottom")==-1))){u.tBodies[0].appendChild(h[i])
}}for(i=0;
i<h.length;
i++){if(h[i].className&&(h[i].className.indexOf("sortbottom")!=-1)){u.tBodies[0].appendChild(h[i])
}}var o=document.getElementsByTagName("span");
for(var w=0;
w<o.length;
w++){if(o[w].className=="sortarrow"){if(this._getParent(o[w],"table")==this._getParent(k,"table")){o[w].innerHTML="&nbsp;&nbsp;&nbsp;"
}}}var s=this._getParent(k,"tr");
var b=s.getElementsByTagName("a");
for(var w=0;
w<b.length;
w++){if(b[w]!=k){b[w].className="sortheader"
}}r.innerHTML=p;
this.callback()
};
SortableTable.prototype._sort_caseInsensitive=function(f,c){aa=this._getInnerText(f.cells[this.sortColumn]).toLowerCase();
bb=this._getInnerText(c.cells[this.sortColumn]).toLowerCase();
if(String.prototype.trim){aa=aa.trim();
bb=bb.trim()
}if(aa==bb){return 0
}if(aa<bb){return -1
}return 1
};
SortableTable.prototype._sort_default=function(f,c){aa=this._getInnerText(f.cells[this.sortColumn]);
bb=this._getInnerText(c.cells[this.sortColumn]);
if(aa==bb){return 0
}if(aa<bb){return -1
}return 1
};
SortableTable.prototype._sort_numeric=function(f,c){return this._doNumericSort(this._getInnerText(f.cells[this.sortColumn]),this._getInnerText(c.cells[this.sortColumn]))
};
SortableTable.prototype._sort_pounds=function(f,c){return this._doNumericSort(this._getInnerText(f.cells[this.sortColumn]).replace(this.poundRegex,"").replace(",",""),this._getInnerText(c.cells[this.sortColumn]).replace(this.poundRegex,"").replace(",",""))
};
SortableTable.prototype._sort_ddmm=function(f,c){aa=this._getInnerText(f.cells[this.sortColumn]);
bb=this._getInnerText(c.cells[this.sortColumn]);
mtch=aa.match(SortableTables.DATE_RE);
y=mtch[3];
m=mtch[2];
d=mtch[1];
if(m.length==1){m="0"+m
}if(d.length==1){d="0"+d
}dt1=y+m+d;
mtch=bb.match(SortableTables.DATE_RE);
y=mtch[3];
m=mtch[2];
d=mtch[1];
if(m.length==1){m="0"+m
}if(d.length==1){d="0"+d
}dt2=y+m+d;
if(dt1==dt2){return 0
}if(dt1<dt2){return -1
}return 1
};
SortableTable.prototype._sort_mmdd=function(f,c){aa=this._getInnerText(f.cells[this.sortColumn]);
bb=this._getInnerText(c.cells[this.sortColumn]);
mtch=aa.match(SortableTables.DATE_RE);
y=mtch[3];
d=mtch[2];
m=mtch[1];
if(m.length==1){m="0"+m
}if(d.length==1){d="0"+d
}dt1=y+m+d;
mtch=bb.match(SortableTables.DATE_RE);
y=mtch[3];
d=mtch[2];
m=mtch[1];
if(m.length==1){m="0"+m
}if(d.length==1){d="0"+d
}dt2=y+m+d;
if(dt1==dt2){return 0
}if(dt1<dt2){return -1
}return 1
};
SortableTable.prototype._sort_filesize=function(f,c){aa=this._getInnerText(f.cells[this.sortColumn]);
bb=this._getInnerText(c.cells[this.sortColumn]);
a_bytes=0;
b_bytes=0;
mtch=aa.match(SortableTables.FILESIZE_RE);
if(mtch){num=mtch[1];
units=mtch[2];
if(units=="MB"){a_bytes=num*1024*1024
}else{a_bytes=num*1024
}}mtch=bb.match(SortableTables.FILESIZE_RE);
if(mtch){num=mtch[1];
units=mtch[2];
if(units=="MB"){b_bytes=num*1024*1024
}else{b_bytes=num*1024
}}return this._doNumericSort(a_bytes,b_bytes)
};
SortableTable.prototype._doNumericSort=function(f,c){aa=parseFloat(f);
if(isNaN(aa)){aa=0
}bb=parseFloat(c);
if(isNaN(bb)){bb=0
}return aa-bb
};
SortableTable.prototype._getInnerText=function(f){if(typeof f=="string"){return f.trim()
}if(typeof f=="undefined"){return""
}if(f.innerText){return f.innerText.trim()
}var g="";
var c=f.childNodes;
var a=c.length;
for(var b=0;
b<a;
b++){switch(c[b].nodeType){case 1:g+=this._getInnerText(c[b]);
break;
case 3:g+=c[b].nodeValue;
break
}}return g.trim()
};
SortableTable.prototype._getParent=function(b,a){if(b==null){return null
}else{if(b.nodeType==1&&b.tagName.toLowerCase()==a.toLowerCase()){return b
}else{return this._getParent(b.parentNode,a)
}}};
SortableTable.prototype._zebraStripe=function(){var b=this.table;
rows=b.getElementsByTagName("tr");
var c=true;
for(var a=0;
a<rows.length;
a++){cells=rows[a].getElementsByTagName("td");
if(cells.length==0){continue
}rows[a].className=c?"odd":"even";
c=!c
}};
SortableTable.prototype.callback=function(){this._zebraStripe()
};
Event.onDOMReady(SortableTables.init)
}SitebuilderHeaderSlideshow={beforeTransition:function(b,a){},afterTransition:function(a,b){}};
jQuery(document).ready(function(g){var l=function(n){var o=g.grep(n.get(0).className.split(" "),function(p){return p.match(/slide_\d+/)
});
if(o.length==0){return null
}else{return o[0]
}};
if(g("#header .slide").length>1&&(!g.browser.msie||parseInt(g.browser.version)>=7)){var k=g("#header").data("transition")||"slideshow";
var f=g("#header .slide");
var k=g("#header").data("transition")||"slide";
var c=g('<div class="slideshow-button slideshow-button-left" unselectable="on"><a href="#prev" title="View the previous slide" unselectable="on"></a></div>"');
var b=g('<div class="slideshow-button slideshow-button-right" unselectable="on"><a href="#next" title="View the next slide" unselectable="on"></a></div>"');
var h=g("#header .slide.active");
g("#header").addClass(l(h));
b.click(function(n){var s=g("#header .slide.active");
if(s.is(":animated")){n.stopPropagation();
n.preventDefault();
return false
}var q=s.next(".slide");
if(q.length==0){q=g("#header .slide").first()
}SitebuilderHeaderSlideshow.beforeTransition(s,q);
var p=g("#header").width();
if(k=="crossfade"){q.stop().hide().css({left:"0px"}).fadeTo(400,1,function(){g(this).addClass("active");
g("#header").removeClass(l(g("#header")));
g("#header").addClass(l(g(this)));
SitebuilderHeaderSlideshow.afterTransition(s,q)
});
s.stop().css({left:"0px"}).fadeTo(400,0,function(){g(this).removeClass("active")
})
}else{q.stop().css({left:p+"px"});
s.stop().css({left:"0px"});
g([s[0],q[0]]).animate({left:"-="+p},{duration:400,easing:"swing",complete:function(){s.removeClass("active").css({left:p+"px"});
q.addClass("active").css({left:"0px"});
g("#header").removeClass(l(g("#header")));
g("#header").addClass(l(g(this)));
SitebuilderHeaderSlideshow.afterTransition(s,q)
}})
}var r=q.find(".strapline");
if(r.length>0){var o=r.html();
if(o!=g("#strapline").html()){g("#strapline").fadeTo(200,0,function(){g(this).html(o).fadeTo(200,1)
})
}}n.stopPropagation();
n.preventDefault();
return false
});
c.click(function(n){var r=g("#header .slide.active");
if(r.is(":animated")){n.stopPropagation();
n.preventDefault();
return false
}var q=r.prev(".slide");
if(q.length==0){q=g("#header .slide").last()
}SitebuilderHeaderSlideshow.beforeTransition(r,q);
var p=g("#header").width();
if(k=="crossfade"){q.stop().hide().css({left:"0px"}).fadeTo(400,1,function(){g(this).addClass("active");
g("#header").removeClass(l(g("#header")));
g("#header").addClass(l(g(this)));
SitebuilderHeaderSlideshow.afterTransition(r,q)
});
r.stop().css({left:"0px"}).fadeTo(400,0,function(){g(this).removeClass("active")
})
}else{q.stop().css({left:"-"+p+"px"});
r.stop().css({left:"0px"});
g([r[0],q[0]]).animate({left:"+="+p},{duration:400,easing:"swing",complete:function(){r.removeClass("active").css({left:"-"+p+"x"});
q.addClass("active").css({left:"0px"});
g("#header").removeClass(l(g("#header")));
g("#header").addClass(l(g(this)));
SitebuilderHeaderSlideshow.afterTransition(r,q)
}})
}if(q.find(".strapline").length>0){var o=q.find(".strapline").html();
g("#strapline").fadeTo(200,0,function(){g(this).html(o).fadeTo(200,1)
})
}n.stopPropagation();
n.preventDefault();
return false
});
g("#header").prepend(c);
g("#header").append(b);
var a=g("#header").data("delay");
if(a){g("#header").mouseenter(function(){g(this).data("hover",true)
}).mouseleave(function(){g(this).data("hover",false)
});
window.setInterval(function(){if(!g("#header").data("hover")){b.click()
}},a*1000)
}}});
(function(h){var o=false;
var g=false;
var l=function(p,q){p.hover(function(){h(this).addClass(q)
},function(){h(this).removeClass(q)
})
};
jQuery.exist=function(){var p=true;
jQuery.each(arguments,function(){if(!this.length){p=false;
return false
}});
return p
};
window.SitebuilderInfo={url:null,lastContentUpdated:null,searchHasFocus:false,setupHeight:function(){var u=jQuery("#footer");
var q=jQuery("#navigation-and-content");
if(!q.length){return
}var p;
if(u.length){p=function(){var w=u.offset().top+u.outerHeight(true)+1;
return q.outerHeight(true)+(jQuery(window).height()-w)
}
}else{p=function(){return jQuery(window).height()-(q.offset().top+1)
}
}var r=p();
if(jQuery("#navigation.vertical").length&&r<jQuery("#navigation.vertical").outerHeight(true)){r=jQuery("#navigation.vertical").outerHeight(true)
}q.css({"min-height":r+"px"});
if(u.length){footerHeight=u.offset().top+u.outerHeight(true)+1;
var s=jQuery(window).height()-footerHeight;
if(s>0){q.css({"min-height":(r+s)+"px"})
}}},setupSearch:function(){h("#search-container").mouseover(function(){h("#search-container #search-button").addClass("hover")
}).mouseout(function(){if(!SitebuilderInfo.searchHasFocus){h("#search-container #search-button").removeClass("hover")
}});
h("#search-box").focus(function(x){SitebuilderInfo.searchHasFocus=true
}).blur(function(x){SitebuilderInfo.searchHasFocus=false;
h("#search-container").mouseout()
});
var u=h("#search-container ul:nth(0)");
var w=h("#search-index-menu");
u.find("li").not(".more-link").appendTo(w).click(function(){q(h(this));
p();
h("#search-box").focus()
});
l(w.find("li"),"hover");
if(!h.browser.msie||parseInt(h.browser.version)>=8){var r,s=false;
var p=function(){w.fadeOut("fast")
};
u.hover(function(x){if(r){clearTimeout(r)
}w.fadeIn("fast")
},function(x){if(r){clearTimeout(r)
}r=setTimeout(p,200)
}).bind("touchstart",function(x){s=true;
if(w.is(":visible")){return true
}else{x.preventDefault();
w.fadeIn("fast");
return false
}}).find(".more-link").bind("touchstart",function(x){s=true;
if(w.is(":visible")){return true
}else{x.preventDefault();
w.fadeIn("fast");
return false
}});
h(document.body).bind("touchstart",function(x){if(h(x.target).closest("#search-container ul").length==0){w.fadeOut("fast")
}})
}function q(B){var x,z,A;
z=h("#search-box");
u.find("> li").not(".more-link").remove();
$searchlink='<a title="View more search options" href="//search.warwick.ac.uk/website" rel="nofollow">'+B.data("index-title")+"</a>";
B.clone().empty().prependTo(u).addClass("active").append($searchlink);
x=z.parent("form");
x.attr("action","//search.warwick.ac.uk/"+B.data("index-section"));
x.find("input[name=source]").remove();
x.find("input[name=fileFormat]").remove();
if(B.data("source")){x.prepend(h('<input type="hidden" name="source">').val(B.data("source")))
}if(B.data("index-file-format")){h.each(B.data("index-file-format").split(";"),function(C,D){x.prepend(h('<input type="hidden" name="fileFormat">').val(D))
})
}x.find("input[name=urlPrefix]").remove();
if(B.data("url-prefix")){x.prepend(h('<input type="hidden" name="urlPrefix">').val(B.data("url-prefix")))
}A=B.text();
if(A.length>28){A=A.substring(0,28)+"\u2026"
}z.attr("placeholder",A)
}q(w.find("li.active"));
w.find("li.active").removeClass("active")
},setupWarwickLinks:function(){var p=h("#warwick-logo-container.on-hover");
if(p.length>0){var q=p.find("#warwick-site-links");
q.hide();
var r=function(s,u){if(u){p.unbind("mouseover").unbind("mouseout")
}else{p.mouseover(function(){q.stop().fadeTo("fast",1)
});
p.mouseout(function(){q.stop().fadeTo("fast",0)
})
}};
h(document.body).bind("smallscreen",r);
r(null,h(document.body).hasClass("is-smallscreen"))
}h("#masthead.transparent").mouseover(function(){h(this).removeClass("transparent")
}).mouseout(function(){h(this).addClass("transparent")
})
}};
var k=720;
var b=960;
var c=h(window).width();
var n=new WCookie("ForceTablet");
var f=h("html").hasClass("force-tablet-in-edit");
var a=f||(n.value&&n.value=="yes");
if(c>b){h("#meta-viewport").attr("content","width=device-width");
h("#meta-mobile-optimized").attr("content",b)
}else{if(c>=k||(c<k&&a)){h("#meta-viewport").attr("content","width="+b);
h("#meta-mobile-optimized").attr("content",b)
}}h(function(w){var s=w(document.body),q=w(window),x=w("#masthead"),B=w("#utility-container"),u=w("#search-container"),r=w("#warwick-logo-container"),A=w("#utility-bar"),z;
if(u.length>0){s.bind("smallscreen",function(C,D){if(D){if(w("#alternate-search").length==0){if(s.is(".site-root")){w("#content-wrapper").prepend(w('<div id="alternate-search"><hr /></div>'))
}else{w("#content-wrapper").append(w('<div id="alternate-search"><hr /></div>'))
}}if(s.is(".site-root")){w("#alternate-search").prepend(u)
}else{w("#alternate-search").append(u)
}}else{w("#utility-container").append(u)
}})
}var p=function(){var C=q.width();
var D=C<k&&!a;
if(D&&!o){s.addClass("is-smallscreen");
s.trigger("smallscreen",true)
}else{if(!D&&o){s.removeClass("is-smallscreen");
s.trigger("smallscreen",false)
}}o=D;
var E=C<=b&&(C>=k||a);
if(E&&!g){s.addClass("is-tablet");
if(a){s.addClass("force-tablet")
}s.trigger("tablet",true)
}else{if(!E&&g){s.removeClass("is-tablet");
if(a){s.removeClass("force-tablet")
}s.trigger("tablet",false)
}}g=E
};
q.resize(p);
p()
});
h(function(){var q=function(s,u){if(u&&h("#footer .mobile-switcher-link").length==0){var r=h('<div class="mobile-switcher-link"></div>');
h("#footer > .content").prepend(r);
r.append("View website in: ");
r.append(h('<a href="#force-tablet" />').html("Standard").click(function(w){n.value="yes";
n.expires="; expires=31 December 2034 23:54:30";
n.save();
w.stopPropagation();
w.preventDefault();
window.location.reload();
return false
}));
r.append(" | <strong>Mobile</strong>")
}};
var p=function(u,r){if(r&&h(document.body).hasClass("force-tablet")&&h("#footer .mobile-switcher-link").length==0){var s=h('<div class="mobile-switcher-link"></div>');
h("#footer > .content").prepend(s);
s.append("View website in: <strong>Standard</strong> | ");
s.append(h('<a href="#unforce-tablet" />').html("Mobile").click(function(w){n.value="";
n.expires="; expires=31 December 2034 23:54:30";
n.save();
w.stopPropagation();
w.preventDefault();
window.location.reload();
return false
}))
}};
h(document.body).bind("smallscreen",q);
h(document.body).bind("tablet",p);
if(h(document.body).hasClass("is-smallscreen")){q(null,true)
}if(h(document.body).hasClass("is-tablet")){p(null,true)
}});
h(function(){SitebuilderInfo.setupSearch();
SitebuilderInfo.setupWarwickLinks();
var u=h("#container");
var q=h("#edit-link");
if(q.length>0){var p=new NavigableList({linkElement:q,listElement:h("#edit-tool-menu"),containerElement:h("#edit-tool-container"),selectFunction:function(w){if(w.children("a.disabled").length==0){window.location=w.find("a").attr("href")
}else{return false
}},transition:"fade"});
var s=function(w,x){if(x){p.unregisterMouseListeners()
}else{p.registerMouseListeners()
}};
h(document.body).bind("smallscreen",s);
if(h(document.body).hasClass("is-smallscreen")){s(null,true)
}h(window).load(function(){var w=u.width()-(q.offset().left-u.offset().left)-q.width();
h("#edit-tool-container").css({right:w})
})
}if(h("#utility-bar").length>0){var r=function(w,x){if(x){h("#utility-bar").insertAfter(h("#page-footer-elements"))
}else{h("#utility-bar").prependTo(h("#utility-container"))
}};
h(document.body).bind("smallscreen",r);
if(h(document.body).hasClass("is-smallscreen")){r(null,true)
}}SitebuilderInfo.setupHeight();
if(!h.browser.msie||parseInt(h.browser.version)>=9){h(window).bind("resize orientationchange",SitebuilderInfo.setupHeight)
}if(h.browser.msie&&parseInt(h.browser.version)<=6){h.fx.off=true
}});
h(function(){h("#main-content img").load(function(){if(h(this).closest("a").length>0){return
}if(this.naturalWidth&&this.clientWidth&&this.naturalWidth>(this.clientWidth*1.33)){h(this).wrap(h("<a />").attr({href:this.src,rel:"lightbox"}));
initLightbox()
}})
});
h(window).load(function(){h("table").each(function(){var r=h(this);
if(r.is(':visible') && Math.floor(r.width())>r.parent().width()){r.wrap(h('<div><div class="sb-wide-table-wrapper"></div></div>'))
}});
if(h("body.is-smallscreen").length===0&&h("div.sb-wide-table-wrapper").length>0){var p=function(r){r.stopPropagation();
r.preventDefault();
if(!Shadowbox.initialized){Shadowbox.initialized=true;
Shadowbox.init(shadowboxOptions)
}var s=h(this).closest("div").find("div.sb-wide-table-wrapper");
Shadowbox.open({link:this,content:'<div class="sb-wide-table-wrapper" style="background: white;">'+s.html()+"</div>",player:"html",width:h(window).width(),height:h(window).height()})
};
var q=function(){return h("<span/>").addClass("sb-table-wrapper-popout").append("(").append(h("<a/>").attr("href","#").html("Pop-out table").on("click",p)).append(")")
};
h("div.sb-wide-table-wrapper > table").each(function(){var r=h(this);
if(!r.hasClass('sb-no-wrapper-table-popout')&&Math.floor(r.width())>r.parent().width()){r.parent().parent("div").prepend(q()).append(q())
}})
}})
})(jQuery);
jQuery(function(a){new NavigableList({inputElement:a("#search-box"),queryURL:"//sitebuilder.warwick.ac.uk/sitebuilder2/api/go/redirects.json?maxResults=6&prefix=",containerElement:a("#search-suggestions"),jsonCallback:function(f){this.options.containerElement.empty();
var b=a("<ul />");
var c=true;
a.each(f,function(h,k){var g=a("<li />");
if(c){g.addClass("odd")
}else{g.addClass("even")
}c=!c;
g.append(a('<span class="redirectpath" />').html(k.path));
g.append(a('<span class="redirectdescription" />').append(a('<span class="informal" />').html(k.description)));
b.append(g)
});
this.options.containerElement.append(b)
},selectFunction:function(b){window.location="http://go.warwick.ac.uk/"+a(b).find(".redirectpath").html()+"?goSearchReferer="+encodeURIComponent(window.location)+"&goSearchQuery="+a("#search-box").val();
a("#search-container").submit(function(c){c.preventDefault();
return false
});
return false
},enabledFunction:function(){var b=a("#search-box").closest("form").find("li.active");
return !b.data("go-disabled")
},urlRewriterFunction:function(b){var c=a("#search-box").closest("form").find("li.active");
if(c.data("go-type")){b+="&type="+c.data("go-type")
}if(c.data("go-prefix")){b+="&urlPrefix="+c.data("go-prefix")
}return b+"&callback=?"
}})
});
(function(a){window.id6nav=window.id6nav||{};
id6nav.isIE6or7=/msie|MSIE 6/.test(navigator.userAgent)||/msie|MSIE 7/.test(navigator.userAgent);
id6nav.isiOS=(navigator.userAgent.match(/iP(hone|od|ad)/i)!=null);
id6nav.isAndroid=(navigator.userAgent.match(/Android/i)!=null);
id6nav.repositionNavigation=function(){};
id6nav.states={UNDEFINED:"undefined",NORMAL:"normal",FIXED:"fixed",FIXEDBOTTOM:"fixed-bottom",SMALLSCREEN:"smallscreen"};
id6nav.layouts={UNDEFINED:"undefined",VERTICAL:"vertical",HORIZONTAL:"horizontal"};
id6nav.state=id6nav.states.UNDEFINED;
id6nav.layout=id6nav.layouts.UNDEFINED;
id6nav.$element=a();
id6nav.hasLayout=function(){return id6nav.layout!=id6nav.layouts.UNDEFINED
};
id6nav.changeState=function(b){if(id6nav.state!=b){for(var c in id6nav.states){if(id6nav.states.hasOwnProperty(c)&&id6nav.states[c]===b){id6nav.state=b;
id6nav.$element.trigger("fixed",id6nav.state);
return true
}}}else{return false
}};
id6nav.fixHorizontalNavLinkHeight=function(){var b=0;
var f=0;
a("#navigation.horizontal ul#primary-navigation > li:visible").each(function(){b=Math.max(a(this).height(),b);
f+=a(this).width()
});
a("#navigation.horizontal ul#primary-navigation > li:visible").height(b);
var c=Math.ceil(f/a("#navigation.horizontal").width())
};
id6nav.repositionChildList=function(l){if(l.length==0){return
}l.width("auto").removeClass("column-nav").find("li").width("auto").css({"float":"none"});
var o=l.closest("li");
var p=o.position();
var q=p.top+o.height();
l.css({top:q});
var f=l.height();
var u=a("#container").height();
var k=a("#header").height();
var h=(a("body").scrollTop()>k)?0:k-a("body").scrollTop();
if(a(window).height()<u){u=a(window).height()
}if(f+q+h>u){var n=l.width();
var s=u-h-q;
var g=Math.ceil(f/s);
l.width(n*g).addClass("column-nav").find("li").width(n).css({"float":"left"})
}var c=o.position().left;
var b=l.width();
var r=a("#container").width();
if((c+b)>r){c=r-(b+2)
}l.css({left:c})
}
})(jQuery);
jQuery(function(o){id6nav.hoverClass=function(x,z){x.hover(function(){o(this).addClass(z)
},function(){o(this).removeClass(z)
})
};
id6nav.$element=o("#navigation");
if(id6nav.$element.is(".vertical")){id6nav.layout=id6nav.layouts.VERTICAL;
id6nav.navigationOffset=id6nav.$element.offset().top;
id6nav.navigationHeight=id6nav.$element.outerHeight(true);
id6nav.repositionNavigation=function(){var z=o(window).scrollTop();
var x=o("#navigation-wrapper").height()+id6nav.navigationOffset;
var A=o(window).height();
if(!o(document.body).hasClass("is-smallscreen")){if(z<=id6nav.navigationOffset||A<id6nav.navigationHeight){if(id6nav.changeState(id6nav.states.NORMAL)){id6nav.$element.removeClass("fixed").removeClass("fixed-bottom")
}}else{if(z>id6nav.navigationOffset&&(z+id6nav.navigationHeight)<x){if(id6nav.changeState(id6nav.states.FIXED)){id6nav.$element.removeClass("fixed-bottom").addClass("fixed")
}}else{if((z+id6nav.navigationHeight)>x){if(id6nav.changeState(id6nav.states.FIXEDBOTTOM)){id6nav.$element.removeClass("fixed").addClass("fixed-bottom")
}}}}}};
o("#navigation.vertical .description.l1 span[data-href]").closest(".description").click(function(z){if(o(z.target).closest("span[data-href]").length>0){var x=o(z.target).closest("span[data-href]").data("href");
if(z.shiftKey||z.ctrlKey||z.which==2){window.open(x)
}else{window.location=x
}z.preventDefault();
z.stopPropagation();
return false
}});
o("#navigation.vertical div.rendered-link-content").each(function(x,z){if(z.scrollWidth&&z.offsetWidth&&z.scrollWidth>z.offsetWidth){z.title=o(z).text()
}})
}else{if(id6nav.$element.is(".horizontal")){var s=o("#navigation.horizontal #secondary-navigation > li:not(.breadcrumb)");
if(s.length>0){var a=s.map(function(x,z){return o(z).position().left
});
var h=a.filter(function(x,z){return x!=0&&z===0
}).length>0;
if(h){var b=o('<div class="link-content arrow"><div class="title rendered-link-content"><div class="arrow" title="Show more"></div></div><div class="separator rendered-link-content"></div></div>');
var r=o('<li class="show-more-link rendered-link" />').append(b);
o("#navigation.horizontal #secondary-navigation").prepend(r);
r.find(".arrow .arrow").css("border-top-color",r.find(".arrow .arrow").css("color"));
var c=function(B){var C=/rgba*\((\d{1,3}),\s*(\d{1,3}),\s*(\d{1,3})\s*\)/;
var A=/^#([0-9A-Fa-f]{1})([0-9A-Fa-f]{1})([0-9A-Fa-f]{1})$|^#([0-9A-Fa-f]{2})([0-9A-Fa-f]{2})([0-9A-Fa-f]{2})$/;
var E=function(L,K,I,J){if(G/255>0.5){return"rgb("+Math.max(L-50,0)+","+Math.max(K-50,0)+","+Math.max(I-50,0)+")"
}else{return"rgb("+Math.min(L+50,255)+","+Math.min(K+50,255)+","+Math.min(I+50,255)+")"
}};
var H=B.match(C),z=B.match(A);
if(H){var x=parseInt(H[1],10),D=parseInt(H[2],10),F=parseInt(H[3],10);
var G=(Math.max(x,D,F)+Math.min(x,D,F))/2;
return E(x,D,F,G)
}else{if(z){if(z[1]&&z[1].length>0){var x=parseInt(z[1],16),D=parseInt(z[2],16),F=parseInt(z[3],16)
}else{var x=parseInt(z[4],16),D=parseInt(z[5],16),F=parseInt(z[6],16)
}var G=(Math.max(x,D,F)+Math.min(x,D,F))/2;
return E(x,D,F,G)
}else{return B
}}};
r.css("border-color",c(o("#secondary-navigation-wrapper").css("background-color")));
var f=r[0];
while(f.previousSibling&&f.previousSibling.nodeType===3){f.previousSibling.parentNode.removeChild(f.previousSibling)
}var n=false,l=s.filter(function(x,z){if(x!=0&&o(z).position().left===0){n=true
}return n
});
var q=o('<ul class="children-list" />').hide();
l.appendTo(q);
hoverClass(l,"hover");
q.find(".rendered-link-content").removeClass("rendered-link-content");
q.appendTo(b);
b.hoverIntent({timeout:400,over:function(z){try{z.stopPropagation()
}catch(x){}q.fadeIn("fast");
id6nav.repositionChildList(q)
},out:function(){q.fadeOut("fast")
}}).on("touchstart",function(z){try{z.stopPropagation()
}catch(x){}q.fadeIn("fast");
id6nav.repositionChildList(q)
});
o(document.body).on("touchstart",function(){q.fadeOut("fast")
})
}}o("#navigation.horizontal ul#primary-navigation > li > .link-content > a").closest("li").click(function(A){var z=o(A.target);
if(z.closest("span[data-href]").length>0){var x=o(A.target).closest("span[data-href]").data("href");
if(A.shiftKey||A.ctrlKey||A.which==2){window.open(x)
}else{window.location=x
}A.preventDefault();
A.stopPropagation();
return false
}if(z.closest("a").length>0){return true
}if(z.closest("li").is(".current-page")){A.preventDefault();
A.stopPropagation();
return false
}if(o(this).has(".link-content .title a")){var x=o(this).find(".link-content a").attr("href");
if(A.shiftKey||A.ctrlKey||A.which==2){window.open(x)
}else{window.location=x
}A.preventDefault();
A.stopPropagation();
return false
}});
id6nav.layout=id6nav.layouts.HORIZONTAL;
id6nav.navigationOffset=id6nav.$element.offset().top;
id6nav.navigationHeight=id6nav.$element.outerHeight(true);
id6nav.repositionNavigation=function(){var z=o(window).scrollTop();
var x=o("#navigation-wrapper").height()+id6nav.navigationOffset;
var A=o(window).height();
if(!o(document.body).hasClass("is-smallscreen")){if(z>id6nav.navigationOffset){if(id6nav.changeState(id6nav.states.FIXED)){id6nav.$element.removeClass("fixed-bottom").addClass("fixed");
if(o("#header-chrome-spacer").length==0){o("#navigation-wrapper").after('<div id="header-chrome-spacer" style="display:block; height:'+id6nav.navigationHeight+'px"></div>')
}}}else{if(z<=id6nav.navigationOffset){if(id6nav.changeState(id6nav.states.NORMAL)){id6nav.$element.removeClass("fixed");
o("#header-chrome-spacer").remove()
}}}}};
if("onhashchange" in window){o(window).on("hashchange",function(){if(location.hash.length){var z=location.hash.substr(1);
var x=o("#"+z).add("a[name='"+z+"']");
if(x.length){var A=x.first().offset().top-id6nav.navigationHeight;
o(window).scrollTop(A)
}}});
setTimeout(function(){o(window).trigger("hashchange")
},200)
}}}if(!id6nav.isiOS&&!id6nav.isAndroid&&!id6nav.isIE6or7){o(window).on("scroll resize",id6nav.repositionNavigation)
}var w=function(x,z){if(z){id6nav.changeState(id6nav.states.SMALLSCREEN)
}else{if(!id6nav.isiOS&&!id6nav.isAndroid&&!id6nav.isIE6or7&&id6nav.$element.length>0){id6nav.navigationOffset=id6nav.$element.offset().top;
id6nav.navigationHeight=id6nav.$element.outerHeight(true);
id6nav.repositionNavigation();
if(id6nav.layout==id6nav.layouts.VERTICAL){id6nav.fixHorizontalNavLinkHeight()
}}}o("#header-chrome-spacer").toggle(!z)
};
o(document.body).on("smallscreen",w);
if(o(document.body).hasClass("is-smallscreen")){w(null,true)
}if(id6nav.hasLayout()){if(!o(document.body).hasClass("site-root")){var k;
if(id6nav.layout==id6nav.layouts.VERTICAL){k=function(A){var B=o("#navigation li.current-page");
while(B.length>0){var x=o("<li />");
A.prepend(x);
if(B.hasClass("current-page")){x.addClass("current")
}var z=B.find("> a[data-page-url]").data("page-url");
var C=B.find("div.title").html().trim();
if(z){x.append(o("<a />").attr("href",z).html(C))
}else{x.html(C)
}x.prepend("&raquo; ");
B=B.parents("li.selected-section,li.breadcrumb")
}}
}else{k=function(A){var B=o("#secondary-navigation li.current-page");
while(B.length>0){var x=o("<li />");
A.prepend(x);
if(B.hasClass("current-page")){x.addClass("current")
}var z=B.find("a[data-page-url]").data("page-url");
var C=B.find("div.title").html().trim();
if(z){x.append(o("<a />").attr("href",z).html(C))
}else{x.html(C)
}x.find(".breadcrumb-icon").remove();
x.prepend("&raquo; ");
B=B.prev("li.breadcrumb")
}B=o("#navigation li.selected-section");
if(B.length>0){var x=o("<li />");
A.prepend(x);
if(B.hasClass("current-page")){x.addClass("current")
}var z=B.find("a[data-page-url]").data("page-url");
var C=B.find("div.title").html().trim();
if(z){x.append(o("<a />").attr("href",z).html(C))
}else{x.html(C)
}x.prepend("&raquo; ")
}}
}var g=function(B,D){if(D){if(o(".alternate-breadcrumbs").length==0){var z=o("<ol />");
k(z);
var E=o("#current-site-header").find("a").attr("href");
var C="Home";
var A=o('#search-container .index-section li[data-index-section="website"][data-url-prefix]').data("index-title");
if(A){C=A
}else{var x=o("#current-site-header").find("a,span").text();
if(x){C=x
}}z.prepend(o('<li class="home" />').append(o("<a />").attr("href",E).text(C)));
o("#content-wrapper").append(o('<div class="alternate-breadcrumbs bottom" />').append(z));
o("#content-wrapper").prepend(o('<div class="alternate-breadcrumbs top"><hr /></div>').prepend(z.clone()))
}}};
o(document.body).on("smallscreen",g);
if(o(document.body).hasClass("is-smallscreen")){g(null,true)
}}var p=function(A,B){if(B){if(o("#alternate-navigation").length==0){if(id6nav.layout==id6nav.layouts.VERTICAL){var z=o('<select name="mobilesubnav" />');
var C=o('<option value="" />').html(o("#current-site-header a,#current-site-header span").html());
z.append(C);
if(o("#current-site-header a").length>0){C.val(o("#current-site-header a").attr("href"))
}var x=/l(\d+)\s?/;
o("#navigation.vertical li").each(function(G,D){$li=o(D);
var I=$li.find(".title.rendered-link-content").html().trim();
var F=$li.find("a[data-page-url]").data("page-url");
var E=x.exec($li[0].className);
if(E&&E[1]){var J=parseInt(E[1]);
for(var G=0;
G<J;
G++){I="- "+I
}}var H=o('<option value="">'+I+"</option>");
if($li.hasClass("current-page")){H.attr("selected","selected")
}else{if(F){H.attr("value",F)
}}z.append(H)
});
z.change(function(E){var D=z.val();
if(D){window.location=D
}});
o("#content-wrapper").append(o('<div id="alternate-navigation" />').append(o('<form method="post" action />').append(z)))
}else{var z=o('<select name="mobilesubnav" />');
var C=o('<option value="" />').html(o("#current-site-header a,#current-site-header span").html());
z.append(C);
if(o("#current-site-header a").length>0){C.val(o("#current-site-header a").attr("href"))
}o("#primary-navigation > li").each(function(F,D){$li=o(D);
var H="- "+$li.find(".title.rendered-link-content").html().trim();
var E=$li.find("[data-page-url]").data("page-url");
var G=o('<option value="">'+H+"</option>");
if($li.hasClass("current-page")){G.attr("selected","selected")
}else{if(E){G.attr("value",E)
}}z.append(G);
if($li.hasClass("selected-section")){var I=1;
o("#secondary-navigation > li.breadcrumb").each(function(M,J){$li=o(J);
var K=$li.find(".title.rendered-link-content");
if(K.find("> span:not(.breadcrumb-icon)").length>0){K=K.find("> span:not(.breadcrumb-icon)")
}else{K=K.clone();
K.find(".breadcrumb-icon").remove()
}var O=K.html().trim();
var L=$li.find("[data-page-url]").data("page-url");
I++;
for(var M=0;
M<I;
M++){O="- "+O
}var N=o('<option value="">'+O+"</option>");
if($li.hasClass("current-page")){N.attr("selected","selected")
}else{if(L){N.attr("value",L)
}}z.append(N)
});
I++;
o("#secondary-navigation > li:not(.breadcrumb):not(.show-more-link), #secondary-navigation .show-more-link .children-list > li").each(function(L,J){$li=o(J);
var N=$li.find(".title").html().trim();
var K=$li.find("[data-page-url]").data("page-url");
for(var L=0;
L<I;
L++){N="- "+N
}var M=o('<option value="">'+N+"</option>");
if($li.hasClass("current-page")){M.attr("selected","selected")
}else{if(K){M.attr("value",K)
}}z.append(M)
})
}})
}z.change(function(E){var D=z.val();
if(D){window.location=D
}});
o("#content-wrapper").append(o('<div id="alternate-navigation" />').append(o('<form method="post" action />').append(z)))
}}};
o(document.body).on("smallscreen",p);
if(o(document.body).hasClass("is-smallscreen")){p(null,true)
}}var u=function(z,A){if(A&&o("#masthead .smallscreen-anchor-links").length==0){var x=(o(document.body).is(".site-root"))?"#alternate-navigation":"#alternate-search";
o("#masthead").append(o('<div class="smallscreen-anchor-links"><a href="'+window.location.pathname+x+'">Skip to navigation</a></div>'));
o("#footer > .content").prepend(o('<div class="smallscreen-anchor-links"><a href="'+window.location.pathname+'#masthead">Back to top</a></div>'))
}};
o(document.body).on("smallscreen",u);
if(o(document.body).hasClass("is-smallscreen")){u(null,true)
}});