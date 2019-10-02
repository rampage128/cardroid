package de.jlab.cardroid.errors;

public final class Error {
    private int errorNumber;
    private long lastOccurence;
    private int count;

    public Error(int errorNumber) {
        this.errorNumber = errorNumber;
    }

    public void count(int errorNumber) {
        if (this.errorNumber == errorNumber) {
            this.lastOccurence = System.currentTimeMillis();
            this.count++;
        }
    }

    public long getLastOccurence() {
        return lastOccurence;
    }

    public int getCount() {
        return this.count;
    }

    public String getErrorCode() {
        return String.format("%02X", this.errorNumber);
    }
}
