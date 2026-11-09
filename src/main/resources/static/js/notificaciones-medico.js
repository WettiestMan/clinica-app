(() => {
    'use strict';
    const iniciar = () => {
        const region = document.getElementById('sc-toast-region');
        if (!region) return;
        const contador = document.getElementById('sc-notif-count');
        const memoria = new Map();
        let bandeja = null, cargando = false;
        const DURACION = 60000;
        function clave(id) { return `saludcitas.aviso.${bandeja.medicoId}.${id}`; }
        function leer(id) {
            const key = clave(id);
            try {
                const valor = localStorage.getItem(key);
                if (valor) {
                    const estado = JSON.parse(valor);
                    if (Number.isFinite(estado.hasta) && typeof estado.cerrada === 'boolean') return estado;
                }
            } catch (_) { /* El modo privado puede impedir almacenamiento. */ }
            return memoria.get(key) || null;
        }
        function guardar(id, estado) {
            const key = clave(id);memoria.set(key, estado);
            try { localStorage.setItem(key, JSON.stringify(estado)); } catch (_) { /* Conserva el estado en memoria. */ }
        }
        function cerrar(id) {
            const estado = leer(id);
            guardar(id, {hasta: estado ? estado.hasta : Date.now(), cerrada: true});
            renderizar();
        }
        function renderizar() {
            if (!bandeja) return;
            const ahora = Date.now();
            const visibles = [];
            // Todos los avisos comparten su plazo entre pantallas y pestañas del mismo navegador.
            bandeja.avisos.slice().reverse().forEach(aviso => {
                let estado = leer(aviso.id);
                if (!estado) {
                    estado = {hasta: ahora + DURACION, cerrada: false};
                    guardar(aviso.id, estado);
                }
                if (!estado.cerrada && estado.hasta > ahora) visibles.push(aviso);
            });
            const ids = new Set(visibles.map(a => String(a.id)));
            Array.from(region.children).forEach(nodo => { if (!ids.has(nodo.dataset.id)) nodo.remove(); });
            visibles.forEach(aviso => {
                if (Array.from(region.children).some(n => n.dataset.id === String(aviso.id))) return;
                const tarjeta = document.createElement('article');tarjeta.className = 'sc-toast';tarjeta.dataset.id = String(aviso.id);
                const titulo = document.createElement('h3');titulo.textContent = 'Nueva notificación';
                const texto = document.createElement('p');texto.textContent = aviso.mensaje;
                const boton = document.createElement('button');boton.type = 'button';boton.className = 'sc-toast-close';
                boton.textContent = '×';boton.setAttribute('aria-label', 'Cerrar notificación');boton.addEventListener('click', () => cerrar(aviso.id));
                const enlace = document.createElement('a');enlace.href = region.dataset.centro;enlace.textContent = 'Ver notificaciones';
                tarjeta.append(titulo, texto, boton, enlace);region.append(tarjeta);
            });
        }
        async function cargar() {
            if (cargando) return;
            cargando = true;
            try {
                const respuesta = await fetch(region.dataset.url, {cache: 'no-store', headers: {Accept: 'application/json'}});
                if (!respuesta.ok || respuesta.redirected || !(respuesta.headers.get('content-type') || '').includes('application/json')) {
                    bandeja = null;region.replaceChildren();if (contador) contador.hidden = true;return;
                }
                const datos = await respuesta.json();
                if (!Number.isSafeInteger(datos.medicoId) || !Number.isFinite(datos.pendientes) || !Array.isArray(datos.avisos)) return;
                if (bandeja && bandeja.medicoId !== datos.medicoId) region.replaceChildren();
                bandeja = datos;
                if (contador) {
                    contador.textContent = String(datos.pendientes);contador.hidden = datos.pendientes === 0;
                    contador.setAttribute('aria-label', `${datos.pendientes} notificaciones sin leer`);
                }
                renderizar();
            } catch (_) { /* Un fallo temporal no modifica el historial ni el estado leído. */ }
            finally { cargando = false; }
        }
        window.addEventListener('storage', evento => {
            if (!evento.key || (bandeja && evento.key.startsWith(`saludcitas.aviso.${bandeja.medicoId}.`))) renderizar();
        });
        window.addEventListener('focus', () => {renderizar();cargar();});
        document.addEventListener('visibilitychange', () => {if (!document.hidden) {renderizar();cargar();}});
        setInterval(renderizar, 1000);
        setInterval(cargar, 10000);
        cargar();
    };
    if (document.readyState === 'loading') document.addEventListener('DOMContentLoaded', iniciar, {once: true});
    else iniciar();
})();
