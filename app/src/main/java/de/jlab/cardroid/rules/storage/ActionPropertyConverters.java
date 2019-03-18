package de.jlab.cardroid.rules.storage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import androidx.arch.core.util.Function;
import androidx.room.TypeConverter;
import de.jlab.cardroid.rules.properties.Property;

public class ActionPropertyConverters {


    private static final ValueConverter<String, Object, Function<String, Boolean>, Function<String, Object>> DESERIALIZER = new ValueConverter<>();
    static {
        DESERIALIZER.addConversion((s) -> s.equals("null"), (s)->null);
        DESERIALIZER.addConversion((s) -> s.equals("true") || s.equals("false"), Boolean::valueOf);
        DESERIALIZER.addConversion((s) -> s.matches("[-+]?\\d+"), Integer::valueOf);
        DESERIALIZER.addConversion((s) -> s.matches("[-+]?\\d+L"), Long::valueOf);
        DESERIALIZER.addConversion((s) -> s.matches("[-+]?\\d*\\.\\d+D"), Double::valueOf);
        DESERIALIZER.addConversion((s) -> s.matches("[-+]?\\d*\\.\\d+F"), Float::valueOf);
        DESERIALIZER.addConversion((s) -> s.matches("\".*\""), (s) -> s.substring(1, s.length()-1));
    }

    private static final ValueConverter<Object, String, Function<Object, Boolean>, Function<Object, String>> SERIALIZER = new ValueConverter<>();
    static {
        SERIALIZER.addConversion((o) -> o == null, (o) -> "null");
        SERIALIZER.addConversion((o) -> o instanceof Boolean, (o) -> Boolean.toString((Boolean) o));
        SERIALIZER.addConversion((o) -> o instanceof Integer, (o) -> Integer.toString((Integer) o));
        SERIALIZER.addConversion((o) -> o instanceof Long, (o) -> Long.toString((Long) o) + "L");
        SERIALIZER.addConversion((o) -> o instanceof Double, (o) -> Long.toString((Long) o) + "D");
        SERIALIZER.addConversion((o) -> o instanceof Float, (o) -> Long.toString((Long) o) + "F");
        SERIALIZER.addConversion((o) -> o instanceof String, (o) -> "\"" + o.toString() + "\"");
    }

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

    private static class ValueConverter<I, O, P extends Function<I, Boolean>, F extends Function<I, O>> {

        private HashMap<P, F> conversionMap = new HashMap<>();

        public void addConversion(P predicate, F function) {
            this.conversionMap.put(predicate, function);
        }

        public O convert(I input) {
            for (P predicate : this.conversionMap.keySet()) {
                F function = this.conversionMap.get(predicate);
                assert function != null;
                if (predicate.apply(input)) {
                    return function.apply(input);
                }
            }
            String className = input != null ? input.getClass().getSimpleName() : "NullPointer";
            throw new UnsupportedOperationException("Cannot convert " + className + " \"" + input + "\"!");
        }

    }

}
