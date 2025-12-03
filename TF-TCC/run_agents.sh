#!/usr/bin/env bash
# Coloca este archivo en proyecto-root/run_agents.sh y dale permisos de ejecución (chmod +x run_agents.sh)
# Uso: ./run_agents.sh [N_VEHICLES]
# Ejemplo: ./run_agents.sh 5

NUM_VEHICLES=${1:-3}
JADE_JAR="lib/jade.jar"
SRC_DIR="src"
BIN_DIR="bin"
PKG="swarmintelligence"

# 1) Compilar
echo "Compilando fuentes..."
mkdir -p "$BIN_DIR"
javac -cp "$JADE_JAR" -d "$BIN_DIR" $SRC_DIR/$PKG/*.java
if [ $? -ne 0 ]; then
  echo "Error en compilación. Revisa los errores arriba."
  exit 1
fi
echo "Compilación OK."

# 2) Construir cadena de agentes
AGENTS="visualizer:${PKG}.VisualiserAgent;broker:${PKG}.BrokerAgent"
for i in $(seq 1 $NUM_VEHICLES); do
  AGENTS="${AGENTS};v${i}:${PKG}.VehicleAgent"
done

echo "Iniciando JADE con agentes: $AGENTS"
java -cp "$BIN_DIR:$JADE_JAR" jade.Boot -gui -agents "$AGENTS"