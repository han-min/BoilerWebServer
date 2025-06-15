package com.mint.boilerws.scheduler;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.File;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import org.junit.Test;

import com.mint.boilerws.scheduler.ScheduleItem.DayType;
import com.mint.boilerws.util.FileUtil;

public class TestSchedule {

    final DateTimeFormatter fd = DateTimeFormatter.ofPattern("HH:mm");

    @Test
    public void testJson() {
        final DayType e = DayType.WEEKEND;
        final DayType d = DayType.WEEKDAY;
        final Map<DayType, SortedSet<ScheduleItem>> scheduleMap = new HashMap<>();
        SortedSet<ScheduleItem> s;
        s = new TreeSet<>();
        s.add(get(d, "12:00", true));
        s.add(get(d, "12:30", true));
        s.add(get(d, "13:00", false));
        s.add(get(d, "13:30", false));
        s.add(get(d, "14:00", false));
        s.add(get(d, "14:30", false));
        scheduleMap.put(DayType.WEEKDAY, s);
        s = new TreeSet<>();
        s.add(get(e, "12:00", false));
        s.add(get(e, "12:30", false));
        s.add(get(e, "13:00", true));
        s.add(get(e, "13:30", true));
        s.add(get(e, "14:00", false));
        s.add(get(e, "14:30", false));
        scheduleMap.put(DayType.WEEKEND, s);
        //
        try {
            final Schedule oriSchedule = new Schedule(scheduleMap);
            final File tempFile = File.createTempFile(this.getClass().getSimpleName(), ".tmp");
            FileUtil.toFile(oriSchedule, tempFile);
            final Schedule fromFileSchedule = FileUtil.fromFile(tempFile);
            assertEquals(oriSchedule, fromFileSchedule);

        } catch (Exception er) {
            er.printStackTrace();
            fail();
        }
    }

    public ScheduleItem get(DayType dayType, String hhMm, boolean isOn) {
        return new ScheduleItem(dayType, LocalTime.parse(hhMm, fd), isOn);
    }

}
