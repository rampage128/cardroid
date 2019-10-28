package de.jlab.cardroid.devices.ui;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.RecyclerView;
import de.jlab.cardroid.R;
import de.jlab.cardroid.devices.DeviceHandler;
import de.jlab.cardroid.devices.DeviceService;
import de.jlab.cardroid.devices.DeviceType;
import de.jlab.cardroid.devices.Feature;
import de.jlab.cardroid.devices.FeatureObserver;
import de.jlab.cardroid.devices.FeatureType;
import de.jlab.cardroid.devices.storage.DeviceEntity;
import de.jlab.cardroid.devices.usb.serial.carduino.CarduinoUsbDeviceHandler;
import de.jlab.cardroid.utils.ui.DialogUtils;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link DeviceDetailFragment.DeviceDetailInteractionListener} interface
 * to handle interaction events.
 * Use the {@link DeviceDetailFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public final class DeviceDetailFragment extends Fragment implements View.OnClickListener, DeviceHandler.Observer, FeatureObserver<Feature> {
    private static final String ARG_DEVICE_ID = "deviceId";

    private DeviceDetailViewModel model;
    private DeviceEntity deviceEntity;
    private DeviceDetailFragment.DeviceFeatureAdapter adapter;
    private boolean isDeviceOnline = false;

    private DeviceDetailInteractionListener mListener;

    private Button rebootButton;
    private Button resetButton;
    private Button disconnectButton;

    private DeviceService.DeviceServiceBinder deviceService;
    private ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            deviceService = (DeviceService.DeviceServiceBinder) service;
            deviceService.subscribe(DeviceDetailFragment.this, Feature.class);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            deviceService.unsubscribe(DeviceDetailFragment.this, Feature.class);
            deviceService = null;
        }
    };


    public DeviceDetailFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param deviceEntityId database uid of DeviceEntity to edit.
     * @return A new instance of fragment DeviceDetailFragment.
     */
    public static DeviceDetailFragment newInstance(int deviceEntityId) {
        DeviceDetailFragment fragment = new DeviceDetailFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_DEVICE_ID, deviceEntityId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.fragment_device_detail, container, false);

        TextView displayNameText = rootView.findViewById(R.id.display_name);
        TextView uidText = rootView.findViewById(R.id.uid);
        ImageView typeIcon = rootView.findViewById(R.id.type_icon);
        this.rebootButton = rootView.findViewById(R.id.action_device_reboot);
        this.resetButton = rootView.findViewById(R.id.action_device_reset);
        this.disconnectButton = rootView.findViewById(R.id.action_device_disconnect);

        disconnectButton.setOnClickListener(this);
        rebootButton.setOnClickListener(this);
        resetButton.setOnClickListener(this);
        rootView.findViewById(R.id.action_device_delete).setOnClickListener(this);
        displayNameText.setOnClickListener(this);

        if (getArguments() != null) {
            int deviceEntityId = getArguments().getInt(ARG_DEVICE_ID, 0);
            assert this.getActivity() != null;

            RecyclerView recyclerView = rootView.findViewById(R.id.feature_list);
            assert recyclerView != null;
            this.adapter = new DeviceDetailFragment.DeviceFeatureAdapter((DeviceDetailInteractionListener) this.getActivity());
            recyclerView.setAdapter(adapter);

            this.model = ViewModelProviders.of(this).get(DeviceDetailViewModel.class);
            this.model.getDeviceEntity(deviceEntityId).observe(this, deviceEntity -> {
                DeviceDetailFragment.this.deviceEntity = deviceEntity;

                DeviceType type = DeviceType.get(deviceEntity);

                if (getActivity() != null) {
                    final Toolbar toolbar = getActivity().findViewById(R.id.toolbar);
                    toolbar.setTitle(deviceEntity.displayName);
                }

                // TODO: Disable reboot, reset and disconnect button if device is not connected
                // TODO: Device interactability should be determined with an Interactable directly from the device
                boolean isInteractableDevice = isDeviceOnline && deviceEntity.className.equals(CarduinoUsbDeviceHandler.class.getSimpleName());

                rebootButton.setEnabled(isInteractableDevice);
                resetButton.setEnabled(isInteractableDevice);
                disconnectButton.setEnabled(isDeviceOnline);
                rebootButton.setAlpha(isInteractableDevice ? 1f : .25f);
                resetButton.setAlpha(isInteractableDevice ? 1f : .25f);
                disconnectButton.setAlpha(isDeviceOnline ? 1f : .25f);


                displayNameText.setText(deviceEntity.displayName);
                uidText.setText(deviceEntity.deviceUid.toString());
                typeIcon.setImageResource(type.getTypeIcon());

                adapter.setFeatures(FeatureType.get(deviceEntity));
            });
        }

        return rootView;
    }

    @Override
    public void onClick(View v) {
        if (mListener != null) {
            switch(v.getId()) {
                case R.id.action_device_reboot:
                    this.mListener.onDeviceReboot(this.deviceEntity);
                    break;
                case R.id.action_device_disconnect:
                    this.mListener.onDeviceDisconnect(this.deviceEntity);
                    break;
                case R.id.action_device_reset:
                    this.mListener.onDeviceReset(this.deviceEntity);
                    break;
                case R.id.action_device_delete:
                    this.mListener.onDeviceDeleted(this.deviceEntity);
                    break;
                case R.id.display_name:
                    this.renameDevice();
                    break;
            }
        }
    }

    @Override
    public void onStateChange(@NonNull DeviceHandler device, @NonNull DeviceHandler.State state, @NonNull DeviceHandler.State previous) {
        if (this.deviceEntity != null && device.isDevice(this.deviceEntity.deviceUid)) {
            if (state == DeviceHandler.State.READY) {
                this.isDeviceOnline = true;
            } else {
                this.isDeviceOnline = false;
            }

            if (this.getActivity() != null) {
                this.getActivity().runOnUiThread(() -> {
                    boolean isInteractableDevice = isDeviceOnline && deviceEntity.className.equals(CarduinoUsbDeviceHandler.class.getSimpleName());

                    rebootButton.setEnabled(isInteractableDevice);
                    resetButton.setEnabled(isInteractableDevice);
                    disconnectButton.setEnabled(isDeviceOnline);
                    rebootButton.setAlpha(isInteractableDevice ? 1f : .25f);
                    resetButton.setAlpha(isInteractableDevice ? 1f : .25f);
                    disconnectButton.setAlpha(isDeviceOnline ? 1f : .25f);
                });
            }
        }
    }

    @Override
    public void onFeatureAvailable(@NonNull Feature feature) {
        if (this.getActivity() != null) {
            this.getActivity().runOnUiThread(() -> this.adapter.addActiveFeature(feature));
        }
    }

    @Override
    public void onFeatureUnavailable(@NonNull Feature feature) {
        if (this.getActivity() != null) {
            this.getActivity().runOnUiThread(() -> this.adapter.removeActiveFeature(feature));
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof DeviceDetailInteractionListener) {
            mListener = (DeviceDetailInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement DeviceDetailInteractionListener");
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        if (this.deviceService != null && this.getContext() != null) {
            this.getContext().getApplicationContext().unbindService(this.connection);
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        final Handler handler = new Handler();
        handler.postDelayed(() -> {
            if (this.getContext() != null) {
                this.getContext().getApplicationContext().bindService(new Intent(this.getContext().getApplicationContext(), DeviceService.class), this.connection, Context.BIND_AUTO_CREATE);
            }
        }, 500);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    private void renameDevice() {
        if (this.getContext() != null) {
            DialogUtils.showEditTextDialog(this.getLayoutInflater(), R.string.title_device_rename, R.string.action_device_rename, new DialogUtils.TextDialogListener() {
                @Override
                public void initialize(EditText input) {
                    input.setInputType(InputType.TYPE_CLASS_TEXT);
                    input.setSingleLine(true);
                    input.setText(DeviceDetailFragment.this.deviceEntity.displayName);
                    input.setHint(R.string.device_name);
                }

                @Override
                public String validate(String text) {
                    return text.trim().length() < 1 ? getString(R.string.error_device_name_blank) : null;
                }

                @Override
                public void onSuccess(String text) {
                    DeviceDetailFragment.this.deviceEntity.displayName = text;
                    DeviceDetailFragment.this.model.update(DeviceDetailFragment.this.deviceEntity);
                }
            });
        }
    }

    public static class DeviceFeatureAdapter extends RecyclerView.Adapter<DeviceDetailFragment.DeviceFeatureAdapter.ViewHolder> {

        private final DeviceDetailInteractionListener listener;
        private FeatureType[] mValues;
        private ArrayList<FeatureType> activeFeatures = new ArrayList<>();
        //private HashMap<String, FeatureType> activeFeatures = new HashMap<>();
        private final View.OnClickListener mOnClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                listener.onFeatureSelected((FeatureType) view.getTag());
            }
        };

        DeviceFeatureAdapter(DeviceDetailInteractionListener listener) {
            this.listener = listener;
        }

        public void setFeatures(FeatureType[] features) {
            mValues = features;
            notifyDataSetChanged();
        }

        public void addActiveFeature(@NonNull Feature feature) {
            this.activeFeatures.add(FeatureType.get(Objects.requireNonNull(feature.getClass().getCanonicalName())));
            //this.activeFeatures.put(feature.getClass().getCanonicalName(), );
            notifyDataSetChanged();
        }

        public void removeActiveFeature(@NonNull Feature feature) {
            this.activeFeatures.remove(FeatureType.get(Objects.requireNonNull(feature.getClass().getCanonicalName())));
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.listitem_feature, parent, false);
            return new DeviceDetailFragment.DeviceFeatureAdapter.ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull final DeviceDetailFragment.DeviceFeatureAdapter.ViewHolder holder, int position) {
            FeatureType feature = mValues[position];

            holder.name.setText(feature.getTypeName());
            holder.description.setText(feature.getTypeDescription());
            holder.icon.setImageResource(feature.getTypeIcon());

            if (this.activeFeatures.contains(feature)) {
                holder.status.setBackgroundResource(R.color.colorPrimaryDark);
            } else {
                holder.status.setBackgroundResource(R.color.colorDeviceUnavailable);
            }

            holder.itemView.setTag(feature);
            holder.itemView.setOnClickListener(mOnClickListener);
        }

        @Override
        public int getItemCount() {
            return mValues != null ? mValues.length : 0;
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            final TextView name;
            final TextView description;
            final ImageView icon;
            final View status;

            ViewHolder(View view) {
                super(view);
                this.name = view.findViewById(R.id.name);
                this.description = view.findViewById(R.id.description);
                this.icon = view.findViewById(R.id.icon);
                this.status = view.findViewById(R.id.active);
            }
        }
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface DeviceDetailInteractionListener {
        void onDeviceDisconnect(DeviceEntity deviceEntity);
        void onDeviceReboot(DeviceEntity deviceEntity);
        void onDeviceReset(DeviceEntity deviceEntity);
        void onDeviceDeleted(DeviceEntity deviceEntity);
        void onFeatureSelected(FeatureType feature);
    }
}
