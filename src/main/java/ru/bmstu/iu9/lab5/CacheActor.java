package ru.bmstu.iu9.lab5;

import akka.actor.AbstractActor;
import akka.japi.pf.ReceiveBuilder;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class CacheActor extends AbstractActor {

    private final Map<String, Long> results = new HashMap<>();

    @Override
    public Receive createReceive() {
        return ReceiveBuilder.create()
                .match(String.class, req -> getSender()
                                            .tell(Optional.ofNullable(results.get(req)), self()))
                .match(Result.class, res -> {
                    results.put(res.getUrl(), res.getTime());
                })
                .build();
    }
}
