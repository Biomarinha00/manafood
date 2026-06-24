@echo off
title Mana Food - Tunnel
echo Iniciando tunnel para acesso externo...
echo O link publico aparecera abaixo:
echo.
cd /d "%~dp0"
cloudflared-windows-amd64.exe tunnel --url http://localhost:5000
pause
