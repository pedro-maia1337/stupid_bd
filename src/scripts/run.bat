@echo off
REM ================================================================
REM QueryShell - Compilar e Executar
REM Localizacao: src\scripts\run_queryshell.bat
REM ================================================================

REM Voltar para o diretorio raiz do projeto (2 niveis acima)
cd ..\..

echo ================================================================
echo   QueryShell - Compilacao e Execucao
echo ================================================================
echo.

REM ================================================================
REM COMPILAR
REM ================================================================

echo [1/2] Compilando...
javac -d out\production\stupid_bd ^
      -cp "C:\Users\loona\.m2\repository\org\antlr\antlr4-runtime\4.13.1\antlr4-runtime-4.13.1.jar" ^
      src\lib\persistence\*.java ^
      src\lib\index\*.java ^
      src\lib\*.java ^
      src\lib\parser\*.java ^
      src\QueryShell.java


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

echo [2/2] Iniciando QueryShell...
echo ================================================================
echo.

java -cp "out\production\stupid_bd;C:\Users\loona\.m2\repository\org\antlr\antlr4-runtime\4.13.1\antlr4-runtime-4.13.1.jar" QueryShell

echo.
echo ================================================================
echo QueryShell finalizado
echo ================================================================
pause