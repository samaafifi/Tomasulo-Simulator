package simulator.parser;
import java.util.*;

/**
 * FIXED: BranchHandler with NO BRANCH PREDICTION
 * - Branches stall pipeline until resolved
 * - Fixed flush logic for taken branches
 * - Proper handling of both forward and backward branches
 */
public class BranchHandler {
    private final Map<String, Integer> labelToIndex;
    private final Map<Integer, String> indexToLabel;
    private BranchStatistics statistics;

    public BranchHandler() {
        this.labelToIndex = new HashMap<>();
        this.indexToLabel = new HashMap<>();
        this.statistics = new BranchStatistics();
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

    /**
     * FIXED: Returns instructions to flush when branch is taken
     * NO PREDICTION: Instructions after branch that were issued need to be flushed
     * 
     * @param branchIndex Index of the branch instruction in program
     * @param issuedInstructions List of instruction indices that have been issued
     * @param taken Whether branch was taken
     * @return List of instruction indices to flush
     */
    public List<Integer> getInstructionsToFlush(int branchIndex, 
                                                 List<Integer> issuedInstructions, 
                                                 boolean taken) {
        List<Integer> toFlush = new ArrayList<>();
        
        if (!taken) {
            // Branch not taken - no flush needed
            return toFlush;
        }
        
        // Branch taken - flush all instructions issued after the branch
        for (Integer instrIndex : issuedInstructions) {
            if (instrIndex > branchIndex) {
                toFlush.add(instrIndex);
            }
        }
        
        return toFlush;
    }

    /**
     * Evaluates branch condition - NO PREDICTION
     */
    public boolean shouldTakeBranch(ParsedInstruction branch, Map<String, Double> regValues) {
        if (!branch.getType().isBranch()) return false;

        List<String> sources = branch.getSourceRegisters();
        if (sources.size() < 2) {
            throw new IllegalArgumentException("Branch requires 2 source registers");
        }
        
        String r1 = sources.get(0).trim();
        String r2 = sources.get(1).trim();
        double v1 = regValues.getOrDefault(r1, 0.0);
        double v2 = regValues.getOrDefault(r2, 0.0);

        boolean taken = switch (branch.getType()) {
            case BEQ -> v1 == v2;
            case BNE -> v1 != v2;
            default -> false;
        };
        
        statistics.recordBranch(taken);
        return taken;
    }

    public int getBranchTarget(ParsedInstruction branch) {
        Integer target = branch.getImmediate();
        if (target == null) {
            throw new IllegalStateException("Branch target not resolved: " + branch);
        }
        return target;
    }

    public int getNextPC(int currentPC, boolean taken, int targetPC) {
        return taken ? targetPC : currentPC + 1;
    }

    public Integer getAddress(String label) {
        return labelToIndex.get(label);
    }
    
    public String getLabel(int address) {
        return indexToLabel.get(address);
    }
    
    public boolean hasLabel(int address) {
        return indexToLabel.containsKey(address);
    }
    
    public Map<String, Integer> getAllLabels() {
        return new HashMap<>(labelToIndex);
    }

    public void printLabels() {
        System.out.println("=== Label Map ===");
        labelToIndex.forEach((l, a) -> System.out.println(l + " -> " + a));
    }

    public void clear() {
        labelToIndex.clear();
        indexToLabel.clear();
        statistics = new BranchStatistics();
    }
    
    public BranchStatistics getStatistics() {
        return statistics;
    }
    
    /**
     * Branch statistics tracking
     */
    public static class BranchStatistics {
        private int branchesTaken = 0;
        private int branchesNotTaken = 0;
        
        public void recordBranch(boolean taken) {
            if (taken) {
                branchesTaken++;
            } else {
                branchesNotTaken++;
            }
        }
        
        public int getBranchesTaken() { return branchesTaken; }
        public int getBranchesNotTaken() { return branchesNotTaken; }
        public int getTotalBranches() { return branchesTaken + branchesNotTaken; }
        
        public double getTakenPercentage() {
            int total = getTotalBranches();
            return total > 0 ? (100.0 * branchesTaken / total) : 0.0;
        }
        
        @Override
        public String toString() {
            return String.format("Branches: %d total (%d taken, %d not taken, %.1f%% taken rate)",
                getTotalBranches(), branchesTaken, branchesNotTaken, getTakenPercentage());
        }
    }
}