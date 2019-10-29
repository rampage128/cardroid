package de.jlab.cardroid.rules;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;

import com.google.android.material.snackbar.Snackbar;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import de.jlab.cardroid.R;
import de.jlab.cardroid.SettingsActivity;
import de.jlab.cardroid.rules.storage.ActionEntity;
import de.jlab.cardroid.rules.storage.ActionRepository;
import de.jlab.cardroid.rules.storage.EventEntity;
import de.jlab.cardroid.rules.storage.RuleDefinition;

public class RuleActivity extends AppCompatActivity implements FragmentActionListener {

    private boolean isTwoPane;

    private int eventId = 0;
    private Fragment activeFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rule_list);

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
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
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
            this.showEventDetails(this.eventId);
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
    public void onEventChange(int command, EventEntity eventEntity) {
        if (command == COMMAND_EDIT) {
            showEventDetails(eventEntity.uid);
        }
    }

    @Override
    public void onActionChange(int command, ActionEntity actionEntity) {
        switch (command) {
            case COMMAND_ADD:
                showActionDetails(0);
                break;
            case COMMAND_EDIT:
                showActionDetails(actionEntity.uid);
                break;
            case COMMAND_SAVE:
                Snackbar.make(findViewById(android.R.id.content), getString(R.string.action_saved), Snackbar.LENGTH_LONG).show();
            case COMMAND_CANCEL:
                showEventDetails(this.eventId);
                break;
            case COMMAND_DELETE:
                ActionRepository repository = new ActionRepository(getApplication());
                repository.delete(actionEntity);
                Snackbar.make(findViewById(android.R.id.content), getString(R.string.action_deleted), Snackbar.LENGTH_LONG).show();
                break;
        }
    }

    @Override
    public void onRuleChange(int command, RuleDefinition ruleDefinition) {
        // Nothing to do here. Rules are now live-data!
    }

    private void showActionDetails(int actionId) {
        Bundle arguments = new Bundle();
        arguments.putInt(ActionDetailFragment.ARG_ACTION_ID, actionId);
        arguments.putInt(RuleDetailFragment.ARG_ITEM_ID,
                getIntent().getIntExtra(RuleDetailFragment.ARG_ITEM_ID, this.eventId));

        ActionDetailFragment fragment = new ActionDetailFragment();
        fragment.setArguments(arguments);

        switchFragment(fragment);
    }

    private void showEventDetails(int eventId) {
        this.eventId = eventId;

        Bundle arguments = new Bundle();
        arguments.putInt(RuleDetailFragment.ARG_ITEM_ID,
                getIntent().getIntExtra(RuleDetailFragment.ARG_ITEM_ID, eventId));

        RuleDetailFragment fragment = new RuleDetailFragment();
        fragment.setArguments(arguments);

        switchFragment(fragment);
    }

    private void showList() {
        RuleListFragment fragment = new RuleListFragment();

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
