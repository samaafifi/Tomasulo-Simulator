package simulator.memory;


public class ByteMemory {
     private final byte[] mem;

    public ByteMemory(int sizeBytes) {
        this.mem = new byte[sizeBytes];
    }

    public void writeByte(int addr, byte value) {
        checkAddr(addr);
        mem[addr] = value;
    }

    public byte readByte(int addr) {
        checkAddr(addr);
        return mem[addr];
    }

    public void writeBytes(int addr, byte[] src) {
        checkAddr(addr + src.length - 1);
        System.arraycopy(src, 0, mem, addr, src.length);
    }

    public byte[] readBytes(int addr, int len) {
        checkAddr(addr + len - 1);
        byte[] out = new byte[len];
        System.arraycopy(mem, addr, out, 0, len);
        return out;
    }

    public int readWord(int addr) {
    byte[] b = readBytes(addr, 4);
    return ((b[0] & 0xFF) << 24) |
           ((b[1] & 0xFF) << 16) |
           ((b[2] & 0xFF) << 8) |
           (b[3] & 0xFF);
}

public void writeWord(int addr, int value) {
    byte[] b = new byte[4];
    b[0] = (byte)(value >> 24);
    b[1] = (byte)(value >> 16);
    b[2] = (byte)(value >> 8);
    b[3] = (byte)value;
    writeBytes(addr, b);
}


    private void checkAddr(int addr) {
        if (addr < 0 || addr >= mem.length) throw new IllegalArgumentException("Bad address " + addr);
    }
}
