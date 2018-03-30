package de.jlab.cardroid.usb.gps;

import android.location.Location;
import android.location.LocationManager;
import android.os.SystemClock;
import android.util.SparseArray;

import java.util.Objects;

public class GpsPosition {

    private static double NANOS_TO_MILLIS   = 1000000d;
    private static long MILLIS_TO_NANOS     = 1000000;

    public static float MAX_DOP  = 50f;

    public static int FIX_NONE  = 1;
    public static int FIX_2D    = 2;
    public static int FIX_3D    = 3;

    private Location location;
    private SparseArray<GpsSatellite> satelliteMap = new SparseArray<>();

    private float pDop = 50f;
    private float hDop = 50f;
    private float vDop = 50f;
    private int fix = 0;

    public GpsPosition() {
        this.location = new Location(LocationManager.GPS_PROVIDER);
    }

    public void updateAccuracy(int fix, float pDop, float hDop, float vDop) {
        this.fix = fix;
        this.pDop = pDop;
        this.hDop = hDop;
        this.vDop = vDop;

        this.location.setAccuracy(hDop * 5f);
    }

    public void updateAltitude(double altitude) {
        this.location.setAltitude(altitude);
    }

    public void updateMotion(float speed, float bearing) {
        this.location.setSpeed(speed * 0.514444f);
        this.location.setBearing(bearing);
    }

    public void updateTime(long time) {
        this.location.setTime(time);
        this.location.setElapsedRealtimeNanos(SystemClock.elapsedRealtimeNanos());
    }

    public void updateCoordinates(double latitude, double longitude) {
        this.location.setLatitude(latitude);
        this.location.setLongitude(longitude);
    }

    public void updateSatellite(int prn, float elevation, float azimuth, int snr) {
        GpsSatellite satellite = this.satelliteMap.get(prn);
        if (satellite == null) {
            satellite = new GpsSatellite(prn);
            this.satelliteMap.append(prn, satellite);
        }
        satellite.update(elevation, azimuth, snr);
    }

    public void flushSatellites() {
        for (int i = this.satelliteMap.size() - 1; i >= 0; i--) {
            GpsSatellite satellite = this.satelliteMap.valueAt(i);
            if (System.currentTimeMillis() - satellite.getLastUpdate() > 10000) {
                this.satelliteMap.remove(satellite.getPrn());
            }
        }
    }

    public boolean hasValidLocation() {
        if (Objects.equals(this.location.getLatitude(), Double.NaN)) return false;
        if (Objects.equals(this.location.getLongitude(), Double.NaN)) return false;
        if (!this.location.hasAccuracy()) return false;
        if (this.location.getTime() == 0) return false;
        if (this.location.getElapsedRealtimeNanos() == 0) return false;
        return true;
    }

    public Location getLocation() {
        return this.location;
    }

    public int getGpsSatelliteCount() {
        return this.satelliteMap.size();
    }

    public GpsSatellite[] getGpsSatellites(GpsSatellite[] satellites) {
        if (this.satelliteMap.size() != satellites.length) {
            satellites = new GpsSatellite[this.satelliteMap.size()];
        }

        for (int i = 0; i < this.satelliteMap.size(); i++) {
            satellites[i] = this.satelliteMap.valueAt(i);
        }

        return satellites;
    }

    public float getpDop() {
        return pDop;
    }

    public float gethDop() {
        return hDop;
    }

    public float getvDop() {
        return vDop;
    }

    public int getFix() {
        return fix;
    }
}
