package me.mykindos.betterpvp.clans.commands.arguments;

import com.google.inject.Inject;
import com.mojang.brigadier.LiteralMessage;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import me.mykindos.betterpvp.clans.Clans;
import me.mykindos.betterpvp.core.command.brigadier.arguments.BPvPArgumentType;
import me.mykindos.betterpvp.core.command.brigadier.arguments.BPvPArgumentTypes;

public class BPvPClansArgumentTypes {
    //TODO properly extend these in their own classes for more customizable behavior
    public static DynamicCommandExceptionType UNKOWNCLANNAMEEXCEPTION = new DynamicCommandExceptionType((name) -> new LiteralMessage("Unknown Clan Name" + name));
    public static DynamicCommandExceptionType NOTINACLANEXCEPTION = new DynamicCommandExceptionType((player) -> new LiteralMessage(player + " is not in a Clan"));
    public static SimpleCommandExceptionType MUSTBEINACLANEXCEPTION = new SimpleCommandExceptionType(new LiteralMessage("You must be in a Clan to use this command"));
    public static BPvPArgumentType<?, ?> CLAN;
    @Inject
    public BPvPClansArgumentTypes(Clans plugin) {
        BPvPClansArgumentTypes.CLAN = BPvPArgumentTypes.createArgumentType(plugin, ClanArgument.class);
    }
}
