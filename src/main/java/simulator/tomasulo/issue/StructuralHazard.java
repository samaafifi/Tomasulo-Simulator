package simulator.tomasulo.issue;

import java.util.*;

 public class StructuralHazard {
    private boolean hasHazard;
    private String message;
    private String resourceType;
    private int cycleDetected;
    
    /**
     * Constructor for structural hazard
     */
    public StructuralHazard(boolean hasHazard, String message) {
        this.hasHazard = hasHazard;
        this.message = message;
        this.resourceType = "";
        this.cycleDetected = -1;
    }
    
    /**
     * Constructor with detailed information
     */
    public StructuralHazard(boolean hasHazard, String message, String resourceType, int cycleDetected) {
        this.hasHazard = hasHazard;
        this.message = message;
        this.resourceType = resourceType;
        this.cycleDetected = cycleDetected;
    }
    
    /**
     * Checks if a hazard exists
     */
    public boolean hasHazard() {
        return hasHazard;
    }
    
    /**
     * Gets the hazard message
     */
    public String getMessage() {
        return message;
    }
    
    /**
     * Gets the resource type causing the hazard
     */
    public String getResourceType() {
        return resourceType;
    }
    
    /**
     * Gets the cycle when the hazard was detected
     */
    public int getCycleDetected() {
        return cycleDetected;
    }
    
    /**
     * Sets the resource type
     */
    public void setResourceType(String resourceType) {
        this.resourceType = resourceType;
    }
    
    /**
     * Sets the cycle detected
     */
    public void setCycleDetected(int cycle) {
        this.cycleDetected = cycle;
    }
    
    /**
     * Creates a no-hazard instance
     */
    public static StructuralHazard noHazard() {
        return new StructuralHazard(false, "No structural hazard");
    }
    
    /**
     * Creates a reservation station full hazard
     */
    public static StructuralHazard reservationStationFull(String stationType, int cycle) {
        return new StructuralHazard(
            true,
            "All " + stationType + " reservation stations are full",
            stationType,
            cycle
        );
    }
    
    /**
     * Creates a load buffer full hazard
     */
    public static StructuralHazard loadBufferFull(int cycle) {
        return new StructuralHazard(
            true,
            "All load buffers are full",
            "LOAD",
            cycle
        );
    }
    
    /**
     * Creates a store buffer full hazard
     */
    public static StructuralHazard storeBufferFull(int cycle) {
        return new StructuralHazard(
            true,
            "All store buffers are full",
            "STORE",
            cycle
        );
    }
    
    /**
     * Creates a common data bus conflict hazard
     */
    public static StructuralHazard cdbConflict(int cycle) {
        return new StructuralHazard(
            true,
            "Common Data Bus is busy - multiple instructions want to write",
            "CDB",
            cycle
        );
    }
    
    /**
     * Creates a reorder buffer full hazard (if using ROB)
     */
    public static StructuralHazard reorderBufferFull(int cycle) {
        return new StructuralHazard(
            true,
            "Reorder buffer is full",
            "ROB",
            cycle
        );
    }
    
    /**
     * Returns a detailed string representation
     */
    @Override
    public String toString() {
        if (!hasHazard) {
            return "No structural hazard";
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append("STRUCTURAL HAZARD: ").append(message);
        
        if (!resourceType.isEmpty()) {
            sb.append(" [Resource: ").append(resourceType).append("]");
        }
        
        if (cycleDetected >= 0) {
            sb.append(" [Cycle: ").append(cycleDetected).append("]");
        }
        
        return sb.toString();
    }
    
    /**
     * Returns a short description suitable for logging
     */
    public String toShortString() {
        if (!hasHazard) {
            return "OK";
        }
        return resourceType + " unavailable";
    }
    
    /**
     * Checks if this hazard is of a specific type
     */
    public boolean isType(String type) {
        return this.resourceType.equalsIgnoreCase(type);
    }
    
    /**
     * Compares two hazards for equality
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        StructuralHazard other = (StructuralHazard) obj;
        return hasHazard == other.hasHazard &&
               resourceType.equals(other.resourceType) &&
               cycleDetected == other.cycleDetected;
    }
    
    /**
     * Returns hash code
     */
    @Override
    public int hashCode() {
        int result = Boolean.hashCode(hasHazard);
        result = 31 * result + resourceType.hashCode();
        result = 31 * result + cycleDetected;
        return result;
    }
}

/**
 * Utility class for tracking structural hazards throughout simulation
 */
class StructuralHazardTracker {
    private List<StructuralHazard> hazardHistory;
    private Map<String, Integer> hazardCountByType;
    
    public StructuralHazardTracker() {
        this.hazardHistory = new ArrayList<>();
        this.hazardCountByType = new HashMap<>();
    }
    
    /**
     * Records a hazard
     */
    public void recordHazard(StructuralHazard hazard) {
        if (hazard.hasHazard()) {
            hazardHistory.add(hazard);
            
            String type = hazard.getResourceType();
            hazardCountByType.put(type, hazardCountByType.getOrDefault(type, 0) + 1);
        }
    }
    
    /**
     * Gets total number of hazards
     */
    public int getTotalHazards() {
        return hazardHistory.size();
    }
    
    /**
     * Gets number of hazards by type
     */
    public int getHazardCount(String type) {
        return hazardCountByType.getOrDefault(type, 0);
    }
    
    /**
     * Gets all hazards
     */
    public List<StructuralHazard> getAllHazards() {
        return new ArrayList<>(hazardHistory);
    }
    
    /**
     * Gets hazards for a specific cycle
     */
    public List<StructuralHazard> getHazardsForCycle(int cycle) {
        List<StructuralHazard> cycleHazards = new ArrayList<>();
        for (StructuralHazard hazard : hazardHistory) {
            if (hazard.getCycleDetected() == cycle) {
                cycleHazards.add(hazard);
            }
        }
        return cycleHazards;
    }
    
    /**
     * Clears all tracked hazards
     */
    public void reset() {
        hazardHistory.clear();
        hazardCountByType.clear();
    }
    
    /**
     * Generates a summary report
     */
    public String generateReport() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== Structural Hazard Report ===\n");
        sb.append("Total hazards: ").append(getTotalHazards()).append("\n\n");
        
        if (!hazardCountByType.isEmpty()) {
            sb.append("Hazards by resource type:\n");
            for (Map.Entry<String, Integer> entry : hazardCountByType.entrySet()) {
                sb.append("  ").append(entry.getKey()).append(": ")
                  .append(entry.getValue()).append("\n");
            }
        }
        
        return sb.toString();
    }
}
