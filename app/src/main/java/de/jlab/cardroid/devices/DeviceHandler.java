package de.jlab.cardroid.devices;

import android.app.Application;
import android.util.Log;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import de.jlab.cardroid.devices.identification.DeviceConnectionId;
import de.jlab.cardroid.devices.identification.DeviceUid;
import de.jlab.cardroid.devices.storage.DeviceEntity;
import de.jlab.cardroid.devices.storage.DeviceRepository;

/* TODO add bluetooth support
 * - create a device wrapper to unify usb and bluetooth devices
 * - create a devicehandler wrapper to unify usb and bluetooth handling
 * - create a serial connection wrapper to unify usb and bluetooth serial connection
 */
public abstract class DeviceHandler {

    public enum State {
        ATTACHED,
        OPEN,
        READY,
        INVALID
    }

    private ArrayList<Feature> features = new ArrayList<>();
    private ArrayList<Observer> observers = new ArrayList<>();

    private Application app;
    private DeviceEntity descriptor;
    private DeviceConnectionId connectionId;
    private State state = State.ATTACHED;

    public DeviceHandler(@NonNull Application app) {
        this.app = app;
    }

    //////////////////////////////////////////
    // External methods for app interaction //
    //////////////////////////////////////////

    /* TODO: Remove this, if it turns out to be obsolete
    @Nullable
    public <FeatureType extends Feature> FeatureType getFeature(Class<FeatureType> featureType) {
        for (int i = 0; i < this.features.size(); i++) {
            Feature feature = this.features.get(i);
            if (featureType.isInstance(feature)) {
                return featureType.cast(feature);
            }
        }
        return null;
    }
    */

    public void addObserver(@NonNull Observer observer) {
        this.observers.add(observer);
    }

    public void removeObserver(@NonNull Observer observer) {
        this.observers.remove(observer);
    }

    @NonNull
    public State getState() {
        return this.state;
    }

    public boolean isPhysicalDevice(@NonNull DeviceConnectionId connectionId) {
        return this.connectionId != null && this.connectionId.equals(connectionId);
    }

    public boolean isDevice(@NonNull DeviceUid uid) {
        return this.descriptor != null && this.descriptor.deviceUid.equals(uid);
    }

    public boolean equals(@NonNull DeviceHandler other) {
        return this.getClass().equals(other.getClass()) && this.descriptor != null && other.descriptor != null && this.descriptor.deviceUid.equals(other.descriptor.deviceUid);
    }

    public abstract void open();
    public abstract void close();

    @NonNull
    @Override
    public String toString() {
        return this.descriptor.deviceUid + "@" + this.connectionId;
    }

    //////////////////////////////////////////////////////////
    // Internal stuff meant to be called by implementations //
    //////////////////////////////////////////////////////////

    protected final void setConnectionId(@NonNull DeviceConnectionId connectionId) {
        this.connectionId = connectionId;
    }

    public final DeviceConnectionId getConnectionId() {
        return this.connectionId;
    }

    protected final void setDeviceUid(@NonNull DeviceUid uid) {
        if (connectionId == null) {
            throw new IllegalStateException("Device can not be identified without a connectionId. Did you call setConnectionId(DeviceConnectionId) first?");
        }

        DeviceRepository repo = new DeviceRepository(this.app);
        List<DeviceEntity> entities = repo.getSynchronous(uid);

        // Filter retrieved entities by device type in case the uid was not unique
        for (Iterator<DeviceEntity> it = entities.iterator(); it.hasNext(); ) {
            if (!it.next().isDeviceType(this)) {
                it.remove();
            }
        }

        if (entities.isEmpty()) {
            Log.i(this.getClass().getSimpleName(), "Registering new device with id \"" + uid + "\".");

            if (!uid.isUnique()) {
                // TODO show a warning to the user if device could not be uniquely identified (!newUid.isUnique())
                Log.w(this.getClass().getSimpleName(), "Device uid \"" + uid + "\" is not guaranteed to be unique!");
            }

            this.descriptor = new DeviceEntity(uid, this.app.getString(DeviceType.get(this.getClass()).getTypeName()), this.getClass());
            repo.insertSynchronous(this.descriptor);

            // TODO figure out if we need to read entity again to retrieve it's database id
            this.descriptor = repo.getSynchronous(uid).get(0);

            new NewDeviceNotifier(this.app).notify(this.descriptor);
        } else if (entities.size() == 1) {
            this.descriptor = entities.get(0);
        } else {
            // TODO handle case properly where more than one device entity are returned
            Log.e(this.getClass().getSimpleName(), "More than one device registered with id \"" + uid.toString() + "\".");
            this.close();
        }
    }

    protected final void setState(@NonNull State newState) {
        if (this.state.equals(newState)) {
            return;
        }

        if (this.state.ordinal() > newState.ordinal()) {
            throw new IllegalStateException("Device can not switch from state \"" + this.state + "\" to \"" + newState + "\".");
        }

        if (newState != State.INVALID) {
            if (newState.ordinal() >= State.OPEN.ordinal() && this.connectionId == null) {
                throw new IllegalStateException("Device can not change to state \"" + newState + "\" without a valid connectionId. Did you call setConnectionId(DeviceConnectionId) first?");
            }
            if (newState.ordinal() >= State.READY.ordinal() && this.descriptor == null) {
                throw new IllegalStateException("Device can not change to state \"" + newState + "\" without a valid descriptor. Did you call setDeviceUid(DeviceUid) first?");
            }
        } else {
            while (!this.features.isEmpty()) {
                Feature feature = this.features.remove(0);
                this.removeFeature(feature);
            }
        }

        State previous = this.state;
        this.state = newState;
        for (int i = 0; i < this.observers.size(); i++) {
            this.observers.get(i).onStateChange(this, newState, previous);
        }
    }

    protected final void addFeature(@NonNull Feature feature) {
        if (connectionId == null || this.descriptor == null) {
            throw new IllegalStateException("Device can not notify about features without a connectionId and descriptor. Did you call setConnectionId(DeviceConnectionId) and setDeviceUid(DeviceUid) first?");
        }

        this.features.add(feature);
        feature.setDevice(this);

        this.descriptor.addFeature(feature);
        DeviceRepository repo = new DeviceRepository(this.app);
        repo.update(this.descriptor);

        for (int i = 0; i < this.observers.size(); i++) {
            this.observers.get(i).onFeatureAvailable(feature);
        }
    }

    protected final void removeFeature(@NonNull Feature feature) {
        if (connectionId == null || this.descriptor == null) {
            throw new IllegalStateException("Device can not notify about features without a connectionId and descriptor. Did you call setConnectionId(DeviceConnectionId) and setDeviceUid(DeviceUid) first?");
        }

        this.features.remove(feature);

        // We never remove features from the DB, so the user can browse them offline

        for (int i = 0; i < this.observers.size(); i++) {
            this.observers.get(i).onFeatureUnavailable(feature);
        }
    }

    public final ArrayList<Feature> getFeatures() { return this.features; }

    ////////////////////////
    // Coupled interfaces //
    ////////////////////////

    public interface Observer {
        void onStateChange(@NonNull DeviceHandler device, @NonNull State state, @NonNull State previous);
        void onFeatureAvailable(@NonNull Feature feature);
        void onFeatureUnavailable(@NonNull Feature feature);
    }

}
