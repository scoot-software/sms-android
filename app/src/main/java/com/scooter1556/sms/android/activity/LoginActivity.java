package com.scooter1556.sms.android.activity;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Base64;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

import com.loopj.android.http.TextHttpResponseHandler;
import com.scooter1556.sms.android.R;
import com.scooter1556.sms.lib.android.service.RESTService;

import org.apache.http.Header;

public class LoginActivity extends AppCompatActivity {

    private Context context = this;

    // Stores preferences for the application
    SharedPreferences sharedPreferences;

    // Preferences
    public static final String Username = "username";
    public static final String Password = "password";
    public static final String Server = "serverUrl";
    public static final String Version = "serverVersion";
    public static final String Remember = "rememberLogin";

    // REST Client
    RESTService restService = null;

    // UI Elements
    EditText usernameText = null;
    EditText passwordText = null;
    EditText serverText = null;
    CheckBox rememberMe = null;

    // Variables
    String username = "";
    String password = "";
    String server = "";
    boolean remember = true;
    boolean logout = false;

    ProgressDialog loginProgress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Action Bar
        getSupportActionBar().setDisplayShowHomeEnabled(false);

        // Retrieve parameters passed to activity
        Bundle parameters = getIntent().getExtras();
        if (parameters != null) {
            logout = parameters.getBoolean("logout", false);
        }

        // Retrieve preferences if they exist
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        // Determine whether to retrieve saved login details
        remember = sharedPreferences.getBoolean(Remember, true);
        server = sharedPreferences.getString(Server, getString(R.string.login_url_default));

        // Determine whether to retrieve user details or clear them on logout
        if(logout) {
            sharedPreferences.edit().remove(Username).apply();
            sharedPreferences.edit().remove(Password).apply();
        }
        else {
            username = sharedPreferences.getString(Username, "");
            password = new String(Base64.decode(sharedPreferences.getString(Password, ""), Base64.DEFAULT));
        }

        // Setup REST service
        restService = RESTService.getInstance();
        restService.init(getApplicationContext());

        if(logout || username.isEmpty() || password.isEmpty() || server.isEmpty()) { setupUserInterface(); }
        else { testAuthentication(); }
    }

    private void setupUserInterface() {
        setContentView(R.layout.activity_login);
        setTitle(R.string.login_title);

        // Set UI elements
        usernameText = (EditText) findViewById(R.id.loginUsername);
        passwordText = (EditText) findViewById(R.id.loginPassword);
        serverText = (EditText) findViewById(R.id.loginServer);
        rememberMe = (CheckBox) findViewById(R.id.loginRemember);

        // Update UI with saved data
        serverText.setText(server);
        rememberMe.setChecked(remember);

        if(remember) {
            usernameText.setText(username);
            passwordText.setText(password);
        }
    }

    // Runs when the user clicks the 'login' button
    public void login(View view) {

        // Check required fields
        if(usernameText.getText().toString().isEmpty())
        {
            Toast warning = Toast.makeText(this, getString(R.string.login_no_username), Toast.LENGTH_SHORT);
            warning.show();
        }

        if(passwordText.getText().toString().isEmpty())
        {
            Toast warning = Toast.makeText(this, getString(R.string.login_no_password), Toast.LENGTH_SHORT);
            warning.show();
        }

        if(serverText.getText().toString().isEmpty())
        {
            Toast warning = Toast.makeText(this, getString(R.string.login_no_server), Toast.LENGTH_SHORT);
            warning.show();
        }

        // Update preferences
        SharedPreferences.Editor editor = sharedPreferences.edit();

        remember = rememberMe.isChecked();
        editor.putString(Server, serverText.getText().toString());
        editor.putBoolean(Remember, remember);

        if(remember) {
            editor.putString(Username, usernameText.getText().toString());
            editor.putString(Password, Base64.encodeToString(passwordText.getText().toString().getBytes(), Base64.DEFAULT));
        }

        editor.apply();

        // Set REST authentication and URL
        restService.setServerUrl(serverText.getText().toString());
        restService.setAuthentication(usernameText.getText().toString(), passwordText.getText().toString());

        restService.getVersion(new TextHttpResponseHandler() {

            @Override
            public void onStart() {
                loginProgress = ProgressDialog.show(context, getString(R.string.login_title), getString(R.string.login_progress_dialog), true);
            }

            @Override
            public void onSuccess(int statusCode, Header[] headers, String responseString) {

                // Update server version in shared preferences
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putInt(Version, Integer.parseInt(responseString));
                editor.apply();

                loginProgress.dismiss();

                // Start the main activity
                Intent startApp = new Intent(LoginActivity.this, MainActivity.class);
                startActivity(startApp);
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, String responseString, Throwable throwable) {

                Toast error;

                loginProgress.dismiss();

                switch (statusCode) {
                    case 401:
                        error = Toast.makeText(context, getString(R.string.error_unauthenticated), Toast.LENGTH_SHORT);
                        error.show();
                        break;

                    case 404:
                    case 0:
                        error = Toast.makeText(context, getString(R.string.error_server_not_found), Toast.LENGTH_SHORT);
                        error.show();
                        break;

                    default:
                        error = Toast.makeText(context, getString(R.string.error_server) + statusCode, Toast.LENGTH_SHORT);
                        error.show();
                        break;
                }

            }

            @Override
            public void onRetry(int retryNo)
            {
                String message = getString(R.string.login_progress_dialog);

                // Add visual indicator of retry attempt to progress dialog
                for(int i=0; i<retryNo; i++)
                {
                    message += ".";
                }

                loginProgress.setMessage(message);
            }
        });
    }

    private void testAuthentication() {

        // Set REST authentication and URL
        restService.setServerUrl(server);
        restService.setAuthentication(username, password);

        restService.getVersion(new TextHttpResponseHandler() {

            @Override
            public void onStart() {
                loginProgress = ProgressDialog.show(context, getString(R.string.login_initialise), getString(R.string.notification_please_wait), true);
            }

            @Override
            public void onSuccess(int statusCode, Header[] headers, String responseString) {
                loginProgress.dismiss();

                // Start the main activity
                Intent startApp = new Intent(LoginActivity.this, MainActivity.class);
                startActivity(startApp);
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, String responseString, Throwable throwable) {
                setupUserInterface();
                loginProgress.dismiss();
            }

            @Override
            public void onRetry(int retryNo) {
                String message = getString(R.string.login_progress_dialog);

                // Add visual indicator of retry attempt to progress dialog
                for (int i = 0; i < retryNo; i++) {
                    message += ".";
                }

                loginProgress.setMessage(message);
            }
        });
    }
}
