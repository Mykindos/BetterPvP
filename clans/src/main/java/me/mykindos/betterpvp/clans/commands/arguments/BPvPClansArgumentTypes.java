package me.mykindos.betterpvp.clans.commands.arguments;

import com.google.inject.Inject;
import me.mykindos.betterpvp.clans.Clans;
import me.mykindos.betterpvp.core.command.brigadier.arguments.BPvPArgumentType;
import me.mykindos.betterpvp.core.command.brigadier.arguments.BPvPArgumentTypes;

public class BPvPClansArgumentTypes {
    public static BPvPArgumentType<?, ?> CLAN;
    @Inject
    public BPvPClansArgumentTypes(Clans plugin) {
        BPvPClansArgumentTypes.CLAN = BPvPArgumentTypes.createArgumentType(plugin, ClanArgument.class);
    }
}
