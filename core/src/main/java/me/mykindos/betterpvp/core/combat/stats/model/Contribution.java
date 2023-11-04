package me.mykindos.betterpvp.core.combat.stats.model;

import lombok.Value;
import org.jetbrains.annotations.Range;

import java.util.UUID;

@Value
public class Contribution {

    UUID id = UUID.randomUUID();
    UUID contributor;
    @Range(from = 0, to = Integer.MAX_VALUE) float damage;
    @Range(from = 0, to = 1) float percentage;

}
