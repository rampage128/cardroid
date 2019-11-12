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
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.Spinner;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;
import de.jlab.cardroid.R;
import de.jlab.cardroid.rules.properties.ActionPropertyEditor;
import de.jlab.cardroid.rules.properties.ActionPropertyView;
import de.jlab.cardroid.rules.properties.Property;
import de.jlab.cardroid.rules.properties.PropertyValue;
import de.jlab.cardroid.rules.storage.ActionEntity;
import de.jlab.cardroid.rules.storage.ActionViewDetailModel;
import de.jlab.cardroid.utils.ui.MasterDetailFlowActivity;

public class ActionDetailFragment extends Fragment implements MasterDetailFlowActivity.DetailFragment {

    public static final String ARG_ACTION_ID = "action_id";

    private ActionViewDetailModel actionViewModel;
    private ActionEntity actionEntity;
    private int eventUid = 0;

    private Spinner actionType = null;
    private LinearLayout propertyContainer = null;

    private Map<String, Property<?>> properties = new HashMap<>();

    private FragmentActionListener actionListener;

    private MenuItem saveItem;

    public ActionDetailFragment() {
        // Mandatory empty constructor for automatic system instantiation
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        if (context instanceof FragmentActionListener) {
            this.actionListener = (FragmentActionListener)context;
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.action_detail, container, false);

        setHasOptionsMenu(true);

        this.actionType = rootView.findViewById(R.id.action_type_select);
        this.propertyContainer = rootView.findViewById(R.id.properties_container);

        final ActionTypeAdapter actionTypeAdapter = new ActionTypeAdapter(getContext(), Action.getActionDefinitions(getContext()));
        this.actionType.setAdapter(actionTypeAdapter);

        this.actionType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                ActionDefinition definition = (ActionDefinition)adapterView.getItemAtPosition(i);
                if (ActionDetailFragment.this.actionEntity != null) {
                    ActionDetailFragment.this.actionEntity.className = definition.getActionType().getCanonicalName();
                    updateProperties(definition.getActionType(), ActionDetailFragment.this.actionEntity);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

        this.eventUid = getArguments().getInt(RuleDetailFragment.ARG_ITEM_ID, 0);

        actionViewModel = ViewModelProviders.of(this).get(ActionViewDetailModel.class);

        int actionIdentifier = getArguments().getInt(ARG_ACTION_ID, 0);
        if (actionIdentifier > 0) {
            setTitle(getContext(), R.string.edit_action);

            actionViewModel.get(actionIdentifier).observe(ActionDetailFragment.this, new Observer<ActionEntity>() {
                @Override
                public void onChanged(@Nullable ActionEntity actionEntity) {
                    if (actionEntity != null) {
                        ActionDetailFragment.this.actionEntity = actionEntity;
                        try {
                            ActionDetailFragment.this.actionType.setSelection(actionTypeAdapter.getItemPosition(actionEntity.className));
                            Class<? extends Action> actionType = Class.forName(actionEntity.className).asSubclass(Action.class);
                            updateProperties(actionType, actionEntity);
                        } catch (ClassNotFoundException ex) {
                            // intentionally left blank TODO add error handling here?
                        }
                    }
                }
            });

        }
        else {
            setTitle(getContext(), R.string.add_action);

            this.actionEntity = new ActionEntity();
            this.actionEntity.eventUid = this.eventUid;
        }

        return rootView;
    }

    private void updateProperties(Class<? extends Action> actionType, ActionEntity actionEntity) {
        try {
            Action action = actionType.newInstance();
            HashMap<String, ActionPropertyEditor> propertyMap = action.getKnownProperties();

            this.properties.clear();
            this.propertyContainer.removeAllViews();

            for (final String key : propertyMap.keySet()) {
                ActionPropertyEditor editor = propertyMap.get(key);

                if (editor != null) {
                    PropertyValue defaultPropertyValue = editor.getDefaultValue();
                    Object defaultValue = defaultPropertyValue != null ? defaultPropertyValue.getValue() : null;
                    Property<?> property = new Property<>(key, defaultValue);
                    if (actionEntity != null) {
                        property = actionEntity.getPropertyByKey(key, defaultValue);
                    }

                    this.properties.put(key, property);

                    ActionPropertyView view = new ActionPropertyView(property, editor, getContext());
                    view.setOnChangeListener(new ActionPropertyView.OnChangeListener() {
                        @Override
                        public void onChange(Property<?> newValue) {
                            ActionDetailFragment.this.properties.put(newValue.getKey(), newValue);
                        }
                    });
                    this.propertyContainer.addView(view);
                }
            }
            this.propertyContainer.requestLayout();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (java.lang.InstantiationException e) {
            e.printStackTrace();
        }
    }

    private void setTitle(Context context, int resId) {
        Activity activity = this.getActivity();
        assert activity != null;
        Toolbar toolbar = activity.findViewById(R.id.toolbar);

        if (toolbar != null) {
            toolbar.setTitle(context.getString(resId));
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.action_detail_menu, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_save) {
            if (this.actionEntity.properties == null) {
                this.actionEntity.properties = new ArrayList<>();
            }

            this.actionEntity.properties.clear();
            this.actionEntity.properties.addAll(this.properties.values());

            if (this.actionEntity.uid == 0) {
                this.actionViewModel.insert(actionEntity);
            } else {
                this.actionViewModel.update(actionEntity);
            }

            if (this.actionListener != null) {
                this.actionListener.onActionChange(FragmentActionListener.COMMAND_SAVE, this.actionEntity);
            }

            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private static class ActionTypeAdapter extends ArrayAdapter<ActionDefinition> {
        public ActionTypeAdapter(@NonNull Context context, @NonNull ActionDefinition[] objects) {
            super(context, android.R.layout.simple_list_item_1, objects);
        }

        public int getItemPosition(String className) {
            for (int i = 0; i < this.getCount(); i++) {
                ActionDefinition definition = this.getItem(i);
                if (definition.getActionType().getCanonicalName().equals(className)) {
                    return i;
                }
            }
            return -1;
        }
    }
}
