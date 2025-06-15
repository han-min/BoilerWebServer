package com.mint.boilerws.scheduler;

import java.util.Map;
import java.util.SortedSet;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.mint.boilerws.scheduler.ScheduleItem.DayType;

public class Schedule {

    @JsonProperty
    private Map<DayType, SortedSet<ScheduleItem>> schedule;
    
    public Schedule() {};//for JSON
    
    public Schedule(Map<DayType, SortedSet<ScheduleItem>> schedule) {
        super();
        this.schedule = schedule;
    }

    public SortedSet<ScheduleItem> getSchedule(final DayType dayType) {
        return schedule.get(dayType);
    }

    @Override
    public String toString() {
        return "Schedule [schedule=" + schedule + "]";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((schedule == null) ? 0 : schedule.hashCode());
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
        Schedule other = (Schedule) obj;
        if (schedule == null) {
            if (other.schedule != null)
                return false;
        } else if (!schedule.equals(other.schedule))
            return false;
        return true;
    }

}
