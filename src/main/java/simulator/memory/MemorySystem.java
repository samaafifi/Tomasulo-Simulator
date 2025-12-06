package simulator.memory;

import java.util.*;

/**
 * Memory System - Integrates cache, memory, and load/store buffer
 * 
 * Responsibilities:
 * - Manages byte-addressable memory
 * - Handles cache simulation
 * - Coordinates load/store operations
 * - Returns results for CDB broadcast
 */
public class MemorySystem {
    private final ByteMemory memory;
    private final CacheSimulator cache;
    private final LoadStoreBuffer lsb;

    private final int loadLatency;   // base latency for load operations
    private final int storeLatency;  // base latency for store operations

    /**
     * Container for completed memory operations.
     * Used for broadcasting results on CDB.
     */
    public static class CompletedOp {
        public int entryId;
        public String op;
        public String destReg;     // for loads
        public long value;         // loaded value
        public boolean isLoad;
        public String stationName; // Load/Store station name (L1, L2, S1, S2)

        @Override
        public String toString() {
            return String.format("%s -> %s = %d (from %s)", op, destReg, value, stationName);
        }
    }

    // Default constructor - DEPRECATED: Should not be used
    // NO HARDCODED VALUES - This constructor violates "NO HARDCODE" requirement
    // All MemorySystem instances should be created with explicit user-configured parameters
    // This constructor is kept only for backward compatibility but should not be called
    @Deprecated
    public MemorySystem() {
        // WARNING: This constructor uses hardcoded values - violates "NO HARDCODE" requirement
        // Use the parameterized constructor with user-configured values instead
        this(1024 * 1024,      // 1MB memory - INTENTIONALLY FIXED
             64 * 1024,        // 64KB cache - HARDCODED (should be user-configured)
             64,               // 64 byte blocks - HARDCODED (should be user-configured)
             1,                // 1 cycle hit latency - HARDCODED (should be user-configured)
             10,               // 10 cycle miss penalty - HARDCODED (should be user-configured)
             2,                // 2 cycle load latency - HARDCODED (should be user-configured)
             2,                // 2 cycle store latency - HARDCODED (should be user-configured)
             8);               // 8 entry LSB - HARDCODED (should be user-configured)
    }

    public MemorySystem(int memorySizeBytes,
                        int cacheSizeBytes,
                        int blockSize,
                        int cacheHitLatency,
                        int cacheMissPenalty,
                        int loadLatency,
                        int storeLatency,
                        int lsbSize) {
        this.memory = new ByteMemory(memorySizeBytes);
        this.cache = new CacheSimulator(memory, cacheSizeBytes, blockSize, 
                                       cacheHitLatency, cacheMissPenalty);
        this.lsb = new LoadStoreBuffer(cache, lsbSize);
        this.loadLatency = loadLatency;
        this.storeLatency = storeLatency;
    }

    // ============= LOAD OPERATIONS ============= //

    /**
     * Issue a LOAD instruction (LW, LD, L.S, L.D).
     * Calculates effective address and determines latency based on cache.
     * 
     * @param op Operation type
     * @param baseValue Base register value
     * @param offset Immediate offset
     * @param destReg Destination register name
     * @param stationName Load station name (L1, L2, etc.)
     * @return LSB entry ID
     */
    public int issueLoad(String op, int baseValue, int offset, String destReg, String stationName) {
        int effectiveAddress = baseValue + offset;
        
        // Determine total latency based on cache hit/miss
        int totalCycles = cache.isHit(effectiveAddress)
            ? loadLatency + cache.getHitLatency()
            : loadLatency + cache.getMissPenalty();
        
        LoadStoreBuffer.Entry entry = lsb.addLoad(op, effectiveAddress, totalCycles, destReg, stationName);
        return entry.id;
    }

    // ============= STORE OPERATIONS ============= //

    /**
     * Issue a STORE instruction (SW, SD, S.S, S.D).
     * 
     * @param op Operation type
     * @param baseValue Base register value
     * @param offset Immediate offset
     * @param value Value to store
     * @return LSB entry ID
     */
    public int issueStore(String op, int baseValue, int offset, long value) {
        int effectiveAddress = baseValue + offset;
        
        // Determine total latency based on cache hit/miss
        int totalCycles = cache.isHit(effectiveAddress)
            ? storeLatency + cache.getHitLatency()
            : storeLatency + cache.getMissPenalty();
        
        LoadStoreBuffer.Entry entry = lsb.addStore(op, effectiveAddress, value, totalCycles);
        return entry.id;
    }

    // ============= CYCLE EXECUTION ============= //

    /**
     * Execute one cycle of the memory system.
     * - Advances LSB timers
     * - Commits completed operations
     * - Returns results for CDB broadcast
     * 
     * Called by SimulationEngine every cycle.
     * 
     * @return List of completed operations ready for CDB
     */
    public List<CompletedOp> cycle() {
        List<LoadStoreBuffer.Entry> completed = lsb.stepCycle();
        List<CompletedOp> results = new ArrayList<>();

        for (LoadStoreBuffer.Entry e : completed) {
            // Commit to cache
            Long loadedValue = lsb.commit(e);
            
            if (e.isLoad) {
                // Package result for CDB broadcast
                CompletedOp op = new CompletedOp();
                op.entryId = e.id;
                op.op = e.op;
                op.destReg = e.destReg;
                op.value = loadedValue;
                op.isLoad = true;
                op.stationName = e.stationName;  // Pass through station name
                results.add(op);
            }
            // Stores don't broadcast on CDB
        }

        return results;
    }

    // ============= INITIALIZATION ============= //

    /**
     * Initialize memory with test data.
     * Used for pre-loading register values and test cases.
     */
    public void initializeMemory(int address, int value) {
        memory.writeWord(address, value);
    }

    /**
     * Bulk load multiple address-value pairs.
     */
    public void bulkLoad(Map<Integer, Integer> addressValuePairs) {
        for (Map.Entry<Integer, Integer> entry : addressValuePairs.entrySet()) {
            memory.writeWord(entry.getKey(), entry.getValue());
        }
    }

    /**
     * Initialize memory from array (for test cases).
     * @param startAddress Starting address
     * @param values Array of values to load
     */
    public void loadArray(int startAddress, int[] values) {
        for (int i = 0; i < values.length; i++) {
            memory.writeWord(startAddress + (i * 4), values[i]);
        }
    }

    // ============= DIRECT ACCESS (for debugging/testing) ============= //

    /** Direct load bypassing cache (for initialization) */
    public int directLoad(int address) {
        return memory.readWord(address);
    }

    /** Direct store bypassing cache (for initialization) */
    public void directStore(int address, int value) {
        memory.writeWord(address, value);
    }

    /** Direct load of byte */
    public byte directLoadByte(int address) {
        return memory.readByte(address);
    }

    // ============= QUERY METHODS ============= //

    /** Check if LSB is full (structural hazard check) */
    public boolean isLSBFull() {
        return lsb.isFull();
    }

    /** Get number of active LSB entries */
    public int getLSBSize() {
        return lsb.size();
    }

    /** Get LSB entries for GUI display */
    public List<LoadStoreBuffer.Entry> getLSBEntries() {
        return lsb.getEntries();
    }

    /** Get cache statistics */
    public long getCacheHits() {
        return cache.getHitCount();
    }

    public long getCacheMisses() {
        return cache.getMissCount();
    }

    public double getCacheMissRate() {
        return cache.getMissRate();
    }

    // ============= GETTERS ============= //

    public ByteMemory getMemory() {
        return memory;
    }

    public CacheSimulator getCacheSimulator() {
        return cache;
    }

    public LoadStoreBuffer getLoadStoreBuffer() {
        return lsb;
    }

    /** Get load latency (base latency for load operations) */
    public int getLoadLatency() {
        return loadLatency;
    }

    /** Get store latency (base latency for store operations) */
    public int getStoreLatency() {
        return storeLatency;
    }

    /** Calculate total latency for a load operation at the given address */
    public int calculateLoadLatency(int address) {
        return cache.isHit(address)
            ? loadLatency + cache.getHitLatency()
            : loadLatency + cache.getMissPenalty();
    }

    /** Calculate total latency for a store operation at the given address */
    public int calculateStoreLatency(int address) {
        return cache.isHit(address)
            ? storeLatency + cache.getHitLatency()
            : storeLatency + cache.getMissPenalty();
    }

    /** Reset memory system (for new simulation) */
    public void reset() {
        lsb.clear();
        cache.resetStats();
        // Memory contents preserved unless explicitly cleared
    }

    // ============= DOUBLE PRECISION OPERATIONS ============= //

    /**
     * Write a double precision floating point value to memory (8 bytes)
     */
    public void writeDouble(int address, double value) {
        long longValue = Double.doubleToLongBits(value);
        // Write as two 4-byte words (little-endian)
        memory.writeWord(address, (int)(longValue & 0xFFFFFFFFL));
        memory.writeWord(address + 4, (int)((longValue >> 32) & 0xFFFFFFFFL));
    }

    /**
     * Read a double precision floating point value from memory (8 bytes)
     */
    public double readDouble(int address) {
        int low = memory.readWord(address);
        int high = memory.readWord(address + 4);
        long longValue = ((long)high << 32) | (low & 0xFFFFFFFFL);
        return Double.longBitsToDouble(longValue);
    }
}