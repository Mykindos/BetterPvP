package me.mykindos.betterpvp.core.world.settler;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * One person living at a site. It is data only, kept in its owner's record, so a settler outlives the world it walks
 * around in.
 */
@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Settler {

    private UUID id;
    private String name;
    /** Translation key of the line about where it came from. */
    private String history;
    /** What that line fills in. */
    private List<String> historyArgs = new ArrayList<>();
    private SettlerRarity rarity = SettlerRarity.COMMON;
    /** Its profession id, or null for a settler with none. */
    private @Nullable String profession;
    /** Which of its profession's specialties it has, or null when the profession has none. */
    private @Nullable String specialty;
    /** Trait ids, in the order they were rolled. */
    private List<String> traits = new ArrayList<>();
    /** From -100 to 100, where 0 is neutral. */
    private int morale;
    /** The workplace it is assigned to, or null when it has none. */
    private @Nullable String assignment;
    private SettlerState state = SettlerState.IDLE;
    /** How many jobs it has seen through to the end. */
    private int jobsFinished;
    private long joinedAt;
    /** When {@link #state} last changed. */
    private long stateSince;

    public boolean hasProfession(@NotNull String profession) {
        return profession.equals(this.profession);
    }

    public boolean hasTrait(@NotNull String trait) {
        return traits.contains(trait);
    }

    public void changeState(@NotNull SettlerState state, long now) {
        if (this.state != state) {
            this.state = state;
            this.stateSince = now;
        }
    }
}
