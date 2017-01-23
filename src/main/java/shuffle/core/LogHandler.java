package shuffle.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import shuffle.ports.ActionHandler;

/**
 * @author Drew Fead
 */
public class LogHandler implements ActionHandler {
    private static final Logger log = LoggerFactory.getLogger(LogHandler.class);

    public Result handle(String requestId, byte[] bytes) {
        if(log.isInfoEnabled()) { log.info(String.format("[%s] %s", requestId, bytes)); }

        return new Result() {
            public byte[] payload() { return bytes; }
            public Status status() { return Status.SUCCESS; }
        };
    }
}
