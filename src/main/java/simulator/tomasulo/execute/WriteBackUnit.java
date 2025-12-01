package simulator.tomasulo.execute;

public class WriteBackUnit {
    private BroadcastManager broadcastManager;
    
    public WriteBackUnit() {
        // Default
    }
    
    public WriteBackUnit(BroadcastManager broadcastManager) {
        this.broadcastManager = broadcastManager;
    }
    
    public void writeBackCycle(int currentCycle) {
        if (broadcastManager != null) {
            broadcastManager.processBroadcastsForCycle(currentCycle);
        } else {
            // Fallback
            CommonDataBus.getInstance().processBroadcasts(currentCycle);
        }
    }
    
    public void clear() {
        CommonDataBus.getInstance().clear();
    }
}