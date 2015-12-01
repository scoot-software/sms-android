package com.scooter1556.sms.android.fragment;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.Bundle;
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.Fragment;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.Toast;

import com.loopj.android.http.TextHttpResponseHandler;
import com.scooter1556.sms.android.R;
import com.scooter1556.sms.lib.android.domain.Connection;
import com.scooter1556.sms.lib.android.service.RESTService;

/**
 * Edit a connection.
 *
 * Created by scott2ware.
 */
public class EditConnectionFragment extends Fragment {

    private TextInputLayout nameTextLayout;
    private EditText nameText;
    private TextInputLayout urlTextLayout;
    private EditText urlText;
    private TextInputLayout usernameTextLayout;
    private EditText usernameText;
    private TextInputLayout passwordTextLayout;
    private EditText passwordText;

    ProgressDialog testProgress;
    Connection connection;

    private ConnectionFragment.ConnectionListener connectionListener;

    @Override
    public void onCreate(final Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        // Attempt to get connection
        if(this.getArguments() != null) {
            connection = (Connection) this.getArguments().getSerializable("Connection");
        }

        // Action Bar
        setHasOptionsMenu(true);

        // Retain this fragment across configuration changes.
        setRetainInstance(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view =  inflater.inflate(R.layout.fragment_edit_connection, container, false);

        // Get elements
        nameTextLayout = (TextInputLayout) view.findViewById(R.id.connections_layout_name);
        nameText = (EditText) view.findViewById(R.id.connections_name);
        nameText.requestFocus();

        urlTextLayout = (TextInputLayout) view.findViewById(R.id.connections_layout_url);
        urlText = (EditText) view.findViewById(R.id.connections_url);

        usernameTextLayout = (TextInputLayout) view.findViewById(R.id.connections_layout_username);
        usernameText = (EditText) view.findViewById(R.id.connections_username);

        passwordTextLayout = (TextInputLayout) view.findViewById(R.id.connections_layout_password);
        passwordText = (EditText) view.findViewById(R.id.connections_password);

        // Populate fields if necessary
        if(connection != null) {
            nameText.setText(connection.getTitle());
            urlText.setText(connection.getUrl());
            usernameText.setText(connection.getUsername());
            passwordText.setText(connection.getPassword());
        }

        return view;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        try {
            connectionListener = (ConnectionFragment.ConnectionListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString() + " must implement ConnectionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();

        connectionListener = null;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_add_connections, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.save_connection:
                saveConnection();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    // Check entries and save connection
    public void saveConnection() {
        // Populate connection
        if(connection == null) {
            connection = new Connection();
        }

        connection.setTitle(nameText.getText().toString());
        connection.setUrl(urlText.getText().toString());
        connection.setUsername(usernameText.getText().toString());
        connection.setPassword(passwordText.getText().toString());

        if(connection.getTitle() == null || connection.getTitle().isEmpty()) {
            nameTextLayout.setError(getString(R.string.connections_no_name));
            nameText.requestFocus();
            return;
        } else {
            nameTextLayout.setErrorEnabled(false);
        }

        if(connection.getUrl() == null || !Patterns.WEB_URL.matcher(connection.getUrl()).matches()) {
            urlTextLayout.setError(getString(R.string.connections_invalid_url));
            urlText.requestFocus();
            return;
        } else {
            urlTextLayout.setErrorEnabled(false);
        }

        if(connection.getUsername() == null || connection.getUsername().isEmpty()) {
            usernameTextLayout.setError(getString(R.string.connections_no_username));
            usernameText.requestFocus();
            return;
        } else {
            usernameTextLayout.setErrorEnabled(false);
        }

        if(connection.getPassword() == null || connection.getPassword().isEmpty()) {
            passwordTextLayout.setError(getString(R.string.connections_no_password));
            passwordText.requestFocus();
            return;
        } else {
            passwordTextLayout.setErrorEnabled(false);
        }

        // Test connection
        RESTService.getInstance().testConnection(connection, new TextHttpResponseHandler() {

            @Override
            public void onStart() {
                testProgress = ProgressDialog.show(getContext(), getString(R.string.connections_testing), getString(R.string.notification_please_wait), true);
            }

            @Override
            public void onSuccess(int statusCode, cz.msebera.android.httpclient.Header[] headers, String responseString) {
                testProgress.dismiss();

                // Save connection
                connectionListener.SaveConnection(connection);

            }

            @Override
            public void onFailure(int statusCode, cz.msebera.android.httpclient.Header[] headers, String responseString, Throwable throwable) {
                Toast error;

                testProgress.dismiss();

                switch (statusCode) {
                    case 401:
                        error = Toast.makeText(getContext(), getString(R.string.error_unauthenticated), Toast.LENGTH_SHORT);
                        error.show();
                        break;

                    case 404:
                    case 0:
                        error = Toast.makeText(getContext(), getString(R.string.error_server_not_found), Toast.LENGTH_SHORT);
                        error.show();
                        break;

                    default:
                        error = Toast.makeText(getContext(), getString(R.string.error_server) + statusCode, Toast.LENGTH_SHORT);
                        error.show();
                        break;
                }
            }

            @Override
            public void onRetry(int retryNo) {
                // Do nothing for now...
            }
        });
    }
}
