package me.mykindos.betterpvp.progression.model;

import lombok.Data;
import org.jetbrains.annotations.Range;

/**
 * Represents a player's progression data within a single {@link ProgressionTree}
 * @param <T> The type of {@link ProgressionTree} this data is for.
 */
@Data
public abstract class ProgressionData<T extends ProgressionTree> {

    /**
     * Their total experience in this tree.
     */
    private @Range(from = 0, to = Integer.MAX_VALUE) int experience;

    public void grantExperience(@Range(from = 0, to = Integer.MAX_VALUE) int amount) {
        this.experience += amount;
    }

}
