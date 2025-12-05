package simulator.tomasulo.execute;

import simulator.tomasulo.issue.*;
import java.util.*;

public class ExecutionUnit {
    // Store by station NAME (Add1, Add2, etc.) not RS ID
    private Map<String, Integer> timers = new HashMap<>(); // Station Name -> cycles left
    private Map<String, String> instrTypes = new HashMap<>(); // Station Name -> instruction type
    private Map<String, Integer> destRegs = new HashMap<>(); // Station Name -> dest register
    private Map<String, Integer> latencies = new HashMap<>(); // Instruction type -> latency
    private ReservationStationPool rsPool; // Reference to reservation station pool
    private int currentCycle = 0;
    
    public ExecutionUnit() {
        // NO DEFAULT LATENCIES - user must configure via GUI
        // Latencies will be set by user input only
    }
    
    /**
     * Set the reservation station pool (needed to check ready stations)
     */
    public void setReservationStationPool(ReservationStationPool rsPool) {
        this.rsPool = rsPool;
    }
    
    /**
     * Set latency for an instruction type
     */
    public void setLatency(String instructionType, int latency) {
        if (instructionType == null || instructionType.isEmpty()) {
            System.err.println("WARNING: Attempted to set latency for null/empty instruction type");
            return;
        }
        String key = instructionType.toUpperCase();
        latencies.put(key, latency);
        System.out.println("[ExecutionUnit] Set latency for " + key + " = " + latency + " cycles");
    }
    
    /**
     * Get latency for an instruction type
     * Returns 0 if not configured (user must set latency before execution)
     */
    public int getLatency(String instructionType) {
        if (instructionType == null || instructionType.isEmpty()) {
            return 0;
        }
        String key = instructionType.toUpperCase();
        Integer latency = latencies.get(key);
        if (latency == null) {
            // Don't print warning here - let the caller decide when to warn
            // This method is called frequently and warnings should be contextual
            return 0; // Return 0 to indicate not configured
        }
        return latency;
    }
    
    /**
     * Get all configured latencies (for debugging)
     */
    public Map<String, Integer> getAllLatencies() {
        return new HashMap<>(latencies);
    }
    
    /**
     * Check if a latency is configured for an instruction type
     */
    public boolean hasLatency(String instructionType) {
        return latencies.containsKey(instructionType.toUpperCase());
    }
    
    /**
     * Execute one cycle (without cycle parameter - uses internal counter)
     */
    public void executeCycle() {
        currentCycle++;
        cycle(currentCycle);
    }
    
    // Start execution by station NAME (recommended)
    public void startExecution(String stationName, String instrType, 
                             int destReg, int latency, int currentCycle) {
        timers.put(stationName, latency);
        instrTypes.put(stationName, instrType);
        destRegs.put(stationName, destReg);
        
        System.out.println(String.format(
            "[EXECUTE] Cycle %d: Started %s in %s -> R%d (latency: %d)",
            currentCycle, instrType, stationName, destReg, latency
        ));
    }
    
    // Alternative: Start by RS ID (convert to station name)
    public void startExecution(int rsId, String instrType, 
                             int destReg, int latency, int currentCycle) {
        String stationName = rsIdToStationName(rsId);
        startExecution(stationName, instrType, destReg, latency, currentCycle);
    }
    
    public void cycle(int currentCycle) {
        this.currentCycle = currentCycle;
        
        // CRITICAL: Decrement timers FIRST for stations already executing
        // This ensures that if an instruction starts execution in cycle N with latency L,
        // it will complete in cycle N+L-1 (after L cycles of execution)
        List<String> completed = new ArrayList<>();
        
        // Decrement timers for stations already executing
        for (Map.Entry<String, Integer> entry : timers.entrySet()) {
            String stationName = entry.getKey();
            int cyclesLeft = entry.getValue() - 1;
            
            // Synchronize with ReservationStation's remainingCycles
            if (rsPool != null) {
                ReservationStation rs = rsPool.getStationByName(stationName);
                if (rs != null) {
                    rs.setRemainingCycles(Math.max(0, cyclesLeft));
                }
            }
            
            if (cyclesLeft <= 0) {
                completed.add(stationName);
                completeExecution(stationName, currentCycle);
            } else {
                timers.put(stationName, cyclesLeft);
            }
        }
        
        // Remove completed stations from tracking
        for (String stationName : completed) {
            timers.remove(stationName);
            instrTypes.remove(stationName);
            destRegs.remove(stationName);
        }
        
        // NOW check for ready stations that were issued in a PREVIOUS cycle
        // and start their execution (ensures issue and execution start in different cycles)
        // This must happen AFTER timer decrement to prevent same-cycle completion
        if (rsPool != null) {
            List<ReservationStation> readyStations = rsPool.getReadyStations();
            for (ReservationStation rs : readyStations) {
                // Only start execution if:
                // 1. Execution hasn't started yet
                // 2. Station was issued in a STRICTLY PREVIOUS cycle (issueCycle < currentCycle)
                // 3. NOT a memory operation (load/store execute via MemorySystem, not ExecutionUnit)
                // 4. Station is not already in timers (avoid duplicate starts)
                if (!rs.isExecutionStarted() && 
                    rs.getIssueCycle() >= 0 && 
                    rs.getIssueCycle() < currentCycle &&
                    !timers.containsKey(rs.getName())) {
                    
                    String instrType = rs.getOp();
                    
                    // Skip load/store instructions - they execute via MemorySystem
                    if (isMemoryOperation(instrType)) {
                        continue;
                    }
                    
                    int latency = getLatency(instrType);
                    if (latency <= 0) {
                        // Skip if latency not configured
                        // Log warning only once per instruction to avoid spam
                        // (This check happens every cycle for ready instructions)
                        if (!rs.isExecutionStarted()) {
                            System.err.println("WARNING: Latency not configured for " + instrType + 
                                " in " + rs.getName() + " - user must set latency!");
                            System.err.println("  Available latencies: " + latencies.keySet());
                        }
                        continue;
                    }
                    
                    // Get destination register from instruction
                    int destReg = 0;
                    if (rs.getInstruction() != null) {
                        String destRegStr = rs.getInstruction().getDestRegister();
                        if (destRegStr != null && destRegStr.startsWith("F")) {
                            destReg = Integer.parseInt(destRegStr.substring(1));
                        } else if (destRegStr != null && destRegStr.startsWith("R")) {
                            destReg = Integer.parseInt(destRegStr.substring(1)) + 32;
                        }
                    }
                    
                    // Start execution: timer is set to latency, will count down in future cycles
                    startExecution(rs.getName(), instrType, destReg, latency, currentCycle);
                    rs.startExecution(latency);
                }
            }
        }
    }
    
    private void completeExecution(String stationName, int currentCycle) {
        String instrType = instrTypes.get(stationName);
        int destReg = destRegs.get(stationName);
        
        // Get RS ID from station name for BroadcastRequest
        int rsId = stationNameToRsId(stationName);
        
        // Simulate result
        double result = simulateResult(stationName, instrType);
        
        // CRITICAL FIX: Create broadcast request with completion cycle
        // The broadcast will happen in the NEXT cycle (currentCycle + 1)
        // This ensures write-back happens AFTER execution completes, not in the same cycle
        BroadcastRequest request = new BroadcastRequest(
            rsId,          // Still use RS ID for BroadcastRequest
            result,
            destReg,
            instrType
        );
        // Mark this request as ready for broadcast in the next cycle
        request.setReadyCycle(currentCycle + 1);
        
        // Add to CDB (will be processed in next cycle)
        CommonDataBus.getInstance().addBroadcastRequest(request);
        
        System.out.println(String.format(
            "[EXECUTE] Cycle %d: Completed %s in %s -> result = %.2f (will broadcast in cycle %d)",
            currentCycle, instrType, stationName, result, currentCycle + 1
        ));
    }
    
    private double simulateResult(String stationName, String instrType) {
        // Get actual operand values from reservation station
        ReservationStation rs = rsPool.getStationByName(stationName);
        if (rs == null || rs.getVj() == null) {
            System.err.println("Warning: Cannot get operands for " + stationName + ", using 0.0");
            return 0.0;
        }
        
        double vj = rs.getVj();
        double vk = (rs.getVk() != null) ? rs.getVk() : 0.0;
        Integer immediate = rs.getA();
        
        // Perform actual arithmetic operations
        return switch (instrType) {
            case "ADD.D", "ADD.S" -> vj + vk;
            case "SUB.D", "SUB.S" -> vj - vk;
            case "MUL.D", "MUL.S" -> vj * vk;
            case "DIV.D", "DIV.S" -> {
                if (vk == 0.0) {
                    System.err.println("Warning: Division by zero in " + stationName);
                    yield 0.0;
                }
                yield vj / vk;
            }
            case "DADDI" -> vj + (immediate != null ? immediate : 0);
            case "DSUBI" -> vj - (immediate != null ? immediate : 0);
            default -> {
                System.err.println("Warning: Unknown instruction type " + instrType + " in " + stationName);
                yield vj;
            }
        };
    }
    
    // Helper: Convert RS ID to station name (your friends' convention)
    public static String rsIdToStationName(int rsId) {
        String[] stationNames = {
            "Add1", "Add2", "Add3",      // FP Add stations 1-3
            "Mult1", "Mult2",           // FP Mult stations 4-5  
            "Div1", "Div2",             // FP Div stations 6-7
            "Load1", "Load2",           // Load stations 8-9
            "Store1", "Store2",         // Store stations 10-11
            "IntAdd1", "IntAdd2",       // Integer stations 12-13
            "Branch1"                   // Branch station 14
        };
        
        if (rsId >= 1 && rsId <= stationNames.length) {
            return stationNames[rsId - 1];
        }
        return "RS" + rsId; // Fallback
    }
    
    // Helper: Convert station name to RS ID
    public static int stationNameToRsId(String stationName) {
        Map<String, Integer> nameToId = new HashMap<>();
        String[] stationNames = {
            "Add1", "Add2", "Add3", "Mult1", "Mult2", 
            "Div1", "Div2", "Load1", "Load2", 
            "Store1", "Store2", "IntAdd1", "IntAdd2", "Branch1"
        };
        
        for (int i = 0; i < stationNames.length; i++) {
            nameToId.put(stationNames[i], i + 1);
        }
        
        return nameToId.getOrDefault(stationName, 0);
    }
    
    public Map<String, Integer> getExecutionTimers() {
        return new HashMap<>(timers);
    }
    
    public void clear() {
        timers.clear();
        instrTypes.clear();
        destRegs.clear();
        currentCycle = 0;
        // NOTE: Do NOT clear latencies - they are user-configured and should persist
        // latencies.clear(); // <-- DO NOT UNCOMMENT - latencies must persist across resets
    }
    
    public int getCurrentCycle() {
        return currentCycle;
    }
    
    /**
     * Check if instruction is a memory operation (load/store)
     * Memory operations execute via MemorySystem, not ExecutionUnit
     */
    private boolean isMemoryOperation(String op) {
        if (op == null) return false;
        String upperOp = op.toUpperCase();
        return upperOp.equals("LW") || upperOp.equals("LD") || 
               upperOp.equals("L.S") || upperOp.equals("L.D") ||
               upperOp.equals("SW") || upperOp.equals("SD") || 
               upperOp.equals("S.S") || upperOp.equals("S.D");
    }
}