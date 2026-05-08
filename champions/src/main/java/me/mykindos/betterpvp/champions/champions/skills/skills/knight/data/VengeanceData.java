package me.mykindos.betterpvp.champions.champions.skills.skills.knight.data;

import lombok.Data;

@Data
public class VengeanceData {
    private int hitsTaken = 0;

    /**
     * In milliseconds.
     */
    private long lastTimeWhenTakenDamage = System.currentTimeMillis();
}
