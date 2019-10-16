package de.jlab.cardroid.devices.ui;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import de.jlab.cardroid.R;
import de.jlab.cardroid.SettingsActivity;
import de.jlab.cardroid.devices.storage.DeviceEntity;
import de.jlab.cardroid.rules.ActionDetailFragment;
import de.jlab.cardroid.rules.RuleDetailFragment;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;

import com.google.android.material.snackbar.Snackbar;

public final class DeviceActivity extends AppCompatActivity implements DeviceListFragment.DeviceListInteractionListener {

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
        if (this.activeFragment instanceof ActionDetailFragment) {
            //this.showEventDetails(this.eventId);
            return;
        }

        if (!this.isTwoPane) {
            if (this.activeFragment instanceof RuleDetailFragment) {
                this.showList();
                return;
            }
        }

        navigateUpTo(new Intent(this, SettingsActivity.class));
    }

    @Override
    public void onDeviceSelected(DeviceEntity deviceEntity) {
        // TODO open detail view
        Snackbar.make(findViewById(R.id.list_container), "Device " + deviceEntity.deviceUid + " selected!", Snackbar.LENGTH_LONG).show();
    }

    private void showList() {
        DeviceListFragment fragment = DeviceListFragment.newInstance();

        this.activeFragment = fragment;

        getSupportFragmentManager().beginTransaction()
                .replace(R.id.list_container, fragment)
                .commit();
    }

    private void switchFragment(Fragment fragment) {
        int container = this.isTwoPane ? R.id.detail_container : R.id.list_container;

        this.activeFragment = fragment;

        getSupportFragmentManager().beginTransaction()
                .replace(container, fragment)
                .commit();
    }



}
