// In IntegrationTest.java, change testInstructionExecutionFlow():

//package simulator.tomasulo.execute;

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