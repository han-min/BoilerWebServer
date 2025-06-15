package com.mint.boilerws.handler;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.eclipse.jetty.server.Request;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mint.boilerws.scheduler.Schedule;
import com.mint.boilerws.scheduler.ScheduleItem;
import com.mint.boilerws.scheduler.ScheduleManager;
import com.mint.boilerws.scheduler.ScheduleSummary;
import com.mint.boilerws.util.TimeUtil;

public class ScheduleHandler extends AbstractHandler {

    private static final Logger LOG = Logger.getLogger(ScheduleHandler.class);
    private static final String SUBMIT_ACTION = "submit";
    private static final String ACTION_SUMMARY = "summary";
    

    private final ObjectMapper mapper = new ObjectMapper();
    private final ScheduleManager scheduleManager;
    
    public ScheduleHandler(final String url, final ScheduleManager scheduleManager) {
        super(url);
        this.scheduleManager = scheduleManager;
    }

    @Override
    public void handleRequest(
            final String url, 
            final Request request, 
            final HttpServletRequest httpRequest,
            final HttpServletResponse httpResponse, 
            final String ip) throws IOException {
        final String action = httpRequest.getParameter("action");
        final String form = httpRequest.getParameter("form");
        LOG.info(ip + " " + url + ". Got action: " + action);
        LOG.info(ip + " " + url + ". Got httpRequest: " + httpRequest);
        if (SUBMIT_ACTION.equals(action) && form !=null) {
            // change schedule
            LOG.info(ip + " Got Json:" + form);
            final Schedule schedule = mapper.readValue(form, Schedule.class);
            LOG.info(ip + " Resolved to:" + schedule);
            scheduleManager.setSchedule(schedule);
            request.setHandled(true);
        } else if (ACTION_SUMMARY.equals(action)) {
            LOG.info(ip + " Got action: " + action);
            final long now = System.currentTimeMillis();
            final long to = now + TimeUtil.ONE_DAY_MS;
            final List<ScheduleItem> s = scheduleManager.getScheduleItemSummary(now, to);
            final ScheduleSummary ss = new ScheduleSummary(s);
            final String json = mapper.writeValueAsString(ss);
            httpResponse.setContentType("text/json");
            httpResponse.setHeader("Cache-Control", "no-cache");
            httpResponse.setStatus(HttpServletResponse.SC_OK);
            LOG.info(ip + " Returning result: " + json);
            final PrintWriter p = httpResponse.getWriter();
            p.write(json);
            request.setHandled(true);
        } else {
            httpResponse.setContentType("text/json");
            httpResponse.setHeader("Cache-Control", "no-cache");
            httpResponse.setStatus(HttpServletResponse.SC_OK);
            final String json = mapper.writeValueAsString(scheduleManager.getSchedule());
            LOG.info(ip + " Returning result: " + json);
            final PrintWriter p = httpResponse.getWriter();
            p.write(json);
            request.setHandled(true);
        }
    }

}
