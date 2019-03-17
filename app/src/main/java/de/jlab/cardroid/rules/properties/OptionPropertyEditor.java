package de.jlab.cardroid.rules.properties;

import android.content.Context;
import android.content.DialogInterface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.Objects;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

public class OptionPropertyEditor implements ActionPropertyEditor {

    private PropertyValue[] choices;
    private PropertyValue defaultValue;

    public OptionPropertyEditor(PropertyValue[] choices, int defaultChoice) {
        this.choices = choices;
        this.defaultValue = defaultChoice > 0 ? choices[defaultChoice] : null;
    }

    @Override
    public PropertyValue getDefaultValue() {
        return this.defaultValue;
    }

    @Override
    public void show(Property currentValue, Context context, final PropertyListener listener) {

        int selectedItem = -1;
        if (currentValue != null) {
            for (int i = 0; i < this.choices.length; i++) {
                if (Objects.equals(this.choices[i].getValue(), currentValue.getValue())) {
                    selectedItem = i;
                    break;
                }
            }
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setSingleChoiceItems(new OptionAdapter(context, this.choices), selectedItem, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int index) {
                listener.onItemSelected(OptionPropertyEditor.this.choices[index]);
                dialogInterface.dismiss();
            }
        });
        builder.show();
    }

    private static class OptionAdapter extends ArrayAdapter<PropertyValue> {
        public OptionAdapter(@NonNull Context context, @NonNull PropertyValue[] objects) {
            super(context, 0, objects);
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            Context context = getContext();

            View listItem = convertView;
            if(listItem == null) {
                listItem = LayoutInflater.from(context).inflate(android.R.layout.simple_list_item_single_choice, parent, false);
            }

            PropertyValue value = getItem(position);

            TextView label = listItem.findViewById(android.R.id.text1);
            label.setText(value.getLabel(context));

            return listItem;
        }
    }

    @Override
    public String getPropertyLabel(Property<?> property, Context context) {
        for (PropertyValue<?> choice : this.choices) {
            if (Objects.equals(choice.getValue(), property.getValue())) {
                return choice.getLabel(context);
            }
        }

        return Objects.toString(property.getValue());
    }
}
