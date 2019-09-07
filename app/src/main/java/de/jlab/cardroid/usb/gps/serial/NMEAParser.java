package de.jlab.cardroid.usb.gps.serial;

import android.text.TextUtils;

import java.util.Calendar;
import java.util.HashMap;

import de.jlab.cardroid.usb.gps.GpsPosition;

public abstract class NMEAParser {

    private static HashMap<String, NMEAParser> parsers = new HashMap<>();
    private Calendar calendar = Calendar.getInstance();

    protected double parseDouble(String value, double defaultValue) {
        try {
            return Double.parseDouble(value);
        }
        catch (NumberFormatException ex) { /* Intentionally left blank */ }
        return defaultValue;
    }

    protected float parseFloat(String value, float defaultValue) {
        try {
            return Float.parseFloat(value);
        }
        catch (NumberFormatException ex) { /* Intentionally left blank */ }
        return defaultValue;
    }

    protected int parseInt(String value, int defaultValue) {
        try {
            return Integer.parseInt(value);
        }
        catch (NumberFormatException ex) { /* Intentionally left blank */ }
        return defaultValue;
    }

    protected long parseTime(String value) {
        try {
            this.calendar.setTimeInMillis(System.currentTimeMillis());
            calendar.set(Calendar.HOUR_OF_DAY, Integer.parseInt(value.substring(0, 2)));
            calendar.set(Calendar.MINUTE, Integer.parseInt(value.substring(2, 4)));
            calendar.set(Calendar.SECOND, Integer.parseInt(value.substring(4, 6)));
            calendar.set(Calendar.MILLISECOND, Integer.parseInt(value.substring(7, value.length())));
            return calendar.getTimeInMillis();
        }
        catch (NumberFormatException ex) { /* Intentionally left blank */ }
        catch (StringIndexOutOfBoundsException ex) { /* Intentionally left blank */ }
        return System.currentTimeMillis();
    }

    protected double parseLatitude(String lat, String ns) {
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
        return d;
    }

    protected double parseLongitude(String lon, String we) {
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
        return d;
    }

    public abstract void parseSentence(String[] tokens, GpsPosition position);

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
                case "GPRMC":
                    parser = new GprmcParser();
                    break;
                case "GPGSV":
                    parser = new GpgsvParser();
                    break;
            }
            parsers.put(type, parser);
        }

        return parser;
    }

    static class GpgllParser extends NMEAParser {
        @Override
        public void parseSentence(String[] tokens, GpsPosition position) {
            if (tokens.length < 6) {
                return;
            }
            position.updateCoordinates(parseLatitude(tokens[1], tokens[2]), parseLongitude(tokens[3], tokens[4]));
            position.updateTime(parseTime(tokens[5]));
        }
    }

    static class GpggaParser extends NMEAParser {
        @Override
        public void parseSentence(String[] tokens, GpsPosition position) {
            if (tokens.length < 12) {
                return;
            }
            position.updateTime(parseTime(tokens[1]));
            position.updateCoordinates(parseLatitude(tokens[2], tokens[3]), parseLongitude(tokens[4], tokens[5]));
            //parseInt(NMEAParser.PROPERTY_QUALITY, tokens[6]);
            position.updateAltitude(parseDouble(tokens[11], 0d));
        }
    }

    static class GpgsaParser extends NMEAParser {
        @Override
        public void parseSentence(String[] tokens, GpsPosition position) {
            if (tokens.length < 18) {
                return;
            }
            position.updateAccuracy(
                    parseInt(tokens[2], GpsPosition.FIX_NONE),
                    parseFloat(tokens[15], GpsPosition.MAX_DOP),
                    parseFloat(tokens[16], GpsPosition.MAX_DOP),
                    parseFloat(tokens[17], GpsPosition.MAX_DOP)
            );
            // TODO update fix per satellite (3-14 = IDs of SVs used in position fix)
        }
    }

    static class GprmcParser extends NMEAParser {
        @Override
        public void parseSentence(String[] tokens, GpsPosition position) {
            if (tokens.length < 9) {
                return;
            }
            position.updateTime(parseTime(tokens[1]));
            position.updateCoordinates(parseLatitude(tokens[3], tokens[4]), parseLongitude(tokens[5], tokens[6]));
            position.updateMotion(parseFloat(tokens[7], 0), parseFloat(tokens[8], 0));
        }
    }

    static class GpgsvParser extends NMEAParser {
        @Override
        public void parseSentence(String[] tokens, GpsPosition position) {
            if (tokens.length < 8) {
                return;
            }

            for (int i = 1; i <= 4; i++) {
                if (tokens.length > 4 * i + 3) {
                    String snrValue = tokens[4 * i];
                    if (!TextUtils.isEmpty(snrValue)) {
                        position.updateSatellite(
                                parseInt(snrValue, 0),
                                parseFloat(tokens[4 * i + 1], 0),
                                parseFloat(tokens[4 * i + 2], 90),
                                parseInt(tokens[4 * i + 3], -1)
                        );
                    }
                }
            }
            if (parseInt(tokens[1], -1) == parseInt(tokens[2], -2)) {
                position.flushSatellites();
            }
        }
    }

}
