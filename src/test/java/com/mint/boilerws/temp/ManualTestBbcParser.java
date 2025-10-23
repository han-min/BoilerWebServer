package com.mint.boilerws.temp;

import com.mint.boilerws.config.MockConfig;
import com.mint.boilerws.temp.ext.BbcParser;
import org.apache.log4j.*;

import static org.apache.log4j.ConsoleAppender.SYSTEM_OUT;

public class ManualTestBbcParser {

    static {
        ConsoleAppender ca = new ConsoleAppender(new PatternLayout(PatternLayout.TTCC_CONVERSION_PATTERN));
        Logger.getRootLogger().addAppender(ca);
        Logger.getRootLogger().setLevel(Level.DEBUG);
        System.getProperties().setProperty("org.eclipse.jetty.client", "TRACE");
    }

    private static final Logger log = Logger.getLogger(ManualTestBbcParser.class);

    private final MockConfig config = new MockConfig();

    public void start(){
        BbcParser p = new BbcParser(config);
        p.setTimeout(15);
        double temp = p.getLatestTemperature();
        log.info("v Temp: " + temp);
        System.out.println("x Temp:" + temp);
    }

    public static void main(String[] args){
        (new ManualTestBbcParser()).start();
    }

}
