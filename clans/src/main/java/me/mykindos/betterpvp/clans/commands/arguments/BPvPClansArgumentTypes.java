package me.mykindos.betterpvp.clans.commands.arguments;

import com.google.inject.Singleton;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import me.mykindos.betterpvp.clans.Clans;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.clans.commands.arguments.types.ClanNameArgument;
import me.mykindos.betterpvp.clans.commands.arguments.types.clan.AllyClanArgument;
import me.mykindos.betterpvp.clans.commands.arguments.types.clan.AllyOrEnemyClanArgument;
import me.mykindos.betterpvp.clans.commands.arguments.types.clan.AllyableClanArgument;
import me.mykindos.betterpvp.clans.commands.arguments.types.clan.ClanArgument;
import me.mykindos.betterpvp.clans.commands.arguments.types.clan.EnemyClanArgument;
import me.mykindos.betterpvp.clans.commands.arguments.types.clan.NeutralClanArgument;
import me.mykindos.betterpvp.clans.commands.arguments.types.clan.TrustableClanArgument;
import me.mykindos.betterpvp.clans.commands.arguments.types.clan.TrustedClanArgument;
import me.mykindos.betterpvp.core.command.brigadier.arguments.BPvPArgumentTypes;

@Singleton
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class BPvPClansArgumentTypes {
    private static final ClanArgument CLAN = (ClanArgument) BPvPArgumentTypes.createArgumentType(Clans.getPlugin(Clans.class), ClanArgument.class);
    private static final EnemyClanArgument ENEMY_CLAN = (EnemyClanArgument) BPvPArgumentTypes.createArgumentType(Clans.getPlugin(Clans.class), EnemyClanArgument.class);
    private static final AllyClanArgument ALLY_CLAN = (AllyClanArgument) BPvPArgumentTypes.createArgumentType(Clans.getPlugin(Clans.class), AllyClanArgument.class);
    private static final TrustedClanArgument TRUSTED_CLAN = (TrustedClanArgument) BPvPArgumentTypes.createArgumentType(Clans.getPlugin(Clans.class), TrustedClanArgument.class);
    private static final AllyOrEnemyClanArgument ALLY_OR_ENEMY_CLAN = (AllyOrEnemyClanArgument) BPvPArgumentTypes.createArgumentType(Clans.getPlugin(Clans.class), AllyOrEnemyClanArgument.class);
    private static final NeutralClanArgument NEUTRAL_CLAN = (NeutralClanArgument) BPvPArgumentTypes.createArgumentType(Clans.getPlugin(Clans.class), NeutralClanArgument.class);
    private static final AllyableClanArgument ALLYABLE_CLAN = (AllyableClanArgument) BPvPArgumentTypes.createArgumentType(Clans.getPlugin(Clans.class), AllyableClanArgument.class);
    private static final TrustableClanArgument TRUSTABLE_CLAN = (TrustableClanArgument) BPvPArgumentTypes.createArgumentType(Clans.getPlugin(Clans.class), TrustableClanArgument.class);
    private static final ClanNameArgument CLAN_NAME = (ClanNameArgument) BPvPArgumentTypes.createArgumentType(Clans.getPlugin(Clans.class), ClanNameArgument.class);

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

    /**
     * Prompts the sender with the executor's {@link Clan}'s neutral {@link Clan}s. Guarantees a valid return {@link Clan}, but not if it is a neutral {@link Clan} of the executor.
     * <p>Inverts functionality of {@link BPvPClansArgumentTypes#allyOrEnemyClan()}</p>
     * @return the {@link NeutralClanArgument}
     * @see BPvPClansArgumentTypes#allyOrEnemyClan()
     */
    public static NeutralClanArgument neutralClan() {
        return NEUTRAL_CLAN;
    }

    /**
     * Prompts the sender with the executor's {@link Clan}'s allable {@link Clan}s. Guarantees a valid return {@link Clan}, but not if it is a allyable {@link Clan} of the executor.
     * <p>Uses {@link ClanManager#canAlly(Clan, Clan)} to determine if a {@link Clan} is allyable</p>
     * @return the {@link AllyableClanArgument}
     * @see BPvPClansArgumentTypes#allyOrEnemyClan()
     */
    public static AllyableClanArgument allyableClan() {
        return ALLYABLE_CLAN;
    }

    /**
     * Prompts the sender with the executor's {@link Clan}'s trustable {@link Clan}s. Guarantees a valid return {@link Clan}, but not if it is a trustable {@link Clan} of the executor.
     * <p>Uses {@link ClanManager#canTrust(Clan, Clan)} to determine if a {@link Clan} is trustable</p>
     * @return the {@link NeutralClanArgument}
     * @see BPvPClansArgumentTypes#allyClan()
     */
    public static TrustableClanArgument trustableClan() {
        return TRUSTABLE_CLAN;
    }

    /**
     * Shows a prompt message that this is a Clan Name.
     * <p>Enforces that the returned name contains valid characters and is correct length
     * and is not already taken</p>
     * @return the {@link ClanNameArgument}
     */
    public static ClanNameArgument clanName() {
        return CLAN_NAME;
    }


}
