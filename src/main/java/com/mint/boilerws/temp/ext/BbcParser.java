package com.mint.boilerws.temp.ext;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.log4j.Logger;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.http.HttpMethod;
import org.eclipse.jetty.http.HttpVersion;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import com.mint.boilerws.config.Config;
import com.mint.boilerws.util.XmlUtil;

/*
 * https://www.bbc.co.uk/weather/about/17543675
 * Which RSS feeds are available from BBC Weather?
 * BBC Weather provides RSS feeds for 3-day forecasts as well as observations data for a number of global locations.
 * Example:
 * https://weather-broker-cdn.api.bbci.co.uk/en/observation/rss/2636503
 * 
 * If you want to find the RSS feed for a particular weather location, 
 * simply search for the location on the BBC Weather website and go to 
 * the appropriate forecast location page. Then copy and insert the page 
 * url's seven-digit number into either RSS example above (replacing the 
 * existing example seven-digit number for Manchester).
 */

public class BbcParser {

    private static final String BASE_HTML = "https://weather-broker-cdn.api.bbci.co.uk/en/observation/rss/";
    private static final int LOCATION_ID = 2636503; //2636503 = Sutton
    private static final long STALE_THRESHOLD = 15 * 60 * 1000;
    
    private static final Logger LOG = Logger.getLogger(BbcParser.class);
    
    private SslContextFactory sslContextFactory = new SslContextFactory(true);
    
    private final String rssHtml;
    private final long staleThreshold;
    
    private long lastUpdateTime = 0;
    private double latestValue = Double.NaN;

    private long timeout = 5;

    public BbcParser(final Config config) {
        super();
        final String baseHtml = config.get("bbc.parser.base.html", BASE_HTML);
        final int locationId = config.get("bbc.parser.location.id", LOCATION_ID);
        this.rssHtml = baseHtml + locationId;
        this.staleThreshold = config.get("bbc.parser.stale.ms", STALE_THRESHOLD);
        LOG.info("Using html: " + rssHtml);
    }

    public synchronized double  getLatestTemperature() {
        final long now = System.currentTimeMillis();
        if (Double.isNaN(latestValue) || (now - lastUpdateTime) > staleThreshold) {
            final double temp = getTemperature();
            this.lastUpdateTime = now;
            this.latestValue = temp;
        }
        return this.latestValue;
    }
    
    private HttpClient getHttpClient() {
        HttpClient client = new HttpClient(sslContextFactory);
        client.setFollowRedirects(false);
        return client;
    }
    
    /*
     <?xml version="1.0" encoding="UTF-8"?>
        <rss xmlns:atom="http://www.w3.org/2005/Atom" xmlns:dc="http://purl.org/dc/elements/1.1/" xmlns:georss="http://www.georss.org/georss" version="2.0">
          <channel>
            <title>BBC Weather - Observations for  Sutton, GB</title>
            <link>https://www.bbc.co.uk/weather/2636503</link>
            <description>Latest observations for Sutton from BBC Weather, including weather, temperature and wind information</description>
            <language>en</language>
            <copyright>Copyright: (C) British Broadcasting Corporation, see http://www.bbc.co.uk/terms/additional_rss.shtml for more details</copyright>
            <pubDate>Fri, 16 Oct 2020 22:00:00 GMT</pubDate>
            <dc:date>2020-10-16T22:00:00Z</dc:date>
            <dc:language>en</dc:language>
            <dc:rights>Copyright: (C) British Broadcasting Corporation, see http://www.bbc.co.uk/terms/additional_rss.shtml for more details</dc:rights>
            <atom:link href="https://weather-service-thunder-broker.api.bbci.co.uk/en/observation/rss/2636503" type="application/rss+xml" rel="self" />
            <item>
              <title>Friday - 23:00 BST: Not available, 9°C (48°F)</title>
              <link>https://www.bbc.co.uk/weather/2636503</link>
              <description>Temperature: 9°C (48°F), Wind Direction: North North Easterly, Wind Speed: 8mph, Humidity: 87%, Pressure: 1023mb, Falling, Visibility: Excellent</description>
              <pubDate>Fri, 16 Oct 2020 22:00:00 GMT</pubDate>
              <guid isPermaLink="false">https://www.bbc.co.uk/weather/2636503-2020-10-16T23:00:00.000+01:00</guid>
              <dc:date>2020-10-16T22:00:00Z</dc:date>
              <georss:point>51.35 -0.2</georss:point>
            </item>
          </channel>
        </rss>
     */
    private ContentResponse getResponse() throws Exception {
        final HttpClient client = getHttpClient();
        client.start();
        final ContentResponse res = client.newRequest(this.rssHtml)
                .version(HttpVersion.HTTP_1_1)
                .method(HttpMethod.GET)
                .timeout(this.timeout, TimeUnit.SECONDS)
                .send();
        client.stop();
        return res;
    }

    public void setTimeout(long timeOutSeconds){
        this.timeout = timeOutSeconds;
    }

    private double getTemperature() {
        try {
            final ContentResponse r = getResponse();
            if (r != null) {
                final double x = getTemprature(r.getContentAsString());
                return x;
            }
        } catch (Exception e) {
            e.printStackTrace();
            LOG.error("Error trying to get temperature", e);
        }
        return Double.NaN;
    }
    
    private double getTemprature(final String rssXml) throws ParserConfigurationException, SAXException, IOException {
        final Node rssNode = XmlUtil.getRoot(rssXml);
        // NodeList nodeList = rssNode.getFirstChild();//.getChildNodes(); //channel
        final Node channel = XmlUtil.getChildNode(rssNode, "channel");
        final Node item = XmlUtil.getChildNode(channel, "item");
        final Node description = XmlUtil.getChildNode(item, "description");
        // Temperature: 8Â°C (46Â°F), Wind Direction: Northerly, Wind Speed: 4mph,
        // Humidity: 87%, Pressure: 1024mb, Rising, Visibility: Excellent
        final String descriptionText = description.getTextContent();
        final String[] descriptionList = descriptionText.split(",");
        // Temperature: 8Â°C (46Â°F)
        final String temp = descriptionList[0];
        final String[] tempGroup = temp.split(" ");
        // 8Â°C
        final String tempCWithC = tempGroup[1];
        // remove the last 3 char for the oC symbol
        final String tempCStr = tempCWithC.substring(0, tempCWithC.length()-2);
        return Integer.parseInt(tempCStr);
    }
    
}

