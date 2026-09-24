package me.mykindos.betterpvp.clans.world.camp.settler.prosperity;

import lombok.Value;
import me.mykindos.betterpvp.core.world.settler.SettlerRarity;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/** What makes up a camp's Prosperity: every factor with the signed amount it adds, and the total. */
@Value
public class ProsperityFactors {

    List<Factor> factors;
    int total;

    public enum Kind {
        /** The settlers of one rarity. {@code detail} is how many there are. */
        SETTLERS,
        /** The camp's average morale, which is {@code detail}. */
        MORALE,
        /** The best Chronicler's share, which is {@code detail}. */
        CHRONICLER
    }

    /** One thing that raises or lowers Prosperity, and by how much. */
    @Value
    public static class Factor {
        Kind kind;
        /** The rarity a {@link Kind#SETTLERS} factor counts, otherwise null. */
        @Nullable SettlerRarity rarity;
        double detail;
        double amount;
    }
}
