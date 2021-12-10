package ru.bmstu.iu9.lab5;

import akka.actor.AbstractActor;
import akka.japi.pf.ReceiveBuilder;

import java.util.HashMap;
import java.util.Map;

public class CacheActor extends AbstractActor {
    private final static String EMPTY = "";

    private final Map<String, Long> results = new HashMap<>();

    @Override
    public Receive createReceive() {
        return ReceiveBuilder.create()
                .match(String.class, req -> {
                    if (results.containsKey(req)) {
                        getSender().tell(new Answer(req, results.get(req)), self());
                    } else {
                        getSender().tell(new Answer(EMPTY, results.get(req)), self());
                    }
                })
                .match(Result.class, res -> {
                    results.put(res.getUrl(), res.getTime());
                })
                .build();
    }
}
