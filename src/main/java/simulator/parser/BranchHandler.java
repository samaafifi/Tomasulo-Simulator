package simulator.parser;
import java.util.*;

/**
 * Handles label extraction, branch target resolution, and flush logic (no prediction)
 */
public class BranchHandler {
    private final Map<String, Integer> labelToIndex;
    private final Map<Integer, String> indexToLabel;

    public BranchHandler() {
        this.labelToIndex = new HashMap<>();
        this.indexToLabel = new HashMap<>();
    }

    public void extractLabels(List<ParsedInstruction> instructions) {
        labelToIndex.clear();
        indexToLabel.clear();

        for (int i = 0; i < instructions.size(); i++) {
            ParsedInstruction instr = instructions.get(i);
            if (instr.isLabelDefinition()) {
                String label = instr.getLabel();
                labelToIndex.put(label, i);
                indexToLabel.put(i, label);
            }
        }
    }

    public void resolveBranchTargets(List<ParsedInstruction> instructions) {
        for (ParsedInstruction instr : instructions) {
            if (instr.usesLabel()) {
                String target = instr.getTargetLabel();
                Integer address = labelToIndex.get(target);
                if (address == null) {
                    throw new IllegalArgumentException("Undefined label: " + target +
                            " in instruction: " + instr.getOriginalAssembly());
                }
                instr.setImmediate(address);
            }
        }
    }

    public List<Integer> getInstructionsToFlush(int branchIndex, int currentPC, boolean taken) {
        if (!taken) return Collections.emptyList();

        List<Integer> toFlush = new ArrayList<>();
        for (int i = branchIndex + 1; i < currentPC; i++) {
            toFlush.add(i);
        }
        return toFlush;
    }

    public boolean shouldTakeBranch(ParsedInstruction branch, Map<String, Double> regValues) {
        if (!branch.getType().isBranch()) return false;

        String r1 = branch.getOperands()[0].trim();
        String r2 = branch.getOperands()[1].trim();
        double v1 = regValues.getOrDefault(r1, 0.0);
        double v2 = regValues.getOrDefault(r2, 0.0);

        return switch (branch.getType()) {
            case BEQ -> v1 == v2;
            case BNE -> v1 != v2;
            default -> false;
        };
    }

    public int getBranchTarget(ParsedInstruction branch) {
        Integer target = branch.getImmediate();
        if (target == null) {
            throw new IllegalStateException("Branch target not resolved: " + branch);
        }
        return target;
    }

    public int getNextPC(int currentPC, boolean taken, int targetPC) {
        return taken ? targetPC : currentPC;
    }

    public Integer getAddress(String label) {
        return labelToIndex.get(label);
    }

    public void printLabels() {
        System.out.println("=== Label Map ===");
        labelToIndex.forEach((l, a) -> System.out.println(l + " -> " + a));
    }

    public void clear() {
        labelToIndex.clear();
        indexToLabel.clear();
    }
}