package de.jlab.cardroid.rules.properties;

import android.content.Context;

import java.util.Objects;

import androidx.annotation.NonNull;

public class PropertyValue<T> {
    private T value;
    private String name;

    public PropertyValue(T value, String name) {
        this.value = value;
        this.name = name;
    }

    public T getValue() {
        return this.value;
    }

    public String getName() {
        return this.name;
    }

    public String getLabel(Context context) {
        String key = this.name.replaceAll("([a-z])([A-Z])", "$1_$2").toLowerCase();
        int resId = context.getResources().getIdentifier(key, "string", context.getPackageName());
        if (resId > 0) {
            return context.getResources().getString(resId);
        }
        return key;
    }

    @NonNull
    @Override
    public String toString() {
        return this.value.toString();
    }

    public boolean equals(@NonNull PropertyValue otherValue) {
        return Objects.equals(otherValue.value, this.value);
    }
}
