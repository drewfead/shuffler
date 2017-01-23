package integration;

import com.google.common.collect.ImmutableMap;
import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.MapConfiguration;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.EntityBuilder;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.HttpClientBuilder;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import shuffle.adapters.driver.HttpDriver.Method;
import shuffle.app.App;

import java.net.URI;

public class IntegrationTest {

    static HttpClient httpClient;

    @BeforeClass public static void spinup() {
        final Integer port = 9421;
        final Integer pageSize = 5;
        final Boolean humanShuffle = Boolean.FALSE;

        final Configuration config = new MapConfiguration(
            ImmutableMap.of(
                App.PORT, port,
                App.PAGE_SIZE, pageSize,
                App.HUMAN_SHUFFLE, humanShuffle
            )
        );

        //App.logging();
        App.start(config);
        httpClient = HttpClientBuilder.create()
            .setMaxConnTotal(10)
            .setMaxConnPerRoute(10)
            .build();
    }

    @AfterClass public static void cleanup() {
        App.stop();
    }

    private static HttpResponse request(HttpClient cli, Method method, String path, String message, String name)
            throws Exception {

        final String url = "http://localhost:9421/" + path;
        final URI paramURI = new URIBuilder()
                .setScheme("http")
                .setHost("localhost")
                .setPort(9421)
                .setPath(path)
                .addParameter("name", name)
                .build();

        final HttpResponse response;
        switch(method) {
            case POST:
                final HttpPost post = new HttpPost(url);
                post.setEntity(EntityBuilder.create().setText(message).build());
                response = cli.execute(post);
                break;
            case GET:
                final HttpGet get = new HttpGet(paramURI);
                response = cli.execute(get);
                break;
            case PUT:
                final HttpPut put = new HttpPut(paramURI);
                response = cli.execute(put);
                break;
            case DELETE:
                final HttpDelete delete = new HttpDelete(paramURI);
                response = cli.execute(delete);
                break;
            default:
                throw new RuntimeException();
        }

        return response;
    }

    @Test public void basic() throws Exception {
        final HttpResponse create = request(httpClient, Method.PUT, "/deck/create", null, "test");

        final String shuffleJson = "{\"name\":\"test\"}";
        final HttpResponse shuffle = request(httpClient, Method.POST, "/deck/shuffle", shuffleJson, null);

        final HttpResponse delete = request(httpClient, Method.DELETE, "/deck/delete", null, "test");
    }

}
