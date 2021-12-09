package ru.bmstu.iu9.lab5;

import akka.actor.ActorSystem;

import java.io.IOException;

public class StressTest {
    private final static String AKKA_SYSTEM_NAME = "AkkaStressTester";

    public static void main(String[] args) throws IOException {
        ActorSystem system = ActorSystem.create(AKKA_SYSTEM_NAME);
    }
}
