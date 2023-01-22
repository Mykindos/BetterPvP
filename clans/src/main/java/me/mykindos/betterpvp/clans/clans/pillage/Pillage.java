package me.mykindos.betterpvp.clans.clans.pillage;

import lombok.Data;
import me.mykindos.betterpvp.core.components.clans.IClan;

@Data
public class Pillage {

    private final IClan pillager;
    private final IClan pillaged;
    private long pillageFinishTime;
    private long pillageStartTime;

    public Pillage(IClan pillager, IClan pillaged) {
        this.pillager = pillager;
        this.pillaged = pillaged;
    }
}
