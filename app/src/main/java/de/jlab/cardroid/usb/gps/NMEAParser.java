package de.jlab.cardroid.usb.gps;

import java.util.Calendar;
import java.util.HashMap;

public abstract class NMEAParser {

    public static final String PROPERTY_ACCURACY    = "accuracy";
    public static final String PROPERTY_ALTITUDE    = "alt";
    public static final String PROPERTY_DIRECTION   = "dir";
    public static final String PROPERTY_LATITUDE    = "lat";
    public static final String PROPERTY_LONGITUDE   = "long";
    public static final String PROPERTY_QUALITY     = "quality";
    public static final String PROPERTY_TIME        = "time";
    public static final String PROPERTY_VELOCITY    = "velocity";

    private static HashMap<String, NMEAParser> parsers = new HashMap<>();

    private HashMap<String, Object> tokenMap = new HashMap<>();

    private Calendar calendar = Calendar.getInstance();

    protected void parseDouble(String key, String value, double factor) {
        try {
            this.tokenMap.put(key, Double.parseDouble(value) * factor);
        }
        catch (NumberFormatException ex) { /* Intentionally left blank */ }
    }

    protected void parseFloat(String key, String value, float factor) {
        try {
            this.tokenMap.put(key, Float.parseFloat(value) * factor);
        }
        catch (NumberFormatException ex) { /* Intentionally left blank */ }
    }

    protected void parseInt(String key, String value) {
        try {
            this.tokenMap.put(key, Integer.parseInt(value));
        }
        catch (NumberFormatException ex) { /* Intentionally left blank */ }
    }

    protected void parseTime(String key, String value) {
        try {
            this.calendar.setTimeInMillis(System.currentTimeMillis());
            calendar.set(Calendar.HOUR_OF_DAY, Integer.parseInt(value.substring(0, 2)));
            calendar.set(Calendar.MINUTE, Integer.parseInt(value.substring(2, 4)));
            calendar.set(Calendar.SECOND, Integer.parseInt(value.substring(4, 6)));
            calendar.set(Calendar.MILLISECOND, Integer.parseInt(value.substring(7, value.length())));
            this.tokenMap.put(key, calendar.getTimeInMillis());
        }
        catch (NumberFormatException ex) { /* Intentionally left blank */ }
        catch (StringIndexOutOfBoundsException ex) { /* Intentionally left blank */ }
    }

    protected void parseLatitude(String key, String lat, String ns) {
        double d = Double.NaN;
        if (!lat.isEmpty()) {
            try {
                d = (Double.parseDouble(lat.substring(2)) / 60.0d) + Double.parseDouble(lat.substring(0, 2));
                if (ns.startsWith("S")) {
                    d = -d;
                }
            }
            catch (Exception e) { /* Intentionally left blank */ }
        }
        this.tokenMap.put(key, d);
    }

    protected void parseLongitude(String key, String lon, String we) {
        double d = Double.NaN;
        if (!lon.isEmpty()) {
            try {
                d = (Double.parseDouble(lon.substring(3)) / 60.0d) + Double.parseDouble(lon.substring(0, 3));
                if (we.startsWith("W")) {
                    d = -d;
                }
            }
            catch (Exception e) { /* Intentionally left blank */ }
        }
        this.tokenMap.put(key, d);
    }

    protected Object getToken(String key, Object defaultValue) {
        Object value = this.tokenMap.get(key);
        return value != null ? value : defaultValue;
    }

    abstract void parseSentence(String[] tokens);

    public static NMEAParser createFrom(String type) {
        NMEAParser parser = parsers.get(type);
        if (parser == null) {
            switch(type) {
                case "GPGLL":
                    parser = new GpgllParser();
                    break;
                case "GPGGA":
                    parser = new GpggaParser();
                    break;
                case "GPGSA":
                    parser = new GpgsaParser();
                    break;
                case "GPGGL":
                    parser = new GpgglParser();
                    break;
                case "GPRMC":
                    parser = new GprmcParser();
                    break;
                case "GPRMZ":
                    parser = new GprmzParser();
                    break;
            }
            parsers.put(type, parser);
        }

        return parser;
    }

    static class GpgllParser extends NMEAParser {
        @Override
        public void parseSentence(String[] tokens) {
            if (tokens.length < 5) {
                return;
            }
            parseLatitude(NMEAParser.PROPERTY_LATITUDE, tokens[1], tokens[2]);
            parseLongitude(NMEAParser.PROPERTY_LONGITUDE, tokens[3], tokens[4]);
        }
    }

    static class GpggaParser extends NMEAParser {
        @Override
        public void parseSentence(String[] tokens) {
            if (tokens.length < 10) {
                return;
            }
            parseTime(NMEAParser.PROPERTY_TIME, tokens[1]);
            parseLatitude(NMEAParser.PROPERTY_LATITUDE, tokens[2], tokens[3]);
            parseLongitude(NMEAParser.PROPERTY_LONGITUDE, tokens[4], tokens[5]);
            parseInt(NMEAParser.PROPERTY_QUALITY, tokens[6]);
            parseDouble(NMEAParser.PROPERTY_ALTITUDE, tokens[9], 1f);
        }
    }

    static class GpgsaParser extends NMEAParser {
        @Override
        public void parseSentence(String[] tokens) {
            if (tokens.length < 17) {
                return;
            }
            parseFloat(NMEAParser.PROPERTY_ACCURACY, tokens[16], 1f);
        }
    }

    static class GpgglParser extends NMEAParser {
        @Override
        public void parseSentence(String[] tokens) {
            if (tokens.length < 6) {
                return;
            }
            parseLatitude(NMEAParser.PROPERTY_LATITUDE, tokens[1], tokens[2]);
            parseLongitude(NMEAParser.PROPERTY_LONGITUDE, tokens[3], tokens[4]);
            parseTime(NMEAParser.PROPERTY_TIME, tokens[5]);
        }
    }

    static class GprmcParser extends NMEAParser {
        @Override
        void parseSentence(String[] tokens) {
            if (tokens.length < 9) {
                return;
            }
            parseTime(NMEAParser.PROPERTY_TIME, tokens[1]);
            parseLatitude(NMEAParser.PROPERTY_LATITUDE, tokens[3], tokens[4]);
            parseLongitude(NMEAParser.PROPERTY_LONGITUDE, tokens[5], tokens[6]);
            parseFloat(NMEAParser.PROPERTY_VELOCITY, tokens[7], 0.514444f);
            parseFloat(NMEAParser.PROPERTY_DIRECTION, tokens[8], 1f);
        }
    }

    static class GprmzParser extends NMEAParser {
        @Override
        void parseSentence(String[] tokens) {
            if (tokens.length < 2) {
                return;
            }
            parseDouble(NMEAParser.PROPERTY_ALTITUDE, tokens[1], 1f);
        }
    }

}
