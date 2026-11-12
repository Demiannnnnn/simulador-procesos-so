package org.example.proyectoso.models;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.Test;

class CPUTest {

    @Test
    void creaElNumeroDeCoresIndicado() {
        CPU cpu = new CPU(4);
        assertEquals(4, cpu.getNumeroCores());
        assertEquals("CPU-4Core", cpu.getNombre());
        assertEquals(4, cpu.getCoresLibresCount());
        assertEquals(0, cpu.getCoresOcupadosCount());
        for (int i = 0; i < 4; i++) {
            assertEquals(i, cpu.getCore(i).getId());
        }
        assertNull(cpu.getCore(4));
        cpu.detener();
    }

    @Test
    void minimoUnCore() {
        assertEquals(1, new CPU(0).getNumeroCores());
        assertEquals(1, new CPU(-3).getNumeroCores());
    }

    @Test
    void coresLibresYOcupados() {
        CPU cpu = new CPU(3);
        cpu.getCore(1).asignarProceso(new Proceso("P", 100, 1));
        assertEquals(2, cpu.getCoresLibresCount());
        assertEquals(1, cpu.getCoresOcupadosCount());
        assertEquals(1, cpu.getCoresOcupados().get(0).getId());
        cpu.detener();
    }

    @Test
    void interrumpirTodosLiberaLosCoresYDevuelveLosProcesos() {
        CPU cpu = new CPU(2);
        Proceso a = new Proceso("A", 100, 1);
        Proceso b = new Proceso("B", 100, 1);
        cpu.getCore(0).asignarProceso(a);
        cpu.getCore(1).asignarProceso(b);

        List<Proceso> interrumpidos = cpu.interrumpirTodos();
        assertEquals(2, interrumpidos.size());
        assertTrue(interrumpidos.containsAll(List.of(a, b)));
        assertEquals(2, cpu.getCoresLibresCount());
        assertEquals(EstadoProceso.LISTO, a.getEstado());
        cpu.detener();
    }

    @Test
    void quantumMinimoEsUno() {
        CPU cpu = new CPU(1);
        cpu.setQuantumRoundRobin(0);
        assertTrue(cpu.getEstadoActual().contains("Quantum: 1ms"));
        cpu.setAlgoritmo(CPU.TipoAlgoritmo.SJF);
        assertTrue(cpu.getEstadoActual().contains("Algoritmo: SJF"));
        cpu.detener();
    }
}
