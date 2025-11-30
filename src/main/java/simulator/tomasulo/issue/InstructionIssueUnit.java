package simulator.tomasulo.issue;

 import java.util.*;
import simulator.parser.*;
import simulator.tomasulo.registerfile.*;
import simulator.tomasulo.models.Instruction;
import simulator.tomasulo.issue.*;

 public class InstructionIssueUnit {
     private ReservationStationPool rsPool;
     private InstructionQueue instructionQueue;
     private RegisterFile registerFile;
     private int currentCycle;
     
     // Statistics
     private Map<Integer, Integer> issueCycles; // instruction ID -> issue cycle
     
     public InstructionIssueUnit(ReservationStationPool rsPool, 
                                 InstructionQueue instructionQueue,
                                 RegisterFile registerFile) {
         this.rsPool = rsPool;
         this.instructionQueue = instructionQueue;
         this.registerFile = registerFile;
         this.currentCycle = 0;
         this.issueCycles = new HashMap<>();
     }
     
     /**
      * Attempts to issue the next instruction in the queue
      * Returns true if instruction was successfully issued
      */
     public boolean issueNextInstruction() {
         if (instructionQueue.isEmpty()) {
             return false;
         }
         
         Instruction instruction = instructionQueue.peek();
         
         // Check for structural hazard - is there an available reservation station?
         StructuralHazard hazard = checkStructuralHazard(instruction);
         if (hazard.hasHazard()) {
             // Cannot issue - structural hazard exists
             System.out.println("Cycle " + currentCycle + ": Structural hazard - " 
                              + hazard.getMessage());
             return false;
         }
         
         // Get appropriate reservation station type
         String rsType = determineReservationStationType(instruction);
         ReservationStation rs = rsPool.allocateStation(rsType);
         
         if (rs == null) {
             // Should not happen if checkStructuralHazard works correctly
             return false;
         }
         
         // Issue the instruction to the reservation station
         issueToReservationStation(instruction, rs);
         
         // Update register file status for destination register
         if (instruction.hasDestination()) {
             registerFile.setRegisterStatus(instruction.getDestRegister(), rs.getName());
         }
         
         // Record issue cycle
         issueCycles.put(instruction.getId(), currentCycle);
         instruction.setIssueCycle(currentCycle);
         
         // Remove from queue
         instructionQueue.dequeue();
         
         System.out.println("Cycle " + currentCycle + ": Issued " 
                          + instruction.toString() + " to " + rs.getName());
         
         return true;
     }
     
     /**
      * Issues instruction to a specific reservation station
      * Handles register renaming and operand fetching
      */
     private void issueToReservationStation(Instruction instruction, ReservationStation rs) {
         rs.setBusy(true);
         rs.setOp(instruction.getOperation());
         rs.setInstruction(instruction);
         
         // Handle source operands
         String src1 = instruction.getSourceRegister1();
         String src2 = instruction.getSourceRegister2();
         
         // Process first source operand
         if (src1 != null && !src1.isEmpty()) {
             String producer = registerFile.getRegisterStatus(src1);
             if (producer != null && !producer.equals("")) {
                 // RAW hazard - register is being computed
                 rs.setQj(producer);
                 rs.setVj(null); // Value not available yet
             } else {
                 // Value is available
                 rs.setVj(registerFile.getRegisterValue(src1));
                 rs.setQj(null);
             }
         }
         
         // Process second source operand
         if (src2 != null && !src2.isEmpty()) {
             String producer = registerFile.getRegisterStatus(src2);
             if (producer != null && !producer.equals("")) {
                 // RAW hazard - register is being computed
                 rs.setQk(producer);
                 rs.setVk(null);
             } else {
                 // Value is available
                 rs.setVk(registerFile.getRegisterValue(src2));
                 rs.setQk(null);
             }
         }
         
         // Handle immediate values (for ADDI, SUBI, etc.)
         if (instruction.hasImmediate()) {
             rs.setA(instruction.getImmediate());
         }
         
         // For load/store instructions, compute effective address components
         if (instruction.isMemoryOperation()) {
             // Base register value
             String baseReg = instruction.getBaseRegister();
             if (baseReg != null) {
                 String producer = registerFile.getRegisterStatus(baseReg);
                 if (producer != null && !producer.equals("")) {
                     rs.setQj(producer);
                     rs.setVj(null);
                 } else {
                     rs.setVj(registerFile.getRegisterValue(baseReg));
                     rs.setQj(null);
                 }
             }
             // Offset
             rs.setA(instruction.getOffset());
         }
     }
     
     /**
      * Checks if there's a structural hazard for the given instruction
      */
     private StructuralHazard checkStructuralHazard(Instruction instruction) {
         String rsType = determineReservationStationType(instruction);
         
         if (!rsPool.hasAvailableStation(rsType)) {
             return new StructuralHazard(true, 
                 "No available " + rsType + " reservation stations");
         }
         
         return new StructuralHazard(false, "");
     }
     
     /**
      * Determines which type of reservation station is needed for this instruction
      */
     private String determineReservationStationType(Instruction instruction) {
         String op = instruction.getOperation().toUpperCase();
         
         if (op.contains("ADD") || op.contains("SUB")) {
             if (op.startsWith("D")) {
                 // Integer operations (DADDI, DSUBI)
                 return "INTEGER_ADD";
             } else {
                 // Floating point operations (ADD.D, SUB.D)
                 return "FP_ADD";
             }
         } else if (op.contains("MUL")) {
             return "FP_MUL";
         } else if (op.contains("DIV")) {
             return "FP_DIV";
         } else if (op.startsWith("L")) {
             // Load instructions
             return "LOAD";
         } else if (op.startsWith("S")) {
             // Store instructions
             return "STORE";
         } else if (op.equals("BEQ") || op.equals("BNE")) {
             // Branch instructions
             return "BRANCH";
         } else if (op.equals("ADDI") || op.equals("SUBI")) {
             // Integer immediate operations
             return "INTEGER_ADD";
         }
         
         return "FP_ADD"; // Default
     }
     
     /**
      * Advances to the next cycle
      */
     public void nextCycle() {
         currentCycle++;
     }
     
     /**
      * Gets the current cycle number
      */
     public int getCurrentCycle() {
         return currentCycle;
     }
     
     /**
      * Gets the issue cycle for a specific instruction
      */
     public Integer getIssueCycle(int instructionId) {
         return issueCycles.get(instructionId);
     }
     
     /**
      * Resets the issue unit for a new simulation
      */
     public void reset() {
         currentCycle = 0;
         issueCycles.clear();
     }
 }