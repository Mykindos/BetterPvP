package me.mykindos.betterpvp.core.utilities.model.display.experience.data;

import lombok.Getter;

public class ExperienceLevelData {
    @Getter
    private final int level;

    public ExperienceLevelData(int level) {
        this.level = Math.max(level, 0);
    }
}