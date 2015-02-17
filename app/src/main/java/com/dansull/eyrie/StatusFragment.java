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

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.FrameLayout.LayoutParams;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.yahoo.mobile.client.android.util.ScheduleBar;
import com.yahoo.mobile.client.android.util.ThumbPiece;

import org.joda.time.DateTime;
import org.joda.time.Interval;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import static com.dansull.eyrie.Utils.checkNetworkAndAlert;

//import android.util.Log;

public class StatusFragment extends Fragment implements ThermostatUpdate {
    private static final String ARG_POSITION = "position";
    private static final String MINUTE_TAG = "minute";
    private static final String HOUR_TAG = "hour";
    private static final String YEAR_TAG = "year";
    private static final String MON_TAG = "month";
    private static final String DAY_TAG = "month";
    public volatile int retSuccess = 0;
    private List<ThumbPiece> thumbBuffer;
    private boolean thumbsCopied = false;
    private SwipeRefreshLayout sl;
    private SwipeRefreshLayout.OnRefreshListener refreshListener =
            new SwipeRefreshLayout.OnRefreshListener() {
                @Override
                public void onRefresh() {
                    parentActivity.checkNow(sl);
                }
            };
    private ScheduleBar activeBar;
    //private ScheduleAdapter listAdapter;
    private TimePicker timePicker;
    private DatePicker datePicker;
    private int position;
    private MainActivity parentActivity;
    private String url;
    private FrameLayout fl;
    private volatile boolean timeOverChecked = false;
//    private List<ScheduleBar> scheduleBars = new ArrayList<ScheduleBar>();
    private int contextMenuPushLoc;

    public static StatusFragment newInstance(int position) {
        StatusFragment f = new StatusFragment();
        Bundle b = new Bundle();
        b.putInt(ARG_POSITION, position);
        f.setArguments(b);

        return f;
    }

    public static String capitalize(String str) {
        return capitalize(str, null);
    }

    public static String capitalize(String str, char[] delimiters) {
        int delimLen = (delimiters == null ? -1 : delimiters.length);
        if (str == null || str.length() == 0 || delimLen == 0) {
            return str;
        }
        int strLen = str.length();
        StringBuffer buffer = new StringBuffer(strLen);
        boolean capitalizeNext = true;
        for (int i = 0; i < strLen; i++) {
            char ch = str.charAt(i);

            if (isDelimiter(ch, delimiters)) {
                buffer.append(ch);
                capitalizeNext = true;
            } else if (capitalizeNext) {
                buffer.append(Character.toTitleCase(ch));
                capitalizeNext = false;
            } else {
                buffer.append(Character.toLowerCase(ch));
            }
        }
        return buffer.toString();
    }

    private static boolean isDelimiter(char ch, char[] delimiters) {
        if (delimiters == null) {
            return Character.isWhitespace(ch);
        }
        for (int i = 0, isize = delimiters.length; i < isize; i++) {
            if (ch == delimiters[i]) {
                return true;
            }
        }
        return false;
    }

    public void updateFragment() {
        if (position == parentActivity.adapter.getCount() - 1)
            return;

        // status page
        ThermostatData zoneData = parentActivity.getThermostatData(position);
        ((TextView) fl.findViewById(R.id.curTemp)).setText(String.format("%1.1f ºF", zoneData.getCurTemp()));
        ((TextView) fl.findViewById(R.id.setTempTotal)).setText(String.format("%1.1f ºF", zoneData.getTotalSetTemp()));


        if (Math.abs(0f - zoneData.getOutsideOffset()) < 0.05) {
            fl.findViewById(R.id.outOffsetRow).setVisibility(fl.GONE);
        } else {
            fl.findViewById(R.id.outOffsetRow).setVisibility(fl.VISIBLE);
            ((TextView) fl.findViewById(R.id.outOffset)).setText(String.format("%1.1f ºF", zoneData.getOutsideOffset()));
        }
        if (Math.abs(0f - zoneData.getFloorOffset()) < 0.05) {
            fl.findViewById(R.id.floorOffsetRow).setVisibility(fl.GONE);
        } else {
            ((TextView) fl.findViewById(R.id.floorOffset)).setText(String.format("-%1.1f ºF", zoneData.getFloorOffset()));
            fl.findViewById(R.id.floorOffsetRow).setVisibility(fl.VISIBLE);
        }
        if ((Math.abs(0f - zoneData.getFloorOffset()) < 0.05) && (Math.abs(0f - zoneData.getOutsideOffset()) < 0.05)) {
            fl.findViewById(R.id.setTempRow).setVisibility(fl.GONE);
        } else {
            fl.findViewById(R.id.setTempRow).setVisibility(fl.VISIBLE);
            ((TextView) fl.findViewById(R.id.setTemp)).setText(String.format("%1.1f ºF", zoneData.getSetTemp()));
        }

        ((TextView) fl.findViewById(R.id.percOn)).setText(String.format("%1.1f %%", zoneData.getPercOn()));
        ((TextView) fl.findViewById(R.id.nextTemp)).setText(String.format("%1.1f ºF", zoneData.getNextTemp()));


        SimpleDateFormat sdf = new SimpleDateFormat("M/d/yy\nh:mm a", Locale.US);

        ((TextView) fl.findViewById(R.id.nextTime)).setText(sdf.format(zoneData.getNextTime().toDate()));


        if (!zoneData.overridden())
            fl.findViewById(R.id.overrideTable).setVisibility(fl.GONE);
        else {
            fl.findViewById(R.id.overrideTable).setVisibility(fl.VISIBLE);
            switch (zoneData.getOverType()) {
                case OVERRIDE_TEMPORARY: //temporary
                    ((TextView) fl.findViewById(R.id.overType)).setText("Until Next Switch");
                    fl.findViewById(R.id.overUntilRow).setVisibility(fl.GONE);
                    break;
                case OVERRIDE_TIME: //until time
                    ((TextView) fl.findViewById(R.id.overType)).setText("Until:");
                    fl.findViewById(R.id.overUntilRow).setVisibility(fl.VISIBLE);
                    ((TextView) fl.findViewById(R.id.overUntil)).setText((sdf.format(zoneData.getOverTime().toDate())));
                    break;
                case OVERRIDE_PERMANENT: //permanent
                    ((TextView) fl.findViewById(R.id.overType)).setText("Permanent");
                    fl.findViewById(R.id.overUntilRow).setVisibility(fl.GONE);
                    break;
            }

            ((TextView) fl.findViewById(R.id.overTemp)).setText(String.format("%1.1f ºF", zoneData.getOverTemp()));
        }
    }

    private double cToF(double c) {
        return (c * 1.8) + 32d;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        parentActivity = (MainActivity) getActivity();

        position = getArguments().getInt(ARG_POSITION);

        url = "https://api.spark.io/v1/devices/" + parentActivity.getDeviceID() + "/Command";


    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        LayoutParams params = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);

        fl = new FrameLayout(getActivity());
        fl.setLayoutParams(params);

        final int margin = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 8, getResources()
                .getDisplayMetrics());

        params.setMargins(margin, margin, margin, margin);
        LinearLayout ll;
        switch (position) {
            case 0: // House Status
                ll = (LinearLayout) inflater.inflate(R.layout.status_layout, null);
                ll.setLayoutParams(params);
//                sl.setLayoutParams(params);
                ll.setGravity(Gravity.CENTER_HORIZONTAL);
                ll.setBackgroundResource(R.drawable.background_card);
                ((TextView) ll.findViewById(R.id.zoneName)).setText("House Status");
                sl = (SwipeRefreshLayout) ll.findViewById(R.id.swipe_container);

                sl.setOnRefreshListener(refreshListener);
                fl.addView(ll);
                if (parentActivity.getThermostatData(position).isDataHere())
                    updateFragment();
                parentActivity.getThermostatData(position).addListener(this);

                break;
            case 1: // Floor Status
                ll = (LinearLayout) inflater.inflate(R.layout.status_layout, null);
                ll.setLayoutParams(params);
//                sl.setLayoutParams(params);
                ll.setGravity(Gravity.CENTER_HORIZONTAL);
                ll.setBackgroundResource(R.drawable.background_card);
                ((TextView) ll.findViewById(R.id.zoneName)).setText("Floor Status");
                sl = (SwipeRefreshLayout) ll.findViewById(R.id.swipe_container);
                sl.setOnRefreshListener(refreshListener);
                fl.addView(ll);
                if (parentActivity.getThermostatData(position).isDataHere())
                    updateFragment();
                parentActivity.getThermostatData(position).addListener(this);
                break;
            case 2: // Change Settings
                ll = (LinearLayout) inflater.inflate(R.layout.settings_layout, null);
                params.setMargins(margin, margin, margin, margin);
                ll.setLayoutParams(params);
                ll.setLayoutParams(params);
                ll.setGravity(Gravity.CENTER_HORIZONTAL);
                ll.setBackgroundResource(R.drawable.background_card);
                ll.findViewById(R.id.resetButton).setOnClickListener(
                        new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                resetButtonClick(v);
                            }
                        }
                );
                ll.findViewById(R.id.homeButton).setOnClickListener(
                        new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                homeButtonClick(v);
                            }
                        }
                );
                ll.findViewById(R.id.awayButton).setOnClickListener(
                        new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                awayButtonClick(v);
                            }
                        }
                );
                ll.findViewById(R.id.submitButton).setOnClickListener(
                        new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                submitButtonClick(v);
                            }
                        }
                );
                ll.findViewById(R.id.tempRadio).setOnClickListener(
                        new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                holdRadiosClick(v);
                            }
                        }
                );
                ll.findViewById(R.id.permRadio).setOnClickListener(
                        new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                holdRadiosClick(v);
                            }
                        }
                );
                ll.findViewById(R.id.timeRadio).setOnClickListener(
                        new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                holdRadiosClick(v);
                            }
                        }
                );

                fl.addView(ll);
                fl.findViewById(R.id.floorCheck).setOnClickListener(
                        new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                if (((CheckBox) fl.findViewById(R.id.floorCheck)).isChecked()) {
                                    fl.findViewById(R.id.floorSetTemp).setEnabled(true);
                                    fl.findViewById(R.id.floorSetTemp).setFocusable(true);
                                    fl.findViewById(R.id.floorSetTemp).setFocusableInTouchMode(true);
                                } else {
                                    fl.findViewById(R.id.floorSetTemp).setEnabled(false);
                                    fl.findViewById(R.id.floorSetTemp).setFocusable(false);
                                    fl.findViewById(R.id.floorSetTemp).setFocusableInTouchMode(false);
                                }
                            }
                        }
                );
                fl.findViewById(R.id.houseCheck).setOnClickListener(
                        new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                if (((CheckBox) fl.findViewById(R.id.houseCheck)).isChecked()) {
                                    fl.findViewById(R.id.houseSetTemp).setEnabled(true);
                                    fl.findViewById(R.id.houseSetTemp).setFocusable(true);
                                    fl.findViewById(R.id.houseSetTemp).setFocusableInTouchMode(true);
                                } else {
                                    fl.findViewById(R.id.houseSetTemp).setEnabled(false);
                                    fl.findViewById(R.id.houseSetTemp).setFocusable(false);
                                    fl.findViewById(R.id.houseSetTemp).setFocusableInTouchMode(false);
                                }
                            }
                        }
                );
                timePicker = (TimePicker) fl.findViewById(R.id.timePicker);

                datePicker = (DatePicker) fl.findViewById(R.id.datePicker);
                timePicker.setSaveFromParentEnabled(false);
                timePicker.setSaveEnabled(true);

                datePicker.setSaveFromParentEnabled(false);
                datePicker.setSaveEnabled(true);

                if (timeOverChecked) {
                    timePicker.setVisibility(View.VISIBLE);
                    datePicker.setVisibility(View.VISIBLE);
                } else {
                    timePicker.setVisibility(View.GONE);
                    datePicker.setVisibility(View.GONE);
                }
                if (savedInstanceState != null) {
                    timePicker.setCurrentHour(savedInstanceState.getInt(HOUR_TAG));
                    timePicker.setCurrentMinute(savedInstanceState.getInt(MINUTE_TAG));
                    datePicker.init(savedInstanceState.getInt(YEAR_TAG),
                            savedInstanceState.getInt(MON_TAG),
                            savedInstanceState.getInt(DAY_TAG), null);
                }
                break;
//            case 3:  // Schedule
//                for (int i = 0; i < scheduleBars.size(); i++) {
//                    scheduleBars.add(new ScheduleBar(fl.getContext()));
//                }
//                ScrollView sv = (ScrollView) inflater.inflate(R.layout.schedule_layout, null);
//                params.setMargins(margin, margin, margin, margin);
//                sv.setLayoutParams(params);
//                sv.setLayoutParams(params);
////                sv.setGravity(Gravity.CENTER_HORIZONTAL);
//                sv.setBackgroundResource(R.drawable.background_card);
//                // ll.add
////                ScheduleAdapter scheduleAdapter = new ScheduleAdapter();
////
////                listView = (ListView) ll.findViewById(R.id.listView);
////                listView.setAdapter(scheduleAdapter);
////                listAdapter = (ScheduleAdapter) listView.getAdapter();
//                fl.addView(sv);
//
//                registerForContextMenu(fl);
//                for (int i = 0; i < ((ViewGroup) sv.findViewById(R.id.linearScheduleLayout)).getChildCount(); i++) {
//                    View child = ((ViewGroup) sv.findViewById(R.id.linearScheduleLayout)).getChildAt(i);
//                    if (child instanceof com.yahoo.mobile.client.android.util.ScheduleBar) {
//                        final int ourChildId = child.getId();
//                        Log.i("whee", getResources().getResourceName(child.getId()));
//                        ((ScheduleBar) child).setOnLongPressListener(new ScheduleBar.OnLongPressListener() {
//                                                                          @Override
//                                                                          public void onLongPressListener(ScheduleBar bar, int loc) {
//                                                                              cxtMenu(bar, loc, fl);
//
//                                                                              // getView().showContextMenu();
//                                                                          }
//                                                                      }
//
//                        );
//                    }
//                }
//                //listView.setOnCreateContextMenuListener(this);
//
////                ScheduleDay firstItem = (ScheduleDay)listView.getAdapter().getItem(0);
////                Log.i("hw", firstItem.toString());
////                rangeBar =  ((ScheduleDay)listView.getAdapter().getItem(0)).seekBar;
////             //   Log.i("hw", rangeBar.toString());
//                //  Log.i("get",String.valueOf(rangeBar));
//                // openContextMenu(rangeBar);
//
////                rangeBar.setOnRangeSeekBarChangeListener(new ScheduleBar.OnRangeSeekBarChangeListener<Integer>() {
////                    public static final String TAG = "33";
////
////                    @Override
////                    public void onRangeSeekBarValuesChanged(ScheduleBar<?> bar, Integer minValue, Integer maxValue) {
////                        // handle changed range values
////                        Log.i(TAG, "User selected new range values: MIN=" + minValue + ", MAX=" + maxValue);
////                    }
////                });
//
//                break;
        }
        return fl;
    }


    private void openOurContextMenu(ScheduleBar bar, int loc, View v) {
        activeBar = bar;
        contextMenuPushLoc = loc;
        getActivity().openContextMenu(v);
    }

//    @Override
//    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
//        super.onCreateContextMenu(menu, v, menuInfo);
//        menu.setHeaderTitle(capitalize(String.valueOf(activeBar)));
//        MenuInflater inflater = getActivity().getMenuInflater();
//        inflater.inflate(R.menu.schedule_bar_context_menu, menu);
//
//        menu.findItem(R.id.paste_day).setVisible(thumbsCopied);
//        MenuItem.OnMenuItemClickListener listener = new MenuItem.OnMenuItemClickListener() {
//            @Override
//            public boolean onMenuItemClick(MenuItem item) {
//                onContextItemSelected(item);
//                return true;
//            }
//        };
//
//        for (int i = 0, n = menu.size(); i < n; i++)
//            menu.getItem(i).setOnMenuItemClickListener(listener);
//    }

//    @Override
//    public boolean onContextItemSelected(MenuItem item) {
//
////        Log.i("get", String.valueOf(activeBar));
//        //   Log.i("no", String.valueOf(item.getSubMenu().getItem(item.getSubMenu().size() - 1).getTitle()));
////        toastSend(String.valueOf(activeBar));
//
//
//        switch (item.getItemId()) {
//            case R.id.add_switch:
////                Log.i("ContextMenu", "Add Switch");
////                ((ScheduleBar)fl.findViewById(R.id.scrollView).findViewById(R.id.linearScheduleLayout).findViewById(activeDay)).addThumb();
//                activeBar.addThumb(68, contextMenuPushLoc);
//                break;
//            case R.id.clear_day:
////                Log.i("ContextMenu", "Clear Day");
//                break;
//            case R.id.copy_day:
////                Log.i("ContextMenu", "Copy Day");
//                thumbsCopied = true;
//                thumbBuffer = new ArrayList<>();
//                for (ThumbPiece thumbPiece : activeBar.getThumbs()) {
//                    thumbBuffer.add(thumbPiece.clone());
//                }
//                break;
//            case R.id.paste_day:
////                Log.i("ContextMenu", "Paste Day");
//                activeBar.setThumbs(thumbBuffer);
//                break;
//            case R.id.duplicate_day:
//                Log.i("ContextMenu", "Duplicate Day");
//                thumbsCopied = false;
//                thumbBuffer = new ArrayList<>(activeBar.getThumbs());
//                for (int i = 0; i < ((ViewGroup) fl.findViewById(R.id.scrollView).findViewById(R.id.linearScheduleLayout)).getChildCount(); i++) {
//                    View child = ((ViewGroup) fl.findViewById(R.id.scrollView).findViewById(R.id.linearScheduleLayout)).getChildAt(i);
//                    if (child instanceof ScheduleBar) {
//                        ((ScheduleBar) ((ViewGroup) fl.findViewById(R.id.scrollView).findViewById(R.id.linearScheduleLayout)).getChildAt(i)).setThumbs(thumbBuffer);
//                    }
//                }
//                break;
//        }
//
//        return true;
//    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        if (timePicker != null) {
            outState.putInt(HOUR_TAG, timePicker.getCurrentHour());
            outState.putInt(MINUTE_TAG, timePicker.getCurrentMinute());

        }
        if (datePicker != null) {
            outState.putInt(YEAR_TAG, datePicker.getYear());
            outState.putInt(MON_TAG, datePicker.getMonth());
            outState.putInt(DAY_TAG, datePicker.getDayOfMonth());
        }

//        if

        super.onSaveInstanceState(outState);
    }


    private void submitButtonClick(View v) {
        if (!checkNetworkAndAlert(parentActivity)) return;
        retSuccess = 0;
        for (int i = 0; i < 2; i++) {

            double temp = 0;
            switch (i) {
                case 0:

                    if (!(((CheckBox) fl.findViewById(R.id.houseCheck)).isChecked())) {
                        retSuccess++;
                        continue;
                    }
                    temp = Double.parseDouble(((EditText) fl.findViewById(R.id.houseSetTemp)).getText().toString());
                    break;
                case 1:

                    if (!(((CheckBox) fl.findViewById(R.id.floorCheck)).isChecked())) {
                        retSuccess++;
                        continue;
                    }
                    temp = Double.parseDouble(((EditText) fl.findViewById(R.id.floorSetTemp)).getText().toString());
                    break;
            }
            HashMap<String, String> params = new HashMap();
            params.put("access_token", parentActivity.getAPI());
            if (((RadioButton) fl.findViewById(R.id.permRadio)).isChecked()) {
                params.put("args", "HP" + Integer.toString(i) + ":" + Double.toString(fToC(temp)));
            } else if (((RadioButton) fl.findViewById(R.id.tempRadio)).isChecked()) {
                params.put("args", "HT" + Integer.toString(i) + ":" + Double.toString(fToC(temp)));
            } else if (((RadioButton) fl.findViewById(R.id.timeRadio)).isChecked()) {
                String tempArg = "HF" + Integer.toString(i) + ":" + Double.toString(fToC(temp)) + "|";
                DateTime dtThen = new DateTime();
                dtThen = dtThen.withDate(datePicker.getYear(),
                        datePicker.getMonth() + 1,
                        datePicker.getDayOfMonth())
                        .withTime(timePicker.getCurrentHour(),
                                timePicker.getCurrentMinute(), 0, 0);
                DateTime dtNow = DateTime.now();
                Interval diff;
                try {
                    diff = new Interval(dtNow, dtThen);
                } catch (IllegalArgumentException e) {
//                    Log.e("error", e.toString());
                    diff = new Interval(1, 2);
                }
                params.put("args", tempArg + Long.toString(diff.toDurationMillis() / 1000));
            }

            parentActivity.mRequestQueue.add(new MainActivity.CustomRequest(Request.Method.POST, url, params,
                            new Response.Listener<JSONObject>() {
                                @Override
                                public void onResponse(JSONObject response) {
                                    if (response.optBoolean("connected", false)) {
                                        retSuccess++;

                                    } else {
                                        retSuccess -= 100;
                                    }
                                    if (retSuccess == 2) {
                                        toastSend("Override Success!");
                                        parentActivity.checkNow(1000);
                                    } else if ((retSuccess == -200) || (retSuccess == -99))
                                        toastSend("Override Fail! Uh oh!");
                                }
                            }, new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            retSuccess -= 100;
                            if ((retSuccess == -200) || (retSuccess == -99))
                                toastSend("Override Fail! Uh oh!");
                        }
                    }

                    )
            );


        }
    }

    private void holdRadiosClick(View v) {
        switch (v.getId()) {
            case R.id.tempRadio:

                timeOverChecked = false;
                datePicker.setVisibility(fl.GONE);
                timePicker.setVisibility(fl.GONE);
                break;
            case R.id.permRadio:
                timeOverChecked = false;
                datePicker.setVisibility(fl.GONE);
                timePicker.setVisibility(fl.GONE);
                break;
            case R.id.timeRadio:
                timeOverChecked = true;
                datePicker.setVisibility(fl.VISIBLE);
                timePicker.setVisibility(fl.VISIBLE);
                break;
        }

    }

    private void awayButtonClick(View v) {
        if (!checkNetworkAndAlert(parentActivity)) return;
        retSuccess = 0;
        for (int i = 0; i < 2; i++) {
            HashMap<String, String> params = new HashMap();
            params.put("access_token", parentActivity.getAPI());
            TypedValue temp = new TypedValue();
            getResources().getValue(R.dimen.awayHouseTemp, temp, true);
            if (i == 1)
                getResources().getValue(R.dimen.awayFloorTemp, temp, true);
            params.put("args", "HT" + Integer.toString(i) + ":" + Double.toString(fToC(temp.getFloat())));

            parentActivity.mRequestQueue.add(new MainActivity.CustomRequest(Request.Method.POST, url, params,
                            new Response.Listener<JSONObject>() {
                                @Override
                                public void onResponse(JSONObject response) {
                                    if (response.optBoolean("connected", false)) {
                                        retSuccess++;

                                    } else {
                                        retSuccess -= 100;
                                    }
                                    if (retSuccess == 2) {
                                        toastSend("Override Success!");
                                        parentActivity.checkNow(1000);
                                    } else if ((retSuccess == -200) || (retSuccess == -99))
                                        toastSend("Override Fail! Uh oh!");
                                }
                            }, new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            retSuccess -= 100;
                            if ((retSuccess == -200) || (retSuccess == -99))
                                toastSend("Override Fail! Uh oh!");
                        }
                    }

                    )
            );


        }
    }

    private void homeButtonClick(View v) {
        if (!checkNetworkAndAlert(parentActivity)) return;
        retSuccess = 0;
        for (int i = 0; i < 2; i++) {
            HashMap<String, String> params = new HashMap();
            params.put("access_token", parentActivity.getAPI());
            TypedValue temp = new TypedValue();
            getResources().getValue(R.dimen.homeHouseTemp, temp, true);
            if (i == 1)
                getResources().getValue(R.dimen.homeFloorTemp, temp, true);
            params.put("args", "HT" + Integer.toString(i) + ":" + Double.toString(fToC(temp.getFloat())));

            parentActivity.mRequestQueue.add(new MainActivity.CustomRequest(Request.Method.POST, url, params,
                            new Response.Listener<JSONObject>() {
                                @Override
                                public void onResponse(JSONObject response) {
                                    if (response.optBoolean("connected", false)) {
                                        retSuccess++;

                                    } else {
                                        retSuccess -= 100;
                                    }
                                    if (retSuccess == 2) {
                                        toastSend("Override Success!");
                                        parentActivity.checkNow(1000);
                                    } else if ((retSuccess == -200) || (retSuccess == -99))
                                        toastSend("Override Fail! Uh oh!");
                                }
                            }, new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            retSuccess -= 100;
                            if ((retSuccess == -200) || (retSuccess == -99))
                                toastSend("Override Fail! Uh oh!");
                        }
                    }

                    )
            );


        }
    }

    private double fToC(double f) {
        return (f - 32f) / 1.8;
    }

    public void resetButtonClick(View view) {
        if (!checkNetworkAndAlert(parentActivity)) return;
        retSuccess = 0;
        for (int i = 0; i < 2; i++) {
            HashMap<String, String> params = new HashMap();
            params.put("access_token", parentActivity.getAPI());
            params.put("args", "HR" + Integer.toString(i));

            parentActivity.mRequestQueue.add(new MainActivity.CustomRequest(Request.Method.POST, url, params,
                            new Response.Listener<JSONObject>() {
                                @Override
                                public void onResponse(JSONObject response) {
                                    if (response.optBoolean("connected", false)) {
                                        retSuccess++;

                                    } else {
                                        retSuccess -= 100;
                                    }
                                    if (retSuccess == 2) {
                                        toastSend("Reset Success!");
                                        parentActivity.checkNow(1000);
                                    } else if ((retSuccess == -200) || (retSuccess == -99))
                                        toastSend("Reset Fail! Uh oh!");
                                }
                            }, new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            retSuccess -= 100;
                            if ((retSuccess == -200) || (retSuccess == -99))
                                toastSend("Reset Fail! Uh oh!");
                        }
                    }

                    )
            );


        }

    }

    private void toastSend(String text) {
        Context context = getActivity().getApplicationContext();
        int duration = Toast.LENGTH_SHORT;
        Toast toast = Toast.makeText(context, text, duration);
        toast.show();
    }

    @Override
    public void thermostatUpdate() {
        updateFragment();
    }


}