/*
 * Copyright (C) 2013 Andreas Stuetz <andreas.stuetz@gmail.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.dansull.eyrie;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.TransitionDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.Base64;
import android.util.Log;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.android.volley.NetworkResponse;
import com.android.volley.ParseError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.HttpHeaderParser;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.astuetz.PagerSlidingTabStrip;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.joda.time.DateTime;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static com.dansull.eyrie.Utils.checkNetworkAndAlert;

//import android.util.Log;

public class MainActivity extends FragmentActivity {
    public static final String SPARK_API_KEY = "spark_api";
    //    public static final String SPARK_EXPIRE_KEY = "spark_expire";
    public static final String SPARK_PASSWD_KEY = "spark_passwd";
    public static final String SPARK_USER_KEY = "spark_user";
    public static final String SPARK_SAVED_KEY = "spark_saved";
    public static final String PREFERENCES_KEY = "MY_PREFS";
    public static final String SPARK_DEVICE_KEY = "spark_device";
    public static final String THERMOSTAT_DATA_KEY = "therm_data";
    private static final Type MAP_TYPE = new TypeToken<ArrayList<ThermostatData>>() {
    }.getType();
    ;
    private final Handler handler = new Handler();
    private final Context mContext = this;
    public RequestQueue mRequestQueue;
    public MyPagerAdapter adapter;
    private SwipeRefreshLayout swipeRefreshLayout = null;
    private Gson gson;
    private ProgressDialog progressDialog;
    private String sparkAPI = null;
    //    private DateTime sparkAPIExpire = DateTime.now();
    private String sparkUser = null;
    private String sparkPassword = null;
    private boolean sparkSaved = false;
    private String deviceID = null;
    private PagerSlidingTabStrip tabs;
    private ViewPager pager;
    private Drawable oldBackground = null;
    //    private Dialog login;
    private int currentColor = Resources.getSystem().getColor(android.R.color.holo_blue_light);
    private int mInterval = 30000; // 5 seconds by default, can be changed later    private Runnable mStatusChecker = new Runnable() {
    private Handler mHandler;
    private ArrayList<ThermostatData> thermostatDatas = new ArrayList<>();
    private Drawable.Callback drawableCallback = new Drawable.Callback() {
        @Override
        public void invalidateDrawable(Drawable who) {
            getActionBar().setBackgroundDrawable(who);
        }

        @Override
        public void scheduleDrawable(Drawable who, Runnable what, long when) {
            handler.postAtTime(what, when);
        }

        @Override
        public void unscheduleDrawable(Drawable who, Runnable what) {
            handler.removeCallbacks(what);
        }
    };
    private volatile int numOfRequests = 0;

    public static DateTime intToDate(int inInt) {
        int hour = (int) Math.floor(inInt / 4);
        int minute = (int) ((inInt - (hour * 4)) * 15);
        // Log.i("hour", String.valueOf(hour));
        // Log.i("minute", String.valueOf(minute));
        DateTime dateTime = new DateTime(2000, 1, 1, hour, minute);
        return dateTime;

    }

    public ThermostatData getThermostatData(int i) {
        return thermostatDatas.get(i);
    }

    public String getAPI() {
        return sparkAPI;
    }

    @Override
    protected void onStop() {
        stopRepeatingTask();
        super.onStop();

//        savePrefs();
        // if (RequestQueue != null) {
        //    RequestQueue.cancelAll(TAG);
        ///}
    }


//    private void savePrefs() {
//        SharedPreferences settings = getSharedPreferences(PREFERENCES_KEY, MODE_PRIVATE);
//        SharedPreferences.Editor editor = settings.edit();
////        editor.putString(SPARK_API_KEY, sparkAPI);
//////        editor.putLong(SPARK_EXPIRE_KEY, sparkAPIExpire.getMillis());
////        editor.putString(SPARK_USER_KEY, sparkUser);
////        editor.putString(SPARK_PASSWD_KEY, sparkPassword);
////        editor.putBoolean(SPARK_SAVED_KEY, sparkSaved);
////        Log.i("here", "we are");
////        Log.i("whee", gson.toJson(thermostatDatas));
////        editor.putString(THERMOSTAT_DATA_KEY, gson.toJson(thermostatDatas));
//        // Commit the edits!
//        editor.commit();
//    }


    void startRepeatingTask() {
        numOfRequests = 0;
        mStatusChecker.run();
    }

    void stopRepeatingTask() {
        mHandler.removeCallbacks(mStatusChecker);
    }

    private double parseDouble(String in) {
        double out;
        try {
            out = Double.parseDouble(in);
        } catch (Exception e) {
            out = Double.NaN;
        }
        return out;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu items for use in the action bar
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    public String getDeviceID() {
        return deviceID;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

//        final GsonBuilder gsonBuilder = new GsonBuilder();
//        gsonBuilder.registerTypeAdapter(ThermostatData.class, new ThermostatDataAdapter());
//        gson = gsonBuilder.create();
        /// retrieve prefs
        SharedPreferences settings = getSharedPreferences(PREFERENCES_KEY, MODE_PRIVATE);
        sparkAPI = settings.getString(SPARK_API_KEY, null);
//        sparkAPIExpire = new DateTime(settings.getLong(SPARK_EXPIRE_KEY, 0));
        sparkUser = settings.getString(SPARK_USER_KEY, null);
        sparkPassword = settings.getString(SPARK_PASSWD_KEY, null);
        sparkSaved = settings.getBoolean(SPARK_SAVED_KEY, false);
//        if (settings.contains(THERMOSTAT_DATA_KEY)) {
//            String data = settings.getString(THERMOSTAT_DATA_KEY, null);
//            if (data != null) {
//                Log.i("this", "thing");
//                thermostatDatas = gson.fromJson(data,
//                        new TypeToken<List<ThermostatData>>() {
//                        }.getType());
//            }
//        }
        if (savedInstanceState != null && savedInstanceState.containsKey(THERMOSTAT_DATA_KEY)) {
            thermostatDatas = savedInstanceState.getParcelableArrayList(THERMOSTAT_DATA_KEY);
            for (ThermostatData data : thermostatDatas) {
                data.fireUpdate();
            }
        } else {
            thermostatDatas.add(new ThermostatData(ThermostatData.Units.FAHRENHEIT));
            thermostatDatas.add(new ThermostatData(ThermostatData.Units.FAHRENHEIT));
        }

        deviceID = settings.getString(MainActivity.SPARK_DEVICE_KEY, null);
        mHandler = new Handler();

        mRequestQueue = Volley.newRequestQueue(this);
        startRepeatingTask();
        setContentView(R.layout.activity_main);


        tabs = (PagerSlidingTabStrip) findViewById(R.id.tabs);
        pager = (ViewPager) findViewById(R.id.pager);
        adapter = new MyPagerAdapter(getSupportFragmentManager());

        pager.setAdapter(adapter);

        final int pageMargin = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 4, getResources()
                .getDisplayMetrics());
        pager.setPageMargin(pageMargin);

        tabs.setViewPager(pager);

        changeColor(currentColor);

    }

    private void changeColor(int newColor) {
        tabs.setIndicatorColor(newColor);

        // change ActionBar color just if an ActionBar is available
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {

            Drawable colorDrawable = new ColorDrawable(newColor);
            Drawable bottomDrawable = getResources().getDrawable(R.drawable.actionbar_bottom);
            LayerDrawable ld = new LayerDrawable(new Drawable[]{colorDrawable, bottomDrawable});

            if (oldBackground == null) {

                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR1) {
                    ld.setCallback(drawableCallback);
                } else {
                    getActionBar().setBackgroundDrawable(ld);
                }

            } else {

                TransitionDrawable td = new TransitionDrawable(new Drawable[]{oldBackground, ld});

                // workaround for broken ActionBarContainer drawable handling on
                // pre-API 17 builds
                // https://github.com/android/platform_frameworks_base/commit/a7cc06d82e45918c37429a59b14545c6a57db4e4
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR1) {
                    td.setCallback(drawableCallback);
                } else {
                    getActionBar().setBackgroundDrawable(td);
                }

                td.startTransition(200);

            }

            oldBackground = ld;

            // http://stackoverflow.com/questions/11002691/actionbar-setbackgrounddrawable-nulling-background-from-thread-handler
            getActionBar().setDisplayShowTitleEnabled(false);
            getActionBar().setDisplayShowTitleEnabled(true);

        }

        currentColor = newColor;

    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putParcelableArrayList(THERMOSTAT_DATA_KEY, thermostatDatas);
        outState.putInt("currentColor", currentColor);
        super.onSaveInstanceState(outState);
        //outState.putParcelableArrayList("thermostatList", thermostatDatas);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        currentColor = savedInstanceState.getInt("currentColor");
        changeColor(currentColor);
        thermostatDatas = savedInstanceState.getParcelableArrayList(THERMOSTAT_DATA_KEY);
        for (ThermostatData data : thermostatDatas) {
            data.fireUpdate();
        }

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle presses on the action bar items
        switch (item.getItemId()) {
//            case R.id.action_login:
//                loginDialog();
//                return true;
            case R.id.change_schedule:
                Intent intent = new Intent(this, ScheduleActivity.class);
                //EditText editText = (EditText) findViewById(R.id.edit_message);
                //String message = editText.getText().toString();
                //intent.putExtra(EXTRA_MESSAGE, message);
                startActivity(intent);
                finish();
                break;
            case R.id.action_settings:
                // TODO
                break;
            case R.id.action_logout:
                logoutCloud();
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    private void logoutCloud() {
        SharedPreferences settings = getSharedPreferences(MainActivity.PREFERENCES_KEY, MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString(MainActivity.SPARK_API_KEY, null);
        editor.putString(MainActivity.SPARK_USER_KEY, null);
        editor.putString(MainActivity.SPARK_PASSWD_KEY, null);
        editor.putBoolean(MainActivity.SPARK_SAVED_KEY, false);

        // Commit the edits!
        editor.commit();
        Intent intent = new Intent(this, LoginActivity.class);
        startActivity(intent);
        finish();
    }

    //
    @Override
    protected void onPause() {
        stopRepeatingTask();
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        startRepeatingTask();
    }


    public void checkNow(int delay) {
        swipeRefreshLayout = null;
        mHandler.removeCallbacks(mStatusChecker);
        numOfRequests = 0;
        mHandler.postDelayed(mStatusChecker, delay);
    }

    public void checkNow(SwipeRefreshLayout swipeRefreshLayout) {
        this.swipeRefreshLayout = swipeRefreshLayout;
        mHandler.removeCallbacks(mStatusChecker);
        numOfRequests = 0;
        mStatusChecker.run();
    }


    public static class CustomRequest extends Request<JSONObject> {

        private Response.Listener<JSONObject> listener;
        private Map<String, String> params;
        private String username = "";
        private String password = "";

        public CustomRequest(String url, Map<String, String> params,
                             Response.Listener<JSONObject> reponseListener, Response.ErrorListener errorListener) {
            super(Method.GET, url, errorListener);
            this.listener = reponseListener;
            this.params = params;
        }

        public CustomRequest(int method, String url, Map<String, String> params,
                             Response.Listener<JSONObject> reponseListener, Response.ErrorListener errorListener) {
            super(method, url, errorListener);
            this.listener = reponseListener;
            this.params = params;
        }

        public CustomRequest(String url, Map<String, String> params, String username, String password,
                             Response.Listener<JSONObject> reponseListener, Response.ErrorListener errorListener) {
            super(Method.GET, url, errorListener);
            this.listener = reponseListener;
            this.params = params;
            this.username = username;
            this.password = password;
        }


        public CustomRequest(int method, String url, Map<String, String> params, String username, String password,
                             Response.Listener<JSONObject> reponseListener, Response.ErrorListener errorListener) {
            super(method, url, errorListener);
            this.listener = reponseListener;
            this.params = params;
            this.username = username;
            this.password = password;
        }

        @Override
        public Map<String, String> getHeaders() throws com.android.volley.AuthFailureError {
            HashMap<String, String> headers = new HashMap<String, String>();
            if (username.trim().length() > 0 && password.trim().length() > 0) {
                String creds = String.format("%s:%s", username, password);
                String auth = "Basic " + Base64.encodeToString(creds.getBytes(), Base64.NO_WRAP);
//                Log.i("auth", auth);
                headers.put("Authorization", auth);

            }
            //  headers.put("Host", url.getHost());
//            Log.i("headrs", headers.toString());
            return headers;
        }


        protected Map<String, String> getParams()
                throws com.android.volley.AuthFailureError {
            return params;
        }

        ;

        @Override
        protected Response<JSONObject> parseNetworkResponse(NetworkResponse response) {
            try {
                String jsonString = new String(response.data,
                        HttpHeaderParser.parseCharset(response.headers));
                return Response.success(new JSONObject(jsonString),
                        HttpHeaderParser.parseCacheHeaders(response));
            } catch (UnsupportedEncodingException e) {
                return Response.error(new ParseError(e));
            } catch (JSONException je) {
                return Response.error(new ParseError(je));
            }
        }

        @Override
        protected void deliverResponse(JSONObject response) {
            // TODO Auto-generated method stub
            listener.onResponse(response);
        }
    }

    public class MyPagerAdapter extends FragmentPagerAdapter {


        private final String[] TITLES = {"House Status", "Floor Status", "Change Setpoints"};//, "Change Schedule"};

        public MyPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return TITLES[position];
        }

        @Override
        public int getCount() {
            return TITLES.length;
        }

        @Override
        public Fragment getItem(int position) {
            return StatusFragment.newInstance(position);
        }
    }

    private Runnable mStatusChecker = new Runnable() {
        @Override
        public void run() {
            if ((sparkAPI != null)) {
                if (!checkNetworkAndAlert(mContext)) return;
                for (int i = 0; i < 2; i++) {
                    final int zone = i;
                    String url = "https://api.spark.io/v1/devices/" +
                            deviceID +
                            "/ZoneStr" + Integer.toString(zone) + "?access_token=" +
                            sparkAPI;

                    if (numOfRequests < 2) {
                        numOfRequests++;
                        mRequestQueue.add(new JsonObjectRequest(url, null,
                                new Response.Listener<JSONObject>() {
                                    @Override
                                    public void onResponse(JSONObject response) {
//                                        Log.i("whee", response.toString());
                                        if (response.optJSONObject("coreInfo").optBoolean("connected", false)) {
                                            String result = response.optString("result");
                                            thermostatDatas.get(zone).setAllFromString(result);
                                        }
//                                        Log.i("this", String.valueOf(zone));
                                        //Log.i("whee"+String.valueOf(zone), new Gson().toJson(thermostatDatas));
                                        numOfRequests--;
                                        if (numOfRequests <= 0) {
                                            // done requesting
                                            if (swipeRefreshLayout != null)
                                                swipeRefreshLayout.setRefreshing(false);

                                        }
                                    }
                                }, new Response.ErrorListener() {
                            @Override
                            public void onErrorResponse(VolleyError error) {
                                // error.printStackTrace();
                                numOfRequests--;
                            }
                        }));
                    }

                }
            }


//            else if (sparkSaved) {
//                getAPI();
//            }
            mHandler.postDelayed(mStatusChecker, mInterval);
        }
    };


}

