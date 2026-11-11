(() => {
    const iniciar = () => {
        const form = document.getElementById('formAgenda');
        if (!form) return;
        const fecha = document.getElementById('fecha');
        const contador = document.getElementById('contador');
        const fallo = document.getElementById('fallo');
        const citas = document.getElementById('citas');
        const partes = new Intl.DateTimeFormat('en-US', {timeZone:'America/Lima', year:'numeric', month:'2-digit', day:'2-digit'}).formatToParts(new Date());
        const valor = tipo => partes.find(p => p.type === tipo).value;
        const seleccionada = new URLSearchParams(window.location.search).get('fecha');
        fecha.value = /^\d{4}-\d{2}-\d{2}$/.test(seleccionada || '') ? seleccionada : `${valor('year')}-${valor('month')}-${valor('day')}`;
        let solicitud = 0;
        function celda(fila, texto) { const td = document.createElement('td'); td.textContent = texto; fila.append(td); return td; }
        function renderizar(datos) {
            citas.replaceChildren();
            contador.textContent = `${datos.contador} citas`;
            if (!datos.citas.length) {
                const p = document.createElement('p'); p.className = 'sc-empty'; p.textContent = 'No tienes citas registradas para esta fecha.'; citas.append(p);
            } else {
                const tabla = document.createElement('table'); tabla.className = 'sc-table';
                const cabeza = document.createElement('thead'), tr = document.createElement('tr');
                ['Hora', 'Paciente', 'Estado', 'Acciones'].forEach(texto => { const th = document.createElement('th'); th.scope='col'; th.textContent=texto; tr.append(th); });
                cabeza.append(tr);tabla.append(cabeza);
                const cuerpo = document.createElement('tbody');
                datos.citas.forEach(cita => {
                    const fila = document.createElement('tr'); celda(fila, cita.hora);celda(fila,cita.paciente);
                    const estado = document.createElement('span'); estado.className = 'sc-badge'; estado.textContent = cita.estado;
                    celda(fila,'').append(estado);
                    const detalle = document.createElement('a'); detalle.className = 'sc-btn sc-btn-small sc-btn-secondary';detalle.textContent = 'Ver detalle';
                    detalle.href = form.dataset.detalleBase + encodeURIComponent(cita.id);
                    celda(fila,'').append(detalle);cuerpo.append(fila);
                });
                tabla.append(cuerpo); citas.append(tabla);
            }
        }
        async function cargar() {
            if (!fecha.value) return;
            const actual = ++solicitud;
            fallo.hidden = true;contador.textContent = 'Consultando citas…';citas.setAttribute('aria-busy','true');
            try {
                const url = new URL(form.dataset.url,window.location.href); url.searchParams.set('fecha',fecha.value);
                const response = await fetch(url,{cache:'no-store',headers:{Accept:'application/json'}});
                if (!response.ok || !(response.headers.get('content-type') || '').includes('application/json')) throw new Error('agenda');
                const datos = await response.json();
                if (actual !== solicitud) return;
                if (!Array.isArray(datos.citas) || !Array.isArray(datos.avisos)) throw new Error('datos');
                renderizar(datos);
            } catch (_) {
                if (actual !== solicitud) return;
                citas.replaceChildren();contador.textContent='Sin datos';
                fallo.textContent='No se pudo cargar tu agenda. Revisa tu sesión y vuelve a consultar.';fallo.hidden=false;
            } finally { if (actual===solicitud)citas.removeAttribute('aria-busy'); }
        }
        form.addEventListener('submit',event => {event.preventDefault();cargar();});
        fecha.addEventListener('change',cargar);cargar();
    };
    if (document.readyState==='loading')document.addEventListener('DOMContentLoaded',iniciar,{once:true});else iniciar();
})();
