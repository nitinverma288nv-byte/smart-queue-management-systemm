@echo off
title Smart Queue Management System Launcher
echo ======================================================================
echo           STARTING SMART QUEUE MANAGEMENT SYSTEM
echo ======================================================================
echo.

:: Checking if ports are already in use
echo [1/3] Checking ports...
netstat -ano | findstr :9999 >nul
if %errorlevel% equ 0 (
    echo [WARNING] Port 9999 (Backend) is already in use. Attempting to kill it...
    for /f "tokens=5" %%a in ('netstat -aon ^| findstr :9999') do taskkill /f /pid %%a 2>nul
)

netstat -ano | findstr :5173 >nul
if %errorlevel% equ 0 (
    echo [WARNING] Port 5173 (Frontend) is already in use. Attempting to kill it...
    for /f "tokens=5" %%a in ('netstat -aon ^| findstr :5173') do taskkill /f /pid %%a 2>nul
)

:: Starting Spring Boot Backend
echo [2/3] Starting Backend Server on port 9999...
if exist ".\apache-maven-3.9.9\bin\mvn.cmd" (
    start "Smart Queue Backend" cmd /k "color 0A && cd backend && ..\apache-maven-3.9.9\bin\mvn.cmd spring-boot:run"
) else (
    start "Smart Queue Backend" cmd /k "color 0A && cd backend && mvn spring-boot:run"
)

:: Starting React Frontend (Vite)
echo [3/3] Starting Frontend Server on port 5173...
cd frontend
if exist "..\node_portable\node-v22.12.0-win-x64\npm.cmd" (
    if not exist "node_modules" (
        echo [INFO] node_modules not found. Running npm install...
        call ..\node_portable\node-v22.12.0-win-x64\npm.cmd install
    )
    start "Smart Queue Frontend" cmd /k "color 0B && ..\node_portable\node-v22.12.0-win-x64\npm.cmd run dev"
) else (
    if not exist "node_modules" (
        echo [INFO] node_modules not found. Running npm install...
        call npm install
    )
    start "Smart Queue Frontend" cmd /k "color 0B && npm run dev"
)
cd ..

echo.
echo ======================================================================
echo  SUCCESS: Backend and Frontend are starting in separate windows!
echo  - Backend URL: http://localhost:9999/swagger-ui.html (OpenAPI)
echo  - Frontend URL: http://localhost:5173 (Web App)
echo ======================================================================
echo.
pause
