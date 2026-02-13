@echo off
cd ..
echo [INFO] Limpando arquivos compilados na raiz e subpastas...

del /s /q *.class >nul 2>&1

echo [OK] Tudo limpo.
pause