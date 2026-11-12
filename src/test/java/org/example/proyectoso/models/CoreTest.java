package org.example.proyectoso.models;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class CoreTest {

    @Test
    void coreNuevoEstaLibre() {
        Core c = new Core(2);
        assertTrue(c.isLibre());
        assertFalse(c.isOcupado());
        assertEquals("Core-2", c.getNombre());
        assertEquals("Core-2: LIBRE", c.getEstadoActual());
        assertEquals("Core-2[LIBRE]", c.toString());
    }

    @Test
    void asignarProcesoLoPoneEnEjecucionYOcupaElCore() {
        Core c = new Core(0);
        Proceso p = new Proceso("P", 10, 1);
        assertTrue(c.asignarProceso(p));
        assertTrue(c.isOcupado());
        assertEquals(EstadoProceso.EJECUTANDO, p.getEstado());
        assertTrue(c.getEstadoActual().contains("Proceso " + p.getId()));
    }

    @Test
    void noSePuedeAsignarACoreOcupadoNiNull() {
        Core c = new Core(0);
        assertFalse(c.asignarProceso(null));
        assertTrue(c.asignarProceso(new Proceso("A", 10, 1)));
        assertFalse(c.asignarProceso(new Proceso("B", 10, 1)));
    }

    @Test
    void ejecutarQuantumPausaSiNoTermina() {
        Core c = new Core(0);
        c.setQuantum(5);
        Proceso p = new Proceso("P", 12, 1);
        c.asignarProceso(p);
        assertFalse(c.ejecutarQuantum());
        assertEquals(7, p.getTiempoRestante());
        assertEquals(EstadoProceso.LISTO, p.getEstado());
    }

    @Test
    void ejecutarQuantumLiberaElCoreAlTerminar() {
        Core c = new Core(0);
        c.setQuantum(50);
        Proceso p = new Proceso("P", 5, 1);
        c.asignarProceso(p);
        assertTrue(c.ejecutarQuantum());
        assertTrue(p.haTerminado());
        assertTrue(c.isLibre());
        assertTrue(c.getEstadisticas().contains("Procesos: 1"));
    }

    @Test
    void ejecutarSinProcesoDevuelveFalse() {
        Core c = new Core(0);
        assertFalse(c.ejecutarQuantum());
        assertFalse(c.ejecutarHastaCompletar());
        assertNull(c.interrumpir());
    }

    @Test
    void ejecutarHastaCompletar() {
        Core c = new Core(0);
        Proceso p = new Proceso("P", 8, 1);
        c.asignarProceso(p);
        assertTrue(c.ejecutarHastaCompletar());
        assertTrue(p.haTerminado());
        assertTrue(c.isLibre());
    }

    @Test
    void interrumpirDevuelveElProcesoPausado() {
        Core c = new Core(0);
        Proceso p = new Proceso("P", 100, 1);
        c.asignarProceso(p);
        assertSame(p, c.interrumpir());
        assertEquals(EstadoProceso.LISTO, p.getEstado());
        assertTrue(c.isLibre());
    }

    @Test
    void forzarLiberacionPausaElProceso() {
        Core c = new Core(0);
        Proceso p = new Proceso("P", 100, 1);
        c.asignarProceso(p);
        c.forzarLiberacion();
        assertTrue(c.isLibre());
        assertEquals(EstadoProceso.LISTO, p.getEstado());
    }

    @Test
    void elPorcentajeDeUsoConsideraElTiempoInactivo() throws InterruptedException {
        Core c = new Core(0);
        Thread.sleep(60); // inactivo
        Proceso p = new Proceso("P", 20, 1);
        c.asignarProceso(p);
        c.ejecutarHastaCompletar();
        double uso = c.getPorcentajeUso();
        assertTrue(uso > 0 && uso < 100, "el uso debería excluir el tiempo inactivo, fue " + uso);
    }

    @Test
    void reiniciarEstadisticas() {
        Core c = new Core(0);
        c.asignarProceso(new Proceso("P", 2, 1));
        c.ejecutarHastaCompletar();
        c.reiniciarEstadisticas();
        assertEquals(0.0, c.getPorcentajeUso());
        assertTrue(c.getEstadisticas().contains("Procesos: 0"));
    }

    @Test
    void igualdadPorId() {
        assertEquals(new Core(1), new Core(1));
        assertNotEquals(new Core(1), new Core(2));
    }
}
