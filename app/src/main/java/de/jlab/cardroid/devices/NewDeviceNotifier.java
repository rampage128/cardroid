package de.jlab.cardroid.devices;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import de.jlab.cardroid.R;
import de.jlab.cardroid.devices.storage.DeviceEntity;

public final class NewDeviceNotifier {

    private static final String CHANNEL_ID = "new_devices";

    private Context context;

    public NewDeviceNotifier(Context context) {
        this.context = context;
    }

    public void notify(@NonNull DeviceEntity deviceEntity) {
        this.createNotificationChannel(context);

        // TODO: set notification tap action once device UI exists
        Notification notification = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(context.getString(R.string.notification_new_device_title))
                .setContentText(context.getString(R.string.notification_new_device_text, deviceEntity.displayName))
                .setStyle(new NotificationCompat.InboxStyle()
                        .addLine(context.getString(R.string.notification_new_device_detail_type, this.getDeviceTypeName(deviceEntity.className)))
                        .addLine(context.getString(R.string.notification_new_device_detail_uid, deviceEntity.deviceUid)))
                .build();

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);

        notificationManager.notify(deviceEntity.uid, notification);
    }

    private String getDeviceTypeName(String deviceClassName) {
        int identifier = context.getResources().getIdentifier("device_type_" + deviceClassName.toLowerCase(), "string", context.getPackageName());
        if (identifier == 0) {
            identifier = R.string.device_type_unknown;
        }
        return context.getString(identifier);
    }

    private void createNotificationChannel(Context context) {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = context.getString(R.string.notification_channel_new_devices_name);
            String description = context.getString(R.string.notification_channel_new_devices_description);
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

}
