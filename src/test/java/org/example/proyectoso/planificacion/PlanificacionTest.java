package org.example.proyectoso.planificacion;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import org.example.proyectoso.models.CPU;
import org.example.proyectoso.models.EstadoProceso;
import org.example.proyectoso.models.Proceso;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class PlanificacionTest {

    private final List<CPU> cpus = new ArrayList<>();

    @AfterEach
    void cerrarCpus() {
        cpus.forEach(CPU::detener);
    }

    private CPU cpu(int cores) {
        CPU c = new CPU(cores);
        cpus.add(c);
        return c;
    }

    private static List<Proceso> procesos(int... duraciones) {
        List<Proceso> l = new ArrayList<>();
        for (int i = 0; i < duraciones.length; i++) {
            l.add(new Proceso("P" + i, duraciones[i], 10));
        }
        return l;
    }

    private static void esperarFin(Planificacion p) throws InterruptedException {
        long limite = System.currentTimeMillis() + 15_000;
        while (p.ejecutando && System.currentTimeMillis() < limite) {
            Thread.sleep(10);
        }
        assertFalse(p.ejecutando, "la planificación no terminó a tiempo");
        Thread.sleep(20); // deja que el hilo termine de escribir estadísticas
    }

    @Test
    void sjfNoPreemptivoOrdenaPorDuracion() {
        SJF sjf = new SJF();
        List<Proceso> l = procesos(50, 10, 30, 20);
        List<Proceso> ordenados = sjf.ordenarProcesos(l);
        assertEquals(List.of(l.get(1), l.get(3), l.get(2), l.get(0)), ordenados);
        assertEquals(50, l.get(0).getDuracion(), "no debe modificar la lista original");
    }

    @Test
    void sjfOrdenaEsEstableEIgnoraNulos() {
        SJF sjf = new SJF();
        List<Proceso> l = procesos(20, 20, 5);
        List<Proceso> conNulo = new ArrayList<>(l);
        conNulo.add(1, null);
        assertEquals(List.of(l.get(2), l.get(0), l.get(1)), sjf.ordenarProcesos(conNulo));
        assertTrue(sjf.ordenarProcesos(null).isEmpty());
    }

    @Test
    void sjfPreemptivoOrdenaPorTiempoRestante() {
        SJF srtf = new SJF(true);
        List<Proceso> l = procesos(50, 40);
        Proceso p0 = l.get(0);
        p0.iniciarEjecucion(0);
        p0.ejecutarQuantumSinDelay(45, 0); // le quedan 5
        assertEquals(List.of(p0, l.get(1)), srtf.ordenarProcesos(l));
    }

    @Test
    void nombresDeAlgoritmos() {
        assertEquals("SJF No Preemptivo", new SJF().getNombreAlgoritmo());
        assertEquals("SJF Preemptivo (SRTF)", new SJF(true).getNombreAlgoritmo());
        assertEquals("Round Robin (quantum=25ms)", new RoundRobin(25).getNombreAlgoritmo());
    }

    @Test
    void sinCpuConfiguradaLanzaExcepcion() {
        SJF sjf = new SJF();
        assertThrows(IllegalStateException.class, () -> sjf.ejecutarProcesos(procesos(10)));
    }

    @Test
    void listaVaciaNoArrancaLaEjecucion() {
        RoundRobin rr = new RoundRobin(10);
        rr.setCpu(cpu(1));
        rr.ejecutarProcesos(List.of());
        rr.ejecutarProcesos(null);
        assertFalse(rr.ejecutando);
    }

    @Test
    void roundRobinTerminaTodosConLosCambiosDeContextoEsperados() throws InterruptedException {
        RoundRobin rr = new RoundRobin(10);
        rr.setCpu(cpu(1));
        List<Proceso> l = procesos(10, 20, 30);

        rr.ejecutarProcesos(l);
        esperarFin(rr);

        l.forEach(p -> assertEquals(EstadoProceso.TERMINADO, p.getEstado(), p.toString()));
        l.forEach(p -> assertEquals(0, p.getTiempoRestante()));
        String stats = rr.getEstadisticas();
        assertTrue(stats.contains("Procesos ejecutados: 3"), stats);
        // quanta: 1 + 2 + 3 = 6; cada quantum que no termina el proceso es un cambio de contexto
        assertTrue(stats.contains("Cambios de contexto: 3"), stats);
        assertEquals(1, rr.cpu.getCoresLibresCount(), "el core debe quedar libre");
    }

    @Test
    void sjfNoPreemptivoTerminaTodos() throws InterruptedException {
        SJF sjf = new SJF();
        sjf.setCpu(cpu(2));
        List<Proceso> l = procesos(40, 10, 20);

        sjf.ejecutarProcesos(l);
        esperarFin(sjf);

        l.forEach(p -> assertTrue(p.haTerminado(), p.toString()));
        String stats = sjf.getEstadisticas();
        assertTrue(stats.contains("Procesos ejecutados: 3"), stats);
        assertTrue(stats.contains("Cambios de contexto: 2"), stats);
    }

    @Test
    void sjfPreemptivoTerminaTodos() throws InterruptedException {
        SJF srtf = new SJF(true);
        srtf.setCpu(cpu(1));
        List<Proceso> l = procesos(150, 30);

        srtf.ejecutarProcesos(l);
        esperarFin(srtf);

        l.forEach(p -> assertTrue(p.haTerminado(), p.toString()));
        assertTrue(srtf.getEstadisticas().contains("Procesos ejecutados: 2"));
    }

    @Test
    void detenerInterrumpeLaEjecucion() throws InterruptedException {
        RoundRobin rr = new RoundRobin(50);
        rr.setCpu(cpu(1));
        List<Proceso> l = procesos(5_000);

        rr.ejecutarProcesos(l);
        Thread.sleep(100);
        long inicio = System.currentTimeMillis();
        rr.detener();

        assertTrue(System.currentTimeMillis() - inicio < 2_000, "detener() no debe bloquearse");
        assertFalse(rr.ejecutando);
        assertFalse(l.get(0).haTerminado());
        assertTrue(rr.getEstadoActual().contains("Ejecutando: NO"));
    }

    @Test
    void detenerSjfNoEsperaAQueTerminenLosProcesos() throws InterruptedException {
        SJF sjf = new SJF();
        sjf.setCpu(cpu(1));
        List<Proceso> l = procesos(5_000, 5_000);

        sjf.ejecutarProcesos(l);
        Thread.sleep(100);
        long inicio = System.currentTimeMillis();
        sjf.detener();

        assertTrue(System.currentTimeMillis() - inicio < 2_000, "detener() no debe bloquearse");
        assertFalse(sjf.ejecutando);
        l.forEach(p -> assertFalse(p.haTerminado()));
    }
}
