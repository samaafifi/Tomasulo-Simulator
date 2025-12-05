package simulator.tomasulo.execute;

import simulator.tomasulo.issue.ReservationStation;
import simulator.tomasulo.issue.ReservationStationPool;
import simulator.tomasulo.registerfile.RegisterFile;
import simulator.tomasulo.registerfile.RegisterAliasTable;
import java.util.List;

public class BroadcastManager implements CommonDataBus.BroadcastListener {
    private ReservationStationPool rsPool;
    private RegisterFile registerFile;
    private RegisterAliasTable rat;
    
    public BroadcastManager() {
        // Default constructor - components will be set later if needed
    }
    
    public BroadcastManager(ReservationStationPool rsPool, 
                          RegisterFile registerFile, 
                          RegisterAliasTable rat) {
        this.rsPool = rsPool;
        this.registerFile = registerFile;
        this.rat = rat;
        CommonDataBus.getInstance().registerListener(this);
    }
    
    /**
     * Broadcast results (processes CDB broadcasts)
     * This is a convenience method that processes broadcasts for cycle 0.
     * For proper cycle tracking, use processBroadcastsForCycle() instead.
     */
    public void broadcast() {
        // Process any pending broadcasts (using cycle 0 as default)
        // Note: processBroadcastsForCycle() is preferred for proper cycle tracking
        processBroadcastsForCycle(0);
    }
    
    @Override
    public void onBroadcast(int reservationStationId, double result, int destRegister) {
        // Convert RS ID to station name
        String stationName = ExecutionUnit.rsIdToStationName(reservationStationId);
        
        System.out.println(String.format(
            "[BroadcastManager] %s -> R%d = %.2f",
            stationName, destRegister, result
        ));
        
        try {
            // Convert dest register number to name (e.g., 2 -> "F2")
            String destRegName = convertToRegName(destRegister);
            
            // 1. Update Register File
            // CRITICAL FIX: Check if register's Qi still points to this station (WAW hazard handling)
            // If a later instruction has already updated the Qi, this write should be ignored
            // Only update if Qi matches this station (or if Qi is null, meaning no pending write)
            if (destRegName != null && registerFile != null) {
                String currentQi = registerFile.getQi(destRegName);
                if (stationName.equals(currentQi)) {
                    // Qi still points to this station - safe to update
                registerFile.writeValue(destRegName, result);
                System.out.println("  ✓ Updated " + destRegName + " = " + result);
                } else if (currentQi == null || currentQi.isEmpty()) {
                    // No pending write - safe to update (shouldn't happen in normal flow, but handle gracefully)
                    registerFile.writeValue(destRegName, result);
                    System.out.println("  ✓ Updated " + destRegName + " = " + result + " (no pending write)");
                } else {
                    // Qi points to a different station - this is a WAW scenario
                    // A later instruction has already claimed this register - ignore this write
                    System.out.println("  ⚠ Skipped update of " + destRegName + " - WAW: Qi now points to " + currentQi + " (not " + stationName + ")");
                }
            }
            
            // 2. Update RAT - only clear if we actually updated the register
            if (rat != null && destRegName != null) {
                String currentQi = registerFile.getQi(destRegName);
                if (stationName.equals(currentQi)) {
                rat.clear(destRegName);
                System.out.println("  ✓ Cleared RAT for " + destRegName);
                }
            }
            
            // 3. Update waiting stations using STATION NAME
            updateWaitingStations(stationName, result);
            
            // 4. Free the station using STATION NAME
            if (rsPool != null && stationName != null) {
                rsPool.releaseStation(stationName);
                System.out.println("  ✓ Released " + stationName);
            }
            
        } catch (Exception e) {
            System.err.println("Broadcast error: " + e.getMessage());
        }
    }
    
    private String convertToRegName(int regNum) {
        // F0-F31: regNum 0-31 (floating point registers)
        if (regNum >= 0 && regNum < 32) {
            return "F" + regNum;
        }
        // R0-R31: regNum 32-63 (integer registers, mapped as regNum - 32)
        else if (regNum >= 32 && regNum < 64) {
            return "R" + (regNum - 32);
        }
        // Fallback for other mappings (legacy support)
        else if (regNum < 100) {
            return "R" + regNum;
        }
        else {
            return "R" + (regNum - 100);
        }
    }
    
    private void updateWaitingStations(String producerName, double result) {
        try {
            if (rsPool == null || producerName == null) return;
            
            List<ReservationStation> stations = rsPool.getAllStations();
            
            for (ReservationStation rs : stations) {
                if (rs.isBusy()) {
                    String qj = rs.getQj();
                    String qk = rs.getQk();
                    
                    if (producerName.equals(qj)) {
                        rs.setVj(result);
                        rs.setQj(null);
                        System.out.println("  ✓ Updated " + rs.getName() + ".Vj");
                    }
                    if (producerName.equals(qk)) {
                        rs.setVk(result);
                        rs.setQk(null);
                        System.out.println("  ✓ Updated " + rs.getName() + ".Vk");
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error updating stations: " + e.getMessage());
        }
    }
    
    public void processBroadcastsForCycle(int currentCycle) {
        List<BroadcastRequest> broadcasts = CommonDataBus.getInstance()
            .processBroadcasts(currentCycle);
            
        for (BroadcastRequest request : broadcasts) {
            onBroadcast(
                request.getRsId(),
                request.getResult(),
                request.getDestReg()
            );
        }
    }
}