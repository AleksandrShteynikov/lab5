package ru.bmstu.iu9.lab5;

import akka.NotUsed;
import akka.actor.ActorSystem;
import akka.http.javadsl.ConnectHttp;
import akka.http.javadsl.Http;
import akka.http.javadsl.ServerBinding;
import akka.http.javadsl.model.HttpRequest;
import akka.http.javadsl.model.HttpResponse;
import akka.http.javadsl.model.Query;
import akka.stream.ActorMaterializer;
import akka.stream.javadsl.Flow;

import java.io.IOException;
import java.util.concurrent.CompletionStage;

public class StressTest {
    private final static int PORT = 8080;
    private final static String HOST_NAME = "localhost";
    private final static String AKKA_SYSTEM_NAME = "AkkaStressTester";
    private final static String SERVER_MSG = "Server online at http://" + HOST_NAME + ":" + PORT +"/\nPress RETURN to stop...";

    public static void main(String[] args) throws IOException {
        ActorSystem system = ActorSystem.create(AKKA_SYSTEM_NAME);
        final Http http = Http.get(system);
        final ActorMaterializer materializer = ActorMaterializer.create(system);
        StressTest instance = new StressTest();
        final Flow<HttpRequest, HttpResponse, NotUsed> routeFlow = instance.flow();
        final CompletionStage<ServerBinding> binding = http.bindAndHandle(routeFlow,
                                                                          ConnectHttp.toHost(HOST_NAME, PORT),
                                                                          materializer);
        System.out.println(SERVER_MSG);
        binding.thenCompose(ServerBinding::unbind).thenAccept(unbound -> system.terminate());
    }

    private Flow<HttpRequest, HttpResponse, NotUsed> flow() {
        return Flow.of(HttpRequest.class)
                .map(req -> {
                    Query queries = req.getUri().query();
                    
                })
                .mapAsync()
                .map()
    }
}