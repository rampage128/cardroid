package de.jlab.cardroid.utils.ui.FeatureActivity;

import android.content.Context;
import android.content.DialogInterface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import de.jlab.cardroid.R;
import de.jlab.cardroid.devices.DeviceType;
import de.jlab.cardroid.devices.storage.DeviceEntity;

public final class DeviceSelector {

    private List<DeviceEntity> deviceEntities = new ArrayList<>();
    private DeviceSelectionListener onDeviceSelected;

    public DeviceSelector(@NonNull DeviceSelectionListener onDeviceSelected) {
        this.onDeviceSelected = onDeviceSelected;
    }

    private void onDeviceSelected(DialogInterface dialogInterface, int index) {
        DeviceEntity deviceEntity = this.deviceEntities.get(index);
        this.onDeviceSelected.onDeviceSelected(deviceEntity);
        dialogInterface.dismiss();
    }

    public void updateDevices(@NonNull List<DeviceEntity> deviceEntities) {
        this.deviceEntities = deviceEntities;
    }

    public void show(@NonNull Context context) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setSingleChoiceItems(new DeviceAdapter(context, this.deviceEntities.toArray(new DeviceEntity[0])), -1, this::onDeviceSelected);
        builder.show();
    }

    public interface DeviceSelectionListener {
        void onDeviceSelected(@NonNull DeviceEntity deviceEntity);
    }

    private static class DeviceAdapter extends ArrayAdapter<DeviceEntity> {
        private static class ViewHolder {
            private TextView nameView;
            private TextView uidView;
            private ImageView iconView;
        }

        public DeviceAdapter(@NonNull Context context, @NonNull DeviceEntity[] objects) {
            super(context, 0, objects);
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            Context context = getContext();

            ViewHolder views;
            View listItem = convertView;
            if(listItem == null) {
                listItem = LayoutInflater.from(context).inflate(R.layout.listitem_device, parent, false); //android.R.layout.simple_list_item_single_choice
                views = new ViewHolder();
                views.nameView = listItem.findViewById(R.id.name);
                views.uidView = listItem.findViewById(R.id.uid);
                views.iconView = listItem.findViewById(R.id.type_icon);
                listItem.setTag(views);
            } else {
                views = (ViewHolder)listItem.getTag();
            }

            DeviceEntity deviceEntity = getItem(position);
            assert deviceEntity != null;
            DeviceType type = DeviceType.get(deviceEntity);

            views.nameView.setText(deviceEntity.displayName);
            views.uidView.setText(deviceEntity.deviceUid.toString());
            views.iconView.setImageResource(type.getTypeIcon());

            return listItem;
        }
    }

}
