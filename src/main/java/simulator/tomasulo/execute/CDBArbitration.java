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
        
        // CRITICAL FIX: First-Come-First-Serve (FCFS) arbitration
        // Select the first request that was added (maintain insertion order)
        // Only ONE instruction can broadcast per cycle
        if (readyRequests.size() > 1) {
            // Log CDB conflict when multiple instructions want to broadcast
            System.out.println(String.format(
                "[CDB ARBITRATION] Cycle %d: CDB Conflict - %d instructions want to broadcast (using FCFS)",
                currentCycle, readyRequests.size()
            ));
            for (int i = 0; i < readyRequests.size(); i++) {
                BroadcastRequest req = readyRequests.get(i);
                System.out.println(String.format(
                    "  [%d] RS%d -> R%d = %.2f (%s) - readyCycle=%d",
                    i, req.getRsId(), req.getDestReg(), req.getResult(), 
                    req.getInstrType(), req.getReadyCycle()
                ));
            }
        }
        
        // Select first request (FCFS - first come, first serve)
        // The first request in the list is the one that was added first
        selected.add(readyRequests.remove(0));
        
        // Put remaining ready requests back for next cycle
        if (!readyRequests.isEmpty()) {
            pendingRequests.addAll(readyRequests);
            System.out.println(String.format(
                "[CDB ARBITRATION] Cycle %d: %d instruction(s) deferred to next cycle",
                currentCycle, readyRequests.size()
            ));
        }
        
        return selected;
    }
    
    // REMOVED: getPriority() method - no longer needed with FCFS arbitration
    // Previous priority-based arbitration has been replaced with First-Come-First-Serve
    
    public void clear() {
        pendingRequests.clear();
    }
    
    public String getArbitrationStrategy() {
        return "First-Come-First-Serve (FCFS): The first instruction to complete execution broadcasts first. If multiple instructions complete in the same cycle, the first one added to the queue broadcasts, others wait for the next cycle.";
    }
}