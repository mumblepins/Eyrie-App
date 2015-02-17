package com.dansull.eyrie;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ScrollView;

import com.yahoo.mobile.client.android.util.ScheduleBar;
import com.yahoo.mobile.client.android.util.ThumbPiece;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

//import android.util.Log;

//import android.app.Fragment;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link ScheduleFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link ScheduleFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class ScheduleFragment extends Fragment {
    public static final String SCHEDULE_BARS_KEY = "schedule_bars";
    private static final String ARG_SECTION_NUMBER = "section_number";
    private int sectionNumber;
    private OnFragmentInteractionListener mListener;
    private Map<Day, ScheduleBar> scheduleBars = new HashMap<>();
    private ScheduleBar activeBar;
    private int contextMenuPushLoc;
    private ScrollView scrollView;
    //    private HashMap<Day, List<ThumbPiece>> scheduleList;
//    private Map<Day, ThumbPiece> rightMostThumb = new HashMap<>();
    private List<ThumbPiece> thumbBuffer;
    private boolean thumbsCopied = false;
    private String jsonString = null;

    public ScheduleFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment ScheduleFragment.
     */
    public static ScheduleFragment newInstance(int sectionNumber) {
        ScheduleFragment fragment = new ScheduleFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_SECTION_NUMBER, sectionNumber);
        fragment.setArguments(args);
        return fragment;
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

    public int getSectionNumber() {
        return sectionNumber;
    }

    @Override
    public void onSaveInstanceState(Bundle bundle) {
//        bundle.putSerializable(MONDAY_KEY,);
//
//        Log.i("saving", "saving");
//        bundle.putSerializable(SCHEDULE_BARS_KEY, (Serializable) scheduleBars);
//        saveToPrefs();
        super.onSaveInstanceState(bundle);
    }


    public void saveToPrefs() {

        SharedPreferences settings = getActivity().getSharedPreferences(MainActivity.PREFERENCES_KEY, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString(
                SCHEDULE_BARS_KEY + String.valueOf(sectionNumber),
                toJson().toString());

//        Log.i(SCHEDULE_BARS_KEY + String.valueOf(sectionNumber), toJson().toString());
        // Commit the edits!
        editor.commit();
    }

    private ActionBar getActionBar() {
        return getActivity().getActionBar();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
//        saveToPrefs();
    }

    public JSONObject toJson() {
        JSONObject returnJson = new JSONObject();
        for (Day day : Day.values()) {
            try {
                returnJson.putOpt(String.valueOf(day), scheduleBars.get(day).toJson());
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return returnJson;
    }

    public void fromJson(JSONObject json) {
        for (Day day : Day.values()) {
//            Log.i("day",String.valueOf(day));

            scheduleBars.get(day).clearThumbs();
            if (json.has(String.valueOf(day))) {
//                Log.i("yes","yes");
                try {
                    scheduleBars.get(day).fromJson(json.getJSONArray(String.valueOf(day)));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // getActivity().setContentView(R.layout.fragment_schedule);

        if (getArguments() != null) {
            sectionNumber = getArguments().getInt(ARG_SECTION_NUMBER);
        }


//        for (int i = 0; i < 7; i++) {
//
//            scheduleBars.put(Day.values()[i],new ScheduleBar(getActivity()));
//        }
    }


    private void openOurContextMenu(ScheduleBar bar, int loc, View v) {
        activeBar = bar;
        contextMenuPushLoc = loc;
        getActivity().openContextMenu(v);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        menu.setHeaderTitle(capitalize(activeBar.getDay().toString()));
        MenuInflater inflater = getActivity().getMenuInflater();
        inflater.inflate(R.menu.schedule_bar_context_menu, menu);

        menu.findItem(R.id.paste_day).setVisible(thumbsCopied);
        MenuItem.OnMenuItemClickListener listener = new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                onContextItemSelected(item);
                return true;
            }
        };

        for (int i = 0, n = menu.size(); i < n; i++)
            menu.getItem(i).setOnMenuItemClickListener(listener);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {

//        Log.i("get", String.valueOf(activeBar));
        //   Log.i("no", String.valueOf(item.getSubMenu().getItem(item.getSubMenu().size() - 1).getTitle()));
//        toastSend(String.valueOf(activeBar));


        switch (item.getItemId()) {
            case R.id.add_switch:
//                Log.i("ContextMenu", "Add Switch");
//                ((ScheduleBar)fl.findViewById(R.id.scrollView).findViewById(R.id.linearScheduleLayout).findViewById(activeDay)).addThumb();
                activeBar.addThumb(68, contextMenuPushLoc);
                updateSeekBars();
                break;
            case R.id.clear_day:
//                Log.i("ContextMenu", "Clear Day");
                activeBar.clearThumbs();
                updateSeekBars();
                break;
            case R.id.copy_day:
//                Log.i("ContextMenu", "Copy Day");
                thumbsCopied = true;
                thumbBuffer = new ArrayList<>();
                for (ThumbPiece thumbPiece : activeBar.getThumbs()) {
                    thumbBuffer.add(thumbPiece.clone());
                }

                updateSeekBars();
                break;
            case R.id.paste_day:
//                Log.i("ContextMenu", "Paste Day");
                activeBar.setThumbs(thumbBuffer);

                updateSeekBars();
                break;
            case R.id.duplicate_day:
//                Log.i("ContextMenu", "Duplicate Day");
                thumbsCopied = false;
                thumbBuffer = new ArrayList<>(activeBar.getThumbs());
                for (Day key : scheduleBars.keySet()) {
                    scheduleBars.get(key).setThumbs(thumbBuffer);
                }
//                for (int i = 0; i < ((ViewGroup) scrollView.findViewById(R.id.linearScheduleLayout)).getChildCount(); i++) {
//                    View child = ((ViewGroup) scrollView.findViewById(R.id.scrollView).findViewById(R.id.linearScheduleLayout)).getChildAt(i);
//                    if (child instanceof ScheduleBar) {
//                        ((ScheduleBar) ((ViewGroup) scrollView.findViewById(R.id.scrollView).findViewById(R.id.linearScheduleLayout)).getChildAt(i)).setThumbs(thumbBuffer);
//                    }
//                }

                updateSeekBars();
                break;
        }

        return true;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.fragment_schedule, container, false);

        scrollView = (ScrollView) rootView.findViewById(R.id.scrollView);

        registerForContextMenu(scrollView);

        for (int i = 0; i < ((ViewGroup) scrollView.findViewById(R.id.linearScheduleLayout)).getChildCount(); i++) {
            View child = ((ViewGroup) scrollView.findViewById(R.id.linearScheduleLayout)).getChildAt(i);
            if (child instanceof com.yahoo.mobile.client.android.util.ScheduleBar) {
                final int ourChildId = child.getId();
                switch (ourChildId) {
                    case R.id.sundayBar:
                        ((ScheduleBar) child).setDay(Day.SUNDAY);
                        scheduleBars.put(Day.SUNDAY, (ScheduleBar) child);
                        break;
                    case R.id.mondayBar:
                        ((ScheduleBar) child).setDay(Day.MONDAY);
                        scheduleBars.put(Day.MONDAY, (ScheduleBar) child);
                        break;
                    case R.id.tuesdayBar:
                        ((ScheduleBar) child).setDay(Day.TUESDAY);
                        scheduleBars.put(Day.TUESDAY, (ScheduleBar) child);
                        break;
                    case R.id.wednesdayBar:
                        ((ScheduleBar) child).setDay(Day.WEDNESDAY);
                        scheduleBars.put(Day.WEDNESDAY, (ScheduleBar) child);
                        break;
                    case R.id.thursdayBar:
                        ((ScheduleBar) child).setDay(Day.THURSDAY);
                        scheduleBars.put(Day.THURSDAY, (ScheduleBar) child);
                        break;
                    case R.id.fridayBar:
                        ((ScheduleBar) child).setDay(Day.FRIDAY);
                        scheduleBars.put(Day.FRIDAY, (ScheduleBar) child);
                        break;
                    case R.id.saturdayBar:
                        ((ScheduleBar) child).setDay(Day.SATURDAY);
                        scheduleBars.put(Day.SATURDAY, (ScheduleBar) child);
                        break;
                }

//                Log.i("whee", getResources().getResourceName(child.getId()));
                ((ScheduleBar) child).setOnLongPressListener(
                        new ScheduleBar.OnLongPressListener() {
                            @Override
                            public void onLongPressListener(ScheduleBar bar, int loc) {
                                openOurContextMenu(bar, loc, scrollView);
                            }
                        }
                );
                ((ScheduleBar) child).setOnRangeSeekBarChangeListener(
                        new ScheduleBar.OnRangeSeekBarChangeListener() {
                            @Override
                            public void onRangeSeekBarValuesChanged(ScheduleBar bar, List<?> thumbPieceList) {

                                updateSeekBars();
                            }
                        });
            }
        }
//        if (getArguments().containsKey(SCHEDULE_BARS_KEY)) {
//            Map<Day, ScheduleBar> scheduleBarsTemp = (Map<Day, ScheduleBar>) getArguments().getSerializable(SCHEDULE_BARS_KEY);
//            for (Map.Entry<Day, ScheduleBar> barTemp : scheduleBarsTemp.entrySet()) {
//                Day key = barTemp.getKey();
//                ScheduleBar value = barTemp.getValue();
//                scheduleBars.get(key).setThumbs(value.getThumbs());
//            }
//        }
        refreshBars();
        // ((TextView) rootView.findViewById(R.id.section_label)).setText(String.valueOf(sectionNumber));
        return rootView;
    }

    public void refreshBars() {
        SharedPreferences settings = getActivity().getSharedPreferences(MainActivity.PREFERENCES_KEY, Context.MODE_PRIVATE);
        if (settings.contains(SCHEDULE_BARS_KEY + String.valueOf(sectionNumber))) {
            jsonString = settings.getString(SCHEDULE_BARS_KEY + String.valueOf(sectionNumber), null);
        }
        if (jsonString != null) {
            try {
                fromJson(new JSONObject(jsonString));
                //   Log.i("tried","jj");
            } catch (JSONException e) {
                e.printStackTrace();
            }
            jsonString = null;
        }

        updateSeekBars(false);
    }

    public void enableBars(boolean enabled) {
        for (ScheduleBar bar : scheduleBars.values()) {
            bar.setEnabled(enabled);
        }
    }

    private void updateSeekBars() {

        for (Day day : Day.values()) {
            int n = 1;
            while (n < 7) {
                int prev = day.getValue() - n;
                while (prev < 0)
                    prev += 7;
//                Log.i("Day", Day.get(prev).toString());
                ThumbPiece tempThumb = getScheduleBar(Day.get(prev)).getRightMostThumb();
                if (tempThumb != null) {
                    getScheduleBar(day).updateLeftTemp(tempThumb.getTemp());
                    n = 10;
                    continue;
                }
                n++;
            }
        }
        saveToPrefs();
    }

    private void updateSeekBars(boolean save) {

        for (Day day : Day.values()) {
            int n = 1;
            while (n < 7) {
                int prev = day.getValue() - n;
                while (prev < 0)
                    prev += 7;
//                Log.i("Day", Day.get(prev).toString());
                ThumbPiece tempThumb = getScheduleBar(Day.get(prev)).findRightMostThumb();
                if (tempThumb != null) {
                    getScheduleBar(day).updateLeftTemp(tempThumb.getTemp());
                    n = 10;
                    continue;
                }
                n++;
            }
        }
        if (save)
            saveToPrefs();
    }

    public ScheduleBar getScheduleBar(Day day) {
        return scheduleBars.get(day);
    }


    // TODO: Rename method, update argument and hook method into UI event
    public void onButtonPressed(Uri uri) {
        if (mListener != null) {
            mListener.onFragmentInteraction(uri);
        }
    }


    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (OnFragmentInteractionListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
//        saveToPrefs();
    }

    public enum Day {
        SUNDAY(0),
        MONDAY(1),
        TUESDAY(2),
        WEDNESDAY(3),
        THURSDAY(4),
        FRIDAY(5),
        SATURDAY(6);

        private final int value;

        private Day(int value) {
            this.value = value;
        }

        public static Day get(int value) {
            for (Day day : Day.values())
                if (day.getValue() == value)
                    return day;
            return null;
        }

        public int getValue() {
            return value;
        }


    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p/>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        public void onFragmentInteraction(Uri uri);
    }

}
