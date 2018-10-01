package com.scooter1556.sms.android.activity.tv;

import android.app.ProgressDialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.leanback.app.GuidedStepSupportFragment;
import androidx.leanback.widget.GuidanceStylist;
import androidx.leanback.widget.GuidedAction;
import androidx.leanback.widget.GuidedActionsStylist;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.preference.PreferenceManager;
import android.util.Patterns;
import android.widget.Toast;

import com.loopj.android.http.TextHttpResponseHandler;
import com.scooter1556.sms.android.R;
import com.scooter1556.sms.android.database.ConnectionDatabase;
import com.scooter1556.sms.android.domain.Connection;
import com.scooter1556.sms.android.service.RESTService;

import java.util.List;

public class TvEditConnectionActivity extends FragmentActivity {

    // Preferences
    private static SharedPreferences sharedPreferences;

    private static ConnectionDatabase db;
    private static Connection connection;
    private static ProgressDialog testProgress;

    private static final int OPTION_INPUT = 0;
    private static final int OPTION_NEXT = 1;
    private static final int OPTION_FINISH = 2;
    private static final int OPTION_CANCEL = 3;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Retrieve preferences if they exist
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        // Initialisation
        db = new ConnectionDatabase(getApplicationContext());

        // Attempt to get connection
        int id = getIntent().getIntExtra("Connection", -1);

        if(id >= 0) {
            connection = db.getConnection(id);
        } else {
            connection = new Connection();
        }

        if (null == savedInstanceState) {
            GuidedStepSupportFragment.addAsRoot(this, new ConnectionNameFragment(), android.R.id.content);
        }
    }

    public static class ConnectionNameFragment extends GuidedStepSupportFragment {
        @Override
        @NonNull
        public GuidanceStylist.Guidance onCreateGuidance(@NonNull Bundle savedInstanceState) {
            String title = getString(R.string.connections_name_dialog);

            String breadcrumb;

            if(connection.getID() == null) {
                breadcrumb = getString(R.string.connections_add_title);
            } else {
                breadcrumb = getString(R.string.connections_edit_title);
            }

            return new GuidanceStylist.Guidance(title, null, breadcrumb, null);
        }

        @Override
        public GuidedActionsStylist onCreateActionsStylist() {
            return new GuidedActionsStylist() {
                @Override
                public int onProvideItemLayoutId() {
                    return R.layout.guided_step_text_input;
                }
            };
        }

        @Override
        public void onCreateActions(@NonNull List<GuidedAction> actions, Bundle savedInstanceState) {
            // Name Text Entry
            actions.add(new GuidedAction.Builder(getActivity())
                    .id(OPTION_INPUT)
                    .editable(true)
                    .title(connection.getTitle() == null ? "" : connection.getTitle())
                    .description(getString(R.string.connections_name))
                    .build());

            // Next
            actions.add(new GuidedAction.Builder(getActivity())
                    .id(OPTION_NEXT)
                    .title(getString(R.string.label_next))
                    .build());

            // Cancel
            actions.add(new GuidedAction.Builder(getActivity())
                    .id(OPTION_CANCEL)
                    .title(getString(R.string.label_cancel))
                    .build());
        }

        @Override
        public void onGuidedActionClicked(GuidedAction action) {
            if (action.getId() == OPTION_NEXT) {
                if(getActions().get(OPTION_INPUT).getTitle().toString().isEmpty()) {
                    Toast.makeText(getActivity(), getString(R.string.connections_no_name), Toast.LENGTH_SHORT).show();
                    return;
                }

                // Set connection title
                connection.setTitle(getActions().get(OPTION_INPUT).getTitle().toString());

                // Move to next fragment
                GuidedStepSupportFragment.add(getFragmentManager(), new ConnectionUrlFragment());
            } else if (action.getId() == OPTION_CANCEL) {
                ActivityCompat.finishAfterTransition(getActivity());
            }
        }
    }

    public static class ConnectionUrlFragment extends GuidedStepSupportFragment {
        @Override
        @NonNull
        public GuidanceStylist.Guidance onCreateGuidance(@NonNull Bundle savedInstanceState) {
            String title = getString(R.string.connections_url_dialog);

            String breadcrumb;

            if(connection.getID() == null) {
                breadcrumb = getString(R.string.connections_add_title);
            } else {
                breadcrumb = getString(R.string.connections_edit_title);
            }

            return new GuidanceStylist.Guidance(title, null, breadcrumb, null);
        }

        @Override
        public GuidedActionsStylist onCreateActionsStylist() {
            return new GuidedActionsStylist() {
                @Override
                public int onProvideItemLayoutId() {
                    return R.layout.guided_step_text_input;
                }
            };
        }

        @Override
        public void onCreateActions(@NonNull List<GuidedAction> actions, Bundle savedInstanceState) {
            // URL Text Entry
            actions.add(new GuidedAction.Builder(getActivity())
                    .id(OPTION_INPUT)
                    .editable(true)
                    .description(getString(R.string.connections_url))
                    .title(connection.getUrl() == null ? getString(R.string.connections_url_default) : connection.getUrl())
                    .build());

            // Next
            actions.add(new GuidedAction.Builder(getActivity())
                    .id(OPTION_NEXT)
                    .title(getString(R.string.label_next))
                    .build());

            // Cancel
            actions.add(new GuidedAction.Builder(getActivity())
                    .id(OPTION_CANCEL)
                    .title(getString(R.string.label_cancel))
                    .build());
        }

        @Override
        public void onGuidedActionClicked(GuidedAction action) {
            if (action.getId() == OPTION_NEXT) {
                if(!Patterns.WEB_URL.matcher(getActions().get(OPTION_INPUT).getTitle().toString()).matches()) {
                    Toast.makeText(getActivity(), getString(R.string.connections_invalid_url), Toast.LENGTH_SHORT).show();
                    return;
                }

                // Set connection URL
                connection.setUrl(getActions().get(OPTION_INPUT).getTitle().toString());

                // Move to next fragment
                GuidedStepSupportFragment.add(getFragmentManager(), new ConnectionUsernameFragment());
            } else if (action.getId() == OPTION_CANCEL) {
                ActivityCompat.finishAfterTransition(getActivity());
            }
        }
    }

    public static class ConnectionUsernameFragment extends GuidedStepSupportFragment {
        @Override
        @NonNull
        public GuidanceStylist.Guidance onCreateGuidance(@NonNull Bundle savedInstanceState) {
            String title = getString(R.string.connections_username_dialog);

            String breadcrumb;

            if (connection.getID() == null) {
                breadcrumb = getString(R.string.connections_add_title);
            } else {
                breadcrumb = getString(R.string.connections_edit_title);
            }

            return new GuidanceStylist.Guidance(title, null, breadcrumb, null);
        }

        @Override
        public GuidedActionsStylist onCreateActionsStylist() {
            return new GuidedActionsStylist() {
                @Override
                public int onProvideItemLayoutId() {
                    return R.layout.guided_step_text_input;
                }
            };
        }

        @Override
        public void onCreateActions(@NonNull List<GuidedAction> actions, Bundle savedInstanceState) {
            // Username Text Entry
            actions.add(new GuidedAction.Builder(getActivity())
                    .id(OPTION_INPUT)
                    .editable(true)
                    .description(getString(R.string.connections_username))
                    .title(connection.getUsername() == null ? "" : connection.getUsername())
                    .build());

            // Next
            actions.add(new GuidedAction.Builder(getActivity())
                    .id(OPTION_NEXT)
                    .title(getString(R.string.label_next))
                    .build());

            // Cancel
            actions.add(new GuidedAction.Builder(getActivity())
                    .id(OPTION_CANCEL)
                    .title(getString(R.string.label_cancel))
                    .build());
        }

        @Override
        public void onGuidedActionClicked(GuidedAction action) {
            if (action.getId() == OPTION_NEXT) {
                if (getActions().get(OPTION_INPUT).getTitle().toString().isEmpty()) {
                    Toast.makeText(getActivity(), getString(R.string.connections_no_username), Toast.LENGTH_SHORT).show();
                    return;
                }

                // Set connection URL
                connection.setUsername(getActions().get(OPTION_INPUT).getTitle().toString());

                // Move to next fragment
                GuidedStepSupportFragment.add(getFragmentManager(), new ConnectionPasswordFragment());
            } else if (action.getId() == OPTION_CANCEL) {
                ActivityCompat.finishAfterTransition(getActivity());
            }
        }
    }

    public static class ConnectionPasswordFragment extends GuidedStepSupportFragment {
        @Override
        @NonNull
        public GuidanceStylist.Guidance onCreateGuidance(@NonNull Bundle savedInstanceState) {
            String title = getString(R.string.connections_password_dialog);

            String breadcrumb;

            if (connection.getID() == null) {
                breadcrumb = getString(R.string.connections_add_title);
            } else {
                breadcrumb = getString(R.string.connections_edit_title);
            }

            return new GuidanceStylist.Guidance(title, null, breadcrumb, null);
        }

        @Override
        public GuidedActionsStylist onCreateActionsStylist() {
            return new GuidedActionsStylist() {
                @Override
                public int onProvideItemLayoutId() {
                    return R.layout.guided_step_text_input;
                }
            };
        }

        @Override
        public void onCreateActions(@NonNull List<GuidedAction> actions, Bundle savedInstanceState) {
            // Password Text Entry
            actions.add(new GuidedAction.Builder(getActivity())
                    .id(OPTION_INPUT)
                    .editable(true)
                    .description(getString(R.string.connections_password))
                    .title(connection.getPassword() == null ? "" : connection.getPassword())
                    .build());

            // Finish
            actions.add(new GuidedAction.Builder(getActivity())
                    .id(OPTION_FINISH)
                    .title(getString(R.string.label_finish))
                    .build());

            // Cancel
            actions.add(new GuidedAction.Builder(getActivity())
                    .id(OPTION_CANCEL)
                    .title(getString(R.string.label_cancel))
                    .build());
        }

        @Override
        public void onGuidedActionClicked(GuidedAction action) {
            if (action.getId() == OPTION_FINISH) {
                if (getActions().get(OPTION_INPUT).getTitle().toString().isEmpty()) {
                    Toast.makeText(getActivity(), getString(R.string.connections_no_password), Toast.LENGTH_SHORT).show();
                    return;
                }

                // Set connection URL
                connection.setPassword(getActions().get(OPTION_INPUT).getTitle().toString());

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
                            // Save connection
                            if (connection.getID() == null) {
                                long id = db.addConnection(connection);

                                if (id >= 0) {
                                    sharedPreferences.edit().putLong("Connection", id).apply();
                                }
                            } else {
                                db.updateConnection(connection);
                            }

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
            } else if (action.getId() == OPTION_CANCEL) {
                ActivityCompat.finishAfterTransition(getActivity());
            }
        }
    }
}
