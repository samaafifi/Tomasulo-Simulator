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

    private void checkAddr(int addr) {
        if (addr < 0 || addr >= mem.length) throw new IllegalArgumentException("Bad address " + addr);
    }
}
