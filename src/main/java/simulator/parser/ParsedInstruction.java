import java.util.*;

public class ParsedInstruction {
    private final InstructionType type;
    private final String[] operands;
    private final String originalAssembly;
    private final int lineNumber;

    private String label;           
    private String targetLabel;     
    private Integer immediate;      

    public ParsedInstruction(InstructionType type, String[] operands, String originalAssembly, int lineNumber) {
        this.type = type;
        this.operands = operands != null ? operands.clone() : new String[0];
        this.originalAssembly = originalAssembly != null ? originalAssembly.trim() : "";
        this.lineNumber = lineNumber;
        extractImmediateAndTarget();
    }

    private void extractImmediateAndTarget() {
        if (type == null) return;

        switch (type) {
            case DADDI, DSUBI -> {
                if (operands.length >= 3) {
                    try {
                        this.immediate = Integer.parseInt(operands[2].trim());
                    } catch (NumberFormatException ignored) {}
                }
            }

            case LW, LD, L_D, S_D -> {
                if (operands.length >= 2) {
                    String mem = operands[1].trim();
                    int openParen = mem.indexOf('(');
                    if (openParen > 0) {
                        String immStr = mem.substring(0, openParen).trim();
                        try {
                            this.immediate = Integer.parseInt(immStr);
                        } catch (NumberFormatException e) {
                            this.immediate = 0;
                        }
                    }
                }
            }

            case ADD_D, SUB_D, MUL_D, DIV_D -> {
            }

            case BEQ, BNE -> {
                if (operands.length >= 3) {
                    this.targetLabel = operands[2].trim();
                }
            }
        }
    }

    // === Getters ===
    public InstructionType getType() { return type; }
    public String[] getOperands() { return operands.clone(); }
    public String getOriginalAssembly() { return originalAssembly; }
    public int getLineNumber() { return lineNumber; }
    public String getLabel() { return label; }
    public String getTargetLabel() { return targetLabel; }
    public Integer getImmediate() { return immediate; }

    // === Setters ===
    public void setLabel(String label) { this.label = label; }
    public void setImmediate(Integer immediate) { this.immediate = immediate; }

    public String getDestinationRegister() {
        if (type == null || type.isStore() || type.isBranch()) return null;
        return operands.length > 0 ? operands[0].trim() : null;
    }

    public List<String> getSourceRegisters() {
        if (type == null) return List.of();

        return switch (type) {
            case DADDI, DSUBI -> operands.length >= 2 ? List.of(operands[1].trim()) : List.of();

            case LW, LD, L_D -> {
                if (operands.length < 2) yield List.of();
                String mem = operands[1].trim();
                int start = mem.indexOf('(');
                int end = mem.indexOf(')');
                if (start != -1 && end != -1 && end > start + 1) {
                    yield List.of(mem.substring(start + 1, end).trim());
                }
                yield List.of();
            }

            case S_D -> {
                if (operands.length < 2) yield List.of(operands[0].trim());
                String mem = operands[1].trim();
                int start = mem.indexOf('(');
                int end = mem.indexOf(')');
                if (start != -1 && end != -1 && end > start + 1) {
                    String base = mem.substring(start + 1, end).trim();
                    yield List.of(operands[0].trim(), base);
                }
                yield List.of(operands[0].trim());
            }

            case ADD_D, SUB_D, MUL_D, DIV_D ->
                operands.length >= 3 ? List.of(operands[1].trim(), operands[2].trim()) : List.of();

            case BEQ, BNE ->
                operands.length >= 2 ? List.of(operands[0].trim(), operands[1].trim()) : List.of();

            default -> List.of();
        };
    }

    public boolean usesLabel() {
        return targetLabel != null && !targetLabel.isEmpty();
    }

    public boolean isLabelDefinition() {
        return label != null && !label.isEmpty();
    }

    @Override
    public String toString() {
        return (label != null ? label + ": " : "") + originalAssembly;
    }

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
}