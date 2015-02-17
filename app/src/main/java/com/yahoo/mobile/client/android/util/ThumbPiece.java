package com.yahoo.mobile.client.android.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

import com.dansull.eyrie.MainActivity;

import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.json.JSONException;
import org.json.JSONObject;

//import android.util.Log;

/**
 * Created by Daniel on 2/8/2015.
 */
public class ThumbPiece {
    public static final int TEXT_LATERAL_PADDING_IN_DP = 3;
    private static final int ABSOLUTE_MAXIMUM_TEMP = 100;
    private static final int ABSOLUTE_MINIMUM_TEMP = 40;
    private static double absoluteMinValuePrim = 0d;
    private static double absoluteMaxValuePrim = 95d;
    private int temp, startTemp;
    private DateTimeFormatter dtf = DateTimeFormat.forPattern("h:mm\na");
    private double normalizedValue;
    private boolean pressed = false;
    private float padding;
    private int width;
    private Context context;
    private Paint paint;
    private int mTextOffset;
    private int mTextSize;
    private float thumbHalfWidth;
    private float thumbHalfHeight;
    private Bitmap thumbPressedImage;
    private Bitmap thumbImage;

    public ThumbPiece(Context context,
                      float padding, int width, Paint paint, int mTextOffset, int mTextSize,
                      float thumbHalfHeight, float thumbHalfWidth, Bitmap thumbImage, Bitmap thumbPressedImage) {
        this.context = context;
        this.absoluteMaxValuePrim = absoluteMaxValuePrim;
        this.absoluteMinValuePrim = absoluteMinValuePrim;
        this.padding = padding;
        this.width = width;
        this.paint = paint;
        this.mTextOffset = mTextOffset;
        this.mTextSize = mTextSize;
        this.thumbHalfHeight = thumbHalfHeight;
        this.thumbHalfWidth = thumbHalfWidth;
        this.thumbImage = thumbImage;
        this.thumbPressedImage = thumbPressedImage;
    }

    // hue-range: [0, 360] -> Default = 0
    public static Bitmap changeHue(Bitmap bitmap, int hue) {
        Bitmap newBitmap=bitmap.copy(bitmap.getConfig(),true);

        int width = bitmap.getWidth();
        int height = bitmap.getHeight();

        int size = width * height;
        int[] all_pixels = new int[width * height];
        int top = 0;
        int left = 0;
        int offset = 0;
        int stride = width;
        bitmap.getPixels(all_pixels, offset, stride, top, left, width, height);

        int pixel = 0;
        int alpha = 0;
        float[] hsv = new float[3];

        for (int i = 0; i < size; i++) {
            pixel = all_pixels[i];
            alpha = Color.alpha(pixel);
            Color.colorToHSV(pixel, hsv);
            hsv[0] = hue;
            all_pixels[i] = Color.HSVToColor(alpha, hsv);

        }
        newBitmap.setPixels(all_pixels, offset, stride, top, left, width, height);
        return newBitmap;
    }

    public static int scaleHue(int temp) {
        // 50 scales to 200
        // 90 scales to 0
        float value = 450f - 5f * (float) temp;
        return Math.round(Math.max(Math.min(value, 200f), 0f));
    }

    /**
     * Converts the given Number value to a normalized double.
     *
     * @param value The Number value to normalize.
     * @return The normalized double.
     */
    public static double valueToNormalized(int value) {
        if (0 == absoluteMaxValuePrim - absoluteMinValuePrim) {
            // prevent division by zero, simply return 0.
            return 0d;
        }
        //  Log.i("value", String.valueOf(((double) value - absoluteMinValuePrim) / (absoluteMaxValuePrim - absoluteMinValuePrim)));
        return ((double) value - absoluteMinValuePrim) / (absoluteMaxValuePrim - absoluteMinValuePrim);
    }

    public JSONObject toJson() {
        JSONObject returnJSON = new JSONObject();
        try {
            returnJSON.putOpt("temp", temp);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        try {
            returnJSON.putOpt("value", normalizedValue);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return returnJSON;
    }

    public void fromJson(JSONObject json) {
        temp = json.optInt("temp", temp);
        normalizedValue = json.optDouble("value", normalizedValue);
        updateHues();

    }

    public void updateWidth(int width, float padding) {
        this.width = width;
        this.padding = padding;
    }

    @Override
    public ThumbPiece clone() {
        ThumbPiece ret = new ThumbPiece(context, padding,
                width, paint, mTextOffset, mTextSize,
                thumbHalfHeight, thumbHalfWidth, thumbImage, thumbPressedImage);
        ret.setTemp(temp);
        ret.setNormalizedValue(normalizedValue);
        ret.setPressed(pressed);
        return ret;
    }

    /**
     * Sets the currently selected maximum value. The widget will be invalidated and redrawn.
     *
     * @param value The Number value to set the maximum value to. Will be clamped to given absolute minimum/maximum range.
     */
    public void setSelectedValue(int value) {
        // in case absoluteMinValue == absoluteMaxValue, avoid division by zero when normalizing.
        if (0 == (absoluteMaxValuePrim - absoluteMinValuePrim)) {
            setNormalizedValue(1d);
        } else {
            setNormalizedValue(valueToNormalized(value));
        }
    }

    public float getScreenValue() {
        return normalizedToScreen(normalizedValue);
    }

    public void setScreenValue(float screenValue) {
        normalizedValue = screenToNormalized(screenValue);
    }

    public double getNormalizedValue() {
        return normalizedValue;
    }

    /**
     * Sets normalized min value to value so that 0 <= value <= normalized max value <= 1. The View will get invalidated when calling this method.
     *
     * @param value The new normalized min value to set.
     */
    private void setNormalizedValue(double value) {
        normalizedValue = Math.max(0d, Math.min(1d, value));
        //Log.i("norm", String.valueOf(normalizedValue));
    }

    public int getValue() {
        return normalizedToValue(normalizedValue);
    }

    /**
     * Converts a normalized value to a Number object in the value space between absolute minimum and maximum.
     *
     * @param normalized
     * @return
     */
    @SuppressWarnings("unchecked")
    private int normalizedToValue(double normalized) {
        double v = absoluteMinValuePrim + normalized * (absoluteMaxValuePrim - absoluteMinValuePrim);
        return (int) (Math.round(v * 100) / 100d);
    }

    /**
     * Converts a normalized value into screen space.
     *
     * @param normalizedCoord The normalized value to convert.
     * @return The converted value in screen space.
     */
    private float normalizedToScreen(double normalizedCoord) {
        return (float) (padding + normalizedCoord * (width - 2 * padding));
    }

    /**
     * Converts screen space x-coordinates into normalized values.
     *
     * @param screenCoord The x-coordinate in screen space to convert.
     * @return The normalized value.
     */
    private double screenToNormalized(float screenCoord) {

        if (width <= 2 * padding) {
            // prevent division by zero, simply return 0.
            return 0d;
        } else {
            double result = (screenCoord - padding) / (width - 2 * padding);
            return Math.min(1d, Math.max(0d, result));
        }
    }

    public int getTemp() {
        return temp;
    }

    public void setTemp(int temp) {
//            Log.i("setCalled", "called");
        this.temp = temp;
        updateHues();
    }

    private void updateHues() {

        thumbImage = changeHue(thumbImage, getHue());

        thumbPressedImage = changeHue(thumbPressedImage, getHue());

    }

    public int getHue() {
        return scaleHue(temp);
    }

    public void draw(Canvas canvas) {
        int scaledHue = scaleHue(getTemp());
        int offset = PixelUtil.dpToPx(context, TEXT_LATERAL_PADDING_IN_DP);
//        Log.i("value", String.valueOf(normalizedValue));
        drawThumb(normalizedToScreen(normalizedValue), canvas, scaledHue);
        paint.setTextSize(mTextSize);
        paint.setColor(Color.BLACK);
        String text = MainActivity.intToDate(getValue()).toString(dtf);
        float textWidth = paint.measureText(text) + offset;
        canvas.save();
        canvas.rotate(-90, normalizedToScreen(normalizedValue) - thumbHalfWidth,
                mTextOffset);
        canvas.drawText(text,
                normalizedToScreen(normalizedValue) - thumbHalfWidth,
                mTextOffset + mTextSize * 1.7f,
                paint);
        canvas.drawText(String.valueOf(getTemp()),
                normalizedToScreen(normalizedValue) - thumbHalfHeight * 3 - offset - paint.measureText(String.valueOf(getTemp())),
                mTextOffset + mTextSize * 1.7f,
                paint);
        canvas.restore();
    }

    private void drawThumb(float screenCoord, Canvas canvas, int hue) {
        Bitmap buttonToDraw;
        buttonToDraw = pressed ? thumbPressedImage : thumbImage;
//        Bitmap buttonToDraw2 = buttonToDraw.copy(buttonToDraw.getConfig(), true);
//        changeHue(buttonToDraw, hue);
        canvas.drawBitmap(buttonToDraw, screenCoord - thumbHalfWidth,
                mTextOffset,
                paint);
    }

    public void changeTemp(float v) {
//        Log.i("dp", String.valueOf(v));
        temp = Math.round(v / 20f) + startTemp;
        temp = Math.min(Math.max(temp, ABSOLUTE_MINIMUM_TEMP), ABSOLUTE_MAXIMUM_TEMP);
        updateHues();
    }

    public boolean isInThumbRange(float touchX) {
        return Math.abs(touchX - normalizedToScreen(normalizedValue)) <= thumbHalfWidth;
    }

    public boolean getPressed() {
        return pressed;
    }

    public void setPressed(boolean b) {
        pressed = b;
        if (pressed == true)
            startTemp = temp;

    }


}