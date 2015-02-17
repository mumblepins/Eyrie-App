/*
Copyright 2014 Stephan Tittel and Yahoo Inc.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package com.yahoo.mobile.client.android.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.RectF;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ViewConfiguration;
import android.widget.ImageView;

import com.dansull.eyrie.R;
import com.dansull.eyrie.ScheduleFragment;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;

import static android.view.ViewConfiguration.getLongPressTimeout;
import static com.yahoo.mobile.client.android.util.PixelUtil.pxToDp;

//import android.util.Log;

/**
 * Widget that lets users select a minimum and maximum value on a given numerical range.
 * The range value types can be one of Long, Double, Integer, Float, Short, Byte or BigDecimal.<br />
 * <br />
 * Improved {@link MotionEvent} handling for smoother use, anti-aliased painting for improved aesthetics.
 * <p/>
 * <p/>
 * <p/>
 * <p/>
 * <p/>
 * https://code.google.com/p/range-seek-bar/
 * <p/>
 * Apache License
 * <p/>
 * <p/>
 *
 * @author Stephan Tittel (stephan.tittel@kom.tu-darmstadt.de)
 * @author Peter Sinnott (psinnott@gmail.com)
 * @author Thomas Barrasso (tbarrasso@sevenplusandroid.org)
 * @author Alex Florescu (florescu@yahoo-inc.com)
 * @author Michael Keppler (bananeweizen@gmx.de)
 */
public class ScheduleBar extends ImageView {
    public static final int HEIGHT_IN_DP = 80;
    /**
     * Default color of a {@link ScheduleBar}, #FF33B5E5. This is also known as "Ice Cream Sandwich" blue.
     */
    public static final int DEFAULT_COLOR = Color.argb(0xFF, 0x33, 0xB5, 0xE5);
    /**
     * An invalid pointer id.
     */
    public static final int INVALID_POINTER_ID = 255;
    private int mActivePointerId = INVALID_POINTER_ID;
    private static final String THUMB_LIST_KEY = "thumbList";
    private static final int THUMB_BUFFER = 3;
    private static final int INITIAL_PADDING_IN_DP = 15;
    private static final int DEFAULT_TEXT_SIZE_IN_DP = 12;
    private static final int DEFAULT_TEXT_DISTANCE_TO_BUTTON_IN_DP = 8;
    private static final int DEFAULT_TEXT_DISTANCE_TO_TOP_IN_DP = 36;
    private static final int absoluteMinValue = 0;
    private static final int absoluteMaxValue = 95;
    private final int LINE_HEIGHT_IN_DP = 10;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Bitmap thumbImage = BitmapFactory.decodeResource(getResources(), R.drawable.seek_thumb_normal);
    private final Bitmap thumbPressedImage = BitmapFactory.decodeResource(getResources(),
            R.drawable.seek_thumb_pressed);
    private final Bitmap thumbDisabledImage = BitmapFactory.decodeResource(getResources(),
            R.drawable.seek_thumb_disabled);
    private final float thumbWidth = thumbImage.getWidth();
    private final float thumbHalfWidth = 0.5f * thumbWidth;
    private final float thumbHalfHeight = 0.5f * thumbImage.getHeight();
    CountDownTimer longPressTimer;
    private ScheduleFragment.Day day;
    private float INITIAL_PADDING;
    private float padding;
    //    private NumberType numberType;
    private double absoluteMinValuePrim = 0d;
    private double absoluteMaxValuePrim = 95d;
    private float mTopOfThumb, mBottomOfThumb;
    private int pressedThumb = -1;
    private boolean notifyWhileDragging = false;
    private OnRangeSeekBarChangeListener barChangeListener;
    private OnLongPressListener longPressListener;
    private float mDownMotionX, mDownMotionY;
    private int mScaledTouchSlop;
    private boolean mIsDragging;
    private int mTextOffset;
    private int mTextSize;
    private int mDistanceToTop;
    private RectF mRect;
    private boolean mSingleThumb;
    private List<ThumbPiece> thumbs = new ArrayList<>();
    private MoveDirection moveDirection;
    private boolean longPressPossible = false;
    private long longPressStart;
    private int maxCurrentDrag;
    private int minCurrentDrag;
    private int lastDesiredValue;
    private int mLeftTemp = 0;
    private ThumbPiece mRightMostThumb;

    public ScheduleBar(Context context) {
        super(context);
        init(context, null);
    }

    public ScheduleBar(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public ScheduleBar(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context, attrs);
    }


    public void clearThumbs() {
        this.thumbs = new ArrayList<>();
        invalidate();
    }


    public void addThumb(int temp, int loc) {
        // we attempt to keep it away from other thumbs;
        //int loc=0;
        if (thumbs.size() > 0) {
            for (int i = 0; i <= absoluteMaxValue; i++) {
                int tempLoc = loc + i;
                int dist = Math.abs(findClosestThumb(i) - tempLoc);
                if (dist > 4) {
                    loc = tempLoc;
                    break;
                }
                tempLoc = loc - i;
                dist = Math.abs(findClosestThumb(i) - tempLoc);
                if (dist > 4) {
                    loc = tempLoc;
                    break;
                }
            }
        }
        thumbs.add(
                new ThumbPiece(getContext(), padding,
                        getWidth(), paint, mTextOffset, mTextSize,
                        thumbHalfHeight, thumbHalfWidth, thumbImage, thumbPressedImage));
        thumbs.get(thumbs.size() - 1).setTemp(temp);
        thumbs.get(thumbs.size() - 1).setSelectedValue(loc);


        updateThumbWidths();
        mRightMostThumb = findRightMostThumb();
        invalidate();
    }

    private int findClosestThumb(int loc) {
        int closest = 200;
        int closestVal = -1;
        for (int i = 0; i < thumbs.size(); i++) {
            if (i == pressedThumb) {
                // prevent detecting the one that is already pressed
                continue;
            }
            int dist = Math.abs(loc - thumbs.get(i).getValue());
            if (dist < closest) {
                closest = dist;
                closestVal = thumbs.get(i).getValue();
                //Log.i("closests", String.valueOf(closest));
            }
        }
        return closestVal;
    }

    private void init(Context context, AttributeSet attrs) {
//        if (attrs == null) {
//            setRangeToDefaultValues();
//        } else {
//            TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.ScheduleBar, 0, 0);
//            setRangeValues(
//                    extractNumericValueFromAttributes(a, R.styleable.ScheduleBar_absoluteMinValue, DEFAULT_MINIMUM),
//                    extractNumericValueFromAttributes(a, R.styleable.ScheduleBar_absoluteMaxValue, DEFAULT_MAXIMUM));
//            mSingleThumb = a.getBoolean(R.styleable.ScheduleBar_singleThumb, false);
//            a.recycle();
//        }


//        setValuePrimAndNumberType();

        final ScheduleBar thisbar = this;

//        thumbs.add(new ThumbPiece());
//        thumbs.add(new ThumbPiece());
//        thumbs.add(new ThumbPiece());

        INITIAL_PADDING = PixelUtil.dpToPx(context, INITIAL_PADDING_IN_DP);

        mTextSize = PixelUtil.dpToPx(context, DEFAULT_TEXT_SIZE_IN_DP);
        mDistanceToTop = PixelUtil.dpToPx(context, DEFAULT_TEXT_DISTANCE_TO_TOP_IN_DP);
        mTextOffset = this.mTextSize + PixelUtil.dpToPx(context,
                DEFAULT_TEXT_DISTANCE_TO_BUTTON_IN_DP) + this.mDistanceToTop;

        float lineHeight = PixelUtil.dpToPx(context, LINE_HEIGHT_IN_DP);
        mRect = new RectF(padding,
                mTextOffset + thumbHalfHeight - lineHeight / 2,
                getWidth() - padding,
                mTextOffset + thumbHalfHeight + lineHeight / 2);

        mTopOfThumb = mTextOffset + thumbHalfHeight * 2;
        mBottomOfThumb = mTextOffset;

        // make ScheduleBar focusable. This solves focus handling issues in case EditText widgets are being used along with the ScheduleBar within ScollViews.
        setFocusable(true);
        setFocusableInTouchMode(true);
        mScaledTouchSlop = ViewConfiguration.get(getContext()).getScaledTouchSlop();

        mRightMostThumb = findRightMostThumb();
    }

//    public void setRangeValues(T minValue, T maxValue) {
//        this.absoluteMinValue = minValue;
//        this.absoluteMaxValue = maxValue;
//        setValuePrimAndNumberType();
//    }

//    @SuppressWarnings("unchecked")
//    // only used to set default values when initialised from XML without any values specified
//    private void setRangeToDefaultValues() {
//        this.absoluteMinValue = (T) DEFAULT_MINIMUM;
//        this.absoluteMaxValue = (T) DEFAULT_MAXIMUM;
//        setValuePrimAndNumberType();
//    }

//    private void setValuePrimAndNumberType() {
//        absoluteMinValuePrim = absoluteMinValue.doubleValue();
//        absoluteMaxValuePrim = absoluteMaxValue.doubleValue();
//        numberType = NumberType.fromNumber(absoluteMinValue);
//    }

    public void resetSelectedValues() {
    }

    public boolean isNotifyWhileDragging() {
        return notifyWhileDragging;
    }

    /**
     * Should the widget notify the listener callback while the user is still dragging a thumb? Default is false.
     *
     * @param flag
     */
    public void setNotifyWhileDragging(boolean flag) {
        this.notifyWhileDragging = flag;
    }


    /**
     * Registers given listener callback to notify about changed selected values.
     *
     * @param listener The listener to notify about changed selected values.
     */
    public void setOnRangeSeekBarChangeListener(OnRangeSeekBarChangeListener listener) {
        this.barChangeListener = listener;
    }


    public void setOnLongPressListener(OnLongPressListener listener) {
        this.longPressListener = listener;
    }

    /**
     * Handles thumb selection and movement. Notifies listener callback on certain events.
     */
    @Override
    public boolean onTouchEvent(MotionEvent event) {

        if (!isEnabled()) {
            return false;
        }

        int pointerIndex;

        final int action = event.getAction();
        switch (action & MotionEvent.ACTION_MASK) {

            case MotionEvent.ACTION_DOWN:
                // Remember where the motion event started
                mActivePointerId = event.getPointerId(event.getPointerCount() - 1);
                pointerIndex = event.findPointerIndex(mActivePointerId);
                mDownMotionX = event.getX(pointerIndex);
                mDownMotionY = event.getY(pointerIndex);

                if (mDownMotionY < mTopOfThumb && mDownMotionY > mBottomOfThumb)
                    pressedThumb = evalPressedThumb(mDownMotionX);
                else
                    pressedThumb = -1;

                if (pressedThumb == -1) {
                    // we may have a long press
                    longPressPossible = true;
                    longPressStart = System.currentTimeMillis();
                    final ScheduleBar thisBar = this;
                    longPressTimer = new CountDownTimer(getLongPressTimeout(), getLongPressTimeout()) {
                        @Override
                        public void onTick(long millisUntilFinished) {

                        }

                        @Override
                        public void onFinish() {
                            if (longPressPossible) {
                                longPressPossible = false;
//                                Toast.makeText(getContext(),"Toasty",Toast.LENGTH_LONG).show();
                                if (longPressListener != null)
                                    longPressListener.onLongPressListener(thisBar, screenToValue(mDownMotionX));
                            }

                        }
                    }.start();

                    //return super.onTouchEvent(event);
                } else {
                    thumbs.get(pressedThumb).setPressed(true);

                    maxCurrentDrag = findThumbMax(screenToValue(mDownMotionX));
                    minCurrentDrag = findThumbMin(screenToValue(mDownMotionX));
//                setPressed(true);
                    invalidate();
                    //onStartTrackingTouch();
                    // trackTouchEvent(event);
                    attemptClaimDrag();
                }

                break;
            case MotionEvent.ACTION_MOVE:
                pointerIndex = event.findPointerIndex(mActivePointerId);
                final float x = event.getX(pointerIndex);
                final float y = event.getY(pointerIndex);
                if (pressedThumb != -1) {

                    if (mIsDragging) {
                        trackTouchEvent(event);
                    } else {
                        // Scroll to follow the motion event
                        if (Math.abs(x - mDownMotionX) > mScaledTouchSlop) {
//                            thumbs.get(pressedThumb).setPressed(true);
                            moveDirection = MoveDirection.HORIZONTAL;

                            onStartTrackingTouch();
                            trackTouchEvent(event);
                            attemptClaimDrag();
                        }
                        if (Math.abs(y - mDownMotionY) > mScaledTouchSlop) {
//                            thumbs.get(pressedThumb).setPressed(true);
                            moveDirection = MoveDirection.VERTICAL;

                            onStartTrackingTouch();
                            trackTouchEvent(event);
                            attemptClaimDrag();
                        }
                    }

                    if (notifyWhileDragging && barChangeListener != null) {
                        barChangeListener.onRangeSeekBarValuesChanged(this, thumbs);
                    }
                } else {

                    if ((Math.abs(x - mDownMotionX) > mScaledTouchSlop) || (Math.abs(y - mDownMotionY) > mScaledTouchSlop))
                        longPressPossible = false;
                }
                invalidate();
                break;
            case MotionEvent.ACTION_UP:
                longPressPossible = false;
                if (mIsDragging) {
                    trackTouchEvent(event);
                    onStopTrackingTouch();
                    if (barChangeListener != null) {
                        barChangeListener.onRangeSeekBarValuesChanged(this, thumbs);
                    }


                }
// else {
//                    // Touch up when we never crossed the touch slop threshold
//                    // should be interpreted as a tap-seek to that location.
//
//                    //TODO: Fix?
//                    onStartTrackingTouch();
//                    trackTouchEvent(event);
//                    onStopTrackingTouch();
//                }
                setAllPressedOff();
                pressedThumb = -1;
                invalidate();

                break;
//            case MotionEvent.ACTION_POINTER_DOWN: {
//                final int index = event.getPointerCount() - 1;
//                // final int index = ev.getActionIndex();
//                mDownMotionX = event.getX(index);
//                mActivePointerId = event.getPointerId(index);
//                invalidate();
//                break;
//            }
//            case MotionEvent.ACTION_POINTER_UP:
//                // onSecondaryPointerUp(event);
//                invalidate();
//                break;
            case MotionEvent.ACTION_CANCEL:
                longPressPossible = false;
                if (mIsDragging) {
                    onStopTrackingTouch();
                    setAllPressedOff();
                }
                invalidate(); // see above explanation
                break;
        }
        return true;
    }

    private void setAllPressedOff() {
        for (int i = 0; i < thumbs.size(); i++) {
            thumbs.get(i).setPressed(false);
        }
    }

    private int findThumbMax(int loc) {
        int max = 95 + THUMB_BUFFER;
        for (int i = 0; i < thumbs.size(); i++) {
            if (i == pressedThumb) {
                // prevent detecting the one that is already pressed
                continue;
            }
            if ((thumbs.get(i).getValue() > loc) &&
                    thumbs.get(i).getValue() < max) {
                max = thumbs.get(i).getValue();

            }
        }
        return max;
    }

    public ThumbPiece findRightMostThumb() {
        int max = 0;
        ThumbPiece thumbPiece = null;
        if (thumbs.size() > 0) {
            for (int i = 0; i < thumbs.size(); i++) {
                if (max < thumbs.get(i).getValue()) {
                    max = thumbs.get(i).getValue();
                    thumbPiece = thumbs.get(i);
                }
            }
        }
        return thumbPiece;
    }

    public ThumbPiece getRightMostThumb() {
        return mRightMostThumb;
    }

    private ThumbPiece findThumbToLeft(int loc) {
        ThumbPiece thumbPiece = null;
        int min = 0 - THUMB_BUFFER;
        for (int i = 0; i < thumbs.size(); i++) {
            if (thumbs.get(i).getValue() == loc) {
                // prevent detecting the one that we are looking at
                continue;
            }
            if ((thumbs.get(i).getValue() < loc) &&
                    thumbs.get(i).getValue() > min) {
                min = thumbs.get(i).getValue();
                thumbPiece = thumbs.get(i);

            }
        }
        return thumbPiece;
    }

    private int findThumbMin(int loc) {
        int min = 0 - THUMB_BUFFER;
        for (int i = 0; i < thumbs.size(); i++) {
            if (i == pressedThumb) {
                // prevent detecting the one that is already pressed
                continue;
            }
            if ((thumbs.get(i).getValue() < loc) &&
                    thumbs.get(i).getValue() > min) {
                min = thumbs.get(i).getValue();

            }
        }
        return min;
    }

    private void trackTouchEvent(MotionEvent event) {
        final int pointerIndex = event.findPointerIndex(mActivePointerId);
        final float x = event.getX(pointerIndex);
        final float y = event.getY(pointerIndex);
//        Log.i("x", String.valueOf(x));
//        Log.i("y", String.valueOf(y));
        if (moveDirection == MoveDirection.HORIZONTAL) {
            if (thumbs.size() > 1) {
                int desiredValue = screenToValue(x);
                if ((desiredValue <= maxCurrentDrag - THUMB_BUFFER) &&
                        (desiredValue >= minCurrentDrag + THUMB_BUFFER)) {// &&
                    // (desiredValue < maxCurrentDrag + 2) &&
                    //(desiredValue > minCurrentDrag + 2)) {
                    thumbs.get(pressedThumb).setSelectedValue(desiredValue);
                } else if (desiredValue > maxCurrentDrag - THUMB_BUFFER) {
                    //don't let it move any further
                    thumbs.get(pressedThumb).setSelectedValue(maxCurrentDrag - THUMB_BUFFER);
                } else {
                    thumbs.get(pressedThumb).setSelectedValue(minCurrentDrag + THUMB_BUFFER);
                }

            } else {
                thumbs.get(pressedThumb).setScreenValue(x);
            }
        } else {
            thumbs.get(pressedThumb).changeTemp(pxToDp(getContext(), (int) (mDownMotionY - y)));
            //    thumbs.get(0).setTemp((int) (y - mDownMotionY));

        }

    }

    /**
     * Tries to claim the user's drag motion, and requests disallowing any ancestors from stealing events in the drag.
     */
    private void attemptClaimDrag() {
        if (getParent() != null) {
            getParent().requestDisallowInterceptTouchEvent(true);
        }
    }

    /**
     * This is called when the user has started touching this widget.
     */
    void onStartTrackingTouch() {
        mIsDragging = true;
    }

    /**
     * This is called when the user either releases his touch or the touch is canceled.
     */
    void onStopTrackingTouch() {
        mIsDragging = false;
    }

    /**
     * Ensures correct size of the widget.
     */
    @Override
    protected synchronized void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = 200;
        if (MeasureSpec.UNSPECIFIED != MeasureSpec.getMode(widthMeasureSpec)) {
            width = MeasureSpec.getSize(widthMeasureSpec);
        }
        int height = thumbImage.getHeight() + PixelUtil.dpToPx(getContext(), HEIGHT_IN_DP);
        //if (MeasureSpec.UNSPECIFIED != MeasureSpec.getMode(heightMeasureSpec)) {
        //    height = Math.min(height, MeasureSpec.getSize(heightMeasureSpec));
        // }
        setMeasuredDimension(width, height);
    }


    public void updateLeftTemp(int temp) {
        mLeftTemp = temp;
        invalidate();
    }

    void updateThumbWidths() {
        for (ThumbPiece thumb : thumbs) {
            thumb.updateWidth(getWidth(), padding);
        }

    }

    /**
     * Draws the widget on the given canvas.
     */
    @Override
    protected synchronized void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        paint.setTextSize(mTextSize);
        paint.setStyle(Style.FILL);
        paint.setColor(Color.GRAY);
        paint.setAntiAlias(true);

//        Log.i("parent", String.valueOf(getParent()));
        // draw min and max labels
        // String minLabel = getContext().getString(R.string.min_label);
        // String maxLabel = getContext().getString(R.string.max_label);
        float minMaxLabelSize = paint.measureText("");
//        float minMaxHeight = mTextOffset + thumbHalfHeight + mTextSize / 3;
//        canvas.drawText(minLabel, 0, minMaxHeight, paint);
//        canvas.drawText(maxLabel, getWidth() - minMaxLabelSize, minMaxHeight, paint);
        padding = INITIAL_PADDING + minMaxLabelSize + thumbHalfWidth;

        updateThumbWidths();
        // draw seek bar background line
        mRect.left = padding;
        mRect.right = getWidth() - padding;
//        Log.i(String.valueOf(day), String.valueOf(mRect.left) + "  " + String.valueOf(mRect.right) + "  " + String.valueOf(getWidth()));
        canvas.drawRect(mRect, paint);

        boolean selectedValuesAreDefault = false;
        int colorToUseForButtonsAndHighlightedLine = selectedValuesAreDefault ?
                Color.GRAY :    // default values
                DEFAULT_COLOR; //non default, filter is active

        // draw seek bar active range line
        // TODO: fix
        //  mRect.left = thumbs.get(0).getScreenValue();
        // mRect.right = thumbs.get(1).getScreenValue();

        float[] hsv = new float[3];


        Color.colorToHSV(colorToUseForButtonsAndHighlightedLine, hsv);

        if ((thumbs.size() > 0) && (mRightMostThumb != null)) {
            for (ThumbPiece thumb : thumbs) {
                ThumbPiece leftThumb = findThumbToLeft(thumb.getValue());
                mRect.right = thumb.getScreenValue();

                //TODO: get color from previous day
                if (leftThumb == null) {
                    mRect.left = padding;
                    hsv[0] = ThumbPiece.scaleHue(mLeftTemp);
                } else {
                    mRect.left = leftThumb.getScreenValue();
                    hsv[0] = leftThumb.getHue();
                }
                paint.setColor(Color.HSVToColor(Color.alpha(colorToUseForButtonsAndHighlightedLine), hsv));
                canvas.drawRect(mRect, paint);
                if (mRightMostThumb.getValue() == thumb.getValue()) {
                    // draw to edge of bar
                    mRect.left = thumb.getScreenValue();
                    mRect.right = getWidth() - padding;
                    hsv[0] = thumb.getHue();
                    paint.setColor(Color.HSVToColor(Color.alpha(colorToUseForButtonsAndHighlightedLine), hsv));
                    canvas.drawRect(mRect, paint);
                }
            }
            for (int i = 0; i < thumbs.size(); i++) {
                thumbs.get(i).draw(canvas);
            }
        } else {
            mRect.left = padding;
            mRect.right = getWidth() - padding;
            hsv[0] = ThumbPiece.scaleHue(mLeftTemp);
            paint.setColor(Color.HSVToColor(Color.alpha(colorToUseForButtonsAndHighlightedLine), hsv));
            canvas.drawRect(mRect, paint);
        }


        //Long press detection
//        if (longPressPossible) {
//            if (System.currentTimeMillis() > longPressStart + getLongPressTimeout()) {
//                longPressPossible = false;
//                if (longPressListener != null)
//                    longPressListener.onLongPressListener(this);
//            }
//        }


    }


    public JSONArray toJson() {
        JSONArray returnArray = new JSONArray();
        if (thumbs.isEmpty()) {
            return returnArray;
        }
        for (ThumbPiece thumb : thumbs) {
            returnArray.put(thumb.toJson());
        }
        return returnArray;
    }

    public void fromJson(JSONArray json) {
        if (json.length() > 0) {
            for (int i = 0; i < json.length(); i++) {
                thumbs.add(i,
                        new ThumbPiece(getContext(), padding,
                                getWidth(), paint, mTextOffset, mTextSize,
                                thumbHalfHeight, thumbHalfWidth, thumbImage, thumbPressedImage));
                try {
                    thumbs.get(i).fromJson(json.getJSONObject(i));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }

        mRightMostThumb = findRightMostThumb();
        updateThumbWidths();
    }

    /**
     * Overridden to save instance state when device orientation changes. This method is called automatically if you assign an id to the ScheduleBar widget using the {@link #setId(int)} method. Other members of this class than the normalized min and max values don't need to be saved.
     */
    @Override
    protected Parcelable onSaveInstanceState() {
        final Bundle bundle = new Bundle();
        bundle.putParcelable("SUPER", super.onSaveInstanceState());

        return bundle;
    }

    /**
     * Overridden to restore instance state when device orientation changes. This method is called automatically if you assign an id to the ScheduleBar widget using the {@link #setId(int)} method.
     */
    @Override
    protected void onRestoreInstanceState(Parcelable parcel) {
        final Bundle bundle = (Bundle) parcel;
        super.onRestoreInstanceState(bundle.getParcelable("SUPER"));

        mRightMostThumb = findRightMostThumb();
//        setThumbs((List<ThumbPiece>) bundle.getSerializable(THUMB_LIST_KEY));

    }


    /**
     * Decides which (if any) thumb is touched by the given x-coordinate.
     *
     * @param touchX The x-coordinate of a touch event in screen space.
     * @return The pressed thumb or null if none has been touched.
     */
    private int evalPressedThumb(float touchX) {
        int result = -1;
//        Log.i("a", "1");
        List<Integer> activeThumbs = new ArrayList<>();
        for (int i = 0; i < thumbs.size(); i++) {
            if (thumbs.get(i).isInThumbRange(touchX)) {
//
//                Log.i("a", "2");
                activeThumbs.add(i);
            }
        }
        switch (activeThumbs.size()) {
            case 0:
                return result;
            case 1:
                return activeThumbs.get(0);
            default:
                float closest = 999900;
                int closeThumb = -1;
                for (int i = 0; i < activeThumbs.size(); i++) {
                    //Log.i(String.valueOf(closest), String.valueOf(closeThumb));
                    float val = Math.abs(thumbs.get(activeThumbs.get(i)).getScreenValue() - touchX);
//                    Log.i("val", String.valueOf(val));
                    if (val < closest) {
                        closest = val;
//                        Log.i("closest", String.valueOf(closest));
                        closeThumb = activeThumbs.get(i);
//                        Log.i("closeThumb", String.valueOf(closeThumb));

                    }
                }
                return closeThumb;
        }

    }


    public void onTouchEvent() {
    }

    public List<ThumbPiece> getThumbs() {
        return thumbs;
    }

    public void setThumbs(List<ThumbPiece> thumbs) {
        this.thumbs = new ArrayList<>();
        new ArrayList<>();
        for (ThumbPiece thumbPiece : thumbs) {
            this.thumbs.add(thumbPiece.clone());
        }
        invalidate();
    }


    private int screenToValue(float screenCoord) {
        int width = getWidth();
        double result = (screenCoord - padding) / (width - 2 * padding);
        double normalized = Math.min(1d, Math.max(0d, result));
        double v = absoluteMinValuePrim + normalized * (absoluteMaxValuePrim - absoluteMinValuePrim);
        return (int) (Math.round(v * 100) / 100d);
    }

    public ScheduleFragment.Day getDay() {
        return this.day;
    }

    public void setDay(ScheduleFragment.Day day) {
        this.day = day;
    }


    /**
     * Thumb constants (min and max).
     */
    private static enum Thumb {
        MIN, MAX
    }

    private static enum MoveDirection {
        VERTICAL, HORIZONTAL
    }

    /**
     * Callback listener interface to notify about changed range values.
     *
     * @param < The Number type the RangeSeekBar has been declared with.
     * @author Stephan Tittel (stephan.tittel@kom.tu-darmstadt.de)
     */
    public interface OnRangeSeekBarChangeListener {
        public void onRangeSeekBarValuesChanged(ScheduleBar bar, List<?> thumbPieceList);
    }

    public interface OnLongPressListener {
        public void onLongPressListener(ScheduleBar bar, int nearestValue);
    }


}