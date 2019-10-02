package de.jlab.cardroid.devices.serial.gps;

import java.io.ByteArrayOutputStream;
import java.util.Calendar;
import java.util.StringJoiner;

import androidx.annotation.NonNull;
import de.jlab.cardroid.devices.serial.SerialPacket;

public final class GpsSerialPacket implements SerialPacket {

    private String[] tokens;

    public GpsSerialPacket(String dataLine) {
        this.tokens = dataLine.substring(1).split(",");
    }

    @Override
    public void serialize(@NonNull ByteArrayOutputStream stream) {
        throw new UnsupportedOperationException("GPS serial packets are read only!");
    }

    private Calendar calendar = Calendar.getInstance();

    @NonNull
    public String readSentence() {
        StringBuilder sentenceBuilder = new StringBuilder();
        sentenceBuilder.append("$");
        for (String token : this.tokens) {
            sentenceBuilder.append(token);
        }
        return sentenceBuilder.toString();
    }

    public String readSentenceType() {
        return this.tokens[0];
    }

    public int length() {
        return this.tokens.length;
    }

    public String readString(int tokenIndex) {
        return this.tokens[tokenIndex];
    }

    public double readDouble(int tokenIndex, double defaultValue) {
        try {
            return Double.parseDouble(this.tokens[tokenIndex]);
        }
        catch (NumberFormatException ex) { /* Intentionally left blank */ }
        return defaultValue;
    }

    protected float readFloat(int tokenIndex, float defaultValue) {
        try {
            return Float.parseFloat(this.tokens[tokenIndex]);
        }
        catch (NumberFormatException ex) { /* Intentionally left blank */ }
        return defaultValue;
    }

    protected int readInt(int tokenIndex, int defaultValue) {
        try {
            return Integer.parseInt(this.tokens[tokenIndex]);
        }
        catch (NumberFormatException ex) { /* Intentionally left blank */ }
        return defaultValue;
    }

    protected long readTime(int tokenIndex) {
        try {
            String token = this.tokens[tokenIndex];
            this.calendar.setTimeInMillis(System.currentTimeMillis());
            calendar.set(Calendar.HOUR_OF_DAY, Integer.parseInt(token.substring(0, 2)));
            calendar.set(Calendar.MINUTE, Integer.parseInt(token.substring(2, 4)));
            calendar.set(Calendar.SECOND, Integer.parseInt(token.substring(4, 6)));
            calendar.set(Calendar.MILLISECOND, Integer.parseInt(token.substring(7)));
            return calendar.getTimeInMillis();
        }
        catch (NumberFormatException | StringIndexOutOfBoundsException ex) { /* Intentionally left blank */ }
        return System.currentTimeMillis();
    }

    protected double readLatitude(int latTokenIndex, int nsTokenIndex) {
        String lat = this.tokens[latTokenIndex];
        String ns = this.tokens[nsTokenIndex];
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

    protected double readLongitude(int lonTokenIndex, int weTokenIndex) {
        String lon = this.tokens[lonTokenIndex];
        String we = this.tokens[weTokenIndex];
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

}
