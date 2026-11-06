package org.example.proyectoso.models;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class ProcesoTest {

    @Test
    void estadoInicial() {
        Proceso p = new Proceso("P", 100, 64, 5);
        assertEquals(EstadoProceso.NUEVO, p.getEstado());
        assertEquals(100, p.getDuracion());
        assertEquals(100, p.getTiempoRestante());
        assertEquals(0, p.getTiempoEjecutado());
        assertEquals(64, p.getTamanoMemoria());
        assertEquals(5, p.getTiempoLlegada());
        assertFalse(p.yaComenzo());
        assertEquals(-1, p.getTiempoInicioReal());
        assertEquals(-1, p.getTiempoFinalizacionReal());
        assertEquals(0, p.getTiempoEspera());
        assertEquals(-1, p.getTiempoRespuesta());
        assertEquals(0, p.getTiempoRetorno());
        assertEquals(0.0, p.getPorcentajeCompletitud());
    }

    @Test
    void constructorSinLlegadaUsaCero() {
        assertEquals(0, new Proceso("P", 10, 1).getTiempoLlegada());
    }

    @Test
    void idsUnicosEIgualdadPorId() {
        Proceso a = new Proceso("A", 10, 1);
        Proceso b = new Proceso("A", 10, 1);
        assertNotEquals(a.getId(), b.getId());
        assertNotEquals(a, b);
        assertEquals(a, a);
        assertEquals(Integer.hashCode(a.getId()), a.hashCode());
    }

    @Test
    void soloEjecutaSiEstaEnEjecucion() {
        Proceso p = new Proceso("P", 10, 1);
        assertFalse(p.ejecutarQuantumSinDelay(5, 0));
        assertEquals(10, p.getTiempoRestante());

        p.iniciarEjecucion(0);
        assertTrue(p.estaEjecutando());
        assertFalse(p.ejecutarQuantumSinDelay(4, 0));
        assertEquals(6, p.getTiempoRestante());
        assertEquals(4, p.getTiempoEjecutado());
        assertEquals(40.0, p.getPorcentajeCompletitud(), 1e-9);
    }

    @Test
    void quantumMayorQueRestanteTerminaElProceso() {
        Proceso p = new Proceso("P", 10, 1, 2);
        p.iniciarEjecucion(2);
        assertTrue(p.ejecutarQuantumSinDelay(50, 20));
        assertTrue(p.haTerminado());
        assertEquals(0, p.getTiempoRestante());
        assertEquals(10, p.getTiempoEjecutado(), "no debe ejecutar más que su duración");
        assertEquals(100.0, p.getPorcentajeCompletitud(), 1e-9);
    }

    @Test
    void ejecutarConRetardoTambienDescuentaTiempo() {
        Proceso p = new Proceso("P", 6, 1);
        p.iniciarEjecucion(0);
        assertFalse(p.ejecutar(4, 0));
        assertTrue(p.ejecutar(4, 4));
        assertTrue(p.haTerminado());
    }

    @Test
    void pausarSoloAfectaAProcesosEnEjecucion() {
        Proceso p = new Proceso("P", 10, 1);
        p.pausar();
        assertEquals(EstadoProceso.NUEVO, p.getEstado());
        p.iniciarEjecucion(0);
        p.pausar();
        assertEquals(EstadoProceso.LISTO, p.getEstado());
        p.iniciarEjecucion(1);
        assertEquals(EstadoProceso.EJECUTANDO, p.getEstado());
    }

    @Test
    void iniciarEjecucionNoAfectaProcesosTerminadosOEnEspera() {
        Proceso p = new Proceso("P", 10, 1);
        p.setEstado(EstadoProceso.ESPERANDO);
        p.iniciarEjecucion(0);
        assertEquals(EstadoProceso.ESPERANDO, p.getEstado());
        p.setEstado(EstadoProceso.TERMINADO);
        p.iniciarEjecucion(0);
        assertEquals(EstadoProceso.TERMINADO, p.getEstado());
    }

    @Test
    void tiemposDeEsperaRespuestaYRetorno() {
        Proceso p = new Proceso("P", 4, 1, 3);
        p.marcarInicioEjecucion(7);
        p.marcarInicioEjecucion(9); // solo cuenta la primera vez
        assertTrue(p.yaComenzo());
        assertEquals(7, p.getTiempoInicioReal());
        assertEquals(4, p.getTiempoEspera());
        assertEquals(4, p.getTiempoRespuesta());

        p.marcarFinalizacion(12);
        assertEquals(12, p.getTiempoFinalizacionReal());
        assertEquals(9, p.getTiempoRetorno());
    }

    @Test
    void tiemposNuncaSonNegativos() {
        Proceso p = new Proceso("P", 4, 1, 10);
        p.marcarInicioEjecucion(5);
        p.marcarFinalizacion(6);
        assertEquals(0, p.getTiempoEspera());
        assertEquals(0, p.getTiempoRespuesta());
        assertEquals(0, p.getTiempoRetorno());
    }

    @Test
    void reiniciarTiemposDejaElProcesoComoNuevo() {
        Proceso p = new Proceso("P", 10, 1);
        p.iniciarEjecucion(0);
        p.marcarInicioEjecucion(0);
        p.ejecutarQuantumSinDelay(10, 0);
        p.marcarFinalizacion(10);

        p.reiniciarTiempos();
        assertEquals(EstadoProceso.NUEVO, p.getEstado());
        assertEquals(10, p.getTiempoRestante());
        assertEquals(0, p.getTiempoEjecutado());
        assertFalse(p.yaComenzo());
        assertEquals(-1, p.getTiempoInicioReal());
        assertEquals(-1, p.getTiempoFinalizacionReal());
    }

    @Test
    void toStringIncluyeDatosPrincipales() {
        Proceso p = new Proceso("Editor", 10, 32);
        String s = p.toString();
        assertTrue(s.contains("Editor"));
        assertTrue(s.contains("32MB"));
        assertTrue(s.contains("NUEVO"));
    }
}
