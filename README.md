# MAS-OptimizacionFlotas-CST_TF

**Compilación manual:**
```batch
cd TF-TCC
javac -encoding UTF-8 -cp ".;lib\jade.jar" -d bin src\swarmintelligence\*.java
```

**Ejecución con diferentes configuraciones:**

```batch
# Configuración mínima (3 vehículos, sin GUI JADE)
.\run_agents.bat 3

# Configuración estándar (5 vehículos, con GUI JADE)
.\run_agents.bat 5 gui

# Configuración de alta capacidad (10 vehículos)
.\run_agents.bat 10 gui
```

**Verificación de JADE:**
```batch
java -cp lib\jade.jar jade.Boot -version
```

---
