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
    private ExecutionUnit executionUnit;
    
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
     * Set execution unit reference (needed to mark stations as just ready)
     */
    public void setExecutionUnit(ExecutionUnit executionUnit) {
        this.executionUnit = executionUnit;
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
            // TOMASULO WAW HAZARD PROTECTION:
            // Check if register's Qi still points to this station
            // Example: ADD.D F2, ... (Add1) then SUB.D F2, ... (Add2)
            // When Add1 broadcasts: F2.Qi = "Add2" (NOT "Add1")
            // → Don't update F2 register (newer instruction owns it)
            // → DO update waiting stations (they need Add1's old F2)
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
                    // WAW HAZARD DETECTED!
                    // Qi points to a different station - newer instruction has claimed this register
                    // Don't update register, but DO update waiting stations (they need this old value)
                    System.out.println("  ⚠ WAW Protection: Skipped " + destRegName + " register update");
                    System.out.println("    Current Qi: " + currentQi + ", Broadcasting: " + stationName);
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
    
    /**
     * TOMASULO ASSOCIATIVE BROADCAST
     * Updates ALL waiting reservation stations simultaneously
     * This is the magic of Tomasulo - everyone listening gets the value at once!
     * 
     * For each busy station:
     * - If Qj == producerName → Vj = result, Qj = null (operand 1 ready!)
     * - If Qk == producerName → Vk = result, Qk = null (operand 2 ready!)
     * 
     * When both Qj and Qk become null, station is ready to execute!
     */
    private void updateWaitingStations(String producerName, double result) {
        try {
            if (rsPool == null || producerName == null) return;
            
            List<ReservationStation> stations = rsPool.getAllStations();
            
            for (ReservationStation rs : stations) {
                if (rs.isBusy()) {
                    String qj = rs.getQj();
                    String qk = rs.getQk();
                    boolean operandUpdated = false;
                    
                    // Check first operand (Vj/Qj)
                    if (producerName.equals(qj)) {
                        rs.setVj(result);  // Copy value
                        rs.setQj(null);    // Clear dependency tag
                        System.out.println("  ✓ Updated " + rs.getName() + ".Vj = " + result + " (was waiting on " + producerName + ")");
                        operandUpdated = true;
                    }
                    
                    // Check second operand (Vk/Qk)
                    if (producerName.equals(qk)) {
                        rs.setVk(result);  // Copy value
                        rs.setQk(null);    // Clear dependency tag
                        System.out.println("  ✓ Updated " + rs.getName() + ".Vk = " + result + " (was waiting on " + producerName + ")");
                        operandUpdated = true;
                    }
                    
                    // If operands just became ready, mark station to defer execution start
                    // Execution starts in NEXT cycle, not same cycle as broadcast
                    boolean isStationReady = (rs.getQj() == null || rs.getQj().isEmpty()) && 
                                            (rs.getQk() == null || rs.getQk().isEmpty());
                    if (operandUpdated && isStationReady && executionUnit != null) {
                        executionUnit.markStationJustReady(rs.getName());
                        System.out.println("  ⏸ " + rs.getName() + " now ready (both operands available) - execution starts next cycle");
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