package de.jlab.cardroid.rules;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import com.google.android.material.textfield.TextInputLayout;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;
import de.jlab.cardroid.R;
import de.jlab.cardroid.rules.properties.ActionPropertyEditor;
import de.jlab.cardroid.rules.properties.Property;
import de.jlab.cardroid.rules.storage.ActionEntity;
import de.jlab.cardroid.rules.storage.EventEntity;
import de.jlab.cardroid.rules.storage.EventViewDetailModel;
import de.jlab.cardroid.rules.storage.RuleDefinition;
import de.jlab.cardroid.utils.ui.MasterDetailFlowActivity;

public class RuleDetailFragment extends Fragment implements MasterDetailFlowActivity.DetailFragment {

    public static final String ARG_ITEM_ID = "item_id";

    private EventViewDetailModel eventViewModel;
    private FragmentActionListener actionListener;

    private EditText eventNameInput;

    private RuleDefinition ruleEntity;

    public RuleDetailFragment() {
        // Mandatory empty constructor for automatic system instantiation
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);

        if (context instanceof FragmentActionListener) {
            this.actionListener = (FragmentActionListener)context;
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();

        if (this.ruleEntity != null) {
            EventEntity eventEntity = this.ruleEntity.event;
            String newName = this.eventNameInput.getText().toString();
            if (!Objects.equals(eventEntity.name, newName)) {
                eventEntity.name = newName;
                this.eventViewModel.updateEvent(eventEntity);
            }
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.rule_detail, container, false);

        setHasOptionsMenu(true);

        Bundle arguments = this.getArguments();
        if (arguments != null && arguments.containsKey(ARG_ITEM_ID)) {
            Activity activity = this.getActivity();
            assert activity != null;
            final Toolbar toolbar = activity.findViewById(R.id.toolbar);
            TextInputLayout eventNameInputLayout = rootView.findViewById(R.id.event_name);
            this.eventNameInput = eventNameInputLayout.getEditText();
            assert eventNameInput != null;

            RecyclerView recyclerView = rootView.findViewById(R.id.action_list);
            assert recyclerView != null;
            final ActionListAdapter adapter = new ActionListAdapter((FragmentActionListener)activity);
            recyclerView.setAdapter(adapter);

            if (toolbar != null) {
                toolbar.setTitle(getString(R.string.edit_rule));
            }

            new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
                @Override
                public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                    return false;
                }

                @Override
                public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                    int position = viewHolder.getAdapterPosition();
                    adapter.delete(position);
                }
            }).attachToRecyclerView(recyclerView);

            int eventUid = getArguments().getInt(ARG_ITEM_ID);
            eventViewModel = ViewModelProviders.of(this).get(EventViewDetailModel.class);
            eventViewModel.getAsRule(eventUid).observe(RuleDetailFragment.this, ruleEntity -> {
                if (ruleEntity != null) {
                    RuleDetailFragment.this.ruleEntity = ruleEntity;

                    RuleDetailFragment.this.eventNameInput.setText(ruleEntity.event.name);
                    adapter.setActions(ruleEntity.actions);

                    if (RuleDetailFragment.this.actionListener != null) {
                        RuleDetailFragment.this.actionListener.onRuleChange(FragmentActionListener.COMMAND_UPDATED, ruleEntity);
                    }
                }
            });
        }

        return rootView;
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.rule_detail_menu, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_add) {
            if (this.actionListener != null) {
                this.actionListener.onActionChange(FragmentActionListener.COMMAND_ADD, new ActionEntity());
            }
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public static class ActionListAdapter extends RecyclerView.Adapter<RuleDetailFragment.ActionListAdapter.ViewHolder> {

        private final FragmentActionListener listener;
        private List<ActionEntity> mValues;
        private final View.OnClickListener mOnClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ActionEntity item = (ActionEntity) view.getTag();
                listener.onActionChange(FragmentActionListener.COMMAND_EDIT, item);
            }
        };

        ActionListAdapter(FragmentActionListener listener) {
            this.listener = listener;
        }

        public void setActions(List<ActionEntity> actionEntities){
            mValues = actionEntities;
            notifyDataSetChanged();
        }

        public void delete(int position) {
            ActionEntity actionEntity = this.mValues.get(position);
            listener.onActionChange(FragmentActionListener.COMMAND_DELETE, actionEntity);
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.rule_list_content, parent, false);
            return new RuleDetailFragment.ActionListAdapter.ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull final RuleDetailFragment.ActionListAdapter.ViewHolder holder, int position) {
            ActionEntity actionEntity = mValues.get(position);

            String name = ActionDefinition.getLocalizedNameFromClassName(actionEntity.className, (Activity)this.listener);
            holder.nameView.setText(name);

            Action action = Action.createFromEntity(actionEntity);
            HashMap<String, ActionPropertyEditor> choices = action.getKnownProperties();
            StringBuilder detailText = new StringBuilder();
            for (Iterator<String> propertyNameIterator = choices.keySet().iterator(); propertyNameIterator.hasNext(); ) {
                String propertyName = propertyNameIterator.next();
                ActionPropertyEditor editor = choices.get(propertyName);
                assert editor != null;
                detailText.append(editor.getPropertyLabel(new Property(propertyName, action.getPropertyValue(propertyName)), (Activity) this.listener));
                if (propertyNameIterator.hasNext()) {
                    detailText.append(((Activity)this.listener).getString(R.string.property_delimiter));
                }
            }
            holder.detailView.setText(detailText);

            holder.itemView.setTag(actionEntity);
            holder.itemView.setOnClickListener(mOnClickListener);
        }

        @Override
        public int getItemCount() {
            return mValues != null ? mValues.size() : 0;
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            final TextView nameView;
            final TextView detailView;

            ViewHolder(View view) {
                super(view);
                this.nameView = view.findViewById(R.id.name);
                this.detailView = view.findViewById(R.id.content);
            }
        }
    }
}
