package me.mykindos.betterpvp.champions.champions.skills.skills.assassin.data;

import lombok.Data;

@Data
public class HairTriggerData {
    private int tagCount = 0;
    private long lastTagTime = -1;
    private boolean windingUp = false;
    private long windUpStartTime = -1;
}
