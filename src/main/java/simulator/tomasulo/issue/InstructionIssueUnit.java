package simulator.tomasulo.issue;

import java.util.*;
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
    private simulator.memory.MemorySystem memorySystem; // For load/store operations
    private int currentCycle;
    
    // Branch handling - NO PREDICTION
    private boolean branchPending = false;
    
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
     * Constructor that takes MemorySystem instead of InstructionQueue
     * Creates an internal InstructionQueue
     */
    public InstructionIssueUnit(ReservationStationPool rsPool,
                                RegisterFile registerFile,
                                simulator.memory.MemorySystem memorySystem) {
        this.rsPool = rsPool;
        this.instructionQueue = new InstructionQueue();
        this.registerFile = registerFile;
        this.memorySystem = memorySystem;
        this.hazardHandler = new DataHazardHandler(registerFile);
        this.currentCycle = 0;
        this.issueCycles = new HashMap<>();
    }
    
    /**
     * Check if an instruction can be issued
     */
    public boolean canIssue(Instruction instruction) {
        if (branchPending) {
            return false;
        }
        
        StructuralHazard hazard = checkStructuralHazard(instruction);
        return !hazard.hasHazard();
    }
    
    /**
     * Issue a specific instruction directly (without adding to queue first)
     */
    public boolean issue(Instruction instruction) {
        if (!canIssue(instruction)) {
            return false;
        }
        
        // NO PREDICTION: Stall issue if branch is pending
        if (branchPending) {
            System.out.println("Cycle " + currentCycle + ": Issue stalled - waiting for branch resolution");
            return false;
        }
        
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
        
        // CRITICAL FIX: Set station tag on instruction before calling DataHazardHandler
        // This is required for register renaming - the destination register's Qi must
        // point to the reservation station that will produce its value
        instruction.setStationTag(rs.getName());
        
        // Issue the instruction
        issueToReservationStation(instruction, rs);
        
        // Use DataHazardHandler for register renaming (sets destination Qi)
        // NOTE: For memory operations, operand setup is already done in issueToReservationStation()
        // For non-memory operations, updateReservationStationOperands will set Vj/Vk/Qj/Qk correctly
        DataHazardHandler.SourceOperands operands = hazardHandler.handleIssue(instruction);
        if (!instruction.isMemoryOperation()) {
            // Only update operands for non-memory operations
            // Memory operations (loads/stores) are already set up correctly in issueToReservationStation()
        updateReservationStationOperands(rs, operands);
        }
        
        // CRITICAL FIX: Issue load/store instructions to MemorySystem
        // For loads: issue immediately if base register is ready
        // For stores: issue immediately only if BOTH base register AND source register are ready
        // Otherwise, stores will be issued later when they become ready (handled in SimulationController)
        if (memorySystem != null && instruction.isMemoryOperation()) {
            try {
                String op = instruction.getOperation();
                    
                    if (isLoadInstruction(op)) {
                    // Loads: issue if base register is ready (stored in Vj after issueToReservationStation)
                    Double baseValueObj = rs.getVj();
                    Integer offset = instruction.getOffset();
                    
                    if (baseValueObj != null && offset != null) {
                        int baseValue = baseValueObj.intValue();
                        String destReg = instruction.getDestRegister();
                        memorySystem.issueLoad(op, baseValue, offset, destReg);
                        System.out.println("  → Issued to MemorySystem: " + op + " " + destReg + 
                                         " from address " + (baseValue + offset));
                    } else {
                        System.out.println("  → Load waiting for base register (will issue when ready)");
                    }
                    } else if (isStoreInstruction(op)) {
                    // Stores: issue only if BOTH base register (Vj) AND source register (Vk) are ready
                    // After fix: base register -> Vj/Qj, source register -> Vk/Qk
                    Double baseValueObj = rs.getVj();
                    Double srcValueObj = rs.getVk();
                    Integer offset = instruction.getOffset();
                    
                    if (baseValueObj != null && srcValueObj != null && offset != null) {
                        // Both operands ready - issue immediately
                        int baseValue = baseValueObj.intValue();
                        long value = srcValueObj.longValue();
                                memorySystem.issueStore(op, baseValue, offset, value);
                        System.out.println("  → Issued to MemorySystem: " + op + 
                                         " to address " + (baseValue + offset) + " (value: " + value + ")");
                            } else {
                        // Store will wait for operands - will be issued later when ready
                        // (handled in SimulationController after CDB broadcasts)
                        System.out.println("  → Store waiting for operands (base=" + (baseValueObj != null) + 
                                         ", source=" + (srcValueObj != null) + ")");
                    }
                }
            } catch (Exception e) {
                System.err.println("  ✗ Error issuing memory operation: " + e.getMessage());
            }
        }
        
        // Check if this is a branch instruction
        if (isBranchInstruction(instruction)) {
            branchPending = true;
            System.out.println("Cycle " + currentCycle + ": Branch issued - stalling further issue");
        }
        
        issueCycles.put(instruction.getId(), currentCycle);
        instruction.setIssueCycle(currentCycle);
        rs.setIssueCycle(currentCycle); // Track issue cycle in reservation station
        
        System.out.println("Cycle " + currentCycle + ": Issued " 
                         + instruction.toString() + " to " + rs.getName());
        
        return true;
    }
    
    /**
     * Add instructions to the queue
     */
    public void addInstructions(List<Instruction> instructions) {
        instructionQueue.enqueueAll(instructions);
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
        
        // CRITICAL FIX: Set station tag on instruction before calling DataHazardHandler
        // This is required for register renaming - the destination register's Qi must
        // point to the reservation station that will produce its value
        instruction.setStationTag(rs.getName());
        
        // Issue the instruction
        issueToReservationStation(instruction, rs);
        
        // Use DataHazardHandler for register renaming (sets destination Qi)
        // NOTE: For memory operations, operand setup is already done in issueToReservationStation()
        // For non-memory operations, updateReservationStationOperands will set Vj/Vk/Qj/Qk correctly
        DataHazardHandler.SourceOperands operands = hazardHandler.handleIssue(instruction);
        if (!instruction.isMemoryOperation()) {
            // Only update operands for non-memory operations
            // Memory operations (loads/stores) are already set up correctly in issueToReservationStation()
        updateReservationStationOperands(rs, operands);
        }
        
        // Check if this is a branch instruction
        if (isBranchInstruction(instruction)) {
            branchPending = true;
            System.out.println("Cycle " + currentCycle + ": Branch issued - stalling further issue");
        }
        
        issueCycles.put(instruction.getId(), currentCycle);
        instruction.setIssueCycle(currentCycle);
        rs.setIssueCycle(currentCycle); // Track issue cycle in reservation station
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
     * Check if instruction is a load
     */
    private boolean isLoadInstruction(String op) {
        String upperOp = op.toUpperCase();
        return upperOp.equals("LW") || upperOp.equals("LD") || 
               upperOp.equals("L.S") || upperOp.equals("L.D");
    }
    
    /**
     * Check if instruction is a store
     */
    private boolean isStoreInstruction(String op) {
        String upperOp = op.toUpperCase();
        return upperOp.equals("SW") || upperOp.equals("SD") || 
               upperOp.equals("S.S") || upperOp.equals("S.D");
    }
    
    /**
     * Called when branch resolves - clears branch pending flag
     */
    public void resolveBranch() {
        branchPending = false;
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
        
        // CRITICAL FIX: For memory operations, handle base register and source registers correctly
        // For stores: base register -> Vj/Qj (address), source register (data) -> Vk/Qk
        // For loads: base register -> Vj/Qj (address), no source register needed
        if (instruction.isMemoryOperation()) {
            String baseReg = instruction.getBaseRegister();
            if (baseReg != null && !baseReg.isEmpty()) {
                String producer = registerFile.getRegisterStatus(baseReg);
                if (producer != null && !producer.equals("")) {
                    // Base register is busy, waiting for producer
                    rs.setQj(producer);
                    rs.setVj(null);
                } else if (registerFile.isRegisterReady(baseReg)) {
                    // Base register is ready, can read value
                    rs.setVj(registerFile.getRegisterValue(baseReg));
                    rs.setQj(null);
                } else {
                    // Base register is busy but no producer tag (shouldn't happen, but handle gracefully)
                    rs.setQj(null);
                    rs.setVj(null);
                }
            }
            rs.setA(instruction.getOffset());
            
            // For STORES: source register (data to store) goes to Vk/Qk
            if (isStoreInstruction(instruction.getOperation())) {
                String srcReg = instruction.getSourceRegister1(); // Data register for stores
                if (srcReg != null && !srcReg.isEmpty()) {
                    String producer = registerFile.getRegisterStatus(srcReg);
                    if (producer != null && !producer.equals("")) {
                        // Source register is busy, waiting for producer
                        rs.setQk(producer);
                        rs.setVk(null);
                    } else if (registerFile.isRegisterReady(srcReg)) {
                        // Source register is ready, can read value
                        rs.setVk(registerFile.getRegisterValue(srcReg));
                        rs.setQk(null);
                    } else {
                        // Source register is busy but no producer tag (shouldn't happen, but handle gracefully)
                        rs.setQk(null);
                        rs.setVk(null);
                    }
                }
            }
            // For LOADS: no source register needed (destination is set by DataHazardHandler)
        } else {
            // Non-memory operations: handle src1 and src2 normally
        String src1 = instruction.getSourceRegister1();
        String src2 = instruction.getSourceRegister2();
        
        if (src1 != null && !src1.isEmpty()) {
            String producer = registerFile.getRegisterStatus(src1);
            if (producer != null && !producer.equals("")) {
                // Register is busy, waiting for producer
                rs.setQj(producer);
                rs.setVj(null);
            } else if (registerFile.isRegisterReady(src1)) {
                // Register is ready, can read value
                rs.setVj(registerFile.getRegisterValue(src1));
                rs.setQj(null);
            } else {
                // Register is busy but no producer tag (shouldn't happen, but handle gracefully)
                rs.setQj(null);
                rs.setVj(null);
            }
        }
        
        if (src2 != null && !src2.isEmpty()) {
            String producer = registerFile.getRegisterStatus(src2);
            if (producer != null && !producer.equals("")) {
                // Register is busy, waiting for producer
                rs.setQk(producer);
                rs.setVk(null);
            } else if (registerFile.isRegisterReady(src2)) {
                // Register is ready, can read value
                rs.setVk(registerFile.getRegisterValue(src2));
                rs.setQk(null);
            } else {
                // Register is busy but no producer tag (shouldn't happen, but handle gracefully)
                rs.setQk(null);
                rs.setVk(null);
            }
        }
        
        if (instruction.hasImmediate()) {
            rs.setA(instruction.getImmediate());
        }
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
    }
    
    public DataHazardHandler getHazardHandler() {
        return hazardHandler;
    }
}