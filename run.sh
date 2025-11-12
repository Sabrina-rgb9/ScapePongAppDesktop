#!/bin/bash

cd "$(dirname "$0")/.."

echo "🚀 Ejecutando Cliente Consola..."
echo "🔗 Conectando a: localhost:3000"
echo "----------------------------------------"

mvn clean compile exec:java -Dexec.mainClass="com.broadcast.client.ConsoleClient"