package com.dansull.eyrie;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

import org.joda.time.DateTime;

import java.io.IOException;

/**
 * Created by Daniel on 2/10/2015.
 */
public class ThermostatDataAdapter extends TypeAdapter<ThermostatData> {
    @Override
    public void write(JsonWriter writer, ThermostatData thermostatData) throws IOException {
        if (thermostatData == null) {
            writer.nullValue();
            return;
        }
        writer.beginObject();
        writer.name("setTemp").value(thermostatData.getSetTemp());
        writer.name("outsideOffset").value(thermostatData.getOutsideOffset());
        writer.name("floorOffset").value(thermostatData.getFloorOffset());
        writer.name("curTemp").value(thermostatData.getCurTemp());
        writer.name("percOn").value(thermostatData.getPercOn());
        writer.name("overTemp").value(thermostatData.getOverTemp());
        writer.name("nextTemp").value(thermostatData.getNextTemp());
        writer.name("overrideType").value(thermostatData.getOverType().value());
        writer.name("units").value(thermostatData.getUnit().value());
        writer.name("nextTime").value(thermostatData.getNextTime().getMillis());
        writer.name("overTime").value(thermostatData.getOverTime().getMillis());
        writer.endObject();
    }

    @Override
    public ThermostatData read(final JsonReader in) throws IOException {
        if (in.peek() == JsonToken.NULL) {
            in.nextNull();
            return null;
        }
        ThermostatData data = new ThermostatData();
        in.beginObject();
        while (in.hasNext()) {
            switch (in.nextName()) {
                case "setTemp":
                    data.setSetTemp(in.nextDouble());
                    break;
                case "outsideOffset":
                    data.setOutsideOffset(in.nextDouble());
                    break;
                case "floorOffset":
                    data.setFloorOffset(in.nextDouble());
                    break;
                case "curTemp":
                    data.setCurTemp(in.nextDouble());
                    break;
                case "percOn":
                    data.setPercOn(in.nextDouble());
                    break;
                case "overTemp":
                    data.setOverTemp(in.nextDouble());
                    break;
                case "nextTemp":
                    data.setNextTemp(in.nextDouble());
                    break;
                case "overrideType":
                    data.setOverType(in.nextInt());
                    break;
                case "units":
                    data.setUnit(in.nextInt());
                    break;
                case "nextTime":
                    data.setNextTime(new DateTime(in.nextLong()));
                    break;
                case "overTime":
                    data.setOverTime(new DateTime(in.nextLong()));
                    break;
            }
        }
        in.endObject();
        return data;
    }


}
