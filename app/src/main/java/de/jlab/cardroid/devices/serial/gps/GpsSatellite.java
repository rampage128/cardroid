package de.jlab.cardroid.devices.serial.gps;

public final class GpsSatellite {

    private int prn = 0;
    private float elevation = 0;
    private float azimuth = 0;
    private int snr = 0;

    private long lastUpdate = 0;

    public GpsSatellite(int prn) {
        this.prn = prn;
    }

    public int getPrn() {
        return prn;
    }

    public float getElevation() {
        return elevation;
    }

    public float getAzimuth() {
        return azimuth;
    }

    public int getSnr() {
        return snr;
    }

    public long getLastUpdate() {
        return this.lastUpdate;
    }

    public void update(float elevation, float azimuth, int snr) {
        this.elevation = elevation;
        this.azimuth = azimuth;
        this.snr = snr;
        this.lastUpdate = System.currentTimeMillis();
    }
}
