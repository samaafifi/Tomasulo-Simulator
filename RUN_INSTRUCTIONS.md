# How to Run the Tomasulo Simulator

## Prerequisites
- Java 21 or higher installed
- Maven (optional, but recommended)

## Method 1: Using Maven (Recommended)

### Step 1: Compile the project
```bash
mvn clean compile
```

### Step 2: Run the IntegrationTest
```bash
mvn exec:java -Dexec.mainClass="simulator.tests.IntegrationTest"
```

Or compile and run in one step:
```bash
mvn clean compile exec:java -Dexec.mainClass="simulator.tests.IntegrationTest"
```

## Method 2: Using javac and java directly

### Step 1: Navigate to project root
```bash
cd "C:\Users\hp450 g3\Desktop\Tomasulo-Simulator"
```

### Step 2: Compile all Java files
```bash
javac -d bin -sourcepath src/main/java src/main/java/simulator/tests/IntegrationTest.java
```

### Step 3: Run the test
```bash
java -cp bin simulator.tests.IntegrationTest
```

## Method 3: Using an IDE (IntelliJ IDEA, Eclipse, VS Code)

1. **Open the project** in your IDE
2. **Navigate to**: `src/main/java/simulator/tests/IntegrationTest.java`
3. **Right-click** on the file or the `main` method
4. **Select "Run"** or "Run IntegrationTest"

## Method 4: Quick Run Script (Windows)

Create a file `run.bat` in the project root:

```batch
@echo off
cd /d "%~dp0"
javac -d bin -sourcepath src/main/java src/main/java/simulator/tests/IntegrationTest.java
if %ERRORLEVEL% EQU 0 (
    java -cp bin simulator.tests.IntegrationTest
) else (
    echo Compilation failed!
    pause
)
```

Then double-click `run.bat` to run.

## What the Test Does

The IntegrationTest runs Test Case 1:
```
L.D F6, 0(R2)
L.D F2, 8(R2)
MUL.D F0, F2, F4
SUB.D F8, F2, F6
DIV.D F10, F0, F6
ADD.D F6, F8, F2
S.D F6, 8(R2)
```

It will display:
- Cycle-by-cycle execution
- Reservation station status
- Register file status
- Instruction progress (issue, execution start, completion)
- Final results

## Other Main Classes Available

- `simulator.tomasulo.execute.IntegratedSimulationEngine` - Simple simulation engine test
- `simulator.parser.InstructionParser` - Parser test

To run these, replace `IntegrationTest` with the class name in the commands above.

