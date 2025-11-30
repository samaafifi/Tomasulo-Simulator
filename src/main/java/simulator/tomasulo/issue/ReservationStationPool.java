package simulator.tomasulo.issue;

import java.util.*;

 public class ReservationStationPool {
     // Maps from station type to list of stations
     private Map<String, List<ReservationStation>> stationsByType;
     
     // All stations for easy iteration
     private List<ReservationStation> allStations;
     
     // Configuration for number of each type of station
     private Map<String, Integer> stationCounts;
     
     /**
      * Constructor with default configuration
      */
     public ReservationStationPool() {
         this.stationsByType = new HashMap<>();
         this.allStations = new ArrayList<>();
         this.stationCounts = new HashMap<>();
         
         // Default configuration
         initializeDefaultConfiguration();
     }
     
     /**
      * Constructor with custom configuration
      */
     public ReservationStationPool(Map<String, Integer> stationCounts) {
         this.stationsByType = new HashMap<>();
         this.allStations = new ArrayList<>();
         this.stationCounts = stationCounts;
         
         initializeStations();
     }
     
     /**
      * Initialize with default number of stations
      */
     private void initializeDefaultConfiguration() {
         stationCounts.put("FP_ADD", 3);      // 3 FP Add/Sub stations
         stationCounts.put("FP_MUL", 2);      // 2 FP Multiply stations
         stationCounts.put("FP_DIV", 2);      // 2 FP Divide stations
         stationCounts.put("LOAD", 2);        // 2 Load buffers
         stationCounts.put("STORE", 2);       // 2 Store buffers
         stationCounts.put("INTEGER_ADD", 2); // 2 Integer Add/Sub stations
         stationCounts.put("BRANCH", 1);      // 1 Branch unit
         
         initializeStations();
     }
     
     /**
      * Creates all reservation stations based on configuration
      */
     private void initializeStations() {
         for (Map.Entry<String, Integer> entry : stationCounts.entrySet()) {
             String type = entry.getKey();
             int count = entry.getValue();
             
             List<ReservationStation> stations = new ArrayList<>();
             
             for (int i = 1; i <= count; i++) {
                 String name = getStationName(type, i);
                 ReservationStation rs = new ReservationStation(name, type);
                 stations.add(rs);
                 allStations.add(rs);
             }
             
             stationsByType.put(type, stations);
         }
     }
     
     /**
      * Generates a name for a reservation station
      */
     private String getStationName(String type, int index) {
         switch (type) {
             case "FP_ADD":
                 return "Add" + index;
             case "FP_MUL":
                 return "Mult" + index;
             case "FP_DIV":
                 return "Div" + index;
             case "LOAD":
                 return "Load" + index;
             case "STORE":
                 return "Store" + index;
             case "INTEGER_ADD":
                 return "IntAdd" + index;
             case "BRANCH":
                 return "Branch" + index;
             default:
                 return type + index;
         }
     }
     
     /**
      * Checks if there's an available station of the given type
      */
     public boolean hasAvailableStation(String type) {
         List<ReservationStation> stations = stationsByType.get(type);
         if (stations == null) {
             return false;
         }
         
         for (ReservationStation rs : stations) {
             if (!rs.isBusy()) {
                 return true;
             }
         }
         return false;
     }
     
     /**
      * Allocates an available station of the given type
      * Returns null if no station is available
      */
     public ReservationStation allocateStation(String type) {
         List<ReservationStation> stations = stationsByType.get(type);
         if (stations == null) {
             System.err.println("Unknown reservation station type: " + type);
             return null;
         }
         
         for (ReservationStation rs : stations) {
             if (!rs.isBusy()) {
                 return rs;
             }
         }
         
         return null; // No available station
     }
     
     /**
      * Releases a reservation station (makes it available)
      */
     public void releaseStation(ReservationStation rs) {
         if (rs != null) {
             rs.clear();
         }
     }
     
     /**
      * Releases a reservation station by name
      */
     public void releaseStation(String name) {
         ReservationStation rs = getStationByName(name);
         if (rs != null) {
             rs.clear();
         }
     }
     
     /**
      * Gets a reservation station by name
      */
     public ReservationStation getStationByName(String name) {
         for (ReservationStation rs : allStations) {
             if (rs.getName().equals(name)) {
                 return rs;
             }
         }
         return null;
     }
     
     /**
      * Gets all stations of a specific type
      */
     public List<ReservationStation> getStationsByType(String type) {
         return stationsByType.get(type);
     }
     
     /**
      * Gets all reservation stations
      */
     public List<ReservationStation> getAllStations() {
         return new ArrayList<>(allStations);
     }
     
     /**
      * Gets all busy stations
      */
     public List<ReservationStation> getBusyStations() {
         List<ReservationStation> busy = new ArrayList<>();
         for (ReservationStation rs : allStations) {
             if (rs.isBusy()) {
                 busy.add(rs);
             }
         }
         return busy;
     }
     
     /**
      * Gets all stations ready to execute
      */
     public List<ReservationStation> getReadyStations() {
         List<ReservationStation> ready = new ArrayList<>();
         for (ReservationStation rs : allStations) {
             if (rs.isReadyToExecute()) {
                 ready.add(rs);
             }
         }
         return ready;
     }
     
     /**
      * Updates all stations when a result is broadcast
      */
     public void broadcastResult(String producer, Double value) {
         for (ReservationStation rs : allStations) {
             if (rs.isBusy()) {
                 rs.updateOperand(producer, value);
             }
         }
     }
     
     /**
      * Gets count of available stations by type
      */
     public int getAvailableCount(String type) {
         List<ReservationStation> stations = stationsByType.get(type);
         if (stations == null) {
             return 0;
         }
         
         int count = 0;
         for (ReservationStation rs : stations) {
             if (!rs.isBusy()) {
                 count++;
             }
         }
         return count;
     }
     
     /**
      * Gets total count of stations by type
      */
     public int getTotalCount(String type) {
         List<ReservationStation> stations = stationsByType.get(type);
         return (stations != null) ? stations.size() : 0;
     }
     
     /**
      * Resets all stations to initial state
      */
     public void reset() {
         for (ReservationStation rs : allStations) {
             rs.clear();
         }
     }
     
     /**
      * Gets configuration of station counts
      */
     public Map<String, Integer> getStationCounts() {
         return new HashMap<>(stationCounts);
     }
     
     /**
      * Updates the configuration and reinitializes stations
      */
     public void reconfigure(Map<String, Integer> newCounts) {
         this.stationCounts = newCounts;
         this.stationsByType.clear();
         this.allStations.clear();
         initializeStations();
     }
     
     /**
      * Prints the status of all reservation stations
      */
     public void printStatus() {
         System.out.println("\n=== Reservation Stations Status ===");
         
         for (String type : stationsByType.keySet()) {
             System.out.println("\n" + type + ":");
             List<ReservationStation> stations = stationsByType.get(type);
             for (ReservationStation rs : stations) {
                 System.out.println("  " + rs.toString());
             }
         }
         System.out.println();
     }
     
     /**
      * Gets summary statistics
      */
     public Map<String, String> getStatistics() {
         Map<String, String> stats = new HashMap<>();
         
         int totalStations = allStations.size();
         int busyStations = getBusyStations().size();
         int readyStations = getReadyStations().size();
         
         stats.put("Total Stations", String.valueOf(totalStations));
         stats.put("Busy Stations", String.valueOf(busyStations));
         stats.put("Available Stations", String.valueOf(totalStations - busyStations));
         stats.put("Ready to Execute", String.valueOf(readyStations));
         
         return stats;
     }
 }
