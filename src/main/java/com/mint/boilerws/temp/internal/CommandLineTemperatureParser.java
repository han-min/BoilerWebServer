package com.mint.boilerws.temp.internal;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import org.apache.log4j.Logger;

import com.mint.boilerws.config.Config;
import com.mint.boilerws.temp.TemperatureProvider;

public class CommandLineTemperatureParser implements TemperatureProvider {
    
    private static final Logger LOG = Logger.getLogger(CommandLineTemperatureParser.class);
    
    private static final long DATA_STALE_DEFAULT = 30 * 1000;

    private final String updateCommandLine;

    private final long dataStaleThreshold;
    
    private ReadingCache temperature = ReadingCache.INVALID;
    private ReadingCache humidity = ReadingCache.INVALID;
    
    public CommandLineTemperatureParser(final Config config) {
        this.updateCommandLine = config.get("command.temperature.all", null);
        if (this.updateCommandLine == null) {
            throw new RuntimeException("Command line for temperature and/or humidity not defined!");
        }
        this.dataStaleThreshold = config.get("command.temperature.stale.ms", DATA_STALE_DEFAULT);
    }

    public synchronized double getLatestTemperature() {
        final long now = System.currentTimeMillis();
        if (temperature == null || temperature.isStale(now, dataStaleThreshold)) {
            updateValue(now);
        }
        return temperature.getValue();
    }
    
    public synchronized double getLatestHumidity() {
        final long now = System.currentTimeMillis();
        if (humidity == null || humidity.isStale(now, dataStaleThreshold)){
            updateValue(now);
        }
        return humidity.getValue();
    }
    
    private synchronized void updateValue(final long now) {
        final double[] values = getValue(this.updateCommandLine);
        if (values != null) {
            final double tmp = values[0];
            final double hum = values[1];
            this.temperature = new ReadingCache(tmp, now);
            this.humidity = new ReadingCache(hum, now);
        }
    }
    
    private double[] getValue(final String command) {
        try {
            LOG.info("Running: " + command);
            String s;
            Process p = Runtime.getRuntime().exec(command);
            final BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
            final StringBuilder sb = new StringBuilder();
            while ((s = br.readLine()) != null) {
                sb.append(s).append("\n");
            }
            final String result = sb.toString();
            LOG.info("Got: " + result); // "22.0 66.0"
            final String[] v = result.split(" ");
            final double[] r = new double[v.length];
            for (int i=0; i<v.length; i++) {
                r[i] = Double.parseDouble(v[i]);
            }
            return r;
        } catch (Exception e) {
            LOG.error("Error running command: " + command, e);
        }
        return null;
    }

    private static class ReadingCache {
        
        public static ReadingCache INVALID = new ReadingCache(-99, 0);

        private final double value;
        private final long readTime;

        public ReadingCache(double value, long readTime) {
            super();
            this.value = value;
            this.readTime = readTime;
        }

        public double getValue() {
            return value;
        }

        public boolean isStale(final long now, final long stale) {
            return (now - readTime > stale);
        }
    }

    @Override
    public double getTemperature() {
        return getLatestTemperature();
    }
}
