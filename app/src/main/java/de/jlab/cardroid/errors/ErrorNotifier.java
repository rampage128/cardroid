package de.jlab.cardroid.errors;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;

import java.util.Arrays;
import java.util.LinkedHashMap;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import de.jlab.cardroid.R;

public class ErrorNotifier implements ErrorDataProvider.ErrorListener {

    private static final String CHANNEL_ID = "errors";

    private Context context;
    private LinkedHashMap<Byte, Error> errors = new LinkedHashMap<>();

    public ErrorNotifier(Context context) {
        this.context = context;
    }

    @Override
    public void onError(Error error) {
        this.createNotificationChannel(context);

        Error[] errors = this.errors.values().toArray(new Error[0]);
        Arrays.sort(errors, (e1, e2) -> (int)(e1.getLastOccurence() - e2.getLastOccurence()));

        NotificationCompat.InboxStyle inboxStyle = new NotificationCompat.InboxStyle();
        for (Error listError : errors) {
            String errorCode = listError.getErrorCode();
            String errorMessage = this.getErrorMessage(errorCode);

            inboxStyle.addLine(context.getString(R.string.carduino_error, errorCode, errorMessage, listError.getCount()));
        }

        Notification notification = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(context.getString(R.string.notification_error_title, errors.length))
                .setContentText(context.getString(R.string.notification_error_text, this.getErrorMessage(error.getErrorCode())))
                .setOnlyAlertOnce(true)
                .setStyle(inboxStyle)
                .build();

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);

        notificationManager.notify(0, notification);
    }

    private String getErrorMessage(String errorCode) {
        int identifier = context.getResources().getIdentifier("carduino_error_" + errorCode, "string", context.getPackageName());
        if (identifier == 0) {
            identifier = R.string.carduino_error_unknown;
        }
        return context.getString(identifier);
    }

    private void createNotificationChannel(Context context) {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = context.getString(R.string.notification_channel_error_name);
            String description = context.getString(R.string.notification_channel_error_description);
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
