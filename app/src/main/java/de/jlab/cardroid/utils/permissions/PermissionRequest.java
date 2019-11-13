package de.jlab.cardroid.utils.permissions;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

public final class PermissionRequest implements Parcelable {

    private String permimssionKey;
    private int usage;
    private Permission.Constraint constraint;

    public PermissionRequest(@NonNull String permissionKey, @NonNull Permission.Constraint constraint, @StringRes int usage) {
        if (usage == 0) {
            throw new IllegalArgumentException("Usage parameter must be a valid String resource");
        }

        this.permimssionKey = permissionKey;
        this.constraint = constraint;
        this.usage = usage;
    }

    public String getPermimssionKey() {
        return this.permimssionKey;
    }

    public Permission.Constraint getConstraint() {
        return this.constraint;
    }

    public int getUsage() {
        return this.usage;
    }


    protected PermissionRequest(Parcel in) {
        permimssionKey = in.readString();
        usage = in.readInt();
        constraint = Permission.Constraint.values()[in.readInt()];
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(permimssionKey);
        dest.writeInt(usage);
        dest.writeInt(constraint.ordinal());
    }

    public static final Parcelable.Creator<PermissionRequest> CREATOR = new Parcelable.Creator<PermissionRequest>() {
        @Override
        public PermissionRequest createFromParcel(Parcel in) {
            return new PermissionRequest(in);
        }

        @Override
        public PermissionRequest[] newArray(int size) {
            return new PermissionRequest[size];
        }
    };
}