package de.jlab.cardroid.overlay;

import android.content.Context;

import androidx.annotation.NonNull;

import de.jlab.cardroid.R;
import de.jlab.cardroid.utils.permissions.OverlayPermission;
import de.jlab.cardroid.utils.permissions.Permission;
import de.jlab.cardroid.utils.permissions.PermissionReceiver;
import de.jlab.cardroid.utils.permissions.PermissionRequest;

public abstract class AbstractOverlayController {

    public static final PermissionRequest[] PERMISSIONS = new PermissionRequest[] {
        new PermissionRequest(OverlayPermission.PERMISSION_KEY, Permission.Constraint.REQUIRED, R.string.overlay_permission_reason)
    };

    private Context context;

    private boolean wasStarted = false;

    private PermissionReceiver permissionReceiver;

    public AbstractOverlayController(@NonNull Context context) {
        this.context = context;
        this.permissionReceiver = new PermissionReceiver(context, this.getClass(), this::onOverlayPermissionGranted);
    }

    public abstract void showCarControls();
    public abstract void showVolumeControls(int x, int y);
    public abstract void hideVolumeControls();
    public abstract void setVolumeFromCoords(int x, int y);
    protected abstract void onStart();
    protected abstract void onStop();
    protected abstract void onDispose();
    public abstract void updateBubblePosition(int x, int y);

    public final void start() {
        if (this.permissionReceiver.requestPermissions(this.context, PERMISSIONS)) {
            this.onStart();
        }

        this.wasStarted = true;
    }

    public final void stop() {
        this.onStop();
        this.wasStarted = false;
    }

    public final void dispose() {
        this.permissionReceiver.dispose();
        this.onDispose();
        this.context = null;
    }

    private void onOverlayPermissionGranted() {
        if (this.wasStarted) {
            this.start();
        }
    }

    @NonNull
    protected final Context getContext() {
        return this.context;
    }

    public boolean isRunning() {
        return this.wasStarted;
    }

}
