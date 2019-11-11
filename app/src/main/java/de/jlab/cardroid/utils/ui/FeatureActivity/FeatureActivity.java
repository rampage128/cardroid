package de.jlab.cardroid.utils.ui.FeatureActivity;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.util.SparseArray;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.GridView;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.Iterator;

import androidx.annotation.CallSuper;
import androidx.annotation.IdRes;
import androidx.annotation.MenuRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import de.jlab.cardroid.R;
import de.jlab.cardroid.devices.Device;
import de.jlab.cardroid.devices.DeviceService;
import de.jlab.cardroid.devices.DeviceServiceConnection;
import de.jlab.cardroid.devices.Feature;
import de.jlab.cardroid.devices.identification.DeviceUid;
import de.jlab.cardroid.devices.storage.DeviceEntity;
import de.jlab.cardroid.devices.storage.DeviceRepository;

public abstract class FeatureActivity extends AppCompatActivity {

    private GridView dataView;
    private StatusGridAdapter dataAdapter;
    private BottomNavigationView navigation;

    private SparseArray<FeatureFragment> contentViews = new SparseArray<>();

    private DeviceSelector deviceSelector = new DeviceSelector(this::onDeviceSelected);
    private DeviceUid defaultDeviceUid;
    private DeviceEntity selectedDevice;
    private FeatureFragment currentContentView;

    private boolean deviceConnected = false;
    private Device.StateObserver onDeviceStateChange = this::onDeviceStateChange;

    private DeviceService.DeviceServiceBinder deviceService;
    private DeviceServiceConnection serviceConnection = new DeviceServiceConnection(this::onServiceStateChange);

    @CallSuper
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.template_activity_feature);

        this.defaultDeviceUid = this.getDefaultDeviceUid();

        this.dataView = this.findViewById(R.id.data);
        this.navigation = this.findViewById(R.id.navigation);

        this.dataAdapter = new StatusGridAdapter(this);
        this.dataView.setAdapter(this.dataAdapter);
        this.dataView.setOnItemClickListener(this::onDataItemClick);

        this.navigation.setOnNavigationItemSelectedListener(this::onNavigationItemSelected);

        this.contentViews.clear();
        this.initContentViews(this::addContentView);

        DeviceRepository repo = new DeviceRepository(getApplication());
        repo.getAll().observe(this, deviceEntities -> {
            Class<? extends Feature> featureType = this.getFeatureType();
            for (Iterator<DeviceEntity> entityIt = deviceEntities.iterator(); entityIt.hasNext();) {
                DeviceEntity entity = entityIt.next();
                if (!entity.hasFeature(featureType)) {
                    entityIt.remove();
                } else if (this.selectedDevice == null && entity.deviceUid.equals(this.defaultDeviceUid)) {
                    this.onDeviceSelected(entity);
                }
            }
            this.deviceSelector.updateDevices(deviceEntities);
        });

        navigation.getMenu().clear();
        navigation.inflateMenu(this.getMenuId());

        if (this.navigation.getMenu().size() > 0) {
            this.navigation.setSelectedItemId(this.navigation.getMenu().getItem(0).getItemId());
        }
    }

    private <FT extends Feature> void subscribeFeature(@NonNull Device.FeatureChangeObserver<FT> observer, @NonNull Class<FT> featureClass) {
        if (this.selectedDevice != null) {
            this.deviceService.subscribeFeature(observer, featureClass, this.selectedDevice.deviceUid);
        }
    }

    private <FT extends Feature> void unsubscribeFeature(@NonNull Device.FeatureChangeObserver<FT> observer, @NonNull Class<FT> featureClass) {
        this.deviceService.unsubscribeFeature(observer, featureClass);
    }

    private void onServiceStateChange(@NonNull DeviceService.DeviceServiceBinder deviceService, @NonNull DeviceServiceConnection.Action action) {
        if (action == DeviceServiceConnection.Action.BOUND) {
            this.deviceService = deviceService;
            if (this.selectedDevice != null) {
                this.deviceService.subscribeDeviceState(this.onDeviceStateChange, this.selectedDevice.deviceUid);
                if (this.currentContentView != null) {
                    this.currentContentView.onStart(this.deviceService, this::subscribeFeature);
                }
            }
        } else {
            if (this.currentContentView != null) {
                this.currentContentView.onStop(this.deviceService, this::unsubscribeFeature);
            }
            this.deviceService.unsubscribeDeviceState(this.onDeviceStateChange);
            this.deviceService = null;
        }

        this.onDeviceServiceStateChange(deviceService, action);
    }

    private void onDeviceStateChange(@NonNull Device device, @NonNull Device.State state, @NonNull Device.State previous) {
        this.deviceConnected = state == Device.State.READY;
        int newStatus = this.deviceConnected ? R.string.status_connected : R.string.status_disconnected;
        this.addDataItem(R.string.status_connection, getResources().getString(newStatus));
    }

    private void onDataItemClick(AdapterView<?> parent, View view, int position, long id) {
        if (id == R.string.status_device) {
            this.deviceSelector.show(this);
        } else if (id != R.string.status_connection) {
            String value = (String)this.dataAdapter.getItem(position);
            this.onDataItemClicked((int)id, value);
        }
    }

    private void onDeviceSelected(@NonNull DeviceEntity deviceEntity) {
        this.selectedDevice = deviceEntity;

        if (this.deviceService != null) {
            this.deviceService.unsubscribeDeviceState(this.onDeviceStateChange);
            this.deviceService.subscribeDeviceState(this.onDeviceStateChange, this.selectedDevice.deviceUid);
        }

        this.addDataItem(R.string.status_device, this.selectedDevice.displayName);
    }

    private boolean onNavigationItemSelected(@NonNull MenuItem item) {
        FeatureFragment contentView = this.contentViews.get(item.getItemId());
        if (contentView == null) {
            Log.e(this.getClass().getSimpleName(), "Can not navigate to " + getResources().getResourceName(item.getItemId()) + ". Did you call addContentView(...) for this id?");
            return false;
        }

        getSupportFragmentManager().beginTransaction()
                .replace(R.id.content, contentView)
                .commit();

        return true;
    }

    public void onFragmentReady(@NonNull FeatureFragment contentView) {
        this.dataAdapter.clear();
        this.addDataItem(R.string.status_device, this.selectedDevice != null ? this.selectedDevice.displayName : getString(R.string.status_device_none));
        String connectionStatus = this.selectedDevice != null ?
                this.deviceConnected ?
                        getString(R.string.status_connected) :
                        getString(R.string.status_disconnected) :
                getString(R.string.status_unavailable);
        this.addDataItem(R.string.status_connection, connectionStatus);
        contentView.initDataItems(this::addDataItem, this);
        this.dataAdapter.notifyDataSetChanged();

        if (this.currentContentView != null) {
            this.currentContentView.setDataItemConsumer(null);
            if (deviceService != null) {
                this.currentContentView.onStop(this.deviceService, this::unsubscribeFeature);
            }
        }
        contentView.setDataItemConsumer(this::updateDataItem);
        if (deviceService != null) {
            contentView.onStart(this.deviceService, this::subscribeFeature);
        }

        this.currentContentView = contentView;
    }

    private void updateDataItem(@StringRes int id, @NonNull String value) {
        if (id == R.string.status_device || id == R.string.status_connection) {
            return;
        }

        this.dataAdapter.update(id, value);
        this.runOnUiThread(() -> this.dataAdapter.notifyDataSetChanged());
    }

    private void addDataItem(@StringRes int id, @NonNull String value) {
        this.dataAdapter.update(id, value);
        this.runOnUiThread(() -> this.dataAdapter.notifyDataSetChanged());
    }

    @CallSuper
    @Override
    protected void onResume() {
        super.onResume();

        this.serviceConnection.bind(this);

        if (this.navigation.getMenu().size() < 2) {
            this.navigation.setVisibility(View.GONE);
        }
    }

    @CallSuper
    @Override
    protected void onPause() {
        super.onPause();

        this.serviceConnection.unbind(this);
    }

    @Nullable
    protected abstract DeviceUid getDefaultDeviceUid();
    @NonNull
    protected abstract Class<? extends Feature> getFeatureType();
    protected abstract void onDeviceServiceStateChange(@NonNull DeviceService.DeviceServiceBinder deviceService, @NonNull DeviceServiceConnection.Action action);
    protected abstract void onDataItemClicked(@StringRes int id, @Nullable String value);
    protected abstract void initContentViews(@NonNull ContentViewConsumer consumer);
    @MenuRes
    protected abstract int getMenuId();

    private void addContentView(@IdRes int menuId, @NonNull FeatureFragment fragment) {
        this.contentViews.put(menuId, fragment);
        fragment.setFragmentReadyListener(this::onFragmentReady);
    }

    public interface ContentViewConsumer {
        void addContentView(@IdRes int menuId, @NonNull FeatureFragment fragment);
    }

    public static abstract class FeatureFragment extends Fragment {
        private DataItemConsumer consumer;
        private OnFragmentReadyListener readyListener;

        protected abstract void initDataItems(@NonNull DataItemConsumer consumer, @NonNull Context context);
        protected abstract void onStart(@NonNull DeviceService.DeviceServiceBinder deviceService, @NonNull FeatureSubscriptionConsumer consumer);
        protected abstract void onStop(@NonNull DeviceService.DeviceServiceBinder deviceService, @NonNull FeatureUnsubscriptionConsumer consumer);
        protected void updateDataItem(@StringRes int id, String value) {
            if (consumer != null) {
                consumer.put(id, value);
            }
        }

        @CallSuper
        @Override
        public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
            super.onViewCreated(view, savedInstanceState);

            if (this.readyListener != null) {
                this.readyListener.onFragmentReady(this);
            }
        }

        public void setFragmentReadyListener(@Nullable OnFragmentReadyListener listener) {
            this.readyListener = listener;
        }

        public void setDataItemConsumer(@Nullable DataItemConsumer consumer) {
            this.consumer = consumer;
        }

        public interface OnFragmentReadyListener {
            void onFragmentReady(@NonNull FeatureFragment fragment);
        }

        public interface DataItemConsumer {
            void put(@StringRes int id, @NonNull String value);
        }

        public interface FeatureSubscriptionConsumer {
            <FT extends Feature> void subscribeFeature(@NonNull Device.FeatureChangeObserver<FT> observer, @NonNull Class<FT> featureClass);
        }

        public interface FeatureUnsubscriptionConsumer {
            <FT extends Feature> void unsubscribeFeature(@NonNull Device.FeatureChangeObserver<FT> observer, @NonNull Class<FT> featureClass);
        }
    }

}
