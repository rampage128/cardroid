package de.jlab.cardroid.devices.ui;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.RecyclerView;
import de.jlab.cardroid.R;
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
        final DeviceListAdapter adapter = new DeviceListAdapter(this);
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

    private void onDeviceSelected(DeviceEntity deviceEntity) {
        if (mListener != null) {
            mListener.onDeviceSelected(deviceEntity);
        }
    }

    public static class DeviceListAdapter extends RecyclerView.Adapter<DeviceListFragment.DeviceListAdapter.ViewHolder> {

        private final DeviceListFragment fragment;
        private List<DeviceEntity> mValues;
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
        public DeviceListFragment.DeviceListAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.listitem_device, parent, false);
            return new DeviceListFragment.DeviceListAdapter.ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(final DeviceListFragment.DeviceListAdapter.ViewHolder holder, int position) {
            holder.name.setText(mValues.get(position).displayName);
            holder.uid.setText(mValues.get(position).deviceUid);
            holder.type.setText(mValues.get(position).className);

            holder.itemView.setTag(mValues.get(position));
            holder.itemView.setOnClickListener(mOnClickListener);
        }

        @Override
        public int getItemCount() {
            return mValues != null ? mValues.size() : 0;
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            final TextView name;
            final TextView uid;
            final TextView type;

            ViewHolder(View view) {
                super(view);
                this.name = view.findViewById(R.id.name);
                this.uid = view.findViewById(R.id.uid);
                this.type = view.findViewById(R.id.type);
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
