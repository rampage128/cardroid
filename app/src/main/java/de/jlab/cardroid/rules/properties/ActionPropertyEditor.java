package de.jlab.cardroid.rules.properties;

import android.content.Context;

public interface ActionPropertyEditor {

    void show(Property currentValue, Context context, PropertyListener listener);
    PropertyValue getDefaultValue();

    String getPropertyLabel(Property<?> property, Context context);

    /*
    public Class<T> getType() {
        ParameterizedType parameterizedType = (ParameterizedType)getClass().getGenericSuperclass();
        Objects.requireNonNull(parameterizedType, "ActionPropertyEditor must explicitly define its generic type");
        @SuppressWarnings("unchecked")
        Class<T> type = (Class<T>) parameterizedType.getActualTypeArguments()[0];

        return type;
    }
    */

    interface PropertyListener {
        void onItemSelected(PropertyValue item);
    }

}
