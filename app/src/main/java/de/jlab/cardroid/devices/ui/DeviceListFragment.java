package de.jlab.cardroid.devices.ui;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
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
import de.jlab.cardroid.devices.Device;
import de.jlab.cardroid.devices.DeviceType;
import de.jlab.cardroid.devices.identification.DeviceUid;
import de.jlab.cardroid.devices.storage.DeviceEntity;
import de.jlab.cardroid.utils.ui.MasterDetailFlowActivity;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link DeviceListFragment.DeviceListInteractionListener} interface
 * to handle interaction events.
 * Use the {@link DeviceListFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public final class DeviceListFragment extends Fragment implements MasterDetailFlowActivity.MasterFragment, Device.StateObserver {

    private DeviceListInteractionListener mListener;

    private DeviceListViewModel viewModel;
    private DeviceListAdapter adapter;


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

        mListener.onDeviceListStart(this);

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
        mListener.onDeviceListEnd(this);
        mListener = null;
    }

    @Override
    public void onStateChange(@NonNull Device device, @NonNull Device.State state, @NonNull Device.State previous) {
        this.adapter.onStateChange(device, state, previous);
    }

    private void onDeviceSelected(DeviceEntity deviceEntity) {
        if (mListener != null) {
            mListener.onDeviceSelected(deviceEntity);
        }
    }

    public static class DeviceListAdapter extends RecyclerView.Adapter<DeviceListFragment.DeviceListAdapter.ViewHolder> implements Device.StateObserver {

        private final DeviceListFragment fragment;
        private List<DeviceEntity> mValues = new ArrayList<>();
        private HashMap<DeviceUid, Device> liveDevices = new HashMap<>();
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
        public void onStateChange(@NonNull Device device, @NonNull Device.State state, @NonNull Device.State previous) {
            if (state == Device.State.READY) {
                liveDevices.put(device.getDeviceUid(), device);
            } else if (state == Device.State.INVALID) {
                liveDevices.remove(device.getDeviceUid());
            }

            if (this.fragment.getActivity() != null) {
                this.fragment.getActivity().runOnUiThread(this::notifyDataSetChanged);
            }
        }

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
            boolean isOnline = this.liveDevices.containsKey(descriptor.deviceUid);

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
        void onDeviceSelected(DeviceEntity deviceEntity);
        void onDeviceListStart(@NonNull DeviceListFragment fragment);
        void onDeviceListEnd(@NonNull DeviceListFragment fragment);
    }
}
