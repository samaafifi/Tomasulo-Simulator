package simulator.tomasulo.execute;

public class BroadcastRequest {
    private int rsId;
    private double result;
    private int destReg;
    private String instrType;
    private int readyCycle = 0; // Cycle when this broadcast is ready (default: immediate)
    
    public BroadcastRequest(int rsId, double result, int destReg, String instrType) {
        this.rsId = rsId;
        this.result = result;
        this.destReg = destReg;
        this.instrType = instrType;
        this.readyCycle = 0; // Default: ready immediately (for backward compatibility)
    }
    
    public void setReadyCycle(int cycle) {
        this.readyCycle = cycle;
    }
    
    public int getReadyCycle() {
        return readyCycle;
    }
    
    // Your code uses these
    public int getRsId() { return rsId; }
    public double getResult() { return result; }
    public int getDestReg() { return destReg; }
    public String getInstrType() { return instrType; }
    
    // But some files might expect these names
    public int getReservationStationId() { return rsId; }
    public int getDestRegister() { return destReg; }
    public String getInstructionType() { return instrType; }
    
    @Override
    public String toString() {
        return String.format("RS%d->R%d=%.2f(%s)", rsId, destReg, result, instrType);
    }
}