package de.jlab.cardroid.rules.storage;

import java.util.ArrayList;
import java.util.List;

import androidx.arch.core.util.Function;
import androidx.room.TypeConverter;
import de.jlab.cardroid.rules.properties.Property;
import de.jlab.cardroid.utils.storage.ValueConverter;

public class ActionPropertyConverters {

    private static final ValueConverter<String, Object, Function<String, Boolean>, Function<String, Object>> DESERIALIZER = ValueConverter.deserializer();
    private static final ValueConverter<Object, String, Function<Object, Boolean>, Function<Object, String>> SERIALIZER = ValueConverter.serializer();

    @TypeConverter
    public static List<Property<?>> stringToProperties(String data) {
        List<Property<?>> properties = new ArrayList<>();

        String[] propertyStrings = data.split(",");

        for (String propertyString : propertyStrings) {
            if (propertyString.isEmpty()) {
                continue;
            }
            int separatorIndex = propertyString.indexOf('=');
            String key = propertyString.substring(0, separatorIndex);
            String value = propertyString.substring(separatorIndex+1);

            Property<Object> property = new Property<>(key, DESERIALIZER.convert(value));
            properties.add(property);
        }

        return properties;
    }

    @TypeConverter
    public static String propertiesToString(List<Property<?>> properties) {
        StringBuilder output = new StringBuilder();
        for (int i = 0; i < properties.size(); i++) {
            Property<?> property = properties.get(i);
            if (i > 0) {
                output.append(",");
            }
            output.append(property.getKey()).append("=").append(SERIALIZER.convert(property.getValue()));
        }

        return output.toString();
    }

}
