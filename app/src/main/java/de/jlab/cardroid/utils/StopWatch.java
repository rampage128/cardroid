package de.jlab.cardroid.utils;

import androidx.annotation.NonNull;

public final class StopWatch {

    private static final long NANOS_TO_MILLIS = 1000000;

    public enum Unit {
        NANOS(1),
        MILLIS(NANOS_TO_MILLIS),
        SECONDS(NANOS_TO_MILLIS * 1000);

        private long conversion;

        Unit(long conversion) {
            this.conversion = conversion;
        }

        public long convert(long nanos) {
            return nanos / this.conversion;
        }
    }

    private long start = 0;

    public void start() {
        this.start = System.nanoTime();
    }

    public long take(@NonNull Unit unit) {
        return unit.convert(System.nanoTime() - this.start);
    }

}
