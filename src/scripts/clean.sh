#!/bin/bash
cd "$(dirname "$0")/.."
echo "Limpando .class..."
find . -name "*.class" -type f -delete
echo "Concluído."