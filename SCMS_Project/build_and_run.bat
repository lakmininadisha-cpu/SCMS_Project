@echo off
REM ============================================================
REM  SCMS Build & Run Script  (Windows)
REM  Requires: JDK 17+  (javac must be on PATH)
REM ============================================================

echo ==============================================
echo  Smart Campus Management System - Build Tool
echo ==============================================

where javac >nul 2>&1
IF ERRORLEVEL 1 (
    echo ERROR: javac not found. Install JDK 17+ and add to PATH.
    pause
    exit /b 1
)

SET PROJECT_ROOT=%~dp0
SET SRC=%PROJECT_ROOT%src\main\java
SET OUT=%PROJECT_ROOT%out

if not exist "%OUT%" mkdir "%OUT%"

echo.
echo ==^> Compiling sources...
for /R "%SRC%" %%f in (*.java) do (
    echo     %%f
)
dir /s /b "%SRC%\*.java" > "%PROJECT_ROOT%sources.txt"
javac --release 17 -d "%OUT%" @"%PROJECT_ROOT%sources.txt"
del "%PROJECT_ROOT%sources.txt"
echo     Compilation successful.

echo.
echo ==^> Running standalone test suite...
java -cp "%OUT%" scms.test.TestRunner

echo.
echo ==^> Creating runnable JAR...
cd "%OUT%"
echo Main-Class: scms.Main > manifest.txt
jar cfm "%PROJECT_ROOT%SCMS.jar" manifest.txt .
del manifest.txt
echo     Created: SCMS.jar

echo.
echo ==^> Launching application...
echo     (Press Ctrl+C to exit at any time)
echo.
java -cp "%OUT%" scms.Main

pause
