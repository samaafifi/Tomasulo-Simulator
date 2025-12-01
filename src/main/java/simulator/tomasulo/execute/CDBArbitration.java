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
        
        // Priority-based arbitration - uses getInstrType()
        pendingRequests.sort((a, b) -> {
            return getPriority(b.getInstrType()) - getPriority(a.getInstrType());
        });
        
        // Select first (highest priority)
        if (!pendingRequests.isEmpty()) {
            selected.add(pendingRequests.remove(0));
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