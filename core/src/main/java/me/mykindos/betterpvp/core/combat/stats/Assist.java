package me.mykindos.betterpvp.core.combat.stats;

import lombok.Value;

import java.util.UUID;

@Value
public class Assist {

    UUID id = UUID.randomUUID();
    UUID player;

}
