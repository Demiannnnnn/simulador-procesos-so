package org.example.proyectoso.memoria;

import static org.junit.jupiter.api.Assertions.*;

import org.example.proyectoso.models.Proceso;
import org.junit.jupiter.api.Test;

class BloqueMemoriaTest {

    @Test
    void finDelBloque() {
        assertEquals(149, new BloqueMemoria(100, 50).getFin());
    }

    @Test
    void asignarYLiberar() {
        BloqueMemoria b = new BloqueMemoria(0, 10);
        Proceso p = new Proceso("P", 1, 10);
        b.asignar(p);
        assertTrue(b.isOcupado());
        assertSame(p, b.getProceso());
        assertTrue(b.toString().contains("Proceso " + p.getId()));
        b.liberar();
        assertFalse(b.isOcupado());
        assertNull(b.getProceso());
        assertTrue(b.toString().contains("LIBRE"));
    }

    @Test
    void adyacenciaEnAmbosSentidos() {
        BloqueMemoria a = new BloqueMemoria(0, 10);
        BloqueMemoria b = new BloqueMemoria(10, 5);
        BloqueMemoria c = new BloqueMemoria(16, 5);
        assertTrue(a.esAdyacente(b));
        assertTrue(b.esAdyacente(a));
        assertFalse(a.esAdyacente(c));
        assertFalse(b.esAdyacente(c));
    }

    @Test
    void soloSeUnenBloquesLibresYAdyacentes() {
        BloqueMemoria a = new BloqueMemoria(0, 10);
        BloqueMemoria b = new BloqueMemoria(10, 5);
        assertTrue(a.puedeUnirse(b));
        b.asignar(new Proceso("P", 1, 5));
        assertFalse(a.puedeUnirse(b));
        assertFalse(a.puedeUnirse(new BloqueMemoria(20, 5)));
    }

    @Test
    void igualdadPorInicioYTamano() {
        assertEquals(new BloqueMemoria(5, 10), new BloqueMemoria(5, 10));
        assertNotEquals(new BloqueMemoria(5, 10), new BloqueMemoria(5, 11));
        assertEquals(new BloqueMemoria(5, 10).hashCode(), new BloqueMemoria(5, 10).hashCode());
    }
}
