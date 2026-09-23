package me.mykindos.betterpvp.core.world.settler;

import lombok.Value;

/** What one rarity means for a settler. */
@Value
public class RarityNumbers {

    public static final RarityNumbers PLAIN = new RarityNumbers(1, 1, 1, 0);

    /** How many traits it rolls. */
    int traits;
    /** What its traits' effects are multiplied by. */
    double traitStrength;
    /** What its profession stats are multiplied by. */
    double stats;
    /** The chance, from 0 to 1, that each trait it rolls is a trade-off. */
    double tradeOffChance;
}
