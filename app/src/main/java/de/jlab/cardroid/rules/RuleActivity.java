package de.jlab.cardroid.rules;

import android.app.Activity;
import android.os.Bundle;
import android.view.WindowManager;

import com.google.android.material.snackbar.Snackbar;

import androidx.fragment.app.Fragment;
import de.jlab.cardroid.R;
import de.jlab.cardroid.SettingsActivity;
import de.jlab.cardroid.rules.storage.ActionEntity;
import de.jlab.cardroid.rules.storage.ActionRepository;
import de.jlab.cardroid.rules.storage.EventEntity;
import de.jlab.cardroid.rules.storage.RuleDefinition;
import de.jlab.cardroid.utils.ui.MasterDetailFlowActivity;

public class RuleActivity extends MasterDetailFlowActivity implements FragmentActionListener {

    private int eventId = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        // TODO Allow directly opening a rule when the activity is started with a rule-id
    }

    @Override
    protected Fragment createMasterFragment() {
        return new RuleListFragment();
    }

    @Override
    protected Class<? extends Activity> getParentActivity() {
        return SettingsActivity.class;
    }

    @Override
    public void navigateBack() {
        if (this.getActiveFragment() instanceof ActionDetailFragment) {
            this.showEventDetails(this.eventId);
        } else {
            super.navigateBack();
        }
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

        this.navigateTo(fragment);
    }

    private void showEventDetails(int eventId) {
        this.eventId = eventId;

        Bundle arguments = new Bundle();
        arguments.putInt(RuleDetailFragment.ARG_ITEM_ID,
                getIntent().getIntExtra(RuleDetailFragment.ARG_ITEM_ID, eventId));

        RuleDetailFragment fragment = new RuleDetailFragment();
        fragment.setArguments(arguments);

        this.navigateTo(fragment);
    }

}
