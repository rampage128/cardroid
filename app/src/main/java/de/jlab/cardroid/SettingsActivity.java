package de.jlab.cardroid;


import android.annotation.TargetApi;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.SwitchPreference;
import android.provider.Settings;
import android.view.MenuItem;
import android.view.WindowManager;
import android.widget.Toast;

import java.util.List;
import java.util.Objects;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import de.jlab.cardroid.devices.DeviceService;
import de.jlab.cardroid.devices.DeviceServiceConnection;
import de.jlab.cardroid.devices.storage.DeviceEntity;
import de.jlab.cardroid.devices.storage.DeviceRepository;
import de.jlab.cardroid.overlay.OverlayWindow;

/**
 * FIXME: Migrate this legacy crap to androix.preferences
 * A {@link PreferenceActivity} that presents a set of application settings. On
 * handset devices, settings are presented as a single list. On tablets,
 * settings are split by category, with category headers shown to the left of
 * the list of settings.
 * <p>
 * See <a href="http://developer.android.com/design/patterns/settings.html">
 * Android Design: Settings</a> for design guidelines and the <a
 * href="http://developer.android.com/guide/topics/ui/settings.html">Settings
 * API Guide</a> for more information on developing a Settings UI.
 */
public final class SettingsActivity extends AppCompatPreferenceActivity {

    private DeviceServiceConnection serviceConnection = new DeviceServiceConnection(this::serviceAction);
    private OverlayWindow overlay = null;

    /**
     * A preference value change listener that updates the preference's summary
     * to reflect its new value.
     */
    private static Preference.OnPreferenceChangeListener sBindPreferenceSummaryToValueListener = new Preference.OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, Object value) {
            String stringValue = value.toString();

            if (preference instanceof ListPreference) {
                // For list preferences, look up the correct display value in
                // the preference's 'entries' list.
                ListPreference listPreference = (ListPreference) preference;
                int index = listPreference.findIndexOfValue(stringValue);

                // Set the summary to reflect the new value.
                preference.setSummary(
                        index >= 0
                                ? listPreference.getEntries()[index]
                                : null);

            } else {
                // For all other preferences, set the summary to the value's
                // simple string representation.
                preference.setSummary(stringValue);
            }
            return true;
        }
    };

    /**
     * Helper method to determine if the device has an extra-large screen. For
     * example, 10" tablets are extra-large.
     */
    private static boolean isXLargeTablet(Context context) {
        return (context.getResources().getConfiguration().screenLayout
                & Configuration.SCREENLAYOUT_SIZE_MASK) >= Configuration.SCREENLAYOUT_SIZE_XLARGE;
    }

    /**
     * Binds a preference's summary to its value. More specifically, when the
     * preference's value is changed, its summary (line of text below the
     * preference title) is updated to reflect the value. The summary is also
     * immediately updated upon calling this method. The exact display format is
     * dependent on the type of preference.
     *
     * @see #sBindPreferenceSummaryToValueListener
     */
    private static void bindPreferenceSummaryToValue(Preference preference) {
        // Set the listener to watch for value changes.
        preference.setOnPreferenceChangeListener(sBindPreferenceSummaryToValueListener);

        // Trigger the listener immediately with the preference's
        // current value.
        sBindPreferenceSummaryToValueListener.onPreferenceChange(preference,
                PreferenceManager
                        .getDefaultSharedPreferences(preference.getContext())
                        .getString(preference.getKey(), ""));
    }

    private void serviceAction(@NonNull DeviceService.DeviceServiceBinder deviceService, @NonNull DeviceServiceConnection.Action action) {
        if (action == DeviceServiceConnection.Action.BOUND) {
            this.overlay = deviceService.getOverlay();
        } else {
            this.overlay = null;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupActionBar();
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

    /**
     * Set up the {@link android.app.ActionBar}, if the API is available.
     */
    private void setupActionBar() {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            // Show the Up button in the action bar.
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    private void showOverlay() {
        if (this.overlay != null) {
            this.overlay.create();
        }
    }

    private void hideOverlay() {
        if (this.overlay != null) {
            this.overlay.destroy();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean onIsMultiPane() {
        return isXLargeTablet(this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public void onBuildHeaders(List<Header> target) {
        loadHeadersFromResource(R.xml.pref_headers, target);
    }

    /**
     * This method stops fragment injection in malicious applications.
     * Make sure to deny any unknown fragments here.
     */
    protected boolean isValidFragment(String fragmentName) {
        return PreferenceFragment.class.getName().equals(fragmentName)
                || OverlayPreferenceFragment.class.getName().equals(fragmentName)
                || CarPreferenceFragment.class.getName().equals(fragmentName)
                || GpsPreferenceFragment.class.getName().equals(fragmentName)
                || PowerPreferenceFragment.class.getName().equals(fragmentName);
    }

    /**
     * This fragment shows overlay preferences only. It is used when the
     * activity is showing a two-pane settings UI.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static class OverlayPreferenceFragment extends PreferenceFragment {
        private static final int CODE_DRAW_OVER_OTHER_APP_PERMISSION = 2084;

        private SwitchPreference overlayPermissionPreference;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_overlay);
            setHasOptionsMenu(true);

            bindPreferenceSummaryToValue(findPreference("overlay_temperature_max"));
            bindPreferenceSummaryToValue(findPreference("overlay_temperature_min"));
            bindPreferenceSummaryToValue(findPreference("overlay_fan_max"));
            bindPreferenceSummaryToValue(findPreference("overlay_device_uid"));

            ListPreference devicePreference = (ListPreference)findPreference("overlay_device_uid");
            new DeviceListTask(getActivity().getApplication()).execute(devicePreference);

            overlayPermissionPreference = (SwitchPreference)findPreference("overlay_active");

            overlayPermissionPreference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    if (((Boolean)newValue)) {
                        if (canDrawOverlay()) {
                            ((SettingsActivity)getActivity()).showOverlay();
                            return true;
                        } else {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                // Open screen to grant overlay permission
                                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                        Uri.parse("package:" + getActivity().getPackageName()));
                                startActivityForResult(intent, CODE_DRAW_OVER_OTHER_APP_PERMISSION);
                            }
                            return false;
                        }
                    }
                    else {
                        ((SettingsActivity)getActivity()).hideOverlay();
                    }
                    return true;
                }
            });
        }

        @Override
        public void onResume(){
            super.onResume();

            if (!canDrawOverlay() && overlayPermissionPreference.isChecked()) {
                overlayPermissionPreference.setChecked(false);
            }
        }

        @Override
        public void onActivityResult(int requestCode, int resultCode, Intent data) {
            if (requestCode == CODE_DRAW_OVER_OTHER_APP_PERMISSION) {
                //Check if the permission is granted or not.
                if (canDrawOverlay()) {
                    ((SettingsActivity)getActivity()).showOverlay();
                    if (!overlayPermissionPreference.isChecked()) {
                        overlayPermissionPreference.setChecked(true);
                    }
                }
                //Permission is not available
                else {
                    Toast.makeText(getActivity(), R.string.overlay_permission_missing,
                            Toast.LENGTH_LONG).show();
                    if (overlayPermissionPreference.isChecked()) {
                        overlayPermissionPreference.setChecked(false);
                    }
                }
            } else {
                super.onActivityResult(requestCode, resultCode, data);
            }
        }

        private boolean canDrawOverlay() {
            return Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(getActivity());
        }

        @Override
        public boolean onOptionsItemSelected(MenuItem item) {
            int id = item.getItemId();
            if (id == android.R.id.home) {
                startActivity(new Intent(getActivity(), SettingsActivity.class));
                return true;
            }
            return super.onOptionsItemSelected(item);
        }
    }

    /**
     * This fragment shows usb preferences only. It is used when the
     * activity is showing a two-pane settings UI.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static class CarPreferenceFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_car);
            setHasOptionsMenu(true);
        }

        @Override
        public boolean onOptionsItemSelected(MenuItem item) {
            int id = item.getItemId();
            if (id == android.R.id.home) {
                startActivity(new Intent(getActivity(), SettingsActivity.class));
                return true;
            }
            return super.onOptionsItemSelected(item);
        }
    }

    /**
     * This fragment shows gps preferences only. It is used when the
     * activity is showing a two-pane settings UI.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static class GpsPreferenceFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_gps);
            setHasOptionsMenu(true);
            bindPreferenceSummaryToValue(findPreference("gps_device_uid"));

            ListPreference devicePreference = (ListPreference)findPreference("gps_device_uid");
            new DeviceListTask(getActivity().getApplication()).execute(devicePreference);
        }

        @Override
        public boolean onOptionsItemSelected(MenuItem item) {
            int id = item.getItemId();
            if (id == android.R.id.home) {
                startActivity(new Intent(getActivity(), SettingsActivity.class));
                return true;
            }
            return super.onOptionsItemSelected(item);
        }
    }

    /**
     * This fragment shows power preferences only. It is used when the
     * activity is showing a two-pane settings UI.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static class PowerPreferenceFragment extends PreferenceFragment {
        private static final int CODE_TOGGLE_DISPLAY = 2084;

        SwitchPreference toggleScreenPreference;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_power);
            setHasOptionsMenu(true);

            toggleScreenPreference = (SwitchPreference)findPreference("power_toggle_screen");

            toggleScreenPreference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    if (((Boolean)newValue)) {
                        if (canChangeSettings()) {
                            return true;
                        } else {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                Intent permissionIntent = new Intent(android.provider.Settings.ACTION_MANAGE_WRITE_SETTINGS);
                                permissionIntent.setData(Uri.parse("package:" + getContext().getPackageName()));
                                startActivityForResult(permissionIntent, CODE_TOGGLE_DISPLAY);
                            }
                            return false;
                        }
                    }
                    return true;
                }
            });
        }

        @Override
        public void onResume(){
            super.onResume();

            if (!canChangeSettings() && toggleScreenPreference.isChecked()) {
                toggleScreenPreference.setChecked(false);
            }
        }

        @Override
        public void onActivityResult(int requestCode, int resultCode, Intent data) {
            if (requestCode == CODE_TOGGLE_DISPLAY) {
                //Check if the permission is granted or not.
                if (canChangeSettings()) {
                    if (!toggleScreenPreference.isChecked()) {
                        toggleScreenPreference.setChecked(true);
                    }
                }
                //Permission is not available
                else {
                    Toast.makeText(getActivity(), R.string.write_settings_permission_missing,
                            Toast.LENGTH_LONG).show();
                    if (toggleScreenPreference.isChecked()) {
                        toggleScreenPreference.setChecked(false);
                    }
                }
            } else {
                super.onActivityResult(requestCode, resultCode, data);
            }
        }

        @Override
        public void onPause() {
            super.onPause();
        }

        @Override
        public boolean onOptionsItemSelected(MenuItem item) {
            int id = item.getItemId();
            if (id == android.R.id.home) {
                startActivity(new Intent(getActivity(), SettingsActivity.class));
                return true;
            }
            return super.onOptionsItemSelected(item);
        }

        private boolean canChangeSettings() {
            return Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.System.canWrite(getContext());
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
