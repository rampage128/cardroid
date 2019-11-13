package de.jlab.cardroid.utils.permissions;

import android.app.Activity;
import android.content.Context;
import android.os.Parcelable;

import java.util.ArrayList;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

public abstract class Permission {

    public enum Constraint {
        REQUIRED,
        OPTIONAL;

        @NonNull
        public String getDisplayName(@NonNull Context context) {
            String resourceName = "permission_constraint_" + this.name().toLowerCase();
            int resourceId = context.getResources().getIdentifier(resourceName, "string", context.getPackageName());
            if (resourceId == 0) {
                throw new NullPointerException("String resource for permission constraint " + this.name() + " does not exist!");
            }
            return context.getString(resourceId);
        }

        public int getColor(@NonNull Context context) {
            String resourceName = "permission_constraint_" + this.name().toLowerCase();
            int resourceId = context.getResources().getIdentifier(resourceName, "color", context.getPackageName());
            if (resourceId == 0) {
                throw new NullPointerException("Color resource for permission constraint " + this.name() + " does not exist!");
            }
            return context.getResources().getColor(resourceId);
        }
    }

    private String permissionKey;
    private Constraint constraint;
    private int usage;

    public Permission(@NonNull String permissionKey, @NonNull Constraint constraint, @StringRes int usage) {
        this.permissionKey = permissionKey;
        this.constraint = constraint;
        this.usage = usage;
    }

    public boolean isPermission(@NonNull String permissionKey) {
        return this.permissionKey.equalsIgnoreCase(permissionKey);
    }

    public boolean equals(@NonNull Permission permission) {
        return this.permissionKey.equalsIgnoreCase(permission.permissionKey) &&
                this.usage == permission.usage;
    }

    @NonNull
    public String getPermissionKey() {
        return this.permissionKey;
    }

    @NonNull
    public Constraint getConstraint() {
        return this.constraint;
    }

    public int getUsage() {
        return this.usage;
    }

    @NonNull
    public String getDisplayName(@NonNull Context context) {
        String permissionResourceKey = this.getPermissionKey().toLowerCase();
        int lastDotIndex = permissionResourceKey.lastIndexOf('.');
        if (lastDotIndex > -1) {
            permissionResourceKey = permissionResourceKey.substring(lastDotIndex + 1);
        }
        permissionResourceKey = "permission_" + permissionResourceKey;
        int resourceId = context.getResources().getIdentifier(permissionResourceKey, "string", context.getPackageName());
        if (resourceId == 0) {
            return permissionResourceKey;
        }
        return context.getString(resourceId);
    }

    public abstract boolean isGranted(@NonNull Context context);
    public abstract void request(@NonNull Activity activity);

    @NonNull
    public static Permission fromRequest(@NonNull PermissionRequest request) {
        if (request.getPermimssionKey().equalsIgnoreCase(MockLocationPermission.PERMISSION_KEY)) {
            return new MockLocationPermission(request.getConstraint(), request.getUsage());
        } else if (request.getPermimssionKey().equalsIgnoreCase(OverlayPermission.PERMISSION_KEY)) {
            return new OverlayPermission(request.getConstraint(), request.getUsage());
        }

        return new StandardPermission(request.getPermimssionKey(), request.getConstraint(), request.getUsage());
    }

    @NonNull
    public static ArrayList<Permission> fromParcel(@NonNull Parcelable... requests) {
        ArrayList<Permission> permissions = new ArrayList<>();
        for (Parcelable permissionRequest : requests) {
            permissions.add(Permission.fromRequest((PermissionRequest)permissionRequest));
        }
        return permissions;
    }

}
