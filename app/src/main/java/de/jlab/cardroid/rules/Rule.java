package de.jlab.cardroid.rules;

import android.content.Context;

import java.util.ArrayList;
import java.util.Collections;

import androidx.annotation.NonNull;

public final class Rule {

    private Trigger trigger;
    private ArrayList<Action> actions = new ArrayList<>();

    public Rule(Trigger trigger, Action... actions) {
        this.trigger = trigger;
        Collections.addAll(this.actions, actions);
    }

    public Trigger getTrigger() {
        return this.trigger;
    }

    public void trigger(@NonNull Event event, @NonNull Context context) {
        if (this.trigger.triggers(event)) {
            for (Action action : this.actions) {
                action.execute(context);
            }
        }
    }

}
