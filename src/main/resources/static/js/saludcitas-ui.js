(() => {
    const iniciar = () => {
        const boton = document.getElementById('userMenuBtn');
        const menu = document.getElementById('userDropdown');
        const modal = document.getElementById('modalLogout');
        const cancelar = document.getElementById('btnCancelarLogout');
        const sidebar = document.getElementById('sidebarPrincipal');
        const abrirSidebar = document.querySelector('[data-open-sidebar]');
        const fondoSidebar = document.querySelector('[data-close-sidebar]');
        let origenModal = null;
        const cerrarMenu = () => {
            if (menu) menu.hidden = true;
            if (boton) boton.setAttribute('aria-expanded', 'false');
        };
        const cambiarSidebar = (abierto) => {
            if (!sidebar) return;
            sidebar.classList.toggle('is-open', abierto);
            fondoSidebar?.classList.toggle('is-open', abierto);
            abrirSidebar?.setAttribute('aria-expanded', String(abierto));
            const movil = window.matchMedia('(max-width: 767px)').matches;
            if (movil) sidebar.inert = !abierto;
        };
        const cerrarModal = () => {
            if (modal) modal.hidden = true;
            if (sidebar?.contains(origenModal) && window.matchMedia('(max-width: 767px)').matches) abrirSidebar?.focus();
            else origenModal?.focus();
        };
        boton?.addEventListener('click', () => {
            if (!menu) return;
            menu.hidden = !menu.hidden;
            boton.setAttribute('aria-expanded', String(!menu.hidden));
        });
        document.addEventListener('click', (event) => {
            if (!document.getElementById('userMenuContainer')?.contains(event.target)) cerrarMenu();
        });
        document.querySelectorAll('.abrir-modal-logout').forEach((trigger) => {
            trigger.addEventListener('click', () => {
                if (!modal) return;
                origenModal = trigger;
                cerrarMenu();
                cambiarSidebar(false);
                modal.hidden = false;
                cancelar?.focus();
            });
        });
        cancelar?.addEventListener('click', cerrarModal);
        modal?.addEventListener('click', (event) => { if (event.target === modal) cerrarModal(); });
        abrirSidebar?.addEventListener('click', () => {
            const abrir = !sidebar?.classList.contains('is-open');
            cambiarSidebar(abrir);
            if (abrir) sidebar?.querySelector('a')?.focus();
        });
        fondoSidebar?.addEventListener('click', () => { cambiarSidebar(false); abrirSidebar?.focus(); });
        sidebar?.querySelectorAll('a').forEach((a) => a.addEventListener('click', () => cambiarSidebar(false)));
        window.addEventListener('resize', () => {
            if (!window.matchMedia('(max-width: 767px)').matches) { cambiarSidebar(false); if (sidebar) sidebar.inert = false; }
            else if (sidebar) sidebar.inert = !sidebar.classList.contains('is-open');
        });
        cambiarSidebar(false);
        document.addEventListener('keydown', (event) => {
            if (event.key === 'Escape') {
                if (modal && !modal.hidden) cerrarModal();
                cerrarMenu(); cambiarSidebar(false);
            }
            if (event.key === 'Tab' && modal && !modal.hidden) {
                const controles = [...modal.querySelectorAll('button, a[href], input')].filter(e => e.getClientRects().length);
                const primero = controles[0], ultimo = controles[controles.length - 1];
                if (event.shiftKey && document.activeElement === primero) { event.preventDefault(); ultimo?.focus(); }
                else if (!event.shiftKey && document.activeElement === ultimo) { event.preventDefault(); primero?.focus(); }
            }
        });
    };
    if (document.readyState === 'loading') document.addEventListener('DOMContentLoaded', iniciar, { once: true });
    else iniciar();
})();
