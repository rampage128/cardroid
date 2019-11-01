package de.jlab.cardroid.utils.ui;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;

import androidx.annotation.CallSuper;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import de.jlab.cardroid.R;


public abstract class MasterDetailFlowActivity extends AppCompatActivity {

    private boolean isTwoPane = false;

    private Class<? extends Activity> parentActivity;

    private Fragment master;
    private Fragment active;

    @CallSuper
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.template_activity_master_detail);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setTitle(getTitle());

        this.parentActivity = getParentActivity();

        toggleToolbarNavigation(this.parentActivity != null);

        if (findViewById(R.id.detail_container) != null) {
            this.isTwoPane = true;
        }

        Fragment master = this.createMasterFragment();
        if (!(master instanceof MasterFragment)) {
            throw new IllegalArgumentException("Fragment " + master.getClass().getSimpleName() + " must implement MasterFragment.");
        }
        this.master = master;

        if (savedInstanceState == null || isTwoPane) {
            this.navigateToList();
        }
    }

    @CallSuper
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            this.navigateBack();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public final void onBackPressed() {
        this.navigateBack();
    }

    public final void navigateToList() {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();

        transaction.replace(R.id.list_container, this.master);

        if (this.active != null) {
            transaction.remove(this.active);
            this.active = null;
        }

        toggleToolbarNavigation(this.parentActivity != null);

        transaction.commit();
    }

    @CallSuper
    public void navigateBack() {
        if (this.active instanceof DetailFragment) {
            this.navigateToList();
            return;
        }

        if (this.parentActivity != null) {
            navigateUpTo(new Intent(this, this.parentActivity));
        }
    }

    public final void navigateTo(@NonNull Fragment fragment) {
        if (!(fragment instanceof DetailFragment || fragment instanceof MasterFragment)) {
            throw new IllegalArgumentException("Can not navigate to Fragment " + fragment.getClass().getSimpleName() + " because it does not implement interface DetailFragment.");
        }

        this.active = fragment;
        toggleToolbarNavigation(true);

        int container = this.isTwoPane ? R.id.detail_container : R.id.list_container;
        getSupportFragmentManager().beginTransaction()
                .replace(container, fragment)
                .commit();
    }

    private void toggleToolbarNavigation(boolean state) {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(state);
        }
    }

    protected Fragment getActiveFragment() {
        return this.active;
    }

    protected abstract Fragment createMasterFragment();
    protected abstract Class<? extends Activity> getParentActivity();

    public interface MasterFragment {}
    public interface DetailFragment {}

}
