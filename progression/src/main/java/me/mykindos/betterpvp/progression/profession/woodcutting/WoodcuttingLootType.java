package me.mykindos.betterpvp.progression.profession.woodcutting;

import lombok.Getter;

@Getter
public enum WoodcuttingLootType {
    COMMON(10),
    UNCOMMON(5),
    RARE(2),
    EPIC(1),
    LEGENDARY(1);

    private int numVal;

    WoodcuttingLootType(int numVal) {
        this.numVal = numVal;
    }

}
