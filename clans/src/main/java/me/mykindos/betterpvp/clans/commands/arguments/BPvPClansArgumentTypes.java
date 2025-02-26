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
import me.mykindos.betterpvp.clans.commands.arguments.types.member.ClanMemberArgument;
import me.mykindos.betterpvp.clans.commands.arguments.types.member.DemotableMemberArgument;
import me.mykindos.betterpvp.clans.commands.arguments.types.member.LowerRankMemberArgument;
import me.mykindos.betterpvp.core.command.brigadier.arguments.BPvPArgumentTypes;
import me.mykindos.betterpvp.core.components.clans.data.ClanMember;

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

    private static final ClanMemberArgument CLAN_MEMBER = (ClanMemberArgument) BPvPArgumentTypes.createArgumentType(Clans.getPlugin(Clans.class), ClanMemberArgument.class);
    private static final LowerRankMemberArgument CLAN_MEMBER_LOWER_RANK = (LowerRankMemberArgument) BPvPArgumentTypes.createArgumentType(Clans.getPlugin(Clans.class), LowerRankMemberArgument.class);
    private static final DemotableMemberArgument CLAN_MEMBER_DEMOTABLE = (DemotableMemberArgument) BPvPArgumentTypes.createArgumentType(Clans.getPlugin(Clans.class), DemotableMemberArgument.class);
    /**
     * Prompts the sender with any {@link Clan}. Guarantees a valid return {@link Clan}.
     * <p>Casting class {@link Clan}</p>
     * @return the {@link ClanArgument}
     */
    public static ClanArgument clan() {
        return CLAN;
    }

    /**
     * Prompts the sender with the executor's {@link Clan}'s enemies. Guarantees a valid return {@link Clan}, but not if it is an enemy of the executor.
     * <p>Casting class {@link Clan}</p>
     * @return the {@link EnemyClanArgument}
     */
    public static EnemyClanArgument enemyClan() {
        return ENEMY_CLAN;
    }

    /**
     * Prompts the sender with the executor's {@link Clan}'s allies. Guarantees a valid return {@link Clan}, but not if it is an ally of the executor.
     * <p>Casting class {@link Clan}</p>
     * @return the {@link AllyClanArgument}
     */
    public static AllyClanArgument allyClan() {
        return ALLY_CLAN;
    }

    /**
     * Prompts the sender with the executor's {@link Clan}'s trusted allies. Guarantees a valid return {@link Clan}, but not if it is a trusted ally of the executor.
     * <p>Casting class {@link Clan}</p>
     * @return the {@link TrustedClanArgument}
     */
    public static TrustedClanArgument trustedClan() {
        return TRUSTED_CLAN;
    }

    /**
     * Prompts the sender with the executor's {@link Clan}'s allies or enemies. Guarantees a valid return {@link Clan}, but not if it is an ally or enemy of the executor.
     * <p>Combines functionality of {@link BPvPClansArgumentTypes#allyClan()} and {@link BPvPClansArgumentTypes#enemyClan()}</p>
     * <p>Casting class {@link Clan}</p>
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
     * <p>Casting class {@link Clan}</p>
     * @return the {@link NeutralClanArgument}
     * @see BPvPClansArgumentTypes#allyOrEnemyClan()
     */
    public static NeutralClanArgument neutralClan() {
        return NEUTRAL_CLAN;
    }

    /**
     * Prompts the sender with the executor's {@link Clan}'s allable {@link Clan}s. Guarantees a valid return {@link Clan}, but not if it is a allyable {@link Clan} of the executor.
     * <p>Uses {@link ClanManager#canAllyThrow(Clan, Clan)} to determine if a {@link Clan} is allyable</p>
     * <p>Casting class {@link Clan}</p>
     * @return the {@link AllyableClanArgument}
     * @see BPvPClansArgumentTypes#allyOrEnemyClan()
     */
    public static AllyableClanArgument allyableClan() {
        return ALLYABLE_CLAN;
    }

    /**
     * Prompts the sender with the executor's {@link Clan}'s trustable {@link Clan}s. Guarantees a valid return {@link Clan}, but not if it is a trustable {@link Clan} of the executor.
     * <p>Uses {@link ClanManager#canTrustThrow(Clan, Clan)} to determine if a {@link Clan} is trustable</p>
     * <p>Casting class {@link Clan}</p>
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
     * <p>Casting class{ @link String}</p>
     * @return the {@link ClanNameArgument}
     */
    public static ClanNameArgument clanName() {
        return CLAN_NAME;
    }

    /**
     * Prompts the sender with the executors {@link ClanMember}. Does not guarantee that the name is a valid {@link ClanMember}
     * <p>Casting class {@link String}</p>
     * @return the {@link ClanMemberArgument}
     */
    public static ClanMemberArgument clanMember() {
        return CLAN_MEMBER;
    }

    /**
     * Prompts the sender with the executors clan members that are lower rank than the executor {@link ClanMember}. Does not guarantee that the name is a valid {@link ClanMember}
     * <p>Casting class {@link String}</p>
     * @return the {@link LowerRankMemberArgument}
     */
    public static LowerRankMemberArgument lowerRankClanMember() {
        return CLAN_MEMBER_LOWER_RANK;
    }

    /**
     * Prompts the sender with the executors clan members that demotable by the executor {@link ClanMember}. Does not guarantee that the name is a valid {@link ClanMember}
     * <p>Carries out same logic as {@link LowerRankMemberArgument}, but also removes {@link ClanMember}'s with the {@link ClanMember.MemberRank#RECRUIT} rank</p>
     * <p>Casting class {@link String}</p>
     * @return the {@link DemotableMemberArgument}
     * @see BPvPClansArgumentTypes#lowerRankClanMember()
     */
    public static DemotableMemberArgument demotableClanMember() {
        return CLAN_MEMBER_DEMOTABLE;
    }


}
