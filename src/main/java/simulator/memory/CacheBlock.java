package simulator.memory;

public class CacheBlock {
    private boolean valid;
    private boolean dirty;
    private long tag;
    private final byte[] data;

    public CacheBlock(int blockSize) {
        this.data = new byte[blockSize];
        clear();
    }

    /** Resets the block */
    public void clear() {
        valid = false;
        dirty = false;
        tag = -1;
    }

    /** Checks if the block is valid */
    public boolean isValid() {
        return valid;
    }

    /** Checks if the block is dirty */
    public boolean isDirty() {
        return dirty;
    }

    /** Gets the tag */
    public long getTag() {
        return tag;
    }

    /** Sets the tag */
    public void setTag(long tag) {
        this.tag = tag;
    }

    /** Sets the valid bit */
    public void setValid(boolean valid) {
        this.valid = valid;
    }

    /** Sets the dirty bit */
    public void setDirty(boolean dirty) {
        this.dirty = dirty;
    }

    /** Loads the block data from memory */
    public void loadBlock(byte[] src, long tag) {
        System.arraycopy(src, 0, this.data, 0, this.data.length);
        this.tag = tag;
        this.valid = true;
        this.dirty = false;
    }

    /** Checks if a memory address belongs to this block */
    // public boolean containsAddress(int address, int blockSize) {
    //     long blockStart = tag * blockSize;
    //     return valid && address >= blockStart && address < blockStart + blockSize;
    // }

    /** Reads bytes from the block at a given offset */
    public byte[] readBytes(int offset, int length) {
        byte[] out = new byte[length];
        System.arraycopy(data, offset, out, 0, length);
        return out;
    }

    /** Writes bytes into the block at a given offset */
    public void writeBytes(int offset, byte[] src) {
        System.arraycopy(src, 0, data, offset, src.length);
        dirty = true;
    }

    /** Reads a single byte from the block */
    public byte readByte(int offset) {
        return data[offset];
    }

    /** Writes a single byte into the block */
    public void writeByte(int offset, byte value) {
        data[offset] = value;
        dirty = true;
    }
    
    /** Get block size for GUI display */
    public int getBlockSize() {
        return data.length;
    }
}
