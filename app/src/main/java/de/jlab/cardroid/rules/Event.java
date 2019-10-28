package de.jlab.cardroid.rules;

import android.content.Context;

import java.util.Objects;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import de.jlab.cardroid.R;
import de.jlab.cardroid.rules.storage.EventEntity;

public final class Event {

    private EventEntity descriptor;

    public Event(EventEntity descriptor) {
        this.descriptor = descriptor;
    }

    public EventEntity getDescriptor() {
        return this.descriptor;
    }

    public static String getLocalizedNameFromIdentifier(int identifier, @NonNull Context context) {
        int resId = context.getResources().getIdentifier("event_" + identifier, "string", context.getPackageName());
        if (resId > 0) {
            return context.getResources().getString(resId);
        }
        return context.getResources().getString(R.string.event_unknown, identifier);
    }

    public boolean equals(@Nullable Event other) {
        return other != null && Objects.equals(this.descriptor, other.descriptor);
    }
}
