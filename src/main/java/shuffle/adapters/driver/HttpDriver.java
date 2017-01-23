package shuffle.adapters.driver;

import com.google.common.collect.ImmutableMap;
import org.apache.commons.io.IOUtils;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import shuffle.ports.ActionHandler.Result;
import shuffle.ports.ActionHandler.Status;
import shuffle.ports.Driver;
import shuffle.ports.ActionHandler;
import shuffle.ports.Monitor;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;

import static shuffle.ports.Monitor.MetricType.REQUEST_DURATION;

/**
 * @author Drew Fead
 */
public class HttpDriver implements Driver {
    private static Logger log = LoggerFactory.getLogger(HttpDriver.class);

    private static final String NAME_PARAM = "name";

    public enum Method { PUT, POST, GET, DELETE }

    public interface HttpAction {
        ActionHandler handler();
        Method method();
    }

    public static HttpAction action (ActionHandler handler, Method method) {
        return new HttpAction() {
            public ActionHandler handler() { return handler; }
            public Method method() { return method; }
        };
    }

    private Map<String, HttpAction> pathActions;
    private Monitor monitor;
    private Server server;
    private int port;

    public HttpDriver(int port, Map<String, HttpAction> pathActions, Monitor monitor) {
        this.port = port;
        this.pathActions = pathActions;
        this.monitor = monitor;
    }

    private static final Map<Status, Integer> statusCodes = ImmutableMap.of(
        Status.SUCCESS, HttpServletResponse.SC_OK,
        Status.REJECTED, HttpServletResponse.SC_BAD_REQUEST,
        Status.FAILED, HttpServletResponse.SC_INTERNAL_SERVER_ERROR
    );

    private ContextHandler ctx(String path, HttpAction action) {
        ContextHandler ch = new ContextHandler(path);
        ch.setHandler(new AbstractHandler() {
            public void handle(String tg, Request base, HttpServletRequest req, HttpServletResponse res)
                    throws IOException, ServletException {

                final long start = System.currentTimeMillis();
                final String requestId = UUID.randomUUID().toString();
                MDC.put("request_id", requestId);

                byte[] out = new byte[0];
                int code = 0;
                try {
                    final byte[] in;
                    if(action.method() == Method.POST) {
                        in = IOUtils.toByteArray(base.getInputStream());
                    } else {
                        in = req.getParameter(NAME_PARAM).getBytes();
                    }

                    final Result result = action.handler().handle(requestId, in);
                    code = statusCodes.get(result.status());
                    out = result.payload();

                } catch (Exception e) {
                    code = HttpServletResponse.SC_INTERNAL_SERVER_ERROR;

                } finally {
                    try {
                        res.setStatus(code);
                        res.getOutputStream().write(out);
                        res.getOutputStream().flush();
                        base.setHandled(true);

                    } catch (IOException e) {
                        if(log.isErrorEnabled()) { log.error("failed to write response", e); }
                        throw e;

                    } try {
                        final long duration = System.currentTimeMillis() - start;
                        monitor.logMetric(requestId, REQUEST_DURATION, duration);

                    } catch (Exception e) {
                        if(log.isWarnEnabled()) {log.warn("failed to log metrics", e); }

                    } finally {
                        MDC.clear();
                    }
                }
            }
        });

        return ch;
    }

    public void start() throws Exception {
        server = new Server(port);

        if(log.isInfoEnabled()) { log.info("listening on port" + port); }
        final ContextHandlerCollection contexts = new ContextHandlerCollection();
        contexts.setHandlers(
            pathActions.entrySet().stream()
                .map(e -> ctx(e.getKey(), e.getValue()))
                .toArray(ContextHandler[]::new)
        );

        server.setHandler(contexts);
        server.start();
    }

    public void stop() throws Exception{
        server.stop();
    }
}
