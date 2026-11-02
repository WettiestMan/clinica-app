const {test} = require('node:test');
const assert = require('node:assert/strict');
const vm = require('node:vm');
const fs = require('node:fs');
const path = require('node:path');
const fuente = fs.readFileSync(path.join(__dirname, '../../main/resources/static/js/notificaciones-medico.js'), 'utf8');
class Nodo {
    constructor() {this.children=[];this.dataset={};this.handlers={};this.hidden=false;this.textContent='';}
    append(...nodos) {nodos.forEach(n=>{n.parent=this;this.children.push(n);});}
    replaceChildren() {this.children=[];}
    remove() {this.parent.children=this.parent.children.filter(n=>n!==this);}
    setAttribute(k,v) {this[k]=v;}
    addEventListener(k,fn) {this.handlers[k]=fn;}
}
const turno = () => ({medicoId:7, pendientes:1, avisos:[{id:20,mensaje:'Cupo liberado <script>texto</script>',fecha:'2026-11-02T10:00:00'}]});
async function navegador(almacen=new Map(), reloj={ahora:0}) {
    const region=new Nodo();region.dataset={url:'/medico/notificaciones/datos',centro:'/medico/notificaciones'};
    const contador=new Nodo();const intervalos=[];const handlers={};let datos=turno();
    const contexto={document:{readyState:'complete',hidden:false,getElementById:id=>id==='sc-toast-region'?region:contador,createElement:()=>new Nodo(),addEventListener:(k,v)=>{handlers[k]=v;}},window:{addEventListener:(k,v)=>{handlers[k]=v;}},localStorage:{getItem:k=>almacen.get(k)||null,setItem:(k,v)=>almacen.set(k,v)},Date:{now:()=>reloj.ahora},setInterval:(fn,ms)=>intervalos.push({fn,ms}),fetch:async()=>({ok:true,redirected:false,headers:{get:()=> 'application/json'},json:async()=>datos})};
    vm.runInNewContext(fuente,contexto);await new Promise(setImmediate);
    return {region,contador,handlers,intervalos,datos:v=>{datos=v;},avanzar:ms=>{reloj.ahora+=ms;intervalos.find(i=>i.ms===1000).fn();},poll:async()=>{await intervalos.find(i=>i.ms===10000).fn();}};
}
test('desaparece al minuto y el contador sigue sin leer',async()=>{
    const n=await navegador();assert.equal(n.region.children.length,1);assert.equal(n.contador.textContent,'1');
    n.avanzar(59999);assert.equal(n.region.children.length,1);n.avanzar(1);assert.equal(n.region.children.length,0);await n.poll();assert.equal(n.region.children.length,0);assert.equal(n.contador.textContent,'1');
});
test('cerrar se sincroniza con otra pestaña y una nueva pantalla',async()=>{
    const almacen=new Map(),reloj={ahora:0};const a=await navegador(almacen,reloj),b=await navegador(almacen,reloj);
    a.region.children[0].children[2].handlers.click();assert.equal(a.region.children.length,0);
    b.handlers.storage({key:'saludcitas.aviso.7.20'});assert.equal(b.region.children.length,0);
    const c=await navegador(almacen,reloj);assert.equal(c.region.children.length,0);
});
test('navegar conserva el plazo original de un minuto',async()=>{
    const almacen=new Map(),reloj={ahora:0};await navegador(almacen,reloj);reloj.ahora=45000;const b=await navegador(almacen,reloj);assert.equal(b.region.children.length,1);b.avanzar(15000);assert.equal(b.region.children.length,0);
});
test('leer quita la ventana y actualiza contador sin ejecutar HTML',async()=>{
    const n=await navegador();assert.equal(n.region.children[0].children[1].textContent,'Cupo liberado <script>texto</script>');
    n.datos({medicoId:7,pendientes:0,avisos:[]});await n.poll();assert.equal(n.region.children.length,0);assert.equal(n.contador.hidden,true);
});
test('un aviso nuevo aparece después de vencer el anterior',async()=>{
    const n=await navegador();n.avanzar(60000);n.datos({medicoId:7,pendientes:2,avisos:[{id:21,mensaje:'Nueva asignación'},...turno().avisos]});await n.poll();assert.equal(n.region.children.length,1);assert.equal(n.region.children[0].dataset.id,'21');assert.equal(n.contador.textContent,'2');
});
