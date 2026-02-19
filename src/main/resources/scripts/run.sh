#!/bin/bash

cd ../..

echo "Compilando projeto..."

mkdir -p out/production/stupid_bd

javac -d out/production/stupid_bd \
  -cp "/home/loona/.m2/repository/org/antlr/antlr4-runtime/4.13.1/antlr4-runtime-4.13.1.jar" \
  $(find src -name "*.java")

if [ $? -ne 0 ]; then
    echo "[ERRO] Compilacao falhou!"
    exit 1
fi

echo "Compilacao concluida!"

echo "Executando servidor..."

java -cp "out/production/stupid_bd:/home/loona/.m2/repository/org/antlr/antlr4-runtime/4.13.1/antlr4-runtime-4.13.1.jar" \
  lib.network.SQLServer
