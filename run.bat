@echo off
echo ========================================
echo Tomasulo Simulator - Running Test
echo ========================================
echo.

cd /d "%~dp0"

echo Compiling project...
javac -d bin -sourcepath src/main/java src/main/java/simulator/tests/IntegrationTest.java

if %ERRORLEVEL% EQU 0 (
    echo.
    echo Compilation successful!
    echo.
    echo Running IntegrationTest...
    echo ========================================
    echo.
    java -cp bin simulator.tests.IntegrationTest
) else (
    echo.
    echo Compilation failed! Please check for errors above.
    pause
    exit /b 1
)

echo.
echo ========================================
echo Test completed!
pause

