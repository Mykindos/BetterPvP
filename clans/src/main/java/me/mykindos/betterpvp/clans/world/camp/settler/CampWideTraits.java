package me.mykindos.betterpvp.clans.world.camp.settler;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.world.settler.Roster;
import me.mykindos.betterpvp.core.world.settler.Settler;
import me.mykindos.betterpvp.core.world.settler.SettlerState;
import org.jetbrains.annotations.NotNull;

/**
 * Traits that act on the whole camp from wherever their settler is: Bard, Cook, Recruiter, Haggler, Quartermaster,
 * Chronicler, Lookout and Beloved. Several settlers with one of them do not stack, the strongest counts. A settler on
 * its way out no longer helps.
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
        return roster.getSettlers().stream().anyMatch(settler -> counts(settler, trait));
    }

    /**
     * The strongest {@code number} of {@code trait} anyone in {@code roster} brings, at their trait strength, or 0 when
     * nobody has it.
     */
    public double best(@NotNull Roster roster, @NotNull String trait, @NotNull String number, double fallback) {
        return roster.getSettlers().stream()
                .filter(settler -> counts(settler, trait))
                .mapToDouble(settler -> config.trait(trait, number, fallback) * strength(settler))
                .max()
                .orElse(0);
    }

    private static boolean counts(@NotNull Settler settler, @NotNull String trait) {
        return settler.hasTrait(trait) && settler.getState() != SettlerState.LEAVING;
    }

    private double strength(@NotNull Settler settler) {
        return config.getTable().rarity(settler.getRarity()).getTraitStrength();
    }
}
