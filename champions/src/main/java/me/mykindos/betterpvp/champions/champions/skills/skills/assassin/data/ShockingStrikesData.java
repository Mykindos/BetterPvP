package me.mykindos.betterpvp.champions.champions.skills.skills.assassin.data;

import lombok.Data;

import java.util.UUID;

@Data
public class ShockingStrikesData {

    private final UUID player;
    private final UUID target;
    private int count;
    private int blindCount;
    private long lastHit = System.currentTimeMillis();

    public void addCount() {
        count++;
    }
    public void resetCount(){
        count = 0;
    }
    public void addBlindCount() {
        blindCount++;
    }
    public void resetBlindCount(){
        blindCount = 0;
    }

}
