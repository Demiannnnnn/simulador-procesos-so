package org.example.proyectoso.utils;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.example.proyectoso.models.EstadoProceso;
import org.example.proyectoso.models.Proceso;
import org.junit.jupiter.api.Test;

class EstadisticasTest {

    private static Proceso terminado(int duracion, int llegada, int inicio, int fin) {
        Proceso p = new Proceso("P", duracion, 1, llegada);
        p.marcarInicioEjecucion(inicio);
        p.marcarFinalizacion(fin);
        p.setEstado(EstadoProceso.TERMINADO);
        return p;
    }

    @Test
    void promediosSoloConsideranTerminados() {
        Proceso a = terminado(4, 0, 0, 4);   // espera 0, retorno 4
        Proceso b = terminado(3, 1, 4, 7);   // espera 3, retorno 6
        Proceso c = terminado(2, 2, 7, 9);   // espera 5, retorno 7
        Proceso pendiente = new Proceso("X", 10, 1, 0);
        pendiente.marcarInicioEjecucion(100);
        List<Proceso> l = List.of(a, b, c, pendiente);

        assertEquals(8.0 / 3, Estadisticas.calcularTiempoEsperaPromedio(l), 1e-9);
        assertEquals(8.0 / 3, Estadisticas.calcularTiempoRespuestaPromedio(l), 1e-9);
        assertEquals(17.0 / 3, Estadisticas.calcularTiempoRetornoPromedio(l), 1e-9);
    }

    @Test
    void promediosSinTerminadosSonCero() {
        List<Proceso> l = List.of(new Proceso("X", 10, 1));
        assertEquals(0.0, Estadisticas.calcularTiempoEsperaPromedio(l));
        assertEquals(0.0, Estadisticas.calcularTiempoRetornoPromedio(List.of()));
    }

    @Test
    void throughput() {
        List<Proceso> l = List.of(terminado(2, 0, 0, 2), terminado(2, 0, 2, 4), new Proceso("X", 1, 1));
        assertEquals(0.2, Estadisticas.calcularThroughput(l, 10), 1e-9);
        assertEquals(0.0, Estadisticas.calcularThroughput(l, 0));
    }

    @Test
    void eficienciaEsTiempoUtilSobreTiempoDisponible() {
        List<Proceso> l = List.of(terminado(4, 0, 0, 4), terminado(6, 0, 0, 6), new Proceso("X", 100, 1));
        // 10 unidades útiles / (10 unidades * 2 cores) = 50%
        assertEquals(50.0, Estadisticas.calcularEficiencia(l, 10, 2), 1e-9);
        assertEquals(0.0, Estadisticas.calcularEficiencia(List.of(), 10, 2));
        assertEquals(0.0, Estadisticas.calcularEficiencia(l, 0, 2));
    }

    @Test
    void resumenDeEstados() {
        Proceso p = new Proceso("P", 1, 1);
        String s = Estadisticas.getEstadisticasEstados(List.of(p), List.of(), List.of(), List.of(p, p), List.of(p, p, p, p));
        assertTrue(s.contains("Procesos Nuevos: 1"));
        assertTrue(s.contains("Procesos Terminados: 2"));
        assertTrue(s.contains("Total de Procesos: 4"));
        assertTrue(s.contains("Progreso: 50"), s);
    }
}
