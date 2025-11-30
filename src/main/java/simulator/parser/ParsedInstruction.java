package simulator.parser;
import java.util.*;

/**
 * ParsedInstruction - Represents a single parsed MIPS instruction
 * 
 * Stores:
 * - Instruction type and operands
 * - Labels (if this instruction has a label)
 * - Immediate values (for arithmetic, memory offsets, branch targets)
 * - Source and destination registers
 * 
 * Used throughout the Tomasulo simulator for instruction tracking and execution
 */
public class ParsedInstruction {
    private final InstructionType type;
    private final String[] operands;
    private final String originalAssembly;
    private final int lineNumber;

    private String label;           // Label at this instruction (e.g., "LOOP")
    private String targetLabel;     // Branch target label (e.g., "LOOP" in "BNE R1, R2, LOOP")
    private Integer immediate;      // Immediate value (offset, constant, or resolved branch target)

    public ParsedInstruction(InstructionType type, String[] operands, String originalAssembly, int lineNumber) {
        this.type = type;
        this.operands = operands != null ? operands.clone() : new String[0];
        this.originalAssembly = originalAssembly != null ? originalAssembly.trim() : "";
        this.lineNumber = lineNumber;
        extractImmediateAndTarget();
    }

    /**
     * Extract immediate values and branch targets from operands
     * Called automatically during instruction construction
     */
    private void extractImmediateAndTarget() {
        if (type == null) return;

        switch (type) {
            // ========== INTEGER ARITHMETIC WITH IMMEDIATE ==========
            // Format: DADDI Rdest, Rsrc, immediate
            // Example: DADDI R1, R2, 100
            case DADDI, DSUBI -> {
                if (operands.length >= 3) {
                    try {
                        this.immediate = Integer.parseInt(operands[2].trim());
                    } catch (NumberFormatException ignored) {
                        // Invalid immediate will be caught during execution
                    }
                }
            }

            // ========== ALL LOAD OPERATIONS ==========
            // Format: LOAD Rdest, offset(Rbase)
            // Examples: LW R1, 100(R2)  or  L.D F0, 8(R1)
            case LW, LD, L_S, L_D -> {
                if (operands.length >= 2) {
                    String mem = operands[1].trim();
                    int openParen = mem.indexOf('(');
                    if (openParen > 0) {
                        String immStr = mem.substring(0, openParen).trim();
                        try {
                            this.immediate = Integer.parseInt(immStr);
                        } catch (NumberFormatException e) {
                            this.immediate = 0;  // Default to 0 if invalid
                        }
                    }
                }
            }

            // ========== ALL STORE OPERATIONS ==========
            // Format: STORE Rsrc, offset(Rbase)
            // Examples: SW R1, 100(R2)  or  S.D F0, 8(R1)
            case SW, SD, S_S, S_D -> {
                if (operands.length >= 2) {
                    String mem = operands[1].trim();
                    int openParen = mem.indexOf('(');
                    if (openParen > 0) {
                        String immStr = mem.substring(0, openParen).trim();
                        try {
                            this.immediate = Integer.parseInt(immStr);
                        } catch (NumberFormatException e) {
                            this.immediate = 0;  // Default to 0 if invalid
                        }
                    }
                }
            }

            // ========== FLOATING-POINT ARITHMETIC ==========
            // No immediate values - these are register-to-register operations
            case ADD_D, SUB_D, MUL_D, DIV_D, ADD_S, SUB_S, MUL_S, DIV_S -> {
                // These instructions don't have immediate values
            }

            // ========== BRANCH INSTRUCTIONS ==========
            // Format: BEQ/BNE R1, R2, target
            // Target can be either:
            //   - A label: "BNE R1, R2, LOOP"
            //   - An immediate: "BNE R1, R2, 10"
            case BEQ, BNE -> {
                if (operands.length >= 3) {
                    String target = operands[2].trim();
                    // Try to parse as numeric immediate first
                    try {
                        this.immediate = Integer.parseInt(target);
                        // It's a numeric address - immediate is set
                    } catch (NumberFormatException e) {
                        // It's a label - will be resolved later by BranchHandler
                        this.targetLabel = target;
                    }
                }
            }
        }
    }

    /**
     * Get all source registers used by this instruction
     * Used for dependency tracking in Tomasulo algorithm
     * 
     * @return List of source register names (e.g., ["R1", "R2"] or ["F0", "F2"])
     */
    public List<String> getSourceRegisters() {
        if (type == null) return List.of();

        return switch (type) {
            // ========== INTEGER ARITHMETIC ==========
            // Format: DADDI Rdest, Rsrc, imm
            // Source: Rsrc (operand[1])
            case DADDI, DSUBI -> 
                operands.length >= 2 ? List.of(operands[1].trim()) : List.of();

            // ========== ALL LOAD OPERATIONS ==========
            // Format: LOAD Rdest, offset(Rbase)
            // Source: Rbase (inside parentheses in operand[1])
            case LW, LD, L_S, L_D -> {
                if (operands.length < 2) yield List.of();
                String mem = operands[1].trim();
                int start = mem.indexOf('(');
                int end = mem.indexOf(')');
                if (start != -1 && end != -1 && end > start + 1) {
                    yield List.of(mem.substring(start + 1, end).trim());
                }
                yield List.of();
            }

            // ========== ALL STORE OPERATIONS ==========
            // Format: STORE Rsrc, offset(Rbase)
            // Sources: Rsrc (operand[0]) and Rbase (inside parentheses in operand[1])
            case SW, SD, S_S, S_D -> {
                if (operands.length < 2) yield List.of(operands[0].trim());
                String mem = operands[1].trim();
                int start = mem.indexOf('(');
                int end = mem.indexOf(')');
                if (start != -1 && end != -1 && end > start + 1) {
                    String base = mem.substring(start + 1, end).trim();
                    yield List.of(operands[0].trim(), base);
                }
                // If no base register found, still return source register
                yield List.of(operands[0].trim());
            }

            // ========== ALL FLOATING-POINT ARITHMETIC ==========
            // Format: OP.D/OP.S Fdest, Fsrc1, Fsrc2
            // Sources: Fsrc1 (operand[1]), Fsrc2 (operand[2])
            case ADD_D, SUB_D, MUL_D, DIV_D, ADD_S, SUB_S, MUL_S, DIV_S ->
                operands.length >= 3 ? 
                    List.of(operands[1].trim(), operands[2].trim()) : 
                    List.of();

            // ========== BRANCH OPERATIONS ==========
            // Format: BEQ/BNE R1, R2, target
            // Sources: R1 (operand[0]), R2 (operand[1])
            case BEQ, BNE ->
                operands.length >= 2 ? 
                    List.of(operands[0].trim(), operands[1].trim()) : 
                    List.of();

            default -> List.of();
        };
    }

    /**
     * Get the destination register for this instruction
     * Returns null for stores and branches (they don't write to registers)
     * 
     * @return Destination register name (e.g., "R1" or "F0"), or null
     */
    public String getDestinationRegister() {
        if (type == null || type.isStore() || type.isBranch()) {
            return null;  // Stores and branches don't have destination registers
        }
        return operands.length > 0 ? operands[0].trim() : null;
    }

    // ========== GETTERS ==========
    
    public InstructionType getType() { 
        return type; 
    }
    
    public String[] getOperands() { 
        return operands.clone(); 
    }
    
    public String getOriginalAssembly() { 
        return originalAssembly; 
    }
    
    public int getLineNumber() { 
        return lineNumber; 
    }
    
    public String getLabel() { 
        return label; 
    }
    
    public String getTargetLabel() { 
        return targetLabel; 
    }
    
    public Integer getImmediate() { 
        return immediate; 
    }

    // ========== SETTERS ==========
    
    public void setLabel(String label) { 
        this.label = label; 
    }
    
    public void setImmediate(Integer immediate) { 
        this.immediate = immediate; 
    }

    // ========== UTILITY METHODS ==========
    
    /**
     * Check if this instruction uses a label as a branch target
     */
    public boolean usesLabel() {
        return targetLabel != null && !targetLabel.isEmpty();
    }

    /**
     * Check if this instruction defines a label
     */
    public boolean isLabelDefinition() {
        return label != null && !label.isEmpty();
    }

    /**
     * Get a string representation suitable for display
     */
    @Override
    public String toString() {
        return (label != null ? label + ": " : "") + originalAssembly;
    }

    /**
     * Get a detailed debug string with all parsed information
     */
    public String toDebugString() {
        String dest = getDestinationRegister();
        List<String> srcs = getSourceRegisters();
        String extra = immediate != null ? "Imm=" + immediate : (targetLabel != null ? "Target=" + targetLabel : "-");
        return String.format("L%02d | %-10s | %-30s | Dest:%-4s | Src:%-15s | %s",
                lineNumber,
                type != null ? type.getAssemblyName() : "LABEL",
                originalAssembly,
                dest != null ? dest : "-",
                srcs,
                extra);
    }

    /**
     * Get a human-readable description of what this instruction does
     */
    public String getDescription() {
        if (type == null) return "Label definition";
        
        String dest = getDestinationRegister();
        List<String> srcs = getSourceRegisters();
        
        return switch (type) {
            case DADDI -> String.format("%s = %s + %d", dest, srcs.get(0), immediate);
            case DSUBI -> String.format("%s = %s - %d", dest, srcs.get(0), immediate);
            case LW, LD, L_S, L_D -> String.format("%s = Memory[%s + %d]", dest, srcs.get(0), immediate);
            case SW, SD, S_S, S_D -> String.format("Memory[%s + %d] = %s", srcs.get(1), immediate, srcs.get(0));
            case ADD_D, ADD_S -> String.format("%s = %s + %s", dest, srcs.get(0), srcs.get(1));
            case SUB_D, SUB_S -> String.format("%s = %s - %s", dest, srcs.get(0), srcs.get(1));
            case MUL_D, MUL_S -> String.format("%s = %s × %s", dest, srcs.get(0), srcs.get(1));
            case DIV_D, DIV_S -> String.format("%s = %s ÷ %s", dest, srcs.get(0), srcs.get(1));
            case BEQ -> String.format("if (%s == %s) goto %s", srcs.get(0), srcs.get(1), 
                                     targetLabel != null ? targetLabel : immediate);
            case BNE -> String.format("if (%s ≠ %s) goto %s", srcs.get(0), srcs.get(1), 
                                     targetLabel != null ? targetLabel : immediate);
        };
    }

    /**
     * Check if this instruction is ready to execute given available registers
     * Used for checking if all source registers are available
     */
    public boolean isReadyToExecute(Set<String> availableRegisters) {
        List<String> sources = getSourceRegisters();
        return availableRegisters.containsAll(sources);
    }

    /**
     * Get the memory address for load/store instructions
     * Requires register values to compute effective address
     */
    public int calculateMemoryAddress(Map<String, Integer> registerValues) {
        if (!type.isMemoryOperation()) {
            throw new IllegalStateException("Not a memory operation: " + type);
        }
        
        List<String> sources = getSourceRegisters();
        if (sources.isEmpty()) {
            throw new IllegalStateException("Memory operation missing base register");
        }
        
        String baseReg = sources.get(0);
        int baseValue = registerValues.getOrDefault(baseReg, 0);
        int offset = immediate != null ? immediate : 0;
        
        return baseValue + offset;
    }
}