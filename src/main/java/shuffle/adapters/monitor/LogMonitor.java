package shuffle.adapters.monitor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import shuffle.ports.Monitor;

/**
 * @author Drew Fead
 */
public class LogMonitor implements Monitor {
    private static final Logger log = LoggerFactory.getLogger(LogMonitor.class);

    public void logMetric(String requestId, MetricType type, long magnitude) {
        if(log.isInfoEnabled()) { log.info(String.format("%s: %s", type, magnitude)); }
    }
}
