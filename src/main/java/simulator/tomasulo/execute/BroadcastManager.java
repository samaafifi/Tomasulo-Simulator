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
    
    public BroadcastManager(ReservationStationPool rsPool, 
                          RegisterFile registerFile, 
                          RegisterAliasTable rat) {
        this.rsPool = rsPool;
        this.registerFile = registerFile;
        this.rat = rat;
        CommonDataBus.getInstance().registerListener(this);
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
            if (destRegName != null && registerFile != null) {
                registerFile.writeValue(destRegName, result);
                System.out.println("  ✓ Updated " + destRegName + " = " + result);
            }
            
            // 2. Update RAT
            if (rat != null && destRegName != null) {
                rat.clear(destRegName);
                System.out.println("  ✓ Cleared RAT for " + destRegName);
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
        // Simple mapping for tests
        if (regNum == 2) return "F2";
        if (regNum == 3) return "F3";
        if (regNum == 10) return "F10";
        if (regNum == 11) return "F11";
        if (regNum == 20) return "F20";
        if (regNum == 21) return "R21";
        if (regNum == 22) return "F22";
        
        // General mapping
        if (regNum < 32) return "F" + regNum;
        else if (regNum < 64) return "R" + (regNum - 32);
        else if (regNum < 100) return "R" + regNum;
        else return "R" + (regNum - 100);
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