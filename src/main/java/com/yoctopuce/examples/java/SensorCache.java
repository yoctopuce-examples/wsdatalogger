package com.yoctopuce.examples.java;

import com.yoctopuce.YoctoAPI.YAPI_Exception;
import com.yoctopuce.YoctoAPI.YDataSet;
import com.yoctopuce.YoctoAPI.YMeasure;
import com.yoctopuce.YoctoAPI.YSensor;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

public class SensorCache
{
    private final String _hwid;
    private final YSensor _ysensor;
    double _currentValue;
    double _max;
    double _min;
    String _unit;
    YMeasure _summary;
    ArrayList<YMeasure> _details;
    private final Calendar _cal = Calendar.getInstance();
    private Date _timestamp = null;
    private String _logicalName;


    public SensorCache(YSensor sensor) throws YAPI_Exception
    {
        _ysensor = sensor;
        _hwid = sensor.get_hardwareId();
        _unit = sensor.get_unit();
        _logicalName = sensor.getLogicalName();
    }


    public String getHwid()
    {
        return _hwid;
    }

    public double getCurrentValue()
    {
        return _currentValue;
    }

    public void setCurrentValue(double currentValue)
    {
        _currentValue = currentValue;
    }

    public double getMax()
    {
        return _max;
    }


    public double getMin()
    {
        return _min;
    }

    public String getUnit()
    {
        return _unit;
    }


    public YMeasure getSummary()
    {
        return _summary;
    }

    public ArrayList<YMeasure> getDetails()
    {
        return _details;
    }


    public void refresh() throws YAPI_Exception
    {
        Date now = new Date();
        if (_timestamp != null && (now.getTime() - _timestamp.getTime()) < 1000 * 60 * 5) {
            return;
        }
        _currentValue = _ysensor.get_currentValue();
        _max = _ysensor.get_highestValue();
        _min = _ysensor.get_lowestValue();
        _logicalName = _ysensor.get_logicalName();


        _cal.setTime(now);
        _cal.add(Calendar.HOUR_OF_DAY, 10);
        Date startTime = _cal.getTime();
        System.out.println("load from " + startTime.toString());
        //fixme
        YDataSet dataset = _ysensor.get_recordedData(0, 0);
        System.out.println("loading summary... ");
        dataset.loadMore();
        _summary = dataset.get_summary();
        int progress = 0;
        do {
            progress = dataset.loadMore();
        } while (progress < 100);
        _details = dataset.get_measures();
        _timestamp = now;
    }

    public String getLogicalName()
    {
        return _logicalName;
    }
}
