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
import akka.stream.javadsl.Keep;
import akka.stream.javadsl.Sink;
import akka.stream.javadsl.Source;
import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.Dsl;
import org.asynchttpclient.Request;
import org.asynchttpclient.Response;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
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
    private final static String CONNECTOR = " : ";

    public static void main(String[] args) throws IOException {
        ActorSystem system = ActorSystem.create(AKKA_SYSTEM_NAME);
        ActorRef cacheActor = system.actorOf(Props.create(CacheActor.class));
        final Http http = Http.get(system);
        final ActorMaterializer materializer = ActorMaterializer.create(system);
        StressTest instance = new StressTest();
        final Flow<HttpRequest, HttpResponse, NotUsed> routeFlow = instance.flow(cacheActor, materializer);
        final CompletionStage<ServerBinding> binding = http.bindAndHandle(routeFlow,
                                                                          ConnectHttp.toHost(HOST_NAME, PORT),
                                                                          materializer);
        System.out.println(SERVER_MSG);
        System.in.read();
        binding.thenCompose(ServerBinding::unbind).thenAccept(unbound -> system.terminate());
    }

    private Flow<HttpRequest, HttpResponse, NotUsed> flow(ActorRef actor, ActorMaterializer materializer) {
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
                                                           java.time.Duration.ofMillis(TIMEOUT))
                        .thenCompose(resp -> {
                            Answer respAnsw = (Answer) resp;
                            if (!respAnsw.getUrl().isEmpty()) {
                                return CompletableFuture.completedFuture(respAnsw.getUrl() + CONNECTOR + respAnsw.getTime());
                            } else {
                                Sink<Pair<String, Integer>, CompletionStage<Long>> sink = Flow.<Pair<String, Integer>>create()
                                        .mapConcat(reqSink -> {
                                            ArrayList<String> urls = new ArrayList<>();
                                            for (int i =0; i < reqSink.second(); i++) {
                                                urls.add(reqSink.first());
                                            }
                                            return urls;
                                        })
                                        .mapAsync(req.second(), url -> {
                                            AsyncHttpClient client = Dsl.asyncHttpClient();
                                            Request getRequest = Dsl.get(url).build();
                                            long start = System.currentTimeMillis();
                                            CompletableFuture<Response> whenResponse = client.executeRequest(getRequest)
                                                                                             .toCompletableFuture();
                                            return whenResponse
                                                   .thenCompose(res ->
                                                                CompletableFuture.completedFuture(System.currentTimeMillis() - start));
                                        })
                                        .toMat(Sink.fold(0L, Long::sum), Keep.right());
                                return Source.from(Collections.singletonList(req))
                                        .toMat(sink, Keep.right())
                                        .run(materializer)
                                        .thenApply(sum -> new Result(req.first(), sum / req.second()));
                            }
                        }))
                .map(resp -> {
                    actor.tell(resp, ActorRef.noSender());
                    return HttpResponse.create().withEntity("");
                });
    }
}