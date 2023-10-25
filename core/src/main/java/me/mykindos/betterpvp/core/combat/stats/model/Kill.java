package me.mykindos.betterpvp.core.combat.stats.model;

import lombok.Value;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Value
public class Kill {

    UUID id = UUID.randomUUID();
    UUID killer;
    UUID victim;
    int ratingDelta;
    List<Contribution> contributions;
    Instant timestamp = Instant.now();

}
