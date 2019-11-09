package de.jlab.cardroid.overlay;

import androidx.annotation.NonNull;

public final class SeekArcEvent {

    public enum Type {
        TRACKING_STARTED,
        TRACKING_STOPPED,
        PROGRESS_CHANGED
    }

    private SeekArc seekArc;
    private Type type = Type.TRACKING_STARTED;
    private int progress = 0;
    private boolean isUserEvent = false;

    public SeekArcEvent(@NonNull SeekArc seekArc) {
        this.seekArc = seekArc;
    }

    public void update(int progress, boolean isUserEvent, @NonNull Type type) {
        this.progress = progress;
        this.isUserEvent = isUserEvent;
        this.type = type;
    }

    public int getProgress() {
        return this.progress;
    }

    @NonNull
    public SeekArc getSeekArc() {
        return this.seekArc;
    }

    public boolean isUserEvent() {
        return this.isUserEvent;
    }

    @NonNull
    public Type getType() {
        return this.type;
    }

}
