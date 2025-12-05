@echo off
REM Ensure we use Java 17
set JAVA_HOME=
for /f "tokens=*" %%i in ('where java') do (
    set JAVA_PATH=%%i
    goto :found
)
:found
echo Using Java: %JAVA_PATH%
java -version

REM Clean and compile
call mvn clean compile

REM Run with explicit Java 17
java --module-path "%USERPROFILE%\.m2\repository\org\openjfx\javafx-controls\21\javafx-controls-21-win.jar;%USERPROFILE%\.m2\repository\org\openjfx\javafx-fxml\21\javafx-fxml-21-win.jar;%USERPROFILE%\.m2\repository\org\openjfx\javafx-graphics\21\javafx-graphics-21-win.jar;%USERPROFILE%\.m2\repository\org\openjfx\javafx-base\21\javafx-base-21-win.jar" --add-modules javafx.controls,javafx.fxml -cp "target\classes;%USERPROFILE%\.m2\repository\org\openjfx\javafx-controls\21\javafx-controls-21-win.jar;%USERPROFILE%\.m2\repository\org\openjfx\javafx-fxml\21\javafx-fxml-21-win.jar;%USERPROFILE%\.m2\repository\org\openjfx\javafx-graphics\21\javafx-graphics-21-win.jar;%USERPROFILE%\.m2\repository\org\openjfx\javafx-base\21\javafx-base-21-win.jar" simulator.gui.TomasuloSimulatorGUI
