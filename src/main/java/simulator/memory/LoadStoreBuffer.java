package simulator.memory;

import java.util.*;

/**
 * Load/Store Buffer - MINIMAL FIX VERSION
 * Compatible with your original constructor
 */
public class LoadStoreBuffer {

    // ---------------- ENTRY STRUCT ---------------- //
    public static class Entry {
        public int id;               // unique ID
        public String op;            // "LW", "SW", "LD", "SD", "L.S", "L.D", "S.S", "S.D"
        public int address;          // effective address
        public long value;           // for stores: value to write; for loads: loaded value
        public int remainingCycles;  // countdown until ready
        public boolean busy;         // true = still executing
        public boolean isLoad;       // true for loads, false for stores
        public String destReg;       // destination register (for loads)
    }

    // ------------- INTERNAL STATE --------------- //
    private final List<Entry> buffer = new ArrayList<>();
    private int nextId = 1;

    // Memory system needed for commit
    private final CacheSimulator cache;
    private final int maxSize;  // Maximum size of buffer

    // Constructor with maxSize parameter
    public LoadStoreBuffer(CacheSimulator cache, int maxSize) {
        this.cache = cache;
        this.maxSize = maxSize;
    }

    // Keep original constructor for backward compatibility (optional)
    public LoadStoreBuffer(CacheSimulator cache) {
        this(cache, Integer.MAX_VALUE);  // No limit by default
    }

    // ------------- ADD NEW OPERATIONS ------------- //

    /** Add a new LOAD instruction (ENHANCED with destReg) */
    public Entry addLoad(String op, int address, int latency, String destReg) {
         if (isFull()) {
            throw new IllegalStateException("Load/Store Buffer is full!");
        }
        Entry e = new Entry();
        e.id = nextId++;
        e.op = op;               // "LW", "LD", "L.S", "L.D"
        e.address = address;
        e.remainingCycles = latency;
        e.busy = true;
        e.isLoad = true;
        e.destReg = destReg;
        buffer.add(e);
        return e;
    }

    /** Add a new STORE instruction (ENHANCED with long value) */
    public Entry addStore(String op, int address, long value, int latency) {
        if (isFull()) {
            throw new IllegalStateException("Load/Store Buffer is full!");
        }
        Entry e = new Entry();
        e.id = nextId++;
        e.op = op;               // "SW", "SD", "S.S", "S.D"
        e.address = address;
        e.value = value;
        e.remainingCycles = latency;
        e.busy = true;
        e.isLoad = false;
        buffer.add(e);
        return e;
    }

    // ---------------- CYCLE UPDATE ---------------- //

    /**
     * Call this every cycle.
     * Returns a list of entries that completed this cycle.
     * NOW WITH ADDRESS CLASH DETECTION!
     */
    public List<Entry> stepCycle() {
        List<Entry> completed = new ArrayList<>();

        Iterator<Entry> it = buffer.iterator();
        while (it.hasNext()) {
            Entry e = it.next();

            if (!e.busy) continue;

            // NEW: Check for address clash with earlier operations
            if (hasAddressClashWithEarlier(e)) {
                // Address clash - must wait for earlier operations to complete
                // Don't decrement timer - this operation is stalled
                continue;  // Must wait for earlier operations
            }

            // decrement timer (only if no address clash)
            e.remainingCycles--;

            if (e.remainingCycles <= 0) {
                // mark complete
                e.busy = false;
                completed.add(e);
                it.remove();      // remove from LSB
            }
        }
        return completed;
    }

    /**
     * NEW: Check if this entry has an address clash with an earlier operation.
     * Ensures memory operations execute in program order.
     */
    private boolean hasAddressClashWithEarlier(Entry current) {
    for (Entry earlier : buffer) {
        // Skip newer or same-age entries; never break (unsafe!)
        if (earlier.id >= current.id)
            continue;

        if (!earlier.busy)
            continue;

        if (addressesOverlap(earlier.address, earlier.op,
                             current.address, current.op)) {
            return true;
        }
    }
    return false;
}

    /**
     * NEW: Check if two memory operations overlap in address space.
     */
    private boolean addressesOverlap(int addr1, String op1, int addr2, String op2) {
        int size1 = getOperationSize(op1);
        int size2 = getOperationSize(op2);
        
        int end1 = addr1 + size1 - 1;
        int end2 = addr2 + size2 - 1;
        
        // Check for overlap
        return !(end1 < addr2 || end2 < addr1);
    }

    /**
     * NEW: Get size in bytes for operation
     */
    private int getOperationSize(String op) {
        switch (op) {
            case "LW":
            case "SW":
            case "L.S":
            case "S.S":
                return 4;  // Word
            case "LD":
            case "SD":
            case "L.D":
            case "S.D":
                return 8;  // Doubleword
            default:
                return 4;  // Default to word size
        }
    }

    // ---------------- COMMIT OPERATIONS ---------------- //

    /**
     * Commit a completed load/store to the cache system.
     * Returns loaded value for loads.
     * ENHANCED: Now supports 8-byte operations!
     */
    public Long commit(Entry e) {
        switch (e.op) {
            case "LW":
            case "L.S":
                // Load 4-byte word
                int wordValue = cache.readWord(e.address);
                e.value = wordValue;
                return (long) wordValue;

            case "LD":
            case "L.D":
                // Load 8-byte doubleword
                long doubleValue = cache.readDoubleWord(e.address);
                e.value = doubleValue;
                return doubleValue;

            case "SW":
            case "S.S":
                // Store 4-byte word
                cache.writeWord(e.address, (int)e.value);
                return null;

            case "SD":
            case "S.D":
                // Store 8-byte doubleword
                cache.writeDoubleWord(e.address, e.value);
                return null;

            default:
                throw new IllegalArgumentException("Unknown op: " + e.op);
        }
    }

    // NEW: Helper methods for integration
    public boolean isFull() {
        return buffer.size() >= maxSize;
    }

    public int size() {
        return buffer.size();
    }

    public List<Entry> getEntries() {
        return new ArrayList<>(buffer);
    }

    public void clear() {
        buffer.clear();
    }
     public int getMaxSize() {
        return maxSize;
    }
}