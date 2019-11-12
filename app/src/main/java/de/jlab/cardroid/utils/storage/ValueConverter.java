package de.jlab.cardroid.utils.storage;

import java.util.HashMap;

import androidx.annotation.NonNull;
import androidx.arch.core.util.Function;

public final class ValueConverter<I, O, P extends Function<I, Boolean>, F extends Function<I, O>> {
    private HashMap<P, F> conversionMap = new HashMap<>();

    private ValueConverter() {}

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

    @NonNull
    public static ValueConverter<Object, String, Function<Object, Boolean>, Function<Object, String>> serializer() {
        ValueConverter<Object, String, Function<Object, Boolean>, Function<Object, String>> serializer = new ValueConverter<>();
        serializer.addConversion((o) -> o == null, (o) -> "null");
        serializer.addConversion((o) -> o instanceof Boolean, (o) -> Boolean.toString((Boolean) o));
        serializer.addConversion((o) -> o instanceof Integer, (o) -> Integer.toString((Integer) o));
        serializer.addConversion((o) -> o instanceof Long, (o) -> Long.toString((Long) o) + "L");
        serializer.addConversion((o) -> o instanceof Double, (o) -> Long.toString((Long) o) + "D");
        serializer.addConversion((o) -> o instanceof Float, (o) -> Long.toString((Long) o) + "F");
        serializer.addConversion((o) -> o instanceof String, (o) -> "\"" + o.toString() + "\"");
        return serializer;
    }

    @NonNull
    public static ValueConverter<String, Object, Function<String, Boolean>, Function<String, Object>> deserializer() {
        ValueConverter<String, Object, Function<String, Boolean>, Function<String, Object>> deserializer = new ValueConverter<>();
        deserializer.addConversion((s) -> s.equals("null"), (s)->null);
        deserializer.addConversion((s) -> s.equals("true") || s.equals("false"), Boolean::valueOf);
        deserializer.addConversion((s) -> s.matches("[-+]?\\d+"), Integer::valueOf);
        deserializer.addConversion((s) -> s.matches("[-+]?\\d+L"), Long::valueOf);
        deserializer.addConversion((s) -> s.matches("[-+]?\\d*\\.\\d+D"), Double::valueOf);
        deserializer.addConversion((s) -> s.matches("[-+]?\\d*\\.\\d+F"), Float::valueOf);
        deserializer.addConversion((s) -> s.matches("\".*\""), (s) -> s.substring(1, s.length()-1));
        return deserializer;
    }

}
