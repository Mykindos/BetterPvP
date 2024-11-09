package me.mykindos.betterpvp.champions.champions.skills.skills.assassin.data;

import lombok.Data;

import java.util.UUID;

@Data
public class ShockingStrikesData {

    private final UUID player;
    private final UUID target;
    private int count;
    private long lastHit = System.currentTimeMillis();

    public void addCount() {
        count++;
    }

}
