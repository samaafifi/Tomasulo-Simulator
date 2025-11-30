// File: InstructionType.java
package simulator.parser;

/**
 * CSEN702 Tomasulo Simulator - Complete Instruction Set
 * As specified by TA requirements document
 * 
 * INSTRUCTION CATEGORIES:
 * ======================
 * 1. Integer Arithmetic: DADDI, DSUBI
 * 2. Integer Memory: LW, LD, SW, SD
 * 3. FP Memory: L.S, L.D, S.S, S.D
 * 4. FP Arithmetic (Double): ADD.D, SUB.D, MUL.D, DIV.D
 * 5. FP Arithmetic (Single): ADD.S, SUB.S, MUL.S, DIV.S
 * 6. Branches: BEQ, BNE
 * 
 * TOTAL: 20 instructions
 */
public enum InstructionType {
    // ========== INTEGER ARITHMETIC ==========
    DADDI("DADDI", "integer"),
    DSUBI("DSUBI", "integer"),
    
    // ========== INTEGER MEMORY OPERATIONS ==========
    LW("LW", "load"),           // Load Word (4 bytes)
    LD("LD", "load"),           // Load Doubleword (8 bytes)
    SW("SW", "store"),          // Store Word (4 bytes)
    SD("SD", "store"),          // Store Doubleword (8 bytes)
    
    // ========== FLOATING-POINT MEMORY OPERATIONS ==========
    L_S("L.S", "fp_load"),      // Load FP Single (4 bytes)
    L_D("L.D", "fp_load"),      // Load FP Double (8 bytes)
    S_S("S.S", "fp_store"),     // Store FP Single (4 bytes)
    S_D("S.D", "fp_store"),     // Store FP Double (8 bytes)
    
    // ========== FLOATING-POINT ARITHMETIC - DOUBLE PRECISION ==========
    ADD_D("ADD.D", "fp_add"),   // FP Addition (Double)
    SUB_D("SUB.D", "fp_add"),   // FP Subtraction (Double) - uses same adder
    MUL_D("MUL.D", "fp_mul"),   // FP Multiplication (Double)
    DIV_D("DIV.D", "fp_div"),   // FP Division (Double)
    
    // ========== FLOATING-POINT ARITHMETIC - SINGLE PRECISION ==========
    ADD_S("ADD.S", "fp_add"),   // FP Addition (Single)
    SUB_S("SUB.S", "fp_add"),   // FP Subtraction (Single) - uses same adder
    MUL_S("MUL.S", "fp_mul"),   // FP Multiplication (Single)
    DIV_S("DIV.S", "fp_div"),   // FP Division (Single)
    
    // ========== BRANCH OPERATIONS ==========
    BEQ("BEQ", "branch"),       // Branch if Equal (no prediction)
    BNE("BNE", "branch");       // Branch if Not Equal (no prediction)

    private final String assemblyName;
    private final String category;

    InstructionType(String assemblyName, String category) {
        this.assemblyName = assemblyName;
        this.category = category;
    }

    public String getAssemblyName() {
        return assemblyName;
    }

    public String getCategory() {
        return category;
    }

    // ========== CATEGORY CHECKING METHODS ==========
    
    public boolean isInteger()        { return "integer".equals(category); }
    public boolean isFpAdd()          { return "fp_add".equals(category); }
    public boolean isFpMul()          { return "fp_mul".equals(category); }
    public boolean isFpDiv()          { return "fp_div".equals(category); }
    public boolean isFpLoad()         { return "fp_load".equals(category); }
    public boolean isFpStore()        { return "fp_store".equals(category); }
    public boolean isLoad()           { return "load".equals(category) || isFpLoad(); }
    public boolean isStore()          { return "store".equals(category) || isFpStore(); }
    public boolean isBranch()         { return "branch".equals(category); }
    public boolean isFloatingPoint()  { return category.startsWith("fp_"); }

    /**
     * Returns true if instruction accesses memory (load or store)
     */
    public boolean isMemoryOperation() {
        return isLoad() || isStore();
    }

    /**
     * Returns true if instruction uses floating-point registers (F0-F31)
     */
    public boolean usesFPRegisters() {
        return isFloatingPoint();
    }

    /**
     * Returns true if instruction uses integer registers (R0-R31)
     */
    public boolean usesIntegerRegisters() {
        return isInteger() || "load".equals(category) || "store".equals(category) || isBranch();
    }

    /**
     * Determines if this is a single-precision FP operation
     */
    public boolean isSinglePrecision() {
        return this == ADD_S || this == SUB_S || this == MUL_S || this == DIV_S ||
               this == L_S || this == S_S;
    }

    /**
     * Determines if this is a double-precision FP operation
     */
    public boolean isDoublePrecision() {
        return this == ADD_D || this == SUB_D || this == MUL_D || this == DIV_D ||
               this == L_D || this == S_D;
    }

    /**
     * Returns the number of bytes accessed by this instruction
     * Used for memory operations and cache simulation
     */
    public int getMemoryAccessSize() {
        return switch (this) {
            case LW, SW, L_S, S_S -> 4;  // Word/Single = 4 bytes
            case LD, SD, L_D, S_D -> 8;  // Doubleword/Double = 8 bytes
            default -> 0;                 // Non-memory operations
        };
    }

    /**
     * Parse assembly mnemonic to InstructionType
     * Handles case-insensitive matching and whitespace trimming
     * 
     * Examples:
     *   "MUL.D" → MUL_D
     *   "add.s" → ADD_S
     *   "beq  " → BEQ
     *   "L.D"   → L_D
     * 
     * @param text The assembly mnemonic
     * @return The corresponding InstructionType
     * @throws IllegalArgumentException if instruction is not recognized
     */
    public static InstructionType fromAssemblyName(String text) {
        if (text == null || text.trim().isEmpty()) {
            throw new IllegalArgumentException("Instruction mnemonic cannot be empty");
        }

        String normalized = text.trim().toUpperCase();

        for (InstructionType type : values()) {
            if (type.assemblyName.toUpperCase().equals(normalized)) {
                return type;
            }
        }

        // Build helpful error message
        throw new IllegalArgumentException(
            "Unknown instruction: '" + text + "'\n" +
            "Supported instructions: DADDI, DSUBI, " +
            "LW, LD, SW, SD, L.S, L.D, S.S, S.D, " +
            "ADD.D, ADD.S, SUB.D, SUB.S, MUL.D, MUL.S, DIV.D, DIV.S, " +
            "BEQ, BNE"
        );
    }

    /**
     * Returns the reservation station type needed for this instruction
     * Used by the issue stage to allocate the correct RS
     * 
     * Note: Single and double precision operations use the SAME reservation stations
     * (they share ADD, MUL, DIV units)
     */
    public String getReservationStationType() {
        return switch (this) {
            case DADDI, DSUBI -> "INTEGER";
            case ADD_D, SUB_D, ADD_S, SUB_S -> "ADD";
            case MUL_D, MUL_S -> "MUL";
            case DIV_D, DIV_S -> "DIV";
            case LW, LD, L_S, L_D -> "LOAD";
            case SW, SD, S_S, S_D -> "STORE";
            case BEQ, BNE -> "BRANCH";
        };
    }

    /**
     * Returns a human-readable description of the instruction
     */
    public String getDescription() {
        return switch (this) {
            case DADDI -> "Add immediate to integer register";
            case DSUBI -> "Subtract immediate from integer register";
            case LW -> "Load word (4 bytes) from memory";
            case LD -> "Load doubleword (8 bytes) from memory";
            case SW -> "Store word (4 bytes) to memory";
            case SD -> "Store doubleword (8 bytes) to memory";
            case L_S -> "Load single-precision FP from memory";
            case L_D -> "Load double-precision FP from memory";
            case S_S -> "Store single-precision FP to memory";
            case S_D -> "Store double-precision FP to memory";
            case ADD_D -> "Add two double-precision FP numbers";
            case ADD_S -> "Add two single-precision FP numbers";
            case SUB_D -> "Subtract two double-precision FP numbers";
            case SUB_S -> "Subtract two single-precision FP numbers";
            case MUL_D -> "Multiply two double-precision FP numbers";
            case MUL_S -> "Multiply two single-precision FP numbers";
            case DIV_D -> "Divide two double-precision FP numbers";
            case DIV_S -> "Divide two single-precision FP numbers";
            case BEQ -> "Branch if registers are equal";
            case BNE -> "Branch if registers are not equal";
        };
    }

    /**
     * Returns the typical latency category for this instruction
     * (Actual latencies are user-configurable in the simulator)
     */
    public String getLatencyCategory() {
        return switch (this) {
            case DADDI, DSUBI, BEQ, BNE -> "Fast (1-2 cycles)";
            case LW, LD, L_S, L_D, SW, SD, S_S, S_D -> "Memory-dependent (cache latency)";
            case ADD_D, ADD_S, SUB_D, SUB_S -> "Medium (2-4 cycles)";
            case MUL_D, MUL_S -> "Slow (10-15 cycles)";
            case DIV_D, DIV_S -> "Very Slow (20-40 cycles)";
        };
    }
}