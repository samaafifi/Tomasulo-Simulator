# Tomasulo Algorithm Implementation

A Java implementation of the Tomasulo algorithm for out-of-order instruction execution.

## Project Structure

```
Tomasulo/
├── pom.xml
├── README.md
├── .gitignore
└── src/
    ├── main/
    │   └── java/
    │       └── com/
    │           └── tomasulo/
    │               ├── registerfile/
    │               ├── hazards/
    │               └── models/
    └── test/
        └── java/
            └── com/
                └── tomasulo/
                    └── registerfile/
```

## Building

```bash
mvn clean compile
```

## Testing

```bash
mvn test
```

## Requirements

- Java 17
- Maven 3.6+

## Hazard Handling in Tomasulo's Algorithm

This implementation handles three types of data hazards that can occur in pipelined processors: RAW (Read After Write), WAR (Write After Read), and WAW (Write After Write). Tomasulo's algorithm uses register renaming and the Qi field to handle these hazards efficiently.

### 1. RAW Hazard (Read After Write) - True Data Dependency

**Definition**: A RAW hazard occurs when an instruction tries to read a register that a previous instruction is still writing to. This is a true data dependency that cannot be eliminated.

**Example**:
```java
Instruction 1: ADD.D F0, F1, F2    // Writes to F0
Instruction 2: MUL.D F3, F0, F4    // Reads from F0 (depends on Instruction 1)
```

**How It's Handled**:
- **Detection**: When issuing Instruction 2, the `DataHazardHandler.handleIssue()` method checks if source register F0 is ready.
- **Qi Field**: If F0 has a pending write (Qi != null), it means Instruction 1 hasn't completed yet. The register's `isReady()` method returns `false`.
- **Resolution**: Instead of providing the register value, the system returns the Qi tag (e.g., "Add1") to the reservation station. The reservation station waits for that station to broadcast its result on the Common Data Bus (CDB).
- **Write-Back**: When Instruction 1 completes and broadcasts its result via `writeBackFromStation("Add1", result)`, all registers waiting for "Add1" are updated, and Instruction 2 can proceed.

**Implementation Details**:
- `RegisterFile.isRegisterReady()` checks if `Qi == null`
- `DataHazardHandler.handleIssue()` returns `src1Qi` or `src2Qi` when registers are busy
- `RegisterFile.writeBackFromStation()` updates all waiting registers and clears their Qi fields

**Code Reference**: 
- `RegisterFile.readValue()` throws `IllegalStateException` if register is busy
- `DataHazardHandler.SourceOperands` contains `src1Qi`/`src2Qi` fields for pending dependencies

---

### 2. WAR Hazard (Write After Read) - Anti-Dependency

**Definition**: A WAR hazard occurs when an instruction writes to a register that a previous instruction is still reading from. This is an anti-dependency that can cause incorrect results if instructions execute out of order.

**Example**:
```java
Instruction 1: ADD.D F1, F0, F2    // Reads from F0
Instruction 2: MUL.D F0, F3, F4    // Writes to F0 (after Instruction 1 reads it)
```

**How It's Handled**:
- **Register Renaming**: Tomasulo's algorithm eliminates WAR hazards through register renaming using the Register Alias Table (RAT).
- **Issue Stage**: When Instruction 2 issues, it sets F0's Qi field to its station tag (e.g., "Mul1") via `RegisterFile.setQi("F0", "Mul1")`. This creates a binding in the RAT.
- **No Conflict**: Instruction 1 can still read the old value of F0 because it already has the value. Instruction 2's write is tracked separately via the Qi field.
- **Resolution**: When Instruction 2 completes, it writes back to F0, updating the register value. The RAT binding is cleared.

**Implementation Details**:
- `RegisterFile.setQi()` binds the register to a station in the RAT via `RegisterAliasTable.bind()`
- The RAT tracks which station will produce each register's value
- Multiple instructions can reference the same register without conflicts due to renaming

**Code Reference**:
- `RegisterAliasTable.bind()` creates register-to-station mappings
- `RegisterFile.setQi()` automatically updates the RAT

---

### 3. WAW Hazard (Write After Write) - Output Dependency

**Definition**: A WAW hazard occurs when two instructions write to the same register. The later instruction should overwrite the earlier one, but out-of-order execution can cause the wrong value to be written.

**Example**:
```java
Instruction 1: ADD.D F0, F1, F2    // Writes to F0
Instruction 2: MUL.D F0, F3, F4    // Also writes to F0 (should overwrite Instruction 1's result)
```

**How It's Handled**:
- **Register Renaming**: Like WAR hazards, WAW hazards are eliminated through register renaming.
- **Latest Write Wins**: When Instruction 2 issues, it overwrites F0's Qi field with its own station tag (e.g., "Mul1"). The previous Qi value ("Add1") is replaced.
- **RAT Update**: The RAT binding is updated to point to the new station. The old binding is overwritten.
- **Resolution**: Only Instruction 2's result will be written to F0 when it completes. Instruction 1's result is effectively discarded (or was never needed).

**Implementation Details**:
- `RegisterFile.setQi()` overwrites the previous Qi value and RAT binding
- The latest instruction to write a register "wins" - its Qi is the one that matters
- `RegisterAliasTable.bind()` uses `HashMap.put()`, which automatically overwrites previous bindings

**Code Reference**:
- `RegisterFile.setQi()` - second call overwrites first
- `RegisterAliasTable.bind()` - uses `Map.put()` for automatic overwriting

---

### Hazard Resolution Flow

1. **Issue Stage** (`DataHazardHandler.handleIssue()`):
   - Check source registers: if ready, return value; if busy, return Qi tag
   - Allocate destination register: set Qi field and create RAT binding
   - Return `SourceOperands` object with values/Qi tags

2. **Execution Stage**:
   - Reservation stations wait for operands (either values or Qi tags)
   - When a station broadcasts on CDB, it triggers write-back

3. **Write-Back Stage** (`DataHazardHandler.handleWriteBack()`):
   - `RegisterFile.writeBackFromStation()` updates all registers waiting for the station
   - Clears Qi fields for updated registers
   - Clears RAT bindings for the station
   - Resolves RAW hazards for dependent instructions

### Key Components

- **RegisterFile**: Manages 64 registers (F0-F31, R0-R31), tracks Qi fields, handles write-backs
- **RegisterAliasTable (RAT)**: Implements register renaming to eliminate WAR/WAW hazards
- **DataHazardHandler**: Coordinates register operations between Issue and Write-Back stages
- **Register**: Individual register with value, Qi field, and busy status

### Testing

All hazard types are tested in `HazardDetectionTest.java`:
- `testRAWHazard()` - Verifies RAW detection via Qi field
- `testWARHazard()` - Verifies WAR elimination via renaming
- `testWAWHazard()` - Verifies WAW elimination via renaming
- `testHazardResolutionAfterWriteBack()` - Verifies hazards resolve after write-back
- `testMultipleHazardsSameRegister()` - Verifies multiple WAW hazards handled correctly

See test files in `src/test/java/com/tomasulo/registerfile/` for complete examples.
