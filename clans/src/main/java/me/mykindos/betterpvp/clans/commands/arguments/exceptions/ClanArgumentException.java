package me.mykindos.betterpvp.clans.commands.arguments.exceptions;

import com.mojang.brigadier.LiteralMessage;
import com.mojang.brigadier.exceptions.Dynamic2CommandExceptionType;
import com.mojang.brigadier.exceptions.Dynamic3CommandExceptionType;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;

public class ClanArgumentException {

    public static final DynamicCommandExceptionType MUST_NOT_BE_IN_A_CLAN_EXCEPTION = new DynamicCommandExceptionType(
            (player) -> new LiteralMessage(player + " must not be in a Clan")
    );
    public final static Dynamic3CommandExceptionType INVALID_CLAN_NAME = new Dynamic3CommandExceptionType((name, min, max) ->
        new LiteralMessage(name + " is not a valid Clan name. Clan names must be between " + min + " and " + max + " characters long and only include alphanumeric characters and '_'")
    );
    public final static DynamicCommandExceptionType NAME_ALREADY_EXISTS = new DynamicCommandExceptionType((name) ->
            new LiteralMessage(name + "is already in use by another Clan")
    );
    public final static DynamicCommandExceptionType NAME_IS_FILTERED = new DynamicCommandExceptionType((name) ->
            new LiteralMessage(name + " contains a filtered word")
    );
    public static final SimpleCommandExceptionType MEMBER_CLAN_CANNOT_ACTION_SELF = new SimpleCommandExceptionType(
            new LiteralMessage("You cannot do this action to yourself")
    );
    public static final DynamicCommandExceptionType MEMBER_CANNOT_ACTION_MEMBER_RANK = new DynamicCommandExceptionType(
            (targetName) -> new LiteralMessage("Rank insufficient to do this action to " + targetName)
    );
    public static final Dynamic2CommandExceptionType MEMBER_NOT_MEMBER_OF_CLAN = new Dynamic2CommandExceptionType(
            (clanName, targetName) -> new LiteralMessage(targetName + "is not a member of " + clanName)
    );
    public static final DynamicCommandExceptionType TARGET_MEMBER_RANK_TOO_LOW = new DynamicCommandExceptionType(
            (targetName) -> new LiteralMessage(targetName + "'s rank is too low for this action to be performed")
    );
    public static final DynamicCommandExceptionType TARGET_MEMBER_IN_ENEMY_TERRITORY = new DynamicCommandExceptionType(
            (targetName) -> new LiteralMessage(targetName + " cannot be kicked in enemy territory")
    );
    public static final DynamicCommandExceptionType UNKNOWN_CLAN_NAME_EXCEPTION = new DynamicCommandExceptionType(
            (name) -> new LiteralMessage("Unknown Clan name " + name)
    );
    public static final DynamicCommandExceptionType NOT_IN_A_CLAN_EXCEPTION = new DynamicCommandExceptionType(
            (player) -> new LiteralMessage(player + " is not in a Clan")
    );
    public static final SimpleCommandExceptionType MUST_BE_IN_A_CLAN_EXCEPTION = new SimpleCommandExceptionType(
            new LiteralMessage("You must be in a Clan to use this command")
    );
    public static final Clan2CommandExceptionType CLAN_MUST_NOT_BE_SAME = new Clan2CommandExceptionType(
            (origin, target) -> new LiteralMessage(origin.getName() + " must be a different Clan than " + target.getName())
    );
    //Ally
    public static final Clan2CommandExceptionType CLAN_NOT_NEUTRAL_OF_CLAN = new Clan2CommandExceptionType(
            (origin, target) -> new LiteralMessage(target.getName() + " is not Neutral to " + origin.getName())
    );
    public static final Clan2CommandExceptionType CLAN_NOT_ENEMY_OF_CLAN = new Clan2CommandExceptionType(
            (origin, target) -> new LiteralMessage(target.getName() + " is not an Enemy of " + origin.getName())
    );
    public static final Clan2CommandExceptionType CLAN_NOT_ALLY_OF_CLAN = new Clan2CommandExceptionType(
            (origin, target) -> new LiteralMessage(target.getName() + " is not an Ally of " + origin.getName())
    );
    public static final Clan2CommandExceptionType CLAN_NOT_ALLY_OR_ENEMY_OF_CLAN = new Clan2CommandExceptionType(
            (origin, target) -> new LiteralMessage(target.getName() + " is not an Ally or Enemy of " + origin.getName()));
    public static final Dynamic2CommandExceptionType CLAN_AT_MAX_SQUAD_COUNT_ALLY = new Dynamic2CommandExceptionType(
            (origin, size) -> new LiteralMessage(origin + " is at the maximum squad size " + size + " and cannot ally")
    );
    public static final Dynamic2CommandExceptionType CLAN_OVER_MAX_SQUAD_COUNT_ALLY = new Dynamic2CommandExceptionType(
            (clanName, size) -> new LiteralMessage(clanName + " has too high a squad count " + size + " to ally")
    );
    public static final Clan2CommandExceptionType CLAN_ALREADY_TRUSTS_CLAN = new Clan2CommandExceptionType(
            (origin, target) -> new LiteralMessage(target.getName() + " is already trusted by " + origin.getName())
    );
    public static final Clan2CommandExceptionType CLAN_DOES_NOT_TRUST_CLAN = new Clan2CommandExceptionType(
            (origin, target) -> new LiteralMessage(target.getName() + " does not trust " + origin.getName())
    );

    //Invite
    public static final Dynamic3CommandExceptionType CLAN_AT_MAX_SQUAD_COUNT_INVITE = new Dynamic3CommandExceptionType(
            (originName, targetName, size) -> new LiteralMessage(originName + " is at the maximum squad size " + size + " and cannot invite " + targetName)
    );
    public static final Dynamic3CommandExceptionType ALLY_AT_MAX_SQUAD_COUNT_INVITE = new Dynamic3CommandExceptionType(
            (allyName, targetName, size) -> new LiteralMessage(allyName + " is at the maximum squad size " + size + " and inviting " + targetName + " would put them over")
    );

    //Join
    public static final Dynamic2CommandExceptionType CLAN_AT_MAX_SQUAD_COUNT_JOIN = new Dynamic2CommandExceptionType(
            (originName, size) -> new LiteralMessage(originName + " is at the maximum squad size " + size + " and cannot accept new members")
    );
    public static final Dynamic3CommandExceptionType ALLY_AT_MAX_SQUAD_COUNT_JOIN = new Dynamic3CommandExceptionType(
            (originName, allyName, size) -> new LiteralMessage(allyName + " is at the maximum squad size " + size + " making " + originName + " unable to accept new members")
    );
}
