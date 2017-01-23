package shuffle.ports;

/**
 * @author Drew Fead
 */
public interface Monitor {
    enum MetricType {
        REQUEST_DURATION
    }

    void logMetric(String requestId, MetricType type, long magnitude) throws Exception;
}
