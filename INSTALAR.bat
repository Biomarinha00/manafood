@echo off
title Mana Food - Instalador
color 0A
echo.
echo  ============================================
echo     MANA FOOD - INSTALADOR v4.0
echo  ============================================
echo.

:: Verifica Python
python --version >nul 2>&1
if %errorlevel% neq 0 (
    color 0C
    echo  [ERRO] Python nao encontrado!
    echo  Baixe em: https://python.org/downloads
    echo  Marque "Add Python to PATH" na instalacao!
    echo.
    pause
    exit
)
echo  [OK] Python encontrado!

:: Instala dependencias
echo  Instalando dependencias...
python -m pip install pystray pillow --quiet --disable-pip-version-check >nul 2>&1
echo  [OK] Dependencias instaladas!

:: Cria pasta de instalacao
if not exist "C:\ManaFood" mkdir "C:\ManaFood"
if not exist "C:\ManaFood\interface" mkdir "C:\ManaFood\interface"
if not exist "C:\ManaFood\lanchonete" mkdir "C:\ManaFood\lanchonete"
if not exist "C:\ManaFood\lanchonete\data" mkdir "C:\ManaFood\lanchonete\data"
if not exist "C:\ManaFood\lanchonete\imagens" mkdir "C:\ManaFood\lanchonete\imagens"
if not exist "C:\ManaFood\lanchonete\backups" mkdir "C:\ManaFood\lanchonete\backups"

:: Copia arquivos
copy /Y "%~dp0server.py" "C:\ManaFood\server.py" >nul
copy /Y "%~dp0mana.py" "C:\ManaFood\mana.py" >nul
copy /Y "%~dp0interface\index.html" "C:\ManaFood\interface\index.html" >nul
copy /Y "%~dp0logo.ico" "C:\ManaFood\logo.ico" >nul 2>&1
copy /Y "%~dp0icon-192.png" "C:\ManaFood\icon-192.png" >nul 2>&1
copy /Y "%~dp0icon-512.png" "C:\ManaFood\icon-512.png" >nul 2>&1
copy /Y "%~dp0manifest.json" "C:\ManaFood\manifest.json" >nul 2>&1
copy /Y "%~dp0INICIAR.bat" "C:\ManaFood\INICIAR.bat" >nul
copy /Y "%~dp0ATUALIZAR.bat" "C:\ManaFood\ATUALIZAR.bat" >nul 2>&1

echo  [OK] Arquivos instalados em C:\ManaFood\

:: Cria atalho na area de trabalho (usando pythonw pra rodar sem terminal)
for /f "delims=" %%i in ('where pythonw 2^>nul') do set PYTHONW=%%i
if "%PYTHONW%"=="" (
    for /f "delims=" %%i in ('where python') do set PYTHONW=%%~dpi\pythonw.exe
)

echo Set oWS = WScript.CreateObject("WScript.Shell") > "%temp%\atalho.vbs"
echo sLinkFile = oWS.SpecialFolders("Desktop") ^& "\Mana Food.lnk" >> "%temp%\atalho.vbs"
echo Set oLink = oWS.CreateShortcut(sLinkFile) >> "%temp%\atalho.vbs"
echo oLink.TargetPath = "%PYTHONW%" >> "%temp%\atalho.vbs"
echo oLink.Arguments = """C:\ManaFood\mana.py""" >> "%temp%\atalho.vbs"
echo oLink.WorkingDirectory = "C:\ManaFood" >> "%temp%\atalho.vbs"
echo oLink.IconLocation = "C:\ManaFood\logo.ico" >> "%temp%\atalho.vbs"
echo oLink.Description = "Mana Food - PDV" >> "%temp%\atalho.vbs"
echo oLink.Save >> "%temp%\atalho.vbs"
cscript //nologo "%temp%\atalho.vbs"
del "%temp%\atalho.vbs"
echo  [OK] Atalho criado na area de trabalho!

echo.
echo  ============================================
echo     INSTALACAO CONCLUIDA!
echo  ============================================
echo.
echo  Clique em "Mana Food" na area de trabalho
echo  para iniciar o sistema.
echo.
echo  O servidor abre na bandeja do sistema.
echo.
pause
