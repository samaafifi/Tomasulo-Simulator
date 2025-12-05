package simulator.tomasulo.execute;

import java.util.*;

public class CommonDataBus {
    private static CommonDataBus instance;
    private List<BroadcastListener> listeners;
    private CDBArbitration arbitration;
    
    /**
     * Private constructor for singleton pattern
     * Public constructor for non-singleton usage
     */
    public CommonDataBus() {
        this.listeners = new ArrayList<>();
        this.arbitration = new CDBArbitration();
    }
    
    public static synchronized CommonDataBus getInstance() {
        if (instance == null) {
            instance = new CommonDataBus();
        }
        return instance;
    }
    
    public void registerListener(BroadcastListener listener) {
        listeners.add(listener);
    }
    
    public void broadcastResult(int reservationStationId, double result, int destRegister) {
        for (BroadcastListener listener : listeners) {
            listener.onBroadcast(reservationStationId, result, destRegister);
        }
    }
    
    public void addBroadcastRequest(BroadcastRequest request) {
        arbitration.addRequest(request);
    }
    
    public List<BroadcastRequest> processBroadcasts(int currentCycle) {
        List<BroadcastRequest> broadcasts = arbitration.selectBroadcasts(currentCycle);
        
        // Actually broadcast the results to all listeners
        for (BroadcastRequest request : broadcasts) {
            broadcastResult(
                request.getRsId(),
                request.getResult(),
                request.getDestReg()
            );
        }
        
        return broadcasts;
    }
    
    public void clear() {
        arbitration.clear();
        listeners.clear();
    }
    
    public interface BroadcastListener {
        void onBroadcast(int reservationStationId, double result, int destRegister);
    }
}