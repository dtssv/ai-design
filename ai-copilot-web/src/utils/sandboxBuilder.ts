/**
 * 沙箱预览 HTML 构建器
 *
 * 支持四种项目类型：
 * 1. 纯 HTML/CSS/JS
 * 2. React (TSX/JSX)
 * 3. Vue 3 (SFC .vue)
 * 4. TypeScript (纯 TS/JS)
 */
import type { CodeFile } from '@/stores/workspace';

// ======================== 项目类型检测 ========================

type ProjectType = 'html' | 'react' | 'vue' | 'typescript';

function detectProjectType(files: CodeFile[]): ProjectType {
    if (files.some(f => f.path.endsWith('.vue'))) return 'vue';
    if (files.some(f => /\.(tsx|jsx)$/.test(f.path))) return 'react';
    if (files.some(f => /import\s+.*from\s+['"]react/m.test(f.content))) return 'react';
    if (files.some(f => /\.ts$/.test(f.path) && !f.path.endsWith('.d.ts')) && !files.some(f => f.path.endsWith('.html'))) return 'typescript';
    return 'html';
}

// ======================== 公共工具 ========================

function makeFileMap(files: CodeFile[]) {
    const byPath = new Map<string, CodeFile>();
    const byName = new Map<string, CodeFile>();
    for (const f of files) {
        byPath.set(f.path, f);
        byName.set(f.path.split('/').pop() || '', f);
    }
    return { byPath, byName };
}

function resolveLocalRef(ref: string, maps: ReturnType<typeof makeFileMap>): CodeFile | undefined {
    if (/^https?:\/\//.test(ref)) return undefined;
    const normalized = ref.replace(/^\.\//, '').replace(/^\//, '');
    for (const [path, file] of maps.byPath) {
        if (path === normalized || path.endsWith('/' + normalized) || path === 'src/' + normalized) return file;
    }
    return maps.byName.get(ref.split('/').pop() || '');
}

function buildCssInject(files: CodeFile[]): string {
    return files.filter(f => f.path.endsWith('.css')).map(f => `<style data-path="${f.path}">\n${f.content}\n</style>`).join('\n');
}

function findEntry(files: CodeFile[], priorities: string[]): CodeFile | undefined {
    for (const name of priorities) {
        const found = files.find(f => f.path === name || f.path === `src/${name}` || f.path.endsWith(`/${name}`));
        if (found) return found;
    }
    return files[0];
}

// ======================== 1. 纯 HTML ========================

function buildHtmlSandbox(files: CodeFile[]): string {
    const maps = makeFileMap(files);
    const cssFiles = files.filter(f => f.path.endsWith('.css'));
    const jsFiles = files.filter(f => (f.path.endsWith('.js') || f.path.endsWith('.ts')) && !f.path.endsWith('.d.ts'));
    const htmlFile = files.find(f => f.path.endsWith('index.html')) || files.find(f => f.path.endsWith('.html'));

    if (!htmlFile && cssFiles.length === 0 && jsFiles.length === 0) return '';

    const inlinedCss = new Set<string>();
    const inlinedJs = new Set<string>();

    const processHtml = (html: string): string => {
        html = html.replace(/<link\s+[^>]*?href=["']([^"']+)["'][^>]*?>/gi, (m, href) => {
            if (!/rel=["']stylesheet["']/i.test(m) || /^https?:\/\//.test(href)) return m;
            const file = resolveLocalRef(href, maps);
            if (file) { inlinedCss.add(file.path); return `<style>/* ${file.path} */\n${file.content}</style>`; }
            return `<!-- removed: ${href} -->`;
        });
        html = html.replace(/<script\s+[^>]*?src=["']([^"']+)["'][^>]*?>\s*<\/script>/gi, (m, src) => {
            if (/^https?:\/\//.test(src)) return m;
            const file = resolveLocalRef(src, maps);
            if (file) { inlinedJs.add(file.path); return `<script>/* ${file.path} */\n${file.content}\n<\/script>`; }
            return `<!-- removed: ${src} -->`;
        });
        const remaining = cssFiles.filter(f => !inlinedCss.has(f.path));
        if (remaining.length) {
            const s = remaining.map(f => `<style>/* ${f.path} */\n${f.content}</style>`).join('\n');
            html = html.includes('</head>') ? html.replace('</head>', s + '\n</head>') : s + '\n' + html;
        }
        const remainJs = jsFiles.filter(f => !inlinedJs.has(f.path));
        if (remainJs.length) {
            const s = remainJs.map(f => `<script>/* ${f.path} */\n${f.content}\n<\/script>`).join('\n');
            html = html.includes('</body>') ? html.replace('</body>', s + '\n</body>') : html + '\n' + s;
        }
        return html;
    };

    if (htmlFile) return processHtml(htmlFile.content);
    return `<!DOCTYPE html><html lang="zh-CN"><head><meta charset="UTF-8"><meta name="viewport" content="width=device-width,initial-scale=1.0">
${cssFiles.map(f => `<style>${f.content}</style>`).join('\n')}
</head><body><div id="root"></div>
${jsFiles.map(f => `<script>${f.content}<\/script>`).join('\n')}
</body></html>`;
}

// ======================== 虚拟模块运行时 ========================

/**
 * 生成嵌入 iframe 的模块运行时脚本。
 * 注意：这段代码是作为 HTML 字符串注入 srcdoc 的，
 * 所有正则用 new RegExp() 避免模板字符串转义问题。
 */
function buildModuleRuntime(
    moduleFiles: CodeFile[],
    entryPath: string,
    builtinMapEntries: [string, string][],
    extras: { autoMountReact?: boolean; autoMountVue?: boolean } = {},
): string {
    const modulesJson = JSON.stringify(moduleFiles.map(f => ({ path: f.path, content: f.content })));
    const builtinLines = builtinMapEntries.map(([k, v]) => `B['${k}']=${v};`).join('');

    let mountScript = '';
    if (extras.autoMountReact) {
        mountScript = `
    var _exp = requireModule(ENTRY);
    var _Comp = _exp && (_exp.default || _exp);
    if(typeof _Comp==='function'){
      var _root=document.getElementById('root')||document.getElementById('app');
      if(!_root){_root=document.createElement('div');_root.id='root';document.body.appendChild(_root);}
      if(window.ReactDOM&&window.ReactDOM.createRoot)window.ReactDOM.createRoot(_root).render(window.React.createElement(_Comp));
      else if(window.ReactDOM)window.ReactDOM.render(window.React.createElement(_Comp),_root);
    } else { requireModule(ENTRY); }`;
    } else if (extras.autoMountVue) {
        mountScript = `
    var _exp = requireModule(ENTRY);
    var _Comp = _exp && (_exp.default || _exp);
    if(_Comp && window.Vue){
      var _root=document.getElementById('app')||document.getElementById('root');
      if(!_root){_root=document.createElement('div');_root.id='app';document.body.appendChild(_root);}
      window.Vue.createApp(_Comp).mount(_root);
    } else { requireModule(ENTRY); }`;
    } else {
        mountScript = `requireModule(ENTRY);`;
    }

    // 用字符串拼接避免 TS 模板字符串对正则/转义的干扰
    const lines: string[] = [];
    lines.push('<script>');
    lines.push('(function(){');
    lines.push('  var MODULES=' + modulesJson + ';');
    lines.push('  var ENTRY=' + JSON.stringify(entryPath) + ';');
    lines.push('  var mMap={};MODULES.forEach(function(m){mMap[m.path]=m;});');
    lines.push('  var B={};' + builtinLines);
    lines.push('');
    lines.push('  function showErr(e){');
    lines.push('    var b=document.getElementById("__err");');
    lines.push('    if(!b){b=document.createElement("div");b.id="__err";b.style.cssText="position:fixed;top:0;left:0;right:0;background:#fee2e2;color:#b91c1c;padding:12px 16px;font:12px monospace;z-index:99999;white-space:pre-wrap;max-height:50vh;overflow:auto;border-bottom:2px solid #b91c1c";document.body.prepend(b);}');
    lines.push('    b.textContent="预览错误: "+(e&&e.stack?e.stack:String(e));');
    lines.push('    console.error(e);');
    lines.push('  }');
    lines.push('  window.onerror=function(_m,_s,_l,_c,e){showErr(e||_m);};');
    lines.push('  window.onunhandledrejection=function(ev){showErr(ev.reason);};');
    lines.push('');
    lines.push('  function resolvePath(from,ref){');
    lines.push('    if(!ref)return null;');
    lines.push('    if(B[ref]!==undefined)return "__B__"+ref;');
    // 处理 @/ 别名
    lines.push('    if(ref.charAt(0)==="@"&&ref.charAt(1)==="/"){ref="src/"+ref.slice(2);}');
    // 非相对路径且非别名转换后的路径 → 不支持
    lines.push('    if(ref.charAt(0)!=="."&&ref.charAt(0)!=="/"&&ref.indexOf("src/")!==0)return null;');
    lines.push('    var base=from.split("/").slice(0,-1);');
    lines.push('    ref.split("/").forEach(function(p){if(p===".."&&base.length)base.pop();else if(p&&p!==".")base.push(p);});');
    lines.push('    var c=base.join("/");');
    lines.push('    var exts=[".tsx",".ts",".jsx",".js",".vue",".css",".module.css",".json"];');
    lines.push('    if(mMap[c])return c;');
    lines.push('    for(var i=0;i<exts.length;i++){if(mMap[c+exts[i]])return c+exts[i];}');
    lines.push('    for(var i=0;i<exts.length;i++){if(mMap[c+"/index"+exts[i]])return c+"/index"+exts[i];}');
    lines.push('    return c;');
    lines.push('  }');
    lines.push('');
    lines.push('  var cache={};');
    lines.push('  function requireModule(path){');
    lines.push('    if(path.indexOf("__B__")===0){var k=path.slice(5);return B[k];}');
    lines.push('    if(cache[path])return cache[path].exports;');
    lines.push('    var file=mMap[path];');
    // CSS → 空模块
    lines.push('    if(!file||path.match(/\\.css$/)){cache[path]={exports:{}};return {};}');
    // JSON
    lines.push('    if(path.match(/\\.json$/)){try{var j=JSON.parse(file.content);cache[path]={exports:j};return j;}catch(e){return {};}}');
    lines.push('');
    lines.push('    var mod={exports:{}};cache[path]=mod;');
    lines.push('');
    // Vue SFC
    lines.push('    if(path.match(/\\.vue$/)){compileVue(file,mod,path);return mod.exports;}');
    lines.push('');
    // Babel 编译
    lines.push('    var presets=[];');
    lines.push('    if(path.match(/\\.(tsx|ts)$/))presets.push("typescript");');
    lines.push('    if(path.match(/\\.(tsx|jsx|js)$/))presets.push("react");');
    lines.push('    if(presets.length===0)presets.push("env");');
    lines.push('    var code;');
    lines.push('    try{');
    lines.push('      code=Babel.transform(file.content,{presets:presets,plugins:[["transform-modules-commonjs",{strictMode:false}]],filename:path,sourceType:"module"}).code;');
    lines.push('    }catch(e){showErr("编译失败 ["+path+"]: "+e.message);throw e;}');
    lines.push('');
    lines.push('    var localReq=function(ref){');
    lines.push('      var r=resolvePath(path,ref);');
    lines.push('      if(r===null){showErr("沙箱不支持第三方模块: "+ref);return {};}');
    lines.push('      return requireModule(r);');
    lines.push('    };');
    lines.push('    try{');
    lines.push('      (new Function("require","module","exports","React",code))(localReq,mod,mod.exports,window.React||undefined);');
    lines.push('    }catch(e){showErr("执行失败 ["+path+"]: "+e.message);throw e;}');
    lines.push('    return mod.exports;');
    lines.push('  }');
    lines.push('');
    // Vue SFC 编译函数
    lines.push('  function compileVue(file,mod,path){');
    lines.push('    if(!window.VueCompilerSFC){showErr("Vue SFC 编译器未加载");return;}');
    lines.push('    try{');
    lines.push('      var parsed=window.VueCompilerSFC.parse(file.content,{filename:path});');
    lines.push('      var desc=parsed.descriptor;');
    lines.push('      var templateCode="";');
    lines.push('      if(desc.template){var tpl=window.VueCompilerSFC.compileTemplate({source:desc.template.content,filename:path,id:"scope"});templateCode=tpl.code;}');
    lines.push('      var scriptContent="";');
    lines.push('      if(desc.scriptSetup){var ss=window.VueCompilerSFC.compileScript(desc,{id:"scope"});scriptContent=ss.content;}');
    lines.push('      else if(desc.script){scriptContent=desc.script.content;}');
    lines.push('      if(desc.styles&&desc.styles.length){desc.styles.forEach(function(s,i){var id="__vs_"+path.replace(/[^a-zA-Z0-9]/g,"_")+"_"+i;if(!document.getElementById(id)){var el=document.createElement("style");el.id=id;el.textContent=s.content;document.head.appendChild(el);}});}');
    lines.push('      var jsCode=Babel.transform(scriptContent,{presets:["typescript"],plugins:[["transform-modules-commonjs",{strictMode:false}]],filename:path,sourceType:"module"}).code;');
    lines.push('      var renderCode=Babel.transform(templateCode,{presets:[],plugins:[["transform-modules-commonjs",{strictMode:false}]],filename:path+"__tpl",sourceType:"module"}).code;');
    lines.push('      var localReq=function(ref){var r=resolvePath(path,ref);if(r===null)return {};return requireModule(r);};');
    lines.push('      (new Function("require","module","exports",jsCode))(localReq,mod,mod.exports);');
    lines.push('      var renderMod={exports:{}};');
    lines.push('      (new Function("require","module","exports",renderCode))(localReq,renderMod,renderMod.exports);');
    lines.push('      var comp=mod.exports.default||mod.exports;');
    lines.push('      if(renderMod.exports.render)comp.render=renderMod.exports.render;');
    lines.push('      if(!mod.exports.default)mod.exports.default=comp;');
    lines.push('    }catch(e){showErr("Vue SFC编译失败["+path+"]: "+e.message);}');
    lines.push('  }');
    lines.push('');
    // 启动
    lines.push('  try{');
    lines.push('    if(!ENTRY){showErr("未找到入口文件");return;}');
    lines.push('    ' + mountScript);
    lines.push('  }catch(e){showErr(e);}');
    lines.push('})();');
    lines.push('<\/script>');

    return lines.join('\n');
}

// ======================== 2. React ========================

function buildReactSandbox(files: CodeFile[]): string {
    const moduleFiles = files.filter(f => /\.(tsx|jsx|ts|js)$/.test(f.path) && !f.path.endsWith('.d.ts'));
    const entry = findEntry(moduleFiles, ['main.tsx', 'index.tsx', 'main.jsx', 'index.jsx', 'App.tsx', 'App.jsx']);
    const builtins: [string, string][] = [
        ['react', 'window.React'],
        ['react-dom', 'Object.assign({default:window.ReactDOM},window.ReactDOM)'],
        ['react-dom/client', 'Object.assign({default:window.ReactDOM},window.ReactDOM)'],
    ];
    // 如果入口本身是 App 组件（没有 main/index 包装），则自动挂载
    const isAppEntry = entry ? /App\.(tsx|jsx)$/.test(entry.path) : false;

    return [
        '<!DOCTYPE html><html lang="zh-CN"><head><meta charset="UTF-8"><meta name="viewport" content="width=device-width,initial-scale=1.0">',
        '<script crossorigin src="https://unpkg.com/react@18/umd/react.development.js"><\/script>',
        '<script crossorigin src="https://unpkg.com/react-dom@18/umd/react-dom.development.js"><\/script>',
        '<script src="https://unpkg.com/@babel/standalone/babel.min.js"><\/script>',
        '<script src="https://cdn.tailwindcss.com"><\/script>',
        buildCssInject(files),
        '</head><body>',
        '<div id="root"></div>',
        buildModuleRuntime(moduleFiles, entry?.path || '', builtins, { autoMountReact: isAppEntry }),
        '</body></html>',
    ].join('\n');
}

// ======================== 3. Vue ========================

function buildVueSandbox(files: CodeFile[]): string {
    const moduleFiles = files.filter(f => /\.(vue|tsx|jsx|ts|js)$/.test(f.path) && !f.path.endsWith('.d.ts'));
    const entry = findEntry(moduleFiles, ['main.ts', 'main.js', 'main.tsx', 'App.vue', 'app.vue', 'index.vue']);
    const builtins: [string, string][] = [['vue', 'window.Vue']];
    const isVueEntry = entry ? /\.vue$/.test(entry.path) : false;

    return [
        '<!DOCTYPE html><html lang="zh-CN"><head><meta charset="UTF-8"><meta name="viewport" content="width=device-width,initial-scale=1.0">',
        '<script src="https://unpkg.com/vue@3/dist/vue.global.js"><\/script>',
        '<script src="https://unpkg.com/@vue/compiler-sfc@3/dist/compiler-sfc.global.js"><\/script>',
        '<script src="https://unpkg.com/@babel/standalone/babel.min.js"><\/script>',
        '<script src="https://cdn.tailwindcss.com"><\/script>',
        buildCssInject(files),
        '</head><body>',
        '<div id="app"></div>',
        buildModuleRuntime(moduleFiles, entry?.path || '', builtins, { autoMountVue: isVueEntry }),
        '</body></html>',
    ].join('\n');
}

// ======================== 4. TypeScript ========================

function buildTypeScriptSandbox(files: CodeFile[]): string {
    const moduleFiles = files.filter(f => /\.(ts|js)$/.test(f.path) && !f.path.endsWith('.d.ts'));
    const htmlFile = files.find(f => f.path.endsWith('index.html')) || files.find(f => f.path.endsWith('.html'));
    const entry = findEntry(moduleFiles, ['main.ts', 'index.ts', 'main.js', 'index.js']);

    if (htmlFile) {
        let html = buildHtmlSandbox(files);
        if (!html.includes('babel')) {
            const babelTag = '<script src="https://unpkg.com/@babel/standalone/babel.min.js"><\/script>';
            html = html.includes('</head>') ? html.replace('</head>', babelTag + '\n</head>') : babelTag + '\n' + html;
        }
        return html;
    }

    return [
        '<!DOCTYPE html><html lang="zh-CN"><head><meta charset="UTF-8"><meta name="viewport" content="width=device-width,initial-scale=1.0">',
        '<script src="https://unpkg.com/@babel/standalone/babel.min.js"><\/script>',
        buildCssInject(files),
        '</head><body>',
        '<div id="root"></div>',
        buildModuleRuntime(moduleFiles, entry?.path || '', [], {}),
        '</body></html>',
    ].join('\n');
}

// ======================== 统一入口 ========================

export function buildSandboxHtml(files: CodeFile[]): string {
    if (!files || files.length === 0) return '';
    const type = detectProjectType(files);
    switch (type) {
        case 'react': return buildReactSandbox(files);
        case 'vue': return buildVueSandbox(files);
        case 'typescript': return buildTypeScriptSandbox(files);
        default: return buildHtmlSandbox(files);
    }
}