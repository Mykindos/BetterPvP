package me.mykindos.betterpvp.core.utilities.model.display.experience.data;

import lombok.Getter;

public class ExperienceBarData {
    @Getter
    private final float percentage;

    public ExperienceBarData(float percentage) {
        this.percentage = Math.max(Math.min(0.9999999f, percentage), 0.000f);
    }
}