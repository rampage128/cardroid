package de.jlab.cardroid.devices.ui;

import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.text.Html;
import android.view.MenuItem;

import com.google.android.material.snackbar.Snackbar;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import de.jlab.cardroid.R;
import de.jlab.cardroid.SettingsActivity;
import de.jlab.cardroid.devices.DeviceConnection;
import de.jlab.cardroid.devices.DeviceConnectionStore;
import de.jlab.cardroid.devices.DeviceService;
import de.jlab.cardroid.devices.storage.DeviceEntity;
import de.jlab.cardroid.devices.storage.DeviceRepository;

public final class DeviceActivity extends AppCompatActivity implements DeviceListFragment.DeviceListInteractionListener, DeviceDetailFragment.DeviceDetailInteractionListener, DeviceConnectionStore.DeviceConnectionObserver {

    public static final String EXTRA_DEVICE_ID = "deviceId";

    private boolean isTwoPane;

    private Fragment activeFragment;
    private Fragment listFragment;

    private DeviceService.DeviceServiceBinder deviceService;
    private ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            deviceService = (DeviceService.DeviceServiceBinder) service;
            deviceService.addConnectionObserver(DeviceActivity.this);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            deviceService.removeConnectionObserver(DeviceActivity.this);
            deviceService = null;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_devices);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setTitle(getTitle());

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        if (findViewById(R.id.detail_container) != null) {
            this.isTwoPane = true;
        }

        if (savedInstanceState == null) {
            showList();
        }

        int deviceId = this.getIntent().getIntExtra(EXTRA_DEVICE_ID, 0);
        if (deviceId > 0) {
            this.showDevice(deviceId);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        this.getApplicationContext().bindService(new Intent(this.getApplicationContext(), DeviceService.class), this.connection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onPause() {
        super.onPause();

        this.getApplicationContext().unbindService(this.connection);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            this.handleBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        this.handleBackPressed();
    }

    private void handleBackPressed() {
        if (!this.isTwoPane) {
            if (this.activeFragment instanceof DeviceDetailFragment) {
                this.showList();
                return;
            }
        }

        navigateUpTo(new Intent(this, SettingsActivity.class));
    }

    private void showList() {
        DeviceListFragment fragment = DeviceListFragment.newInstance();
        if (this.isTwoPane) {
            this.listFragment = fragment;
        }

        this.activeFragment = fragment;

        getSupportFragmentManager().beginTransaction()
                .replace(R.id.list_container, fragment)
                .commit();
    }

    private void showDevice(int deviceId) {
        this.switchFragment(DeviceDetailFragment.newInstance(deviceId));
    }

    private void switchFragment(Fragment fragment) {
        int container = this.isTwoPane ? R.id.detail_container : R.id.list_container;

        this.activeFragment = fragment;

        getSupportFragmentManager().beginTransaction()
                .replace(container, fragment)
                .commit();
    }

    @Override
    public void onDeviceSelected(DeviceEntity deviceEntity) {
        showDevice(deviceEntity.uid);
    }

    @Override
    public void onConnectionUpdate(DeviceConnection connection) {

    }

    @Override
    public void onDeviceDisconnect(DeviceEntity deviceEntity) {
        this.confirmDeviceAction(R.string.action_device_disconnect, R.string.action_device_disconnect_confirm, R.string.action_device_disconnect, (dialog, which) -> {
            if (this.deviceService.disconnectDevice(deviceEntity)) {
                Snackbar.make(findViewById(R.id.list_container), getString(R.string.action_device_disconnect_success, deviceEntity.displayName), Snackbar.LENGTH_LONG).show();
            } else {
                Snackbar.make(findViewById(R.id.list_container), getString(R.string.action_device_disconnect_failure, deviceEntity.displayName), Snackbar.LENGTH_LONG).show();
            }
        });
    }

    @Override
    public void onDeviceReboot(DeviceEntity deviceEntity) {
        this.confirmDeviceAction(R.string.action_device_reboot, R.string.action_device_reboot_confirm, R.string.action_device_reboot, (dialog, which) -> {
            // TODO: implement device reboot
            Snackbar.make(findViewById(R.id.list_container), "Reboot not implemented yet!", Snackbar.LENGTH_LONG).show();
        });
    }

    @Override
    public void onDeviceReset(DeviceEntity deviceEntity) {
        this.confirmDeviceAction(R.string.action_device_reset, R.string.action_device_reset_confirm, R.string.action_device_reset, (dialog, which) -> {
            // TODO: implement device reset
            Snackbar.make(findViewById(R.id.list_container), "Reset not implemented yet!", Snackbar.LENGTH_LONG).show();
        });
    }

    @Override
    public void onDeviceDeleted(DeviceEntity deviceEntity) {
        this.confirmDeviceAction(R.string.action_device_delete, R.string.action_device_delete_confirm, R.string.action_device_delete, (dialog, which) -> {
            this.deviceService.disconnectDevice(deviceEntity);

            DeviceRepository repo = new DeviceRepository(getApplication());
            repo.delete(deviceEntity);

            // TODO: maybe add undo button to snackbar (only if device is not reset as well)?
            Snackbar.make(findViewById(R.id.list_container), getString(R.string.action_device_delete_success, deviceEntity.displayName), Snackbar.LENGTH_LONG).show();
            getSupportFragmentManager().beginTransaction().remove(this.activeFragment).commit();
            showList();
        });
    }

    private void confirmDeviceAction(@StringRes int title, @StringRes int text, @StringRes int positiveButtonText, @NonNull DialogInterface.OnClickListener onSuccess) {
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(Html.fromHtml(this.getString(text)))
                .setPositiveButton(positiveButtonText, onSuccess)
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    @Override
    public void onFeatureSelected(String featureName) {
        // TODO: implement feature selection (open activity for provider)
        Snackbar.make(findViewById(R.id.list_container), "Feature selection not implemented yet!", Snackbar.LENGTH_LONG).show();
    }
}
