package me.mykindos.betterpvp.core.logging;

import lombok.Data;

import java.util.UUID;

@Data
public class PendingLog {

    private final UUID id;
    private final String className;
    private final String level;
    private final String message;
    private final long time;
    private final Object[] args;

}
