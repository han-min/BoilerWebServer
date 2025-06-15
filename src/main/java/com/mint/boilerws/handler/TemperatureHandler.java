package com.mint.boilerws.handler;

import java.io.IOException;
import java.io.PrintWriter;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.eclipse.jetty.server.Request;

import com.mint.boilerws.config.Config;
import com.mint.boilerws.temp.TemperatureManager;
import com.mint.boilerws.temp.ext.BbcParser;
import com.mint.boilerws.util.JsonUtil;
import com.mint.boilerws.util.TimeUtil;

public class TemperatureHandler extends AbstractHandler {

    private static final String SET_ACTION = "set";

    private final DateTimeFormatter DTF = TimeUtil.getDateTimeFormatter();

    private final Logger LOG = Logger.getLogger(TemperatureHandler.class);
    
    private final BbcParser bbcParser;
    private final TemperatureManager temperatureManager;
    
    public TemperatureHandler(
            final String url, 
            final Config config, 
            final TemperatureManager temperatureManager) {
        super(url);
        this.bbcParser = new BbcParser(config);
        this.temperatureManager = temperatureManager;
    }

    @Override
    public void handleRequest(String url, Request request, HttpServletRequest httpRequest,
            HttpServletResponse httpResponse, String ip) throws IOException {
        final String action = httpRequest.getParameter("action");
        final String isIncreaseStr = httpRequest.getParameter("is_increase");
        final Boolean isIncrease = (isIncreaseStr != null) ? Boolean.parseBoolean(isIncreaseStr) : null;
        if (SET_ACTION.equals(action) && isIncrease != null) {
            LOG.info("Got action " + action + ", isIncrease: " + isIncrease);
            this.temperatureManager.changeTargetTemperature(isIncrease);
        } else {
            httpResponse.setContentType("text/json");
            httpResponse.setHeader("Cache-Control", "no-cache");
            httpResponse.setStatus(HttpServletResponse.SC_OK);
            final long now = System.currentTimeMillis();
            final double externalTemperature = bbcParser.getLatestTemperature();
            final double internalTemperature = temperatureManager.getTemperature();
            final double internalHumidity = temperatureManager.getHumidity();
            final Map<String, String> result = new HashMap<>();
            result.put("name", url);
            result.put("time", DTF.format(Instant.ofEpochMilli(now)));
            result.put("external_temperature", Double.toString(externalTemperature));
            result.put("internal_temperature", Double.toString(internalTemperature));
            result.put("target_temperature", Double.toString(temperatureManager.getTargetTemperature()));
            result.put("internal_humidity", Double.toString(internalHumidity));
            final PrintWriter p = httpResponse.getWriter();
            final String json = JsonUtil.toJson(result);
            LOG.info(ip + " Returning resultx: " + json);
            p.print(json);
        }
        request.setHandled(true);
    }

}
