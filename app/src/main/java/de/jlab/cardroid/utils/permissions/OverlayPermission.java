package de.jlab.cardroid.utils.permissions;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;

import androidx.annotation.NonNull;

public final class OverlayPermission extends Permission {

    public static final String PERMISSION_KEY = "android.settings.action.MANAGE_OVERLAY_PERMISSION";

    public OverlayPermission(@NonNull Constraint constraint, int usage) {
        super(PERMISSION_KEY, constraint, usage);
    }

    @Override
    public boolean isGranted(@NonNull Context context) {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(context);
    }

    @Override
    public void request(@NonNull Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Open screen to grant overlay permission
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + activity.getPackageName()));
            activity.startActivityForResult(intent, 0);
        }
    }
}
