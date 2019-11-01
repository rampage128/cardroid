package de.jlab.cardroid.devices.ui;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.text.Html;
import android.view.WindowManager;

import com.google.android.material.snackbar.Snackbar;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import de.jlab.cardroid.R;
import de.jlab.cardroid.SettingsActivity;
import de.jlab.cardroid.devices.Device;
import de.jlab.cardroid.devices.DeviceService;
import de.jlab.cardroid.devices.FeatureType;
import de.jlab.cardroid.devices.storage.DeviceEntity;
import de.jlab.cardroid.devices.storage.DeviceRepository;
import de.jlab.cardroid.utils.ui.MasterDetailFlowActivity;

public final class DeviceActivity extends MasterDetailFlowActivity implements DeviceListFragment.DeviceListInteractionListener, DeviceDetailFragment.DeviceDetailInteractionListener {

    public static final String EXTRA_DEVICE_ID = "deviceId";

    private DeviceService.DeviceServiceBinder deviceService;
    private ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            deviceService = (DeviceService.DeviceServiceBinder) service;
            // TODO observe device state and features and relay information to fragments
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            deviceService = null;
        }
    };

    @Override
    protected Fragment createMasterFragment() {
        return DeviceListFragment.newInstance();
    }

    @Override
    protected Class<? extends Activity> getParentActivity() {
        return SettingsActivity.class;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

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

    private void showDevice(int deviceId) {
        this.navigateTo(DeviceDetailFragment.newInstance(deviceId));
    }

    @Override
    public void onDeviceSelected(DeviceEntity deviceEntity) {
        showDevice(deviceEntity.uid);
    }

    @Override
    public void onDeviceDisconnect(DeviceEntity deviceEntity) {
        this.confirmDeviceAction(R.string.action_device_disconnect, R.string.action_device_disconnect_confirm, R.string.action_device_disconnect, (dialog, which) -> {
            Device device = this.deviceService.getDevice(deviceEntity.deviceUid);
            if (device != null) {
                device.close();
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
            Device device = this.deviceService.getDevice(deviceEntity.deviceUid);
            if (device != null) {
                device.close();
            }

            DeviceRepository repo = new DeviceRepository(getApplication());
            repo.delete(deviceEntity);

            // TODO: maybe add undo button to snackbar (only if device is not reset as well)?
            Snackbar.make(findViewById(R.id.list_container), getString(R.string.action_device_delete_success, deviceEntity.displayName), Snackbar.LENGTH_LONG).show();
            this.navigateToList();
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
    public void onFeatureSelected(FeatureType feature) {
        Intent intent = feature.getIntent(this);
        if (intent != null) {
            startActivity(intent);
        }
    }

}
