package de.jlab.cardroid.devices.ui;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
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
import de.jlab.cardroid.devices.storage.DeviceEntity;

public final class DeviceActivity extends AppCompatActivity implements DeviceListFragment.DeviceListInteractionListener, DeviceDetailFragment.DeviceDetailInteractionListener {

    public static final String EXTRA_DEVICE_ID = "deviceId";

    private boolean isTwoPane;

    private Fragment activeFragment;

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

        // TODO: Bind service to get connected devices and allow interaction
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
    public void onDeviceDisconnect(DeviceEntity deviceEntity) {
        this.confirmDeviceAction(R.string.action_device_disconnect, R.string.action_device_disconnect_confirm, R.string.action_device_disconnect, (dialog, which) -> {
            // TODO: implement device disconnect
            Snackbar.make(findViewById(R.id.list_container), "Disconnect not implemented yet!", Snackbar.LENGTH_LONG).show();
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
            // TODO: implement device deletion
            Snackbar.make(findViewById(R.id.list_container), "Delete not implemented yet!", Snackbar.LENGTH_LONG).show();
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
}
