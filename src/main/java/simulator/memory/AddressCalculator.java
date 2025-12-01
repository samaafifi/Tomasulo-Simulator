package simulator.memory;

public class AddressCalculator {

    /** Compute effective address for LW/SW/LD/SD. */
    public static int computeEA(int base, int offset) {
        return base + offset;
    }

    /** Offset inside a block */
    public static int getOffset(int address, int blockSize) {
        return address % blockSize;
    }

    /** Cache index for direct-mapped cache */
    public static int getIndex(int address, int blockSize, int numLines) {
        int blockNum = address / blockSize;
        return blockNum % numLines;
    }

    /** Tag field */
    public static long getTag(int address, int blockSize, int numLines) {
        int blockNum = address / blockSize;
        return blockNum / numLines;
    }

    /** Starting address of a memory block */
    public static int blockStartAddress(int address, int blockSize) {
        return (address / blockSize) * blockSize;
    }
}
