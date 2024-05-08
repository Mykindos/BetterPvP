package me.mykindos.betterpvp.progression.tree.fishing.data;

import lombok.Data;

import java.util.UUID;

@Data
public class CaughtFish {

    private final UUID gamer;
    private final String type;
    private final int weight;

}
