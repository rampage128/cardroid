package de.jlab.cardroid.usb.carduino;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.view.View;
import android.widget.ExpandableListView;
import android.widget.GridView;
import android.widget.ListView;

import java.util.ArrayList;

import de.jlab.cardroid.R;
import de.jlab.cardroid.car.CarSystemFactory;
import de.jlab.cardroid.usb.CarSystemSerialPacket;
import de.jlab.cardroid.usb.UsageStatistics;
import de.jlab.cardroid.StatusGridAdapter;

public class CarduinoMonitorActivity extends AppCompatActivity implements SerialReader.SerialPacketListener {

    private GridView statusGridView;

    private ExpandableListView carSystemListView;
    private StatusGridAdapter carSystemGridAdapter;
    private CarSystemTreeAdapter carSystemListAdapter;

    private ListView packetListView;
    private PacketListAdapter packetListAdapter;
    private StatusGridAdapter connectionGridAdapter;

    private CarduinoService.MainServiceBinder serviceBinder;
    private ServiceConnection serviceConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            CarduinoMonitorActivity.this.serviceBinder = (CarduinoService.MainServiceBinder)service;

            CarduinoMonitorActivity.this.initStatusGrid();
            CarduinoMonitorActivity.this.initConnetionGrid();
            CarduinoMonitorActivity.this.serviceBinder.addSerialPacketListener(CarduinoMonitorActivity.this);
            CarduinoMonitorActivity.this.serviceBinder.addBandwidthStatisticsListener(CarduinoMonitorActivity.this.bandwidthStatisticsListener);
            CarduinoMonitorActivity.this.serviceBinder.addPacketStatisticsListener(CarduinoMonitorActivity.this.packetStatisticsListener);
        }

        public void onServiceDisconnected(ComponentName className) {
            CarduinoMonitorActivity.this.serviceBinder.removeSerialPacketListener(CarduinoMonitorActivity.this);
            CarduinoMonitorActivity.this.serviceBinder.removeBandwidthStatisticsListener(CarduinoMonitorActivity.this.bandwidthStatisticsListener);
            CarduinoMonitorActivity.this.serviceBinder.removePacketStatisticsListener(CarduinoMonitorActivity.this.packetStatisticsListener);
            CarduinoMonitorActivity.this.serviceBinder = null;
        }
    };

    private UsageStatistics.UsageStatisticsListener bandwidthStatisticsListener = new UsageStatistics.UsageStatisticsListener() {
        @Override
        public void onInterval(final int count, final UsageStatistics statistics) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    CarduinoMonitorActivity.this.connectionGridAdapter.updateStatistics(
                            R.string.status_bps,
                            count,
                            Math.round(statistics.getAverage()),
                            R.string.unit_bytes_per_second
                    );
                    CarduinoMonitorActivity.this.connectionGridAdapter.updateStatistics(
                            R.string.status_usage,
                            Math.round(100f / (CarduinoMonitorActivity.this.serviceBinder.getBaudRate() * 0.125f) * count),
                            Math.round(100f / (CarduinoMonitorActivity.this.serviceBinder.getBaudRate() * 0.125f) * statistics.getAverage()),
                            R.string.unit_percent
                    );
                    CarduinoMonitorActivity.this.connectionGridAdapter.notifyDataSetChanged();
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
                    CarduinoMonitorActivity.this.connectionGridAdapter.updateStatistics(
                            R.string.car_status_packets,
                            count,
                            Math.round(statistics.getAverage()),
                            R.string.unit_packets_per_second
                    );
                    CarduinoMonitorActivity.this.connectionGridAdapter.notifyDataSetChanged();
                }
            });
        }
    };


    @Override
    public void onReceivePackets(ArrayList<SerialPacket> packets) {
        for (SerialPacket packet : packets) {
            this.packetListAdapter.updatePacket(packet);
            if (packet instanceof CarSystemSerialPacket) {
                try {
                    this.carSystemListAdapter.updateCarSystem(this.serviceBinder.getCar().getCarSystem(CarSystemFactory.getType((CarSystemSerialPacket) packet)));
                }
                catch (Exception e) { /* Intentionally left blank */ }
            }
        }

        this.carSystemGridAdapter.update(R.string.car_status_systems, Integer.toString(this.serviceBinder.getCar().getCarSystemCount()));
        this.updateConnection(this.carSystemGridAdapter);
        this.updateConnection(this.connectionGridAdapter);

        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                CarduinoMonitorActivity.this.packetListAdapter.notifyDataSetChanged();
                CarduinoMonitorActivity.this.carSystemGridAdapter.notifyDataSetChanged();
                CarduinoMonitorActivity.this.carSystemListAdapter.notifyDataSetChanged();
            }
        });
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_carduino_monitor);

        this.carSystemListView = (ExpandableListView) findViewById(R.id.carSystemListView);
        this.packetListView = (ListView) findViewById(R.id.packetListView);

        final BottomNavigationView bottomBar = (BottomNavigationView)this.findViewById(R.id.bottom_navigation);
        bottomBar.setOnNavigationItemSelectedListener(
                new BottomNavigationView.OnNavigationItemSelectedListener() {
                    @Override
                    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                        switch (item.getItemId()) {
                            case R.id.action_status:
                                CarduinoMonitorActivity.this.carSystemListView.setVisibility(View.VISIBLE);
                                CarduinoMonitorActivity.this.packetListView.setVisibility(View.GONE);
                                CarduinoMonitorActivity.this.statusGridView.setAdapter(CarduinoMonitorActivity.this.carSystemGridAdapter);
                                return true;
                            case R.id.action_connection:
                                CarduinoMonitorActivity.this.carSystemListView.setVisibility(View.GONE);
                                CarduinoMonitorActivity.this.packetListView.setVisibility(View.VISIBLE);
                                CarduinoMonitorActivity.this.statusGridView.setAdapter(CarduinoMonitorActivity.this.connectionGridAdapter);
                                return true;
                        }
                        return false;
                    }
                });

        this.carSystemGridAdapter = new StatusGridAdapter(this);
        this.connectionGridAdapter = new StatusGridAdapter(this);

        this.statusGridView = (GridView) findViewById(R.id.statusGrid);
        this.statusGridView.setAdapter(this.carSystemGridAdapter);

        this.packetListView.setVisibility(View.GONE);
        this.packetListAdapter = new PacketListAdapter(this);
        this.packetListView.setAdapter(this.packetListAdapter);

        this.carSystemListAdapter = new CarSystemTreeAdapter(this);
        this.carSystemListView.setAdapter(this.carSystemListAdapter);
    }

    @Override
    protected void onPause() {
        super.onPause();
        unbindService(this.serviceConnection);
    }

    @Override
    protected void onResume() {
        super.onResume();
        bindService(new Intent(this, CarduinoService.class), this.serviceConnection, Context.BIND_AUTO_CREATE);
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
        this.carSystemGridAdapter.update(R.string.car_status_systems, na);
        this.carSystemGridAdapter.notifyDataSetChanged();
    }

    private void updateConnection(StatusGridAdapter adapter) {
        if (this.serviceBinder.isConnected()) {
            adapter.update(R.string.status_connection, this.getString(R.string.status_connected));
        }
        else {
            adapter.update(R.string.status_connection, this.getString(R.string.status_disconnected));
        }
    }

}
