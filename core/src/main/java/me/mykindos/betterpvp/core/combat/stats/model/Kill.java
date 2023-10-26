package me.mykindos.betterpvp.core.combat.stats.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
@Getter
public class Kill {

    private final UUID id = UUID.randomUUID();
    private final UUID killer;
    private final UUID victim;
    private final int ratingDelta;
    private final List<Contribution> contributions;
    private final Instant timestamp = Instant.now();

}
