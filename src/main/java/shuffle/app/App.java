package shuffle.app;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.ConsoleAppender;
import com.google.common.collect.ImmutableMap;
import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.FileBasedConfiguration;
import org.apache.commons.configuration2.PropertiesConfiguration;
import org.apache.commons.configuration2.builder.FileBasedConfigurationBuilder;
import org.apache.commons.configuration2.builder.fluent.Parameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import shuffle.adapters.driver.HttpDriver;
import shuffle.adapters.driver.HttpDriver.HttpAction;
import shuffle.adapters.monitor.LogMonitor;
import shuffle.adapters.store.InMemoryStore;
import shuffle.core.DeckHandlers;
import shuffle.core.LogHandler;
import shuffle.ports.ActionHandler;
import shuffle.ports.Driver;
import shuffle.ports.Monitor;

import java.io.File;
import java.util.Map;

import static shuffle.adapters.driver.HttpDriver.Method.*;
import static shuffle.adapters.driver.HttpDriver.action;

/*
 * @author Drew Fead
 */
public class App {
    private static Logger log = LoggerFactory.getLogger(App.class);

    private static final String configFilePath = "/opt/apps/shuffle/config.properties";

    public static final String PORT = "driver.http.port";
    public static final String PAGE_SIZE = "driver.store.pagesize";
    public static final String HUMAN_SHUFFLE = "core.shuffle.human";
    private static Driver driver;

    public static void start(Configuration config) {
        final Integer port;
        final Integer pageSize;
        final boolean humanShuffle;

        port = config.getInt(PORT);
        pageSize = config.getInt(PAGE_SIZE);
        humanShuffle = config.getBoolean(HUMAN_SHUFFLE);

        final DeckHandlers deck = new DeckHandlers(new InMemoryStore(), pageSize);

        final ActionHandler shuffle = humanShuffle ? deck.SHUFFLE : deck.RANDOMIZE;

        final Map<String, HttpAction> actions = ImmutableMap.<String,HttpAction>builder()
            .put("deck/create",     action( deck.CREATE,      PUT    ))
            .put("deck/shuffle",    action( shuffle,          POST   ))
            .put("deck/describe",   action( deck.DESCRIBE,    GET    ))
            .put("deck/list",       action( deck.LIST,        POST   )) // modified from requirement
                                                                        // to include pageSize & offset
            .put("deck/delete",     action( deck.DELETE,      DELETE ))
        .build();

        final Monitor monitor = new LogMonitor(); // use an implementation with cloudwatch or riemann

        driver = new HttpDriver(port, actions, monitor);
        try {
            driver.start();
        } catch (Exception e) {
            log.error("couldn't start app", e);
            throw new RuntimeException(e);
        }
    }

    public static void stop() {
        try {
            driver.stop();
        } catch (Exception e) {
            log.error("couldn't stop app", e);
            throw new RuntimeException(e);
        }
    }

//    public static void logging() {
//        final LoggerContext ctx = (LoggerContext) LoggerFactory.getILoggerFactory();
//        ctx.reset();
//
//        final PatternLayoutEncoder encoder = new PatternLayoutEncoder();
//        encoder.setContext(ctx);
//        encoder.setPattern("%-5level [%thread][%Xrequest_id]: %message%n");
//        encoder.start();
//
//        final ConsoleAppender<ILoggingEvent> appender = new ConsoleAppender<>();
//        appender.setContext(ctx);
//        appender.setEncoder(encoder);
//        appender.start();
//    }

    public static void main(String... args) {
        //logging();
        final Parameters params = new Parameters();
        final File props = new File(configFilePath);

        final FileBasedConfigurationBuilder<FileBasedConfiguration> builder =
                new FileBasedConfigurationBuilder<FileBasedConfiguration>(PropertiesConfiguration.class)
                        .configure(params.fileBased().setFile(props));

        final Configuration config;
        try {
             config = builder.getConfiguration();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                log.info("stopping app...");
                App.stop();
                log.info("app stopped.");
            }
        });
        log.info("starting app...");
        start(config);
        log.info("app started.");
    }
}
