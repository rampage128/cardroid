package de.jlab.cardroid.usb.gps;

import android.location.Location;
import android.location.LocationManager;
import android.os.SystemClock;

public class GpsPosition extends Location {

    private boolean fixed = false;
    private int quality = 0;

    public GpsPosition() {
        super(LocationManager.GPS_PROVIDER);
    }

    public boolean update(NMEAParser parser) {
        boolean dirty = false;

        float f = (float)parser.getToken(NMEAParser.PROPERTY_ACCURACY, this.getAccuracy());
        if (f != this.getAccuracy()) {
            dirty = true;
            this.setAccuracy(f);
        }

        double d = (double)parser.getToken(NMEAParser.PROPERTY_ALTITUDE, this.getAltitude());
        if (d != this.getAltitude()) {
            dirty = true;
            this.setAltitude(d);
        }

        f = (float)parser.getToken(NMEAParser.PROPERTY_DIRECTION, this.getBearing());
        if (f != this.getBearing()) {
            dirty = true;
            this.setBearing(f);
        }

        d = (double)parser.getToken(NMEAParser.PROPERTY_LATITUDE, this.getLatitude());
        if (d != this.getLatitude()) {
            dirty = true;
            this.setLatitude(d);
        }

        d = (double)parser.getToken(NMEAParser.PROPERTY_LONGITUDE, this.getLongitude());
        if (d != this.getLongitude()) {
            dirty = true;
            this.setLongitude(d);
        }

        int i = (int)parser.getToken(NMEAParser.PROPERTY_QUALITY, this.quality);
        if (i != this.quality) {
            dirty = true;
            this.quality = i;
        }

        long l = (long)parser.getToken(NMEAParser.PROPERTY_TIME, this.getTime());
        if (l != this.getTime()) {
            dirty = true;
            this.setTime(l);
            this.setElapsedRealtimeNanos(SystemClock.elapsedRealtimeNanos());
        }

        f = (float)parser.getToken(NMEAParser.PROPERTY_VELOCITY, this.getSpeed());
        if (f != this.getSpeed()) {
            dirty = true;
            this.setSpeed(f);
        }

        this.fixed = quality > 0;

        return dirty;
    }

    public boolean isValid() {
        if (!hasAccuracy()) return false;
        if (getTime() == 0) return false;
        if (getElapsedRealtimeNanos() == 0) return false;
        return true;
    }

}
