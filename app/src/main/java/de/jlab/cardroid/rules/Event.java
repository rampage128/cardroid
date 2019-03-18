package de.jlab.cardroid.rules;

import android.content.Context;

import androidx.annotation.Nullable;
import de.jlab.cardroid.R;

public final class Event {

    private int identifier;

    public Event(int identifier) {
        this.identifier = identifier;
    }

    public int getIdentifier() {
        return this.identifier;
    }

    /**
     * @param identifier
     * @param context
     * @return
     */
    public static String getLocalizedNameFromIdentifier(int identifier, Context context) {
        int resId = context.getResources().getIdentifier("event_" + identifier, "string", context.getPackageName());
        if (resId > 0) {
            return context.getResources().getString(resId);
        }
        return context.getResources().getString(R.string.event_unknown, identifier);
    }

    public boolean equals(@Nullable Event event) {
        return event != null && this.identifier == event.identifier;
    }
}
