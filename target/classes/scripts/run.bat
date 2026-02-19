@echo off
REM ================================================================
REM main.java.lib.QueryShell - Compilar e Executar
REM Localizacao: src\main.resources.scripts\run_queryshell.bat
REM ================================================================

REM Voltar para o diretorio raiz do projeto (2 niveis acima)
cd ..\..

echo ================================================================
echo   main.java.lib.QueryShell - Compilacao e Execucao
echo ================================================================
echo.

REM ================================================================
REM COMPILAR
REM ================================================================

echo [1/2] Compilando...
javac -d out\production\stupid_bd ^
      -cp "C:\Users\loona\.m2\repository\org\antlr\antlr4-runtime\4.13.1\antlr4-runtime-4.13.1.jar;out\production\stupid_bd" ^
      src\lib\network\*.java


if %errorlevel% neq 0 (
    echo.
    echo [ERRO] Compilacao falhou!
    pause
    exit /b 1
)

echo [OK] Compilacao concluida!
echo.

REM ================================================================
REM EXECUTAR
REM ================================================================

echo [2/2] Iniciando main.java.lib.QueryShell...
echo ================================================================
echo.

java -cp "out\production\stupid_bd;C:\Users\loona\.m2\repository\org\antlr\antlr4-runtime\4.13.1\antlr4-runtime-4.13.1.jar" network.main.lib.SQLServer

echo.
echo ================================================================
echo main.java.lib.QueryShell finalizado
echo ================================================================
pause