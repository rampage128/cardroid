package de.jlab.cardroid.devices;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.app.TaskStackBuilder;
import de.jlab.cardroid.R;
import de.jlab.cardroid.devices.storage.DeviceEntity;
import de.jlab.cardroid.devices.ui.DeviceActivity;

public final class NewDeviceNotifier {

    private static final String CHANNEL_ID = "new_devices";

    private Context context;

    public NewDeviceNotifier(Context context) {
        this.context = context;
    }

    public void notify(@NonNull DeviceEntity deviceEntity) {
        this.createNotificationChannel(context);

        Intent notificationIntent = new Intent(context, DeviceActivity.class);
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
        stackBuilder.addNextIntentWithParentStack(notificationIntent);
        notificationIntent.putExtra(DeviceActivity.EXTRA_DEVICE_ID, deviceEntity.uid);
        notificationIntent.putExtra(DeviceActivity.EXTRA_DEVICE_UID, deviceEntity.deviceUid.toString());
        PendingIntent pendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_CANCEL_CURRENT | PendingIntent.FLAG_ONE_SHOT);

        Notification notification = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
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
