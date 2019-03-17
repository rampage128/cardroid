package de.jlab.cardroid.rules.properties;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;
import java.util.Objects;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import de.jlab.cardroid.R;

public class AppSelectionPropertyEditor implements ActionPropertyEditor {

    @Override
    public void show(Property currentValue, final Context context, PropertyListener listener) {
        PackageManager packageManager = context.getPackageManager();
        Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
        mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        final List<ResolveInfo> apps = packageManager.queryIntentActivities( mainIntent, 0);

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setSingleChoiceItems(new AppAdapter(context, apps.toArray(new ResolveInfo[0])), -1, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int index) {
                ResolveInfo app = apps.get(index);

                listener.onItemSelected(new PropertyValue<>(app.activityInfo.packageName, app.loadLabel(context.getPackageManager()).toString()));
                dialogInterface.dismiss();
            }
        });
        builder.show();
    }

    private static class AppAdapter extends ArrayAdapter<ResolveInfo> {
        public AppAdapter(@NonNull Context context, @NonNull ResolveInfo[] objects) {
            super(context, 0, objects);
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            Context context = getContext();

            View listItem = convertView;
            if(listItem == null) {
                listItem = LayoutInflater.from(context).inflate(R.layout.list_item_app, parent, false); //android.R.layout.simple_list_item_single_choice
            }

            ResolveInfo app = getItem(position);
            assert app != null;

            TextView labelView = listItem.findViewById(R.id.text1);
            TextView packageView = listItem.findViewById(R.id.text2);
            ImageView iconView = listItem.findViewById(R.id.icon);
            labelView.setText(app.loadLabel(context.getPackageManager()));
            packageView.setText(app.activityInfo.applicationInfo.packageName);
            iconView.setImageDrawable(app.loadIcon(context.getPackageManager()));

            return listItem;
        }
    }

    @Override
    public PropertyValue getDefaultValue() {
        return null;
    }

    @Override
    public String getPropertyLabel(Property<?> property, Context context) {
        return Objects.toString(property.getValue());
    }

}
