package com.mint.boilerws.handler;

import java.io.IOException;
import java.util.StringTokenizer;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.eclipse.jetty.server.Request;

public abstract class AbstractHandler extends org.eclipse.jetty.server.handler.AbstractHandler {
    
    private final Logger LOG = Logger.getLogger(AbstractHandler.class);
    
    private final String url;

    public AbstractHandler(final String url) {
        this.url = url;
    }

    abstract public void handleRequest(final String url, final Request request, final HttpServletRequest httpRequest,
            final HttpServletResponse httpResponse, final String ip) throws IOException;
    
    @Override
    public void handle(final String url, final Request request, final HttpServletRequest httpRequest,
            final HttpServletResponse httpResponse) throws IOException, ServletException {
        try {
            final String ip = getClientIpAddress(httpRequest);
            if (url.equals(this.url)) {
                handleRequest(url, request, httpRequest, httpResponse, ip);
            }
        } catch (Exception e) {
            LOG.error("Exception in handling request at " + url + " with " + httpRequest, e);
        }
    }
    
    public static String getClientIpAddress(HttpServletRequest request) {
        String xForwardedForHeader = request.getHeader("X-Forwarded-For");
        if (xForwardedForHeader == null) {
            return request.getRemoteAddr();
        } else {
            // As of https://en.wikipedia.org/wiki/X-Forwarded-For
            // The general format of the field is: X-Forwarded-For: client, proxy1, proxy2 ...
            // we only want the client
            return new StringTokenizer(xForwardedForHeader, ",").nextToken().trim();
        }
    }

}
