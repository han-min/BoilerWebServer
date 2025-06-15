package com.mint.boilerws.handler;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.eclipse.jetty.server.Request;

import com.mint.boilerws.scheduler.ScheduleManager;
import com.mint.boilerws.util.JsonUtil;

public class OnOffHandler extends AbstractHandler {

    private final Logger LOG = Logger.getLogger(OnOffHandler.class);

    private final ScheduleManager scheduleManager;
    
    public OnOffHandler(final String url, final ScheduleManager scheduleManager) {
        super(url);
        this.scheduleManager = scheduleManager;
    }
    
    @Override
    public void handleRequest(final String url, final Request request, final HttpServletRequest httpRequest,
            final HttpServletResponse httpResponse, final String ip) throws IOException {
        final String action = httpRequest.getParameter("action");
        LOG.info(url + ". Got action: " + action);
        
        httpResponse.setContentType("text/json");
        httpResponse.setHeader("Cache-Control", "no-cache");
        httpResponse.setStatus(HttpServletResponse.SC_OK);
        final PrintWriter p = httpResponse.getWriter();
        if (action != null) {
            if (action.equals("toggle")) {
                scheduleManager.toggle();
            } else if (action.equals("1hour")) {
                scheduleManager.switchOn(true, 60 * 60 * 1000);
            } else if (action.equals("halfhour")) {
                scheduleManager.switchOn(true, 30 * 60 * 1000);
            } else if (action.equals("switch_on")) {
                scheduleManager.switchOn(true, -1);
            } else if (action.equals("switch_off")) {
                scheduleManager.switchOn(false, -1);
            } else if (action.equals("refresh")) {
            	scheduleManager.refresh();
            }
        }
        final Map<String, String> result = new HashMap<>();
        result.put("name", url);
        result.put("state", scheduleManager.getOnOffState().toString());
        final String json = JsonUtil.toJson(result);
        LOG.info(ip + " Returning result: " + json);
        p.print(json);
        request.setHandled(true);
    }

}
