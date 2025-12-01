package simulator.memory;

/**
 * Cache Simulator - Direct-Mapped Cache with Write-Back Policy
 * 
 * Address Mapping:
 * ┌─────────────────────────────────────┐
 * │  TAG  │  INDEX  │  OFFSET  │
 * └─────────────────────────────────────┘
 * 
 * - OFFSET: log2(blockSize) bits - byte position within block
 * - INDEX: log2(numLines) bits - which cache line
 * - TAG: remaining bits - stored with block for validation
 * 
 * Example (32-bit address, 16-byte blocks, 64 cache lines):
 * - OFFSET: 4 bits (0-15)
 * - INDEX: 6 bits (0-63)
 * - TAG: 22 bits
 */
public class CacheSimulator {
    private final CacheBlock[] lines;   // direct-mapped cache lines
    private final int blockSize;
    private final int numLines;
    private final ByteMemory memory;
    private final int hitLatency;
    private final int missPenalty;

    // statistics
    private long hitCount = 0;
    private long missCount = 0;

    public CacheSimulator(ByteMemory memory,
                          int cacheSizeBytes,
                          int blockSize,
                          int hitLatency,
                          int missPenalty) {
        this.memory = memory;
        this.blockSize = blockSize;

        if (cacheSizeBytes % blockSize != 0)
            throw new IllegalArgumentException("Cache size must be a multiple of block size.");

        this.numLines = cacheSizeBytes / blockSize;
        this.lines = new CacheBlock[numLines];
        for (int i = 0; i < numLines; i++) {
            lines[i] = new CacheBlock(blockSize);
        }

        this.hitLatency = hitLatency;
        this.missPenalty = missPenalty;
    }

    /** 
     * Check if block containing address is in cache (valid + matching tag).
     * Used for determining latency before execution.
     */
    public boolean isHit(int address) {
        int index = getIndex(address);
        long tag = getTag(address);
        CacheBlock block = lines[index];
        return block.isValid() && block.getTag() == tag;
    }
    /** True if the given address belongs to the block stored at this index */
private boolean blockContainsAddress(int address, int index, int tag) {
    long blockNum = tag * (long) numLines + index;
    long blockStart = blockNum * (long) blockSize;
    return address >= blockStart && address < blockStart + blockSize;
}

    /** 
     * Read a 4-byte WORD from cache (LW, L.S).
     * Handles cache miss by fetching block.
     */
    public int readWord(int address) {
        int index = getIndex(address);
        long tag = getTag(address);
        int offset = getOffset(address);

        CacheBlock block = lines[index];
        if (block.isValid() && block.getTag() == tag) {
            hitCount++;
            byte[] bytes = block.readBytes(offset, 4);
            return bytesToInt(bytes);
        }

        missCount++;
        // Miss — fetch block (evicts old if dirty)
        fetchBlock(address, index, tag);

        CacheBlock loaded = lines[index];
        byte[] bytes = loaded.readBytes(offset, 4);
        return bytesToInt(bytes);
    }

    /** 
     * Read an 8-byte DOUBLEWORD from cache (LD, L.D).
     * Reads two consecutive 4-byte words.
     */
    public long readDoubleWord(int address) {
        int word1 = readWord(address);      // High word
        int word2 = readWord(address + 4);  // Low word
        return ((long)word1 << 32) | (word2 & 0xFFFFFFFFL);
    }

    /** 
     * Write a 4-byte WORD to cache (SW, S.S).
     * Marks block as dirty.
     */
    public void writeWord(int address, int value) {
        int index = getIndex(address);
        long tag = getTag(address);
        int offset = getOffset(address);

        CacheBlock block = lines[index];
        if (block.isValid() && block.getTag() == tag) {
            hitCount++;
            byte[] bytes = intToBytes(value);
            block.writeBytes(offset, bytes);
            block.setDirty(true);
            return;
        }

        missCount++;
        // Miss — fetch block, then write
        fetchBlock(address, index, tag);

        CacheBlock loaded = lines[index];
        byte[] bytes = intToBytes(value);
        loaded.writeBytes(offset, bytes);
        loaded.setDirty(true);
    }

    /** 
     * Write an 8-byte DOUBLEWORD to cache (SD, S.D).
     * Writes two consecutive 4-byte words.
     */
    public void writeDoubleWord(int address, long value) {
        int word1 = (int)(value >> 32);  // High word
        int word2 = (int)value;          // Low word
        writeWord(address, word1);
        writeWord(address + 4, word2);
    }

    /**
     * Fetch a block from memory into cache.
     * Handles write-back of dirty evicted blocks.
     */
    private void fetchBlock(int address, int index, long newTag) {
        CacheBlock line = lines[index];

        // Write-back: Evict old block if dirty
        if (line.isValid() && line.isDirty()) {
            long oldTag = line.getTag();
            long oldBlockNum = oldTag * (long) numLines + (long) index;
            int oldBlockStart = (int)(oldBlockNum * (long) blockSize);
            memory.writeBytes(oldBlockStart, line.readBytes(0, blockSize));
        }

        // Load new block from memory
        int blockStart = blockStartAddress(address);
        byte[] blockData = memory.readBytes(blockStart, blockSize);

        line.loadBlock(blockData, newTag);
    }

    // ============ ADDRESS CALCULATION ============ //
    
    /** Offset within block (byte position) */
    private int getOffset(int address) {
        return address % blockSize;
    }

    /** Cache line index (which line in cache) */
    private int getIndex(int address) {
        return (address / blockSize) % numLines;
    }

    /** Tag (identifies which memory block) */
    private long getTag(int address) {
        return (address / blockSize) / numLines;
    }

    /** Starting address of the block containing this address */
    private int blockStartAddress(int address) {
        return (address / blockSize) * blockSize;
    }

    // ============ CONVERSION UTILITIES ============ //
    
    /** Convert 4 bytes to int (big-endian) */
    private static int bytesToInt(byte[] b) {
        return ((b[0] & 0xFF) << 24) |
               ((b[1] & 0xFF) << 16) |
               ((b[2] & 0xFF) << 8) |
               (b[3] & 0xFF);
    }

    /** Convert int to 4 bytes (big-endian) */
    private static byte[] intToBytes(int v) {
        return new byte[] {
            (byte)(v >> 24),
            (byte)(v >> 16),
            (byte)(v >> 8),
            (byte)(v)
        };
    }

    // ============ STATISTICS & GETTERS ============ //
    
    public long getHitCount() {
        return hitCount;
    }

    public long getMissCount() {
        return missCount;
    }

    public double getMissRate() {
        long total = hitCount + missCount;
        if (total == 0) return 0.0;
        return (double) missCount / total;
    }

    public int getHitLatency() {
        return hitLatency;
    }

    public int getMissPenalty() {
        return missPenalty;
    }

    public int getBlockSize() {
        return blockSize;
    }

    public int getNumLines() {
        return numLines;
    }

    /** For GUI display: get cache line info */
    public CacheBlock getLine(int index) {
        return lines[index];
    }

    /** Reset statistics */
    public void resetStats() {
        hitCount = 0;
        missCount = 0;
    }
}