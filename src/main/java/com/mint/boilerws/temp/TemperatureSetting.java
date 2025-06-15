package com.mint.boilerws.temp;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * For persisting temperature setting in file
 *
 */
public class TemperatureSetting {

    @JsonProperty
    private double temperature;

    public TemperatureSetting() {};// for JSON

    public TemperatureSetting(double temperature) {
        super();
        this.temperature = temperature;
    }

    public double getTemperature() {
        return temperature;
    }

    @Override
    public String toString() {
        return "TemperatureSetting [temperature=" + temperature + "]";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        long temp;
        temp = Double.doubleToLongBits(temperature);
        result = prime * result + (int) (temp ^ (temp >>> 32));
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
        TemperatureSetting other = (TemperatureSetting) obj;
        if (Double.doubleToLongBits(temperature) != Double.doubleToLongBits(other.temperature))
            return false;
        return true;
    }

}
