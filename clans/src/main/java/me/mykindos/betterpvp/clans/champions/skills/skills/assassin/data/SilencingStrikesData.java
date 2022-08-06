package me.mykindos.betterpvp.clans.champions.skills.skills.assassin.data;

import lombok.Data;

import java.util.UUID;

@Data
public class SilencingStrikesData {

    private final UUID player;
    private final UUID target;
    private int count;
    private long lastHit = System.currentTimeMillis();

    public void addCount() {
        count++;
    }

}
