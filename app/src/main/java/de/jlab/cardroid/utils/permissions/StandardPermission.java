package de.jlab.cardroid.utils.permissions;

import android.app.Activity;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.core.app.ActivityCompat;
import androidx.core.content.PermissionChecker;

public final class StandardPermission extends Permission {

    public StandardPermission(@NonNull String permissionKey, @NonNull Constraint constraint, @StringRes int info) {
        super(permissionKey, constraint, info);
    }

    @Override
    public boolean isGranted(@NonNull Context context) {
        return PermissionChecker.checkSelfPermission(context, this.getPermissionKey()) == PermissionChecker.PERMISSION_GRANTED;
    }

    @Override
    public void request(@NonNull Activity activity) {
        ActivityCompat.requestPermissions(activity, new String[] { this.getPermissionKey() }, 1337);
    }
}
