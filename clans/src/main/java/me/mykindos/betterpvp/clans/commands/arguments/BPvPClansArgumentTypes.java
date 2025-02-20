package me.mykindos.betterpvp.clans.commands.arguments;

import com.google.inject.Singleton;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import me.mykindos.betterpvp.clans.Clans;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.commands.arguments.types.AllyClanArgument;
import me.mykindos.betterpvp.clans.commands.arguments.types.AllyOrEnemyClanArgument;
import me.mykindos.betterpvp.clans.commands.arguments.types.ClanArgument;
import me.mykindos.betterpvp.clans.commands.arguments.types.EnemyClanArgument;
import me.mykindos.betterpvp.clans.commands.arguments.types.TrustedClanArgument;
import me.mykindos.betterpvp.core.command.brigadier.arguments.BPvPArgumentTypes;

@Singleton
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class BPvPClansArgumentTypes {
    private static final ClanArgument CLAN = (ClanArgument) BPvPArgumentTypes.createArgumentType(Clans.getPlugin(Clans.class), ClanArgument.class);;
    private static final EnemyClanArgument ENEMY_CLAN = (EnemyClanArgument) BPvPArgumentTypes.createArgumentType(Clans.getPlugin(Clans.class), EnemyClanArgument.class);
    private static final AllyClanArgument ALLY_CLAN = (AllyClanArgument) BPvPArgumentTypes.createArgumentType(Clans.getPlugin(Clans.class), AllyClanArgument.class);
    private static final TrustedClanArgument TRUSTED_CLAN = (TrustedClanArgument) BPvPArgumentTypes.createArgumentType(Clans.getPlugin(Clans.class), TrustedClanArgument.class);
    private static final AllyOrEnemyClanArgument ALLY_OR_ENEMY_CLAN = (AllyOrEnemyClanArgument) BPvPArgumentTypes.createArgumentType(Clans.getPlugin(Clans.class), AllyOrEnemyClanArgument.class);

    /**
     * Prompts the sender with any {@link Clan}. Guarantees a valid return {@link Clan}.
     * @return the {@link ClanArgument}
     */
    public static ClanArgument clan() {
        return CLAN;
    }

    /**
     * Prompts the sender with the executor's {@link Clan}'s enemies. Guarantees a valid return {@link Clan}, but not if it is an enemy of the executor.
     * @return the {@link EnemyClanArgument}
     */
    public static EnemyClanArgument enemyClan() {
        return ENEMY_CLAN;
    }

    /**
     * Prompts the sender with the executor's {@link Clan}'s allies. Guarantees a valid return {@link Clan}, but not if it is an ally of the executor.
     * @return the {@link AllyClanArgument}
     */
    public static AllyClanArgument allyClan() {
        return ALLY_CLAN;
    }

    /**
     * Prompts the sender with the executor's {@link Clan}'s trusted allies. Guarantees a valid return {@link Clan}, but not if it is a trusted ally of the executor.
     * @return the {@link TrustedClanArgument}
     */
    public static TrustedClanArgument trustedClan() {
        return TRUSTED_CLAN;
    }

    /**
     * Prompts the sender with the executor's {@link Clan}'s allies or enemies. Guarantees a valid return {@link Clan}, but not if it is an ally or enemy of the executor.
     * <p>Combines functionality of {@link BPvPClansArgumentTypes#allyClan()} and {@link BPvPClansArgumentTypes#enemyClan()}</p>
     * @return the {@link AllyOrEnemyClanArgument}
     * @see BPvPClansArgumentTypes#allyClan()
     * @see BPvPClansArgumentTypes#enemyClan()
     */
    public static AllyOrEnemyClanArgument allyOrEnemyClan() {
        return ALLY_OR_ENEMY_CLAN;
    }


}
