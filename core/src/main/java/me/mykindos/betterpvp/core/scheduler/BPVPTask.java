package me.mykindos.betterpvp.core.scheduler;

import lombok.Getter;

import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Predicate;

@Getter
public class BPVPTask {

    private final UUID uuid;
    private final Predicate<UUID> predicate;
    private final Consumer<UUID> consumer;
    private final long expiryTime;

    public BPVPTask(UUID uuid, Predicate<UUID> predicate, Consumer<UUID> consumer, long expiryTime) {
        this.uuid = uuid;
        this.predicate = predicate;
        this.consumer = consumer;
        this.expiryTime = System.currentTimeMillis() + expiryTime;
    }
}
