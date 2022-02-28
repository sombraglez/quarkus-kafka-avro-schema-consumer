package org.acme.kafka;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.smallrye.reactive.messaging.providers.connectors.InMemoryConnector;
import io.smallrye.reactive.messaging.providers.connectors.InMemorySink;
import io.smallrye.reactive.messaging.providers.connectors.InMemorySource;
import org.acme.kafka.config.KafkaTestResourceLifecycleManager;
import org.acme.kafka.quarkus.Movie;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

import javax.enterprise.inject.Any;
import javax.inject.Inject;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.sse.SseEventSource;
import java.net.URI;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.MatcherAssert.assertThat;

@QuarkusTest
@QuarkusTestResource(KafkaTestResourceLifecycleManager.class)
public class ConsumedMovieResourceTest {

    @TestHTTPResource("/consumed-movies")
    URI consumedMovies;

    @Inject
    @Any
    InMemoryConnector connector;

    @Test
    public void testMovieConsumer() throws InterruptedException {
        InMemorySource<Movie> movies = connector.source("movies-from-kafka");
        // create a client for `ConsumedMovieResource` and collect the consumed resources in a list
        Client client = ClientBuilder.newClient();
        WebTarget target = client.target(consumedMovies);

        List<String> received = new CopyOnWriteArrayList<>();

        SseEventSource source = SseEventSource.target(target).build();
        source.register(inboundSseEvent -> received.add(inboundSseEvent.readData()));

        // in a separate thread, feed the Movie Topic
        ExecutorService movieSender = startSendingMovies(movies);

        source.open();

        // check if, after at most 5 seconds, we have at least 2 items collected, and they are what we expect
        await().atMost(5, SECONDS).until(() -> received.size() >= 2);
        assertThat(received, Matchers.hasItems("'The Shawshank Redemption' from 1994",
                "'12 Angry Men' from 1957"));
        source.close();

        // shutdown the executor that is feeding the `MovieResource`
        movieSender.shutdownNow();
        movieSender.awaitTermination(5, SECONDS);
    }

    private ExecutorService startSendingMovies(InMemorySource<Movie> movieTopic) {
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        executorService.execute(() -> {
            while (true) {
                movieTopic.send(new Movie("The Shawshank Redemption",1994));
                movieTopic.send(new Movie("12 Angry Men",1957));
                try {
                    Thread.sleep(200L);
                } catch (InterruptedException e) {
                    break;
                }
            }
        });
        return executorService;
    }


}
