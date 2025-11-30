package simulator.tomasulo.registerfile;


import java.util.Map;

/**
 * Interface for the Register File component in Tomasulo's algorithm.
 * This defines the public API that other team members will use to interact with registers.
 * 
 * Used by:
 * - Member 1 (Issue stage): Check register readiness, get Qi values
 * - Member 2 (Write-back stage): Write results, clear Qi fields
 * - Member 6 (GUI): Display register status
 */
public interface IRegisterFile {
    
    // === Initialization ===
    
    /**
     * Initializes all 64 registers (F0-F31, R0-R31) with default values.
     */
    void initializeRegisters();
    
    /**
     * Resets all registers to initial state (value=0, Qi=null).
     */
    void reset();
    
    /**
     * Preloads specific registers with initial values.
     * Used to set up test cases or user-defined initial state.
     */
    void preloadValues(Map<String, Double> initialValues);
    
    // === Read Operations (for Issue stage) ===
    
    /**
     * Checks if a register has a valid value (Qi is null).
     */
    boolean isRegisterReady(String registerName);
    
    /**
     * Returns the Qi field (reservation station tag) for a register.
     * Returns null if register is ready.
     */
    String getQi(String registerName);
    
    /**
     * Reads the value from a register.
     * Throws IllegalStateException if register is busy (Qi != null).
     */
    double readValue(String registerName);
    
    /**
     * Returns the Register object for direct access.
     * Use with caution - prefer specific methods.
     */
    Register getRegister(String registerName);
    
    // === Write Operations (for Issue stage) ===
    
    /**
     * Sets the Qi field for a register, marking it as waiting for a station's result.
     * Called when an instruction issues and allocates a destination register.
     */
    void setQi(String registerName, String stationTag);
    
    // === Write-Back Operations (for Write-Back stage) ===
    
    /**
     * Writes a value to a register and clears its Qi field.
     */
    void writeValue(String registerName, double value);
    
    /**
     * Clears the Qi field for a register without writing a value.
     */
    void clearQi(String registerName);
    
    /**
     * Write-back operation: updates all registers waiting for a specific station.
     * Called when a reservation station broadcasts its result on the CDB.
     */
    void writeBackFromStation(String stationTag, double value);
    
    // === Status & Display (for GUI) ===
    
    /**
     * Returns a snapshot of all register statuses for GUI display.
     */
    Map<String, RegisterStatus> getAllRegisterStatus(int currentCycle);
    
    /**
     * Returns all Register objects.
     */
    Map<String, Register> getAllRegisters();
}