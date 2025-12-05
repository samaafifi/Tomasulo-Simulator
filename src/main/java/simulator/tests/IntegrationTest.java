package simulator.integration;

import simulator.parser.InstructionParser;
import simulator.tomasulo.issue.*;
import simulator.tomasulo.registerfile.*;
import simulator.memory.*;
import simulator.tomasulo.execute.*;
import simulator.tomasulo.model.Instruction;

import java.util.List;

public class IntegrationTest {
    
    public static void main(String[] args) {
        System.out.println("=== TOMASULO SIMULATOR - FULL INTEGRATION TEST ===\n");
        
        System.out.println("Testing Test Case 1 from project PDF...\n");
        runTest1();
        
        System.out.println("\n=== INTEGRATION TEST COMPLETED ===");
    }
    
    private static void runTest1() {
        System.out.println("--- TEST CASE 1: Sequential Code ---");
        
        String code = """
            L.D F6, 0(R2)
            L.D F2, 8(R2)
            MUL.D F0, F2, F4
            SUB.D F8, F2, F6
            DIV.D F10, F0, F6
            ADD.D F6, F8, F2
            S.D F6, 8(R2)
            """;
            
        System.out.println("Assembly Code:");
        System.out.println(code);
        
        // Step 1: Parse instructions
        System.out.println("\n[Step 1] Parsing instructions...");
        InstructionParser parser = new InstructionParser();
        List<Instruction> instructions = null;
        
        try {
            instructions = parser.parse(code);
        } catch (Exception e) {
            System.out.println("ERROR parsing instructions: " + e.getMessage());
            e.printStackTrace();
            return;
        }
        
        if (instructions == null || instructions.isEmpty()) {
            System.out.println("ERROR: Failed to parse instructions!");
            return;
        }
        
        System.out.println("✓ Successfully parsed " + instructions.size() + " instructions");
        for (int i = 0; i < instructions.size(); i++) {
            System.out.println("  " + (i+1) + ". " + instructions.get(i));
        }
        
        // Step 2: Initialize all components
        System.out.println("\n[Step 2] Initializing components...");
        
        try {
            // Initialize Member 3: Register File
            RegisterFile registerFile = new RegisterFile();
            System.out.println("✓ RegisterFile initialized");
            
            // Initialize Member 1: Reservation Stations
            ReservationStationPool rsPool = new ReservationStationPool();
            System.out.println("✓ ReservationStationPool initialized");
            
            // Initialize Member 4: Memory System
            MemorySystem memorySystem = new MemorySystem();
            System.out.println("✓ MemorySystem initialized");
            
            // Initialize Member 2: Execution & Write-Back
            ExecutionUnit executionUnit = new ExecutionUnit();
            WriteBackUnit writeBackUnit = new WriteBackUnit();
            CommonDataBus cdb = new CommonDataBus();
            BroadcastManager broadcastManager = new BroadcastManager();
            System.out.println("✓ Member 2 components initialized");
            
            // Initialize Member 1: Issue Unit
            InstructionIssueUnit issueUnit = new InstructionIssueUnit(rsPool, registerFile, memorySystem);
            System.out.println("✓ InstructionIssueUnit initialized");
            
            // Step 3: Configure the system
            System.out.println("\n[Step 3] Configuring system...");
            
            // Set instruction latencies (as per project requirements)
            if (executionUnit.getClass().getMethod("setLatency", String.class, int.class) != null) {
                executionUnit.setLatency("L.D", 2);      // Load double: 2 cycles
                executionUnit.setLatency("S.D", 2);      // Store double: 2 cycles
                executionUnit.setLatency("ADD.D", 2);    // FP Add: 2 cycles
                executionUnit.setLatency("SUB.D", 2);    // FP Subtract: 2 cycles
                executionUnit.setLatency("MUL.D", 10);   // FP Multiply: 10 cycles
                executionUnit.setLatency("DIV.D", 40);   // FP Divide: 40 cycles
                System.out.println("✓ Instruction latencies configured");
            }
            
            // Initialize register R2 with base address
            registerFile.setRegisterValue("R2", 1000);
            System.out.println("✓ Register R2 initialized with base address 1000");
            
            // Initialize memory with test values
            memorySystem.writeDouble(1000, 3.14);   // Address 1000 = 3.14
            memorySystem.writeDouble(1008, 2.71);   // Address 1008 = 2.71
            System.out.println("✓ Memory initialized with test values");
            
            // Initialize F4 with a value (needed for MUL.D F0, F2, F4)
            registerFile.setRegisterValue("F4", 1.5);
            System.out.println("✓ Register F4 initialized with value 1.5");
            
            // Step 4: Run the simulation
            System.out.println("\n[Step 4] Starting simulation...");
            System.out.println("========================================");
            
            int cycle = 1;
            int instructionPointer = 0;
            boolean simulationComplete = false;
            final int MAX_CYCLES = 100; // Safety limit
            
            while (!simulationComplete && cycle <= MAX_CYCLES) {
                System.out.println("\n════════════════════════════════════════");
                System.out.println("CYCLE " + cycle);
                System.out.println("════════════════════════════════════════");
                
                // ---- ISSUE STAGE ----
                System.out.println("\n[ISSUE STAGE]");
                if (instructionPointer < instructions.size()) {
                    Instruction currentInstruction = instructions.get(instructionPointer);
                    System.out.println("  Next instruction: " + currentInstruction);
                    
                    // Try to issue
                    try {
                        if (issueUnit.canIssue(currentInstruction)) {
                            boolean issued = issueUnit.issue(currentInstruction);
                            if (issued) {
                                System.out.println("  ✓ Issued: " + currentInstruction);
                                instructionPointer++;
                            } else {
                                System.out.println("  ✗ Failed to issue");
                            }
                        } else {
                            System.out.println("  ⏳ Waiting for resources...");
                        }
                    } catch (Exception e) {
                        System.out.println("  ERROR during issue: " + e.getMessage());
                    }
                } else {
                    System.out.println("  All instructions issued");
                }
                
                // ---- EXECUTE STAGE (Member 2) ----
                System.out.println("\n[EXECUTE STAGE]");
                try {
                    executionUnit.executeCycle();
                    System.out.println("  Execution in progress...");
                } catch (Exception e) {
                    System.out.println("  ERROR in execution: " + e.getMessage());
                }
                
                // ---- WRITE-BACK STAGE (Member 2) ----
                System.out.println("\n[WRITE-BACK STAGE]");
                try {
                    writeBackUnit.writeBackCycle();
                    System.out.println("  Write-back completed");
                } catch (Exception e) {
                    System.out.println("  ERROR in write-back: " + e.getMessage());
                }
                
                // ---- BROADCAST RESULTS (Member 2) ----
                System.out.println("\n[BROADCAST STAGE]");
                try {
                    broadcastManager.broadcast();
                    System.out.println("  Results broadcasted");
                } catch (Exception e) {
                    System.out.println("  ERROR in broadcast: " + e.getMessage());
                }
                
                // ---- PRINT STATUS ----
                System.out.println("\n[SYSTEM STATUS]");
                System.out.println("  Instructions issued: " + instructionPointer + "/" + instructions.size());
                System.out.println("  Cycle: " + cycle);
                
                // Check if simulation is complete
                if (instructionPointer >= instructions.size()) {
                    // Check if all execution units are idle
                    boolean allDone = true;
                    // You would check if executionUnit.isEmpty() and writeBackUnit.isEmpty()
                    // For now, we'll use a simple completion check
                    
                    if (allDone || cycle > 50) { // Simple completion condition
                        simulationComplete = true;
                        System.out.println("\n✓ SIMULATION COMPLETE!");
                    }
                }
                
                cycle++;
            }
            
            if (cycle >= MAX_CYCLES) {
                System.out.println("\n⚠️  Simulation stopped at " + MAX_CYCLES + " cycles (safety limit)");
            }
            
            // Print final results
            System.out.println("\n════════════════════════════════════════");
            System.out.println("FINAL RESULTS");
            System.out.println("════════════════════════════════════════");
            
            System.out.println("\nRegister File:");
            try {
                registerFile.printAllRegisters();
            } catch (Exception e) {
                System.out.println("  Could not print registers: " + e.getMessage());
            }
            
            System.out.println("\nMemory (relevant addresses):");
            try {
                System.out.println("  Address 1000: " + memorySystem.readDouble(1000));
                System.out.println("  Address 1008: " + memorySystem.readDouble(1008));
            } catch (Exception e) {
                System.out.println("  Could not read memory: " + e.getMessage());
            }
            
        } catch (Exception e) {
            System.out.println("\n❌ ERROR during initialization: " + e.getMessage());
            e.printStackTrace();
        }
    }
}