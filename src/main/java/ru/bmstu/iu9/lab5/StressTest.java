package ru.bmstu.iu9.lab5;

import akka.NotUsed;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.http.javadsl.ConnectHttp;
import akka.http.javadsl.Http;
import akka.http.javadsl.ServerBinding;
import akka.http.javadsl.model.HttpRequest;
import akka.http.javadsl.model.HttpResponse;
import akka.http.javadsl.model.Query;
import akka.japi.Pair;
import akka.pattern.Patterns;
import akka.stream.ActorMaterializer;
import akka.stream.javadsl.Flow;
import akka.util.Timeout;

import java.io.IOException;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CompletableFuture;

public class StressTest {
    private final static int PARALLELISM = 1;
    private final static int PORT = 8080;
    private final static int TIMEOUT = 5000;
    private final static String HOST_NAME = "localhost";
    private final static String AKKA_SYSTEM_NAME = "AkkaStressTester";
    private final static String SERVER_MSG = "Server online at http://" + HOST_NAME + ":" + PORT +"/\nPress RETURN to stop...";
    private final static String URL_QUERY_KEY = "testUrl";
    private final static String COUNT_QUERY_KEY = "count";
    private final static String DEFAULT_URL = "https://yandex.ru";
    private final static String DEFAULT_COUNT = "0";

    public static void main(String[] args) throws IOException {
        ActorSystem system = ActorSystem.create(AKKA_SYSTEM_NAME);
        ActorRef cacheActor = system.actorOf(Props.create(CacheActor.class));
        final Http http = Http.get(system);
        final ActorMaterializer materializer = ActorMaterializer.create(system);
        StressTest instance = new StressTest();
        final Flow<HttpRequest, HttpResponse, NotUsed> routeFlow = instance.flow(cacheActor);
        final CompletionStage<ServerBinding> binding = http.bindAndHandle(routeFlow,
                                                                          ConnectHttp.toHost(HOST_NAME, PORT),
                                                                          materializer);
        System.out.println(SERVER_MSG);
        binding.thenCompose(ServerBinding::unbind).thenAccept(unbound -> system.terminate());
    }

    private Flow<HttpRequest, HttpResponse, NotUsed> flow(ActorRef actor) {
        return Flow.of(HttpRequest.class)
                .map(req -> {
                    Query queries = req.getUri().query();
                    String url = DEFAULT_URL;
                    String countS = DEFAULT_COUNT;
                    if (queries.get(URL_QUERY_KEY).isPresent()) {
                        url = queries.get(URL_QUERY_KEY).get();
                    }
                    if (queries.get(COUNT_QUERY_KEY).isPresent()) {
                        countS = queries.get(COUNT_QUERY_KEY).get();
                    }
                    int count = Integer.parseInt(countS);
                    return new Pair<>(url, count);
                })
                .mapAsync(PARALLELISM, req -> Patterns.ask(actor,
                                                           req.first(),
                                                           Timeout.create(Duration.ofMillis(TIMEOUT)))
                        .thenCompose(resp -> {
                            if (((Optional<Long>) resp).isPresent()) {

                            } else {

                            }
                        }))
                .map(resp -> {
                    actor.tell(new Result(), ActorRef.noSender());
                    return HttpResponse.create().withEntity("");
                });
    }
}