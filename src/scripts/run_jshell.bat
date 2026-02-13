@echo off
echo [INFO] Acessando a raiz do projeto...
cd ..

echo [INFO] Compilando arquivos...
javac lib/*.java QueryShell.java

if %errorlevel% neq 0 (
    echo [ERRO] Falha na compilacao.
    pause
    exit /b
)

echo [SUCESSO] Abrindo JShell...
jshell --class-path .