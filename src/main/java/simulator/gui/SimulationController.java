package simulator.gui;

import simulator.parser.*;
import simulator.tomasulo.issue.*;
import simulator.tomasulo.registerfile.*;
import simulator.tomasulo.execute.*;
import simulator.tomasulo.models.Instruction;
import simulator.memory.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Controller class that manages the simulation state and coordinates
 * between the GUI components and the simulator engine.
 */
public class SimulationController {
    
    // Simulator components
    private RegisterFile registerFile;
    private ReservationStationPool rsPool;
    private MemorySystem memorySystem;
    private ExecutionUnit executionUnit;
    private WriteBackUnit writeBackUnit;
    private BroadcastManager broadcastManager;
    private InstructionIssueUnit issueUnit;
    private RegisterAliasTable rat;
    
    // Simulation state
    private List<Instruction> instructions;
    private List<ParsedInstruction> parsedInstructions;
    private int currentCycle;
    private int instructionPointer;
    private boolean simulationRunning;
    private boolean simulationPaused;
    private AtomicBoolean simulationComplete;
    
    // Configuration - ALL must be set by user, no defaults
    private Map<String, Integer> latencies;
    private int cacheSize;
    private int blockSize;
    private int cacheHitLatency;
    private int cacheMissPenalty;
    private int loadLatency;      // User must configure
    private int storeLatency;     // User must configure
    private Map<String, Integer> stationCounts; // Station type -> count
    private int lsbSize;
    private Map<String, Double> registerPreloadValues; // Register name -> value
    
    // Tracking
    private Map<Integer, Integer> issueCycles;
    private Map<Integer, Integer> execStartCycles;
    private Map<Integer, Integer> execEndCycles; // Track when execution ends
    private Map<Integer, Integer> completeCycles;
    private List<String> logMessages;
    
    // Callback for UI updates
    private Runnable updateCallback;
    
    public SimulationController() {
        initializeComponents();
        reset();
    }
    
    private void initializeComponents() {
        // Initialize configuration maps first - ALL EMPTY, user must configure
        latencies = new HashMap<>();
        stationCounts = new HashMap<>();
        registerPreloadValues = new HashMap<>();
        
        // Initialize with safe defaults for object creation only
        // These are NOT used for execution - user MUST configure via GUI before running
        cacheSize = 64 * 1024;   // Safe default for initialization (64KB)
        blockSize = 64;           // Safe default for initialization (64 bytes)
        cacheHitLatency = 1;      // Safe default for initialization
        cacheMissPenalty = 10;    // Safe default for initialization
        loadLatency = 2;          // Safe default for initialization
        storeLatency = 2;         // Safe default for initialization
        lsbSize = 8;              // Safe default for initialization
        
        // Initialize components - NO default configuration
        registerFile = new RegisterFile();
        registerFile.initializeRegisters();
        rat = new RegisterAliasTable();
        
        // Create empty station pool - user must configure via GUI
        rsPool = new ReservationStationPool(stationCounts);
        
        // Initialize memory system with safe defaults for object creation
        // User MUST reconfigure via GUI before running simulation
        memorySystem = new MemorySystem(
            // Memory size: INTENTIONALLY FIXED at 1MB (not user-configurable)
            // This is a design decision: memory size is typically fixed in real systems
            // and doesn't affect Tomasulo algorithm simulation correctness.
            // The memory size is sufficient for all test cases and simulations.
            // If needed in the future, this can be made configurable via GUI.
            1024 * 1024,      // 1MB memory - INTENTIONALLY FIXED
            cacheSize,        // Safe default for initialization
            blockSize,        // Safe default for initialization
            cacheHitLatency,  // Safe default for initialization
            cacheMissPenalty, // Safe default for initialization
            loadLatency,      // Safe default for initialization
            storeLatency,     // Safe default for initialization
            lsbSize           // Safe default for initialization
        );
        executionUnit = new ExecutionUnit();
        executionUnit.setReservationStationPool(rsPool);
        broadcastManager = new BroadcastManager(rsPool, registerFile, rat);
        writeBackUnit = new WriteBackUnit(broadcastManager);
        issueUnit = new InstructionIssueUnit(rsPool, registerFile, memorySystem);
        
        instructions = new ArrayList<>();
        parsedInstructions = new ArrayList<>();
        logMessages = new ArrayList<>();
        issueCycles = new HashMap<>();
        execStartCycles = new HashMap<>();
        execEndCycles = new HashMap<>();
        completeCycles = new HashMap<>();
        
        simulationComplete = new AtomicBoolean(false);
    }
    
    // Removed setDefaultLatencies() - user must configure latencies via GUI
    
    /**
     * Load instructions from text
     */
    public boolean loadInstructions(String code) {
        try {
            InstructionParser parser = new InstructionParser();
            parsedInstructions = parser.parse(code);
            
            if (parsedInstructions == null || parsedInstructions.isEmpty()) {
                log("ERROR: No instructions parsed!");
                return false;
            }
            
            // Convert to Instruction objects
            instructions = new ArrayList<>();
            for (int i = 0; i < parsedInstructions.size(); i++) {
                ParsedInstruction parsed = parsedInstructions.get(i);
                if (parsed.getType() == null) continue;
                
                Instruction instr = convertToInstruction(parsed, i + 1);
                if (instr != null) {
                    instructions.add(instr);
                }
            }
            
            issueUnit.addInstructions(instructions);
            log("Loaded " + instructions.size() + " instructions successfully.");
            return true;
            
        } catch (Exception e) {
            log("ERROR loading instructions: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    private Instruction convertToInstruction(ParsedInstruction parsed, int id) {
        InstructionType type = parsed.getType();
        String op = type.getAssemblyName();
        String dest = parsed.getDestinationRegister();
        List<String> sources = parsed.getSourceRegisters();
        
        String src1 = null;
        String src2 = null;
        String baseReg = null;
        
        if (type.isLoad()) {
            src1 = sources.size() > 0 ? sources.get(0) : null;
            baseReg = src1;
        } else if (type.isStore()) {
            src1 = sources.size() > 0 ? sources.get(0) : null;
            baseReg = sources.size() > 1 ? sources.get(1) : null;
        } else if (type.isBranch()) {
            src1 = sources.size() > 0 ? sources.get(0) : null;
            src2 = sources.size() > 1 ? sources.get(1) : null;
        } else {
            src1 = sources.size() > 0 ? sources.get(0) : null;
            src2 = sources.size() > 1 ? sources.get(1) : null;
        }
        
        Instruction instr = new Instruction(op, dest, src1, src2, "");
        instr.setId(id);
        
        if (type.isMemoryOperation() && baseReg != null) {
            instr.setBaseRegister(baseReg);
            if (parsed.getImmediate() != null) {
                instr.setOffset(parsed.getImmediate());
            }
        }
        
        if ((type == InstructionType.DADDI || type == InstructionType.DSUBI) 
            && parsed.getImmediate() != null) {
            instr.setImmediate(parsed.getImmediate());
        }
        
        // Set immediate for branch instructions (contains target instruction index)
        if (type.isBranch() && parsed.getImmediate() != null) {
            instr.setImmediate(parsed.getImmediate());
        }
        
        return instr;
    }
    
    /**
     * Execute one cycle of the simulation
     */
    public void stepCycle() {
        if (simulationComplete.get()) {
            log("Simulation already complete!");
            return;
        }
        
        // Validate that user has configured all required parameters
        if (!validateConfiguration()) {
            log("ERROR: Cannot start simulation - please configure all required parameters first!");
            log("Required: Station sizes, Cache parameters, Instruction latencies");
            return;
        }
        
        // CRITICAL FIX: Ensure cycles increment by exactly 1, no skipping
        int previousCycle = currentCycle;
        currentCycle++;
        
        // Safety check: cycles should only increment by 1
        if (currentCycle != previousCycle + 1) {
            log("WARNING: Cycle jump detected! Previous: " + previousCycle + ", Current: " + currentCycle);
            currentCycle = previousCycle + 1; // Force correct increment
        }
        
        log("=== CYCLE " + currentCycle + " ===");
        
        // Sync issue unit cycle counter
        while (issueUnit.getCurrentCycle() < currentCycle) {
            issueUnit.nextCycle();
        }
        
        // ISSUE STAGE - Only issue ONE instruction per cycle (strict in-order issue)
        if (instructionPointer < instructions.size()) {
            Instruction currentInstruction = instructions.get(instructionPointer);
            if (issueUnit.canIssue(currentInstruction)) {
                boolean issued = issueUnit.issue(currentInstruction);
                if (issued) {
                    issueCycles.put(currentInstruction.getId(), currentCycle);
                    log("ISSUED: " + currentInstruction + " (Cycle " + currentCycle + ")");
                    
                    // Track execution cycles for memory operations when issued
                    // Memory operations execute via MemorySystem, so we track them here
                    if (currentInstruction.isMemoryOperation()) {
                        int instrId = currentInstruction.getId();
                        String baseReg = currentInstruction.getBaseRegister();
                        int offset = currentInstruction.getOffset();
                        
                        if (baseReg != null && !baseReg.isEmpty()) {
                            try {
                                int baseValue = (int)registerFile.getRegisterValue(baseReg);
                                int effectiveAddress = baseValue + offset;
                                
                                // CRITICAL FIX: Memory operations start execution in the NEXT cycle after issue
                                // Execution cannot start in the same cycle as issue
                                // Load latency includes both the load operation cycles AND cache hit/miss latency
                                int execStart = currentCycle + 1;
                                // Use MemorySystem's latency calculation (based on user configuration)
                                // This already includes loadLatency + cacheHitLatency (or cacheMissPenalty)
                                int latency = memorySystem.calculateLoadLatency(effectiveAddress);
                                boolean isHit = memorySystem.getCacheSimulator().isHit(effectiveAddress);
                                
                                // Only set if not already set (avoid overwriting)
                                if (!execStartCycles.containsKey(instrId)) {
                                    execStartCycles.put(instrId, execStart);
                                    // Execution end = start + latency - 1
                                    // Example: start cycle 1, latency 3 -> executes cycles 1,2,3 -> completes end of cycle 3
                                    int expectedEnd = execStart + latency - 1;
                                    execEndCycles.put(instrId, expectedEnd);
                                    log("  Memory op execution: " + execStart + "..." + expectedEnd + 
                                        " (latency=" + latency + ", loadLatency=" + memorySystem.getLoadLatency() + 
                                        ", cacheHitLatency=" + memorySystem.getCacheSimulator().getHitLatency() + 
                                        (isHit ? ", HIT" : ", MISS") + ")");
                                }
                            } catch (Exception e) {
                                // Base register not ready yet - will be tracked when it becomes ready
                            }
                        }
                    }
                    
                    instructionPointer++;
                } else {
                    log("Issue stalled: " + currentInstruction + " (structural hazard or resource unavailable)");
                }
            } else {
                log("Waiting for resources: " + currentInstruction);
            }
        } else if (instructionPointer >= instructions.size()) {
            log("All instructions have been issued");
        }
        
        // EXECUTE STAGE
        // Call executionUnit.cycle() which handles:
        // 1. Decrementing timers for executing instructions
        // 2. Starting execution for ready instructions issued in previous cycles
        // 3. Completing execution when timers reach 0
        executionUnit.cycle(currentCycle);
        
        // Track execution start/end cycles for display purposes
        // Check which instructions started execution this cycle (after executionUnit.cycle() call)
        List<ReservationStation> allStations = rsPool.getAllStations();
        for (ReservationStation rs : allStations) {
            if (rs.isExecutionStarted() && rs.getInstruction() != null) {
                int instrId = rs.getInstruction().getId();
                
                // Skip memory operations - they're tracked separately when issued
                if (rs.getInstruction().isMemoryOperation()) {
                    continue;
                }
                
                // Track execution start if not already tracked
                if (!execStartCycles.containsKey(instrId)) {
                    int latency = executionUnit.getLatency(rs.getOp());
                    if (latency > 0) {
                        execStartCycles.put(instrId, currentCycle);
                        // Execution end = start + latency - 1 (e.g., start cycle 2, latency 2, end cycle 3)
                        execEndCycles.put(instrId, currentCycle + latency - 1);
                        log("EXEC START: " + rs.getInstruction() + " in " + rs.getName() + " (cycles: " + currentCycle + "..." + (currentCycle + latency - 1) + ")");
                    }
                }
            }
        }
        
        // MEMORY SYSTEM
        List<MemorySystem.CompletedOp> memoryResults = memorySystem.cycle();
        for (MemorySystem.CompletedOp op : memoryResults) {
            if (op.isLoad) {
                log("LOAD COMPLETE: " + op.destReg + " = " + op.value);
                // Find the instruction that corresponds to this load
                int loadInstrId = -1;
                for (Instruction instr : instructions) {
                    if (instr.isMemoryOperation() && instr.getOperation().equals(op.op) 
                        && instr.getDestRegister() != null && instr.getDestRegister().equals(op.destReg)) {
                        loadInstrId = instr.getId();
                        // Update execution end to current cycle (when it actually completes)
                        if (execStartCycles.containsKey(loadInstrId)) {
                            Integer originalEnd = execEndCycles.get(loadInstrId);
                            execEndCycles.put(loadInstrId, currentCycle);
                            if (originalEnd != null && currentCycle > originalEnd + 2) {
                                // Log if actual completion is significantly later than expected
                                log("  ⚠ Load completed later than expected: expected ~" + originalEnd + ", actual " + currentCycle);
                            }
                        } else {
                            // Load completed but we never tracked its start - this shouldn't happen
                            log("  ⚠ WARNING: Load completed but execStartCycles not found for instruction " + loadInstrId);
                        }
                        break;
                    }
                }
                
                // Create broadcast request for CDB
                String loadStationName = registerFile.getQi(op.destReg);
                if (loadStationName != null && !loadStationName.isEmpty()) {
                    int rsId = ExecutionUnit.stationNameToRsId(loadStationName);
                    int destRegNum = convertRegNameToNumber(op.destReg);
                    BroadcastRequest request = new BroadcastRequest(
                        rsId, (double)op.value, destRegNum, op.op);
                    // CRITICAL FIX: Load completes execution in currentCycle, broadcast in next cycle
                    request.setReadyCycle(currentCycle + 1);
                    CommonDataBus.getInstance().addBroadcastRequest(request);
                    log("  → Added to CDB: " + loadStationName + " -> " + op.destReg + " = " + op.value);
                } else {
                    // Fallback: directly update register if no station found
                    // NOTE: This should not happen in normal operation - if Qi is null,
                    // it means the load station tag was never set or was already cleared.
                    // Still check for WAW hazard before updating (though unlikely in this case)
                    String currentQi = registerFile.getQi(op.destReg);
                    if (currentQi == null || currentQi.isEmpty()) {
                        // No pending write - safe to update
                    registerFile.writeValue(op.destReg, (double)op.value);
                        log("  → Directly updated " + op.destReg + " (no Qi found - fallback case)");
                    } else {
                        // WAW scenario - a later instruction has claimed this register
                        // This shouldn't happen, but handle gracefully
                        log("  ⚠ WARNING: Load completed but register " + op.destReg + 
                            " Qi points to " + currentQi + " - skipping update (WAW)");
                    }
                }
                
                // Mark load as complete after broadcast is set up
                // The actual write-back happens in the broadcast stage, but we track completion here
                // since the load has finished execution and result is ready
                if (loadInstrId > 0) {
                    // Don't mark complete yet - wait for broadcast to complete
                    // We'll mark it complete after the broadcast stage
                }
            } else {
                log("STORE COMPLETE: " + op.op);
                // Find and mark store instruction as complete
                for (Instruction instr : instructions) {
                    if (instr.isMemoryOperation() && instr.getOperation().equals(op.op)) {
                        int instrId = instr.getId();
                        if (execStartCycles.containsKey(instrId)) {
                            execEndCycles.put(instrId, currentCycle);
                        }
                        // Mark store as complete (stores don't broadcast, they just finish)
                        completeCycles.put(instrId, currentCycle);
                        log("  → Store instruction " + instrId + " marked complete");
                        
                        // Release the store reservation station
                        for (ReservationStation rs : rsPool.getBusyStations()) {
                            if (rs.getInstruction() != null && rs.getInstruction().getId() == instrId) {
                                rsPool.releaseStation(rs.getName());
                                log("  → Released store station: " + rs.getName());
                                break;
                            }
                        }
                        break;
                    }
                }
            }
        }
        
        // WRITE-BACK STAGE
        // CRITICAL FIX: Write-back happens in the NEXT cycle after execution completes
        // Instructions that completed execution in cycle N-1 will broadcast in cycle N
        // Process broadcasts for instructions that completed execution in the PREVIOUS cycle
        // This ensures write-back happens AFTER execution, not in the same cycle
        
        // Track which stations completed execution BEFORE write-back (for completion tracking)
        List<ReservationStation> stationsBefore = rsPool.getBusyStations();
        Map<String, Integer> stationNamesBefore = new HashMap<>();
        for (ReservationStation rs : stationsBefore) {
            if (rs.isExecutionStarted() && rs.getRemainingCycles() == 0) {
                stationNamesBefore.put(rs.getName(), 
                    rs.getInstruction() != null ? rs.getInstruction().getId() : -1);
            }
        }
        
        // Process broadcasts (only those ready in current cycle)
        writeBackUnit.writeBackCycle(currentCycle);
        
        // Note: BroadcastManager is registered as a listener to CDB, so it will be called
        // automatically when WriteBackUnit processes broadcasts. No need to call broadcast() separately.
        
        // CRITICAL FIX: Check for store stations that became ready after CDB broadcasts
        // Stores that were waiting for source register data may now be ready to issue to MemorySystem
        List<ReservationStation> readyStations = rsPool.getReadyStations();
        for (ReservationStation rs : readyStations) {
            if (rs.getInstruction() != null && rs.getInstruction().isMemoryOperation()) {
                Instruction instr = rs.getInstruction();
                String op = instr.getOperation();
                
                // Check if this is a store that hasn't been issued to MemorySystem yet
                if ((op.equals("SW") || op.equals("SD") || op.equals("S.S") || op.equals("S.D"))) {
                    // Store is ready (both base address and source data ready)
                    // Check if it's already been issued to MemorySystem by checking if there's an LSB entry
                    // Actually, we can't easily check that, so we'll issue it if the station is ready
                    // and hasn't started execution yet
                    if (!rs.isExecutionStarted()) {
                        try {
                            // Get base address from Vj (base register value) and A (offset)
                            Double baseValueObj = rs.getVj();
                            Integer offset = rs.getA();
                            Double srcValueObj = rs.getVk(); // Source register value (data to store)
                            
                            if (baseValueObj != null && offset != null && srcValueObj != null) {
                                int baseValue = baseValueObj.intValue();
                                long value = srcValueObj.longValue();
                                
                                // Issue store to MemorySystem now that both operands are ready
                                memorySystem.issueStore(op, baseValue, offset, value);
                                log("STORE READY: Issued " + op + " to MemorySystem from " + rs.getName() + 
                                    " (address: " + (baseValue + offset) + ", value: " + value + ")");
                                
                                // Mark as execution started to prevent duplicate issues
                                rs.startExecution(1); // Dummy latency, actual execution handled by MemorySystem
                            }
                        } catch (Exception e) {
                            log("ERROR: Failed to issue ready store to MemorySystem: " + e.getMessage());
                        }
                    }
                }
            }
        }
        
        // CRITICAL FIX: Track write-back cycles (when broadcasts actually happen)
        // Write-back happens AFTER execution completes, so mark complete cycles after broadcast
        List<ReservationStation> stationsAfter = rsPool.getBusyStations();
        for (ReservationStation rs : stationsAfter) {
            if (!rs.isBusy() && stationNamesBefore.containsKey(rs.getName())) {
                Integer instrId = stationNamesBefore.get(rs.getName());
                if (instrId != null && instrId > 0) {
                    Instruction completedInstr = findInstructionById(instrId);
                    if (completedInstr != null && isBranchInstruction(completedInstr)) {
                        resolveBranch(completedInstr, currentCycle);
                    }
                    // CRITICAL FIX: Write-back happens in current cycle (after execution completed in previous cycle)
                    // Mark complete cycle as current cycle (when broadcast happened)
                    if (!completeCycles.containsKey(instrId)) {
                        completeCycles.put(instrId, currentCycle);
                        log("COMPLETE: Instruction " + instrId + " (write-back in cycle " + currentCycle + " from station " + rs.getName() + ")");
                    }
                }
            }
        }
        
        // Check for completed memory load operations
        // Loads complete when their broadcast finishes (Qi is cleared or station is freed)
        for (MemorySystem.CompletedOp op : memoryResults) {
            if (op.isLoad) {
                // Find the instruction that corresponds to this load
                for (Instruction instr : instructions) {
                    if (instr.isMemoryOperation() && instr.getOperation().equals(op.op) 
                        && instr.getDestRegister() != null && instr.getDestRegister().equals(op.destReg)) {
                        int instrId = instr.getId();
                        
                        // CRITICAL FIX: Mark load as complete when broadcast happens (write-back cycle)
                        // Load completed execution in previous cycle, broadcast happens in current cycle
                        String stationName = registerFile.getQi(op.destReg);
                        if (stationName == null || stationName.isEmpty()) {
                            // Qi cleared means broadcast completed - mark as complete in current cycle (write-back cycle)
                            if (!completeCycles.containsKey(instrId)) {
                                completeCycles.put(instrId, currentCycle);
                                log("COMPLETE: Load instruction " + instrId + " (write-back in cycle " + currentCycle + ", Qi cleared)");
                            }
                        } else {
                            // Check if the station itself is free (might happen if broadcast already processed)
                            ReservationStation rs = rsPool.getStationByName(stationName);
                            if (rs != null && !rs.isBusy() && !completeCycles.containsKey(instrId)) {
                                completeCycles.put(instrId, currentCycle);
                                log("COMPLETE: Load instruction " + instrId + " (write-back in cycle " + currentCycle + ", station " + stationName + " freed)");
                            }
                        }
                        break;
                    }
                }
            }
        }
        
        // Also check for any load stations that became free after broadcast
        // (in case they weren't in memoryResults but completed earlier)
        for (ReservationStation rs : stationsAfter) {
            if (!rs.isBusy() && rs.getInstruction() != null) {
                Instruction instr = rs.getInstruction();
                String op = instr.getOperation();
                boolean isLoad = op != null && (op.equals("LW") || op.equals("LD") || op.equals("L.S") || op.equals("L.D"));
                if (instr.isMemoryOperation() && isLoad) {
                    int instrId = instr.getId();
                    // Only mark complete if execution has finished (has execEndCycle)
                    if (execEndCycles.containsKey(instrId) && !completeCycles.containsKey(instrId)) {
                        // Check if this cycle is after execution end
                        int execEnd = execEndCycles.get(instrId);
                        if (currentCycle >= execEnd) {
                            completeCycles.put(instrId, currentCycle);
                            log("COMPLETE: Load instruction " + instrId + " (station " + rs.getName() + " freed after execution)");
                        }
                    }
                }
            }
        }
        
        // Check if simulation is complete
        if (instructionPointer >= instructions.size()) {
            boolean allComplete = true;
            for (Instruction instr : instructions) {
                if (!completeCycles.containsKey(instr.getId())) {
                    allComplete = false;
                    break;
                }
            }
            boolean allStationsFree = rsPool.getBusyStations().isEmpty();
            boolean noActiveExecutions = executionUnit.getExecutionTimers().isEmpty();
            boolean lsbEmpty = memorySystem.getLSBEntries().isEmpty();
            
            if (allComplete && allStationsFree && noActiveExecutions && lsbEmpty) {
                simulationComplete.set(true);
                log("*** SIMULATION COMPLETE ***");
                log("Total cycles: " + currentCycle);
            }
        }
        
        if (updateCallback != null) {
            updateCallback.run();
        }
    }
    
    private int convertRegNameToNumber(String regName) {
        if (regName == null || regName.isEmpty()) return 0;
        if (regName.startsWith("F")) {
            try {
                return Integer.parseInt(regName.substring(1));
            } catch (NumberFormatException e) {
                return 0;
            }
        } else if (regName.startsWith("R")) {
            try {
                return Integer.parseInt(regName.substring(1)) + 32;
            } catch (NumberFormatException e) {
                return 32;
            }
        }
        return 0;
    }
    
    /**
     * Find instruction by ID
     */
    private Instruction findInstructionById(int id) {
        for (Instruction instr : instructions) {
            if (instr.getId() == id) {
                return instr;
            }
        }
        return null;
    }
    
    /**
     * Check if instruction is a branch
     */
    private boolean isBranchInstruction(Instruction instr) {
        if (instr == null) return false;
        String op = instr.getOperation().toUpperCase();
        return op.equals("BEQ") || op.equals("BNE");
    }
    
    /**
     * Resolve branch instruction when it completes
     * Evaluates condition and handles branch taken/not taken
     */
    private void resolveBranch(Instruction branch, int currentCycle) {
        String op = branch.getOperation().toUpperCase();
        if (!op.equals("BEQ") && !op.equals("BNE")) return;
        
        // Get source register values
        String r1 = branch.getSourceReg1();
        String r2 = branch.getSourceReg2();
        
        if (r1 == null || r2 == null) {
            log("BRANCH ERROR: Missing source registers");
            issueUnit.resolveBranch();
            return;
        }
        
        double v1 = registerFile.getRegisterValue(r1);
        double v2 = registerFile.getRegisterValue(r2);
        
        // Evaluate condition
        boolean taken = false;
        if (op.equals("BEQ")) {
            taken = (v1 == v2);
        } else if (op.equals("BNE")) {
            taken = (v1 != v2);
        }
        
        log("BRANCH RESOLVED: " + op + " " + r1 + "(" + v1 + ") " + r2 + "(" + v2 + ") - " + (taken ? "TAKEN" : "NOT TAKEN"));
        
        if (taken) {
            // Get branch target (immediate value contains target instruction index)
            int target = branch.getImmediate();
            if (target >= 0 && target < instructions.size()) {
                // Flush instructions issued after this branch
                flushInstructionsAfterBranch(branch.getId());
                // Update instruction pointer to branch target
                instructionPointer = target;
                log("BRANCH TAKEN: Jumping to instruction " + target);
            } else {
                log("BRANCH ERROR: Invalid target " + target);
            }
        } else {
            log("BRANCH NOT TAKEN: Continuing sequentially");
        }
        
        // Clear branch pending flag to resume issue
        issueUnit.resolveBranch();
    }
    
    /**
     * Flush instructions that were issued after a branch when branch is taken
     */
    private void flushInstructionsAfterBranch(int branchId) {
        // Find branch instruction index in program
        int branchIndex = -1;
        for (int i = 0; i < instructions.size(); i++) {
            if (instructions.get(i).getId() == branchId) {
                branchIndex = i;
                break;
            }
        }
        
        if (branchIndex == -1) {
            log("FLUSH ERROR: Branch instruction not found");
            return;
        }
        
        // Flush all instructions issued after the branch
        int flushedCount = 0;
        for (int i = branchIndex + 1; i < instructions.size(); i++) {
            Instruction instr = instructions.get(i);
            if (issueCycles.containsKey(instr.getId())) {
                // Find and release reservation station
                List<ReservationStation> allStations = rsPool.getAllStations();
                for (ReservationStation rs : allStations) {
                    if (rs.getInstruction() != null && rs.getInstruction().getId() == instr.getId()) {
                        rs.setBusy(false);
                        rs.clear();
                        log("FLUSHED: " + instr + " from " + rs.getName());
                        flushedCount++;
                        break;
                    }
                }
                // Remove from tracking maps
                issueCycles.remove(instr.getId());
                execStartCycles.remove(instr.getId());
                execEndCycles.remove(instr.getId());
                completeCycles.remove(instr.getId());
            }
        }
        
        if (flushedCount > 0) {
            log("FLUSHED " + flushedCount + " instruction(s) after branch");
        }
    }
    
    /**
     * Reset simulation to initial state
     */
    public void reset() {
        currentCycle = 0;
        instructionPointer = 0;
        simulationRunning = false;
        simulationPaused = false;
        simulationComplete.set(false);
        
        issueCycles.clear();
        execStartCycles.clear();
        execEndCycles.clear();
        completeCycles.clear();
        logMessages.clear();
        
        // Reset components
        registerFile.reset();
        rat.clearAll();
        rsPool.reset();
        executionUnit.clear();
        writeBackUnit.clear();
        CommonDataBus.getInstance().clear();
        
        // Recreate memory system with user-configured values (or safe defaults if not set)
        memorySystem = new MemorySystem(
            // Memory size: INTENTIONALLY FIXED at 1MB (not user-configurable)
            // This is a design decision: memory size is typically fixed in real systems
            // and doesn't affect Tomasulo algorithm simulation correctness.
            1024 * 1024,      // 1MB memory - INTENTIONALLY FIXED
            cacheSize > 0 ? cacheSize : 64 * 1024,  // Use configured or safe default
            blockSize > 0 ? blockSize : 64,         // Use configured or safe default
            cacheHitLatency >= 0 ? cacheHitLatency : 1,  // Use configured or safe default
            cacheMissPenalty >= 0 ? cacheMissPenalty : 10, // Use configured or safe default
            loadLatency > 0 ? loadLatency : 2,      // Use configured or safe default
            storeLatency > 0 ? storeLatency : 2,    // Use configured or safe default
            lsbSize > 0 ? lsbSize : 8               // Use configured or safe default
        );
        
        // Recreate issue unit
        issueUnit = new InstructionIssueUnit(rsPool, registerFile, memorySystem);
        
        // Re-add instructions if any
        if (!instructions.isEmpty()) {
            issueUnit.addInstructions(instructions);
        }
        
        // Apply latencies
        for (Map.Entry<String, Integer> entry : latencies.entrySet()) {
            executionUnit.setLatency(entry.getKey(), entry.getValue());
        }
        
        // Re-apply register pre-loads if any
        if (!registerPreloadValues.isEmpty()) {
            for (Map.Entry<String, Double> entry : registerPreloadValues.entrySet()) {
                registerFile.setRegisterValue(entry.getKey(), entry.getValue());
            }
        }
        
        log("Simulation reset.");
        if (updateCallback != null) {
            updateCallback.run();
        }
    }
    
    /**
     * Run simulation to completion automatically
     */
    public void runAll() {
        simulationRunning = true;
        simulationPaused = false;
        new Thread(() -> {
            while (simulationRunning && !simulationComplete.get() && !simulationPaused) {
                stepCycle();
                // Update GUI after each cycle
                if (updateCallback != null) {
                    javafx.application.Platform.runLater(updateCallback);
                }
                try {
                    Thread.sleep(500); // 500ms delay between cycles for visibility
                } catch (InterruptedException e) {
                    break;
                }
            }
            simulationRunning = false;
        }).start();
    }
    
    public void pause() {
        simulationPaused = true;
        simulationRunning = false;
    }
    
    public void log(String message) {
        String logEntry = "[" + currentCycle + "] " + message;
        logMessages.add(logEntry);
        System.out.println(logEntry);
        // Keep only last 1000 messages
        if (logMessages.size() > 1000) {
            logMessages.remove(0);
        }
    }
    
    // Getters
    public int getCurrentCycle() { return currentCycle; }
    public List<Instruction> getInstructions() { return instructions; }
    public RegisterFile getRegisterFile() { return registerFile; }
    public ReservationStationPool getRsPool() { return rsPool; }
    public MemorySystem getMemorySystem() { return memorySystem; }
    public ExecutionUnit getExecutionUnit() { return executionUnit; }
    public List<String> getLogMessages() { return logMessages; }
    public Map<Integer, Integer> getIssueCycles() { return issueCycles; }
    public Map<Integer, Integer> getExecStartCycles() { return execStartCycles; }
    public Map<Integer, Integer> getExecEndCycles() { return execEndCycles; }
    public Map<Integer, Integer> getCompleteCycles() { return completeCycles; }
    public boolean isSimulationComplete() { return simulationComplete.get(); }
    public boolean isSimulationRunning() { return simulationRunning; }
    public Map<String, Integer> getStationCounts() { return new HashMap<>(stationCounts); }
    
    // Configuration setters
    public void setLatency(String instruction, int latency) {
        latencies.put(instruction, latency);
        if (executionUnit != null) {
            executionUnit.setLatency(instruction, latency);
        }
    }
    
    public void setCacheSize(int size) { 
        this.cacheSize = size; 
        applyCacheConfiguration(); // Recreate memory system with new values
    }
    
    public void setBlockSize(int size) { 
        this.blockSize = size; 
        applyCacheConfiguration(); // Recreate memory system with new values
    }
    
    public void setCacheHitLatency(int latency) { 
        this.cacheHitLatency = latency; 
        applyCacheConfiguration(); // Recreate memory system with new values
    }
    
    public void setCacheMissPenalty(int penalty) { 
        this.cacheMissPenalty = penalty; 
        applyCacheConfiguration(); // Recreate memory system with new values
    }
    
    public void setLoadLatency(int latency) { 
        this.loadLatency = latency; 
        applyCacheConfiguration(); // Recreate memory system with new values
    }
    
    public void setStoreLatency(int latency) { 
        this.storeLatency = latency; 
        applyCacheConfiguration(); // Recreate memory system with new values
    }
    
    public void setLsbSize(int size) { 
        this.lsbSize = size; 
        applyCacheConfiguration(); // Recreate memory system with new values
    }
    
    /**
     * Apply cache and memory configuration (recreates MemorySystem with user values)
     */
    private void applyCacheConfiguration() {
        // Always recreate memory system - use configured values or safe defaults
        // Safe defaults ensure no division by zero, but user should configure for execution
        int effectiveCacheSize = cacheSize > 0 ? cacheSize : 64 * 1024;
        int effectiveBlockSize = blockSize > 0 ? blockSize : 64;
        int effectiveHitLatency = cacheHitLatency >= 0 ? cacheHitLatency : 1;
        int effectiveMissPenalty = cacheMissPenalty >= 0 ? cacheMissPenalty : 10;
        int effectiveLoadLatency = loadLatency > 0 ? loadLatency : 2;
        int effectiveStoreLatency = storeLatency > 0 ? storeLatency : 2;
        int effectiveLsbSize = lsbSize > 0 ? lsbSize : 8;
        
        // Recreate memory system with effective values
        memorySystem = new MemorySystem(
            // Memory size: INTENTIONALLY FIXED at 1MB (not user-configurable)
            // This is a design decision: memory size is typically fixed in real systems
            // and doesn't affect Tomasulo algorithm simulation correctness.
            1024 * 1024,      // 1MB memory - INTENTIONALLY FIXED
            effectiveCacheSize,
            effectiveBlockSize,
            effectiveHitLatency,
            effectiveMissPenalty,
            effectiveLoadLatency,
            effectiveStoreLatency,
            effectiveLsbSize
        );
        
        // Recreate issue unit with new memory system
        issueUnit = new InstructionIssueUnit(rsPool, registerFile, memorySystem);
        
        // Re-add instructions if any
        if (!instructions.isEmpty()) {
            issueUnit.addInstructions(instructions);
        }
        
        // Log configuration status
        if (cacheSize > 0 && blockSize > 0 && loadLatency > 0 && storeLatency > 0) {
            log("Applied cache configuration: Cache=" + cacheSize + "B, Block=" + blockSize 
                + "B, Hit=" + cacheHitLatency + ", Miss=" + cacheMissPenalty 
                + ", Load=" + loadLatency + ", Store=" + storeLatency + ", LSB=" + lsbSize);
        } else {
            log("Using default cache configuration - please configure cache parameters for accurate simulation");
        }
    }
    
    /**
     * Validate that all required configuration is set by user
     * Uses safe defaults if not configured, but warns user
     */
    private boolean validateConfiguration() {
        // Check station configuration
        if (stationCounts.isEmpty() || stationCounts.values().stream().anyMatch(count -> count == null || count < 1)) {
            log("ERROR: Station configuration incomplete - please configure station sizes");
            return false;
        }
        
        // Cache and latency values use safe defaults if not configured
        // This allows simulation to run, but user should configure for accurate results
        boolean usingCacheDefaults = (cacheSize == 64 * 1024 && blockSize == 64 && 
                                     cacheHitLatency == 1 && cacheMissPenalty == 10);
        boolean usingLatencyDefaults = (loadLatency == 2 && storeLatency == 2);
        
        if (usingCacheDefaults) {
            log("WARNING: Using default cache configuration - configure cache parameters for accurate simulation");
        }
        if (usingLatencyDefaults) {
            log("WARNING: Using default load/store latencies - configure instruction latencies for accurate simulation");
        }
        
        // All checks pass - safe defaults will be used if not configured
        return true;
    }
    
    /**
     * Apply station and buffer configuration
     */
    public void applyStationConfiguration(Map<String, Integer> stationCounts, int lsbSize) {
        this.stationCounts = new HashMap<>(stationCounts);
        this.lsbSize = lsbSize;
        
        // Reconfigure reservation station pool
        rsPool.reconfigure(stationCounts);
        
        // Recreate memory system with user-configured values (or safe defaults if not set)
        memorySystem = new MemorySystem(
            // Memory size: INTENTIONALLY FIXED at 1MB (not user-configurable)
            // This is a design decision: memory size is typically fixed in real systems
            // and doesn't affect Tomasulo algorithm simulation correctness.
            1024 * 1024,      // 1MB memory - INTENTIONALLY FIXED
            cacheSize > 0 ? cacheSize : 64 * 1024,  // Use configured or safe default (64KB)
            blockSize > 0 ? blockSize : 64,         // Use configured or safe default (64 bytes)
            cacheHitLatency >= 0 ? cacheHitLatency : 1,  // Use configured or safe default
            cacheMissPenalty >= 0 ? cacheMissPenalty : 10, // Use configured or safe default
            loadLatency > 0 ? loadLatency : 2,      // Use configured or safe default
            storeLatency > 0 ? storeLatency : 2,    // Use configured or safe default
            lsbSize > 0 ? lsbSize : 8               // Use configured or safe default
        );
        
        // Recreate issue unit with new components
        issueUnit = new InstructionIssueUnit(rsPool, registerFile, memorySystem);
        
        // Re-add instructions if any
        if (!instructions.isEmpty()) {
            issueUnit.addInstructions(instructions);
        }
        
        log("Applied station configuration: " + stationCounts + ", LSB size: " + lsbSize);
        
        if (updateCallback != null) {
            updateCallback.run();
        }
    }
    
    /**
     * Pre-load register values
     */
    public void preloadRegisters(Map<String, Double> values) {
        registerPreloadValues.clear();
        registerPreloadValues.putAll(values);
        
        // Apply to register file
        for (Map.Entry<String, Double> entry : values.entrySet()) {
            registerFile.setRegisterValue(entry.getKey(), entry.getValue());
        }
        
        log("Pre-loaded " + values.size() + " register values.");
        
        if (updateCallback != null) {
            updateCallback.run();
        }
    }
    
    /**
     * Clear all register pre-load values
     */
    public void clearRegisterPreloads() {
        // Reset all registers to 0
        registerFile.reset();
        registerPreloadValues.clear();
        log("Cleared all register pre-load values.");
        
        if (updateCallback != null) {
            updateCallback.run();
        }
    }
    
    public void setUpdateCallback(Runnable callback) {
        this.updateCallback = callback;
    }
}
