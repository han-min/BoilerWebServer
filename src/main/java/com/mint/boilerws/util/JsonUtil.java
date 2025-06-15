package com.mint.boilerws.util;

import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedSet;

import com.mint.boilerws.scheduler.Schedule;
import com.mint.boilerws.scheduler.ScheduleItem;
import com.mint.boilerws.scheduler.ScheduleItem.DayType;

public class JsonUtil {

    private static final String QUOTE = "\"";
    private static final String COLON = ":";
    
    public static String toJson(final Schedule schedule) {
        final SortedSet<ScheduleItem> wd = schedule.getSchedule(DayType.WEEKDAY);
        final SortedSet<ScheduleItem> we = schedule.getSchedule(DayType.WEEKEND);
        final StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append(toJson("weekday", toJson(wd), false));
        sb.append(",");
        sb.append(toJson("weekend", toJson(we), false));
        sb.append("}");
        return sb.toString();
    }
    
    public static String toJson(final SortedSet<ScheduleItem> items) {
        final StringBuilder sb = new StringBuilder();
        for (ScheduleItem i : items) {
            if (sb.length() == 0) {
                sb.append("["); //start
            } else {
                sb.append(",");
            }
            sb.append(toJson(i));
        }
        sb.append("]");
        return sb.toString();
    }

    private static String toJson(ScheduleItem item) {
        final StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append(toJson("dayType", item.getDayType().toString(), true));
        sb.append(",");
        sb.append(toJson("hour", item.getHour(), true));
        sb.append(",");
        sb.append(toJson("minute", item.getMinute(), true));
        sb.append(",");
        sb.append(toJson("isOn", item.isOn(), true));
        sb.append("}");
        return sb.toString();
    }

    
    public static String toJson(final Map<String, String> data) {
        final StringBuilder sb = new StringBuilder();
        for (final Entry<String, String> e : data.entrySet()) {
            if (sb.length() == 0) {
                sb.append("{"); //start
            } else {
                sb.append(",");
            }
            sb.append(toJson(e.getKey(), e.getValue(), true));
        }
        sb.append("}");
        return sb.toString();
    }
    
    private static String toJson(final String key, final boolean value, final boolean wrapValue) {
        return toJson(key, Boolean.toString(value), wrapValue);
    }
    
    private static String toJson(final String key, final int value, final boolean wrapValue) {
        return toJson(key, Integer.toString(value), wrapValue);
    }
    
    private static String toJson(final String key, final String value, final boolean wrapValue) {
        final StringBuilder sb = new StringBuilder();
        wrap(key, sb);
        sb.append(COLON);
        if (wrapValue) {
            wrap(value, sb);
        } else {
            sb.append(value);
        }
        return sb.toString();
    }
    
    private static StringBuilder wrap(final String value, final StringBuilder sb) {
        sb.append(QUOTE).append(value).append(QUOTE);
        return sb;
    }

}
