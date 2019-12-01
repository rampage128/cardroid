package de.jlab.cardroid;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.ColorRes;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.RecyclerView;
import de.jlab.cardroid.car.ui.CanSnifferActivity;
import de.jlab.cardroid.car.ui.CarMonitorActivity;
import de.jlab.cardroid.devices.Device;
import de.jlab.cardroid.devices.DeviceService;
import de.jlab.cardroid.devices.DeviceServiceConnection;
import de.jlab.cardroid.devices.Feature;
import de.jlab.cardroid.devices.identification.DeviceUid;
import de.jlab.cardroid.devices.ui.DeviceActivity;
import de.jlab.cardroid.gps.ui.GpsMonitorActivity;
import de.jlab.cardroid.rules.RuleActivity;
import de.jlab.cardroid.utils.ui.CircularMenuView;

public final class MainActivity extends AppCompatActivity {

    private StatsListAdapter statsListAdapter;
    private DeviceServiceConnection deviceServiceConnection = new DeviceServiceConnection(this::deviceServiceAction);

    private Device.StateObserver deviceObserver = this::onDeviceStateChange;
    private Device.FeatureChangeObserver<Feature> featureChangeObserver = this::onFeatureChange;
    private int deviceCount = 0;
    private int featureCount = 0;

    private DashboardStatistic deviceStats = null;
    private DashboardStatistic featureStats = null;
    private DashboardStatistic rulesStats = null;
    private DashboardStatistic variableStats = null;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        CircularMenuView actionMenu = findViewById(R.id.actionMenu);
        actionMenu.setOnMenuItemClickListener(this::onActionClicked);

        this.statsListAdapter = new StatsListAdapter(this);
        RecyclerView statistics = findViewById(R.id.statistics);
        statistics.setAdapter(this.statsListAdapter);

        this.deviceStats = new DashboardStatistic(R.string.dashboard_stats_devices, R.color.dashboard_stats_devices, R.drawable.ic_dashboard_stats_devices, this);
        this.featureStats = new DashboardStatistic(R.string.dashboard_stats_features, R.color.dashboard_stats_features, R.drawable.ic_dashboard_stats_feature, this);
        this.rulesStats = new DashboardStatistic(R.string.dashboard_stats_rules, R.color.dashboard_stats_rules, R.drawable.ic_dashboard_stats_rules, this);
        this.variableStats = new DashboardStatistic(R.string.dashboard_stats_variables, R.color.dashboard_stats_variables, R.drawable.ic_dashboard_stats_vars, this);

        this.featureStats.setAction(this::deviceStatsClicked);
        this.deviceStats.setAction(this::deviceStatsClicked);
        this.rulesStats.setAction(this::ruleStatsClicked);
        this.variableStats.setAction(this::variableStatsClicked);

        this.statsListAdapter.addStatistic(this.deviceStats);
        this.statsListAdapter.addStatistic(this.featureStats);
        this.statsListAdapter.addStatistic(this.rulesStats);
        this.statsListAdapter.addStatistic(this.variableStats);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.dashboard_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_settings:
                startActivity(SettingsActivity.class);
                break;
        }
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();

        this.deviceServiceConnection.bind(this);
    }

    @Override
    protected void onPause() {
        super.onPause();

        this.deviceServiceConnection.unbind(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    private void onFeatureChange(@NonNull Feature feature, @NonNull Feature.State state) {
        if (state == Feature.State.AVAILABLE) {
            this.featureCount++;
        } else {
            this.featureCount = Math.max(0, --this.featureCount);
        }

        this.featureStats.update(this.featureCount);
        this.runOnUiThread(() -> this.statsListAdapter.notifyDataSetChanged());
    }

    private void onDeviceStateChange(@NonNull Device device, @NonNull Device.State state, @NonNull Device.State previous) {
        if (state == Device.State.READY) {
            this.deviceCount++;
        } else {
            this.deviceCount = Math.max(0, --this.deviceCount);
        }

        Log.e(this.getClass().getSimpleName(), previous + " -> " + state + ": " + device);

        this.deviceStats.update(this.deviceCount);
        this.runOnUiThread(() -> this.statsListAdapter.notifyDataSetChanged());
    }

    private void deviceServiceAction(@NonNull DeviceService.DeviceServiceBinder deviceService, @NonNull DeviceServiceConnection.Action action) {
        if (action == DeviceServiceConnection.Action.BOUND) {
            this.variableStats.update(deviceService.getVariableStore().getAll().length);
            this.rulesStats.update(deviceService.getRuleController().getRuleCount());
            this.statsListAdapter.notifyDataSetChanged();

            this.deviceCount = 0;
            this.featureCount = 0;

            deviceService.subscribeDeviceState(this.deviceObserver, (DeviceUid) null);
            deviceService.subscribeFeature(this.featureChangeObserver, Feature.class, (DeviceUid) null);
        } else {
            this.deviceCount = 0;
            this.featureCount = 0;

            deviceService.unsubscribeDeviceState(this.deviceObserver);
            deviceService.unsubscribeFeature(this.featureChangeObserver, Feature.class);
        }

        Log.e(this.getClass().getSimpleName(), "Service " + action + ": " + deviceService);
    }

    private boolean onActionClicked(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_can:
                this.startActivity(CarMonitorActivity.class);
                break;
            case R.id.action_sniffer:
                this.startActivity(CanSnifferActivity.class);
                break;
            case R.id.action_location:
                this.startActivity(GpsMonitorActivity.class);
                break;
            case R.id.action_devices:
                this.startActivity(DeviceActivity.class);
                break;
            case R.id.action_rules:
                this.startActivity(RuleActivity.class);
                break;
        }

        return true;
    }

    private void ruleStatsClicked() {
        this.startActivity(RuleActivity.class);
    }

    private void deviceStatsClicked() {
        this.startActivity(DeviceActivity.class);
    }

    private void variableStatsClicked() {
        this.startActivity(CarMonitorActivity.class);
    }

    private void startActivity(@NonNull Class<? extends Activity> activityClass) {
        Intent intent = new Intent(this, activityClass);
        this.startActivity(intent);
    }

    private static class StatsListAdapter extends RecyclerView.Adapter<StatsListAdapter.ViewHolder> {

        private final MainActivity activity;
        private List<DashboardStatistic> mValues = new ArrayList<>();
        private final View.OnClickListener mOnClickListener = view -> {
            DashboardStatistic item = (DashboardStatistic) view.getTag();
            item.performAction();
        };

        public StatsListAdapter(MainActivity activity) {
            this.activity = activity;
        }

        public void addStatistic(DashboardStatistic statistic){
            mValues.add(statistic);
            notifyDataSetChanged();
        }

        @Override
        public MainActivity.StatsListAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.listitem_app_statistic, parent, false);
            return new MainActivity.StatsListAdapter.ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(final MainActivity.StatsListAdapter.ViewHolder holder, int position) {
            DashboardStatistic stats = mValues.get(position);

            holder.container.setAlpha(stats.getNumber() == 0 ? .5f : 1f);
            holder.container.setCardBackgroundColor(stats.getColor());
            holder.icon.setImageResource(stats.getIcon());
            holder.name.setText(stats.getCaption(this.activity));

            holder.itemView.setTag(stats);
            holder.itemView.setOnClickListener(mOnClickListener);
        }

        @Override
        public int getItemCount() {
            return mValues != null ? mValues.size() : 0;
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            final MaterialCardView container;
            final ImageView icon;
            final TextView name;

            ViewHolder(View view) {
                super(view);
                this.container = (MaterialCardView)view;
                this.icon = view.findViewById(R.id.icon);
                this.name = view.findViewById(R.id.name);
            }
        }
    }

    private static class DashboardStatistic {

        private int number;
        @StringRes
        private int caption;
        @DrawableRes
        private int icon;
        private int color;
        private Runnable action;

        public DashboardStatistic(@StringRes int caption, @ColorRes int color, @DrawableRes int icon, @NonNull Context context) {
            this.color = context.getResources().getColor(color);
            this.caption = caption;
            this.icon = icon;
            this.number = 0;
        }

        public void setAction(@Nullable Runnable action) {
            this.action = action;
        }

        public void update(int number) {
            this.number = number;
        }

        public String getCaption(@NonNull Context context) {
            return context.getString(this.caption, this.number);
        }

        public int getNumber() {
            return this.number;
        }

        @DrawableRes
        public int getIcon() {
            return this.icon;
        }

        public int getColor() {
            return this.color;
        }

        public void performAction() {
            if (this.action != null) {
                this.action.run();
            }
        }

    }

}
