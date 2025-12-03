@echo off
REM run_agents.bat
REM Uso: run_agents.bat [N_VEHICLES] [gui]
REM   N_VEHICLES: nº de VehicleAgent a lanzar (por defecto 3)
REM   gui: si pasas "gui" (cualquier mayúsc/minúsc) se añade GuiAgent para abrir MainFrame
REM Ejemplos:
REM   run_agents.bat
REM   run_agents.bat 5
REM   run_agents.bat 5 gui

SETLOCAL ENABLEDELAYEDEXPANSION

REM --- Parámetros y rutas ---
SET NUM_VEHICLES=%1
IF "%NUM_VEHICLES%"=="" SET NUM_VEHICLES=3

SET WANT_GUI=%2

SET JADE_JAR=lib\jade.jar
SET SRC_DIR=src
SET BIN_DIR=bin
SET PKG=swarmintelligence

REM --- Ir al directorio del script (para rutas relativas consistentes) ---
PUSHD "%~dp0"

REM --- Comprobaciones básicas ---
echo Verificando Java...
java -version >nul 2>&1
IF ERRORLEVEL 1 (
  echo ERROR: Java no esta disponible en PATH. Instala JDK y agrega javac/java a PATH.
  PAUSE
  POPD
  EXIT /B 1
)

echo Verificando que exista %JADE_JAR%...
IF NOT EXIST "%JADE_JAR%" (
  echo ERROR: No se encontro %JADE_JAR% en "%JADE_JAR%". Coloca jade.jar en la carpeta lib\.
  PAUSE
  POPD
  EXIT /B 1
)

REM --- Preparar carpeta bin ---
if not exist "%BIN_DIR%" mkdir "%BIN_DIR%"

REM --- Compilar todos los .java del paquete en una sola llamada ---
echo Compilando fuentes Java (todos a la vez)...
javac -cp "%JADE_JAR%" -d "%BIN_DIR%" %SRC_DIR%\%PKG%\*.java
IF ERRORLEVEL 1 (
  echo.
  echo ERROR: la compilacion fallo. Revisa los errores mostrados arriba.
  PAUSE
  POPD
  EXIT /B 1
)
echo Compilacion exitosa.

REM --- Construir cadena de agentes ---
SET AGENTS=visualizer:%PKG%.VisualiserAgent;broker:%PKG%.BrokerAgent

IF /I "%WANT_GUI%"=="gui" (
  SET AGENTS=%AGENTS%;gui:%PKG%.GuiAgent
)

REM Añadir N VehicleAgent
FOR /L %%i IN (1,1,%NUM_VEHICLES%) DO (
  SET AGENTS=!AGENTS!;v%%i:%PKG%.VehicleAgent
)

echo.
echo Iniciando JADE con agentes:
echo   %AGENTS%
echo.

REM --- Ejecutar JADE (abrirá RMA con -gui) ---
java -cp "%BIN_DIR%;%JADE_JAR%" jade.Boot -gui -agents %AGENTS%

echo JADE finalizo o se cerro. Presiona una tecla para salir.
PAUSE
POPD
ENDLOCAL