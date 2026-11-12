package org.example.proyectoso.memoria;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.example.proyectoso.models.EstadoProceso;
import org.example.proyectoso.models.Proceso;
import org.junit.jupiter.api.Test;

class SwappingTest {

    @Test
    void colaFifoYMemoriaRequerida() {
        Swapping s = new Swapping();
        Proceso a = new Proceso("A", 1, 30);
        Proceso b = new Proceso("B", 1, 70);
        s.moverASwap(a);
        s.moverASwap(b);
        assertEquals(List.of(a, b), s.getProcesosEnSwapping());
        assertEquals(2, s.getCantidadProcesos());
        assertEquals(100, s.getMemoriaRequerida());
        assertEquals(EstadoProceso.ESPERANDO, a.getEstado());
    }

    @Test
    void procesarColaAsignaEnOrdenMientrasHayaEspacio() {
        Swapping s = new Swapping();
        Memoria m = new Memoria(100);
        Proceso a = new Proceso("A", 1, 60);
        Proceso b = new Proceso("B", 1, 60);
        Proceso c = new Proceso("C", 1, 40);
        s.moverASwap(a);
        s.moverASwap(b);
        s.moverASwap(c);

        List<Proceso> liberados = s.procesarCola(m);

        assertEquals(List.of(a, c), liberados);
        assertEquals(List.of(b), s.getProcesosEnSwapping());
        assertEquals(1, s.getCantidadProcesos());
        assertEquals(EstadoProceso.LISTO, a.getEstado());
        assertEquals(EstadoProceso.ESPERANDO, b.getEstado());
        assertEquals(100, m.getMemoriaUsada());
    }

    @Test
    void limpiar() {
        Swapping s = new Swapping();
        s.moverASwap(new Proceso("A", 1, 10));
        s.limpiar();
        assertEquals(0, s.getCantidadProcesos());
        assertEquals(0, s.getMemoriaRequerida());
    }
}
