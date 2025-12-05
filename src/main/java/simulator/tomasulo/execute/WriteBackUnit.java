package simulator.tomasulo.execute;

public class WriteBackUnit {
    private BroadcastManager broadcastManager;
    private int currentCycle = 0;
    
    public WriteBackUnit() {
        // Default - will use CommonDataBus directly
    }
    
    public WriteBackUnit(BroadcastManager broadcastManager) {
        this.broadcastManager = broadcastManager;
    }
    
    public void writeBackCycle(int currentCycle) {
        this.currentCycle = currentCycle;
        if (broadcastManager != null) {
            broadcastManager.processBroadcastsForCycle(currentCycle);
        } else {
            // Fallback
            CommonDataBus.getInstance().processBroadcasts(currentCycle);
        }
    }
    
    /**
     * Write-back cycle without parameter (uses internal cycle counter)
     */
    public void writeBackCycle() {
        currentCycle++;
        writeBackCycle(currentCycle);
    }
    
    public void clear() {
        CommonDataBus.getInstance().clear();
        currentCycle = 0;
    }
}