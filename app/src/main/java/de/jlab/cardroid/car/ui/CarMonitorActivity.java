package de.jlab.cardroid.car.ui;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.view.View;
import android.widget.GridView;
import android.widget.ListView;
import android.widget.ScrollView;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import de.jlab.cardroid.R;
import de.jlab.cardroid.StatusGridAdapter;
import de.jlab.cardroid.car.CanObservable;
import de.jlab.cardroid.car.CanPacket;
import de.jlab.cardroid.devices.DeviceService;
import de.jlab.cardroid.devices.FeatureFilter;

public class CarMonitorActivity extends AppCompatActivity {

    private boolean canReaderConnected = false;

    private GridView statusGridView;

    private ListView carSystemListView;
    private StatusGridAdapter carSystemGridAdapter;
    private VariableListAdapter carSystemListAdapter;
    private BottomNavigationView bottomBar;

    private CanView packetListView;
    private ScrollView packetListViewContainer;
    private StatusGridAdapter connectionGridAdapter;

    private FeatureFilter<CanObservable> readFilter = new FeatureFilter<>(CanObservable.class, null, this::canReaderConnected, this::canReaderDisconnected);

    private DeviceService.DeviceServiceBinder serviceBinder;
    private ServiceConnection deviceServiceConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            CarMonitorActivity.this.serviceBinder = (DeviceService.DeviceServiceBinder) service;

            CarMonitorActivity.this.initStatusGrid();
            CarMonitorActivity.this.initConnetionGrid();
            CarMonitorActivity.this.serviceBinder.subscribe(CarMonitorActivity.this.readFilter, CanObservable.class);
            //CarMonitorActivity.this.serviceBinder.addBandwidthStatisticsListener(CarMonitorActivity.this.bandwidthStatisticsListener);
            //CarMonitorActivity.this.serviceBinder.addPacketStatisticsListener(CarMonitorActivity.this.packetStatisticsListener);
            CarMonitorActivity.this.carSystemListAdapter.updateFromStore(CarMonitorActivity.this.serviceBinder.getVariableStore());
        }

        public void onServiceDisconnected(ComponentName className) {
            CarMonitorActivity.this.serviceBinder.unsubscribe(CarMonitorActivity.this.readFilter, CanObservable.class);
            //CarMonitorActivity.this.serviceBinder.removeBandwidthStatisticsListener(CarMonitorActivity.this.bandwidthStatisticsListener);
            //CarMonitorActivity.this.serviceBinder.removePacketStatisticsListener(CarMonitorActivity.this.packetStatisticsListener);
            CarMonitorActivity.this.serviceBinder = null;
        }
    };

    /*
    private UsageStatistics.UsageStatisticsListener bandwidthStatisticsListener = new UsageStatistics.UsageStatisticsListener() {
        @Override
        public void onInterval(final int count, final UsageStatistics statistics) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    CarMonitorActivity.this.connectionGridAdapter.updateStatistics(
                            R.string.status_bps,
                            count,
                            Math.round(statistics.getAverage()),
                            R.string.unit_bytes_per_second
                    );
                    CarMonitorActivity.this.connectionGridAdapter.updateStatistics(
                            R.string.status_usage,
                            Math.round(100f / (CarMonitorActivity.this.serviceBinder.getBaudRate() * 0.125f) * count),
                            Math.round(100f / (CarMonitorActivity.this.serviceBinder.getBaudRate() * 0.125f) * statistics.getAverage()),
                            R.string.unit_percent
                    );
                    CarMonitorActivity.this.connectionGridAdapter.notifyDataSetChanged();
                }
            });
        }
    };

    private UsageStatistics.UsageStatisticsListener packetStatisticsListener = new UsageStatistics.UsageStatisticsListener() {
        @Override
        public void onInterval(final int count, final UsageStatistics statistics) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    CarMonitorActivity.this.connectionGridAdapter.updateStatistics(
                            R.string.car_status_packets,
                            count,
                            Math.round(statistics.getAverage()),
                            R.string.unit_packets_per_second
                    );
                    CarMonitorActivity.this.connectionGridAdapter.notifyDataSetChanged();
                }
            });
        }
    };
     */

    private void canReaderConnected(CanObservable feature) {
        feature.addListener(this::onReceiveCanPacket);
        this.canReaderConnected = true;
    }

    private void canReaderDisconnected(CanObservable feature) {
        feature.removeListener(this::onReceiveCanPacket);
        this.canReaderConnected = false;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_carduino_monitor);

        this.carSystemListView = findViewById(R.id.carSystemListView);
        this.packetListViewContainer = findViewById(R.id.packetListViewContainer);
        this.packetListView = findViewById(R.id.packetListView);

        this.bottomBar = this.findViewById(R.id.bottom_navigation);
        bottomBar.setOnNavigationItemSelectedListener(
                item -> {
                    switch (item.getItemId()) {
                        case R.id.action_status:
                            CarMonitorActivity.this.carSystemListView.setVisibility(View.VISIBLE);
                            CarMonitorActivity.this.packetListViewContainer.setVisibility(View.GONE);
                            CarMonitorActivity.this.statusGridView.setAdapter(CarMonitorActivity.this.carSystemGridAdapter);
                            CarMonitorActivity.this.packetListView.stopLiveMode();
                            return true;
                        case R.id.action_connection:
                            CarMonitorActivity.this.carSystemListView.setVisibility(View.GONE);
                            CarMonitorActivity.this.packetListViewContainer.setVisibility(View.VISIBLE);
                            CarMonitorActivity.this.statusGridView.setAdapter(CarMonitorActivity.this.connectionGridAdapter);
                            CarMonitorActivity.this.packetListView.startLiveMode();
                            return true;
                    }
                    return false;
                });

        this.carSystemGridAdapter = new StatusGridAdapter(this);
        this.connectionGridAdapter = new StatusGridAdapter(this);

        this.statusGridView = findViewById(R.id.statusGrid);
        this.statusGridView.setAdapter(this.carSystemGridAdapter);

        this.packetListViewContainer.setVisibility(View.GONE);

        this.carSystemListAdapter = new VariableListAdapter(this);
        this.carSystemListView.setAdapter(this.carSystemListAdapter);
    }

    @Override
    protected void onPause() {
        super.onPause();
        CarMonitorActivity.this.packetListView.stopLiveMode();
        this.getApplicationContext().unbindService(this.deviceServiceConnection);
    }

    @Override
    protected void onResume() {
        super.onResume();
        this.getApplicationContext().bindService(new Intent(this.getApplicationContext(), DeviceService.class), this.deviceServiceConnection, Context.BIND_AUTO_CREATE);
        if (this.bottomBar.getSelectedItemId() == R.id.action_connection) {
            CarMonitorActivity.this.packetListView.startLiveMode();
        }
    }

    private void initConnetionGrid() {
        String na = this.getString(R.string.status_unavailable);
        this.updateConnection(this.connectionGridAdapter);
        this.connectionGridAdapter.update(R.string.car_status_packets, na);
        this.connectionGridAdapter.update(R.string.status_bps, na);
        this.connectionGridAdapter.update(R.string.status_usage, na);
        this.connectionGridAdapter.notifyDataSetChanged();
    }

    private void initStatusGrid() {
        String na = this.getString(R.string.status_unavailable);
        this.updateConnection(this.carSystemGridAdapter);
        this.carSystemGridAdapter.update(R.string.car_status_variables, na);
        this.carSystemGridAdapter.notifyDataSetChanged();
    }

    private void updateConnection(StatusGridAdapter adapter) {
        if (this.canReaderConnected) {
            adapter.update(R.string.status_connection, this.getString(R.string.status_connected));
        }
        else {
            adapter.update(R.string.status_connection, this.getString(R.string.status_disconnected));
        }
    }

    private void onReceiveCanPacket(@NonNull CanPacket packet) {
        this.runOnUiThread(() -> {
            if (this.packetListView.updatePacket(packet)) {
                this.packetListView.invalidate();
            }
        });

        this.updateConnection(this.carSystemGridAdapter);
        this.updateConnection(this.connectionGridAdapter);
        this.carSystemGridAdapter.update(R.string.car_status_variables, Integer.toString(this.carSystemListAdapter.getCount()));

        this.runOnUiThread(() -> {
            CarMonitorActivity.this.carSystemGridAdapter.notifyDataSetChanged();
            CarMonitorActivity.this.carSystemListAdapter.notifyDataSetChanged();
        });
    }

}
