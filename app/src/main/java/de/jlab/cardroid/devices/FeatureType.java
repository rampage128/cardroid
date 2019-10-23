package de.jlab.cardroid.devices;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import de.jlab.cardroid.R;
import de.jlab.cardroid.car.CanObservable;
import de.jlab.cardroid.car.ui.CarMonitorActivity;
import de.jlab.cardroid.devices.serial.carduino.EventObservable;
import de.jlab.cardroid.devices.storage.DeviceEntity;
import de.jlab.cardroid.errors.ErrorObservable;
import de.jlab.cardroid.gps.GpsObservable;
import de.jlab.cardroid.gps.ui.GpsMonitorActivity;
import de.jlab.cardroid.rules.RuleActivity;

public enum FeatureType {

    UNKNOWN_FEATURE(Feature.class, null, R.string.device_feature_unknown, R.string.device_feature_unknown_description, R.drawable.ic_device_feature_unknown),
    LOCATION_FEATURE(GpsObservable.class, GpsMonitorActivity.class, R.string.device_feature_location, R.string.device_feature_location_description, R.drawable.ic_device_feature_location),
    EVENT_FEATURE(EventObservable.class, RuleActivity.class, R.string.device_feature_events, R.string.device_feature_events_description, R.drawable.ic_device_feature_events),
    ERROR_FEATURE(ErrorObservable.class, null, R.string.device_feature_errors, R.string.device_feature_errors_description, R.drawable.ic_device_feature_errors),
    CAN_FEATURE(CanObservable.class, CarMonitorActivity.class, R.string.device_feature_can, R.string.device_feature_can_description, R.drawable.ic_device_feature_can);

    private Class<? extends Feature> featureClass;
    @StringRes
    private int typeName;
    @StringRes
    private int typeDescription;
    private int typeIcon;
    private Class<? extends Activity> activity;


    FeatureType(@NonNull Class<? extends Feature> featureClass, @Nullable Class<? extends Activity> activity, @StringRes int typeName, @StringRes int typeDescription, @DrawableRes int typeIcon) {
        this.featureClass = featureClass;
        this.typeName = typeName;
        this.typeDescription = typeDescription;
        this.typeIcon = typeIcon;
        this.activity = activity;
    }

    @StringRes
    public int getTypeName() {
        return this.typeName;
    }

    @StringRes
    public int getTypeDescription() {
        return this.typeDescription;
    }

    @DrawableRes
    public int getTypeIcon() {
        return this.typeIcon;
    }

    @NonNull
    public Class<? extends Feature> getFeatureClass() {
        return this.featureClass;
    }

    @Nullable
    public Intent getIntent(@NonNull Context context) {
        if (this.activity == null) {
            return null;
        }

        return new Intent(context, this.activity);
    }

    @NonNull
    public static FeatureType get(@NonNull Class<? extends Feature> featureClass) {
        return get(featureClass.getSimpleName());
    }

    @NonNull
    public static FeatureType get(@NonNull String providerClassName) {
        for (FeatureType type : FeatureType.values()) {
            if (type.featureClass.getSimpleName().equals(providerClassName)) {
                return type;
            }
        }
        return UNKNOWN_FEATURE;
    }

    @NonNull
    public static FeatureType[] get(@NonNull DeviceEntity entity) {
        FeatureType[] types = new FeatureType[entity.features.size()];
        for (int i = 0; i < entity.features.size(); i++) {
            types[i] = get(entity.features.get(i));
        }
        return types;
    }

}
