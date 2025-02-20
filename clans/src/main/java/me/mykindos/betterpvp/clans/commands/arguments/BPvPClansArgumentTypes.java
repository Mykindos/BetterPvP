package me.mykindos.betterpvp.clans.commands.arguments;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.clans.Clans;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.commands.arguments.types.AllyClanArgument;
import me.mykindos.betterpvp.clans.commands.arguments.types.AllyOrEnemyClanArgument;
import me.mykindos.betterpvp.clans.commands.arguments.types.ClanArgument;
import me.mykindos.betterpvp.clans.commands.arguments.types.EnemyClanArgument;
import me.mykindos.betterpvp.clans.commands.arguments.types.TrustedClanArgument;
import me.mykindos.betterpvp.core.command.brigadier.arguments.BPvPArgumentTypes;

@Singleton
public class BPvPClansArgumentTypes {
    private static ClanArgument CLAN;
    private static EnemyClanArgument ENEMY_CLAN;
    private static AllyClanArgument ALLY_CLAN;
    private static TrustedClanArgument TRUSTED_CLAN;
    private static AllyOrEnemyClanArgument ALLY_OR_ENEMY_CLAN;
    @Inject
    public BPvPClansArgumentTypes(Clans plugin) {
        BPvPClansArgumentTypes.CLAN = (ClanArgument) BPvPArgumentTypes.createArgumentType(plugin, ClanArgument.class);
        BPvPClansArgumentTypes.ENEMY_CLAN = (EnemyClanArgument) BPvPArgumentTypes.createArgumentType(plugin, EnemyClanArgument.class);
        BPvPClansArgumentTypes.ALLY_CLAN = (AllyClanArgument) BPvPArgumentTypes.createArgumentType(plugin, AllyClanArgument.class);
        BPvPClansArgumentTypes.TRUSTED_CLAN = (TrustedClanArgument) BPvPArgumentTypes.createArgumentType(plugin, TrustedClanArgument.class);
        BPvPClansArgumentTypes.ALLY_OR_ENEMY_CLAN = (AllyOrEnemyClanArgument) BPvPArgumentTypes.createArgumentType(plugin, AllyOrEnemyClanArgument.class);
    }

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
