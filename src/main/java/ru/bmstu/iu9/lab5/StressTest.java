package ru.bmstu.iu9.lab5;

import akka.actor.ActorSystem;
import akka.http.javadsl.Http;
import akka.stream.ActorMaterializer;

import java.io.IOException;

public class StressTest {
    private final static String AKKA_SYSTEM_NAME = "AkkaStressTester";

    public static void main(String[] args) throws IOException {
        ActorSystem system = ActorSystem.create(AKKA_SYSTEM_NAME);
        final Http http = Http.get(system);
        final ActorMaterializer materializer = ActorMaterializer.create(system);
        final Flow
    }
}
