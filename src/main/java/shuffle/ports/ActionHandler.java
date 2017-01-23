package shuffle.ports;

/**
 * @author Drew Fead
 */
@FunctionalInterface
public interface ActionHandler {
    enum Status { SUCCESS, REJECTED, FAILED }

    interface Result {
        byte[] payload();
        Status status();
    }

    Result handle(String requestId, byte[] bytes) throws Exception;
}
