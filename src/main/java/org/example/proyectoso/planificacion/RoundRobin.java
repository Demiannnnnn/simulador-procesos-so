package org.example.proyectoso.planificacion;

import org.example.proyectoso.models.*;
import org.example.proyectoso.utils.SimuladorTiempo;
import java.util.*;


public class RoundRobin extends Planificacion {

    
    private final int quantum;
    private final Object rrLock = new Object();

    
    private volatile boolean pausado;
    private Thread hiloEjecucion;

    
    private int cambiosContexto;
    private long tiempoPromedioEspera;
    private long tiempoPromedioRespuesta;

    


    
    public RoundRobin(int quantum) {
        super();
        this.quantum = quantum;
        this.pausado = false;
        this.cambiosContexto = 0;
        this.tiempoPromedioEspera = 0;
        this.tiempoPromedioRespuesta = 0;
        System.out.println("🔧 Round Robin inicializado - Quantum: " + quantum + "ms");
    }

    @Override
    public String getNombreAlgoritmo() {
        return "Round Robin (quantum=" + quantum + "ms)";
    }

    @Override
    protected List<Proceso> ordenarProcesos(List<Proceso> procesos) {
        return List.of();
    }

    @Override
    public void ejecutarProcesos(List<Proceso> procesos) {
        if (procesos == null || procesos.isEmpty()) {
            System.out.println("⚠️ No hay procesos para ejecutar en Round Robin");
            return;
        }

        iniciarEjecucion(procesos);
        hiloEjecucion = new Thread(() -> {
            try {
                ejecutarRoundRobin(new ArrayList<>(procesos));
            } catch (Exception e) {
                System.err.println("❌ Error en ejecución Round Robin: " + e.getMessage());
                e.printStackTrace();
            } finally {
                finalizarEjecucion();
            }
        });
        hiloEjecucion.setName("RR-Executor");
        hiloEjecucion.start();
    }

    
    private void ejecutarRoundRobin(List<Proceso> procesos) {
        synchronized (rrLock) {
            SimuladorTiempo.iniciar();
            System.out.println("🚀 Iniciando Round Robin con " + procesos.size() + " procesos (quantum=" + quantum + "ms)");
            Queue<Proceso> cola = new LinkedList<>(procesos);

            while (!cola.isEmpty() && ejecutando && !pausado) {
                Proceso proceso = cola.poll();
                if (proceso == null || proceso.haTerminado()) {
                    continue;
                }

                
                int tiempoEjecucion = Math.min(quantum, proceso.getTiempoRestante());
                ejecutarProcesoParcialRR(proceso, tiempoEjecucion);

                if (!proceso.haTerminado()) {
                    cola.offer(proceso);
                    cambiosContexto++;
                } else {
                    procesosEjecutados++;
                }

                
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }

            calcularEstadisticas(procesos);
        }
    }


    private void ejecutarProcesoParcialRR(Proceso proceso, int tiempoEjecucion) {
        if (proceso == null || proceso.haTerminado() || tiempoEjecucion <= 0) {
            return;
        }

        Core core = esperarCoreLibre();
        if (core == null) {
            return;
        }

        try {
            if (!proceso.yaComenzo()) {
                proceso.marcarInicioEjecucion((int) SimuladorTiempo.getTiempoActual());
            }

            proceso.setEstado(EstadoProceso.EJECUTANDO);
            core.asignarProceso(proceso);

            System.out.println("▶️ RR ejecutando proceso " + proceso.getId() +
                    " por " + tiempoEjecucion + "ms (Restante: " +
                    proceso.getTiempoRestante() + "ms)");
            boolean terminado = proceso.ejecutarQuantumSinDelay(tiempoEjecucion, System.currentTimeMillis());
            try {
                Thread.sleep(tiempoEjecucion);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }

            if (terminado) {
                proceso.setEstado(EstadoProceso.TERMINADO);
                System.out.println("✅ Proceso " + proceso.getId() + " completado");
            } else {
                proceso.setEstado(EstadoProceso.LISTO);
                System.out.println("⏸️ Proceso " + proceso.getId() + " suspendido (quantum agotado)");
            }

        } finally {
            core.liberarCore();
        }
    }

    
    private void calcularEstadisticas(List<Proceso> procesos) {
        List<Proceso> terminados = procesos.stream().filter(Proceso::haTerminado).toList();
        if (!terminados.isEmpty()) {
            tiempoPromedioEspera = (long) terminados.stream().mapToInt(Proceso::getTiempoEspera).average().orElse(0.0);
            tiempoPromedioRespuesta = (long) terminados.stream().mapToInt(Proceso::getTiempoRespuesta).average().orElse(0.0);
        }
        System.out.println("📊 Estadísticas RR:");
        System.out.println("   - Cambios de contexto: " + cambiosContexto);
        System.out.println("   - Tiempo promedio espera: " + tiempoPromedioEspera + "ms");
        System.out.println("   - Tiempo promedio respuesta: " + tiempoPromedioRespuesta + "ms");
    }

    // pausar/reanudar/detener no toman rrLock: el hilo de ejecución lo mantiene durante
    // toda la simulación, por lo que antes estas llamadas quedaban bloqueadas hasta que
    // terminaban todos los procesos. Los flags son volatile y basta con cambiarlos.
    @Override
    public void pausar() {
        pausado = true;
        SimuladorTiempo.pausar();
        super.pausar();
    }

    @Override
    public void reanudar() {
        pausado = false;
        SimuladorTiempo.reanudar();
        super.reanudar();
    }

    @Override
    public void detener() {
        pausado = true;
        super.detener();

        Thread hilo = hiloEjecucion;
        if (hilo != null && hilo.isAlive() && hilo != Thread.currentThread()) {
            hilo.interrupt();
            try {
                hilo.join(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    @Override
    public String getEstadisticas() {
        StringBuilder stats = new StringBuilder(super.getEstadisticas());
        synchronized (rrLock) {
            stats.append("Quantum: ").append(quantum).append("ms\n");
            stats.append("Cambios de contexto: ").append(cambiosContexto).append("\n");
            stats.append("T.promedio espera: ").append(tiempoPromedioEspera).append("ms\n");
            stats.append("T.promedio respuesta: ").append(tiempoPromedioRespuesta).append("ms\n");
        }
        return stats.toString();
    }

    @Override
    public String getEstadoActual() {
        StringBuilder estado = new StringBuilder(super.getEstadoActual());
        synchronized (rrLock) {
            estado.append("Pausado: ").append(pausado ? "SÍ" : "NO").append("\n");
            estado.append("Cambios contexto: ").append(cambiosContexto).append("\n");
            if (hiloEjecucion != null) {
                estado.append("Hilo ejecución: ").append(hiloEjecucion.getState()).append("\n");
            }
        }
        return estado.toString();
    }

    @Override
    public String toString() {
        synchronized (rrLock) {
            return String.format("RoundRobin[Q=%dms, %s, Procesos:%d, Contextos:%d]", quantum,
                    ejecutando ? "EJECUTANDO" : "DETENIDO", procesosEjecutados, cambiosContexto);
        }
    }

}
