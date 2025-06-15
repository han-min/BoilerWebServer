package com.mint.boilerws.scheduler;

import java.time.LocalTime;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

public class ScheduleItem implements Comparable<ScheduleItem> {

    public enum DayType {
        WEEKDAY, WEEKEND
    };
    
    @JsonProperty
    private DayType dayType;
    @JsonProperty
    private int hour;
    @JsonProperty
    private int minute;
    @JsonProperty
    private boolean isOn;
    
    public ScheduleItem() {}; //for JSON
    
    public ScheduleItem(DayType dayType, int hour, int minute, boolean isOn) {
        super();
        this.dayType = dayType;
        this.hour = hour;
        this.minute = minute;
        this.isOn = isOn;
    }

    public ScheduleItem(DayType dayType, LocalTime time, boolean isOn) {
        super();
        this.dayType = dayType;
        this.hour = time.getHour();
        this.minute = time.getMinute();
        this.isOn = isOn;
    }

    @JsonIgnore
    public LocalTime getTime() {
        return LocalTime.of(this.hour, this.minute);
    }
    
    public DayType getDayType() {
        return dayType;
    }

    public void setDayType(DayType dayType) {
        this.dayType = dayType;
    }

    public int getHour() {
        return hour;
    }

    public void setHour(int hour) {
        this.hour = hour;
    }

    public int getMinute() {
        return minute;
    }

    public void setMinute(int minute) {
        this.minute = minute;
    }

    public boolean isOn() {
        return isOn;
    }

    public void setOn(boolean isOn) {
        this.isOn = isOn;
    }

    @Override
    public String toString() {
        return "ScheduleItem [dayType=" + dayType + ", hour=" + hour + ", minute=" + minute + ", isOn=" + isOn + "]";
    }

    @Override
    public int compareTo(ScheduleItem o) {
        if (this.hour == o.hour) {
            if (this.minute == o.minute) {
                // don't really need this, but if do not have this, it will mess up equals
                return Boolean.compare(this.isOn, o.isOn);
            } else {
                return Integer.compare(this.minute, o.minute);
            }
        } else {
            return Integer.compare(this.hour, o.hour);
        }
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + hour;
        result = prime * result + (isOn ? 1231 : 1237);
        result = prime * result + minute;
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        ScheduleItem other = (ScheduleItem) obj;
        if (hour != other.hour)
            return false;
        if (isOn != other.isOn)
            return false;
        if (minute != other.minute)
            return false;
        return true;
    }

}
