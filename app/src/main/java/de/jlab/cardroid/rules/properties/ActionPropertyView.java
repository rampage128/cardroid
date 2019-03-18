package de.jlab.cardroid.rules.properties;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import de.jlab.cardroid.R;

public class ActionPropertyView extends LinearLayout {

    private TextView labelText;
    private TextView valueText;

    private Property property;
    private ActionPropertyEditor editor;

    private OnChangeListener listener;

    public ActionPropertyView(@NonNull Property<?> property, ActionPropertyEditor editor, final Context context) {
        super(context);

        this.property = property;

        LayoutInflater inflater = LayoutInflater.from(context);
        inflater.inflate(R.layout.action_property, this);

        this.setOrientation(VERTICAL);

        this.labelText = this.findViewById(R.id.label);
        this.valueText = this.findViewById(R.id.value);

        this.labelText.setText(getLocalizedName(this.property.getKey(), getContext()));
        this.valueText.setText(editor.getPropertyLabel(this.property, getContext()));

        this.editor = editor;

        final ActionPropertyEditor.PropertyListener listener = new ActionPropertyEditor.PropertyListener() {
            @Override
            public void onItemSelected(PropertyValue newValue) {
                valueText.setText(newValue.getLabel(getContext()));
                ActionPropertyView.this.property = new Property<>(ActionPropertyView.this.property.getKey(), newValue.getValue());

                if (ActionPropertyView.this.listener != null) {
                    ActionPropertyView.this.listener.onChange(ActionPropertyView.this.property);
                }
            }
        };

        this.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                ActionPropertyView.this.editor.show(ActionPropertyView.this.property, context, listener);
            }
        });
    }

    public void setOnChangeListener(OnChangeListener listener) {
        this.listener = listener;
    }

    private String getLocalizedName(String key, Context context) {
        String identifier = key.replaceAll("([a-z])([A-Z])", "$1_$2").toLowerCase();
        int resId = context.getResources().getIdentifier(identifier, "string", context.getPackageName());
        if (resId > 0) {
            return context.getResources().getString(resId);
        }
        return identifier;
    }

    public interface OnChangeListener {
        void onChange(Property<?> newValue);
    }

}
