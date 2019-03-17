package de.jlab.cardroid.usb.gps;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.Bundle;
import android.text.format.DateUtils;
import android.text.method.ScrollingMovementMethod;
import android.view.MenuItem;
import android.view.View;
import android.widget.GridView;
import android.widget.ScrollView;
import android.widget.TextView;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import de.jlab.cardroid.StatusGridAdapter;
import de.jlab.cardroid.R;
import de.jlab.cardroid.usb.UsageStatistics;

public class GpsMonitorActivity extends AppCompatActivity implements GPSSerialReader.PositionListener {

    private ScrollView rawDataScrollView;
    private TextView rawDataTextView;
    private GpsService.GpsServiceBinder gpsService;

    private GridView statusGridView;
    private StatusGridAdapter statusGridAdapter;
    private StatusGridAdapter rawGridAdapter;
    private GpsSatView satView;
    private GpsSatellite[] satellites = new GpsSatellite[0];

    private int sentenceCounter = 0;
    private StringBuilder rawText = new StringBuilder();

    private ServiceConnection gpsServiceConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            gpsService = (GpsService.GpsServiceBinder)service;

            GpsMonitorActivity.this.updateStatusGrid(null);
            GpsMonitorActivity.this.updateRawGrid();
            GpsMonitorActivity.this.updateSatelliteView(null);
            gpsService.addPositionListener(GpsMonitorActivity.this);
            gpsService.addBandwidthStatisticsListener(GpsMonitorActivity.this.bandwidthStatisticsListener);
            gpsService.addSentenceStatisticsListener(GpsMonitorActivity.this.sentenceStatisticsListener);
            gpsService.addUpdateStatisticsListener(GpsMonitorActivity.this.updateStatisticsListener);
        }

        public void onServiceDisconnected(ComponentName className) {
            gpsService.removePositionListener(GpsMonitorActivity.this);
            gpsService.removeBandwidthStatisticsListener(GpsMonitorActivity.this.bandwidthStatisticsListener);
            gpsService.removeSentenceStatisticsListener(GpsMonitorActivity.this.sentenceStatisticsListener);
            gpsService.removeUpdateStatisticsListener(GpsMonitorActivity.this.updateStatisticsListener);
            gpsService = null;
        }
    };

    private UsageStatistics.UsageStatisticsListener bandwidthStatisticsListener = new UsageStatistics.UsageStatisticsListener() {
        @Override
        public void onInterval(final int count, final UsageStatistics statistics) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    GpsMonitorActivity.this.rawGridAdapter.updateStatistics(
                            R.string.status_bps,
                            count,
                            Math.round(statistics.getAverage()),
                            R.string.unit_bytes_per_second
                    );
                    GpsMonitorActivity.this.rawGridAdapter.updateStatistics(
                            R.string.status_usage,
                            Math.round(100f / (GpsMonitorActivity.this.gpsService.getBaudRate() * 0.125f) * count),
                            Math.round(100f / (GpsMonitorActivity.this.gpsService.getBaudRate() * 0.125f) * statistics.getAverage()),
                            R.string.unit_percent
                    );
                    GpsMonitorActivity.this.rawGridAdapter.notifyDataSetChanged();
                }
            });
        }
    };

    private UsageStatistics.UsageStatisticsListener sentenceStatisticsListener = new UsageStatistics.UsageStatisticsListener() {
        @Override
        public void onInterval(final int count, final UsageStatistics statistics) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    GpsMonitorActivity.this.rawGridAdapter.updateStatistics(
                            R.string.gps_status_sps,
                            count,
                            Math.round(statistics.getAverage()),
                            R.string.unit_sentences_per_second
                    );
                    GpsMonitorActivity.this.rawGridAdapter.notifyDataSetChanged();
                }
            });
        }
    };

    private UsageStatistics.UsageStatisticsListener updateStatisticsListener = new UsageStatistics.UsageStatisticsListener() {
        @Override
        public void onInterval(final int count, final UsageStatistics statistics) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    GpsMonitorActivity.this.statusGridAdapter.updateStatistics(
                            R.string.gps_status_frequency,
                            count,
                            Math.round(statistics.getAverage()),
                            R.string.unit_hertz
                    );
                    GpsMonitorActivity.this.statusGridAdapter.notifyDataSetChanged();
                }
            });
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

        this.statusGridAdapter = new StatusGridAdapter(this);
        this.statusGridView = (GridView) findViewById(R.id.statusGrid);
        this.statusGridView.setAdapter(this.statusGridAdapter);

        this.rawGridAdapter = new StatusGridAdapter(this);
        this.rawDataScrollView.setVisibility(View.GONE);
    }

    @Override
    public void onUpdate(final GpsPosition position, final String rawData) {
        this.sentenceCounter++;

        if (this.sentenceCounter > 1) {
            this.rawText.append("\n");
        }
        this.rawText.append(rawData);
        if (this.sentenceCounter >= 100) {
            this.rawText.replace(0, this.rawText.indexOf("\n") + 1, "");
        }

        position.flushSatellites();
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                GpsMonitorActivity.this.updateRawText(rawText.toString());
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

    public void updateRawText(String rawData) {
        this.rawDataTextView.setText(rawData);
        this.rawDataScrollView.fullScroll(ScrollView.FOCUS_DOWN);
    }

    public void updateRawGrid() {
        String na = this.getString(R.string.status_unavailable);
        if (this.gpsService.isConnected()) {
            this.rawGridAdapter.update(R.string.status_connection, this.getString(R.string.status_connected));
        }
        else {
            this.rawGridAdapter.update(R.string.status_connection, this.getString(R.string.status_disconnected));
        }
        this.rawGridAdapter.update(R.string.gps_status_sps, na);
        this.rawGridAdapter.update(R.string.status_bps, na);
        this.rawGridAdapter.update(R.string.status_usage, na);
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
        String na = this.getString(R.string.status_unavailable);
        if (position == null) {
            this.statusGridAdapter.update(R.string.status_connection, this.getString(R.string.status_disconnected));
            this.statusGridAdapter.update(R.string.gps_status_fix, na);
            this.statusGridAdapter.update(R.string.gps_status_latitude, na);
            this.statusGridAdapter.update(R.string.gps_status_longitude, na);
            this.statusGridAdapter.update(R.string.gps_status_altitude, na);
            this.statusGridAdapter.update(R.string.gps_status_speed, na);
            this.statusGridAdapter.update(R.string.gps_status_bearing, na);
            this.statusGridAdapter.update(R.string.gps_status_time, na);
            this.statusGridAdapter.update(R.string.gps_status_accuracy, na);
            this.statusGridAdapter.update(R.string.gps_status_frequency, na);
            this.statusGridAdapter.update(R.string.gps_status_satellite_count, na);
        }
        else {
            String translationString = "gps_status_fix_" + position.getFix();
            int fixId = getResources().getIdentifier(translationString, "string", getPackageName());

            this.statusGridAdapter.update(R.string.status_connection, this.getString(R.string.status_connected));
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
