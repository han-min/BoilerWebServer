package com.mint.boilerws.temp;

import java.io.File;
import java.nio.file.Path;

import org.apache.log4j.Logger;

import com.mint.boilerws.Main;
import com.mint.boilerws.config.Config;
import com.mint.boilerws.temp.internal.CommandLineTemperatureParser;
import com.mint.boilerws.util.FileUtil;

public class TemperatureManager {

    private static final int DEFAULT_TEMPERATURE_C = 21;

    private final Logger LOG = Logger.getLogger(TemperatureManager.class);

    private TemperatureSetting temperatureSetting = null;

    private final CommandLineTemperatureParser temperatureProvider;
    
    // margin in which temperature falling into this area is consider in target
    private final double temperatureMargin;

    private final Config config;
    
    private File configFile;

    public enum TemperatureState {
        ABOVE, IN_TARGET, BELOW, ERROR
    }
    
    public TemperatureManager(final Config config) {
        super();
        this.config = config;
        this.temperatureMargin = config.get("temperature.target.margin", 1.0);
        final String schFile = config.get("temperature.config.file", Main.DEFAULT_TEMPERATURE_FILE);
        final Path schPath = FileUtil.getFilePath(schFile);
        this.configFile = schPath.toFile();
        this.temperatureProvider = new CommandLineTemperatureParser(config);
        if (this.configFile.exists()) {
            LOG.info("Loading from schedule file.");
            setTemperature(FileUtil.temperatureSettingfromFile(configFile));
        } else {
            LOG.warn("Schedule file not found, creating empty new config file.");
            final double temperature = DEFAULT_TEMPERATURE_C;
            final TemperatureSetting defaultSetting = new TemperatureSetting(temperature);
            setTemperature(defaultSetting);
        }
    }
    
    public void changeTargetTemperature(final boolean isIncrease) {
        final double maxTemp = config.get("temperature.target.max", 26.0);
        final double minTemp = config.get("temperature.target.min", 16.0);
        final double delta = config.get("temperature.target.delta", 0.5);
        final double targetTemp = getTargetTemperature();
        if (isIncrease && targetTemp < maxTemp) {
            setTemperature(targetTemp + delta);
        } else if (!isIncrease && targetTemp > minTemp) {
            setTemperature(targetTemp - delta);
        }
    }
    
    public double getTargetTemperature() {
        return temperatureSetting.getTemperature();
    }
    
    public double getTemperature() {
        return this.temperatureProvider.getTemperature();
    }
    
    public double getHumidity() {
        return this.temperatureProvider.getLatestHumidity();
    }

    private void setTemperature(final double temperature) {
        TemperatureSetting setting = new TemperatureSetting(temperature);
        setTemperature(setting);
    }
    
    private void setTemperature(final TemperatureSetting temperatureSetting) {
        if (temperatureSetting != null && !temperatureSetting.equals(this.temperatureSetting)) {
            this.temperatureSetting = temperatureSetting;
            LOG.info("Using new temperature: " + this.configFile.getAbsolutePath());
            FileUtil.toFile(temperatureSetting, configFile);
        } else {
            LOG.info("Same schedule, do nothing");
        }
    }

    public TemperatureState getTemperatureState() {
        final double target = this.temperatureSetting.getTemperature();
        final double now = this.temperatureProvider.getTemperature();
        if (now < 0) {
            return TemperatureState.ERROR;
        } else if (now > target + this.temperatureMargin) {
            return TemperatureState.ABOVE;
        } else if (now < target - this.temperatureMargin) {
            return TemperatureState.BELOW;
        } else {
            return TemperatureState.IN_TARGET;
        }
    }

}
