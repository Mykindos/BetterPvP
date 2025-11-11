package me.mykindos.betterpvp.core.loot;

import lombok.Data;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Represents the progress of a player in a loot table. An instance of this class holds the history
 * of generated loot bundles for a player so pity rules and drop strategies can be applied.
 */
@Data
public class LootProgress {

    /**
     * The history of generated loot bundles for the player.
     */
    protected final List<@NotNull LootBundle> history = new ArrayList<>();

    public @NotNull Collection<@NotNull LootBundle> getHistory() {
        return Collections.unmodifiableCollection(this.history);
    }

    /**
     * Gets the number of failed rolls for a given candidate.
     * @param candidate The candidate loot entry to check
     * @return The number of failed rolls for the given candidate. If the candidate is not found in the history,
     *         this method returns the total number of rolls.
     */
    public int getFailedRolls(Loot<?, ?> candidate) {
        for (int i = this.history.size() - 1; i >= 0; i--) {
            final LootBundle bundle = this.history.get(i);
            if (bundle.getLoot().contains(candidate)) {
                return this.history.size() - i - 1;
            }
        }
        return this.history.size();
    }
}
