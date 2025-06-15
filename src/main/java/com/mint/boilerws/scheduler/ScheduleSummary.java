package com.mint.boilerws.scheduler;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ScheduleSummary {

    @JsonProperty
    private List<ScheduleItem> schedule;

    public ScheduleSummary() {
    }
    
    public ScheduleSummary(List<ScheduleItem> schedule) {
        super();
        this.schedule = schedule;
    }

    public List<ScheduleItem> getSchedule() {
        return schedule;
    }

    public void setSchedule(List<ScheduleItem> schedule) {
        this.schedule = schedule;
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
        ScheduleSummary other = (ScheduleSummary) obj;
        if (schedule == null) {
            if (other.schedule != null)
                return false;
        } else if (!schedule.equals(other.schedule))
            return false;
        return true;
    };// for JSON

}
