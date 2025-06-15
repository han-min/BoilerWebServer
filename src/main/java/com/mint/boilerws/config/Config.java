package com.mint.boilerws.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.apache.log4j.Logger;

public class Config {

    private final Logger LOG = Logger.getLogger(Config.class);
    
    private final Properties properties;
    
    public Config(final File configFile) {
        this.properties = new Properties();
        LOG.info("Using config file: " + configFile.getAbsolutePath() + ", exists: " + configFile.exists());
        try (final InputStream in = new FileInputStream(configFile);) {
            properties.load(in);
        } catch (IOException e) {
            LOG.error("Error loading properties file", e);
        }
    }
    
    public String get(final String key, final String defaultValue) {
        final String v = properties.getProperty(key);
        return (v != null) ? v : defaultValue;
    }
    
    public boolean get(final String key, final boolean defaultValue) {
        final String v = properties.getProperty(key);
        return (v != null) ? Boolean.parseBoolean(v) : defaultValue;
    }

    public int get(final String key, final int defaultValue) {
        final String v = properties.getProperty(key);
        return (v != null) ? Integer.parseInt(v) : defaultValue;
    }

    public long get(final String key, final long defaultValue) {
        final String v = properties.getProperty(key);
        return (v != null) ? Long.parseLong(v) : defaultValue;
    }

    public double get(final String key, final double defaultValue) {
        final String v = properties.getProperty(key);
        return (v != null) ? Double.parseDouble(v) : defaultValue;
    }


}
