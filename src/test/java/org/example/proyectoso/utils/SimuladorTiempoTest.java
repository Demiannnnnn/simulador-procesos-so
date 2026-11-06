package org.example.proyectoso.utils;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class SimuladorTiempoTest {

    @AfterEach
    void restaurar() {
        SimuladorTiempo.reanudar();
        SimuladorTiempo.setFactorVelocidad(1.0);
    }

    @Test
    void formatearTiempo() {
        assertEquals("00:00:00.000", SimuladorTiempo.formatearTiempo(0));
        assertEquals("00:00:01.250", SimuladorTiempo.formatearTiempo(1250));
        assertEquals("00:01:05.007", SimuladorTiempo.formatearTiempo(65_007));
        assertEquals("02:03:04.005", SimuladorTiempo.formatearTiempo(((2 * 60 + 3) * 60 + 4) * 1000L + 5));
    }

    @Test
    void factorDeVelocidadDebeSerPositivo() {
        assertThrows(IllegalArgumentException.class, () -> SimuladorTiempo.setFactorVelocidad(0));
        assertThrows(IllegalArgumentException.class, () -> SimuladorTiempo.setFactorVelocidad(-2));
    }

    @Test
    void elTiempoAvanzaYSeDetieneAlPausar() throws InterruptedException {
        SimuladorTiempo.iniciar();
        Thread.sleep(30);
        long t1 = SimuladorTiempo.getTiempoActual();
        assertTrue(t1 >= 25, "debería haber avanzado, fue " + t1);

        SimuladorTiempo.pausar();
        long pausado = SimuladorTiempo.getTiempoActual();
        Thread.sleep(40);
        assertEquals(pausado, SimuladorTiempo.getTiempoActual());

        SimuladorTiempo.reanudar();
        Thread.sleep(20);
        long t2 = SimuladorTiempo.getTiempoActual();
        assertTrue(t2 >= pausado && t2 < pausado + 40 + 200, "al reanudar no debe contar el tiempo en pausa: " + t2);
    }

    @Test
    void factorDeVelocidadAceleraElTiempo() throws InterruptedException {
        SimuladorTiempo.iniciar();
        SimuladorTiempo.setFactorVelocidad(10.0);
        long antes = SimuladorTiempo.getTiempoActual();
        Thread.sleep(30);
        long avance = SimuladorTiempo.getTiempoActual() - antes;
        assertTrue(avance >= 250, "con factor 10 deberían pasar ~300ms simulados, fueron " + avance);
    }

    @Test
    void esperarIgnoraValoresNoPositivos() {
        long inicio = System.nanoTime();
        SimuladorTiempo.esperar(0);
        SimuladorTiempo.esperar(-100);
        assertTrue(System.nanoTime() - inicio < 50_000_000L);
    }
}
