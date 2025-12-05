package simulator.tomasulo.execute;

import java.util.*;

public class CDBArbitration {
    private List<BroadcastRequest> pendingRequests = new ArrayList<>();
    
    public void addRequest(BroadcastRequest request) {
        pendingRequests.add(request);
    }
    
    public List<BroadcastRequest> selectBroadcasts(int currentCycle) {
        List<BroadcastRequest> selected = new ArrayList<>();
        
        if (pendingRequests.isEmpty()) {
            return selected;
        }
        
        // CRITICAL FIX: Only process requests that are ready in the current cycle
        // This ensures write-back happens AFTER execution completes, not in the same cycle
        List<BroadcastRequest> readyRequests = new ArrayList<>();
        Iterator<BroadcastRequest> it = pendingRequests.iterator();
        while (it.hasNext()) {
            BroadcastRequest req = it.next();
            // Process if readyCycle is 0 (immediate/legacy) or readyCycle <= currentCycle
            if (req.getReadyCycle() == 0 || req.getReadyCycle() <= currentCycle) {
                readyRequests.add(req);
                it.remove();
            }
        }
        
        if (readyRequests.isEmpty()) {
            return selected;
        }
        
        // Priority-based arbitration - uses getInstrType()
        readyRequests.sort((a, b) -> {
            return getPriority(b.getInstrType()) - getPriority(a.getInstrType());
        });
        
        // Select first (highest priority)
        if (!readyRequests.isEmpty()) {
            selected.add(readyRequests.remove(0));
            // Put remaining ready requests back for next cycle
            pendingRequests.addAll(readyRequests);
        }
        
        return selected;
    }
    
    private int getPriority(String instructionType) {
        if (instructionType == null) return 0;
        
        if (instructionType.contains("DADD") || instructionType.contains("DSUB") || 
            "BEQ".equals(instructionType) || "BNE".equals(instructionType)) {
            return 4;
        }
        if (instructionType.contains("ADD") || instructionType.contains("SUB")) {
            return 3;
        }
        if (instructionType.contains("MUL") || instructionType.contains("DIV")) {
            return 2;
        }
        if (instructionType.contains("L.") || instructionType.contains("S.") || 
            "LW".equals(instructionType) || "SW".equals(instructionType) ||
            "LD".equals(instructionType) || "SD".equals(instructionType)) {
            return 1;
        }
        return 0;
    }
    
    public void clear() {
        pendingRequests.clear();
    }
    
    public String getArbitrationStrategy() {
        return "Priority: Integer(4) > FP Add/Sub(3) > FP Mul/Div(2) > Load/Store(1)";
    }
}