# Simulador de Procesos SO

[![CI](https://github.com/Demiannnnnn/simulador-procesos-so/actions/workflows/ci.yml/badge.svg?branch=main)](https://github.com/Demiannnnnn/simulador-procesos-so/actions/workflows/ci.yml)

Simulador interactivo de planificación de procesos y administración de memoria para sistemas operativos desarrollado con Java y JavaFX.

## Descripción

El proyecto modela y visualiza la gestión del ciclo de vida de procesos concurrentes en arquitecturas multinúcleo:
- **Planificación de CPU:** Soporte para algoritmos SJF (Shortest Job First) en modos no preemptivo y preemptivo (SRTF), y Round Robin con quantum configurable.
- **Gestión de Memoria:** Esquema de particiones variables con estrategia de asignación de Primer Ajuste (*First Fit*), división y fusión de bloques contiguos liberados, y mecanismo de *swapping* cuando la memoria física es insuficiente.
- **Visualización y Métricas:** Interfaz gráfica basada en JavaFX con diagrama de Gantt en tiempo real, seguimiento del estado de núcleos y memoria, y generación automática de reportes de estadísticas de simulación.

## Stack Tecnológico

- **Lenguaje:** Java 22+ (código fuente y target en Java 22)
- **Framework de interfaz:** JavaFX 22 (`javafx-controls`, `javafx-fxml`, `javafx-maven-plugin`)
- **Gestor de dependencias y construcción:** Apache Maven (vía `./mvnw`)
- **Pruebas unitarias:** JUnit Jupiter 5.10.0 (JUnit 5) con `maven-surefire-plugin` 3.5.2

## Requisitos Previos

- JDK 22 o superior (el CI usa Temurin 25).

## Ejecución de la Aplicación

Para compilar y lanzar la interfaz gráfica mediante el plugin de JavaFX configurado en el `pom.xml`:

```bash
./mvnw clean javafx:run
```

o de forma directa:

```bash
./mvnw javafx:run
```

## Ejecución de Tests

Para ejecutar las pruebas unitarias:

```bash
./mvnw test
```

Para ejecutar el ciclo de verificación completo (compilación, pruebas unitarias y empaquetado JAR):

```bash
./mvnw -B -ntp verify
```
