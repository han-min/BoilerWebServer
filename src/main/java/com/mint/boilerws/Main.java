package com.mint.boilerws;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.rolling.RollingFileAppender;
import org.apache.log4j.rolling.TimeBasedRollingPolicy;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.SecureRequestCustomizer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.util.resource.PathResource;

import com.mint.boilerws.config.Config;
import com.mint.boilerws.handler.OnOffHandler;
import com.mint.boilerws.handler.ScheduleHandler;
import com.mint.boilerws.handler.StatusHandler;
import com.mint.boilerws.handler.TemperatureHandler;
import com.mint.boilerws.scheduler.ScheduleManager;
import com.mint.boilerws.switcher.CommandSwitcher;
import com.mint.boilerws.switcher.GpioSwitcher;
import com.mint.boilerws.switcher.Switcher;
import com.mint.boilerws.temp.TemperatureManager;
import com.mint.boilerws.util.FileUtil;

public class Main {

    private static final Logger LOG = Logger.getLogger(Main.class);

    private static final String LOG_PATTERN = "%d [%p|%C{1}] %m%n";
    private static final String LOG_CONSOLE = "console";
    private static final String LOG_FILE = "file";
    // make native files executable on copy
    private static final Set<String> EXECUTABLE_FILENAME = new HashSet<>(Arrays.asList("codesend", "read"));
    
    private static final boolean OVERRIDE_EXISTING_RESOURCE = false;
    
    private static final int DEFAULT_HTTP_PORT = 80;
    public static final String DEFAULT_SCHEDULE_FILE = "src/main/resources/config/boilerwsSchedule.conf";
    public static final String DEFAULT_TEMPERATURE_FILE = "src/main/resources/config/boilerwsTemperature.conf";
    private final Config config;

    private Main(final String[] args) {
        String configFileName = "src/main/resources/config/config.properties";
        if (args.length > 0) {
            configFileName = args[1];
        }
        Path configFile = FileUtil.getFilePath(configFileName);
        Config config = null;
        boolean overrideExistingResource = OVERRIDE_EXISTING_RESOURCE;
        if (configFile.toFile().exists()) {
            config = new Config(configFile.toFile());
            initLogger(config);
            overrideExistingResource = config.get("resource.override.existing", overrideExistingResource);
        }
        initResources(overrideExistingResource);
        //
        if (config == null) {
            configFile = FileUtil.getFilePath(configFileName);
            config = new Config(configFile.toFile());
            initLogger(config);
        }
        this.config = config;
        LOG.info("---- Starting BoilerWebService ---- ");
    }

    private static void initLogger(final Config config) {
        final String appenderStr = config.get("log.appender", LOG_FILE);// LOG_CONSOLE);
        if (appenderStr.equals(LOG_CONSOLE)) {
            LOG.info("Logging to console");
        } else if (appenderStr.equals(LOG_FILE)) {
            final String fileName = config.get("log.file.name", "/log/boilerws.log");
            final String fileNameGz = config.get("log.file.name.gz", "/log/boilerws.%d{yyyy-MM-dd}.log.gz");
            final TimeBasedRollingPolicy p = new TimeBasedRollingPolicy();
            p.setFileNamePattern(fileNameGz);
            final RollingFileAppender r = new RollingFileAppender();
            r.setLayout(new PatternLayout(LOG_PATTERN));
            r.setFile(fileName);
            r.setRollingPolicy(p);
            r.activateOptions();
            //
            final File logFile = new File(fileName);
            if (logFile.getParentFile().exists()) {
                LOG.info("Setting log file to: " + logFile.getAbsolutePath());
                Logger.getRootLogger().removeAllAppenders();
                Logger.getRootLogger().addAppender(r);
            } else {
                LOG.error("Illegal file location: " + logFile.getAbsolutePath());
            }
        }
   }
    
    private void initResources(final boolean overrideExistingResource) {
        if (FileUtil.isRunningInJar()) {
            LOG.info("Running from JAR, initializing resources from JAR");
            try {
                FileUtil.copyFromJar(this, "/resources", Paths.get("./resources"), EXECUTABLE_FILENAME, overrideExistingResource);
            } catch (URISyntaxException | IOException e) {
                LOG.error("Error init JAR", e);
            }
        }
    }
    
    private void start() throws Exception {
        final int port = config.get("http.port", DEFAULT_HTTP_PORT);
        final boolean commandSwitcherConfigFound = CommandSwitcher.isConfigured(config);
        final Switcher switcher;
        if (commandSwitcherConfigFound) {
            LOG.info("Using command switcher");
            switcher = new CommandSwitcher(config);
        } else {
            LOG.info("Using GPIO switcher");
            switcher = new GpioSwitcher(config);
        }
        //
        final TemperatureManager temperatureManager = new TemperatureManager(config);
        final ScheduleManager scheduleManager = new ScheduleManager(config, temperatureManager, switcher);
        Server server = new Server();
        HandlerList handlers = new HandlerList();
        handlers.addHandler(new OnOffHandler("/centralheating.json", scheduleManager));
        handlers.addHandler(new StatusHandler("/status.json", scheduleManager));
        handlers.addHandler(new ScheduleHandler("/schedule.json", scheduleManager));
        handlers.addHandler(new TemperatureHandler("/temperature.json", config, temperatureManager));
        //
        final Path htmlPath = FileUtil.getFilePath("src/main/resources/html");
        LOG.info("Using html path: " + htmlPath.toFile().getAbsolutePath());
        final PathResource pathResource = new PathResource(htmlPath);
        final ResourceHandler resourceHandler = new ResourceHandler();
        resourceHandler.setDirectoriesListed(false);
        resourceHandler.setWelcomeFiles(new String[]{"index.html"});
        resourceHandler.setBaseResource(pathResource);
        handlers.addHandler(resourceHandler);
        //
        
        server.setHandler(handlers);

        HttpConfiguration https = new HttpConfiguration();
        https.addCustomizer(new SecureRequestCustomizer());

        ServerConnector connector = new ServerConnector(server);
        connector.setPort(port);

        server.setConnectors(new Connector[] {connector});
        LOG.info("Server starting with port: " + port);
        server.start();
    }

    public static void main(String[] args) throws Exception {
        final ConsoleAppender console = new ConsoleAppender();
        console.setLayout(new PatternLayout(LOG_PATTERN)); 
        console.activateOptions();
        Logger.getRootLogger().addAppender(console);
        Logger.getRootLogger().setLevel(Level.INFO);
        //
        (new Main(args)).start();
    }

}
