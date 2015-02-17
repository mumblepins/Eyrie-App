package com.dansull.eyrie;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.LoaderManager.LoaderCallbacks;
import android.content.Context;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Loader;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static com.dansull.eyrie.Utils.checkNetworkAndAlert;


/**
 * A login screen that offers login via email/password.
 */
public class LoginActivity extends Activity implements LoaderCallbacks<Cursor> {

    /**
     * A dummy authentication store containing known user names and passwords.
     * TODO: remove after connecting to a real authentication system.
     */
    private static final String[] DUMMY_CREDENTIALS = new String[]{
            "foo@example.com:hello", "bar@example.com:world"
    };


    private Dialog login;
    // UI references.
    private AutoCompleteTextView mEmailView;
    private EditText mPasswordView;
    private View mProgressView;
    private View mLoginFormView;
    private CheckBox mCheckBox;
    private EditText mAppIdView;
    private String sparkAPI = null;
    private String sparkUser = null;
    private String sparkPassword = null;
    private boolean sparkSaved = false;
    private String deviceID = null;
    private SharedPreferences settings;
    private RequestQueue mRequestQueue;
    private List<String> coreList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mRequestQueue = Volley.newRequestQueue(this);
        /// retrieve prefs
        settings = getSharedPreferences(MainActivity.PREFERENCES_KEY, MODE_PRIVATE);
        sparkAPI = settings.getString(MainActivity.SPARK_API_KEY, null);
        sparkUser = settings.getString(MainActivity.SPARK_USER_KEY, null);
        sparkPassword = settings.getString(MainActivity.SPARK_PASSWD_KEY, null);
        sparkSaved = settings.getBoolean(MainActivity.SPARK_SAVED_KEY, false);
        deviceID = settings.getString(MainActivity.SPARK_DEVICE_KEY, null);


        // Set up the login form.
        mEmailView = (AutoCompleteTextView) findViewById(R.id.email);
        populateAutoComplete();

        mPasswordView = (EditText) findViewById(R.id.password);
        mPasswordView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
                if (id == R.id.login || id == EditorInfo.IME_NULL) {
                    attemptLogin();
                    return true;
                }
                return false;
            }
        });
        mCheckBox = (CheckBox) findViewById(R.id.save_password_check);

        mAppIdView = (EditText) findViewById(R.id.app_id);

        Button mEmailSignInButton = (Button) findViewById(R.id.email_sign_in_button);
        mEmailSignInButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                attemptLogin();
            }
        });

        mLoginFormView = findViewById(R.id.login_form);
        mProgressView = findViewById(R.id.login_progress);

        // test api
        testAPIKey();
//        getDeviceIds();
    }

    private void populateAutoComplete() {
        getLoaderManager().initLoader(0, null, this);
    }

    /**
     * Attempts to sign in or register the account specified by the login form.
     * If there are form errors (invalid email, missing fields, etc.), the
     * errors are presented and no actual login attempt is made.
     */
    public void attemptLogin() {

        // Reset errors.
        mEmailView.setError(null);
        mPasswordView.setError(null);

        // Store values at the time of the login attempt.
        String email = mEmailView.getText().toString();
        String password = mPasswordView.getText().toString();

        String key = mAppIdView.getText().toString().trim().toLowerCase();
        boolean cancel = false;
        View focusView = null;


        if (!TextUtils.isEmpty(key) && key.length() ==getResources().getInteger(R.integer.SparkApiKeyLength)) {
            // assume we have a api key
//            Log.i("here we are","yes");
            sparkAPI = key;
            savePrefs();
            getDeviceIds();
            return;
        }

        // Check for a valid password, if the user entered one.
        if (!TextUtils.isEmpty(password) && !isPasswordValid(password)) {
            mPasswordView.setError(getString(R.string.error_invalid_password));
            focusView = mPasswordView;
            cancel = true;
        }

        // Check for a valid email address.
        if (TextUtils.isEmpty(email)) {
            mEmailView.setError(getString(R.string.error_field_required));
            focusView = mEmailView;
            cancel = true;
        } else if (!isEmailValid(email)) {
            mEmailView.setError(getString(R.string.error_invalid_email));
            focusView = mEmailView;
            cancel = true;
        }

        if (cancel) {
            // There was an error; don't attempt login and focus the first
            // form field with an error.
            focusView.requestFocus();
        } else {
            // Show a progress spinner, and kick off a background task to
            // perform the user login attempt.
            showProgress(true);
            getAPIKey(email, password, mCheckBox.isChecked());
//            mAuthTask.execute((Void) null);
        }
    }

    private void testAPIKey() {
        if ((sparkAPI != null) && (deviceID != null)) {
            if (!checkNetworkAndAlert(this)) return;

            showProgress(true);
            String url = "https://api.spark.io/v1/devices/" +
                    deviceID +
                    "?access_token=" + sparkAPI;
            mRequestQueue.add(
                    new JsonObjectRequest(url, null,
                            new Response.Listener<JSONObject>() {
                                @Override
                                public void onResponse(JSONObject response) {
//                                        Log.i("whee", response.toString());
                                    if (response.optBoolean("connected", false)) {
                                        startMainActivity();
                                    } else {
                                        if (sparkSaved && sparkUser != null && sparkPassword != null) {
                                            // let's try and get a new key
                                            getAPIKey();
                                        } else {
                                            Toast.makeText(getApplicationContext(),
                                                    "Error, API Key doesn't work.  Please Login", Toast.LENGTH_LONG).show();
                                            showProgress(false);
                                        }
                                    }

                                }
                            },
                            new Response.ErrorListener() {
                                @Override
                                public void onErrorResponse(VolleyError error) {
//                                    error.printStackTrace();
                                    Toast.makeText(getApplicationContext(),
                                            "Error, API Key doesn't work.  Please Login", Toast.LENGTH_LONG).show();
                                    showProgress(false);
                                }
                            }
                    )
            );
        }
    }

    private void getDeviceIds() {
        showProgress(true);
        if (!checkNetworkAndAlert(this)) return;
        if (sparkAPI != null) {
            String url = "https://api.spark.io/v1/devices/" + "?access_token=" + sparkAPI;
            mRequestQueue.add(
                    new JsonArrayRequest(url,
                            new Response.Listener<JSONArray>() {
                                @Override
                                public void onResponse(JSONArray response) {
                                    if (response.length() > 0) {
                                        for (int i = 0; i < response.length(); i++) {
                                            try {
                                                coreList.add(response.getJSONObject(i).optString("name"));
                                            } catch (JSONException e) {
                                                e.printStackTrace();
                                            }
//                                            try {
//                                                Log.i("json", response.getJSONObject(i).toString(2));
//                                            } catch (JSONException e) {
//                                                e.printStackTrace();
//                                            }
                                            displayDevicePicker();
                                        }
                                    } else {
                                        Toast.makeText(getApplicationContext(),
                                                "No cores found...", Toast.LENGTH_LONG).show();
                                        showProgress(false);
                                    }

                                }
                            },
                            new Response.ErrorListener() {
                                @Override
                                public void onErrorResponse(VolleyError error) {
                                    // error.printStackTrace();

                                }
                            }
                    )
            );
        }
    }

    private void getAPIKey() {
        getAPIKey(sparkUser, sparkPassword, true);  // don't save, as we have already saved
    }

    private void getAPIKey(String email, String password, boolean save) {
        if (!checkNetworkAndAlert(this)) return;
        HashMap<String, String> params = new HashMap();
        params.put("grant_type", "password");
        params.put("username", email);
        params.put("password", password);
        if (save) {
            sparkUser = email;
            sparkPassword = password;
            sparkSaved = true;
        }
//        Log.i("params", params.toString());
        mRequestQueue.add(
                new MainActivity.CustomRequest(Request.Method.POST,
                        "https://api.spark.io/oauth/token",
                        params,
                        "Android_" + Build.SERIAL,
                        "Android_" + Build.SERIAL,
                        new Response.Listener<JSONObject>() {
                            @Override
                            public void onResponse(JSONObject response) {
                                try {
                                    sparkAPI = response.getString("access_token");
                                    Toast.makeText(getApplicationContext(),
                                            "Login Successful, API Key Obtained", Toast.LENGTH_LONG).show();
                                    savePrefs();
                                    getDeviceIds();
                                } catch (Exception e) {
                                    Toast.makeText(getApplicationContext(),
                                            "Unknown Login Problem, try again", Toast.LENGTH_LONG).show();

                                }
                                showProgress(false);
                            }
                        }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
//                                    Log.i("error", error.toString());
                        showProgress(false);
                        Toast.makeText(getApplicationContext(),
                                "Error logging in, try again", Toast.LENGTH_LONG).show();
                    }
                }

                )
        );
        // Redirect to dashboard / home screen.


    }

    private void savePrefs() {
        SharedPreferences settings = getSharedPreferences(MainActivity.PREFERENCES_KEY, MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString(MainActivity.SPARK_API_KEY, sparkAPI);
        editor.putString(MainActivity.SPARK_USER_KEY, sparkUser);
        editor.putString(MainActivity.SPARK_PASSWD_KEY, sparkPassword);
        editor.putBoolean(MainActivity.SPARK_SAVED_KEY, sparkSaved);

        // Commit the edits!
        editor.commit();
    }

    private void displayDevicePicker() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
// Add the buttons
        // Set GUI of login screen
        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                showProgress(false);
            }
        });
        builder.setTitle(R.string.pick_device)
                .setAdapter(new ArrayAdapter<String>(this, android.R.layout.select_dialog_item, coreList), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        // The 'which' argument contains the index position
                        // of the selected item
                        SharedPreferences settings = getSharedPreferences(MainActivity.PREFERENCES_KEY, Context.MODE_PRIVATE);
                        SharedPreferences.Editor editor = settings.edit();
                        editor.putString(MainActivity.SPARK_DEVICE_KEY, coreList.get(which));
                        // Commit the edits!
                        editor.commit();
                        startMainActivity();
                    }
                });
        login = builder.create();
        // Make dialog box visible.
        login.show();
    }

    private void startMainActivity() {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        finish();
    }

    private boolean isEmailValid(String email) {
        return email.contains("@");
    }

    private boolean isPasswordValid(String password) {
        //TODO: Wait on spark to update their password requirements
        return password.length() > 1;
    }

    /**
     * Shows the progress UI and hides the login form.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    public void showProgress(final boolean show) {
        // On Honeycomb MR2 we have the ViewPropertyAnimator APIs, which allow
        // for very easy animations. If available, use these APIs to fade-in
        // the progress spinner.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
            int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);

            mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
            mLoginFormView.animate().setDuration(shortAnimTime).alpha(
                    show ? 0 : 1).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
                }
            });

            mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            mProgressView.animate().setDuration(shortAnimTime).alpha(
                    show ? 1 : 0).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
                }
            });
        } else {
            // The ViewPropertyAnimator APIs are not available, so simply show
            // and hide the relevant UI components.
            mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        return new CursorLoader(this,
                // Retrieve data rows for the device user's 'profile' contact.
                Uri.withAppendedPath(ContactsContract.Profile.CONTENT_URI,
                        ContactsContract.Contacts.Data.CONTENT_DIRECTORY), ProfileQuery.PROJECTION,

                // Select only email addresses.
                ContactsContract.Contacts.Data.MIMETYPE +
                        " = ?", new String[]{ContactsContract.CommonDataKinds.Email
                .CONTENT_ITEM_TYPE},

                // Show primary email addresses first. Note that there won't be
                // a primary email address if the user hasn't specified one.
                ContactsContract.Contacts.Data.IS_PRIMARY + " DESC");
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        List<String> emails = new ArrayList<String>();
        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            emails.add(cursor.getString(ProfileQuery.ADDRESS));
            cursor.moveToNext();
        }

        addEmailsToAutoComplete(emails);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {

    }

    private void addEmailsToAutoComplete(List<String> emailAddressCollection) {
        //Create adapter to tell the AutoCompleteTextView what to show in its dropdown list.
        ArrayAdapter<String> adapter =
                new ArrayAdapter<String>(LoginActivity.this,
                        android.R.layout.simple_dropdown_item_1line, emailAddressCollection);

        mEmailView.setAdapter(adapter);
    }


    private interface ProfileQuery {
        String[] PROJECTION = {
                ContactsContract.CommonDataKinds.Email.ADDRESS,
                ContactsContract.CommonDataKinds.Email.IS_PRIMARY,
        };

        int ADDRESS = 0;
        int IS_PRIMARY = 1;
    }


}



