package de.jlab.cardroid.rules;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.List;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.RecyclerView;
import de.jlab.cardroid.R;
import de.jlab.cardroid.rules.storage.EventEntity;
import de.jlab.cardroid.rules.storage.EventViewListModel;

public class RuleListFragment extends Fragment {

    private EventViewListModel eventListModel;
    private FragmentActionListener actionListener;

    public RuleListFragment() {
        // Mandatory empty constructor for automatic system instantiation
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        if (context instanceof FragmentActionListener) {
            this.actionListener = (FragmentActionListener)context;
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.rule_list, container, false);

        Activity activity = this.getActivity();
        assert activity != null;
        final Toolbar toolbar = activity.findViewById(R.id.toolbar);
        toolbar.setTitle(activity.getTitle());

        RecyclerView recyclerView = rootView.findViewById(R.id.rule_list);
        assert recyclerView != null;
        final EventListAdapter adapter = new EventListAdapter((FragmentActionListener)activity);
        recyclerView.setAdapter(adapter);

        this.eventListModel = ViewModelProviders.of(this).get(EventViewListModel.class);
        this.eventListModel.getAll().observe(this, new Observer<List<EventEntity>>() {
            @Override
            public void onChanged(@Nullable final List<EventEntity> words) {
                adapter.setEvents(words);
            }
        });

        return rootView;
    }

    public static class EventListAdapter extends RecyclerView.Adapter<EventListAdapter.ViewHolder> {

        private final FragmentActionListener listener;
        private List<EventEntity> mValues;
        private final View.OnClickListener mOnClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                EventEntity item = (EventEntity) view.getTag();
                listener.onEventChange(FragmentActionListener.COMMAND_EDIT, item);
            }
        };

        EventListAdapter(FragmentActionListener listener) {
            this.listener = listener;
        }

        void setEvents(List<EventEntity> eventEntities){
            mValues = eventEntities;
            notifyDataSetChanged();
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.rule_list_content, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(final ViewHolder holder, int position) {
            holder.nameView.setText(mValues.get(position).name);

            holder.itemView.setTag(mValues.get(position));
            holder.itemView.setOnClickListener(mOnClickListener);
        }

        @Override
        public int getItemCount() {
            return mValues != null ? mValues.size() : 0;
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            final TextView nameView;

            ViewHolder(View view) {
                super(view);
                this.nameView = view.findViewById(R.id.name);
            }
        }
    }
}
