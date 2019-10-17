package de.jlab.cardroid.devices.ui;

import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProviders;
import de.jlab.cardroid.R;
import de.jlab.cardroid.devices.DeviceType;
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
public final class DeviceDetailFragment extends Fragment implements View.OnClickListener {
    private static final String ARG_DEVICE_ID = "deviceId";

    private DeviceDetailViewModel model;
    private DeviceEntity deviceEntity;

    private DeviceDetailInteractionListener mListener;

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
        Button rebootButton = rootView.findViewById(R.id.action_device_reboot);
        Button resetButton = rootView.findViewById(R.id.action_device_reset);

        rootView.findViewById(R.id.action_device_disconnect).setOnClickListener(this);
        rebootButton.setOnClickListener(this);
        resetButton.setOnClickListener(this);
        rootView.findViewById(R.id.action_device_delete).setOnClickListener(this);
        displayNameText.setOnClickListener(this);

        if (getArguments() != null) {
            int deviceEntityId = getArguments().getInt(ARG_DEVICE_ID, 0);
            assert this.getActivity() != null;
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
                boolean isInteractableDevice = deviceEntity.className.equals(CarduinoUsbDeviceHandler.class.getSimpleName());

                rebootButton.setEnabled(isInteractableDevice);
                resetButton.setEnabled(isInteractableDevice);
                rebootButton.setAlpha(isInteractableDevice ? 1f : .25f);
                resetButton.setAlpha(isInteractableDevice ? 1f : .25f);

                displayNameText.setText(deviceEntity.displayName);
                uidText.setText(deviceEntity.deviceUid);
                typeIcon.setImageResource(type.getTypeIcon());
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
    }
}
