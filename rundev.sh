#!/bin/bash

# Cargar variables de entorno
set -a
source .env
set +a

# Arrancar base de datos
docker compose up -d

# Ejecutar la aplicación con livereloading
./gradlew --no-daemon bootRun
