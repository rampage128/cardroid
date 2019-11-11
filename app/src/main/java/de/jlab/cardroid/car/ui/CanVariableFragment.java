package de.jlab.cardroid.car.ui;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.Collections;

import androidx.annotation.NonNull;
import de.jlab.cardroid.R;
import de.jlab.cardroid.devices.DeviceService;
import de.jlab.cardroid.utils.UsageStatistics;
import de.jlab.cardroid.utils.ui.FeatureActivity.FeatureActivity;
import de.jlab.cardroid.utils.ui.UnitValues;
import de.jlab.cardroid.variables.Variable;

public final class CanVariableFragment extends FeatureActivity.FeatureFragment {

    private ListView variableListView;
    private VariableListAdapter variableListAdapter;

    private Variable.VariableChangeListener onVariableChange = this::onVariableChange;
    private ArrayList<Variable> canVariables = new ArrayList<>();

    private UsageStatistics variableStatistic = new UsageStatistics(1000, 60);
    private UsageStatistics.UsageStatisticsListener onVariableStatisticUpdate = this::onVariableStatisticUpdate;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.fragment_can_variables, container, false);

        this.variableListView = rootView.findViewById(R.id.carSystemListView);

        this.variableListAdapter = new VariableListAdapter(this.getContext());
        this.variableListView.setAdapter(this.variableListAdapter);

        return rootView;
    }

    private void onVariableChange(Object oldValue, Object newValue, String variableName) {
        this.variableStatistic.count();
        if (this.getActivity() != null) {
            this.getActivity().runOnUiThread(() -> this.variableListAdapter.update(variableName));
        }
    }

    private void onVariableStatisticUpdate(int count, UsageStatistics statistics) {
        String value = UnitValues.getStatisticString(count, Math.round(statistics.getAverage()), R.string.unit_hertz, getContext());
        this.updateDataItem(R.string.status_ups, value);
    }

    @Override
    protected void initDataItems(@NonNull DataItemConsumer consumer, @NonNull Context context) {
        consumer.put(R.string.car_status_variables, context.getResources().getString(R.string.status_unavailable));
        consumer.put(R.string.status_ups, context.getResources().getString(R.string.status_unavailable));
    }

    @Override
    public void onStart(@NonNull DeviceService.DeviceServiceBinder deviceService, @NonNull FeatureSubscriptionConsumer consumer) {
        Variable[] variables = deviceService.getVariableStore().getAll();
        for (Variable variable : variables) {
            // TODO: We only want can variables (for the selected device?) here. So maybe we should retrieve this from CanController instead of VariableStore?
            this.canVariables.add(variable);
            variable.addChangeListener(this.onVariableChange);
        }
        Collections.sort(this.canVariables, (o1, o2) -> o1.getName().compareTo(o2.getName()));

        this.variableStatistic.addListener(this.onVariableStatisticUpdate);

        this.variableListAdapter.update(this.canVariables);
        this.updateDataItem(R.string.car_status_variables, Integer.toString(this.variableListAdapter.getCount()));
    }

    @Override
    public void onStop(@NonNull DeviceService.DeviceServiceBinder deviceService, @NonNull FeatureUnsubscriptionConsumer consumer) {
        this.variableStatistic.removeListener(this.onVariableStatisticUpdate);
        for (Variable variable : this.canVariables) {
            variable.removeChangeListener(this.onVariableChange);
        }
        this.variableListAdapter.clear();
    }

}
