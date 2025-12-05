package simulator.tests;

import simulator.parser.*;
import simulator.tomasulo.issue.*;
import simulator.tomasulo.registerfile.*;
import simulator.memory.*;
import simulator.tomasulo.execute.*;
import simulator.tomasulo.hazards.*;
import simulator.tomasulo.models.Instruction;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;

public class IntegrationTest {
    
    public static void main(String[] args) {
        System.out.println("=== TOMASULO SIMULATOR - FULL INTEGRATION TEST ===\n");
        
        System.out.println("Testing Test Case 1 from project PDF...\n");
        runTest1();
        
        System.out.println("\n=== INTEGRATION TEST COMPLETED ===");
    }
    
    private static void runTest1() {
        System.out.println("--- TEST CASE 1: Sequential Code ---");
        
        String code = "L.D F6, 0(R2)\n" +
                     "L.D F2, 8(R2)\n" +
                     "MUL.D F0, F2, F4\n" +
                     "SUB.D F8, F2, F6\n" +
                     "DIV.D F10, F0, F6\n" +
                     "ADD.D F6, F8, F2\n" +
                     "S.D F6, 8(R2)";
            
        System.out.println("Assembly Code:");
        System.out.println(code);
        
        // Step 1: Parse instructions
        System.out.println("\n[Step 1] Parsing instructions...");
        InstructionParser parser = new InstructionParser();
        List<ParsedInstruction> parsedInstructions = null;
        
        try {
            parsedInstructions = parser.parse(code);
        } catch (Exception e) {
            System.out.println("ERROR parsing instructions: " + e.getMessage());
            e.printStackTrace();
            return;
        }
        
        if (parsedInstructions == null || parsedInstructions.isEmpty()) {
            System.out.println("ERROR: Failed to parse instructions!");
            return;
        }
        
        // Convert ParsedInstruction to Instruction
        List<Instruction> instructions = new ArrayList<>();
        for (ParsedInstruction parsed : parsedInstructions) {
            if (parsed.getType() == null) continue; // Skip label-only lines
            
            InstructionType type = parsed.getType();
            String op = type.getAssemblyName();
            String dest = parsed.getDestinationRegister();
            List<String> sources = parsed.getSourceRegisters();
            
            String src1 = null;
            String src2 = null;
            String baseReg = null;
            
            // Handle different instruction formats
            if (type.isLoad()) {
                // Load: L.D F6, 0(R2) -> dest=F6, base=R2
                src1 = sources.size() > 0 ? sources.get(0) : null; // Base register
                baseReg = src1;
            } else if (type.isStore()) {
                // Store: S.D F6, 8(R2) -> src=F6, base=R2
                src1 = sources.size() > 0 ? sources.get(0) : null; // Value register
                baseReg = sources.size() > 1 ? sources.get(1) : null; // Base register
            } else if (type.isBranch()) {
                // Branch: BEQ R1, R2, target -> src1=R1, src2=R2
                src1 = sources.size() > 0 ? sources.get(0) : null;
                src2 = sources.size() > 1 ? sources.get(1) : null;
            } else {
                // Arithmetic: ADD.D F6, F8, F2 -> dest=F6, src1=F8, src2=F2
                src1 = sources.size() > 0 ? sources.get(0) : null;
                src2 = sources.size() > 1 ? sources.get(1) : null;
            }
            
            Instruction instr = new Instruction(op, dest, src1, src2, "");
            
            // Set memory operation fields
            if (type.isMemoryOperation()) {
                if (baseReg != null) {
                    instr.setBaseRegister(baseReg);
                }
                if (parsed.getImmediate() != null) {
                    instr.setOffset(parsed.getImmediate());
                }
            }
            
            // Set immediate for integer operations
            if (type == InstructionType.DADDI || type == InstructionType.DSUBI) {
                if (parsed.getImmediate() != null) {
                    instr.setImmediate(parsed.getImmediate());
                }
            }
            
            instructions.add(instr);
        }
        
        System.out.println("Successfully parsed " + instructions.size() + " instructions");
        for (int i = 0; i < instructions.size(); i++) {
            System.out.println("  " + (i+1) + ". " + instructions.get(i));
        }
        
        // Step 2: Initialize all components
        System.out.println("\n[Step 2] Initializing components...");
        
        try {
            // Initialize Member 3: Register File
            RegisterFile registerFile = new RegisterFile();
            System.out.println("RegisterFile initialized");
            
            // Initialize Member 1: Reservation Stations
            ReservationStationPool rsPool = new ReservationStationPool();
            System.out.println("ReservationStationPool initialized");
            
            // Initialize Member 4: Memory System
            MemorySystem memorySystem = new MemorySystem();
            System.out.println("MemorySystem initialized");
            
            // Initialize Member 2: Execution & Write-Back
            ExecutionUnit executionUnit = new ExecutionUnit();
            executionUnit.setReservationStationPool(rsPool); // Connect execution unit to RS pool
            WriteBackUnit writeBackUnit = new WriteBackUnit();
            CommonDataBus cdb = new CommonDataBus();
            BroadcastManager broadcastManager = new BroadcastManager();
            System.out.println("Member 2 components initialized");
            
            // Initialize register file first
            registerFile.initializeRegisters();
            
            // Initialize Member 1: Issue Unit
            InstructionIssueUnit issueUnit = new InstructionIssueUnit(rsPool, registerFile, memorySystem);
            // Add instructions to the issue unit's queue
            issueUnit.addInstructions(instructions);
            System.out.println("InstructionIssueUnit initialized");
            
            // Step 3: Configure the system
            System.out.println("\n[Step 3] Configuring system...");
            
            // Set instruction latencies (as per project requirements)
            try {
                executionUnit.setLatency("L.D", 2);      // Load double: 2 cycles
                executionUnit.setLatency("S.D", 2);      // Store double: 2 cycles
                executionUnit.setLatency("ADD.D", 2);    // FP Add: 2 cycles
                executionUnit.setLatency("SUB.D", 2);    // FP Subtract: 2 cycles
                executionUnit.setLatency("MUL.D", 10);   // FP Multiply: 10 cycles
                executionUnit.setLatency("DIV.D", 40);   // FP Divide: 40 cycles
                System.out.println("Instruction latencies configured");
            } catch (Exception e) {
                System.out.println("Could not set latencies: " + e.getMessage());
            }
            
            // Initialize register R2 with base address
            try {
                registerFile.setRegisterValue("R2", 1000);
                System.out.println("Register R2 initialized with base address 1000");
            } catch (Exception e) {
                System.out.println("Could not set R2: " + e.getMessage());
            }
            
            // Initialize memory with test values
            try {
                memorySystem.writeDouble(1000, 3.14);   // Address 1000 = 3.14
                memorySystem.writeDouble(1008, 2.71);   // Address 1008 = 2.71
                System.out.println("Memory initialized with test values");
            } catch (Exception e) {
                System.out.println("Could not initialize memory: " + e.getMessage());
            }
            
            // Initialize F4 with a value (needed for MUL.D F0, F2, F4)
            try {
                registerFile.setRegisterValue("F4", 1.5);
                System.out.println("Register F4 initialized with value 1.5");
            } catch (Exception e) {
                System.out.println("Could not set F4: " + e.getMessage());
            }
            
            // Step 4: Run the simulation
            System.out.println("\n[Step 4] Starting simulation...");
            System.out.println("========================================");
            
            // Track instruction progress
            Map<Integer, Integer> instructionIssueCycles = new HashMap<>();
            Map<Integer, Integer> instructionExecStartCycles = new HashMap<>();
            Map<Integer, Integer> instructionCompleteCycles = new HashMap<>();
            Map<String, Integer> stationToInstructionId = new HashMap<>(); // Map station name to instruction ID
            
            int cycle = 1;
            int instructionPointer = 0;
            boolean simulationComplete = false;
            final int MAX_CYCLES = 100; // Safety limit
            
            while (!simulationComplete && cycle <= MAX_CYCLES) {
                System.out.println("\n" + "=".repeat(70));
                System.out.println("CYCLE " + cycle);
                System.out.println("=".repeat(70));
                
                // ---- ISSUE STAGE ----
                System.out.println("\n[ISSUE STAGE]");
                // Advance issue unit cycle counter before issuing
                issueUnit.nextCycle();
                
                if (instructionPointer < instructions.size()) {
                    Instruction currentInstruction = instructions.get(instructionPointer);
                    System.out.println("  Attempting to issue: " + currentInstruction);
                    
                    // Try to issue
                    try {
                        if (issueUnit.canIssue(currentInstruction)) {
                            boolean issued = issueUnit.issue(currentInstruction);
                            if (issued) {
                                System.out.println(" ISSUED: " + currentInstruction);
                                instructionIssueCycles.put(currentInstruction.getId(), cycle);
                                instructionPointer++;
                            } else {
                                System.out.println(" Failed to issue (structural hazard)");
                            }
                        } else {
                            System.out.println(" Waiting for resources (structural hazard)");
                        }
                    } catch (Exception e) {
                        System.out.println(" ERROR during issue: " + e.getMessage());
                        e.printStackTrace();
                    }
                } else {
                    System.out.println("  All instructions issued");
                }
                
                // ---- EXECUTE STAGE (Member 2) ----
                System.out.println("\n[EXECUTE STAGE]");
                try {
                    // Check which stations are ready to start execution
                    List<ReservationStation> readyStations = rsPool.getReadyStations();
                    for (ReservationStation rs : readyStations) {
                        if (!rs.isExecutionStarted() && rs.getIssueCycle() >= 0 && rs.getIssueCycle() < cycle) {
                            if (rs.getInstruction() != null) {
                                int instrId = rs.getInstruction().getId();
                                instructionExecStartCycles.put(instrId, cycle);
                                stationToInstructionId.put(rs.getName(), instrId);
                                System.out.println("  ▶ Starting execution: " + rs.getInstruction() + 
                                                 " in " + rs.getName() + " (issued cycle " + rs.getIssueCycle() + ")");
                            }
                        }
                    }
                    
                    // Track all busy stations and their instructions for completion detection
                    List<ReservationStation> allBusyStations = rsPool.getBusyStations();
                    for (ReservationStation rs : allBusyStations) {
                        if (rs.getInstruction() != null && !stationToInstructionId.containsKey(rs.getName())) {
                            stationToInstructionId.put(rs.getName(), rs.getInstruction().getId());
                        }
                    }
                    
                    executionUnit.cycle(cycle); // Pass cycle number explicitly
                    
                    // Show active executions
                    Map<String, Integer> timers = executionUnit.getExecutionTimers();
                    if (!timers.isEmpty()) {
                        System.out.println("  Active executions:");
                        for (Map.Entry<String, Integer> entry : timers.entrySet()) {
                            System.out.println("    " + entry.getKey() + ": " + entry.getValue() + " cycles remaining");
                        }
                    }
                } catch (Exception e) {
                    System.out.println("  ✗ ERROR in execution: " + e.getMessage());
                    e.printStackTrace();
                }
                
                // ---- WRITE-BACK STAGE (Member 2) ----
                System.out.println("\n[WRITE-BACK STAGE]");
                try {
                    writeBackUnit.writeBackCycle(cycle);
                } catch (Exception e) {
                    System.out.println("  ✗ ERROR in write-back: " + e.getMessage());
                    e.printStackTrace();
                }
                
                // ---- BROADCAST RESULTS (Member 2) ----
                System.out.println("\n[BROADCAST STAGE]");
                try {
                    // Check stations before broadcast to detect completions
                    List<ReservationStation> stationsBeforeBroadcast = rsPool.getBusyStations();
                    Map<String, Integer> stationNamesBefore = new HashMap<>();
                    for (ReservationStation rs : stationsBeforeBroadcast) {
                        if (rs.isExecutionStarted() && rs.getRemainingCycles() == 0) {
                            stationNamesBefore.put(rs.getName(), rs.getInstruction() != null ? rs.getInstruction().getId() : -1);
                        }
                    }
                    
                    broadcastManager.broadcast();
                    
                    // Check which stations were released (completed)
                    List<ReservationStation> stationsAfterBroadcast = rsPool.getBusyStations();
                    Map<String, Boolean> stationsAfterMap = new HashMap<>();
                    for (ReservationStation rs : stationsAfterBroadcast) {
                        stationsAfterMap.put(rs.getName(), true);
                    }
                    
                    for (Map.Entry<String, Integer> entry : stationNamesBefore.entrySet()) {
                        String stationName = entry.getKey();
                        int instrId = entry.getValue();
                        if (!stationsAfterMap.containsKey(stationName) || !stationsAfterMap.get(stationName)) {
                            // Station was released, instruction completed
                            if (instrId >= 0 && !instructionCompleteCycles.containsKey(instrId)) {
                                instructionCompleteCycles.put(instrId, cycle);
                                // Find instruction
                                for (Instruction instr : instructions) {
                                    if (instr.getId() == instrId) {
                                        System.out.println("  ✓ Completed and broadcasted: " + instr);
                                        break;
                                    }
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    System.out.println("  ERROR in broadcast: " + e.getMessage());
                    e.printStackTrace();
                }
                
                // ---- PRINT RESERVATION STATIONS STATUS ----
                System.out.println("\n[RESERVATION STATIONS]");
                try {
                    List<ReservationStation> busyStations = rsPool.getBusyStations();
                    if (busyStations.isEmpty()) {
                        System.out.println("  No busy stations");
                    } else {
                        System.out.println("  Busy stations (" + busyStations.size() + "):");
                        for (ReservationStation rs : busyStations) {
                            String status = rs.isReadyToExecute() ? "READY" : "WAITING";
                            String execStatus = rs.isExecutionStarted() ? " (EXECUTING)" : "";
                            System.out.println(String.format("    %-8s | %-8s | %-6s | Issue:%2d | Cycles:%2d | Qj:%-6s | Qk:%-6s",
                                rs.getName(), rs.getOp() != null ? rs.getOp() : "-", 
                                status + execStatus, rs.getIssueCycle(), rs.getRemainingCycles(),
                                rs.getQj() != null ? rs.getQj() : "-",
                                rs.getQk() != null ? rs.getQk() : "-"));
                        }
                    }
                } catch (Exception e) {
                    System.out.println("  ✗ Could not display reservation stations: " + e.getMessage());
                }
                
                // ---- PRINT REGISTER FILE STATUS ----
                System.out.println("\n[REGISTER FILE]");
                try {
                    // Show registers that are busy or have non-zero values
                    Map<String, Register> registers = registerFile.getAllRegisters();
                    boolean hasBusyRegs = false;
                    for (Map.Entry<String, Register> entry : registers.entrySet()) {
                        Register reg = entry.getValue();
                        if (reg.isBusy() || (reg.getValue() != 0.0 && entry.getKey().startsWith("F"))) {
                            if (!hasBusyRegs) {
                                System.out.println("  Active registers:");
                                hasBusyRegs = true;
                            }
                            String qiStatus = reg.isBusy() ? " (waiting for " + reg.getQi() + ")" : "";
                            System.out.println(String.format("    %-4s = %10.2f%s", 
                                entry.getKey(), reg.getValue(), qiStatus));
                        }
                    }
                    if (!hasBusyRegs) {
                        System.out.println("  No active registers");
                    }
                } catch (Exception e) {
                    System.out.println("  ✗ Could not display register file: " + e.getMessage());
                }
                
                // ---- PRINT INSTRUCTION PROGRESS ----
                System.out.println("\n[INSTRUCTION PROGRESS]");
                for (int i = 0; i < instructions.size(); i++) {
                    Instruction instr = instructions.get(i);
                    int id = instr.getId();
                    String issueCycle = instructionIssueCycles.containsKey(id) ? 
                                       String.valueOf(instructionIssueCycles.get(id)) : "-";
                    String execCycle = instructionExecStartCycles.containsKey(id) ? 
                                      String.valueOf(instructionExecStartCycles.get(id)) : "-";
                    String completeCycle = instructionCompleteCycles.containsKey(id) ? 
                                           String.valueOf(instructionCompleteCycles.get(id)) : "-";
                    
                    String status = "";
                    if (completeCycle != "-") {
                        status = "COMPLETE";
                    } else if (execCycle != "-") {
                        status = "EXECUTING";
                    } else if (issueCycle != "-") {
                        status = "ISSUED";
                    } else {
                        status = "PENDING";
                    }
                    
                    System.out.println(String.format("  %d. %-30s | Issue:%3s | Exec:%3s | Complete:%3s | [%s]",
                        i+1, instr.toString(), issueCycle, execCycle, completeCycle, status));
                }
                
                // Check if simulation is complete
                // Complete when all instructions are issued AND all reservation stations are free
                if (instructionPointer >= instructions.size()) {
                    List<ReservationStation> busyStations = rsPool.getBusyStations();
                    if (busyStations.isEmpty()) {
                        simulationComplete = true;
                        System.out.println("\n✓ SIMULATION COMPLETE! All instructions finished.");
                    }
                }
                
                // Safety check
                if (cycle >= MAX_CYCLES) {
                    System.out.println("\n⚠ WARNING: Reached maximum cycle limit (" + MAX_CYCLES + ")");
                    break;
                }
                
                cycle++;
            }
            
            // Print final results
            System.out.println("\n" + "=".repeat(70));
            System.out.println("FINAL RESULTS");
            System.out.println("=".repeat(70));
            
            System.out.println("\n[INSTRUCTION SUMMARY]");
            System.out.println("Total cycles: " + (cycle - 1));
            System.out.println("Instructions completed: " + instructionCompleteCycles.size() + "/" + instructions.size());
            
            System.out.println("\n[FINAL REGISTER FILE]");
            try {
                registerFile.printAllRegisters();
            } catch (Exception e) {
                System.out.println("  ✗ Could not print registers: " + e.getMessage());
            }
            
            System.out.println("\n[FINAL MEMORY VALUES]");
            try {
                System.out.println("  Address 1000 (0(R2)): " + memorySystem.readDouble(1000));
                System.out.println("  Address 1008 (8(R2)): " + memorySystem.readDouble(1008));
            } catch (Exception e) {
                System.out.println("  ✗ Could not read memory: " + e.getMessage());
            }
            
            System.out.println("\n[FINAL RESERVATION STATIONS]");
            try {
                rsPool.printStatus();
            } catch (Exception e) {
                System.out.println("  ✗ Could not print reservation stations: " + e.getMessage());
            }
            
            System.out.println("\n[EXPECTED RESULTS]");
            System.out.println("  After execution:");
            System.out.println("    F6 should contain: ADD.D result (F8 + F2)");
            System.out.println("    F2 should contain: 2.71 (from L.D F2, 8(R2))");
            System.out.println("    F0 should contain: MUL.D result (F2 * F4)");
            System.out.println("    F8 should contain: SUB.D result (F2 - F6)");
            System.out.println("    F10 should contain: DIV.D result (F0 / F6)");
            System.out.println("    Memory[1008] should contain: F6 value (from S.D)");
            
        } catch (Exception e) {
            System.out.println("\nERROR during initialization: " + e.getMessage());
            e.printStackTrace();
        }
    }
}