package simulator.tomasulo.execute;

import java.util.*;

public class ExecutionUnit {
    // Store by station NAME (Add1, Add2, etc.) not RS ID
    private Map<String, Integer> timers = new HashMap<>(); // Station Name -> cycles left
    private Map<String, String> instrTypes = new HashMap<>(); // Station Name -> instruction type
    private Map<String, Integer> destRegs = new HashMap<>(); // Station Name -> dest register
    
    public ExecutionUnit() {
        // Initialize empty
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
        List<String> completed = new ArrayList<>();
        
        // Decrement timers
        for (Map.Entry<String, Integer> entry : timers.entrySet()) {
            String stationName = entry.getKey();
            int cyclesLeft = entry.getValue() - 1;
            
            if (cyclesLeft <= 0) {
                completed.add(stationName);
                completeExecution(stationName, currentCycle);
            } else {
                timers.put(stationName, cyclesLeft);
            }
        }
        
        // Remove completed
        for (String stationName : completed) {
            timers.remove(stationName);
            instrTypes.remove(stationName);
            destRegs.remove(stationName);
        }
    }
    
    private void completeExecution(String stationName, int currentCycle) {
        String instrType = instrTypes.get(stationName);
        int destReg = destRegs.get(stationName);
        
        // Get RS ID from station name for BroadcastRequest
        int rsId = stationNameToRsId(stationName);
        
        // Simulate result
        double result = simulateResult(stationName, instrType);
        
        // Create broadcast request
        BroadcastRequest request = new BroadcastRequest(
            rsId,          // Still use RS ID for BroadcastRequest
            result,
            destReg,
            instrType
        );
        
        // Add to CDB
        CommonDataBus.getInstance().addBroadcastRequest(request);
        
        System.out.println(String.format(
            "[EXECUTE] Cycle %d: Completed %s in %s -> result = %.2f",
            currentCycle, instrType, stationName, result
        ));
    }
    
    private double simulateResult(String stationName, String instrType) {
        // Base on station name hash
        int base = Math.abs(stationName.hashCode() % 100);
        Random rand = new Random(stationName.hashCode());
        
        switch (instrType) {
            case "ADD.D": case "ADD.S":
                return base + 10.5 + rand.nextDouble();
            case "SUB.D": case "SUB.S":
                return base + 20.5 + rand.nextDouble();
            case "MUL.D": case "MUL.S":
                return base + 30.5 + rand.nextDouble();
            case "DIV.D": case "DIV.S":
                return base + 40.5 + rand.nextDouble();
            case "DADDI": case "DSUBI":
                return base + 50.5 + rand.nextDouble();
            default:
                return base + rand.nextDouble();
        }
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
    }
}