package de.jlab.cardroid.rules.properties;

import android.content.Context;

import java.util.Objects;

public class AppSelectionPropertyEditor implements ActionPropertyEditor {

    @Override
    public void show(Property currentValue, Context context, PropertyListener listener) {
        // TODO implement app selector
    }

    @Override
    public PropertyValue getDefaultValue() {
        return null;
    }

    @Override
    public String getPropertyLabel(Property<?> property, Context context) {
        return Objects.toString(property.getValue());
    }
}
