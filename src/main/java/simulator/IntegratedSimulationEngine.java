package simulator;

import simulator.tomasulo.execute.*;
import simulator.tomasulo.issue.*;
import simulator.tomasulo.registerfile.*;
import java.util.*;

public class IntegratedSimulationEngine {
    // Components from all members
    private ExecutionUnit executionUnit;
    private WriteBackUnit writeBackUnit;
    private CommonDataBus cdb;
    private BroadcastManager broadcastManager;
    
    private ReservationStationPool rsPool;
    private RegisterFile registerFile;
    private RegisterAliasTable rat;
    
    private int currentCycle;
    private Map<String, Integer> instructionLatencies;
    
    public IntegratedSimulationEngine() {
        System.out.println("Initializing Integrated Tomasulo Simulator...");
        initializeComponents();
        currentCycle = 0;
        instructionLatencies = new HashMap<>();
        setDefaultLatencies();
    }
    
    private void initializeComponents() {
        try {
            // Initialize Member 3 components
            registerFile = new RegisterFile();
            registerFile.initializeRegisters();
            rat = new RegisterAliasTable();
            
            // Initialize Member 1 components
            rsPool = new ReservationStationPool();
            
            // Initialize Member 2 components
            cdb = CommonDataBus.getInstance();
            broadcastManager = new BroadcastManager(rsPool, registerFile, rat);
            executionUnit = new ExecutionUnit();
            writeBackUnit = new WriteBackUnit();
            
            System.out.println("All components initialized successfully");
            
        } catch (Exception e) {
            System.err.println("Error initializing components: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void setDefaultLatencies() {
        instructionLatencies.put("DADDI", 1);
        instructionLatencies.put("DSUBI", 1);
        instructionLatencies.put("ADD.D", 2);
        instructionLatencies.put("SUB.D", 2);
        instructionLatencies.put("MUL.D", 10);
        instructionLatencies.put("DIV.D", 40);
        instructionLatencies.put("LW", 2);
        instructionLatencies.put("LD", 2);
        instructionLatencies.put("L.S", 2);
        instructionLatencies.put("L.D", 2);
        instructionLatencies.put("SW", 2);
        instructionLatencies.put("SD", 2);
        instructionLatencies.put("S.S", 2);
        instructionLatencies.put("S.D", 2);
        instructionLatencies.put("BEQ", 1);
        instructionLatencies.put("BNE", 1);
    }
    
    public void runOneCycle() {
        currentCycle++;
        System.out.println("\n" + "=".repeat(50));
        System.out.println("CYCLE " + currentCycle);
        System.out.println("=".repeat(50));
        
        // Tomasulo stages
        
        // 1. WRITE-BACK STAGE (Member 2)
        System.out.println("\n[WRITE-BACK STAGE]");
        writeBackUnit.writeBackCycle(currentCycle);
        
        // 2. EXECUTE STAGE (Member 2)
        System.out.println("\n[EXECUTE STAGE]");
        executionUnit.cycle(currentCycle);
        
        // Display system state
        printSystemStatus();
    }
    
    private void printSystemStatus() {
        System.out.println("\n[SYSTEM STATUS]");
        
        // Show reservation stations
        try {
            List<ReservationStation> stations = rsPool.getAllStations();
            int busyCount = 0;
            for (ReservationStation rs : stations) {
                if (rs.isBusy()) busyCount++;
            }
            System.out.println("Reservation Stations: " + busyCount + "/" + stations.size() + " busy");
        } catch (Exception e) {
            System.out.println("Reservation Stations: Info not available");
        }
        
        // Show execution timers
        Map<String, Integer> timers = executionUnit.getExecutionTimers();
        if (!timers.isEmpty()) {
            System.out.println("Active executions: " + timers);
        }
        
        // Show register file status
        try {
            System.out.println("Register File: 64 registers initialized");
        } catch (Exception e) {
            System.out.println("Register File: Info not available");
        }
    }
    
    // Method to simulate instruction execution
    public void simulateInstruction(String instructionType, int destReg) {
        System.out.println("\n>>> Simulating instruction: " + instructionType + " R" + destReg);
        
        try {
            // Map instruction type to station type
            String stationType = mapToStationType(instructionType);
            
            // Allocate reservation station
            ReservationStation rs = rsPool.allocateStation(stationType);
            if (rs == null) {
                System.out.println("No available " + stationType + " station");
                return;
            }
            
            // Mark RS as busy
            rs.setBusy(true);
            rs.setOp(instructionType);
            
            // Start execution
            int latency = instructionLatencies.getOrDefault(instructionType, 1);
            executionUnit.startExecution(rs.getName(), instructionType, destReg, latency, currentCycle);
            
            System.out.println("✓ Instruction issued to " + rs.getName());
            
        } catch (Exception e) {
            System.out.println("Error simulating instruction: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private String mapToStationType(String instructionType) {
        if (instructionType.contains("ADD.D") || instructionType.contains("SUB.D") ||
            instructionType.contains("ADD.S") || instructionType.contains("SUB.S")) {
            return "FP_ADD";
        } else if (instructionType.contains("MUL.D") || instructionType.contains("MUL.S")) {
            return "FP_MUL";
        } else if (instructionType.contains("DIV.D") || instructionType.contains("DIV.S")) {
            return "FP_DIV";
        } else if (instructionType.contains("DADD") || instructionType.contains("DSUB")) {
            return "INTEGER_ADD";
        } else if (instructionType.contains("L.") || instructionType.equals("LW") || instructionType.equals("LD")) {
            return "LOAD";
        } else if (instructionType.contains("S.") || instructionType.equals("SW") || instructionType.equals("SD")) {
            return "STORE";
        } else if (instructionType.equals("BEQ") || instructionType.equals("BNE")) {
            return "BRANCH";
        }
        return "FP_ADD"; // Default
    }
    
    // Getters
    public ExecutionUnit getExecutionUnit() { return executionUnit; }
    public WriteBackUnit getWriteBackUnit() { return writeBackUnit; }
    public RegisterFile getRegisterFile() { return registerFile; }
    public ReservationStationPool getReservationStationPool() { return rsPool; }
    public int getCurrentCycle() { return currentCycle; }
    
    public void reset() {
        currentCycle = 0;
        executionUnit.clear();
        writeBackUnit.clear();
        cdb.clear();
        
        try {
            registerFile.reset();
            rat.clearAll();
            rsPool.reset();
        } catch (Exception e) {
            System.out.println("Note: Some components reset with errors");
        }
        
        initializeComponents();
    }
    
    public static void main(String[] args) {
        System.out.println("=== Integrated Tomasulo Simulation Engine ===");
        IntegratedSimulationEngine engine = new IntegratedSimulationEngine();
        
        // Run a simple simulation
        engine.simulateInstruction("ADD.D", 2);
        engine.simulateInstruction("DADDI", 1);
        
        for (int i = 1; i <= 5; i++) {
            engine.runOneCycle();
        }
        
        System.out.println("\nSimulation completed!");
    }
}