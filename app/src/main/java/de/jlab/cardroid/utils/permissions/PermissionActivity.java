package de.jlab.cardroid.utils.permissions;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.os.Bundle;
import android.os.Parcelable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import java.util.ArrayList;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;
import de.jlab.cardroid.R;


public final class PermissionActivity extends AppCompatActivity {

    public static final String EXTRA_PERMISSION_REQUESTS = "PERMISSION_REQUESTS";
    public static final String EXTRA_PERMISSION_ACTION = "PERMISSION_ACTION";

    private ArrayList<Permission> permissions = null;
    private PermissionAdapter permissionAdapter;
    private String action = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_permissions);

        RecyclerView permissionList = this.findViewById(R.id.permissionList);

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            Parcelable[] permissionRequests = extras.getParcelableArray(EXTRA_PERMISSION_REQUESTS);
            if (permissionRequests != null) {
                this.action = extras.getString(EXTRA_PERMISSION_ACTION);
                if (action == null) {
                    throw new UnsupportedOperationException("PermissionActivity has to be started with a string to specify the action for the receiver!");
                }

                this.permissions = Permission.fromParcel(permissionRequests);
                // FIXME: If a new permission request comes in while the Activity is already created, these new permissions should just be added to the list
                this.permissionAdapter = new PermissionAdapter(this, this.permissions, this::permissionRequested);
                permissionList.setAdapter(this.permissionAdapter);
                this.permissionAdapter.notifyDataSetChanged();

                ApplicationInfo applicationInfo = this.getApplicationInfo();
                int stringId = applicationInfo.labelRes;
                String appName = stringId == 0 ? applicationInfo.nonLocalizedLabel.toString() : this.getString(stringId);

                TextView introductionView = this.findViewById(R.id.introduction);
                introductionView.setText(getString(R.string.permissions_introduction, appName));

                Button dismissAction = this.findViewById(R.id.dismissAction);
                dismissAction.setOnClickListener(view -> this.dismiss());

                this.checkGrantedPermissions();

                return;
            }
        }

        throw new UnsupportedOperationException("PermissionActivity has to be started with a set of permissions!");
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        this.checkGrantedPermissions();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        for (String permission : permissions) {
            for (Permission storedPermission : this.permissions) {
                if (storedPermission.isPermission(permission) && storedPermission.isGranted(this)) {
                    this.permissionAdapter.removePermission(storedPermission);
                }
            }
        }

        this.allPermissionsGranted();
    }

    @Override
    protected void onResume() {
        super.onResume();

        this.allPermissionsGranted();
    }

    private void permissionRequested(@NonNull Permission permission) {
        permission.request(this);
    }

    @Override
    public void onBackPressed() {
        this.dismiss();
    }

    public void dismiss() {
        this.finish();
    }

    public void checkGrantedPermissions() {
        for (int i = 0; i < this.permissions.size(); i++) {
            Permission permission = this.permissions.get(i);
            if (permission.isGranted(this)) {
                this.permissionAdapter.removePermission(permission);
            }
        }

        this.allPermissionsGranted();
    }

    private void allPermissionsGranted() {
        boolean areAllGranted = true;
        for (Permission permission : this.permissions) {
            areAllGranted &= permission.isGranted(this);
        }
        if (areAllGranted) {
            PermissionReceiver.notifyReceiver(this.action, this);
            this.finish();
        }
    }

    // TODO group permissions by usage to display permission for same usage in one box as a sub-list
    public static class PermissionAdapter extends RecyclerView.Adapter<PermissionActivity.PermissionAdapter.ViewHolder> {

        private Context context;
        private PermissionRequestedConsumer permissionRequestedConsumer;
        private ArrayList<Permission> mValues;

        PermissionAdapter(@NonNull Context context, @NonNull ArrayList<Permission> permissions, @NonNull PermissionRequestedConsumer permissionRequestedConsumer) {
            this.context = context;
            this.permissionRequestedConsumer = permissionRequestedConsumer;
            this.mValues = permissions;
        }

        public void removePermission(@NonNull Permission permission) {
            if (this.mValues.remove(permission)) {
                this.notifyDataSetChanged();
            }
        }

        private void requestPermissionClicked(View view) {
            Permission permission = (Permission) view.getTag();
            this.permissionRequestedConsumer.onPermissionRequested(permission);
        }

        @Override
        @NonNull
        public PermissionActivity.PermissionAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.listitem_permission, parent, false);
            return new PermissionActivity.PermissionAdapter.ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(final PermissionActivity.PermissionAdapter.ViewHolder holder, int position) {
            Permission permission = this.mValues.get(position);

            holder.name.setText(permission.getDisplayName(this.context));
            int usageRes = permission.getUsage();
            if (usageRes == 0) {
                holder.info.setVisibility(View.GONE);
            } else {
                holder.info.setVisibility(View.VISIBLE);
                holder.info.setText(usageRes);
            }
            holder.constraint.setText(permission.getConstraint().getDisplayName(this.context));
            holder.constraint.setTextColor(permission.getConstraint().getColor(this.context));
            holder.grantAction.setTag(permission);
        }

        @Override
        public int getItemCount() {
            return mValues != null ? mValues.size() : 0;
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            final Button grantAction;
            final TextView name;
            final TextView constraint;
            final TextView info;

            ViewHolder(View view) {
                super(view);
                this.grantAction = view.findViewById(R.id.grantAction);
                this.name = view.findViewById(R.id.name);
                this.constraint = view.findViewById(R.id.constraint);
                this.info = view.findViewById(R.id.info);

                this.grantAction.setOnClickListener(PermissionAdapter.this::requestPermissionClicked);
            }
        }

        public interface PermissionRequestedConsumer {
            void onPermissionRequested(@NonNull Permission permission);
        }
    }

    public static boolean checkPermissions(@NonNull Context context, @NonNull PermissionRequest... permissionRequests) {
        boolean allPermissionsGranted = true;
        for (PermissionRequest permissionRequest : permissionRequests) {
            Permission permission = Permission.fromRequest(permissionRequest);
            allPermissionsGranted &= permission.isGranted(context);
        }
        return allPermissionsGranted;
    }

    public static boolean requestPermissions(@NonNull Context context, @NonNull PermissionReceiver receiver, @NonNull PermissionRequest... permissionRequests) {
        if (checkPermissions(context, permissionRequests)) {
            return true;
        }

        Intent activityIntent = new Intent(context, PermissionActivity.class);
        activityIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        activityIntent.putExtra(PermissionActivity.EXTRA_PERMISSION_REQUESTS, permissionRequests);
        activityIntent.putExtra(PermissionActivity.EXTRA_PERMISSION_ACTION, receiver.getAction());
        context.startActivity(activityIntent);

        return false;
    }

}
