package simulator.tomasulo.registerfile;


import java.util.HashMap;
import java.util.Map;

/**
 * Register Alias Table (RAT) for Tomasulo's algorithm.
 * Tracks which reservation station will produce each register's value.
 * This implements register renaming to eliminate WAR and WAW hazards.
 */
public class RegisterAliasTable {
    private final Map<String, String> bindings; // register name -> station tag
    
    public RegisterAliasTable() {
        this.bindings = new HashMap<>();
    }
    
    /**
     * Creates a binding between a register and the reservation station that will produce its value.
     */
    public void bind(String registerName, String stationTag) {
        bindings.put(registerName, stationTag);
    }
    
    /**
     * Returns the reservation station tag that will produce this register's value.
     * Returns null if no binding exists (register is ready).
     */
    public String getProducer(String registerName) {
        return bindings.get(registerName);
    }
    
    /**
     * Checks if a register has a pending write (is bound to a station).
     */
    public boolean hasBinding(String registerName) {
        return bindings.containsKey(registerName);
    }
    
    /**
     * Removes the binding for a specific register.
     * Called when a register's value is written back.
     */
    public void clear(String registerName) {
        bindings.remove(registerName);
    }
    
    /**
     * Clears all bindings. Used for reset.
     */
    public void clearAll() {
        bindings.clear();
    }
    
    /**
     * Returns a copy of all current bindings for debugging/display.
     */
    public Map<String, String> getAllBindings() {
        return new HashMap<>(bindings);
    }
    
    /**
     * Clears all bindings for a specific reservation station.
     * Called during write-back when a station broadcasts its result.
     */
    public void clearByStation(String stationTag) {
        bindings.entrySet().removeIf(entry -> entry.getValue().equals(stationTag));
    }
}
