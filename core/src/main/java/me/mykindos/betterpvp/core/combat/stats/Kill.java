package me.mykindos.betterpvp.core.combat.stats;

import lombok.Value;

import java.util.UUID;

@Value
public class Kill {

    UUID id = UUID.randomUUID();
    UUID killer;
    UUID victim;
    Assist[] assists;

}
