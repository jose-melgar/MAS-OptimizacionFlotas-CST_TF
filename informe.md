**Optimización de Flotas con Sistemas Multiagente (MAS)**
1. Introducción

En entornos urbanos modernos, los sistemas de transporte bajo demanda como taxis, movilidad compartida o vehículos autónomos requieren mecanismos eficientes para asignar solicitudes de viaje a una flota de vehículos dispersos en la ciudad. El problema radica en la variabilidad dinámica del entorno: la demanda cambia constantemente, el tráfico es incierto y cada vehículo busca maximizar su propia eficiencia.

Por ello, surge la necesidad de diseñar una solución capaz de optimizar en tiempo real la asignación de servicios, reduciendo tiempos de espera y aumentando la utilización de la flota.

**Problema**

Asignación dinámica y óptima de viajes a una flota de vehículos en un entorno urbano variable.

Motivación

Optimizar esta asignación incrementa:

- La satisfacción del cliente (menos tiempo de espera),

- La eficiencia operativa (mayor ocupación vehicular),

- Los beneficios del sistema (mejor distribución de carga).

**Solución Propuesta**

Se plantea un Sistema Multiagente (MAS) implementado en JADE, donde:

Cada vehículo es un agente autónomo capaz de razonar y decidir, existe un Broker central encargado de la coordinación, la asignación se realiza mediante un mecanismo de subasta tipo FIPA Contract Net.
Este enfoque permite que la flota responda de manera flexible, distribuida y eficiente a las condiciones cambiantes de la ciudad.

2. Marco Teórico
**Sistemas Multiagente (MAS)**

Un MAS está compuesto por múltiples agentes autónomos que interactúan, colaboran o compiten para resolver problemas complejos. Cada agente posee autonomía, percepción del entorno, capacidad de decisión, comunicación con otros agentes.
En contextos logísticos o de transporte, los MAS ofrecen ventajas como descentralización, robustez ante fallos, adaptabilidad al cambio, toma de decisiones distribuida.

**JADE (Java Agent Development Framework)**

JADE es un framework FIPA-compliant que permite crear agentes inteligentes en Java. Proporciona contenedor de agentes, comportamiento basado en behaviours, comunicación con mensajes ACL, servicios de directorio DF.
Estas características hacen que JADE sea ideal para modelar flotas de vehículos como agentes cooperativos.

**FIPA Contract Net Protocol (CNP)**

Es un protocolo de negociación donde el agente iniciador anuncia una tarea (CFP). Los participantes envían propuestas (PROPOSE). El iniciador selecciona la mejor propuesta. Envía ACCEPT_PROPOSAL al ganador y REJECT_PROPOSAL al resto.En este proyecto, este protocolo modela la subasta entre vehículos para decidir quién realizará un viaje.

**Modelos Híbridos de Agentes**

Los agentes híbridos combinan 2 capas (capa reactiva: respuesta inmediata a estímulos, capa deliberativa: razonamiento estratégico).

Este enfoque es ideal para un vehículo que debe reaccionar al tráfico, pero también planificar sus movimientos.

3. Desarrollo

*3.1 Obtención y Preparación de Datos*

En esta versión del prototipo se trabaja con datos simulados:

- La ciudad es representada como un plano cartesiano.

- Las posiciones de clientes y vehículos son coordenadas (x, y).

- La distancia se calcula mediante distancia Manhattan o Euclidiana.

- Cada cierto tiempo se simula una nueva solicitud de viaje.

- Evita la dependencia de APIs externas y permite concentrarse en el funcionamiento del MAS.

*3.2 Preparación del Modelo Multiagente*

**Agente Vehículo**

Cada vehículo es un agente autónomo que posee una ubicación actual, conoce si está libre u ocupado, participa en subastas para decidir si toma un viaje. Implementa registro en DF, recepción de CFP, envío de ofertas (PROPOSE), aceptación de viajes.

**Agente Broker**

Es el agente coordinador que genera solicitudes de viaje cada cierto tiempo, consulta el DF para ubicar agentes disponibles, envía CFP, evalúa las ofertas, asigna el viaje y aplica una subasta tipo CNP.

3.3 Implementación del Prototipo (JADE / Java)

El sistema se compone de dos clases principales:

VehicleAgent.java

Incluye:

- Posición (x,y),

- Estado del vehículo,

- Behaviour de subasta.

El vehículo calcula una oferta basada en:

- bid = distancia_al_cliente

BrokerAgent.java

Incluye:

- Generación periódica de nuevas solicitudes,

- Envío de CFP,

- Recepción de propuestas,

- Selección del vehículo óptimo.

La versión entregada en el código divide claramente las responsabilidades siguiendo buenas prácticas de JADE.

3.4 Pruebas

Se realizaron pruebas controladas con:

- 5 agentes vehículo,

- 1 broker,

- solicitudes generadas cada 5 segundos.

Las pruebas evaluaron:

- Correcta comunicación ACL,

- Registro y descubrimiento en el DF,

- Cálculo de ofertas,

- Asignación de viajes continua.

Los agentes respondieron de forma autónoma y el intercambio de mensajes siguió correctamente el protocolo FIPA CNP.

4. Resultados

Los principales resultados del prototipo fueron:

✓ Asignación Dinámica Funcional

El sistema asigna cada solicitud al vehículo más cercano, logrando una reducción significativa del tiempo esperado para responder a cada viaje.

✓ Comportamiento Autónomo de Vehículos

Los vehículos:

- calculan ofertas,

- compiten por viajes,

- cambian de estado según asignación,

- continúan operando sin supervisión centralizada estricta.

✓ Comunicación Basada en Protocolos Estándar

La implementación con mensajes ACL y el protocolo CNP permitió una interacción ordenada y robusta entre agentes.

✓ Alto Potencial de Escalabilidad

La arquitectura permite fácilmente aumentar:

- número de vehículos,

- complejidad del mapa,

- métricas de optimización,

- integración con datos reales.

5. Conclusiones

El proyecto demostró que el uso de Sistemas Multiagente es una metodología efectiva para abordar problemas de optimización dinámica en flotas vehiculares. Gracias al uso de JADE y protocolos de negociación estándar, se implementó un prototipo funcional capaz de:

- Coordinar múltiples vehículos autónomos,

- Responder en tiempo real a solicitudes de viaje,

- Distribuir tareas mediante un mecanismo de subasta,

- Operar de manera descentralizada y flexible.

Aunque el sistema utiliza una lógica simplificada, los resultados muestran claramente su aplicabilidad en escenarios reales como taxis, transporte autónomo o plataformas de movilidad compartida.

Como trabajo futuro se podría:

- Mejorar los modelos de predicción de demanda,

- Aplicar heurísticas más avanzadas para el cálculo de ofertas,

- Integrar mapas reales y datos de tráfico,

- Evaluar estrategias cooperativas.
