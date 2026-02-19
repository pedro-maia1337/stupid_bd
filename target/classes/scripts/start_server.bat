@echo off
REM ================================================================
REM Script para gerenciar o servidor SQL
REM ================================================================

echo ╔════════════════════════════════════════════════╗
echo ║         SQL Server Manager                     ║
echo ╚════════════════════════════════════════════════╝
echo.

REM Verificar se porta 5432 está em uso
echo Verificando porta 5432...
netstat -ano | findstr ":5432" > temp_port_check.txt

if %errorlevel% equ 0 (
    echo.
    echo [AVISO] Porta 5432 em uso por outro processo:
    type temp_port_check.txt
    echo.
    
    REM Extrair PID
    for /f "tokens=5" %%a in ('netstat -ano ^| findstr ":5432" ^| findstr "LISTENING"') do (
        set PID=%%a
    )
    
    if defined PID (
        echo Processo ocupando a porta: PID=%PID%
        echo.
        echo Opcoes:
        echo   1. Matar processo (taskkill /F /PID %PID%)
        echo   2. Usar porta alternativa
        echo   3. Cancelar
        echo.
        set /p choice="Escolha (1/2/3): "
        
        if "!choice!"=="1" (
            echo.
            echo Matando processo %PID%...
            taskkill /F /PID %PID%
            timeout /t 2 > nul
            echo.
            echo Iniciando servidor na porta 5432...
            java -cp "out\production\stupid_bd;C:\Users\loona\.m2\repository\org\antlr\antlr4-runtime\4.13.1\antlr4-runtime-4.13.1.jar" network.main.lib.SQLServer 5432
        ) else if "!choice!"=="2" (
            echo.
            echo Iniciando servidor em porta alternativa...
            java -cp "out\production\stupid_bd;C:\Users\loona\.m2\repository\org\antlr\antlr4-runtime\4.13.1\antlr4-runtime-4.13.1.jar" network.main.lib.SQLServer 5433
        ) else (
            echo.
            echo Cancelado.
            pause
            exit /b 0
        )
    )
) else (
    echo [OK] Porta 5432 disponivel!
    echo.
    echo Iniciando servidor...
    java -cp "out\production\stupid_bd;C:\Users\loona\.m2\repository\org\antlr\antlr4-runtime\4.13.1\antlr4-runtime-4.13.1.jar" network.main.lib.SQLServer 5432
)

del temp_port_check.txt 2>nul
pause
