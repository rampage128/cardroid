package de.jlab.cardroid.car.ui;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.List;
import java.util.TreeMap;

import androidx.annotation.NonNull;
import de.jlab.cardroid.R;
import de.jlab.cardroid.variables.Expression;
import de.jlab.cardroid.variables.Variable;

public class VariableListAdapter extends BaseAdapter {

    private Context context;
    private TreeMap<String, Variable> variables = new TreeMap<>();
    private Variable[] variablesByIndex = new Variable[0];

    static class ViewHolder {
        TextView name;
        TextView value;
        TextView expression;
    }

    public VariableListAdapter(Context context) {
        this.context = context;
    }

    public void clear() {
        this.variables.clear();
        this.variablesByIndex = new Variable[0];
    }

    public void update(List<Variable> variables) {
        for (Variable variable : variables) {
            this.variables.put(variable.getName(), variable);
        }
        this.variablesByIndex = this.variables.values().toArray(new Variable[0]);
        this.notifyDataSetChanged();
    }

    public void update(@NonNull String variableName) {
        boolean isPresent = this.variables.containsKey(variableName);
        if (isPresent) {
            this.notifyDataSetChanged();
        }
    }

    @Override
    public int getCount() {
        return this.variables.size();
    }

    @Override
    public Object getItem(int position) {
        return this.variablesByIndex[position];
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        Variable variable = this.variablesByIndex[position];

        ViewHolder holder;
        if (convertView == null) {
            holder = new ViewHolder();
            final LayoutInflater layoutInflater = LayoutInflater.from(this.context);
            convertView = layoutInflater.inflate(R.layout.listitem_carduino_variable, null);
            holder.name = convertView.findViewById(R.id.name);
            holder.value = convertView.findViewById(R.id.value);
            holder.expression = convertView.findViewById(R.id.expression);
            convertView.setTag(holder);
        }
        else {
            holder = (ViewHolder)convertView.getTag();
        }

        if (variable.getValue() instanceof Expression) {
            holder.expression.setText(((Expression)variable.getValue()).getExpression());
        } else {
            holder.expression.setText("");
        }

        holder.value.setText(variable.getValue().getValue().toString());
        holder.name.setText(variable.getName());

        return convertView;
    }

}
