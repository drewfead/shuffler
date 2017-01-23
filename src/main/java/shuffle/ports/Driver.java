package shuffle.ports;

/**
 * @author Drew Fead
 */
public interface Driver {
    void start() throws Exception;
    void stop() throws Exception;
}
