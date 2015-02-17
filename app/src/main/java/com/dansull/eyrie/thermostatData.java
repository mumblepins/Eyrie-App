package com.dansull.eyrie;

import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;

import org.joda.time.DateTime;
import org.joda.time.Duration;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Daniel on 2/2/2015.
 */
public class ThermostatData implements Parcelable {
    public static final Creator<ThermostatData> CREATOR = new Creator<ThermostatData>() {
        @Override
        public ThermostatData createFromParcel(Parcel source) {
            // read the bundle containing key value pairs from the parcel
            Bundle bundle = source.readBundle();
            ThermostatData data = new ThermostatData(bundle.getDouble("curTemp"), bundle.getDouble("setTemp"),
                    bundle.getDouble("outsideOffset"), bundle.getDouble("floorOffset"),
                    bundle.getDouble("percOn"), OverrideType.get(bundle.getInt("overrideType")),
                    new DateTime(bundle.getLong("overTime")), bundle.getDouble("overTemp"),
                    new DateTime(bundle.getLong("nextTime")), bundle.getDouble("nextTemp"),
                    Units.get(bundle.getInt("units")));

            return data;
        }

        @Override
        public ThermostatData[] newArray(int size) {
            return new ThermostatData[size];
        }
    };
    private List<ThermostatUpdate> listeners = new ArrayList<ThermostatUpdate>();
    private double setTemp, outsideOffset, floorOffset, curTemp;
    private double percOn, overTemp, nextTemp;
    private DateTime nextTime = DateTime.now();
    private DateTime overTime = DateTime.now();
    private OverrideType overType = OverrideType.OVERRIDE_NONE;
    private Units unit = Units.CELSIUS;
    private boolean dataHere = false;

    public ThermostatData(Units unit) {
        this.unit = unit;
    }


    public ThermostatData() {
        this.unit = Units.CELSIUS;
    }

    public ThermostatData(double curTemp, double setTemp, double outsideOffset, double floorOffset,
                          double percOn, OverrideType overType, long overTime, double overTemp,
                          long nextTime, double nextTemp, Units unit) {
        this.setTemp = setTemp;
        this.curTemp = curTemp;
        this.outsideOffset = outsideOffset;
        this.floorOffset = floorOffset;
        this.percOn = percOn;
        this.overType = overType;
        this.overTemp = overTemp;
        this.nextTemp = nextTemp;
        this.overTime = DateTime.now().plusSeconds((int) overTime);
        this.nextTime = DateTime.now().plusSeconds((int) nextTime);
        this.unit = unit;
        fireUpdate();
    }

    public ThermostatData(double curTemp, double setTemp, double outsideOffset, double floorOffset,
                          double percOn, OverrideType overType, DateTime overTime, double overTemp,
                          DateTime nextTime, double nextTemp, Units unit) {
        this.setTemp = setTemp;
        this.curTemp = curTemp;
        this.outsideOffset = outsideOffset;
        this.floorOffset = floorOffset;
        this.percOn = percOn;
        this.overType = overType;
        this.overTemp = overTemp;
        this.nextTemp = nextTemp;
        this.overTime = overTime;
        this.nextTime = nextTime;
        this.unit = unit;
        fireUpdate();
    }

    public ThermostatData(double curTemp, double setTemp, double outsideOffset, double floorOffset,
                          double percOn, OverrideType overType, long overTime, double overTemp,
                          long nextTime, double nextTemp) {
        this.setTemp = setTemp;
        this.curTemp = curTemp;
        this.outsideOffset = outsideOffset;
        this.floorOffset = floorOffset;
        this.percOn = percOn;
        this.overType = overType;
        this.overTemp = overTemp;
        this.nextTemp = nextTemp;
        this.overTime = DateTime.now().plusSeconds((int) overTime);
        this.nextTime = DateTime.now().plusSeconds((int) nextTime);
        this.unit = Units.CELSIUS;
        fireUpdate();
    }

    public void addListener(ThermostatUpdate toAdd) {
        listeners.add(toAdd);
    }

    public void removeListener(ThermostatUpdate toRemove) {
        listeners.remove(toRemove);
    }

    public void fireUpdate() {
        this.dataHere = true;
        for (ThermostatUpdate l : listeners)
            l.thermostatUpdate();
    }

    public void setAll(double curTemp, double setTemp, double outsideOffset, double floorOffset,
                       double percOn, OverrideType overType, long overTime, double overTemp,
                       long nextTime, double nextTemp) {
        this.setTemp = setTemp;
        this.curTemp = curTemp;
        this.outsideOffset = outsideOffset;
        this.floorOffset = floorOffset;
        this.percOn = percOn;
        this.overType = overType;
        this.overTemp = overTemp;
        this.nextTemp = nextTemp;
        this.overTime = DateTime.now().plusSeconds((int) overTime);
        this.nextTime = DateTime.now().plusSeconds((int) nextTime);
        fireUpdate();
    }

    public void setAllFromString(String result) {
        String[] parts = result.split("\\|");
        if (parts.length >= 12) {
            // Format:
            // current temp|reported set temp (total)
            // current scheduled setting|current override type|current override time|
            // current override temp| next scheduled time|next schedule temp|
            // preheat On| outside offset temp | offset due to other zones| zone percent on
            // prop value|integral value | derivative value| offset value
            // Ua value|Mt value
//                                                Log.i("1", String.valueOf(parseDouble(parts[0])));
//                                                Log.i("1", String.valueOf(parseDouble(parts[2])));
//                                                Log.i("1", String.valueOf(parseDouble(parts[9])));
//                                                Log.i("1", String.valueOf(parseDouble(parts[10])));
//                                                Log.i("1", String.valueOf(parseDouble(parts[11])));
//                                                Log.i("1", String.valueOf(Integer.parseInt(parts[3])));
//                                                Log.i("1", String.valueOf(Long.parseLong(parts[4])));
//                                                Log.i("1", String.valueOf(parseDouble(parts[5])));
//                                                Log.i("1", String.valueOf(Long.parseLong(parts[6])));
//                                                Log.i("1", String.valueOf(parseDouble(parts[7])));
            setAll(
                    parseDouble(parts[0]),
                    parseDouble(parts[2]),
                    parseDouble(parts[9]),
                    parseDouble(parts[10]),
                    parseDouble(parts[11]),
                    Integer.parseInt(parts[3]),
                    Long.parseLong(parts[4]),
                    parseDouble(parts[5]),
                    Long.parseLong(parts[6]),
                    parseDouble(parts[7]));

        }
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

    public void setAll(double curTemp, double setTemp, double outsideOffset, double floorOffset,
                       double percOn, int overType, long overTime, double overTemp,
                       long nextTime, double nextTemp) {
        this.setTemp = setTemp;
        this.curTemp = curTemp;
        this.outsideOffset = outsideOffset;
        this.floorOffset = floorOffset;
        this.percOn = percOn;
        setOverType(overType);
        this.overTemp = overTemp;
        this.nextTemp = nextTemp;
        this.overTime = DateTime.now().plusSeconds((int) overTime);
        this.nextTime = DateTime.now().plusSeconds((int) nextTime);
        fireUpdate();
    }

    private double convertUnitAbsolute(double in) {
        if (unit == Units.CELSIUS)
            return in;
        else return in * 1.8;
    }

    private double convertUnit(double in) {
        if (unit == Units.CELSIUS)
            return in;
        else return (in * 1.8 + 32d);
    }

    public boolean isDataHere() {
        return dataHere;
    }

    public double getSetTemp() {
        return convertUnit(setTemp);
    }

    public void setSetTemp(double setTemp) {
        this.setTemp = setTemp;
    }

    public double getOutsideOffset() {
        return convertUnitAbsolute(outsideOffset);
    }

    public void setOutsideOffset(double outsideOffset) {
        this.outsideOffset = outsideOffset;
    }

    public double getFloorOffset() {
        return convertUnitAbsolute(floorOffset);
    }

    public void setFloorOffset(double floorOffset) {
        this.floorOffset = floorOffset;
    }

    public double getCurTemp() {
        return convertUnit(curTemp);
    }

    public void setCurTemp(double curTemp) {
        this.curTemp = curTemp;
    }

    public double getPercOn() {
        return percOn;
    }

    public void setPercOn(double percOn) {
        this.percOn = percOn;
    }

    public double getOverTemp() {
        return convertUnit(overTemp);
    }

    public void setOverTemp(double overTemp) {
        this.overTemp = overTemp;
    }

    public double getNextTemp() {
        return convertUnit(nextTemp);
    }

    public void setNextTemp(double nextTemp) {
        this.nextTemp = nextTemp;
    }

    public double getTotalSetTemp() {
        return convertUnit(setTemp + outsideOffset - floorOffset);
    }

    public DateTime getNextTime() {
        // rounds to nearest quarter hour
        DateTime hour = nextTime.hourOfDay().roundFloorCopy();
        long millisSinceHour = new Duration(hour, nextTime).getMillis();
        int roundedMinutes = ((int) Math.round(millisSinceHour / 60000.0 / 15)) * 15;
        DateTime temp = hour.plusMinutes(roundedMinutes);
        return temp;
    }

    public void setNextTime(long nextTime) {
        this.nextTime = DateTime.now().plusSeconds((int) nextTime);
    }

    public void setNextTime(DateTime nextTime) {
        this.nextTime = nextTime;
    }

    public DateTime getOverTime() {
        return overTime;
    }

    public void setOverTime(DateTime overTime) {
        this.overTime = overTime;

    }

    public OverrideType getOverType() {
        return overType;
    }

    public void setOverType(OverrideType overType) {
        this.overType = overType;
    }

    public void setOverType(int overType) {
        this.overType = OverrideType.get(overType);
    }

    public boolean overridden() {
        if (overType == OverrideType.OVERRIDE_NONE)
            return false;
        else
            return true;
    }

    public Units getUnit() {
        return unit;
    }

    public void setUnit(int unit) {
        this.unit = Units.get(unit);
    }

    /**
     * Describe the kinds of special objects contained in this Parcelable's
     * marshalled representation.
     *
     * @return a bitmask indicating the set of special object types marshalled
     * by the Parcelable.
     */
    @Override
    public int describeContents() {
        return 0;
    }

    /**
     * Flatten this object in to a Parcel.
     *
     * @param dest  The Parcel in which the object should be written.
     * @param flags Additional flags about how the object should be written.
     *              May be 0 or {@link #PARCELABLE_WRITE_RETURN_VALUE}.
     */
    @Override
    public void writeToParcel(Parcel dest, int flags) {
        Bundle bundle = new Bundle();

        bundle.putDouble("setTemp", setTemp);
        bundle.putDouble("outsideOffset", outsideOffset);
        bundle.putDouble("floorOffset", floorOffset);
        bundle.putDouble("curTemp", curTemp);
        bundle.putDouble("percOn", percOn);
        bundle.putDouble("overTemp", overTemp);
        bundle.putDouble("nextTemp", nextTemp);
        bundle.putInt("overrideType", overType.value());
        bundle.putInt("units", unit.value());
        bundle.putLong("nextTime", nextTime.getMillis());
        bundle.putLong("overTime", overTime.getMillis());

        dest.writeBundle(bundle);

    }

    public enum OverrideType {
        OVERRIDE_NONE(0),
        OVERRIDE_TEMPORARY(1),
        OVERRIDE_TIME(2),
        OVERRIDE_PERMANENT(3);

        private int i;

        OverrideType(int i) {
            this.i = i;
        }

        public static OverrideType get(int i) {
            for (OverrideType e : OverrideType.values()) {
                if (i == e.i)
                    return e;
            }
            return null;
        }

        public static int value(OverrideType overrideType) {
            return overrideType.i;
        }

        public int value() {
            return this.i;
        }


    }

    public enum Units {
        CELSIUS(1), FAHRENHEIT(2);
        private int i;

        Units(int i) {
            this.i = i;
        }

        public static Units get(int i) {
            for (Units e : Units.values()) {
                if (i == e.i)
                    return e;
            }
            return null;
        }

        public static int value(Units units) {
            return units.i;
        }

        public int value() {
            return this.i;
        }
    }

}
