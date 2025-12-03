# MAS-OptimizacionFlotas-CST_TF

## 1. Contexto de la Problemática

El proyecto implementa una solución al problema de **Optimización Dinámica de Flotas y Asignación de Viajes** aplicando la disciplina de **Sistemas Multiagente (MAS)**.

* **Objetivo Principal**: Maximizar la eficiencia operativa y minimizar el tiempo de espera del cliente mediante la coordinación descentralizada.
* **Problema Central**: La asignación eficiente de servicios de transporte a una flota de vehículos autónomos en un entorno dinámico.

### Metodología de Solución

Se utiliza un mecanismo de **Coordinación Negociada** a través del protocolo de subasta simplificado **FIPA-Contract Net Protocol (FIPA-CNP)**.

* El `BrokerAgent` (Iniciador) selecciona la oferta óptima (el *bid* mínimo, basado en la **distancia**) presentada por los `VehicleAgent` (Participantes).
* **Implementación**: La solución está desarrollada sobre el *framework* **JADE**, utilizando los servicios de **DF** (*Directory Facilitator*) y el lenguaje **ACL** (*Agent Communication Language*).

---

## 2. Comandos para Ejecución

Todos los comandos deben ser ejecutados desde el directorio **`TF-TCC/`** del repositorio. El uso de la opción `-gui` es obligatorio para iniciar la Consola RMA, y la inclusión de `GuiAgent` es esencial para la visualización gráfica de la simulación.

### Comando Manual Completo (5 Vehículos, con GUI)

Para iniciar el sistema manualmente con 5 agentes de vehículos:

java -cp "bin;lib\jade.jar" jade.Boot -gui -agents gui:swarmintelligence.GuiAgent;visualizer:swarmintelligence.VisualiserAgent;broker:swarmintelligence.BrokerAgent;v1:swarmintelligence.VehicleAgent;v2:swarmintelligence.VehicleAgent;v3:swarmintelligence.VehicleAgent;v4:swarmintelligence.VehicleAgent;v5:swarmintelligence.VehicleAgent

### Ejecución Mediante Script (Recomendado)

Se recomienda usar el *script* de lotes `run_agents.bat` (o `run_agents.sh`) para automatizar la compilación y el inicio, incluyendo el argumento "gui".

**Ejemplo de uso:**

`.\run_agents.bat 5 gui`

---

## 3. Declaración de Uso de IA

Se declara formalmente el uso de herramientas de **Inteligencia Artificial (IA)** para la generación inicial y la depuración del código base de los agentes `BrokerAgent.java` y `VehicleAgent.java`, así como su posterior corrección durante la aparición de errores.
