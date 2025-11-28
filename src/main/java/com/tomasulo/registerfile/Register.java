package com.tomasulo.registerfile;

/**
 * Represents a register in the Tomasulo algorithm register file.
 * Each register has a name, type, value, and tracks pending writes via Qi.
 */
public class Register {
    private final String name;
    private final RegisterType type;
    private double value;
    private String Qi;
    private boolean busy;

    /**
     * Constructs a new Register with the specified name and type.
     * Initializes value to 0.0, Qi to null, and busy to false.
     *
     * @param name the register name (e.g., "F0", "R1")
     * @param type the register type (INTEGER or FLOATING_POINT)
     */
    public Register(String name, RegisterType type) {
        this.name = name;
        this.type = type;
        this.value = 0.0;
        this.Qi = null;
        this.busy = false;
    }

    /**
     * Gets the register name.
     *
     * @return the register name
     */
    public String getName() {
        return name;
    }

    /**
     * Gets the register type.
     *
     * @return the register type
     */
    public RegisterType getType() {
        return type;
    }

    /**
     * Gets the current register value.
     *
     * @return the register value
     */
    public double getValue() {
        return value;
    }

    /**
     * Gets the reservation station tag for pending writes.
     *
     * @return the reservation station tag, or null if no pending write
     */
    public String getQi() {
        return Qi;
    }

    /**
     * Checks if the register is busy (has a pending write).
     *
     * @return true if busy, false otherwise
     */
    public boolean isBusy() {
        return busy;
    }

    /**
     * Sets the register value and clears any pending write.
     * This method sets the value, clears Qi to null, and sets busy to false.
     *
     * @param value the new register value
     */
    public void setValue(double value) {
        this.value = value;
        this.Qi = null;
        this.busy = false;
    }

    /**
     * Sets the reservation station tag for a pending write.
     * This method sets Qi to the station tag and marks the register as busy.
     *
     * @param stationTag the reservation station tag
     */
    public void setQi(String stationTag) {
        this.Qi = stationTag;
        this.busy = true;
    }

    /**
     * Clears the pending write reservation station tag.
     * This method sets Qi to null and marks the register as not busy.
     */
    public void clearQi() {
        this.Qi = null;
        this.busy = false;
    }

    /**
     * Checks if the register is ready (not busy).
     *
     * @return true if ready (not busy), false otherwise
     */
    public boolean isReady() {
        return !busy;
    }

    /**
     * Returns a string representation of the register for debugging.
     *
     * @return a formatted string with all register fields
     */
    @Override
    public String toString() {
        return String.format("Register{name='%s', type=%s, value=%.2f, Qi=%s, busy=%s}",
                name, type, value, Qi, busy);
    }
}
