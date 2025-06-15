package com.mint.boilerws.scheduler;

import java.io.File;
import java.nio.file.Path;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import com.mint.boilerws.Main;
import com.mint.boilerws.config.Config;
import com.mint.boilerws.scheduler.ScheduleItem.DayType;
import com.mint.boilerws.switcher.Switcher;
import com.mint.boilerws.switcher.Switcher.SwitchOnOffState;
import com.mint.boilerws.temp.TemperatureManager;
import com.mint.boilerws.temp.TemperatureManager.TemperatureState;
import com.mint.boilerws.util.FileUtil;
import com.mint.boilerws.util.TimeUtil;


public class ScheduleManager {
    private final Logger LOG = Logger.getLogger(ScheduleManager.class);
    private final long ONE_DAY_MS = 24 * 60 * 60 * 1000;
    private final long DEFAULT_NEAR_MS = 1000; //if it's near, trigger it
    
    private final File configFile;
    private final TemperatureManager temperatureManager;
    private final Switcher switcher;
    private final SingleThreadScheduler singleThreadExec = SingleThreadScheduler.getInstance();
    private final Config config;
    
    private enum OverrideType {
        NONE,
        ON, 
        OFF, 
        UNTIL_ON,   // override to 'on', until we hit an 'on' schedule  
        UNTIL_OFF
    }
    private OverrideType override = OverrideType.NONE;
    private long overrideUntil = -1; //epoch, -1 = permanently.
    
    private final long timeNearThreshold;
    
    private Schedule schedule = null;
    private ScheduledFuture<?> scheduledWake = null;
    private ScheduledFuture<?> temperatureSchedule = null;
    
    public ScheduleManager(
            final Config config,
            final TemperatureManager temperatureManager,
            final Switcher switcher) {
        super();
        this.config = config;
        final String schFile = config.get("schedule.config.file", Main.DEFAULT_SCHEDULE_FILE);
        final Path schPath = FileUtil.getFilePath(schFile);
        this.configFile = schPath.toFile();
        this.temperatureManager = temperatureManager;
        this.switcher = switcher;
        this.timeNearThreshold = config.get("scheduler.near.threshold.ms", DEFAULT_NEAR_MS);
        LOG.info("Using schedule file: " + configFile.getAbsolutePath());
        if (this.configFile.exists()) {
            LOG.info("Loading from schedule file.");
            setSchedule(FileUtil.fromFile(configFile));
        } else {
            LOG.warn("Schedule file not found, creating empty new schedule.");
            setSchedule(getEmptySchedule());
        }
        // make sure switch state is consistent
        this.switcher.repeatSwitch();
    }
    
    public void setSchedule(final Schedule schedule) {
        if (schedule != null && !schedule.equals(this.schedule)) {
            this.schedule = schedule;
            LOG.info("Using new schedule: " + this.configFile.getAbsolutePath());
            FileUtil.toFile(schedule, configFile);
            //
            processSchedule();
        } else {
            LOG.info("Same schedule, do nothing");
        }
    }    
    
    public Schedule getSchedule() {
        return schedule;
    }
    
    public SwitchOnOffState getOnOffState() {
        return this.switcher.getOnOffState();
    }
    
    public Optional<String> getDetailMessage(){
        if (this.override != OverrideType.NONE) {
            final StringBuilder sb = new StringBuilder();
            sb.append("Override to ");
            if (isNowOrGoingOn()) {
                sb.append("on");
            } else {
                sb.append("off");
            }
            final Optional<LocalTime> lt = getOverrideUntil();
            if (lt.isPresent()) {
                sb.append(" until ").append(lt.get()).append(".");
            } else {
                sb.append(".");
            }
            return Optional.of(sb.toString());
        }
        return Optional.empty();
    }
    
    public boolean isNowOrGoingOn() {
        final SwitchOnOffState onOffNow = getOnOffState();
        return (onOffNow == SwitchOnOffState.ON || onOffNow == SwitchOnOffState.PENDING_ON);
    }
    
    public void toggle() {
        final boolean toTurnOn = !isNowOrGoingOn(); //toggle
        final ScheduleItem nsi = getCurrentItem(System.currentTimeMillis());
        if (nsi.isOn() == toTurnOn) {
            // same as what the schedule now is
            this.override = OverrideType.NONE;
        } else {
            this.override = (toTurnOn ? OverrideType.UNTIL_ON : OverrideType.UNTIL_OFF);
        }
        this.overrideUntil = -1; //not applicable
        LOG.info("Toggle: " + toTurnOn + ", override to " + this.override);
        final SwitchOnOffState result = switchOn(toTurnOn);
        LOG.info("Switch result: " + result + ", for switching " + (toTurnOn ? "on" : "off"));
    }
    
    public void switchOn(final boolean toTurnOn, final long durationMs) {
        this.override = (toTurnOn ? OverrideType.ON : OverrideType.OFF);
        if (durationMs > 0) {
            this.overrideUntil = System.currentTimeMillis() + durationMs;
        } else {
            this.overrideUntil = -1;
        }
        LOG.info("Override " + this.override
                + (this.overrideUntil > 0 ? " until " + TimeUtil.getLocalTime(this.overrideUntil) : " permenantly") 
                + ".");
        final boolean nowIsOn = isNowOrGoingOn();
        if (toTurnOn != nowIsOn) {
            final SwitchOnOffState result = switchOn(toTurnOn);
            LOG.info("Switch result: " + result + ", for switching " + (toTurnOn ? "on" : "off"));
        }
    }
    
    private SwitchOnOffState switchOn(final boolean toTurnOn) {
        return switcher.switchOn(toTurnOn);
    }
    
    public void refresh() {
    	switcher.repeatSwitch();
    }
    
    private void checkAndScheduleTemperature(final boolean toTurnOn) {
        checkTemperature();
        //
        if (this.temperatureSchedule != null &&
                this.temperatureSchedule.isDone()) {
            this.temperatureSchedule.cancel(false);
        }
        if (toTurnOn) {
            final long delay = this.config.get("schedule.temperature.check.period", 5 * 60 * 1000L);
            this.temperatureSchedule = this.singleThreadExec.schedule(()->{
                checkAndScheduleTemperature(toTurnOn);
            }, delay, TimeUnit.MILLISECONDS);
            LOG.info("Checking temperature in: " + delay);
        } else {
            LOG.info("Not checking temperature anymore");
        }
    }
    
    private void checkTemperature() {
        final TemperatureState state = this.temperatureManager.getTemperatureState();
        switch (state) {
        case ABOVE:
            
            break;
        case BELOW:
            
            break;
        case IN_TARGET:
        case ERROR:
        default:
            //no op?
        }
    }
    
    private void processSchedule() {
        final long now = System.currentTimeMillis(); 
        final ScheduleItem nextItem = getNextItem(now);
        final LocalTime nowTime = TimeUtil.getLocalTime(now);
        final LocalTime nextTriggerTime = nextItem.getTime();
        // find what's the next time to action next, and schedule it
        final long untilNext =  nowTime.until(nextTriggerTime, ChronoUnit.MILLIS);
        if (untilNext < this.timeNearThreshold) {
            activate(nextItem);
        } else {
            final ScheduleItem currentItem = getCurrentItem(now);
            activate(currentItem);
        }
    }
    
    private String getUntilString(final long futureTime) {
        if (futureTime < 0) {
            return "forever";
        } else {
            return TimeUtil.getLocalTime(futureTime).toString();
        }
    }
    
    private void activate(final ScheduleItem item) {
        final SwitchOnOffState onOffNow = getOnOffState();
        final long now = System.currentTimeMillis();
        final boolean isToSwitchOn;
        if ((this.override == OverrideType.ON || this.override == OverrideType.OFF)) {
            if (this.overrideUntil < 0 //forever 
                    || this.overrideUntil > now) { //in the future
                // simple override on/off until specific time or forever
                LOG.info("Override to " + this.override + " until " 
                        + getUntilString(this.overrideUntil));
                isToSwitchOn = (this.override == OverrideType.ON);
            } else {
                LOG.info("Reset override as time has passed: " + TimeUtil.getLocalTime(this.overrideUntil));
                this.override = OverrideType.NONE;
                this.overrideUntil = now;
                isToSwitchOn = item.isOn();
            }
        } else if (this.override == OverrideType.UNTIL_ON || this.override == OverrideType.UNTIL_OFF){
            final boolean overrideNowOn = (this.override == OverrideType.UNTIL_ON);
            if (overrideNowOn == item.isOn()) {
                LOG.info("Reset override as state already reached: " + this.override);
                this.override = OverrideType.NONE;
                this.overrideUntil = now;
                isToSwitchOn = item.isOn();
            } else {
                LOG.info("The 'override until' is still valid: " + this.override);
                isToSwitchOn = overrideNowOn;
            }
        } else {
            isToSwitchOn = item.isOn(); // simple case no override
        }
        final String onOffString = (isToSwitchOn ? "on" : "off");
        final boolean isNowOrGoingOn = isNowOrGoingOn();
        if ((isToSwitchOn && isNowOrGoingOn) || (!isToSwitchOn && !isNowOrGoingOn)) {
            LOG.debug("Is already in same state " + onOffNow + ". Do nothing. Wanted to switch " + onOffString + ".");
        } else {
            LOG.info("Switching " + onOffString + ".");
            final SwitchOnOffState result = switchOn(isToSwitchOn);
            LOG.info("Switch result: " + result + ", for switching " + onOffString + ".");
        }
        // schedule next
        final LocalTime thisItemTime = item.getTime();
        final SortedSet<ScheduleItem> items = getScheduleItems(now);
        final ScheduleItem nextItem = getNextItem(thisItemTime, items);
        final LocalTime nowLt = TimeUtil.getLocalTime(System.currentTimeMillis());
        final long delay = nowLt.until(nextItem.getTime(), ChronoUnit.MILLIS);
        if (scheduledWake != null && !scheduledWake.isDone()) {
            // this can be the schedule/thread that's running this, 'false' not to interrupt
            scheduledWake.cancel(false);
        }
        final LocalTime checkTime = nowLt.plus(delay, ChronoUnit.MILLIS);
        LOG.info("Scheduled to check again at " + checkTime);
        scheduledWake = this.singleThreadExec.schedule(()->{
            processSchedule();
        }, delay, TimeUnit.MILLISECONDS);
        checkAndScheduleTemperature(isToSwitchOn);
    }
    
    private SortedSet<ScheduleItem> getScheduleItems(final long now) {
        final boolean isWeekend = TimeUtil.isWeekend(now);
        final SortedSet<ScheduleItem> items = schedule.getSchedule((isWeekend) ? DayType.WEEKEND : DayType.WEEKDAY);
        return items;
    }
    
    public List<ScheduleItem> getScheduleItemSummary(final long from, final long to){
        final List<ScheduleItem> items = getScheduleItem(from, to);
        final List<ScheduleItem> summary = new LinkedList<>();
        ScheduleItem prev = null;
        for (ScheduleItem i : items) {
            if (prev == null) {
                summary.add(i);
            } else if (i.isOn() != prev.isOn()) {
                summary.add(i);
            }
            //
            prev = i;
        }
        return summary;
    }
    
    private List<ScheduleItem> getScheduleItem(final long from, final long to){
        if ((to - from) > TimeUtil.ONE_DAY_MS) {
            LOG.error("Will not work for a span more than a day");
            return Collections.emptyList();
        }
        final List<ScheduleItem> r = new LinkedList<>();
        final long tomorrowOfFrom = TimeUtil.getTomorrowStartOfDay(from);
        final boolean isToTomorrow = to >= tomorrowOfFrom;
        final SortedSet<ScheduleItem> fromItems = getScheduleItems(System.currentTimeMillis());
        final LocalTime fromLt = TimeUtil.getLocalTime(from);
        final LocalTime toLt = TimeUtil.getLocalTime(to);
        for (final ScheduleItem i : fromItems) {
            if (i.getTime().isAfter(fromLt) || i.getTime().equals(fromLt)) {
                r.add(i);
            } else if (!isToTomorrow && i.getTime().isAfter(toLt)) {
                break;
            }
        }
        if (isToTomorrow) {
            final SortedSet<ScheduleItem> toItems = getScheduleItems(System.currentTimeMillis());
            for (final ScheduleItem i : toItems) {
                if (i.getTime().isBefore(toLt) || i.getTime().equals(toLt)) {
                    r.add(i);
                } else if (i.getTime().isAfter(toLt)) {
                    break;
                }
            }
        }
        return r;
    }
    
    public Optional<LocalTime> getOverrideUntil(){
        if (this.override == OverrideType.ON || this.override == OverrideType.OFF) {
            if (this.overrideUntil > 0) {
                final LocalTime lt = TimeUtil.getLocalTime(this.overrideUntil);
                // round it to minutes, the display time will not have seconds
                LocalTime roundedLocalTime = LocalTime.of(lt.getHour(), lt.getMinute());
                return Optional.of(roundedLocalTime);
            }
        } else if (this.override == OverrideType.UNTIL_ON || this.override == OverrideType.UNTIL_OFF) {
            final long now = System.currentTimeMillis();
            final boolean isToSwitchOn = (this.override == OverrideType.UNTIL_ON);
            final Optional<ScheduleItem> item = getNextItemWithSwitchOn(now, isToSwitchOn);
            if (item.isPresent()) {
                return Optional.of(item.get().getTime());
            }
        }
        return Optional.empty();
    }
    
    /*
     * Give indication of when the override will stop. Do not use this for schedule
     * as it may not work if there's no matching on/off. Use for text/status
     * reporting only.
     */
    private Optional<ScheduleItem> getNextItemWithSwitchOn(final long now, final boolean isToSwitchOn) {
        final ScheduleItem todayItem = getNextItemWithSwitchOnToday(now, isToSwitchOn);
        if (todayItem != null) {
            return Optional.of(todayItem); // found today's, return
        }
        // unable to find anything today, look for tomorrow
        long scanTime = now + ONE_DAY_MS;
        SortedSet<ScheduleItem> items;
        final long maxScanTime = now + (6 * ONE_DAY_MS); // 6 days
        boolean foundFirstItem = false; // first item with the same state as 'isToSwitchOn'
        while (scanTime < maxScanTime) {
            items = getScheduleItems(scanTime);
            for (final ScheduleItem i : items) {
                if (foundFirstItem) {
                    // found 1st item that's already the same
                    // found the last item that's the same
                    // return this as the prev will send at this start
                    if (i.isOn() != isToSwitchOn) {
                        return Optional.of(i);
                    }
                } else if (i.isOn() == isToSwitchOn) {
                    foundFirstItem = true;
                }
            } // found nothing for this day, try tomorrow
            scanTime = scanTime + ONE_DAY_MS;
        }
        return Optional.empty();
    }
    
    private ScheduleItem getNextItemWithSwitchOnToday(final long now, final boolean isToSwitchOn) {
        final LocalTime nowLt = TimeUtil.getLocalTime(now);
        final SortedSet<ScheduleItem> items = getScheduleItems(System.currentTimeMillis());
        for (final ScheduleItem i : items) {
            if (i.getTime().isAfter(nowLt) && i.isOn() == isToSwitchOn) {
                return i;
            }
        }
        return null;
    }
    
    private ScheduleItem getCurrentItem(final long now) {
        final SortedSet<ScheduleItem> items = getScheduleItems(System.currentTimeMillis());
        final LocalTime nowLt = TimeUtil.getLocalTime(now);
        ScheduleItem prev = items.last();
        for (final ScheduleItem i : items) {
            if (i.getTime().isAfter(nowLt)) {
                return prev;
            }
            prev = i;
        }
        return prev;
    }
    
    private ScheduleItem getNextItem(final long now) {
        final SortedSet<ScheduleItem> items = getScheduleItems(System.currentTimeMillis());
        final LocalTime nowLt = TimeUtil.getLocalTime(now);
        return getNextItem(nowLt, items);
    }
    
    private ScheduleItem getNextItem(final LocalTime now, final SortedSet<ScheduleItem> items) {
        for (ScheduleItem i : items) {
            if (i.getTime().isAfter(now)) {
                return i;
            }
        }
        // almost at midnight, return 1st item
        return items.first();
    }
    
    private static Schedule getEmptySchedule() {
        final Map<DayType, SortedSet<ScheduleItem>> map = new HashMap<>(); 
        for (int h=0; h<24; h++) {
            add(h, map);
        }
        return new Schedule(map);
    }

    private static void add(final int hour, Map<DayType, SortedSet<ScheduleItem>> map) {
        add(DayType.WEEKDAY, hour, map);
        add(DayType.WEEKEND, hour, map);
    }
    
    private static void add(DayType dayType, int hour, Map<DayType, SortedSet<ScheduleItem>> map) {
        add(dayType, hour, 0, initValue(), map);
        add(dayType, hour, 30, initValue(), map);
    }
    
    private static boolean initValue() {
        return (Math.random() < 0.5); //hack
    }
    
    private static void add(DayType dayType, int hour, int minute, boolean isOn, Map<DayType, SortedSet<ScheduleItem>> map) {
        final ScheduleItem i = new ScheduleItem(dayType, hour, minute, isOn);
        final SortedSet<ScheduleItem> si = map.computeIfAbsent(dayType, (k) -> {
            return new TreeSet<>();
        });
        si.add(i);
    }
}
