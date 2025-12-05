package simulator.tomasulo.models;

/**
 * Represents an instruction in the Tomasulo algorithm.
 * Used for testing and instruction representation.
 */
public class Instruction {
    private String operation;
    private String destRegister;
    private String sourceReg1;
    private String sourceReg2;
    private String stationTag;
    private int id;
    private int issueCycle;
    private String baseRegister;
    private int offset;
    private int immediate;
    private boolean hasImmediate;

    /**
     * Constructs a new Instruction with the specified parameters.
     *
     * @param operation the operation name (e.g., "ADD.D", "MUL.D", "L.D")
     * @param destRegister the destination register (can be null for stores/branches)
     * @param sourceReg1 the first source register
     * @param sourceReg2 the second source register (can be null)
     * @param stationTag the reservation station tag (e.g., "Add1", "Mul2")
     */
    public Instruction(String operation, String destRegister, String sourceReg1, 
                      String sourceReg2, String stationTag) {
        this.operation = operation;
        this.destRegister = destRegister;
        this.sourceReg1 = sourceReg1;
        this.sourceReg2 = sourceReg2;
        this.stationTag = stationTag;
        this.id = 0;
        this.issueCycle = -1;
        this.immediate = 0;
        this.hasImmediate = false;
        this.offset = 0;
        this.baseRegister = "";
    }

    /**
     * Gets the operation name.
     *
     * @return the operation name
     */
    public String getOperation() {
        return operation;
    }

    /**
     * Gets the destination register.
     *
     * @return the destination register, or null if not applicable
     */
    public String getDestRegister() {
        return destRegister;
    }

    /**
     * Gets the first source register.
     *
     * @return the first source register
     */
    public String getSourceReg1() {
        return sourceReg1;
    }

    /**
     * Gets the first source register using renamed method.
     *
     * @return the first source register
     */
    public String getSourceRegister1() {
        return sourceReg1;
    }

    /**
     * Gets the second source register.
     *
     * @return the second source register, or null if not applicable
     */
    public String getSourceReg2() {
        return sourceReg2;
    }

    /**
     * Gets the second source register using renamed method.
     *
     * @return the second source register, or null if not applicable
     */
    public String getSourceRegister2() {
        return sourceReg2;
    }

    /**
     * Gets the reservation station tag.
     *
     * @return the station tag
     */
    public String getStationTag() {
        return stationTag;
    }

    /**
     * Sets the reservation station tag.
     *
     * @param stationTag the station tag to set
     */
    public void setStationTag(String stationTag) {
        this.stationTag = stationTag;
    }

    /**
     * Gets the instruction ID.
     *
     * @return the instruction ID
     */
    public int getId() {
        return id;
    }

    /**
     * Sets the instruction ID.
     *
     * @param id the instruction ID
     */
    public void setId(int id) {
        this.id = id;
    }

    /**
     * Gets the issue cycle.
     *
     * @return the cycle number when this instruction was issued
     */
    public int getIssueCycle() {
        return issueCycle;
    }

    /**
     * Sets the issue cycle.
     *
     * @param cycle the cycle number when this instruction was issued
     */
    public void setIssueCycle(int cycle) {
        this.issueCycle = cycle;
    }

    /**
     * Checks if this instruction has a destination register.
     *
     * @return true if the instruction has a destination register, false otherwise
     */
    public boolean hasDestination() {
        return destRegister != null && !destRegister.isEmpty();
    }

    /**
     * Checks if this instruction is a memory operation (load or store).
     *
     * @return true if the operation is a load or store, false otherwise
     */
    public boolean isMemoryOperation() {
        if (operation == null) return false;
        String op = operation.toUpperCase();
        return op.equals("LW") || op.equals("LD") || op.equals("L.S") || op.equals("L.D") ||
               op.equals("SW") || op.equals("SD") || op.equals("S.S") || op.equals("S.D");
    }

    /**
     * Gets the base register for memory operations.
     *
     * @return the base register
     */
    public String getBaseRegister() {
        return baseRegister;
    }

    /**
     * Sets the base register for memory operations.
     *
     * @param baseRegister the base register
     */
    public void setBaseRegister(String baseRegister) {
        this.baseRegister = baseRegister;
    }

    /**
     * Gets the memory offset for memory operations.
     *
     * @return the offset value
     */
    public int getOffset() {
        return offset;
    }

    /**
     * Sets the memory offset for memory operations.
     *
     * @param offset the offset value
     */
    public void setOffset(int offset) {
        this.offset = offset;
    }

    /**
     * Checks if this instruction has an immediate value.
     *
     * @return true if the instruction has an immediate value, false otherwise
     */
    public boolean hasImmediate() {
        return hasImmediate;
    }

    /**
     * Gets the immediate value.
     *
     * @return the immediate value
     */
    public int getImmediate() {
        return immediate;
    }

    /**
     * Sets the immediate value.
     *
     * @param immediate the immediate value
     */
    public void setImmediate(int immediate) {
        this.immediate = immediate;
        this.hasImmediate = true;
    }

    /**
     * Creates an arithmetic instruction.
     *
     * @param op the operation (e.g., "ADD.D", "MUL.D")
     * @param dest the destination register
     * @param src1 the first source register
     * @param src2 the second source register
     * @param tag the reservation station tag
     * @return a new Instruction instance
     */
    public static Instruction createArithmetic(String op, String dest, String src1, 
                                               String src2, String tag) {
        return new Instruction(op, dest, src1, src2, tag);
    }

    /**
     * Creates a load instruction.
     *
     * @param dest the destination register
     * @param baseReg the base register for address calculation
     * @param tag the reservation station tag
     * @return a new Instruction instance
     */
    public static Instruction createLoad(String dest, String baseReg, String tag) {
        return new Instruction("L.D", dest, baseReg, null, tag);
    }

    /**
     * Creates a store instruction.
     *
     * @param src the source register to store
     * @param baseReg the base register for address calculation
     * @param tag the reservation station tag
     * @return a new Instruction instance
     */
    public static Instruction createStore(String src, String baseReg, String tag) {
        return new Instruction("S.D", null, src, baseReg, tag);
    }

    /**
     * Creates a branch instruction (BEQ or BNE).
     *
     * @param op the operation ("BEQ" or "BNE")
     * @param src1 the first source register to compare
     * @param src2 the second source register to compare
     * @param tag the reservation station tag
     * @return a new Instruction instance
     */
    public static Instruction createBranch(String op, String src1, String src2, String tag) {
        return new Instruction(op, null, src1, src2, tag);
    }

    /**
     * Returns a string representation of the instruction for debugging.
     *
     * @return a formatted string with all instruction fields
     */
    @Override
    public String toString() {
        return String.format("Instruction{operation='%s', destRegister='%s', sourceReg1='%s', sourceReg2='%s', stationTag='%s'}",
                operation, destRegister, sourceReg1, sourceReg2, stationTag);
    }
}
