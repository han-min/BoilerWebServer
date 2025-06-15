package com.mint.boilerws.handler;

import java.io.IOException;
import java.io.PrintWriter;
import java.time.Instant;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.eclipse.jetty.server.Request;

import com.mint.boilerws.scheduler.ScheduleManager;
import com.mint.boilerws.switcher.Switcher.SwitchOnOffState;
import com.mint.boilerws.util.JsonUtil;
import com.mint.boilerws.util.TimeUtil;

public class StatusHandler extends AbstractHandler {

    private final Logger LOG = Logger.getLogger(StatusHandler.class);

    private final DateTimeFormatter DTF = TimeUtil.getDateTimeFormatter();

    private final ScheduleManager scheduleManager;
    
    public StatusHandler(String url, final ScheduleManager scheduleManager) {
        super(url);
        this.scheduleManager = scheduleManager;
    }

    @Override
    public void handleRequest(String url, Request request, HttpServletRequest httpRequest,
            HttpServletResponse httpResponse, String ip) throws IOException {
        httpResponse.setContentType("text/json");
        httpResponse.setHeader("Cache-Control", "no-cache");
        httpResponse.setStatus(HttpServletResponse.SC_OK);
        final SwitchOnOffState chOnOffState = scheduleManager.getOnOffState();
        final Optional<String> detailMsg = scheduleManager.getDetailMessage();
        final Optional<LocalTime> ovrRideOpt = scheduleManager.getOverrideUntil();
        final String ovrRideTime = ovrRideOpt.isPresent() ? ovrRideOpt.get().toString() : "";
        //
        String statusMessageOut = scheduleManager.isNowOrGoingOn() ? "On" : "Off";
        if (chOnOffState == SwitchOnOffState.PENDING_ON 
                || chOnOffState == SwitchOnOffState.PENDING_OFF) {
            statusMessageOut = "Pending switch "
                    + (chOnOffState == SwitchOnOffState.PENDING_ON ? "on" : "off")
                    + ".";
        }
        //
        final Map<String, String> result = new HashMap<>();
        result.put("name", url);
        result.put("time", DTF.format(Instant.now()));
        result.put("status", "OK"); //always ok for now
        result.put("message", statusMessageOut);
        if (detailMsg.isPresent()) {
            result.put("details", detailMsg.get());
        } else {
            result.put("details", "");
        }
        result.put("centralheating_state", chOnOffState.toString());
        result.put("centralheating_overridetime", ovrRideTime);
        //
        final PrintWriter p = httpResponse.getWriter();
        final String json = JsonUtil.toJson(result);
        LOG.info(ip + " Returning result: " + json); //too chatty
        p.print(json);
        request.setHandled(true);
    }

}
