@echo off
title Mana Food - Atualizador
color 0B
echo.
echo  ============================================
echo     MANA FOOD - ATUALIZADOR
echo  ============================================
echo.
echo  Atualizando sistema sem mexer nos dados...
echo.

:: Verifica se tem instalacao
if not exist "C:\ManaFood\server.py" (
    color 0C
    echo  [ERRO] Sistema nao instalado!
    echo  Execute INSTALAR.bat primeiro.
    pause
    exit
)

:: Faz backup do banco antes de atualizar
if exist "C:\ManaFood\lanchonete\data\lanchonete.db" (
    for /f "tokens=1-3 delims=/ " %%a in ('date /t') do set DATA=%%c%%b%%a
    for /f "tokens=1-2 delims=: " %%a in ('time /t') do set HORA=%%a%%b
    copy "C:\ManaFood\lanchonete\data\lanchonete.db" "C:\ManaFood\lanchonete\backups\pre_update_%DATA%_%HORA%.db" >nul 2>&1
    echo  [OK] Backup do banco criado
)

:: Atualiza apenas servidor e frontend (preserva banco, imagens, backups)
copy /Y "%~dp0server.py" "C:\ManaFood\server.py" >nul
copy /Y "%~dp0interface\index.html" "C:\ManaFood\interface\index.html" >nul
copy /Y "%~dp0icon-192.png" "C:\ManaFood\icon-192.png" >nul 2>&1
copy /Y "%~dp0manifest.json" "C:\ManaFood\manifest.json" >nul 2>&1

echo  [OK] Arquivos atualizados!
echo.
echo  ============================================
echo     ATUALIZACAO CONCLUIDA!
echo  ============================================
echo.
echo  Reinicie o servidor para aplicar.
echo.
pause
