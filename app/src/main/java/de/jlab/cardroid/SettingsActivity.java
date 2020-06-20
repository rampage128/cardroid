package de.jlab.cardroid;

import android.app.Application;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.InputType;
import android.view.MenuItem;
import android.view.WindowManager;

import java.util.List;
import java.util.Objects;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.EditTextPreference;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SwitchPreference;
import de.jlab.cardroid.devices.DeviceService;
import de.jlab.cardroid.devices.DeviceServiceConnection;
import de.jlab.cardroid.devices.storage.DeviceEntity;
import de.jlab.cardroid.devices.storage.DeviceRepository;
import de.jlab.cardroid.overlay.DemoOverlayController;
import de.jlab.cardroid.overlay.OverlayController;
import de.jlab.cardroid.utils.permissions.PermissionReceiver;

public final class SettingsActivity extends AppCompatActivity {

    private static final EditTextPreference.OnBindEditTextListener INPUT_INT_UNSIGNED = editText -> editText.setInputType(InputType.TYPE_CLASS_NUMBER);
    private static final EditTextPreference.OnBindEditTextListener INPUT_INT_SIGNED = editText -> editText.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_SIGNED);

    private DeviceServiceConnection serviceConnection = new DeviceServiceConnection(this::serviceAction);
    private OverlayController overlayController = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.settings_container, new MainScreen())
                .commit();

        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    @Override
    protected void onResume() {
        super.onResume();

        this.serviceConnection.bind(this.getApplicationContext());
    }

    @Override
    protected void onPause() {
        super.onPause();

        this.serviceConnection.unbind(this.getApplicationContext());
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            this.onBackPressed();
            return true;
        }
        return false;
    }

    private void serviceAction(@NonNull DeviceService.DeviceServiceBinder deviceService, @NonNull DeviceServiceConnection.Action action) {
        if (action == DeviceServiceConnection.Action.BOUND) {
            this.overlayController = deviceService.getOverlayController();
        } else {
            this.overlayController = null;
        }
    }

    private void showOverlay() {
        if (this.overlayController != null) {
            this.overlayController.start();
        }
    }

    private void hideOverlay() {
        if (this.overlayController != null) {
            this.overlayController.stop();
        }
    }

    // Settings screen implementations

    public static class MainScreen extends PreferenceFragmentCompat {
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.pref_main, rootKey);

            SettingsActivity activity = (SettingsActivity)Objects.requireNonNull(getActivity());

            ListPreference overlayDevicePreference = findPreference("overlay_device_uid");
            new DeviceListTask(activity.getApplication()).execute(overlayDevicePreference);
            Objects.requireNonNull(overlayDevicePreference).setOnPreferenceChangeListener(this::onDeviceSelected);

            ListPreference carDevicePreference = findPreference("car_device_uid");
            new DeviceListTask(activity.getApplication()).execute(carDevicePreference);
            Objects.requireNonNull(carDevicePreference).setOnPreferenceChangeListener(this::onDeviceSelected);

            ListPreference gpsDevicePreference = findPreference("gps_device_uid");
            new DeviceListTask(activity.getApplication()).execute(gpsDevicePreference);
            Objects.requireNonNull(gpsDevicePreference).setOnPreferenceChangeListener(this::onDeviceSelected);
        }

        private boolean onDeviceSelected(Preference preference, Object newValue) {
            ListPreference list = (ListPreference)preference;

            int index = 0;
            Object[] values = list.getEntryValues();
            for (int i = 0; i < values.length; i++) {
                if (Objects.equals(values[i], newValue)) {
                    index = i;
                }
            }

            preference.setSummary(list.getEntries()[index]);
            return true;
        }
    }

    @SuppressWarnings("unused")
    public static class OverlayScreen extends PreferenceFragmentCompat {
        private PermissionReceiver overlayPermissionReceiver;
        private SwitchPreference overlayPermissionPreference;
        private DemoOverlayController demoOverlayController;

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.pref_overlay, rootKey);

            SettingsActivity activity = (SettingsActivity)Objects.requireNonNull(getActivity());

            this.overlayPermissionReceiver = new PermissionReceiver(activity, this.getClass(), this::overlayPermissionGranted);

            overlayPermissionPreference = findPreference("overlay_active");
            Objects.requireNonNull(this.overlayPermissionPreference).setOnPreferenceChangeListener(this::onOverlayToggle);

            setTextInputType(findPreference("overlay_volume_touch_duration"), INPUT_INT_UNSIGNED);
            setTextInputType(findPreference("overlay_temperature_min"), INPUT_INT_UNSIGNED);
            setTextInputType(findPreference("overlay_temperature_max"), INPUT_INT_UNSIGNED);
            setTextInputType(findPreference("overlay_fan_max"), INPUT_INT_UNSIGNED);

            Preference showDemo = findPreference("overlay_demo_show");
            Objects.requireNonNull(showDemo).setOnPreferenceClickListener(this::showDemoOverlay);

            Preference resetPosition = findPreference("overlay_position_reset");
            Objects.requireNonNull(resetPosition).setOnPreferenceClickListener(this::resetOverlayPosition);

            this.demoOverlayController = new DemoOverlayController(activity);
        }

        private boolean resetOverlayPosition(Preference preference) {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this.getContext());
            SharedPreferences.Editor editor = prefs.edit();
            editor.remove("overlay_bubble_x");
            editor.remove("overlay_bubble_y");
            editor.apply();

            return true;
        }

        private boolean showDemoOverlay(Preference preference) {
            if (this.demoOverlayController.isRunning()) {
                this.demoOverlayController.stop();
            } else {
                this.demoOverlayController.start();
            }
            return true;
        }

        private boolean onOverlayToggle(Preference preference, Object newValue) {
            SettingsActivity activity = (SettingsActivity)Objects.requireNonNull(getActivity());
            if (((Boolean)newValue)) {
                if (this.overlayPermissionReceiver.requestPermissions(activity, OverlayController.PERMISSIONS)) {
                    activity.showOverlay();
                    return true;
                } else {
                    return false;
                }
            }
            else {
                activity.hideOverlay();
            }
            return true;
        }

        private void overlayPermissionGranted() {
            Objects.requireNonNull((SettingsActivity)getActivity()).showOverlay();
            if (!this.overlayPermissionPreference.isChecked()) {
                this.overlayPermissionPreference.setChecked(true);
            }
        }

        @Override
        public void onResume(){
            super.onResume();

            if (!this.overlayPermissionReceiver.checkPermissions(Objects.requireNonNull(getActivity()), OverlayController.PERMISSIONS)) {
                overlayPermissionPreference.setChecked(false);
            }
        }

        @Override
        public void onPause() {
            super.onPause();

            this.demoOverlayController.stop();
        }

        @Override
        public void onDestroy() {
            super.onDestroy();

            this.overlayPermissionReceiver.dispose();
            this.demoOverlayController.dispose();
        }
    }

    public static class CompatibilityScreen extends PreferenceFragmentCompat {

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.pref_compatibility, rootKey);

            setTextInputType(findPreference("device_detection_delay"), INPUT_INT_UNSIGNED);
            setTextInputType(findPreference("device_detection_timeout"), INPUT_INT_UNSIGNED);
        }

    }

    // Helpers

    private static void setTextInputType(@Nullable EditTextPreference preference, @NonNull EditTextPreference.OnBindEditTextListener inputType) {
        if (preference != null) {
            preference.setOnBindEditTextListener(inputType);
        }
    }

    private static class DeviceListTask extends AsyncTask<ListPreference, Void, Void> {

        private Application application;

        public DeviceListTask(@NonNull Application application) {
            this.application = application;
        }

        @Override
        protected Void doInBackground(ListPreference... preferences) {
            DeviceRepository deviceRepo = new DeviceRepository(this.application);
            List<DeviceEntity> deviceEntities = deviceRepo.getAllSynchronous();
            String[] deviceNames = new String[deviceEntities.size() + 1];
            String[] deviceUids = new String[deviceEntities.size() + 1];

            int lastIndex = deviceEntities.size();

            deviceUids[lastIndex] = "";
            deviceNames[lastIndex] = this.application.getString(R.string.pref_device_none);

            for (ListPreference preference : preferences) {
                int selectedIndex = lastIndex;
                for (int i = 0; i < deviceEntities.size(); i++) {
                    DeviceEntity deviceEntity = deviceEntities.get(i);
                    deviceNames[i] = deviceEntity.displayName;
                    deviceUids[i] = deviceEntity.deviceUid.toString();
                    if (Objects.equals(preference.getValue(), deviceEntity.deviceUid.toString())) {
                        selectedIndex = i;
                    }
                }

                preference.setEntries(deviceNames);
                preference.setEntryValues(deviceUids);
                preference.setSummary(deviceNames[selectedIndex]);
            }

            return null;
        }
    }

}
