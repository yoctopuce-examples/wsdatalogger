package com.yoctopuce.examples.java;

import com.yoctopuce.YoctoAPI.*;

import javax.websocket.Session;
import java.util.ArrayList;
import java.util.HashMap;

public class Worker implements Runnable, YAPI.DeviceArrivalCallback, YAPI.DeviceRemovalCallback, YSensor.UpdateCallback
{

    private boolean _mustRun = true;
    private HashMap<String, SensorCache> _sensors_hwid = new HashMap<>();
    private ArrayList<String> _displays_hwid = new ArrayList<>();


    @Override
    public void run()
    {
        YAPI.RegisterDeviceArrivalCallback(this);
        YAPI.RegisterDeviceRemovalCallback(this);
        System.out.printf("worker thread started\n");
        while (_mustRun) {
            try {
                YAPI.UpdateDeviceList();
                for (String disp_hwid : _displays_hwid) {
                    YDisplay ydisplay = YDisplay.FindDisplay(disp_hwid);
                    try {
                        String sensor_targeted = ydisplay.get_logicalName();
                        SensorCache sensorCache = null;
                        for (SensorCache scache : _sensors_hwid.values()) {
                            if (sensor_targeted.equals(scache.getLogicalName())) {
                                sensorCache = scache;
                                break;
                            }
                        }
                        if (sensorCache != null) {
                            sensorCache.refresh();
                            repaintDisplay(sensorCache, ydisplay);
                        } else {
                            ydisplay.resetAll();
                            ydisplay.get_displayLayer(0);
                            // retreive the display size
                            int w = ydisplay.get_displayWidth();
                            int h = ydisplay.get_displayHeight();
                            // reteive the first layer
                            YDisplayLayer l0 = ydisplay.get_displayLayer(0);
                            // display a text in the middle of the screen
                            l0.drawText(w / 2, h / 2, YDisplayLayer.ALIGN.CENTER, "Sensor " + sensor_targeted + " is offline");
                        }
                    } catch (YAPI_Exception e) {
                        e.printStackTrace();
                    }
                }
            } catch (YAPI_Exception e) {
                e.printStackTrace();
            }
            try {
                YAPI.Sleep(1000);
            } catch (YAPI_Exception e) {
                e.printStackTrace();
            }


        }
    }


    private void repaintDisplay(SensorCache sensorCache, YDisplay display) throws YAPI_Exception
    {

        int width = display.get_displayWidth();
        int height = display.get_displayHeight();
        System.out.printf("Refresh display %s\n", display.get_hardwareId());
        // will use double buffering
        YDisplayLayer layer4 = display.get_displayLayer(4);
        layer4.reset();
        layer4.hide();

        // display humidity and pressure in the corners
        layer4.selectFont("Small.yfm");
        layer4.drawText(0, 0, YDisplayLayer.ALIGN.TOP_LEFT, String.format("max:%2.1f", sensorCache.getMax()));
        layer4.drawText(0, height - 1, YDisplayLayer.ALIGN.BOTTOM_LEFT, String.format("min:%2.1f", sensorCache.getMin()));

        // if the display is big enough, lets display _currentValue time
        if (height > 32) {
            layer4.drawText(width / 2, height - 1, YDisplayLayer.ALIGN.BOTTOM_CENTER, sensorCache.getLogicalName());
        }

        YMeasure summary = sensorCache.getSummary();
        double minValue = summary.get_minValue();
        double maxValue = summary.get_maxValue();
        double middle = summary.get_averageValue();
        System.out.printf("Avg Temp =%s\n", middle);
        // reframe the graph;
        if ((maxValue - middle) > height / 2) {
            System.out.printf("max peak detected\n");
            middle = maxValue - (height / 2);
        } else {
            if ((middle - minValue) > height / 2) {
                System.out.printf("min peak detected\n");
                middle = minValue + (height / 2);
            }
        }

        // draws the scale on the right side
        int middleScale = 5 * (int) Math.round(middle / 5);
        System.out.printf("max=%f min=%f middle=%d\n", maxValue, minValue, middleScale);
        for (int i = middleScale - 15; i <= middleScale + 15; i += 5) {
            layer4.drawText(width - 1, (int) Math.round((height / 2) - 2 * (i - middle)), YDisplayLayer.ALIGN.CENTER_RIGHT, Integer.toString(i));
            layer4.moveTo(width - 14, (int) Math.round((height / 2) - 2 * (i - middle)));
            layer4.lineTo(width - 13, (int) Math.round((height / 2) - 2 * (i - middle)));
        }

        ArrayList<YMeasure> details = sensorCache.getDetails();
        if (details.size() > 0) {
            int max_x = width - 15;
            layer4.moveTo(max_x, (int) Math.round((height / 2 - (details.get(0).get_averageValue() - middle) * 2)));
            int count = max_x;
            if (count > details.size()) {
                count = details.size();
            }
            for (int j = 1; j < count; j++) {
                layer4.lineTo(max_x - j, (int) Math.round((height / 2 - (details.get(j).get_averageValue() - middle) * 2)));
            }
        }
        // if no data available since more than 15 minutes then there is a problem
        String lastTemp = String.format("%2.1f%s", sensorCache.getCurrentValue(), sensorCache.getUnit());
        layer4.selectFont("Medium.yfm");
        // draws the temperature with a black border
        layer4.selectColorPen(0);
        layer4.drawText(width / 2 - 1, height / 2, YDisplayLayer.ALIGN.CENTER, lastTemp);
        layer4.drawText(width / 2 + 1, height / 2, YDisplayLayer.ALIGN.CENTER, lastTemp);
        layer4.drawText(width / 2, height / 2 + 1, YDisplayLayer.ALIGN.CENTER, lastTemp);
        layer4.drawText(width / 2, height / 2 - 1, YDisplayLayer.ALIGN.CENTER, lastTemp);
        layer4.selectColorPen(0xffff);
        layer4.drawText(width / 2, height / 2, YDisplayLayer.ALIGN.CENTER, lastTemp);
        // double buffering
        display.swapLayerContent(3, 4);
    }

    public void addSession(Session session)
    {
        try {
            YAPI.PreregisterHubWebSocketCallback(session);
        } catch (YAPI_Exception e) {
            e.printStackTrace();
        }
    }

    public void removeSession(Session session)
    {
        YAPI.UnregisterHubWebSocketCallback(session);
    }

    @Override
    public void yDeviceArrival(YModule module)
    {
        try {
            ArrayList<String> functionIds = module.get_functionIds("Sensor");
            String serialNumber = module.get_serialNumber();
            for (String funid : functionIds) {
                String hwid = serialNumber + "." + funid;
                if (!_sensors_hwid.keySet().contains(hwid)) {
                    YSensor sensor = YSensor.FindSensor(hwid);
                    sensor.set_logFrequency("12/h");
                    sensor.registerValueCallback(this);
                    SensorCache sensorCache = new SensorCache(sensor);
                    _sensors_hwid.put(hwid, sensorCache);
                }
            }
            if (functionIds.size() > 0) {
                YDataLogger dataLogger = YDataLogger.FindDataLogger(serialNumber + ".dataLogger");
                dataLogger.set_autoStart(YDataLogger.AUTOSTART_ON);
                dataLogger.set_recording(YDataLogger.RECORDING_ON);
                module.saveToFlash();
            }
            functionIds = module.get_functionIds("Display");
            for (String funid : functionIds) {
                String hwid = serialNumber + "." + funid;
                if (!_displays_hwid.contains(hwid)) {
                    YDisplay display = YDisplay.FindDisplay(hwid);
                    display.resetAll();
                    _displays_hwid.add(hwid);
                }
            }
        } catch (YAPI_Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void yDeviceRemoval(YModule module)
    {
        try {
            ArrayList<String> functionIds = module.get_functionIds("YSensor");
            for (String hwid : functionIds) {
                if (_sensors_hwid.keySet().contains(hwid)) {
                    _sensors_hwid.remove(hwid);
                }
            }
            functionIds = module.get_functionIds("YDisplay");
            for (String hwid : functionIds) {
                if (_displays_hwid.contains(hwid)) {
                    _displays_hwid.remove(hwid);
                }
            }
        } catch (YAPI_Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void yNewValue(YSensor function, String functionValue)
    {
        double value;
        try {
            value = Double.valueOf(functionValue);
        } catch (NumberFormatException ex) {
            return;
        }
        try {
            String hwid = function.get_hardwareId();
            if (_sensors_hwid.containsKey(hwid)) {
                _sensors_hwid.get(hwid).setCurrentValue(value);
            }
        } catch (YAPI_Exception e) {
            e.printStackTrace();
        }

    }
}
