package me.mykindos.betterpvp.clans.commands.arguments;

import com.google.inject.Singleton;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import me.mykindos.betterpvp.clans.Clans;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.clans.commands.arguments.types.ClanNameArgument;
import me.mykindos.betterpvp.clans.commands.arguments.types.InvitablePlayerNameArgument;
import me.mykindos.betterpvp.clans.commands.arguments.types.clan.AllyClanArgument;
import me.mykindos.betterpvp.clans.commands.arguments.types.clan.AllyableClanArgument;
import me.mykindos.betterpvp.clans.commands.arguments.types.clan.ClanArgument;
import me.mykindos.betterpvp.clans.commands.arguments.types.clan.EnemyClanArgument;
import me.mykindos.betterpvp.clans.commands.arguments.types.clan.EnemyableClanArgument;
import me.mykindos.betterpvp.clans.commands.arguments.types.clan.JoinableClanArgument;
import me.mykindos.betterpvp.clans.commands.arguments.types.clan.NeutralClanArgument;
import me.mykindos.betterpvp.clans.commands.arguments.types.clan.NeutralableClanArgument;
import me.mykindos.betterpvp.clans.commands.arguments.types.clan.TrustableClanArgument;
import me.mykindos.betterpvp.clans.commands.arguments.types.clan.TrustedClanArgument;
import me.mykindos.betterpvp.clans.commands.arguments.types.member.ClanMemberArgument;
import me.mykindos.betterpvp.clans.commands.arguments.types.member.DemotableMemberArgument;
import me.mykindos.betterpvp.clans.commands.arguments.types.member.LowerRankMemberArgument;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.command.brigadier.arguments.BPvPArgumentTypes;
import me.mykindos.betterpvp.core.components.clans.data.ClanMember;
import org.bukkit.entity.Player;

@Singleton
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class BPvPClansArgumentTypes {
    private static final ClanArgument CLAN = (ClanArgument) BPvPArgumentTypes.createArgumentType(Clans.getPlugin(Clans.class), ClanArgument.class);
    private static final EnemyClanArgument ENEMY_CLAN = (EnemyClanArgument) BPvPArgumentTypes.createArgumentType(Clans.getPlugin(Clans.class), EnemyClanArgument.class);
    private static final AllyClanArgument ALLY_CLAN = (AllyClanArgument) BPvPArgumentTypes.createArgumentType(Clans.getPlugin(Clans.class), AllyClanArgument.class);
    private static final TrustedClanArgument TRUSTED_CLAN = (TrustedClanArgument) BPvPArgumentTypes.createArgumentType(Clans.getPlugin(Clans.class), TrustedClanArgument.class);
    private static final NeutralableClanArgument NEUTRALABLE_CLAN = (NeutralableClanArgument) BPvPArgumentTypes.createArgumentType(Clans.getPlugin(Clans.class), NeutralableClanArgument.class);
    private static final NeutralClanArgument NEUTRAL_CLAN = (NeutralClanArgument) BPvPArgumentTypes.createArgumentType(Clans.getPlugin(Clans.class), NeutralClanArgument.class);
    private static final AllyableClanArgument ALLYABLE_CLAN = (AllyableClanArgument) BPvPArgumentTypes.createArgumentType(Clans.getPlugin(Clans.class), AllyableClanArgument.class);
    private static final EnemyableClanArgument ENEMYABLE_CLAN = (EnemyableClanArgument) BPvPArgumentTypes.createArgumentType(Clans.getPlugin(Clans.class), EnemyableClanArgument.class);
    private static final TrustableClanArgument TRUSTABLE_CLAN = (TrustableClanArgument) BPvPArgumentTypes.createArgumentType(Clans.getPlugin(Clans.class), TrustableClanArgument.class);
    private static final JoinableClanArgument JOINABLE_CLAN = (JoinableClanArgument) BPvPArgumentTypes.createArgumentType(Clans.getPlugin(Clans.class), JoinableClanArgument.class);

    private static final ClanNameArgument CLAN_NAME = (ClanNameArgument) BPvPArgumentTypes.createArgumentType(Clans.getPlugin(Clans.class), ClanNameArgument.class);

    private static final ClanMemberArgument CLAN_MEMBER = (ClanMemberArgument) BPvPArgumentTypes.createArgumentType(Clans.getPlugin(Clans.class), ClanMemberArgument.class);
    private static final LowerRankMemberArgument CLAN_MEMBER_LOWER_RANK = (LowerRankMemberArgument) BPvPArgumentTypes.createArgumentType(Clans.getPlugin(Clans.class), LowerRankMemberArgument.class);
    private static final DemotableMemberArgument CLAN_MEMBER_DEMOTABLE = (DemotableMemberArgument) BPvPArgumentTypes.createArgumentType(Clans.getPlugin(Clans.class), DemotableMemberArgument.class);

    private static final InvitablePlayerNameArgument INVITABLE_PLAYER = (InvitablePlayerNameArgument) BPvPArgumentTypes.createArgumentType(Clans.getPlugin(Clans.class), InvitablePlayerNameArgument.class);
    /**
     * Prompts the sender with any {@link Clan}. Guarantees a valid return {@link Clan}.
     * <p>Casting class {@link Clan}</p>
     * @return the {@link ClanArgument}
     */
    public static ClanArgument clan() {
        return CLAN;
    }

    /**
     * Prompts the sender with the executor's {@link Clan}'s enemies. Guarantees a valid return {@link Clan} that is an enemy to the {@link CommandSourceStack#getExecutor() executor}
     * <p>Casting class {@link Clan}</p>
     * @return the {@link EnemyClanArgument}
     */
    public static EnemyClanArgument enemyClan() {
        return ENEMY_CLAN;
    }

    /**
     * Prompts the sender with the executor's {@link Clan}'s allies. Guarantees a valid return {@link Clan} that is allied to the {@link CommandSourceStack#getExecutor() executor}
     * <p>Casting class {@link Clan}</p>
     * @return the {@link AllyClanArgument}
     */
    public static AllyClanArgument allyClan() {
        return ALLY_CLAN;
    }

    /**
     * Prompts the sender with the executor's {@link Clan}'s trusted allies. Guarantees a valid return {@link Clan} that is trustable to the {@link CommandSourceStack#getExecutor() executor}
     * <p>Casting class {@link Clan}</p>
     * @return the {@link TrustedClanArgument}
     */
    public static TrustedClanArgument trustedClan() {
        return TRUSTED_CLAN;
    }

    /**
     * Prompts the sender with the executor's {@link Clan}'s neutralable {@link Clan}. Guarantees a valid return {@link Clan} that is a neutralable to the {@link CommandSourceStack#getExecutor() executor}
     * <p>Casting class {@link Clan}</p>
     * @return the {@link NeutralableClanArgument}
     * @see ClanManager#canNeutralThrow(Clan, Clan)
     */
    public static NeutralableClanArgument neutralableClan() {
        return NEUTRALABLE_CLAN;
    }

    /**
     * Prompts the sender with the executor's {@link Clan}'s neutral {@link Clan}s. Guarantees a valid return {@link Clan} that is neutral to the {@link CommandSourceStack#getExecutor() executor}
     * <p>Casting class {@link Clan}</p>
     * @return the {@link NeutralClanArgument}
     */
    public static NeutralClanArgument neutralClan() {
        return NEUTRAL_CLAN;
    }

    /**
     * Prompts the sender with the executor's {@link Clan}'s allable {@link Clan}s. Guarantees a valid return {@link Clan} that is allyable to the {@link CommandSourceStack#getExecutor() executor}
     * <p>Uses {@link ClanManager#canAllyThrow(Clan, Clan)} to determine if a {@link Clan} is allyable</p>
     * <p>Casting class {@link Clan}</p>
     * @return the {@link AllyableClanArgument}
     */
    public static AllyableClanArgument allyableClan() {
        return ALLYABLE_CLAN;
    }

    /**
     * Prompts the sender with the executor's {@link Clan}'s enemyable {@link Clan}s. Guarantees a valid return {@link Clan} that is enemyable to the {@link CommandSourceStack#getExecutor() executor}
     * <p>Uses {@link ClanManager#canEnemyThrow(Clan, Clan)} to determine if a {@link Clan} is enemyable</p>
     * <p>Casting class {@link Clan}</p>
     * @return the {@link EnemyableClanArgument}
     */
    public static EnemyableClanArgument enemyableClan() {
        return ENEMYABLE_CLAN;
    }

    /**
     * Prompts the sender with the executor's {@link Clan}'s trustable {@link Clan}s. Guarantees a valid return {@link Clan} that is trustable to the {@link CommandSourceStack#getExecutor() executor}
     * <p>Uses {@link ClanManager#canTrustThrow(Clan, Clan)} to determine if a {@link Clan} is trustable</p>
     * <p>Casting class {@link Clan}</p>
     * @return the {@link NeutralClanArgument}
     * @see BPvPClansArgumentTypes#allyClan()
     */
    public static TrustableClanArgument trustableClan() {
        return TRUSTABLE_CLAN;
    }

    /**
     * Prompts the sender with the executor's {@link Clan}'s joinable {@link Clan}s. Guarantees a valid return {@link Clan} that is joinable to the {@link CommandSourceStack#getExecutor() executor}
     * <p>Uses {@link ClanManager#canJoinClan(Client, Clan)} to determine if a {@link Clan} is joinable</p>
     * <p>Casting class {@link Clan}</p>
     * @return the {@link JoinableClanArgument}
     */
    public static JoinableClanArgument joinableClan() {
        return JOINABLE_CLAN;
    }

    /**
     * Shows a prompt message that this is a Clan Name.
     * <p>Enforces that the returned name contains valid characters and is correct length
     * and is not already taken</p>
     * <p>Casting class {@link String}</p>
     * @return the {@link ClanNameArgument}
     */
    public static ClanNameArgument clanName() {
        return CLAN_NAME;
    }

    /**
     * Prompts the sender with the executors {@link ClanMember}. Guarantee that the name is a valid {@link ClanMember} to the {@link CommandSourceStack#getExecutor() executor}
     * <p>Casting class {@link String}</p>
     * @return the {@link ClanMemberArgument}
     */
    public static ClanMemberArgument clanMember() {
        return CLAN_MEMBER;
    }

    /**
     * Prompts the sender with the executors clan members that are lower rank than the executor {@link ClanMember}.
     * Gurantees the {@link ClanMember} is a lower {@link ClanMember.MemberRank} than the {@link CommandSourceStack#getExecutor() executor}
     * <p>Casting class {@link ClanMember}</p>
     * @return the {@link LowerRankMemberArgument}
     */
    public static LowerRankMemberArgument lowerRankClanMember() {
        return CLAN_MEMBER_LOWER_RANK;
    }

    /**
     * Prompts the sender with the executors clan members that demotable by the executor {@link ClanMember}. Guaruntees a valid demotable {@link ClanMember}
     * <p>Carries out same logic as {@link LowerRankMemberArgument}, but also removes {@link ClanMember}'s with the {@link ClanMember.MemberRank#RECRUIT} rank</p>
     * <p>Casting class {@link ClanMember}</p>
     * @return the {@link DemotableMemberArgument}
     * @see BPvPClansArgumentTypes#lowerRankClanMember()
     */
    public static DemotableMemberArgument demotableClanMember() {
        return CLAN_MEMBER_DEMOTABLE;
    }

    /**
     * Prompts the sender with all invitable {@link Player Players}. Ensures that the {@link Player} returned is valid
     * <p>Casting class {@link Player}</p>
     * @return the {@link InvitablePlayerNameArgument}
     * //TODO player name argument in main
     * @see BPvPClansArgumentTypes#lowerRankClanMember()
     */
    public static InvitablePlayerNameArgument invitablePlayer() {
        return INVITABLE_PLAYER;
    }




}
