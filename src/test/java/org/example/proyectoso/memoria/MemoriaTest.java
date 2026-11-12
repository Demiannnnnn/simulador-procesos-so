package org.example.proyectoso.memoria;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.example.proyectoso.models.EstadoProceso;
import org.example.proyectoso.models.Proceso;
import org.junit.jupiter.api.Test;

class MemoriaTest {

    private static void assertCubreTodaLaMemoria(Memoria m) {
        List<BloqueMemoria> bloques = m.getBloques();
        int esperado = 0;
        int usada = 0;
        for (BloqueMemoria b : bloques) {
            assertEquals(esperado, b.getInicio(), "los bloques deben ser contiguos y ordenados: " + bloques);
            esperado += b.getTamaño();
            if (b.isOcupado()) usada += b.getTamaño();
        }
        assertEquals(m.getTamañoTotal(), esperado);
        assertEquals(usada, m.getMemoriaUsada());
        for (int i = 0; i < bloques.size() - 1; i++) {
            assertFalse(bloques.get(i).puedeUnirse(bloques.get(i + 1)),
                    "no deben quedar bloques libres adyacentes sin fusionar: " + bloques);
        }
    }

    @Test
    void memoriaNuevaEsUnSoloBloqueLibre() {
        Memoria m = new Memoria(1024);
        assertEquals(1024, m.getTamañoTotal());
        assertEquals(0, m.getMemoriaUsada());
        assertEquals(1024, m.getMemoriaLibre());
        assertEquals(1, m.getBloques().size());
        assertFalse(m.getBloques().get(0).isOcupado());
    }

    @Test
    void asignarDivideElBloque() {
        Memoria m = new Memoria(1000);
        Proceso p1 = new Proceso("P1", 1, 100);
        Proceso p2 = new Proceso("P2", 1, 200);
        assertTrue(m.asignarMemoria(p1));
        assertTrue(m.asignarMemoria(p2));

        List<BloqueMemoria> b = m.getBloques();
        assertEquals(3, b.size());
        assertSame(p1, b.get(0).getProceso());
        assertEquals(100, b.get(0).getTamaño());
        assertSame(p2, b.get(1).getProceso());
        assertEquals(100, b.get(1).getInicio());
        assertFalse(b.get(2).isOcupado());
        assertEquals(700, b.get(2).getTamaño());

        assertEquals(300, m.getMemoriaUsada());
        assertEquals(700, m.getMemoriaLibre());
        assertEquals(30.0, m.getPorcentajeUso(), 1e-9);
        assertCubreTodaLaMemoria(m);
    }

    @Test
    void ajusteExactoNoCreaBloqueVacio() {
        Memoria m = new Memoria(256);
        assertTrue(m.asignarMemoria(new Proceso("P", 1, 256)));
        assertEquals(1, m.getBloques().size());
        assertEquals(0, m.getMemoriaLibre());
        assertCubreTodaLaMemoria(m);
    }

    @Test
    void sinEspacioSuficienteNoAsigna() {
        Memoria m = new Memoria(100);
        assertFalse(m.asignarMemoria(new Proceso("Grande", 1, 101)));
        assertEquals(0, m.getMemoriaUsada());
    }

    @Test
    void primerAjusteReutilizaElPrimerHuecoQueCabe() {
        Memoria m = new Memoria(1000);
        Proceso p1 = new Proceso("P1", 1, 100);
        Proceso p2 = new Proceso("P2", 1, 100);
        Proceso p3 = new Proceso("P3", 1, 300);
        m.asignarMemoria(p1);
        m.asignarMemoria(p2);
        m.asignarMemoria(p3);
        m.liberarMemoria(p1); // hueco 0-99

        Proceso p4 = new Proceso("P4", 1, 50);
        assertTrue(m.asignarMemoria(p4));
        assertEquals(0, m.getBloques().get(0).getInicio());
        assertSame(p4, m.getBloques().get(0).getProceso());

        Proceso p5 = new Proceso("P5", 1, 80); // no cabe en el hueco de 50 restante
        assertTrue(m.asignarMemoria(p5));
        BloqueMemoria bloqueP5 = m.getBloques().stream().filter(b -> b.getProceso() == p5).findFirst().orElseThrow();
        assertEquals(500, bloqueP5.getInicio());
        assertCubreTodaLaMemoria(m);
    }

    @Test
    void fragmentacionExternaImpideAsignarAunqueHayaMemoriaLibreTotal() {
        Memoria m = new Memoria(300);
        Proceso a = new Proceso("A", 1, 100);
        Proceso b = new Proceso("B", 1, 100);
        Proceso c = new Proceso("C", 1, 100);
        m.asignarMemoria(a);
        m.asignarMemoria(b);
        m.asignarMemoria(c);
        m.liberarMemoria(a);
        m.liberarMemoria(c);

        assertEquals(200, m.getMemoriaLibre());
        assertFalse(m.asignarMemoria(new Proceso("D", 1, 150)), "no hay un hueco contiguo de 150");
        assertCubreTodaLaMemoria(m);
    }

    @Test
    void liberarFusionaBloquesLibresAdyacentes() {
        Memoria m = new Memoria(1000);
        Proceso p1 = new Proceso("P1", 1, 100);
        Proceso p2 = new Proceso("P2", 1, 200);
        Proceso p3 = new Proceso("P3", 1, 300);
        m.asignarMemoria(p1);
        m.asignarMemoria(p2);
        m.asignarMemoria(p3);

        assertTrue(m.liberarMemoria(p2));
        assertEquals(4, m.getBloques().size());
        assertTrue(m.liberarMemoria(p1));
        assertEquals(3, m.getBloques().size(), "P1 y P2 libres deben fusionarse");
        assertEquals(300, m.getBloques().get(0).getTamaño());
        assertTrue(m.liberarMemoria(p3));
        assertEquals(1, m.getBloques().size());
        assertEquals(1000, m.getMemoriaLibre());
        assertCubreTodaLaMemoria(m);
    }

    @Test
    void liberarProcesoSinMemoriaDevuelveFalse() {
        Memoria m = new Memoria(100);
        assertFalse(m.liberarMemoria(new Proceso("X", 1, 10)));
    }

    @Test
    void liberarDosVecesSoloFuncionaLaPrimera() {
        Memoria m = new Memoria(100);
        Proceso p = new Proceso("P", 1, 10);
        m.asignarMemoria(p);
        assertTrue(m.liberarMemoria(p));
        assertFalse(m.liberarMemoria(p));
        assertEquals(0, m.getMemoriaUsada());
    }

    @Test
    void procesoEnSwappingEntraAlLiberarseMemoria() {
        Memoria m = new Memoria(300);
        Proceso p1 = new Proceso("P1", 1, 200);
        Proceso p2 = new Proceso("P2", 1, 200);
        assertTrue(m.asignarMemoria(p1));
        assertFalse(m.asignarMemoria(p2));
        m.moverASwapping(p2);

        assertEquals(EstadoProceso.ESPERANDO, p2.getEstado());
        assertEquals(1, m.getSwapping().getCantidadProcesos());
        assertEquals(200, m.getSwapping().getMemoriaRequerida());

        m.liberarMemoria(p1);

        assertEquals(EstadoProceso.LISTO, p2.getEstado());
        assertEquals(0, m.getSwapping().getCantidadProcesos());
        assertTrue(m.getSwapping().getProcesosEnSwapping().isEmpty());
        assertSame(p2, m.getBloques().get(0).getProceso());
        assertEquals(200, m.getMemoriaUsada());
        assertCubreTodaLaMemoria(m);
    }

    @Test
    void swappingSoloSacaLosProcesosQueCaben() {
        Memoria m = new Memoria(300);
        Proceso base = new Proceso("Base", 1, 300);
        m.asignarMemoria(base);
        Proceso grande = new Proceso("Grande", 1, 400);
        Proceso chico = new Proceso("Chico", 1, 100);
        m.moverASwapping(grande);
        m.moverASwapping(chico);

        m.liberarMemoria(base);

        assertEquals(EstadoProceso.ESPERANDO, grande.getEstado());
        assertEquals(EstadoProceso.LISTO, chico.getEstado());
        assertEquals(List.of(grande), m.getSwapping().getProcesosEnSwapping());
    }

    @Test
    void reiniciarVaciaMemoriaYSwapping() {
        Memoria m = new Memoria(100);
        m.asignarMemoria(new Proceso("P", 1, 60));
        m.moverASwapping(new Proceso("Q", 1, 60));
        m.reiniciar();
        assertEquals(0, m.getMemoriaUsada());
        assertEquals(1, m.getBloques().size());
        assertEquals(0, m.getSwapping().getCantidadProcesos());
    }

    @Test
    void getBloquesDevuelveUnaCopia() {
        Memoria m = new Memoria(100);
        m.getBloques().clear();
        assertEquals(1, m.getBloques().size());
    }

    @Test
    void asignacionesYLiberacionesConcurrentesMantienenLaConsistencia() throws InterruptedException {
        Memoria m = new Memoria(10_000);
        List<Proceso> procesos = new ArrayList<>();
        for (int i = 0; i < 200; i++) {
            procesos.add(new Proceso("P" + i, 1, 10 + (i % 7) * 5));
        }
        ExecutorService pool = Executors.newFixedThreadPool(8);
        for (Proceso p : procesos) {
            pool.submit(() -> {
                if (m.asignarMemoria(p) && p.getId() % 2 == 0) {
                    m.liberarMemoria(p);
                }
            });
        }
        pool.shutdown();
        assertTrue(pool.awaitTermination(30, TimeUnit.SECONDS));

        int esperada = procesos.stream().filter(p -> p.getId() % 2 != 0).mapToInt(Proceso::getTamanoMemoria).sum();
        assertEquals(esperada, m.getMemoriaUsada());
        assertCubreTodaLaMemoria(m);
    }
}
