// File: InstructionType.java
package simulator.parser;
public enum InstructionType {
    DADDI("DADDI", "integer"),
    DSUBI("DSUBI", "integer"),
    LW("LW", "load"),
    LD("LD", "load"),
    L_D("L.D", "fp_load"),
    S_D("S.D", "fp_store"),
    ADD_D("ADD.D", "fp_add"),
    SUB_D("SUB.D", "fp_add"),
    MUL_D("MUL.D", "fp_mul"),
    DIV_D("DIV.D", "fp_div"),
    BEQ("BEQ", "branch"),
    BNE("BNE", "branch");

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

    public boolean isInteger()        { return "integer".equals(category); }
    public boolean isFpAdd()          { return "fp_add".equals(category); }
    public boolean isFpMul()          { return "fp_mul".equals(category); }
    public boolean isFpDiv()          { return "fp_div".equals(category); }
    public boolean isFpLoad()         { return "fp_load".equals(category); }
    public boolean isFpStore()        { return "fp_store".equals(category); }
    public boolean isLoad()           { return "load".equals(category) || isFpLoad(); }
    public boolean isStore()          { return isFpStore(); }
    public boolean isBranch()         { return "branch".equals(category); }
    public boolean isFloatingPoint()  { return category.startsWith("fp_"); }

    /**
     * Critical: This method now correctly handles both "MUL.D" and "MUL.D " (with spaces)
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

        throw new IllegalArgumentException("Unknown instruction: " + text);
    }
}