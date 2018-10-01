package com.scooter1556.sms.android.activity.tv;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.leanback.app.GuidedStepSupportFragment;
import androidx.leanback.widget.GuidanceStylist;
import androidx.leanback.widget.GuidedAction;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;
import android.widget.Toast;

import com.loopj.android.http.TextHttpResponseHandler;
import com.scooter1556.sms.android.R;
import com.scooter1556.sms.android.database.ConnectionDatabase;
import com.scooter1556.sms.android.domain.Connection;
import com.scooter1556.sms.android.service.RESTService;

import java.util.List;

public class TvConnectionOptionsActivity extends FragmentActivity {

    // Preferences
    private static SharedPreferences sharedPreferences;

    private static ConnectionDatabase db;
    private static Connection connection;

    private static ProgressDialog testProgress;

    private static final int OPTION_CONNECT = 0;
    private static final int OPTION_EDIT = 1;
    private static final int OPTION_DELETE = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Retrieve preferences if they exist
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        // Initialisation
        db = new ConnectionDatabase(getApplicationContext());

        // Attempt to get connection
        int id = (int) getIntent().getLongExtra("Connection", -1);

        if(id >= 0) {
            connection = db.getConnection(id);
        } else {
            ActivityCompat.finishAfterTransition(this);
        }

        if (null == savedInstanceState) {
            GuidedStepSupportFragment.addAsRoot(this, new ConnectionOptionsFragment(), android.R.id.content);
        }
    }

    public static class ConnectionOptionsFragment extends GuidedStepSupportFragment {
        @Override
        @NonNull
        public GuidanceStylist.Guidance onCreateGuidance(@NonNull Bundle savedInstanceState) {
            String title = connection.getTitle();
            String breadcrumb = getString(R.string.preferences_title_connections);
            Drawable icon = ContextCompat.getDrawable(getActivity(), R.drawable.ic_connection_settings);

            return new GuidanceStylist.Guidance(title, null, breadcrumb, icon);
        }

        @Override
        public void onCreateActions(@NonNull List<GuidedAction> actions, Bundle savedInstanceState) {
            // Connect
            actions.add(new GuidedAction.Builder(getActivity())
                    .id(OPTION_CONNECT)
                    .title(getString(R.string.label_connect))
                    .build());

            // Edit
            actions.add(new GuidedAction.Builder(getActivity())
                    .id(OPTION_EDIT)
                    .title(getString(R.string.label_edit))
                    .build());

            // Delete
            actions.add(new GuidedAction.Builder(getActivity())
                    .id(OPTION_DELETE)
                    .title(getString(R.string.label_delete))
                    .build());
        }

        @Override
        public void onGuidedActionClicked(GuidedAction action) {
            if (action.getId() == OPTION_CONNECT) {
                // Test connection
                RESTService.getInstance().testConnection(connection, new TextHttpResponseHandler() {
                    Toast error;

                    @Override
                    public void onStart() {
                        testProgress = ProgressDialog.show(getActivity(), getString(R.string.connections_testing), getString(R.string.notification_please_wait), true);
                    }

                    @Override
                    public void onSuccess(int statusCode, cz.msebera.android.httpclient.Header[] headers, String responseString) {
                        int version = Integer.valueOf(responseString);

                        testProgress.dismiss();

                        if(version < RESTService.MIN_SUPPORTED_SERVER_VERSION) {
                            error = Toast.makeText(getActivity(), getString(R.string.error_unsupported_server_version), Toast.LENGTH_SHORT);
                            error.show();
                        } else {
                            // Set as default connection
                            sharedPreferences.edit().putLong("Connection", connection.getID()).apply();

                            // Go back to manage connections activity
                            ActivityCompat.finishAfterTransition(getActivity());
                        }
                    }

                    @Override
                    public void onFailure(int statusCode, cz.msebera.android.httpclient.Header[] headers, String responseString, Throwable throwable) {
                        testProgress.dismiss();

                        switch (statusCode) {
                            case 401:
                                error = Toast.makeText(getActivity(), getString(R.string.error_unauthenticated), Toast.LENGTH_SHORT);
                                error.show();
                                break;

                            case 404:
                            case 0:
                                error = Toast.makeText(getActivity(), getString(R.string.error_server_not_found), Toast.LENGTH_SHORT);
                                error.show();
                                break;

                            default:
                                error = Toast.makeText(getActivity(), getString(R.string.error_server) + statusCode, Toast.LENGTH_SHORT);
                                error.show();
                                break;
                        }
                    }
                });
            } else if (action.getId() == OPTION_EDIT) {
                Intent intent = new Intent(getActivity(), TvEditConnectionActivity.class);
                intent.putExtra("Connection", connection.getID());
                startActivity(intent);
            } else if (action.getId() == OPTION_DELETE) {
                db.deleteConnection(connection.getID());

                if(sharedPreferences.getLong("Connection", -1) == connection.getID().longValue()) {
                    // Update preferences
                    sharedPreferences.edit().putLong("Connection", -1).apply();
                }

                // Finish activity
                ActivityCompat.finishAfterTransition(getActivity());
            }
        }
    }
}
