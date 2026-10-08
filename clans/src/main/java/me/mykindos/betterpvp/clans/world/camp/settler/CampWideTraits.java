package me.mykindos.betterpvp.clans.world.camp.settler;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.world.settler.Roster;
import me.mykindos.betterpvp.core.world.settler.Settler;
import org.jetbrains.annotations.NotNull;

/**
 * Traits that act on the whole camp from wherever their settler is: Bard, Cook, Recruiter, Haggler, Quartermaster,
 * Chronicler, Lookout and Beloved. When several settlers have the same one, only the strongest counts.
 */
@Singleton
public class CampWideTraits {

    private final SettlerConfig config;

    @Inject
    public CampWideTraits(@NotNull SettlerConfig config) {
        this.config = config;
    }

    /** Whether anyone in {@code roster} has {@code trait}. */
    public boolean any(@NotNull Roster roster, @NotNull String trait) {
        return roster.getSettlers().stream().anyMatch(settler -> settler.hasTrait(trait));
    }

    /**
     * The strongest {@code number} of {@code trait} anyone in {@code roster} brings, at their trait strength, or 0 when
     * nobody has it.
     */
    public double best(@NotNull Roster roster, @NotNull String trait, @NotNull String number, double fallback) {
        return roster.getSettlers().stream()
                .filter(settler -> settler.hasTrait(trait))
                .mapToDouble(settler -> config.trait(trait, number, fallback) * strength(settler))
                .max()
                .orElse(0);
    }

    private double strength(@NotNull Settler settler) {
        return config.getTable().rarity(settler.getRarity()).getTraitStrength();
    }
}
