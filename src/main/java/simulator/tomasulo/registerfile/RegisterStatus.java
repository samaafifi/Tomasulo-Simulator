package simulator.tomasulo.registerfile;


/**
 * Immutable snapshot of a register's status at a specific cycle.
 * Used for GUI display and logging without exposing mutable Register objects.
 */
public class RegisterStatus {
    private final String registerName;
    private final boolean busy;
    private final String Qi;
    private final double value;
    private final int lastUpdatedCycle;
    
    public RegisterStatus(String registerName, boolean busy, String Qi, 
                         double value, int lastUpdatedCycle) {
        this.registerName = registerName;
        this.busy = busy;
        this.Qi = Qi;
        this.value = value;
        this.lastUpdatedCycle = lastUpdatedCycle;
    }
    
    /**
     * Creates a RegisterStatus from a Register object.
     */
    public static RegisterStatus fromRegister(Register register, int currentCycle) {
        return new RegisterStatus(
            register.getName(),
            register.isBusy(),
            register.getQi(),
            register.getValue(),
            currentCycle
        );
    }
    
    // Getters
    public String getRegisterName() {
        return registerName;
    }
    
    public boolean isBusy() {
        return busy;
    }
    
    public String getQi() {
        return Qi;
    }
    
    public double getValue() {
        return value;
    }
    
    public int getLastUpdatedCycle() {
        return lastUpdatedCycle;
    }
    
    @Override
    public String toString() {
        return String.format("RegisterStatus{name='%s', value=%.2f, Qi=%s, busy=%s, cycle=%d}",
            registerName, value, Qi, busy, lastUpdatedCycle);
    }
}
