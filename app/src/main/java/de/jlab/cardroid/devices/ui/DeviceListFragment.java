package de.jlab.cardroid.devices.ui;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

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
import de.jlab.cardroid.devices.identification.DeviceUid;
import de.jlab.cardroid.devices.storage.DeviceEntity;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link DeviceListFragment.DeviceListInteractionListener} interface
 * to handle interaction events.
 * Use the {@link DeviceListFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public final class DeviceListFragment extends Fragment {

    private DeviceListInteractionListener mListener;

    private DeviceListViewModel viewModel;
    private DeviceListAdapter adapter;
    private DeviceService.DeviceServiceBinder deviceService;

    private ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            deviceService = (DeviceService.DeviceServiceBinder) service;
            deviceService.addExternalDeviceObserver(adapter);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            deviceService.addExternalDeviceObserver(null);
            deviceService = null;
        }
    };

    public DeviceListFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment.
     *
     * @return A new instance of fragment DeviceListFragment.
     */
    public static DeviceListFragment newInstance() {
        return new DeviceListFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.fragment_device_list, container, false);

        Activity activity = this.getActivity();
        assert activity != null;
        final Toolbar toolbar = activity.findViewById(R.id.toolbar);
        toolbar.setTitle(activity.getTitle());

        RecyclerView recyclerView = rootView.findViewById(R.id.device_list);
        assert recyclerView != null;
        this.adapter = new DeviceListAdapter(this);
        recyclerView.setAdapter(adapter);

        this.viewModel = ViewModelProviders.of(this).get(DeviceListViewModel.class);
        this.viewModel.getAll().observe(this, adapter::setDevices);

        return rootView;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof DeviceListInteractionListener) {
            mListener = (DeviceListInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement DeviceListInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
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

        //final Handler handler = new Handler();
        //handler.postDelayed(() -> {
        if (this.getContext() != null) {
            this.getContext().getApplicationContext().bindService(new Intent(this.getContext().getApplicationContext(), DeviceService.class), this.connection, Context.BIND_AUTO_CREATE);
        }
    //    }, 500);
    }

    private void onDeviceSelected(DeviceEntity deviceEntity) {
        if (mListener != null) {
            mListener.onDeviceSelected(deviceEntity);
        }
    }

    public static class DeviceListAdapter extends RecyclerView.Adapter<DeviceListFragment.DeviceListAdapter.ViewHolder> implements DeviceHandler.Observer {

        private final DeviceListFragment fragment;
        private List<DeviceEntity> mValues = new ArrayList<>();
        private HashMap<DeviceUid, DeviceHandler> liveDevices = new HashMap<>();
        private final View.OnClickListener mOnClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                DeviceEntity item = (DeviceEntity) view.getTag();
                fragment.onDeviceSelected(item);
            }
        };

        DeviceListAdapter(DeviceListFragment fragment) {
            this.fragment = fragment;
        }

        void setDevices(List<DeviceEntity> eventEntities){
            mValues = eventEntities;
            notifyDataSetChanged();
        }

        @Override
        public void onStateChange(@NonNull DeviceHandler device, @NonNull DeviceHandler.State state, @NonNull DeviceHandler.State previous) {
            for (int i = 0; i < this.mValues.size(); i++) {
                DeviceEntity descriptor = this.mValues.get(i);
                if (device.isDevice(descriptor.deviceUid) && this.fragment.getActivity() != null) {
                    this.fragment.getActivity().runOnUiThread(() -> {
                        if (state == DeviceHandler.State.READY) {
                            liveDevices.put(descriptor.deviceUid, device);
                        } else {
                            liveDevices.remove(descriptor.deviceUid);
                        }
                        notifyDataSetChanged();
                    });
                }
            }
        }

        @Override
        public void onFeatureAvailable(@NonNull Feature feature) {}

        @Override
        public void onFeatureUnavailable(@NonNull Feature feature) {}

        @Override
        public DeviceListFragment.DeviceListAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.listitem_device, parent, false);
            return new DeviceListFragment.DeviceListAdapter.ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(final DeviceListFragment.DeviceListAdapter.ViewHolder holder, int position) {
            DeviceEntity descriptor = mValues.get(position);
            DeviceType type = DeviceType.get(descriptor);
            DeviceHandler device = this.liveDevices.get(descriptor.deviceUid);
            boolean isOnline = device != null;

            holder.name.setText(descriptor.displayName);
            holder.uid.setText(descriptor.deviceUid.toString());
            holder.type.setImageResource(type.getTypeIcon());
            holder.connection.setBackgroundResource(isOnline ? R.color.colorPrimaryDark : R.color.colorDeviceUnavailable);

            holder.itemView.setTag(descriptor);
            holder.itemView.setOnClickListener(mOnClickListener);
        }

        @Override
        public int getItemCount() {
            return mValues != null ? mValues.size() : 0;
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            final View connection;
            final TextView name;
            final TextView uid;
            final ImageView type;

            ViewHolder(View view) {
                super(view);
                this.connection = view.findViewById(R.id.connection);
                this.name = view.findViewById(R.id.name);
                this.uid = view.findViewById(R.id.uid);
                this.type = view.findViewById(R.id.type_icon);
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
    public interface DeviceListInteractionListener {
        // TODO: Update argument type and name
        void onDeviceSelected(DeviceEntity deviceEntity);
    }
}
