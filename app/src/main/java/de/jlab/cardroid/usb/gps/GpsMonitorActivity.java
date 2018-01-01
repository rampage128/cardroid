package de.jlab.cardroid.usb.gps;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.format.DateUtils;
import android.text.method.ScrollingMovementMethod;
import android.view.MenuItem;
import android.view.View;
import android.widget.GridView;
import android.widget.ScrollView;
import android.widget.TextView;

import de.jlab.cardroid.R;
import de.jlab.cardroid.usb.UsageStatistics;

public class GpsMonitorActivity extends AppCompatActivity implements GPSSerialReader.PositionListener, UsageStatistics.UsageStatisticsListener {

    private ScrollView rawDataScrollView;
    private TextView rawDataTextView;
    private GpsService.GpsServiceBinder gpsService;

    private GridView statusGridView;
    private GpsStatusGridAdapter statusGridAdapter;
    private GpsStatusGridAdapter rawGridAdapter;
    private GpsSatView satView;
    private GpsSatellite[] satellites = new GpsSatellite[0];

    private ServiceConnection gpsServiceConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            gpsService = (GpsService.GpsServiceBinder)service;

            GpsMonitorActivity.this.updateStatusGrid(null);
            GpsMonitorActivity.this.updateRawGrid();
            GpsMonitorActivity.this.updateSatelliteView(null);
            gpsService.addPositionListener(GpsMonitorActivity.this);
            gpsService.addBandwidthStatisticsListener(GpsMonitorActivity.this);
        }

        public void onServiceDisconnected(ComponentName className) {
            gpsService.removePositionListener(GpsMonitorActivity.this);
            gpsService.removeBandwidthStatisticsListener(GpsMonitorActivity.this);
            gpsService = null;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gps_monitor);

        this.rawDataScrollView = (ScrollView) findViewById(R.id.rawDataScrollView);
        this.rawDataTextView = (TextView) findViewById(R.id.rawDataTextView);
        this.rawDataTextView.setMovementMethod(new ScrollingMovementMethod());

        this.satView = (GpsSatView)findViewById(R.id.gpsSatView);

        final BottomNavigationView bottomBar = (BottomNavigationView)this.findViewById(R.id.bottom_navigation);
        bottomBar.setOnNavigationItemSelectedListener(
                new BottomNavigationView.OnNavigationItemSelectedListener() {
                    @Override
                    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                        switch (item.getItemId()) {
                            case R.id.action_status:
                                satView.setVisibility(View.VISIBLE);
                                GpsMonitorActivity.this.rawDataScrollView.setVisibility(View.GONE);
                                GpsMonitorActivity.this.statusGridView.setAdapter(GpsMonitorActivity.this.statusGridAdapter);
                                return true;
                            case R.id.action_raw_data:
                                satView.setVisibility(View.GONE);
                                GpsMonitorActivity.this.rawDataScrollView.setVisibility(View.VISIBLE);
                                GpsMonitorActivity.this.statusGridView.setAdapter(GpsMonitorActivity.this.rawGridAdapter);
                                return true;
                        }
                        return false;
                    }
                });

        this.statusGridAdapter = new GpsStatusGridAdapter(this);
        this.statusGridView = (GridView) findViewById(R.id.statusGrid);
        this.statusGridView.setAdapter(this.statusGridAdapter);

        this.rawGridAdapter = new GpsStatusGridAdapter(this);
        this.rawDataScrollView.setVisibility(View.GONE);
    }

    @Override
    public void onInterval(final int count, final UsageStatistics statistics) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                GpsMonitorActivity.this.rawGridAdapter.update(R.string.gps_status_bps, Integer.toString(Math.round(statistics.getAverage())));
                GpsMonitorActivity.this.rawGridAdapter.update(R.string.gps_status_usage, getString(R.string.gps_status_usage_value, Math.round(100f / (GpsMonitorActivity.this.gpsService.getBaudRate() * 0.125f) * count)));
            }
        });
    }

    @Override
    public void onUpdate(final GpsPosition position, final String rawData) {
        position.flushSatellites();
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                GpsMonitorActivity.this.rawDataTextView.append(rawData);
                GpsMonitorActivity.this.rawDataTextView.append("\n");
                GpsMonitorActivity.this.rawDataScrollView.fullScroll(ScrollView.FOCUS_DOWN);
                GpsMonitorActivity.this.updateStatusGrid(position);
                GpsMonitorActivity.this.updateSatelliteView(position);
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        unbindService(this.gpsServiceConnection);
    }

    @Override
    protected void onResume() {
        super.onResume();
        bindService(new Intent(this, GpsService.class), this.gpsServiceConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    public void onError() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                GpsMonitorActivity.this.updateStatusGrid(null);
            }
        });
    }

    public void updateRawGrid() {
        String na = this.getString(R.string.gps_status_unavailable);
        if (this.gpsService.isConnected()) {
            this.rawGridAdapter.update(R.string.gps_status_connection, this.getString(R.string.gps_status_connected));
        }
        else {
            this.rawGridAdapter.update(R.string.gps_status_connection, this.getString(R.string.gps_status_disconnected));
        }
        this.rawGridAdapter.update(R.string.gps_status_sps, na);
        this.rawGridAdapter.update(R.string.gps_status_bps, na);
        this.rawGridAdapter.update(R.string.gps_status_usage, na);
        this.rawGridAdapter.notifyDataSetChanged();
    }

    public void updateSatelliteView(GpsPosition position) {
        if (position == null) {
            this.satellites = new GpsSatellite[0];
        }
        else {
            this.satellites = position.getGpsSatellites(this.satellites);
        }
        this.satView.setSatellites(this.satellites);
        this.satView.invalidate();
    }

    public void updateStatusGrid(GpsPosition position) {
        if (position == null) {
            String na = this.getString(R.string.gps_status_unavailable);
            this.statusGridAdapter.update(R.string.gps_status_connection, this.getString(R.string.gps_status_disconnected));
            this.statusGridAdapter.update(R.string.gps_status_fix, na);
            this.statusGridAdapter.update(R.string.gps_status_latitude, na);
            this.statusGridAdapter.update(R.string.gps_status_longitude, na);
            this.statusGridAdapter.update(R.string.gps_status_altitude, na);
            this.statusGridAdapter.update(R.string.gps_status_speed, na);
            this.statusGridAdapter.update(R.string.gps_status_bearing, na);
            this.statusGridAdapter.update(R.string.gps_status_time, na);
            this.statusGridAdapter.update(R.string.gps_status_accuracy, na);
            this.statusGridAdapter.update(R.string.gps_status_satellite_count, na);
        }
        else {
            String translationString = "gps_status_fix_" + position.getFix();
            int fixId = getResources().getIdentifier(translationString, "string", getPackageName());

            this.statusGridAdapter.update(R.string.gps_status_connection, this.getString(R.string.gps_status_connected));
            this.statusGridAdapter.update(R.string.gps_status_fix, fixId > 0 ? this.getString(fixId) : translationString);
            this.statusGridAdapter.update(R.string.gps_status_latitude, Double.toString(position.getLocation().getLatitude()));
            this.statusGridAdapter.update(R.string.gps_status_longitude, Double.toString(position.getLocation().getLongitude()));
            this.statusGridAdapter.update(R.string.gps_status_altitude, Double.toString(position.getLocation().getAltitude()));
            this.statusGridAdapter.update(R.string.gps_status_speed, Float.toString(position.getLocation().getSpeed()));
            this.statusGridAdapter.update(R.string.gps_status_bearing, Float.toString(position.getLocation().getBearing()));
            this.statusGridAdapter.update(R.string.gps_status_time, DateUtils.formatDateTime(this, position.getLocation().getTime(), DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_SHOW_TIME | DateUtils.FORMAT_ABBREV_ALL));
            this.statusGridAdapter.update(R.string.gps_status_accuracy, Float.toString(position.getLocation().getAccuracy()));
            this.statusGridAdapter.update(R.string.gps_status_satellite_count, Integer.toString(position.getGpsSatelliteCount()));
        }
        this.statusGridAdapter.notifyDataSetChanged();
    }
}
