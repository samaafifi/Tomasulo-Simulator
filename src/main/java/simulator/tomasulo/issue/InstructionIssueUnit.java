package simulator.tomasulo.issue;

import java.util.*;
import simulator.parser.*;
import simulator.tomasulo.registerfile.*;
import simulator.tomasulo.models.Instruction;
import simulator.tomasulo.hazards.DataHazardHandler;

/**
 * FIXED: InstructionIssueUnit
 * - Fixed DADDI/DSUBI detection bug
 * - Added proper branch handling (NO PREDICTION - branches stall until resolved)
 * - Improved memory operation handling
 */
public class InstructionIssueUnit {
    private ReservationStationPool rsPool;
    private InstructionQueue instructionQueue;
    private RegisterFile registerFile;
    private DataHazardHandler hazardHandler;
    private int currentCycle;
    
    // Branch handling - NO PREDICTION
    private boolean branchPending = false;
    private ReservationStation pendingBranch = null;
    
    private Map<Integer, Integer> issueCycles;
    
    public InstructionIssueUnit(ReservationStationPool rsPool, 
                                InstructionQueue instructionQueue,
                                RegisterFile registerFile) {
        this.rsPool = rsPool;
        this.instructionQueue = instructionQueue;
        this.registerFile = registerFile;
        this.hazardHandler = new DataHazardHandler(registerFile);
        this.currentCycle = 0;
        this.issueCycles = new HashMap<>();
    }
    
    /**
     * Attempts to issue the next instruction
     * NO BRANCH PREDICTION: If branch is pending, stall issue until branch resolves
     */
    public boolean issueNextInstruction() {
        if (instructionQueue.isEmpty()) {
            return false;
        }
        
        // NO PREDICTION: Stall issue if branch is pending
        if (branchPending) {
            System.out.println("Cycle " + currentCycle + ": Issue stalled - waiting for branch resolution");
            return false;
        }
        
        Instruction instruction = instructionQueue.peek();
        
        // Check for structural hazard
        StructuralHazard hazard = checkStructuralHazard(instruction);
        if (hazard.hasHazard()) {
            System.out.println("Cycle " + currentCycle + ": Structural hazard - " 
                             + hazard.getMessage());
            return false;
        }
        
        // Get reservation station
        String rsType = determineReservationStationType(instruction);
        ReservationStation rs = rsPool.allocateStation(rsType);
        
        if (rs == null) {
            return false;
        }
        
        // Issue the instruction
        issueToReservationStation(instruction, rs);
        
        // Use DataHazardHandler
        DataHazardHandler.SourceOperands operands = hazardHandler.handleIssue(instruction);
        updateReservationStationOperands(rs, operands);
        
        // Check if this is a branch instruction
        if (isBranchInstruction(instruction)) {
            branchPending = true;
            pendingBranch = rs;
            System.out.println("Cycle " + currentCycle + ": Branch issued - stalling further issue");
        }
        
        issueCycles.put(instruction.getId(), currentCycle);
        instruction.setIssueCycle(currentCycle);
        instructionQueue.dequeue();
        
        System.out.println("Cycle " + currentCycle + ": Issued " 
                         + instruction.toString() + " to " + rs.getName());
        
        return true;
    }
    
    /**
     * Check if instruction is a branch
     */
    private boolean isBranchInstruction(Instruction instruction) {
        String op = instruction.getOperation().toUpperCase();
        return op.equals("BEQ") || op.equals("BNE");
    }
    
    /**
     * Called when branch resolves - clears branch pending flag
     */
    public void resolveBranch() {
        branchPending = false;
        pendingBranch = null;
        System.out.println("Cycle " + currentCycle + ": Branch resolved - resuming issue");
    }
    
    /**
     * Check if branch is currently pending
     */
    public boolean isBranchPending() {
        return branchPending;
    }
    
    private void issueToReservationStation(Instruction instruction, ReservationStation rs) {
        rs.setBusy(true);
        rs.setOp(instruction.getOperation());
        rs.setInstruction(instruction);
        
        String src1 = instruction.getSourceRegister1();
        String src2 = instruction.getSourceRegister2();
        
        if (src1 != null && !src1.isEmpty()) {
            String producer = registerFile.getRegisterStatus(src1);
            if (producer != null && !producer.equals("")) {
                rs.setQj(producer);
                rs.setVj(null);
            } else {
                rs.setVj(registerFile.getRegisterValue(src1));
                rs.setQj(null);
            }
        }
        
        if (src2 != null && !src2.isEmpty()) {
            String producer = registerFile.getRegisterStatus(src2);
            if (producer != null && !producer.equals("")) {
                rs.setQk(producer);
                rs.setVk(null);
            } else {
                rs.setVk(registerFile.getRegisterValue(src2));
                rs.setQk(null);
            }
        }
        
        if (instruction.hasImmediate()) {
            rs.setA(instruction.getImmediate());
        }
        
        if (instruction.isMemoryOperation()) {
            String baseReg = instruction.getBaseRegister();
            if (baseReg != null && !baseReg.isEmpty()) {
                String producer = registerFile.getRegisterStatus(baseReg);
                if (producer != null && !producer.equals("")) {
                    rs.setQj(producer);
                    rs.setVj(null);
                } else {
                    rs.setVj(registerFile.getRegisterValue(baseReg));
                    rs.setQj(null);
                }
            }
            rs.setA(instruction.getOffset());
        }
    }
    
    private void updateReservationStationOperands(ReservationStation rs, 
                                                   DataHazardHandler.SourceOperands operands) {
        if (operands.isSource1Ready() && operands.getSrc1Value() != null) {
            rs.setVj(Double.parseDouble(operands.getSrc1Value()));
            rs.setQj(null);
        } else if (operands.getSrc1Qi() != null) {
            rs.setQj(operands.getSrc1Qi());
            rs.setVj(null);
        }
        
        if (operands.isSource2Ready() && operands.getSrc2Value() != null) {
            rs.setVk(Double.parseDouble(operands.getSrc2Value()));
            rs.setQk(null);
        } else if (operands.getSrc2Qi() != null) {
            rs.setQk(operands.getSrc2Qi());
            rs.setVk(null);
        }
    }
    
    private StructuralHazard checkStructuralHazard(Instruction instruction) {
        String rsType = determineReservationStationType(instruction);
        
        if (!rsPool.hasAvailableStation(rsType)) {
            return new StructuralHazard(true, 
                "No available " + rsType + " reservation stations");
        }
        
        return new StructuralHazard(false, "");
    }
    
    /**
     * FIXED: Proper detection of all instruction types including DADDI/DSUBI
     */
    private String determineReservationStationType(Instruction instruction) {
        String op = instruction.getOperation().toUpperCase();
        
        // FIXED: Explicit check for integer immediate operations
        if (op.equals("DADDI") || op.equals("DSUBI") || op.equals("ADDI") || op.equals("SUBI")) {
            return "INTEGER_ADD";
        }
        
        // Branch instructions
        if (op.equals("BEQ") || op.equals("BNE")) {
            return "BRANCH";
        }
        
        // Floating point add/sub
        if (op.equals("ADD.D") || op.equals("SUB.D") || op.equals("ADD.S") || op.equals("SUB.S")) {
            return "FP_ADD";
        }
        
        // Floating point multiply
        if (op.equals("MUL.D") || op.equals("MUL.S")) {
            return "FP_MUL";
        }
        
        // Floating point divide
        if (op.equals("DIV.D") || op.equals("DIV.S")) {
            return "FP_DIV";
        }
        
        // Load instructions
        if (op.equals("LW") || op.equals("LD") || op.equals("L.S") || op.equals("L.D")) {
            return "LOAD";
        }
        
        // Store instructions
        if (op.equals("SW") || op.equals("SD") || op.equals("S.S") || op.equals("S.D")) {
            return "STORE";
        }
        
        return "FP_ADD"; // Default fallback
    }
    
    public void nextCycle() {
        currentCycle++;
    }
    
    public int getCurrentCycle() {
        return currentCycle;
    }
    
    public Integer getIssueCycle(int instructionId) {
        return issueCycles.get(instructionId);
    }
    
    public void reset() {
        currentCycle = 0;
        issueCycles.clear();
        branchPending = false;
        pendingBranch = null;
    }
    
    public DataHazardHandler getHazardHandler() {
        return hazardHandler;
    }
}