package simulator.tomasulo.execute;

import simulator.tomasulo.execute.*;
import simulator.tomasulo.issue.*;
import simulator.tomasulo.registerfile.*;
import java.util.*;

public class IntegrationTest {
    public static void main(String[] args) {
        System.out.println("=== TOMASULO SIMULATOR INTEGRATION TEST ===\n");
        
        // Test 1: Basic component connection
        testComponentIntegration();
        
        // Test 2: Instruction execution flow
        testInstructionExecutionFlow();
        
        // Test 3: CDB arbitration with multiple instructions
        testCDBArbitration();
        
        System.out.println("\n✅ Integration tests completed!");
    }
    
    static void testComponentIntegration() {
        System.out.println("Test 1: Basic Component Integration");
        System.out.println("------------------------------------");
        
        try {
            // Create components from all members
            RegisterFile registerFile = new RegisterFile();
            RegisterAliasTable rat = new RegisterAliasTable();
            ReservationStationPool rsPool = new ReservationStationPool();
            
            // Create Member 2 components
            CommonDataBus cdb = CommonDataBus.getInstance();
            BroadcastManager broadcastManager = new BroadcastManager(rsPool, registerFile, rat);
            ExecutionUnit execUnit = new ExecutionUnit();
            WriteBackUnit wbUnit = new WriteBackUnit(broadcastManager);
            
            // Initialize register file
            registerFile.initializeRegisters();
            
            System.out.println("✓ All components created successfully:");
            System.out.println("  - RegisterFile: " + registerFile.getClass().getSimpleName());
            System.out.println("  - RegisterAliasTable: " + rat.getClass().getSimpleName());
            System.out.println("  - ReservationStationPool: " + rsPool.getClass().getSimpleName());
            System.out.println("  - CommonDataBus: " + cdb.getClass().getSimpleName());
            System.out.println("  - BroadcastManager: " + broadcastManager.getClass().getSimpleName());
            System.out.println("  - ExecutionUnit: " + execUnit.getClass().getSimpleName());
            System.out.println("  - WriteBackUnit: " + wbUnit.getClass().getSimpleName());
            
            // Test some basic operations
            System.out.println("\nTesting basic operations:");
            
            // Test register file
            registerFile.writeValue("F1", 10.5);
            double value = registerFile.readValue("F1");
            System.out.println("  ✓ Register F1 write/read: " + value);
            
            // Test reservation station pool
            ReservationStation rs = rsPool.allocateStation("FP_ADD");
            if (rs != null) {
                System.out.println("  ✓ Allocated reservation station: " + rs.getName());
                rsPool.releaseStation(rs);
                System.out.println("  ✓ Released reservation station");
            }
            
            System.out.println("✓ Basic integration test passed\n");
            
        } catch (Exception e) {
            System.err.println("Integration error: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
// In IntegrationTest.java, change testInstructionExecutionFlow():

	static void testInstructionExecutionFlow() {
	    System.out.println("Test 2: Instruction Execution Flow");
	    System.out.println("-----------------------------------");
	    
	    try {
	        // Setup components
	        RegisterFile registerFile = new RegisterFile();
	        registerFile.initializeRegisters();
	        
	        // Preload some register values
	        Map<String, Double> initialValues = new HashMap<>();
	        initialValues.put("F0", 5.0);
	        initialValues.put("F4", 3.0);
	        registerFile.preloadValues(initialValues);
	        
	        RegisterAliasTable rat = new RegisterAliasTable();
	        ReservationStationPool rsPool = new ReservationStationPool();
	        BroadcastManager broadcastManager = new BroadcastManager(rsPool, registerFile, rat);
	        ExecutionUnit execUnit = new ExecutionUnit();
	        WriteBackUnit wbUnit = new WriteBackUnit(broadcastManager);
	        
	        System.out.println("Simulating: ADD.D F2, F0, F4");
	        System.out.println("(F0 = 5.0, F4 = 3.0)");
	        
	        // Allocate a reservation station - use Add1 (RS1 equivalent)
	        ReservationStation addStation = rsPool.allocateStation("FP_ADD");
	        if (addStation == null) {
	            System.out.println("✗ No available FP_ADD station");
	            return;
	        }
	        
	        System.out.println("Allocated station: " + addStation.getName());
	        
	        // Configure the reservation station
	        addStation.setBusy(true);
	        addStation.setOp("ADD.D");
	        addStation.setVj(5.0);  // F0 value
	        addStation.setVk(3.0);  // F4 value
	        addStation.setQj(null); // No dependencies
	        addStation.setQk(null); // No dependencies
	        
	        // Start execution using STATION NAME, not RS ID
	        execUnit.startExecution(addStation.getName(), "ADD.D", 2, 2, 0);
	        
	        // Run simulation for 4 cycles
	        for (int cycle = 1; cycle <= 4; cycle++) {
	            System.out.println("\n--- Cycle " + cycle + " ---");
	            
	            // Execute stage
	            execUnit.cycle(cycle);
	            
	            // Write-back stage
	            wbUnit.writeBackCycle(cycle);
	            
	            // Check register F2
	            try {
	                double f2Value = registerFile.readValue("F2");
	                System.out.println("  F2 value: " + f2Value);
	            } catch (Exception e) {
	                System.out.println("  F2 not ready yet");
	            }
	        }
	        
	        System.out.println("✓ Instruction flow test completed\n");
	        
	    } catch (Exception e) {
	        System.err.println("Instruction flow error: " + e.getMessage());
	        e.printStackTrace();
	    }
	}

    static void testCDBArbitration() {
        System.out.println("Test 3: CDB Arbitration with Multiple Instructions");
        System.out.println("--------------------------------------------------");
        
        try {
            // Setup
            RegisterFile registerFile = new RegisterFile();
            registerFile.initializeRegisters();
            RegisterAliasTable rat = new RegisterAliasTable();
            ReservationStationPool rsPool = new ReservationStationPool();
            BroadcastManager broadcastManager = new BroadcastManager(rsPool, registerFile, rat);
            ExecutionUnit execUnit = new ExecutionUnit();
            WriteBackUnit wbUnit = new WriteBackUnit(broadcastManager);
            
            System.out.println("Testing CDB priority with mixed instruction types:");
            System.out.println("1. DADDI R1, R0, 10  (priority 4, latency 1)");
            System.out.println("2. ADD.D F2, F0, F4  (priority 3, latency 2)");
            System.out.println("3. LW R2, 0(R3)      (priority 1, latency 2)");
            
            // Manually test CDB arbitration
            CommonDataBus cdb = CommonDataBus.getInstance();
            
            // Create broadcast requests with different priorities
            cdb.addBroadcastRequest(new BroadcastRequest(1, 10.0, 1, "LW"));      // Priority 1
            cdb.addBroadcastRequest(new BroadcastRequest(2, 20.0, 2, "ADD.D"));    // Priority 3
            cdb.addBroadcastRequest(new BroadcastRequest(3, 30.0, 3, "DADDI"));    // Priority 4
            
            // Process cycle 1 - should select DADDI first (highest priority)
            System.out.println("\nCycle 1 - CDB arbitration:");
            List<BroadcastRequest> broadcasts = cdb.processBroadcasts(1);
            System.out.println("Selected: " + broadcasts);
            
            if (!broadcasts.isEmpty() && broadcasts.get(0).getInstrType().equals("DADDI")) {
                System.out.println("✓ PASS: DADDI (priority 4) selected before ADD.D (3) and LW (1)");
            }
            
            // Clear for next test
            cdb.clear();
            
            // Test execution timing
            System.out.println("\nTesting execution timing:");
            execUnit.startExecution(1, "ADD.D", 10, 2, 0);
            execUnit.startExecution(2, "MUL.D", 11, 4, 0);
            
            for (int i = 1; i <= 5; i++) {
                System.out.println("Cycle " + i + ": " + execUnit.getExecutionTimers());
                execUnit.cycle(i);
                wbUnit.writeBackCycle(i);
            }
            
            System.out.println("✓ CDB arbitration test completed\n");
            
        } catch (Exception e) {
            System.err.println("CDB arbitration error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}