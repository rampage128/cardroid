package de.jlab.cardroid.utils.permissions;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import java.util.Objects;

import androidx.annotation.NonNull;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

public final class PermissionReceiver {

    private LocalBroadcastManager localBroadcastManager;
    private PermissionResultConsumer consumer;
    private String action;

    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (PermissionReceiver.this.action.equalsIgnoreCase(intent.getAction())) {
                PermissionReceiver.this.consumer.onPermissionsGranted();
                // TODO figure out a way to auto-remove the BroadcastReceiver. Then we can get rid of dispose()
            }
        }
    };

    public PermissionReceiver(@NonNull Context context, @NonNull Class caller, @NonNull PermissionResultConsumer consumer) {
        this.consumer = consumer;
        this.localBroadcastManager = LocalBroadcastManager.getInstance(context);
        this.action = Objects.requireNonNull(caller.getCanonicalName()).toUpperCase() + ".PERMISSIONS";

        IntentFilter filter = new IntentFilter();
        filter.addAction(action);
        this.localBroadcastManager.registerReceiver(this.receiver, filter);
    }

    public void dispose() {
        PermissionReceiver.this.localBroadcastManager.unregisterReceiver(this.receiver);
    }

    @NonNull
    public String getAction() {
        return this.action;
    }

    public boolean checkPermissions(@NonNull Context context, @NonNull PermissionRequest... permissionRequests) {
        return PermissionActivity.checkPermissions(context, permissionRequests);
    }

    public boolean requestPermissions(@NonNull Context context, @NonNull PermissionRequest... permissionRequests) {
        return PermissionActivity.requestPermissions(context, this, permissionRequests);
    }

    public static void notifyReceiver(@NonNull String action, @NonNull Context context) {
        LocalBroadcastManager localBroadcastManager = LocalBroadcastManager.getInstance(context);
        Intent allPermissionsGrantedIntent = new Intent(action);
        localBroadcastManager.sendBroadcast(allPermissionsGrantedIntent);
    }

    public interface PermissionResultConsumer {
        void onPermissionsGranted();
    }

}
