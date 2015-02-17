package com.dansull.eyrie;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.SparseArray;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.yahoo.mobile.client.android.util.ThumbPiece;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Locale;

import static com.dansull.eyrie.Utils.checkNetworkAndAlert;


public class ScheduleActivity extends FragmentActivity implements ScheduleFragment.OnFragmentInteractionListener {
    public static RequestQueue mRequestQueue;
    private final Handler handler = new Handler();
    private final ViewPager.OnPageChangeListener mPageChangeListener = new ViewPager.OnPageChangeListener() {

        /**
         * This method will be invoked when the current page is scrolled, either as part
         * of a programmatically initiated smooth scroll or a user initiated touch scroll.
         *
         * @param position             Position index of the first page currently being displayed.
         *                             Page position+1 will be visible if positionOffset is nonzero.
         * @param positionOffset       Value from [0, 1) indicating the offset from the page at position.
         * @param positionOffsetPixels Value in pixels indicating the offset from position.
         */
        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

        }

        /**
         * This method will be invoked when a new page becomes selected. Animation is not
         * necessarily complete.
         *
         * @param position Position index of the new selected page.
         */
        @Override
        public void onPageSelected(int position) {
            getActionBar().setTitle(String.format("Zone %d", position));
        }

        /**
         * Called when the scroll state changes. Useful for discovering when the user
         * begins dragging, when the pager is automatically settling to the current page,
         * or when it is fully stopped/idle.
         *
         * @param state The new scroll state.
         * @see android.support.v4.view.ViewPager#SCROLL_STATE_IDLE
         * @see android.support.v4.view.ViewPager#SCROLL_STATE_DRAGGING
         * @see android.support.v4.view.ViewPager#SCROLL_STATE_SETTLING
         */
        @Override
        public void onPageScrollStateChanged(int state) {

        }
    };
    SectionsPagerAdapter mSectionsPagerAdapter;
    ViewPager mViewPager;
    private boolean mRequestMade = false;
    private int mCurrentZone = 0;
    private int mInterval = 5000; // 5 seconds by default, can be changed later    private Runnable mRefreshHandler = new Runnable() {
    //    private Handler mHandler;
    private String sparkAPI = null;
    private JSONObject mCopyZone = null;
    private String deviceID = null;
    private Context mContext;
    private Runnable mRefreshHandler = new Runnable() {
        @Override
        public void run() {
//            Log.i("run", "whee");
            if ((sparkAPI != null)) {

                if (!checkNetworkAndAlert(mContext)) return;
                if (!mRequestMade) {
                    String url = "https://api.spark.io/v1/devices/" + deviceID + "/Command";
                    HashMap<String, String> params = new HashMap();
                    params.put("access_token", sparkAPI);
                    params.put("args", "SR" + Integer.toString(mCurrentZone) + ".A");
                    mRequestQueue.add(
                            new MainActivity.CustomRequest(
                                    Request.Method.POST, url, params,
                                    new Response.Listener<JSONObject>() {
                                        @Override
                                        public void onResponse(JSONObject response) {
                                            if (response.optBoolean("connected", false) && response.optInt("return_value", -1) == 1) {
                                                mRequestMade = true;
                                                mRefreshHandler.run();
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
                } else {
                    String url = "https://api.spark.io/v1/devices/" +
                            deviceID +
                            "/OutputString?access_token=" + sparkAPI;
                    mRequestQueue.add(
                            new JsonObjectRequest(url, null,
                                    new Response.Listener<JSONObject>() {
                                        @Override
                                        public void onResponse(JSONObject response) {
                                            if (response.optJSONObject("coreInfo").optBoolean("connected", false)) {

                                                JSONObject newjson = stringToJson(response.optString("result", null));
//                                                try {
//                                                    Log.i("json", newjson.toString(2));
//                                                } catch (JSONException e) {
//                                                    e.printStackTrace();
//                                                }
                                                saveToPrefs(mCurrentZone, newjson);

                                                ((ScheduleFragment) mSectionsPagerAdapter.getRegisteredFragment(mCurrentZone)).refreshBars();
                                                mRequestMade = false;
                                                mCurrentZone++;
                                                if (mCurrentZone < 2)
                                                    mRefreshHandler.run();
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
        }
    };
    private Boolean isEditScheduleChecked = false;
    private String url;
    //    private DateTime sparkAPIExpire = DateTime.now();
    //    @Override
//    protected void onSaveInstanceState(Bundle outState) {
//        super.onSaveInstanceState(outState);
//        //Save the fragment's instance
//        getSupportFragmentManager().putFragment(outState, ZONE_0_KEY, mSectionsPagerAdapter.getRegisteredFragment(0));
//
//        getSupportFragmentManager().putFragment(outState, ZONE_1_KEY, mSectionsPagerAdapter.getRegisteredFragment(0));
//
//    }
    private boolean sparkSaved = false;
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

    private double CtoF(double C) {
        return (C * 1.8) + 32f;
    }

    private JSONObject stringToJson(String inString) {
        if (inString == null) {
            return null;
        }
        JSONObject retJson = new JSONObject();
        String[] parts = inString.split(":");
        parts = (parts[1]).split(",");
        for (int i = 0; i < 7; i++) {
            JSONArray jsonChanges = new JSONArray();
            String changes[] = parts[i].split(";");
            for (String change : changes) {
                String pieces[] = change.split("\\|", 2);
                JSONObject jsonChange = new JSONObject();
                try {
                    jsonChange.putOpt("value", ThumbPiece.valueToNormalized(Integer.parseInt(pieces[0])));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                try {
                    jsonChange.putOpt("temp", (int) CtoF(Double.parseDouble(pieces[1])));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                jsonChanges.put(jsonChange);
            }
            try {
                retJson.putOpt(String.valueOf(ScheduleFragment.Day.get(i)), jsonChanges);
            } catch (JSONException e) {
                e.printStackTrace();
            }

        }
        return retJson;
    }

//    void startRepeatingTask() {
//        mRefreshHandler.run();
//    }

//    void stopRepeatingTask() {
//        mHandler.removeCallbacks(mRefreshHandler);
//    }

    @Override
    protected void onPause() {
//        stopRepeatingTask();
        super.onPause();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = this;
        /// retrieve prefs
        SharedPreferences settings = getSharedPreferences(MainActivity.PREFERENCES_KEY, Context.MODE_PRIVATE);
        sparkAPI = settings.getString(MainActivity.SPARK_API_KEY, null);
//        sparkAPIExpire = new DateTime(settings.getLong(MainActivity.SPARK_EXPIRE_KEY, 0));


        deviceID = settings.getString(MainActivity.SPARK_DEVICE_KEY, null);
//        Log.i("settings", (new PrettyPrintingMap(settings.getAll())).toString());
//        Log.i("api",new String(sparkAPI));
//        Log.i("expire",sparkAPIExpire.toString());
//        sparkSaved = settings.getBoolean(MainActivity.SPARK_SAVED_KEY, false);

        setContentView(R.layout.activity_schedule_activity2);
//        mHandler = new Handler();

        mRequestQueue = Volley.newRequestQueue(this);
//        startRepeatingTask();

        // Create the adapter that will return a fragment for each of the three
        // primary sections of the activity.
        mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());
//        if (savedInstanceState != null) {
//            //Restore the fragment's instance
//            mContent = getSupportFragmentManager().getFragment(
//                    savedInstanceState, "mContent");
//            ...
//        }
        // Set up the ViewPager with the sections adapter.
        mViewPager = (ViewPager) findViewById(R.id.pager);
        mViewPager.setAdapter(mSectionsPagerAdapter);
        mViewPager.setOnPageChangeListener(mPageChangeListener);
        Drawable colorDrawable = new ColorDrawable(Resources.getSystem().getColor(android.R.color.holo_blue_light));
        Drawable bottomDrawable = getResources().getDrawable(R.drawable.actionbar_bottom);
        LayerDrawable ld = new LayerDrawable(new Drawable[]{colorDrawable, bottomDrawable});


        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR1) {
            ld.setCallback(drawableCallback);
        } else {
            getActionBar().setBackgroundDrawable(ld);
        }
        getActionBar().setTitle(String.format("Zone %d", mViewPager.getCurrentItem()));

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_schedule_activity, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        ScheduleFragment thisFragment = (ScheduleFragment) mSectionsPagerAdapter.getRegisteredFragment(mViewPager.getCurrentItem());
        //noinspection SimplifiableIfStatement
        switch (id) {
            case R.id.edit_schedule:
                isEditScheduleChecked = !item.isChecked();
                invalidateOptionsMenu();
                return true;
            case R.id.action_refresh:
                mCurrentZone = 0;
                mRequestMade = false;
                mRefreshHandler.run();
                return true;
            case R.id.action_copy_zone:

//                findViewById(R.id.action_paste_zone).setEnabled(true);
                mCopyZone = thisFragment.toJson();
                invalidateOptionsMenu();
                return true;
            case R.id.action_paste_zone:
                if (mCopyZone != null) {
                    thisFragment.fromJson(mCopyZone);
                    return true;
                }
                break;
            case R.id.action_clear_all:
                thisFragment.fromJson(new JSONObject());
                return true;
        }


        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {

        menu.findItem(R.id.edit_schedule).setChecked(isEditScheduleChecked);
        menu.findItem(R.id.action_copy_zone).setEnabled(isEditScheduleChecked);
        menu.findItem(R.id.action_clear_all).setEnabled(isEditScheduleChecked);

        if (mCopyZone != null)
            menu.findItem(R.id.action_paste_zone).setEnabled(isEditScheduleChecked);
        for (int i = 0; i < 2; i++) {
            ((ScheduleFragment) mSectionsPagerAdapter.getRegisteredFragment(i)).enableBars(isEditScheduleChecked);
        }
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public void onFragmentInteraction(Uri uri) {

    }

    public void saveToPrefs(int sectionNumber, JSONObject json) {

        SharedPreferences settings = getSharedPreferences(MainActivity.PREFERENCES_KEY, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString(
                ScheduleFragment.SCHEDULE_BARS_KEY + String.valueOf(sectionNumber),
                json.toString());

//        Log.i(ScheduleFragment.SCHEDULE_BARS_KEY + String.valueOf(sectionNumber), json.toString());
        // Commit the edits!
        editor.commit();
    }


    /**
     * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
     * one of the sections/tabs/pages.
     */
    public class SectionsPagerAdapter extends FragmentPagerAdapter {

        SparseArray<android.support.v4.app.Fragment> registeredFragments = new SparseArray<>();


        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public android.support.v4.app.Fragment getItem(int position) {
            // getItem is called to instantiate the fragment for the given page.
            // Return a PlaceholderFragment (defined as a static inner class below).
            return ScheduleFragment.newInstance(position);
        }

        @Override
        public int getCount() {
            // Show 2 total pages.
            return 2;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            Locale l = Locale.getDefault();
            switch (position) {
                case 0:
                    return getString(R.string.title_section1).toUpperCase(l);
                case 1:
                    return getString(R.string.title_section2).toUpperCase(l);

            }
            return null;
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            android.support.v4.app.Fragment fragment = (android.support.v4.app.Fragment) super.instantiateItem(container, position);
            registeredFragments.put(position, fragment);


            return fragment;
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            registeredFragments.remove(position);
            super.destroyItem(container, position, object);
        }

        public android.support.v4.app.Fragment getRegisteredFragment(int position) {
            return registeredFragments.get(position);
        }
    }

}
